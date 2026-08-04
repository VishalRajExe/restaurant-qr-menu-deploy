package com.restaurantqr.platform.users.service;

import com.restaurantqr.platform.audit.service.AuditLogService;
import com.restaurantqr.platform.common.BadRequestException;
import com.restaurantqr.platform.common.ConflictException;
import com.restaurantqr.platform.common.ResourceNotFoundException;
import com.restaurantqr.platform.modules.branch.entity.Branch;
import com.restaurantqr.platform.modules.branch.repository.BranchRepository;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import com.restaurantqr.platform.security.JwtUserDetails;
import com.restaurantqr.platform.users.entity.StaffInvitation;
import com.restaurantqr.platform.users.entity.User;
import com.restaurantqr.platform.users.repository.StaffInvitationRepository;
import com.restaurantqr.platform.users.repository.UserRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffInvitationService {

    private final StaffInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final RestaurantService restaurantService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Data
    public static class InviteRequest {
        private String email;
        private String name;
        private User.Role role;
        private Long branchId;
    }

    @Data
    public static class AcceptInvitationRequest {
        private String token;
        private String name;
        private String password;
    }

    @Data
    @Builder
    public static class InvitationResponse {
        private Long id;
        private Long restaurantId;
        private String email;
        private User.Role role;
        private String token;
        private StaffInvitation.Status status;
        private LocalDateTime expiresAt;
        private String invitationUrl;
    }

    @Transactional
    public InvitationResponse createInvitation(Long restaurantId, InviteRequest request) {
        restaurantService.assertRestaurantAccess(restaurantId);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("User with email " + request.getEmail() + " already exists");
        }

        invitationRepository.findByRestaurantIdAndEmailAndStatus(restaurantId, request.getEmail(), StaffInvitation.Status.PENDING)
                .ifPresent(existing -> {
                    existing.setStatus(StaffInvitation.Status.EXPIRED);
                    invitationRepository.save(existing);
                });

        Restaurant restaurant = restaurantService.findById(restaurantId);
        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getBranchId()));
        }

        String creator = "SYSTEM";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtUserDetails details) {
            creator = details.getEmail();
        }

        String token = UUID.randomUUID().toString();
        StaffInvitation invitation = StaffInvitation.builder()
                .restaurant(restaurant)
                .branch(branch)
                .email(request.getEmail().toLowerCase().trim())
                .role(request.getRole() != null ? request.getRole() : User.Role.STAFF)
                .token(token)
                .status(StaffInvitation.Status.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(48))
                .createdBy(creator)
                .build();

        StaffInvitation saved = invitationRepository.save(invitation);
        auditLogService.log(restaurantId, "USER_INVITED", "StaffInvitation", saved.getId(), null, saved.getEmail() + " (" + saved.getRole() + ")");

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public InvitationResponse getInvitationByToken(String token) {
        StaffInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found with token: " + token));

        if (invitation.isExpired() || invitation.getStatus() != StaffInvitation.Status.PENDING) {
            throw new BadRequestException("Invitation token is expired or invalid");
        }

        return mapToResponse(invitation);
    }

    @Transactional
    public User acceptInvitation(AcceptInvitationRequest request) {
        StaffInvitation invitation = invitationRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found with token: " + request.getToken()));


        if (invitation.isExpired() || invitation.getStatus() != StaffInvitation.Status.PENDING) {
            throw new BadRequestException("Invitation token is expired or invalid");
        }

        if (userRepository.existsByEmail(invitation.getEmail())) {
            throw new ConflictException("User with email " + invitation.getEmail() + " is already registered");
        }

        String name = (request.getName() != null && !request.getName().isBlank()) ? request.getName() : invitation.getEmail().split("@")[0];

        User user = User.builder()
                .name(name)
                .email(invitation.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(invitation.getRole())
                .status(User.Status.ACTIVE)
                .restaurant(invitation.getRestaurant())
                .build();

        User savedUser = userRepository.save(user);

        invitation.setStatus(StaffInvitation.Status.ACCEPTED);
        invitationRepository.save(invitation);

        auditLogService.log(invitation.getRestaurant().getId(), "INVITATION_ACCEPTED", "User", savedUser.getId(), null, savedUser.getEmail());

        return savedUser;
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> getPendingInvitations(Long restaurantId) {
        restaurantService.assertRestaurantAccess(restaurantId);
        return invitationRepository.findByRestaurantId(restaurantId).stream()
                .filter(i -> i.getStatus() == StaffInvitation.Status.PENDING && !i.isExpired())
                .map(this::mapToResponse)
                .toList();
    }

    private InvitationResponse mapToResponse(StaffInvitation invitation) {
        return InvitationResponse.builder()
                .id(invitation.getId())
                .restaurantId(invitation.getRestaurant().getId())
                .email(invitation.getEmail())
                .role(invitation.getRole())
                .token(invitation.getToken())
                .status(invitation.getStatus())
                .expiresAt(invitation.getExpiresAt())
                .invitationUrl("/auth/accept-invitation?token=" + invitation.getToken())
                .build();
    }
}
