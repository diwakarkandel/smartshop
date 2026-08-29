package com.smartshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartshop.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SaleTaxFlowTest {

    private static final String PASSWORD = "secret12345";
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory factory;

    private String shopAdminToken;
    private String shopId;
    private String branchId;
    private String categoryId;
    private String productId;
    private String supplierId;

    @BeforeEach
    void setUp() throws Exception {
        int seq = SEQ.incrementAndGet();
        String suffix = "tax" + seq;
        String superEmail = "super." + suffix + "@test.com";
        String adminEmail = "admin." + suffix + "@test.com";
        String pan = String.valueOf(200000000L + seq);

        factory.createSuperAdmin(superEmail, PASSWORD);
        String superAdminToken = login(superEmail);

        shopId = json(mockMvc.perform(post("/api/v1/shops")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Tax Shop %s","panVatNumber":"%s","phone":"9800000000",
                                 "email":"shop.%s@test.com","address":"Kathmandu"}
                                """.formatted(seq, pan, suffix)))
                .andExpect(status().isCreated())
                .andReturn(), "data.id");

        branchId = json(mockMvc.perform(post("/api/v1/branches")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId":"%s","name":"Main Branch","code":"TAX%s","address":"Kathmandu",
                                 "contactNumber":"9800000000","isMainBranch":true}
                                """.formatted(shopId, seq)))
                .andExpect(status().isCreated())
                .andReturn(), "data.id");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Shop","lastName":"Admin","email":"%s","password":"%s"}
                                """.formatted(adminEmail, PASSWORD)))
                .andExpect(status().isCreated());
        factory.grantShopRole(factory.findUserId(adminEmail), UUID.fromString(shopId), "SHOP_ADMIN");
        shopAdminToken = login(adminEmail);

        categoryId = json(mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId":"%s","name":"Vatable","code":"VAT%s"}
                                """.formatted(shopId, seq)))
                .andExpect(status().isCreated())
                .andReturn(), "data.id");

        productId = json(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId":"%s","categoryId":"%s","name":"Taxed Lamp","sku":"LMP-%s",
                                 "unit":"pcs","purchasePrice":100.00,"sellingPrice":150.00,
                                 "vatApplicable":true,"vatRate":13.00,"reorderLevel":5}
                                """.formatted(shopId, categoryId, suffix)))
                .andExpect(status().isCreated())
                .andReturn(), "data.id");

        supplierId = json(mockMvc.perform(post("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId":"%s","name":"Tax Supplies","companyName":"ACME","phone":"9800000001"}
                                """.formatted(shopId)))
                .andExpect(status().isCreated())
                .andReturn(), "data.id");

        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId":"%s","branchId":"%s","supplierId":"%s","paymentStatus":"UNPAID",
                                 "items":[{"productId":"%s","quantity":100.00,"unitCost":100.00,"vatRate":13.00}]}
                                """.formatted(shopId, branchId, supplierId, productId)))
                .andExpect(status().isCreated());
    }

    @Test
    void saleResponseExposesPerItemAndHeaderTaxBreakdown() throws Exception {
        String saleId = json(mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId":"%s","branchId":"%s","paymentStatus":"PAID","paymentMethod":"CASH",
                                 "cashTendered":400.00,
                                 "items":[{"productId":"%s","quantity":2.00,"unitPrice":150.00,"vatRate":13.00}]}
                                """.formatted(shopId, branchId, productId)))
                .andExpect(status().isCreated())
                .andReturn(), "data.id");

        mockMvc.perform(get("/api/v1/sales/{id}", saleId)
                        .header("Authorization", "Bearer " + shopAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subtotal").value(300.0))
                .andExpect(jsonPath("$.data.discountAmount").value(0.0))
                .andExpect(jsonPath("$.data.taxableAmount").value(300.0))
                .andExpect(jsonPath("$.data.vatAmount").value(39.0))
                .andExpect(jsonPath("$.data.totalAmount").value(339.0))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2.0))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(150.0))
                .andExpect(jsonPath("$.data.items[0].vatRate").value(13.0))
                .andExpect(jsonPath("$.data.items[0].vatAmount").value(39.0))
                .andExpect(jsonPath("$.data.items[0].taxableAmount").value(300.0))
                .andExpect(jsonPath("$.data.items[0].lineTotal").value(339.0));
    }

    @Test
    void purchaseResponseExposesPerItemTaxableAmount() throws Exception {
        String purchaseId = json(mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId":"%s","branchId":"%s","supplierId":"%s","paymentStatus":"UNPAID",
                                 "items":[{"productId":"%s","quantity":10.00,"unitCost":100.00,"vatRate":13.00}]}
                                """.formatted(shopId, branchId, supplierId, productId)))
                .andExpect(status().isCreated())
                .andReturn(), "data.id");

        mockMvc.perform(get("/api/v1/purchases/{id}", purchaseId)
                        .header("Authorization", "Bearer " + shopAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subtotal").value(1000.0))
                .andExpect(jsonPath("$.data.taxableAmount").value(1000.0))
                .andExpect(jsonPath("$.data.vatAmount").value(130.0))
                .andExpect(jsonPath("$.data.totalAmount").value(1130.0))
                .andExpect(jsonPath("$.data.items[0].vatRate").value(13.0))
                .andExpect(jsonPath("$.data.items[0].vatAmount").value(130.0))
                .andExpect(jsonPath("$.data.items[0].taxableAmount").value(1000.0))
                .andExpect(jsonPath("$.data.items[0].lineTotal").value(1130.0));
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();
        return json(result, "data.accessToken");
    }

    private String json(MvcResult result, String path) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.at("/" + path.replace(".", "/")).asText();
    }
}