package com.restaurantqr.platform.modules.subscription.service;

import com.restaurantqr.platform.common.BadRequestException;
import com.restaurantqr.platform.common.ResourceNotFoundException;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.repository.RestaurantRepository;
import com.restaurantqr.platform.modules.subscription.entity.Subscription;
import com.restaurantqr.platform.modules.subscription.repository.SubscriptionRepository;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final RestaurantRepository restaurantRepository;
    private final com.restaurantqr.platform.modules.restaurant.service.RestaurantService restaurantService;
    private final CouponService couponService;
    private final com.restaurantqr.platform.modules.branch.repository.BranchRepository branchRepository;
    private final com.restaurantqr.platform.modules.menuitem.repository.MenuItemRepository menuItemRepository;
    private final com.restaurantqr.platform.users.repository.UserRepository userRepository;
    private final com.restaurantqr.platform.analytics.repository.ScanEventRepository scanEventRepository;

    // Plan pricing (INR / month)
    public static final BigDecimal STARTER_PRICE      = new BigDecimal("999");
    public static final BigDecimal BASIC_PRICE        = new BigDecimal("999");
    public static final BigDecimal PROFESSIONAL_PRICE = new BigDecimal("2999");
    public static final BigDecimal BUSINESS_PRICE     = new BigDecimal("5999");
    public static final BigDecimal ENTERPRISE_PRICE   = new BigDecimal("12999");

    public Optional<Subscription> getActiveSubscription(Long restaurantId) {
        restaurantService.findById(restaurantId);
        return subscriptionRepository.findActiveSubscription(restaurantId, LocalDate.now());
    }

    public List<Subscription> getHistory(Long restaurantId) {
        restaurantService.findById(restaurantId);
        return subscriptionRepository.findByRestaurantId(restaurantId);
    }

    @Transactional
    public Subscription activate(Long restaurantId, SubscriptionRequest request) {
        var restaurant = restaurantService.findById(restaurantId);

        subscriptionRepository.findActiveSubscription(restaurantId, LocalDate.now())
                .ifPresent(sub -> {
                    sub.setStatus(Subscription.Status.EXPIRED);
                    subscriptionRepository.save(sub);
                });

        int months = request.months != null ? request.months : 1;
        BigDecimal baseAmount = getPlanPrice(request.plan).multiply(BigDecimal.valueOf(months));
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (request.couponCode != null && !request.couponCode.isBlank()) {
            var validation = couponService.validateCoupon(request.couponCode, baseAmount);
            discountAmount = validation.getCalculatedDiscount();
            couponService.incrementUsage(request.couponCode);
        }

        BigDecimal taxableAmount = baseAmount.subtract(discountAmount);
        BigDecimal taxAmount = taxableAmount.multiply(new BigDecimal("0.18")).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalPaid = taxableAmount.add(taxAmount);

        String invoiceNo = "INV-" + System.currentTimeMillis();

        var subscription = Subscription.builder()
                .restaurant(restaurant)
                .plan(request.plan)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(months))
                .amountPaid(totalPaid)
                .taxAmount(taxAmount)
                .discountAmount(discountAmount)
                .couponCode(request.couponCode)
                .invoiceNumber(invoiceNo)
                .paymentId(request.paymentId != null ? request.paymentId : "PAY-" + UUID.randomUUID())
                .paymentGateway(request.paymentGateway != null ? request.paymentGateway : "RAZORPAY")
                .status(Subscription.Status.ACTIVE)
                .autoRenew(true)
                .build();

        Subscription savedSub = subscriptionRepository.save(subscription);

        restaurant.setSubscriptionPlan(mapToRestaurantPlan(request.plan));
        restaurant.setIsTrial(false);
        restaurantRepository.save(restaurant);

        log.info("Subscription activated: restaurant={} plan={} invoice={}",
                restaurantId, request.plan, invoiceNo);

        return savedSub;
    }

    @Transactional
    public void cancel(Long restaurantId) {
        restaurantService.findById(restaurantId);
        var sub = subscriptionRepository.findActiveSubscription(restaurantId, LocalDate.now())
                .orElseThrow(() -> new BadRequestException("No active subscription found"));
        sub.setStatus(Subscription.Status.CANCELLED);
        sub.setAutoRenew(false);
        subscriptionRepository.save(sub);
        log.info("Subscription cancelled for restaurant={}", restaurantId);
    }

    @Transactional
    public void setAutoRenew(Long restaurantId, boolean autoRenew) {
        restaurantService.findById(restaurantId);
        var sub = subscriptionRepository.findActiveSubscription(restaurantId, LocalDate.now())
                .orElseThrow(() -> new BadRequestException("No active subscription found"));
        sub.setAutoRenew(autoRenew);
        subscriptionRepository.save(sub);
    }

    public com.restaurantqr.platform.modules.subscription.dto.UsageMeterDto getUsageMeter(Long restaurantId) {
        var restaurant = restaurantService.findById(restaurantId);
        var plan = restaurant.getSubscriptionPlan();

        long branchesUsed = branchRepository.countByRestaurantIdAndIsDeletedFalse(restaurantId);
        long menuItemsUsed = menuItemRepository.countByRestaurantIdAndIsDeletedFalse(restaurantId);
        long staffUsed = userRepository.countByRestaurantIdAndIsDeletedFalse(restaurantId);

        long branchesLimit = switch (plan) {
            case STARTER, BASIC -> 1;
            case PROFESSIONAL -> 5;
            case BUSINESS -> 15;
            case ENTERPRISE -> -1;
        };

        long menuItemsLimit = switch (plan) {
            case STARTER, BASIC -> 100;
            default -> -1;
        };

        long staffLimit = switch (plan) {
            case STARTER, BASIC -> 2;
            case PROFESSIONAL -> 10;
            case BUSINESS -> 50;
            case ENTERPRISE -> -1;
        };

        long storageLimitMb = switch (plan) {
            case STARTER, BASIC -> 1024;
            case PROFESSIONAL -> 10240;
            case BUSINESS -> 51200;
            case ENTERPRISE -> -1;
        };

        long storageUsedMb = (menuItemsUsed * 2); // 2MB estimated per image
        long scans = scanEventRepository.countByRestaurantId(restaurantId);

        boolean trialExpired = Boolean.TRUE.equals(restaurant.getIsTrial())
                && restaurant.getTrialEndsAt() != null
                && java.time.LocalDateTime.now().isAfter(restaurant.getTrialEndsAt());

        return com.restaurantqr.platform.modules.subscription.dto.UsageMeterDto.builder()
                .restaurantId(restaurantId)
                .restaurantName(restaurant.getName())
                .currentPlan(plan)
                .isTrial(Boolean.TRUE.equals(restaurant.getIsTrial()))
                .trialEndsAt(restaurant.getTrialEndsAt())
                .trialExpired(trialExpired)
                .branchesUsed(branchesUsed)
                .branchesLimit(branchesLimit)
                .menuItemsUsed(menuItemsUsed)
                .menuItemsLimit(menuItemsLimit)
                .staffUsersUsed(staffUsed)
                .staffUsersLimit(staffLimit)
                .storageUsedMb(storageUsedMb)
                .storageLimitMb(storageLimitMb)
                .totalQrScansThisMonth(scans)
                .monthlyVisitors(scans)
                .build();
    }

    public List<com.restaurantqr.platform.modules.subscription.dto.InvoiceDto> getInvoices(Long restaurantId) {
        restaurantService.findById(restaurantId);
        return subscriptionRepository.findByRestaurantId(restaurantId).stream()
                .map(sub -> com.restaurantqr.platform.modules.subscription.dto.InvoiceDto.builder()
                        .subscriptionId(sub.getId())
                        .invoiceNumber(sub.getInvoiceNumber() != null ? sub.getInvoiceNumber() : "INV-" + sub.getId())
                        .restaurantId(restaurantId)
                        .restaurantName(sub.getRestaurant().getName())
                        .plan(sub.getPlan())
                        .startDate(sub.getStartDate())
                        .endDate(sub.getEndDate())
                        .baseAmount(sub.getAmountPaid() != null ? sub.getAmountPaid() : BigDecimal.ZERO)
                        .discountAmount(sub.getDiscountAmount() != null ? sub.getDiscountAmount() : BigDecimal.ZERO)
                        .taxAmount(sub.getTaxAmount() != null ? sub.getTaxAmount() : BigDecimal.ZERO)
                        .totalAmountPaid(sub.getAmountPaid() != null ? sub.getAmountPaid() : BigDecimal.ZERO)
                        .currency(sub.getCurrency())
                        .gstNumber(sub.getGstNumber())
                        .paymentGateway(sub.getPaymentGateway())
                        .paymentId(sub.getPaymentId())
                        .status(sub.getStatus())
                        .downloadUrl("/subscriptions/invoices/" + sub.getId() + "/download")
                        .build())
                .toList();
    }

    public PlanDetails getPlanDetails() {
        return new PlanDetails(
                new PlanInfo("STARTER", STARTER_PRICE, "1 branch", "100 menu items", "2 staff users", "1 GB Storage", "Standard Analytics"),
                new PlanInfo("PROFESSIONAL", PROFESSIONAL_PRICE, "5 branches", "Unlimited menu items", "10 staff users", "10 GB Storage", "Custom branding"),
                new PlanInfo("BUSINESS", BUSINESS_PRICE, "15 branches", "Unlimited menu items", "50 staff users", "50 GB Storage", "Advanced Analytics"),
                new PlanInfo("ENTERPRISE", ENTERPRISE_PRICE, "Unlimited branches", "Unlimited items", "Unlimited staff", "Unlimited storage", "API Access")
        );
    }

    private BigDecimal getPlanPrice(Subscription.Plan plan) {
        return switch (plan) {
            case STARTER, BASIC -> STARTER_PRICE;
            case PROFESSIONAL   -> PROFESSIONAL_PRICE;
            case BUSINESS       -> BUSINESS_PRICE;
            case ENTERPRISE     -> ENTERPRISE_PRICE;
        };
    }

    private Restaurant.SubscriptionPlan mapToRestaurantPlan(Subscription.Plan plan) {
        return switch (plan) {
            case STARTER, BASIC -> Restaurant.SubscriptionPlan.STARTER;
            case PROFESSIONAL   -> Restaurant.SubscriptionPlan.PROFESSIONAL;
            case BUSINESS       -> Restaurant.SubscriptionPlan.BUSINESS;
            case ENTERPRISE     -> Restaurant.SubscriptionPlan.ENTERPRISE;
        };
    }

    public record PlanDetails(PlanInfo starter, PlanInfo professional, PlanInfo business, PlanInfo enterprise) {}
    public record PlanInfo(String name, BigDecimal pricePerMonth, String... features) {}
}

