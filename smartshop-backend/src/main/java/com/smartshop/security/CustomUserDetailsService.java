package com.smartshop.security;

import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.features.userBranchRole.entity.UserBranchRole;
import com.smartshop.features.userBranchRole.repository.UserBranchRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserBranchRoleRepository userBranchRoleRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toPrincipal(user);
    }

    public UserPrincipal loadUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toPrincipal(user);
    }

    private UserPrincipal toPrincipal(User user) {
        List<UserBranchRole> assignments = userBranchRoleRepository.findByUserIdAndIsActiveTrue(user.getId());
        List<String> roleNames = assignments.stream()
                .map(a -> a.getRole().getName())
                .distinct()
                .toList();
        List<BranchRoleGrant> grants = assignments.stream()
                .map(a -> new BranchRoleGrant(
                        a.getBranch() == null ? null : a.getBranch().getId(),
                        a.getShop() == null ? null : a.getShop().getId(),
                        a.getRole().getName()))
                .toList();
        return UserPrincipal.of(user.getId(), user.getEmail(), user.getPasswordHash(),
                user.getStatus().name().equals("ACTIVE"), roleNames, grants);
    }
}