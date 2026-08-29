package com.smartshop.support;

import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.branch.repository.BranchRepository;
import com.smartshop.features.role.entity.Role;
import com.smartshop.features.role.repository.RoleRepository;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.features.userBranchRole.entity.UserBranchRole;
import com.smartshop.features.userBranchRole.repository.UserBranchRoleRepository;
import com.smartshop.shared.constant.AppConstants;
import com.smartshop.shared.enumeration.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class TestDataFactory {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserBranchRoleRepository userBranchRoleRepository;
    private final ShopRepository shopRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    public TestDataFactory(UserRepository userRepository,
                           RoleRepository roleRepository,
                           UserBranchRoleRepository userBranchRoleRepository,
                           ShopRepository shopRepository,
                           BranchRepository branchRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userBranchRoleRepository = userBranchRoleRepository;
        this.shopRepository = shopRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(String email, String password) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    @Transactional
    public User createSuperAdmin(String email, String password) {
        User user = createUser(email, password);
        grantGlobalRole(user.getId(), AppConstants.ROLE_SUPER_ADMIN);
        return user;
    }

    @Transactional
    public UUID findUserId(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
    }

    @Transactional
    public UserBranchRole grantGlobalRole(UUID userId, String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not seeded: " + roleName));
        User user = userRepository.findById(userId).orElseThrow();
        UserBranchRole assignment = new UserBranchRole();
        assignment.setUser(user);
        assignment.setShop(null);
        assignment.setBranch(null);
        assignment.setRole(role);
        assignment.setIsActive(true);
        return userBranchRoleRepository.save(assignment);
    }

    @Transactional
    public UserBranchRole grantShopRole(UUID userId, UUID shopId, String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not seeded: " + roleName));
        User user = userRepository.findById(userId).orElseThrow();
        Shop shop = shopRepository.findById(shopId).orElseThrow();
        UserBranchRole assignment = new UserBranchRole();
        assignment.setUser(user);
        assignment.setShop(shop);
        assignment.setBranch(null);
        assignment.setRole(role);
        assignment.setIsActive(true);
        return userBranchRoleRepository.save(assignment);
    }

    @Transactional
    public UserBranchRole grantBranchRole(UUID userId, UUID branchId, String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not seeded: " + roleName));
        User user = userRepository.findById(userId).orElseThrow();
        Branch branch = branchRepository.findById(branchId).orElseThrow();
        UserBranchRole assignment = new UserBranchRole();
        assignment.setUser(user);
        assignment.setShop(branch.getShop());
        assignment.setBranch(branch);
        assignment.setRole(role);
        assignment.setIsActive(true);
        return userBranchRoleRepository.save(assignment);
    }
}