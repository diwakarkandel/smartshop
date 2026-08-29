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
class InventoryFlowTest {

    private static final String PASSWORD = "secret12345";
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory factory;

    private String superAdminToken;
    private String shopAdminToken;
    private String shopId;
    private String branchId;

    @BeforeEach
    void setUp() throws Exception {
        int seq = SEQ.incrementAndGet();
        String suffix = "inv" + seq;
        String superEmail = "super." + suffix + "@test.com";
        String adminEmail = "admin." + suffix + "@test.com";
        String pan = String.valueOf(100000000L + seq);

        factory.createSuperAdmin(superEmail, PASSWORD);
        superAdminToken = login(superEmail);

        shopId = json(mockMvc.perform(post("/api/v1/shops")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Shop %s","panVatNumber":"%s","phone":"9800000000",
                                 "email":"shop.%s@test.com","address":"Kathmandu"}
                                """.formatted(seq, pan, suffix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andReturn(), "data.id");

        branchId = json(mockMvc.perform(post("/api/v1/branches")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId":"%s","name":"Main Branch","code":"MAIN%s","address":"Kathmandu",
                                 "contactNumber":"9800000000","isMainBranch":true}
                                """.formatted(shopId, seq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andReturn(), "data.id");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Shop","lastName":"Admin","email":"%s","password":"%s"}
                                """.formatted(adminEmail, PASSWORD)))
                .andExpect(status().isCreated());

        factory.grantShopRole(factory.findUserId(adminEmail), UUID.fromString(shopId), "SHOP_ADMIN");
        shopAdminToken = login(adminEmail);
    }

    @Test
    void purchaseAddsStockAndSaleDecrementsIt() throws Exception {
        String categoryId = json(mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId":"%s","name":"Electronics","code":"ELEC"}
                                """.formatted(shopId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andReturn(), "data.id");

        String productId = json(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId":"%s","categoryId":"%s","name":"LED Bulb","sku":"LED-001",
                                 "unit":"pcs","purchasePrice":100.00,"sellingPrice":150.00,
                                 "vatApplicable":true,"vatRate":13.00,"reorderLevel":5}
                                """.formatted(shopId, categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andReturn(), "data.id");

        String supplierId = json(mockMvc.perform(post("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId":"%s","name":"ACME Supplies","companyName":"ACME","phone":"9800000001"}
                                """.formatted(shopId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andReturn(), "data.id");

        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId":"%s","branchId":"%s","supplierId":"%s","paymentStatus":"UNPAID",
                                 "items":[{"productId":"%s","quantity":100.00,"unitCost":100.00,"vatRate":13.00}]}
                                """.formatted(shopId, branchId, supplierId, productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.purchaseNumber").isNotEmpty());

        mockMvc.perform(get("/api/v1/inventory")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .param("branchId", branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].productId").value(productId))
                .andExpect(jsonPath("$.data.content[0].quantityAvailable").value(100.0))
                .andExpect(jsonPath("$.data.content[0].averageCost").value(100.0));

        mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId":"%s","branchId":"%s","paymentStatus":"PAID","paymentMethod":"CASH",
                                 "cashTendered":400.00,
                                 "items":[{"productId":"%s","quantity":2.00,"unitPrice":150.00,"vatRate":13.00}]}
                                """.formatted(shopId, branchId, productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.invoiceNumber").isNotEmpty());

        mockMvc.perform(get("/api/v1/inventory")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .param("branchId", branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].productId").value(productId))
                .andExpect(jsonPath("$.data.content[0].quantityAvailable").value(98.0));
    }

    @Test
    void saleFailsWhenStockInsufficient() throws Exception {
        String categoryId = json(mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId":"%s","name":"Groceries","code":"GROC"}
                                """.formatted(shopId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andReturn(), "data.id");

        String productId = json(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId":"%s","categoryId":"%s","name":"Rice","sku":"RICE-001",
                                 "unit":"kg","purchasePrice":60.00,"sellingPrice":80.00,"vatRate":13.00}
                                """.formatted(shopId, categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andReturn(), "data.id");

        mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId":"%s","branchId":"%s","paymentStatus":"PAID","paymentMethod":"CASH",
                                 "items":[{"productId":"%s","quantity":10.00,"unitPrice":80.00,"vatRate":13.00}]}
                                """.formatted(shopId, branchId, productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void nonAdminCannotCreateShops() throws Exception {
        mockMvc.perform(post("/api/v1/shops")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Hacked Shop","panVatNumber":"000000000"}
                                """))
                .andExpect(status().isForbidden());
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