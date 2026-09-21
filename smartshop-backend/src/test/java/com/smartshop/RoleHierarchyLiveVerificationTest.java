package com.smartshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartshop.features.role.entity.Role;
import com.smartshop.features.role.repository.RoleRepository;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.features.userBranchRole.repository.UserBranchRoleRepository;
import com.smartshop.shared.enumeration.UserStatus;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RoleHierarchyLiveVerificationTest {

    private static final String PASSWORD = "secret12345";
    private static final AtomicInteger SEQ = new AtomicInteger(100);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory factory;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserBranchRoleRepository userBranchRoleRepository;

    private String superAdminToken;
    private String shopAdminAToken;
    private String staffAToken;
    private String shopAdminBToken;
    private String staffBToken;
    private String customerToken;

    private UUID shopAId;
    private UUID shopBId;
    private UUID branchAId;
    private UUID branchBId;

    private UUID userShopAId;
    private UUID userShopBId;
    private UUID staffAUserId;

    private UUID productAId;
    private UUID productBId;
    private UUID taxAId;

    @BeforeEach
    void setUp() throws Exception {
        int seq = SEQ.incrementAndGet();
        String superEmail = "super." + seq + "@test.com";
        String adminAEmail = "adminA." + seq + "@test.com";
        String staffAEmail = "staffA." + seq + "@test.com";
        String adminBEmail = "adminB." + seq + "@test.com";
        String staffBEmail = "staffB." + seq + "@test.com";
        String custEmail = "cust." + seq + "@test.com";

        // 1. Create Super Admin
        factory.createSuperAdmin(superEmail, PASSWORD);
        superAdminToken = login(superEmail);

        // 2. Create Shop A and Shop B
        String resShopA = postJson(superAdminToken, "/api/v1/shops", """
                {"name":"Shop A %s","panVatNumber":"3000%s","phone":"9811111111","email":"shopA%s@test.com","address":"KTM"}
                """.formatted(seq, seq, seq), 201);
        shopAId = UUID.fromString(json(resShopA, "data.id"));

        String resShopB = postJson(superAdminToken, "/api/v1/shops", """
                {"name":"Shop B %s","panVatNumber":"4000%s","phone":"9822222222","email":"shopB%s@test.com","address":"PKR"}
                """.formatted(seq, seq, seq), 201);
        shopBId = UUID.fromString(json(resShopB, "data.id"));

        // 3. Create Branches
        String resBranchA = postJson(superAdminToken, "/api/v1/branches", """
                {"shopId":"%s","name":"Branch A","code":"BRA%s","address":"KTM","isMainBranch":true}
                """.formatted(shopAId, seq), 201);
        branchAId = UUID.fromString(json(resBranchA, "data.id"));

        String resBranchB = postJson(superAdminToken, "/api/v1/branches", """
                {"shopId":"%s","name":"Branch B","code":"BRB%s","address":"PKR","isMainBranch":true}
                """.formatted(shopBId, seq), 201);
        branchBId = UUID.fromString(json(resBranchB, "data.id"));

        // 4. Create Users
        postJson(null, "/api/v1/auth/register", """
                {"firstName":"Admin","lastName":"A","email":"%s","password":"%s"}
                """.formatted(adminAEmail, PASSWORD), 201);
        userShopAId = factory.findUserId(adminAEmail);
        factory.grantShopRole(userShopAId, shopAId, "SHOP_ADMIN");
        shopAdminAToken = login(adminAEmail);

        postJson(null, "/api/v1/auth/register", """
                {"firstName":"Staff","lastName":"A","email":"%s","password":"%s"}
                """.formatted(staffAEmail, PASSWORD), 201);
        staffAUserId = factory.findUserId(staffAEmail);
        factory.grantBranchRole(staffAUserId, branchAId, "CASHIER");
        staffAToken = login(staffAEmail);

        postJson(null, "/api/v1/auth/register", """
                {"firstName":"Admin","lastName":"B","email":"%s","password":"%s"}
                """.formatted(adminBEmail, PASSWORD), 201);
        userShopBId = factory.findUserId(adminBEmail);
        factory.grantShopRole(userShopBId, shopBId, "SHOP_ADMIN");
        shopAdminBToken = login(adminBEmail);

        postJson(null, "/api/v1/auth/register", """
                {"firstName":"Staff","lastName":"B","email":"%s","password":"%s"}
                """.formatted(staffBEmail, PASSWORD), 201);
        UUID staffBUserId = factory.findUserId(staffBEmail);
        factory.grantBranchRole(staffBUserId, branchBId, "CASHIER");
        staffBToken = login(staffBEmail);

        // Customer (regular registered user with no branch roles)
        postJson(null, "/api/v1/auth/register", """
                {"firstName":"Customer","lastName":"User","email":"%s","password":"%s"}
                """.formatted(custEmail, PASSWORD), 201);
        customerToken = login(custEmail);

        // Products in Shop A and Shop B
        String resProdA = postJson(shopAdminAToken, "/api/v1/products", """
                {"shopId":"%s","name":"Product A","sku":"SKU-A-%s","unit":"pcs","purchasePrice":10,"sellingPrice":20}
                """.formatted(shopAId, seq), 201);
        productAId = UUID.fromString(json(resProdA, "data.id"));

        String resProdB = postJson(shopAdminBToken, "/api/v1/products", """
                {"shopId":"%s","name":"Product B","sku":"SKU-B-%s","unit":"pcs","purchasePrice":15,"sellingPrice":25}
                """.formatted(shopBId, seq), 201);
        productBId = UUID.fromString(json(resProdB, "data.id"));


    }

    @Test
    void runAll20RoleHierarchyTests() throws Exception {
        System.out.println("===============================================================================");
        System.out.println("STARTING 20 ROLE HIERARCHY VERIFICATION TESTS");
        System.out.println("===============================================================================");

        // --- 1. SHOP_ADMIN of Shop A calls GET /api/v1/users ---
        MvcResult res1 = mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + shopAdminAToken)).andReturn();
        System.out.println("TEST 1 Status: " + res1.getResponse().getStatus());
        System.out.println("TEST 1 Body: " + res1.getResponse().getContentAsString());

        // --- 2. SHOP_ADMIN of Shop A calls GET /api/v1/users/{id} with Shop B user id ---
        MvcResult res2 = mockMvc.perform(get("/api/v1/users/" + userShopBId).header("Authorization", "Bearer " + shopAdminAToken)).andReturn();
        System.out.println("TEST 2 Status: " + res2.getResponse().getStatus());
        System.out.println("TEST 2 Body: " + res2.getResponse().getContentAsString());

        // --- 3. SHOP_ADMIN of Shop A calls GET /api/v1/shops/{id} with Shop B id ---
        MvcResult res3 = mockMvc.perform(get("/api/v1/shops/" + shopBId).header("Authorization", "Bearer " + shopAdminAToken)).andReturn();
        System.out.println("TEST 3 Status: " + res3.getResponse().getStatus());
        System.out.println("TEST 3 Body: " + res3.getResponse().getContentAsString());

        // --- 4. STAFF of Shop A calls endpoints scoped to Shop B (products, reports) ---
        MvcResult res4a = mockMvc.perform(get("/api/v1/products/" + productBId).header("Authorization", "Bearer " + staffAToken)).andReturn();
        MvcResult res4b = mockMvc.perform(get("/api/v1/reports/dashboard?shopId=" + shopBId).header("Authorization", "Bearer " + staffAToken)).andReturn();
        System.out.println("TEST 4a (Product B) Status: " + res4a.getResponse().getStatus() + " Body: " + res4a.getResponse().getContentAsString());
        System.out.println("TEST 4b (Reports Shop B) Status: " + res4b.getResponse().getStatus() + " Body: " + res4b.getResponse().getContentAsString());

        // --- 5. STAFF of Shop A attempts to modify a resource by changing shopId in body ---
        MvcResult res5 = mockMvc.perform(put("/api/v1/products/" + productAId)
                .header("Authorization", "Bearer " + staffAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"shopId":"%s","name":"Moved Product","sku":"SKU-A-MOVED","unit":"pcs","purchasePrice":10,"sellingPrice":20}
                        """.formatted(shopBId))).andReturn();
        System.out.println("TEST 5 Status: " + res5.getResponse().getStatus());
        System.out.println("TEST 5 Body: " + res5.getResponse().getContentAsString());

        // --- 6. STAFF attempts PATCH on their own user record with {"role": "SHOP_ADMIN"} ---
        MvcResult res6 = mockMvc.perform(patch("/api/v1/users/me")
                .header("Authorization", "Bearer " + staffAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName":"Hacked","role":"SHOP_ADMIN","shopId":"%s"}
                        """.formatted(shopAId))).andReturn();
        System.out.println("TEST 6 Status: " + res6.getResponse().getStatus());
        System.out.println("TEST 6 Body: " + res6.getResponse().getContentAsString());

        // --- 7. STAFF attempts to call a SHOP_ADMIN-only endpoint (staff invitation approval, tax) ---
        MvcResult res7a = mockMvc.perform(get("/api/v1/staff-invitations/pending?shopId=" + shopAId)
                .header("Authorization", "Bearer " + staffAToken)).andReturn();
        MvcResult res7b = mockMvc.perform(get("/api/v1/taxes?shopId=" + shopAId)
                .header("Authorization", "Bearer " + staffAToken)).andReturn();
        System.out.println("TEST 7a (Pending invitations) Status: " + res7a.getResponse().getStatus());
        System.out.println("TEST 7b (Tax list ShopAdmin-only) Status: " + res7b.getResponse().getStatus());

        // --- 8. CUSTOMER attempts to call STAFF / SHOP_ADMIN / SUPER_ADMIN endpoint ---
        MvcResult res8a = mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + customerToken)).andReturn();
        MvcResult res8b = mockMvc.perform(get("/api/v1/products?shopId=" + shopAId).header("Authorization", "Bearer " + customerToken)).andReturn();
        MvcResult res8c = mockMvc.perform(get("/api/v1/admin/dashboard/shop-sales").header("Authorization", "Bearer " + customerToken)).andReturn();
        System.out.println("TEST 8a (Users list) Status: " + res8a.getResponse().getStatus());
        System.out.println("TEST 8b (Products list) Status: " + res8b.getResponse().getStatus());
        System.out.println("TEST 8c (Admin dashboard) Status: " + res8c.getResponse().getStatus());

        // --- 9. SHOP_ADMIN attempts to call SUPER_ADMIN-only endpoint ---
        MvcResult res9a = mockMvc.perform(get("/api/v1/admin/dashboard/shop-sales").header("Authorization", "Bearer " + shopAdminAToken)).andReturn();
        MvcResult res9b = mockMvc.perform(patch("/api/v1/users/" + staffAUserId + "/status")
                .header("Authorization", "Bearer " + shopAdminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"BLOCKED\"}")).andReturn();
        System.out.println("TEST 9a (Shop-sales) Status: " + res9a.getResponse().getStatus());
        System.out.println("TEST 9b (User status) Status: " + res9b.getResponse().getStatus());

        // --- 10. SHOP_ADMIN attempts to self-assign SUPER_ADMIN ---
        Role superRole = roleRepository.findByName("SUPER_ADMIN").orElseThrow();
        MvcResult res10 = mockMvc.perform(post("/api/v1/user-branch-roles")
                .header("Authorization", "Bearer " + shopAdminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId":"%s","shopId":"%s","roleId":"%s"}
                        """.formatted(userShopAId, shopAId, superRole.getId()))).andReturn();
        System.out.println("TEST 10 Status: " + res10.getResponse().getStatus());
        System.out.println("TEST 10 Body: " + res10.getResponse().getContentAsString());

        // --- 11. SUPER_ADMIN calls every shop-scoped endpoint (tax, purchase payment, staff invitations, invite codes) ---
        MvcResult res11Tax = mockMvc.perform(get("/api/v1/taxes?shopId=" + shopAId).header("Authorization", "Bearer " + superAdminToken)).andReturn();
        MvcResult res11Invite = mockMvc.perform(get("/api/v1/shops/" + shopAId + "/invite-code").header("Authorization", "Bearer " + superAdminToken)).andReturn();
        MvcResult res11Pending = mockMvc.perform(get("/api/v1/staff-invitations/pending?shopId=" + shopAId).header("Authorization", "Bearer " + superAdminToken)).andReturn();
        System.out.println("TEST 11 Tax Status: " + res11Tax.getResponse().getStatus());
        System.out.println("TEST 11 InviteCode Status: " + res11Invite.getResponse().getStatus());
        System.out.println("TEST 11 PendingStaffInvites Status: " + res11Pending.getResponse().getStatus());

        // --- 12. SUPER_ADMIN calls GET /api/v1/users and GET /api/v1/shops ---
        MvcResult res12Users = mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + superAdminToken)).andReturn();
        MvcResult res12Shops = mockMvc.perform(get("/api/v1/shops").header("Authorization", "Bearer " + superAdminToken)).andReturn();
        System.out.println("TEST 12 Users Status: " + res12Users.getResponse().getStatus());
        System.out.println("TEST 12 Shops Status: " + res12Shops.getResponse().getStatus());

        // --- 13. SUPER_ADMIN has zero shop grants ---
        MvcResult res13 = mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + superAdminToken)).andReturn();
        System.out.println("TEST 13 UserMe Status: " + res13.getResponse().getStatus() + " Body: " + res13.getResponse().getContentAsString());

        // --- 14. SUPER_ADMIN deactivates a currently-logged-in SHOP_ADMIN ---
        // Step a: Login user
        String sessionUserEmail = "session.user." + SEQ.get() + "@test.com";
        postJson(null, "/api/v1/auth/register", """
                {"firstName":"Session","lastName":"User","email":"%s","password":"%s"}
                """.formatted(sessionUserEmail, PASSWORD), 201);
        UUID sessionUserId = factory.findUserId(sessionUserEmail);
        factory.grantShopRole(sessionUserId, shopAId, "SHOP_ADMIN");
        MvcResult loginRes14 = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(sessionUserEmail, PASSWORD))).andReturn();
        String activeAccessToken = objectMapper.readTree(loginRes14.getResponse().getContentAsString()).path("data").path("accessToken").asText();
        String activeRefreshToken = loginRes14.getResponse().getCookie("refresh_token").getValue();

        // Step b: Deactivate user
        mockMvc.perform(patch("/api/v1/users/" + sessionUserId + "/status")
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"BLOCKED\"}")).andExpect(status().isOk());

        // 14a: Existing access token works until expiry
        MvcResult res14a = mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + activeAccessToken)).andReturn();
        System.out.println("TEST 14a (Access token mid-session) Status: " + res14a.getResponse().getStatus());

        // 14b: POST /api/v1/auth/refresh returns 401
        MvcResult res14b = mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("refresh_token", activeRefreshToken))).andReturn();
        System.out.println("TEST 14b (Refresh token after block) Status: " + res14b.getResponse().getStatus() + " Body: " + res14b.getResponse().getContentAsString());

        // 14c: POST /api/v1/auth/login returns 401
        MvcResult res14c = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(sessionUserEmail, PASSWORD))).andReturn();
        System.out.println("TEST 14c (Login after block) Status: " + res14c.getResponse().getStatus());

        // --- 15. Blacklisted refresh token cannot be reused after logout ---
        String logoutUserEmail = "logout.user." + SEQ.get() + "@test.com";
        postJson(null, "/api/v1/auth/register", """
                {"firstName":"Logout","lastName":"User","email":"%s","password":"%s"}
                """.formatted(logoutUserEmail, PASSWORD), 201);
        MvcResult loginRes15 = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(logoutUserEmail, PASSWORD))).andReturn();
        String logoutRefreshToken = loginRes15.getResponse().getCookie("refresh_token").getValue();
        mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(new jakarta.servlet.http.Cookie("refresh_token", logoutRefreshToken))).andExpect(status().isOk());
        MvcResult res15 = mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("refresh_token", logoutRefreshToken))).andReturn();
        System.out.println("TEST 15 (Refresh after logout) Status: " + res15.getResponse().getStatus() + " Body: " + res15.getResponse().getContentAsString());

        // --- 16. SHOP_ADMIN attempts POST /api/v1/shop-registrations/apply ---
        MvcResult res16 = mockMvc.perform(post("/api/v1/shop-registrations/apply")
                .header("Authorization", "Bearer " + shopAdminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"shopName":"Second Shop","panVatNumber":"999999999","panCertificateUrl":"https://example.com/pan.png","phone":"9833333333","address":"KTM"}
                        """)).andReturn();
        System.out.println("TEST 16 Status: " + res16.getResponse().getStatus());

        // --- 17. SHOP_ADMIN or SUPER_ADMIN attempts POST /api/v1/staff-invitations/join ---
        MvcResult res17a = mockMvc.perform(post("/api/v1/staff-invitations/join")
                .header("Authorization", "Bearer " + shopAdminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"inviteCode\":\"TEST12\",\"requestedRole\":\"CASHIER\"}")).andReturn();
        MvcResult res17b = mockMvc.perform(post("/api/v1/staff-invitations/join")
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"inviteCode\":\"TEST12\",\"requestedRole\":\"CASHIER\"}")).andReturn();
        System.out.println("TEST 17a (ShopAdmin join) Status: " + res17a.getResponse().getStatus());
        System.out.println("TEST 17b (SuperAdmin join) Status: " + res17b.getResponse().getStatus());

        // --- 18. STAFF attempts to view or approve staff-invitations for other shop ---
        MvcResult res18 = mockMvc.perform(get("/api/v1/staff-invitations/pending?shopId=" + shopBId)
                .header("Authorization", "Bearer " + staffAToken)).andReturn();
        System.out.println("TEST 18 Status: " + res18.getResponse().getStatus());

        // --- 19. CASHIER calls GET /api/v1/roles ---
        MvcResult res19 = mockMvc.perform(get("/api/v1/roles").header("Authorization", "Bearer " + staffAToken)).andReturn();
        System.out.println("TEST 19 Status: " + res19.getResponse().getStatus());

        // --- 20. Any non-owning role requesting a resource ID that exists but isn't theirs (user, shop, product) ---
        MvcResult res20User = mockMvc.perform(get("/api/v1/users/" + userShopBId).header("Authorization", "Bearer " + shopAdminAToken)).andReturn();
        MvcResult res20Shop = mockMvc.perform(get("/api/v1/shops/" + shopBId).header("Authorization", "Bearer " + shopAdminAToken)).andReturn();
        MvcResult res20Product = mockMvc.perform(get("/api/v1/products/" + productBId).header("Authorization", "Bearer " + shopAdminAToken)).andReturn();
        System.out.println("TEST 20 User Status: " + res20User.getResponse().getStatus());
        System.out.println("TEST 20 Shop Status: " + res20Shop.getResponse().getStatus());
        System.out.println("TEST 20 Product Status: " + res20Product.getResponse().getStatus());
        System.out.println("===============================================================================");
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private String postJson(String token, String uri, String json, int expectedStatus) throws Exception {
        var req = post(uri).contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(req)
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    private String json(String json, String path) throws Exception {
        JsonNode current = objectMapper.readTree(json);
        for (String part : path.split("\\.")) {
            current = current.path(part);
        }
        return current.asText();
    }
}
