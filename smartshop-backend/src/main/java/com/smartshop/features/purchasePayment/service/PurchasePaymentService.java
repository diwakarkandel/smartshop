package com.smartshop.features.purchasePayment.service;

import com.smartshop.features.audit.service.AuditService;
import com.smartshop.features.purchase.entity.Purchase;
import com.smartshop.features.purchase.repository.PurchaseRepository;
import com.smartshop.features.purchasePayment.dto.PurchasePaymentRequest;
import com.smartshop.features.purchasePayment.dto.PurchasePaymentResponse;
import com.smartshop.features.purchasePayment.entity.PurchasePayment;
import com.smartshop.features.purchasePayment.repository.PurchasePaymentRepository;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.security.SecurityUtils;
import com.smartshop.security.ShopAdminGuard;
import com.smartshop.shared.enumeration.PaymentStatus;
import com.smartshop.shared.exception.BadRequestException;
import com.smartshop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchasePaymentService {

    private final PurchasePaymentRepository purchasePaymentRepository;
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final ShopAdminGuard shopAdminGuard;
    private final AuditService auditService;

    @Transactional
    public PurchasePaymentResponse recordPayment(UUID purchaseId, PurchasePaymentRequest request) {
        log.info("Recording payment of {} for purchase {}", request.getAmount(), purchaseId);
        
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", purchaseId));
                
        // Ensure user has access to the shop where the purchase was made
        shopAdminGuard.requireShopAdmin(SecurityUtils.currentUserId(), purchase.getShop().getId());

        if (purchase.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Purchase is already fully paid");
        }

        BigDecimal totalPaidSoFar = purchasePaymentRepository.getTotalPaidAmountByPurchaseId(purchaseId);
        BigDecimal remainingBalance = purchase.getTotalAmount().subtract(totalPaidSoFar);

        if (request.getAmount().compareTo(remainingBalance) > 0) {
            throw new BadRequestException(
                    "Payment amount (" + request.getAmount() + ") exceeds the remaining balance (" + remainingBalance + ")");
        }

        User currentUser = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", SecurityUtils.currentUserId()));

        PurchasePayment payment = new PurchasePayment();
        payment.setPurchase(purchase);
        payment.setPaymentDate(request.getPaymentDate());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setNotes(request.getNotes());
        payment.setCreatedBy(currentUser);

        PurchasePayment savedPayment = purchasePaymentRepository.save(payment);

        // Update parent purchase payment status
        BigDecimal newTotalPaid = totalPaidSoFar.add(savedPayment.getAmount());
        if (newTotalPaid.compareTo(purchase.getTotalAmount()) >= 0) {
            purchase.setPaymentStatus(PaymentStatus.PAID);
        } else if (newTotalPaid.compareTo(BigDecimal.ZERO) > 0) {
            purchase.setPaymentStatus(PaymentStatus.PARTIAL);
        }
        purchaseRepository.save(purchase);

        auditService.log(
                "CREATE",
                "PurchasePayment",
                savedPayment.getId().toString(),
                null,
                savedPayment.getAmount().toString()
        );

        return mapToResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<PurchasePaymentResponse> listPayments(UUID purchaseId) {
        log.info("Fetching payments for purchase {}", purchaseId);
        
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", purchaseId));
                
        shopAdminGuard.requireShopAdmin(SecurityUtils.currentUserId(), purchase.getShop().getId());

        return purchasePaymentRepository.findByPurchaseIdOrderByPaymentDateDescCreatedAtDesc(purchaseId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PurchasePaymentResponse mapToResponse(PurchasePayment payment) {
        return PurchasePaymentResponse.builder()
                .id(payment.getId())
                .purchaseId(payment.getPurchase().getId())
                .paymentDate(payment.getPaymentDate())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .referenceNumber(payment.getReferenceNumber())
                .notes(payment.getNotes())
                .createdById(payment.getCreatedBy().getId())
                .createdByName(payment.getCreatedBy().getFullName())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
