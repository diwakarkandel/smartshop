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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full business journey through the HTTP layer:
 * auth -> shops/branches -> roles -> categories -> products -> suppliers/customers
 * -> purchases (stock in, weighted cost) -> POS sales (stock out) -> sale return (restock)
 * -> purchase return -> stock transfer (approve) -> expense -> settings -> reports -> audit.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EndToEndJourneyTest {

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
    private String branch1;
    private String branch2;
    private String productId;
    private String saleItemId;

    @BeforeEach
    void setUp() throws Exception {
        int seq = SEQ.incrementAndGet();
        String suffix = "e2e" + seq;
        String superEmail = "super." + suffix + "@test.com";
        String adminEmail = "admin." + suffix + "@test.com";
        String pan = String.valueOf(200000000L + seq);

        factory.createSuperAdmin(superEmail, PASSWORD);
        superAdminToken = login(superEmail);

        shopId = json(postJson(superAdminToken, "/api/v1/shops", """
                {"name":"E2E Shop %s","panVatNumber":"%s","phone":"9800000000",
                 "email":"shop.%s@test.com","address":"Kathmandu"}
                """.formatted(seq, pan, suffix), 201), "data.id");

        branch1 = json(postJson(superAdminToken, "/api/v1/branches", """
                {"shopId":"%s","name":"Branch One","code":"B1%s","address":"KTM","isMainBranch":true}
                """.formatted(shopId, seq), 201), "data.id");

        branch2 = json(postJson(superAdminToken, "/api/v1/branches", """
                {"shopId":"%s","name":"Branch Two","code":"B2%s","address":"PKR"}
                """.formatted(shopId, seq), 201), "data.id");

        postJson(null, "/api/v1/auth/register", """
                {"firstName":"E2E","lastName":"Admin","email":"%s","password":"%s"}
                """.formatted(adminEmail, PASSWORD), 201);

        factory.grantShopRole(factory.findUserId(adminEmail), UUID.fromString(shopId), "SHOP_ADMIN");
        shopAdminToken = login(adminEmail);
    }

    @Test
    void fullBusinessJourney() throws Exception {
        // Categories and products
        String categoryId = json(postJson(shopAdminToken, "/api/v1/categories", """
                {"shopId":"%s","name":"Electronics","code":"ELEC%s"}
                """.formatted(shopId, SEQ.get()), 201), "data.id");

        productId = json(postJson(shopAdminToken, "/api/v1/products", """
                {"shopId":"%s","categoryId":"%s","name":"LED Bulb","sku":"LED-E2E-%s",
                 "unit":"pcs","purchasePrice":100.00,"sellingPrice":150.00,
                 "vatApplicable":true,"vatRate":13.00,"reorderLevel":5}
                """.formatted(shopId, categoryId, SEQ.get()), 201), "data.id");

        String supplierId = json(postJson(shopAdminToken, "/api/v1/suppliers", """
                {"shopId":"%s","name":"ACME Supplies","companyName":"ACME","phone":"9800000001"}
                """.formatted(shopId), 201), "data.id");

        String customerId = json(postJson(shopAdminToken, "/api/v1/customers", """
                {"shopId":"%s","name":"Ram Sharma","phone":"9811111111"}
                """.formatted(shopId), 201), "data.id");

        // Purchase 1: 100 @ 100 -> stock 100, avg cost 100
        String purchase1Id = json(postJson(shopAdminToken, "/api/v1/purchases", """
                {"shopId":"%s","branchId":"%s","supplierId":"%s","paymentStatus":"UNPAID",
                 "items":[{"productId":"%s","quantity":100.00,"unitCost":100.00,"vatRate":13.00}]}
                """.formatted(shopId, branch1, supplierId, productId), 201), "data.id");
        assertStock(branch1, 100.0, 100.0);

        // Purchase 2: 50 @ 120 -> stock 150, weighted avg cost (100*100+50*120)/150 = 106.67
        postJson(shopAdminToken, "/api/v1/purchases", """
                {"shopId":"%s","branchId":"%s","supplierId":"%s","paymentStatus":"UNPAID",
                 "items":[{"productId":"%s","quantity":50.00,"unitCost":120.00,"vatRate":13.00}]}
                """.formatted(shopId, branch1, supplierId, productId), 201);
        assertStock(branch1, 150.0, 106.67);

        // POS sale (CASH) qty 2 -> stock 148, change computed
        MvcResult saleResult = postJson(shopAdminToken, "/api/v1/sales", """
                {"shopId":"%s","branchId":"%s","customerId":"%s","paymentStatus":"PAID","paymentMethod":"CASH",
                 "cashTendered":400.00,
                 "items":[{"productId":"%s","quantity":2.00,"unitPrice":150.00,"vatRate":13.00}]}
                """.formatted(shopId, branch1, customerId, productId), 201);
        String saleId = json(saleResult, "data.id");
        String invoice = json(saleResult, "data.invoiceNumber");
        MvcResult saleDetail = getJson(shopAdminToken, "/api/v1/sales/" + saleId, null);
        saleItemId = json(saleDetail, "data.items[0].id");
        assertStock(branch1, 148.0, 106.67);

        // Sale return qty 1 -> restock -> 149
        postJson(shopAdminToken, "/api/v1/sale-returns", """
                {"saleId":"%s","branchId":"%s","reason":"Customer changed mind",
                 "items":[{"saleItemId":"%s","productId":"%s","quantity":1.00}]}
                """.formatted(saleId, branch1, saleItemId, productId), 201);
        assertStock(branch1, 149.0, 106.67);

        // Purchase return qty 10 -> 139
        MvcResult purchaseResult = getJson(shopAdminToken, "/api/v1/purchases/" + purchase1Id, null);
        String purchaseItemId = json(purchaseResult, "data.items[0].id");
        postJson(shopAdminToken, "/api/v1/purchase-returns", """
                {"purchaseId":"%s","branchId":"%s","reason":"Damaged goods",
                 "items":[{"purchaseItemId":"%s","productId":"%s","quantity":10.00}]}
                """.formatted(purchase1Id, branch1, purchaseItemId, productId), 201);
        assertStock(branch1, 139.0, 106.67);

        // Stock transfer branch1 -> branch2 qty 5, approve -> 134 / 5
        String transferId = json(postJson(shopAdminToken, "/api/v1/stock-transfers", """
                {"shopId":"%s","fromBranchId":"%s","toBranchId":"%s",
                 "items":[{"productId":"%s","quantity":5.00}]}
                """.formatted(shopId, branch1, branch2, productId), 201), "data.id");
        postJson(shopAdminToken, "/api/v1/stock-transfers/" + transferId + "/approve", "{}", 200);
        assertStock(branch1, 134.0, 106.67);
        assertStock(branch2, 5.0, 106.67);

        // Expense
        postJson(shopAdminToken, "/api/v1/expenses", """
                {"shopId":"%s","branchId":"%s","title":"Electricity bill","category":"Utilities",
                 "amount":1500.00,"paymentMethod":"CASH"}
                """.formatted(shopId, branch1), 201);

        // Settings: update VAT 13 -> 15
        putJson(shopAdminToken, "/api/v1/settings", """
                {"shopId":"%s","key":"VAT_RATE","value":"15.00"}
                """.formatted(shopId), 200);
        mockMvc.perform(get("/api/v1/settings")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .param("shopId", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.settingKey=='VAT_RATE')].settingValue")
                        .value(hasItem("15.00")));

        // Reports
        mockMvc.perform(get("/api/v1/reports/dashboard")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .param("shopId", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSales").isNumber())
                .andExpect(jsonPath("$.data.lowStockCount").isNumber());

        mockMvc.perform(get("/api/v1/reports/sales-summary")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .param("shopId", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSales").isNumber());

        // Audit log recorded the journey
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        // Invoice number is branch-prefixed
        mockMvc.perform(get("/api/v1/sales/" + saleId)
                        .header("Authorization", "Bearer " + shopAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoiceNumber").value(invoice));

        // Overselling is rejected with 400
        postJson(shopAdminToken, "/api/v1/sales", """
                {"shopId":"%s","branchId":"%s","paymentStatus":"PAID","paymentMethod":"CASH",
                 "items":[{"productId":"%s","quantity":99999.00,"unitPrice":150.00,"vatRate":13.00}]}
                """.formatted(shopId, branch1, productId), 400);
    }

    private void assertStock(String branchId, double expectedQty, double expectedCost) throws Exception {
        mockMvc.perform(get("/api/v1/inventory")
                        .header("Authorization", "Bearer " + shopAdminToken)
                        .param("branchId", branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.productId=='%s')].quantityAvailable".formatted(productId))
                        .value(hasItem(expectedQty)))
                .andExpect(jsonPath("$.data.content[?(@.productId=='%s')].averageCost".formatted(productId))
                        .value(hasItem(expectedCost)));
    }

    private MvcResult postJson(String token, String path, String body, int expectedStatus) throws Exception {
        var request = post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (token != null) {
            request = request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
    }

    private MvcResult putJson(String token, String path, String body, int expectedStatus) throws Exception {
        return mockMvc.perform(put(path)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expectedStatus))
                .andReturn();
    }

    private MvcResult getJson(String token, String path, String query) throws Exception {
        var request = get(path).header("Authorization", "Bearer " + token);
        if (query != null) {
            for (String pair : query.split("&")) {
                String[] kv = pair.split("=");
                request = request.param(kv[0], kv[1]);
            }
        }
        return mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    }

    private String login(String email) throws Exception {
        MvcResult result = postJson(null, "/api/v1/auth/login", """
                {"email":"%s","password":"%s"}
                """.formatted(email, PASSWORD), 200);
        return json(result, "data.accessToken");
    }

    private String json(MvcResult result, String path) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        String pointer = "/" + path.replace(".", "/").replaceAll("\\[(\\d+)\\]", "/$1");
        JsonNode value = node.at(pointer);
        if (value.isMissingNode() || value.isNull()) {
            throw new AssertionError("Missing JSON path: " + path);
        }
        return value.asText();
    }
}
