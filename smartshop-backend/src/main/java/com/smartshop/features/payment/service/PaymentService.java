package com.smartshop.features.payment.service;

import com.smartshop.features.payment.dto.PaymentRequest;
import com.smartshop.features.payment.dto.PaymentResponse;
import com.smartshop.features.payment.entity.Payment;
import com.smartshop.features.payment.gateway.PaymentGateway;
import com.smartshop.features.payment.gateway.PaymentResult;
import com.smartshop.features.payment.repository.PaymentRepository;
import com.smartshop.features.sale.entity.Sale;
import com.smartshop.features.sale.repository.SaleRepository;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.security.BranchScopeGuard;
import com.smartshop.security.SecurityUtils;
import com.smartshop.shared.enumeration.PaymentMethod;
import com.smartshop.shared.enumeration.PaymentState;
import com.smartshop.shared.enumeration.PaymentStatus;
import com.smartshop.shared.exception.BadRequestException;
import com.smartshop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final BranchScopeGuard branchScopeGuard;
    private final PaymentGateway paymentGateway;

    @Transactional
    public PaymentResponse recordPayment(PaymentRequest request) {
        log.info("Recording payment of amount {} via {} for sale {}",
                request.getAmount(), request.getPaymentMethod(), request.getSaleId());
        Sale sale = saleRepository.findById(request.getSaleId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale", request.getSaleId()));
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), sale.getShop().getId());

        if (request.getPaymentMethod() == PaymentMethod.CASH) {
            return processCash(sale, request.getAmount(), request.getReferenceNumber());
        }
        PaymentResult result = paymentGateway.process(request.getPaymentMethod(),
                request.getAmount(), request.getReferenceNumber());
        if (!result.isSuccess()) {
            log.warn("Payment gateway processing failed: {}", result.getMessage());
            throw new BadRequestException("Payment failed: " + result.getMessage());
        }
        User receiver = currentUser();
        Payment payment = new Payment();
        payment.setSale(sale);
        payment.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentStatus(PaymentState.COMPLETED);
        payment.setReferenceNumber(result.getReferenceNumber());
        payment.setPaidAt(LocalDateTime.now());
        payment.setReceivedBy(receiver);
        Payment saved = paymentRepository.save(payment);
        refreshSalePaymentStatus(sale);
        log.info("Payment of {} recorded successfully with id: {}", saved.getAmount(), saved.getId());
        return toResponse(saved);
    }

    private PaymentResponse processCash(Sale sale, BigDecimal amount, String referenceNumber) {
        User receiver = currentUser();
        Payment payment = new Payment();
        payment.setSale(sale);
        payment.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setPaymentStatus(PaymentState.COMPLETED);
        payment.setReferenceNumber(referenceNumber);
        payment.setPaidAt(LocalDateTime.now());
        payment.setReceivedBy(receiver);
        Payment saved = paymentRepository.save(payment);
        refreshSalePaymentStatus(sale);
        log.info("Cash payment of {} recorded successfully with id: {}", saved.getAmount(), saved.getId());
        return toResponse(saved);
    }

    public void refreshSalePaymentStatus(Sale sale) {
        List<Payment> payments = paymentRepository.findBySaleId(sale.getId());
        BigDecimal paid = payments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentState.COMPLETED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paid.compareTo(sale.getTotalAmount()) >= 0) {
            sale.setPaymentStatus(PaymentStatus.PAID);
        } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
            sale.setPaymentStatus(PaymentStatus.PARTIAL);
        }
        saleRepository.save(sale);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listBySale(UUID saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        branchScopeGuard.requireShopAccess(SecurityUtils.currentUserId(), sale.getShop().getId());
        return paymentRepository.findBySaleId(saleId).stream().map(this::toResponse).toList();
    }

    private User currentUser() {
        return userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", SecurityUtils.currentUserId()));
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .saleId(payment.getSale().getId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .referenceNumber(payment.getReferenceNumber())
                .paidAt(payment.getPaidAt())
                .receivedById(payment.getReceivedBy().getId())
                .receivedByName(payment.getReceivedBy().getFullName())
                .build();
    }
}