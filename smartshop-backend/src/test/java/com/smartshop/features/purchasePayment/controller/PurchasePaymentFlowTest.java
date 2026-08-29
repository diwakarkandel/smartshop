package com.smartshop.features.purchasePayment.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.branch.repository.BranchRepository;
import com.smartshop.shared.enumeration.BranchStatus;
import com.smartshop.features.purchase.entity.Purchase;
import com.smartshop.features.purchase.repository.PurchaseRepository;
import com.smartshop.features.purchasePayment.dto.PurchasePaymentRequest;
import com.smartshop.features.purchasePayment.dto.PurchasePaymentResponse;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.features.supplier.entity.Supplier;
import com.smartshop.features.supplier.repository.SupplierRepository;
import com.smartshop.features.user.entity.User;
import com.smartshop.shared.enumeration.PaymentMethod;
import com.smartshop.shared.enumeration.PaymentStatus;
import com.smartshop.shared.enumeration.ShopStatus;
import com.smartshop.shared.enumeration.Status;
import com.smartshop.shared.response.ApiResponse;
import com.smartshop.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PurchasePaymentFlowTest {

    private static final String PASSWORD = "secret12345";
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory factory;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    private String adminToken;
    private Purchase testPurchase;

    @BeforeEach
    void setUp() throws Exception {
        int seq = SEQ.incrementAndGet();
        String suffix = "pp" + seq;

        // Create a shop admin user
        User adminUser = factory.createUser("admin." + suffix + "@pp.test", PASSWORD);

        // Create a shop and branch
        Shop shop = new Shop();
        shop.setName("PayTest Shop " + suffix);
        shop.setPanVatNumber("PP-" + suffix);
        shop.setStatus(ShopStatus.ACTIVE);
        shop = shopRepository.save(shop);

        Branch branch = new Branch();
        branch.setShop(shop);
        branch.setName("Main Branch");
        branch.setCode("PP-BR-" + suffix);
        branch.setAddress("123 Test St");
        branch.setStatus(BranchStatus.ACTIVE);
        branch = branchRepository.save(branch);

        // Grant admin role on the shop
        factory.grantShopRole(adminUser.getId(), shop.getId(), "SHOP_ADMIN");

        // Create a supplier
        Supplier supplier = new Supplier();
        supplier.setShop(shop);
        supplier.setName("Test Supplier " + suffix);
        supplier.setPhone("9800000000");
        supplier.setStatus(Status.ACTIVE);
        supplier = supplierRepository.save(supplier);

        // Create a purchase
        Purchase purchase = new Purchase();
        purchase.setShop(shop);
        purchase.setBranch(branch);
        purchase.setSupplier(supplier);
        purchase.setPurchaseNumber("PUR-" + suffix);
        purchase.setPurchaseDate(LocalDate.now());
        purchase.setSubtotal(new BigDecimal("1000.00"));
        purchase.setTaxableAmount(new BigDecimal("1000.00"));
        purchase.setTotalAmount(new BigDecimal("1000.00"));
        purchase.setPaymentStatus(PaymentStatus.UNPAID);
        purchase.setCreatedBy(adminUser);
        testPurchase = purchaseRepository.save(purchase);

        // Log in to get JWT token
        String loginBody = """
                {"email":"admin.%s@pp.test","password":"%s"}
                """.formatted(suffix, PASSWORD);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        adminToken = loginJson.at("/data/accessToken").asText();
    }

    @Test
    void testPartialPaymentThenFullPayment() throws Exception {
        String purchaseUrl = "/api/v1/purchases/" + testPurchase.getId() + "/payments";

        // 1. Make a partial payment of 400
        PurchasePaymentRequest req1 = new PurchasePaymentRequest();
        req1.setPaymentDate(LocalDate.now());
        req1.setAmount(new BigDecimal("400.00"));
        req1.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        req1.setReferenceNumber("REF-001");

        mockMvc.perform(post(purchaseUrl)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Verify purchase is now PARTIAL
        Purchase p1 = purchaseRepository.findById(testPurchase.getId()).orElseThrow();
        assertThat(p1.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIAL);

        // 2. Make second payment of 600 — completing the full 1000 total
        PurchasePaymentRequest req2 = new PurchasePaymentRequest();
        req2.setPaymentDate(LocalDate.now());
        req2.setAmount(new BigDecimal("600.00"));
        req2.setPaymentMethod(PaymentMethod.CASH);

        mockMvc.perform(post(purchaseUrl)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());

        // Verify purchase is now PAID
        Purchase p2 = purchaseRepository.findById(testPurchase.getId()).orElseThrow();
        assertThat(p2.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

        // 3. Try to pay again on a PAID purchase — should fail
        PurchasePaymentRequest req3 = new PurchasePaymentRequest();
        req3.setPaymentDate(LocalDate.now());
        req3.setAmount(new BigDecimal("100.00"));
        req3.setPaymentMethod(PaymentMethod.CASH);

        mockMvc.perform(post(purchaseUrl)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isBadRequest());

        // 4. Verify list endpoint returns 2 payments in descending order
        MvcResult listResult = mockMvc.perform(get(purchaseUrl)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<List<PurchasePaymentResponse>> response = objectMapper.readValue(
                listResult.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );

        assertThat(response.getData()).hasSize(2);
        // Most recent (600) should be first
        assertThat(response.getData().get(0).getAmount()).isEqualByComparingTo("600.00");
        assertThat(response.getData().get(1).getAmount()).isEqualByComparingTo("400.00");
    }

    @Test
    void testOverpaymentIsRejected() throws Exception {
        String purchaseUrl = "/api/v1/purchases/" + testPurchase.getId() + "/payments";

        // Try to pay more than the total amount (1000 total, paying 1500)
        PurchasePaymentRequest req = new PurchasePaymentRequest();
        req.setPaymentDate(LocalDate.now());
        req.setAmount(new BigDecimal("1500.00"));
        req.setPaymentMethod(PaymentMethod.CASH);

        mockMvc.perform(post(purchaseUrl)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        // Purchase should still be UNPAID
        Purchase p = purchaseRepository.findById(testPurchase.getId()).orElseThrow();
        assertThat(p.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }
}
