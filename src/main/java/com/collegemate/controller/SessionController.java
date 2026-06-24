package com.collegemate.controller;

import com.collegemate.model.AdvisorProfile;
import com.collegemate.model.ConsultationSession;
import com.collegemate.model.ConsultationSession.SessionStatus;
import com.collegemate.model.ConsultationSession.SessionType;
import com.collegemate.model.User;
import com.collegemate.model.WalletTransaction;
import com.collegemate.model.WalletTransaction.TransactionType;
import com.collegemate.repository.AdvisorProfileRepository;
import com.collegemate.repository.ConsultationSessionRepository;
import com.collegemate.repository.UserRepository;
import com.collegemate.repository.WalletTransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final ConsultationSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final AdvisorProfileRepository advisorProfileRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public SessionController(ConsultationSessionRepository sessionRepository,
                             UserRepository userRepository,
                             AdvisorProfileRepository advisorProfileRepository,
                             WalletTransactionRepository walletTransactionRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.advisorProfileRepository = advisorProfileRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    // Request a session (Seeker requests)
    @PostMapping("/request")
    public ResponseEntity<?> requestSession(
            @RequestParam String advisorId,
            @RequestParam String type,
            @RequestParam(required = false, defaultValue = "5") int selectedDurationMinutes,
            @RequestParam(required = false, defaultValue = "false") boolean isScheduled,
            @RequestParam(required = false) String scheduledTime,
            Principal principal) {
        User seeker = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<AdvisorProfile> advisorProfileOpt = advisorProfileRepository.findByUserId(advisorId);
        if (advisorProfileOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Advisor not found.");
        }
        AdvisorProfile advisorProfile = advisorProfileOpt.get();

        if (!advisorProfile.isVerified()) {
            return ResponseEntity.badRequest().body("Advisor is not verified yet.");
        }

        // Seeker must have enough balance for the selected duration
        double upfrontCost = advisorProfile.getRatePerMinute() * selectedDurationMinutes;
        if (seeker.getWalletBalance() < upfrontCost) {
            return ResponseEntity.badRequest().body("Insufficient wallet balance. Minimum balance needed: $" + String.format("%.2f", upfrontCost));
        }

        // Check if advisor is online (only for instant sessions)
        if (!isScheduled && !advisorProfile.isOnline()) {
            return ResponseEntity.badRequest().body("Advisor is offline.");
        }

        // Deduct upfront cost immediately when request is sent
        seeker.setWalletBalance(seeker.getWalletBalance() - upfrontCost);
        userRepository.save(seeker);

        ConsultationSession session = new ConsultationSession(
                seeker.getId(),
                advisorId,
                SessionType.valueOf(type.toUpperCase())
        );
        session.setSelectedDurationMinutes(selectedDurationMinutes);
        session.setScheduled(isScheduled);
        session.setUpfrontCharged(true);
        session.setAmountCharged(upfrontCost);
        if (isScheduled && scheduledTime != null) {
            session.setScheduledTime(Instant.parse(scheduledTime));
        }

        ConsultationSession saved = sessionRepository.save(session);

        // Log deduction transaction (held funds)
        WalletTransaction seekerTx = new WalletTransaction(
                seeker.getId(),
                WalletTransaction.TransactionType.DEDUCTION,
                upfrontCost,
                "Held upfront fee for " + (isScheduled ? "scheduled " : "") + session.getType() + " session request"
        );
        walletTransactionRepository.save(seekerTx);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Respond to request (Advisor accepts/rejects)
    @PostMapping("/{id}/respond")
    public ResponseEntity<?> respondToSession(@PathVariable String id, @RequestParam boolean accept, Principal principal) {
        Optional<ConsultationSession> sessionOpt = sessionRepository.findById(id);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ConsultationSession session = sessionOpt.get();

        User advisor = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!session.getAdvisorId().equals(advisor.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not the designated advisor for this session.");
        }

        if (session.getStatus() != SessionStatus.PENDING) {
            return ResponseEntity.badRequest().body("Session is not in PENDING state.");
        }

        if (accept) {
            if (session.isScheduled()) {
                session.setStatus(SessionStatus.SCHEDULED);
            } else {
                session.setStatus(SessionStatus.ACTIVE);
                session.setStartTime(Instant.now());

                // Credit advisor immediately for instant session (seeker was charged on request)
                double upfrontCost = session.getAmountCharged();
                advisor.setWalletBalance(advisor.getWalletBalance() + upfrontCost);
                userRepository.save(advisor);

                // Log earning transaction for advisor
                WalletTransaction advisorTx = new WalletTransaction(
                        advisor.getId(),
                        WalletTransaction.TransactionType.EARNING,
                        upfrontCost,
                        "Upfront earning for accepted " + session.getType() + " session (" + session.getSelectedDurationMinutes() + " mins)"
                );
                walletTransactionRepository.save(advisorTx);
            }
        } else {
            session.setStatus(SessionStatus.REJECTED);
            session.setEndTime(Instant.now());

            // Refund seeker (advisor rejected)
            User seeker = userRepository.findById(session.getSeekerId())
                    .orElseThrow(() -> new RuntimeException("Seeker user not found"));
            double refundAmount = session.getAmountCharged();
            seeker.setWalletBalance(seeker.getWalletBalance() + refundAmount);
            userRepository.save(seeker);

            session.setAmountCharged(0.0);
            session.setUpfrontCharged(false);

            // Log refund transaction
            WalletTransaction refundTx = new WalletTransaction(
                    seeker.getId(),
                    WalletTransaction.TransactionType.EARNING,
                    refundAmount,
                    "Refund for rejected " + session.getType() + " session request"
            );
            walletTransactionRepository.save(refundTx);
        }

        ConsultationSession saved = sessionRepository.save(session);
        return ResponseEntity.ok(saved);
    }

    // Start a scheduled session (transition to ACTIVE and credit advisor)
    @PostMapping("/{id}/start")
    public ResponseEntity<?> startSession(@PathVariable String id, Principal principal) {
        Optional<ConsultationSession> sessionOpt = sessionRepository.findById(id);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ConsultationSession session = sessionOpt.get();

        if (session.getStatus() == SessionStatus.ACTIVE) {
            return ResponseEntity.ok(session);
        }

        if (session.getStatus() != SessionStatus.SCHEDULED) {
            return ResponseEntity.badRequest().body("Session is not in SCHEDULED state.");
        }

        User advisor = userRepository.findById(session.getAdvisorId())
                .orElseThrow(() -> new RuntimeException("Advisor user not found"));

        // Credit advisor (seeker was already charged on request)
        double upfrontCost = session.getAmountCharged();
        advisor.setWalletBalance(advisor.getWalletBalance() + upfrontCost);
        userRepository.save(advisor);

        session.setStatus(SessionStatus.ACTIVE);
        session.setStartTime(Instant.now());
        sessionRepository.save(session);

        // Log advisor earning
        WalletTransaction advisorTx = new WalletTransaction(
                advisor.getId(),
                WalletTransaction.TransactionType.EARNING,
                upfrontCost,
                "Upfront earning for scheduled " + session.getType() + " session (" + session.getSelectedDurationMinutes() + " mins)"
        );
        walletTransactionRepository.save(advisorTx);

        return ResponseEntity.ok(session);
    }

    // Cancel a session request (Seeker cancels, refunds seeker)
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelSession(@PathVariable String id, Principal principal) {
        Optional<ConsultationSession> sessionOpt = sessionRepository.findById(id);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ConsultationSession session = sessionOpt.get();

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!session.getSeekerId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only the seeker can cancel this request.");
        }

        if (session.getStatus() != SessionStatus.PENDING && session.getStatus() != SessionStatus.SCHEDULED) {
            return ResponseEntity.badRequest().body("Session cannot be canceled in its current state.");
        }

        // Refund seeker
        double refundAmount = session.getAmountCharged();
        user.setWalletBalance(user.getWalletBalance() + refundAmount);
        userRepository.save(user);

        session.setStatus(SessionStatus.CANCELLED);
        session.setAmountCharged(0.0);
        session.setUpfrontCharged(false);
        session.setEndTime(Instant.now());
        sessionRepository.save(session);

        // Log transaction
        WalletTransaction refundTx = new WalletTransaction(
                user.getId(),
                WalletTransaction.TransactionType.EARNING,
                refundAmount,
                "Refund for canceled " + session.getType() + " session request"
        );
        walletTransactionRepository.save(refundTx);

        return ResponseEntity.ok(session);
    }

    // Advisor decision to continue or cut the session when scheduled time ends
    @PostMapping("/{id}/advisor-decision")
    public ResponseEntity<?> submitAdvisorDecision(@PathVariable String id, @RequestParam boolean continueSession) {
        Optional<ConsultationSession> sessionOpt = sessionRepository.findById(id);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ConsultationSession session = sessionOpt.get();

        if (session.getStatus() != SessionStatus.ACTIVE) {
            return ResponseEntity.badRequest().body("Session is not active.");
        }

        if (continueSession) {
            session.setExtensionActive(true);
            sessionRepository.save(session);
        } else {
            session.setStatus(SessionStatus.COMPLETED);
            session.setEndTime(Instant.now());
            sessionRepository.save(session);
        }

        return ResponseEntity.ok(session);
    }

    // Heartbeat for pay-per-minute billing (client sends this every 30 seconds)
    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<?> sessionHeartbeat(@PathVariable String id, Principal principal) {
        Optional<ConsultationSession> sessionOpt = sessionRepository.findById(id);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ConsultationSession session = sessionOpt.get();

        if (session.getStatus() != SessionStatus.ACTIVE) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "inactive");
            response.put("session", session);
            return ResponseEntity.ok(response);
        }

        // Fetch Seeker and Advisor
        User seeker = userRepository.findById(session.getSeekerId())
                .orElseThrow(() -> new RuntimeException("Seeker user not found"));
        User advisor = userRepository.findById(session.getAdvisorId())
                .orElseThrow(() -> new RuntimeException("Advisor user not found"));

        AdvisorProfile advisorProfile = advisorProfileRepository.findByUserId(advisor.getId())
                .orElseThrow(() -> new RuntimeException("Advisor profile not found"));

        long elapsedSeconds = Duration.between(session.getStartTime(), Instant.now()).toSeconds();
        session.setDurationSeconds(elapsedSeconds);
        sessionRepository.save(session);

        long scheduledSeconds = (long) session.getSelectedDurationMinutes() * 60;
        Map<String, Object> response = new HashMap<>();

        if (elapsedSeconds < scheduledSeconds) {
            // Within scheduled duration. Already paid upfront. No deduction.
            response.put("status", "active");
            response.put("session", session);
            response.put("walletBalance", seeker.getWalletBalance());
        } else {
            // Duration exceeded!
            if (!session.isExtensionActive()) {
                // Scheduled time is over, but advisor hasn't chosen to continue yet.
                response.put("status", "duration_exceeded");
                response.put("session", session);
                response.put("walletBalance", seeker.getWalletBalance());
            } else {
                // Extension is active. Charge pay-per-minute (pro-rated every 30s)
                double chargeRate = advisorProfile.getRatePerMinute() / 2.0;

                if (seeker.getWalletBalance() >= chargeRate) {
                    seeker.setWalletBalance(seeker.getWalletBalance() - chargeRate);
                    userRepository.save(seeker);

                    advisor.setWalletBalance(advisor.getWalletBalance() + chargeRate);
                    userRepository.save(advisor);

                    session.setAmountCharged(session.getAmountCharged() + chargeRate);
                    sessionRepository.save(session);

                    // Log transactions
                    WalletTransaction seekerTx = new WalletTransaction(
                            seeker.getId(),
                            TransactionType.DEDUCTION,
                            chargeRate,
                            "Charged for extended " + session.getType() + " consultation session (30s)"
                    );
                    walletTransactionRepository.save(seekerTx);

                    WalletTransaction advisorTx = new WalletTransaction(
                            advisor.getId(),
                            TransactionType.EARNING,
                            chargeRate,
                            "Earned from extended " + session.getType() + " consultation session (30s)"
                    );
                    walletTransactionRepository.save(advisorTx);

                    response.put("status", "active");
                    response.put("session", session);
                    response.put("walletBalance", seeker.getWalletBalance());
                } else {
                    // Out of funds! Complete session
                    session.setStatus(SessionStatus.COMPLETED);
                    session.setEndTime(Instant.now());
                    sessionRepository.save(session);

                    response.put("status", "completed");
                    response.put("reason", "insufficient_funds");
                    response.put("session", session);
                }
            }
        }

        return ResponseEntity.ok(response);
    }

    // End session explicitly
    @PostMapping("/{id}/end")
    public ResponseEntity<?> endSession(@PathVariable String id) {
        Optional<ConsultationSession> sessionOpt = sessionRepository.findById(id);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ConsultationSession session = sessionOpt.get();

        if (session.getStatus() == SessionStatus.ACTIVE) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setEndTime(Instant.now());
            if (session.getStartTime() != null) {
                session.setDurationSeconds(Duration.between(session.getStartTime(), session.getEndTime()).toSeconds());
            }
            sessionRepository.save(session);
        }

        return ResponseEntity.ok(session);
    }

    // Get active session for user
    @GetMapping("/active")
    public ResponseEntity<?> getActiveSession(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ConsultationSession> activeSessions = sessionRepository.findAll().stream()
                .filter(s -> (s.getSeekerId().equals(user.getId()) || s.getAdvisorId().equals(user.getId()))
                        && (s.getStatus() == SessionStatus.ACTIVE || s.getStatus() == SessionStatus.PENDING))
                .toList();

        if (activeSessions.isEmpty()) {
            return ResponseEntity.ok().body(Map.of("hasActive", false));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("hasActive", true);
        result.put("session", activeSessions.get(0));
        return ResponseEntity.ok(result);
    }

    // Get scheduled sessions for user
    @GetMapping("/scheduled")
    public List<ConsultationSession> getScheduledSessions(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return sessionRepository.findAll().stream()
                .filter(s -> (s.getSeekerId().equals(user.getId()) || s.getAdvisorId().equals(user.getId()))
                        && s.getStatus() == SessionStatus.SCHEDULED)
                .toList();
    }

    // Get pending requests for advisor
    @GetMapping("/pending")
    public List<ConsultationSession> getPendingRequests(Principal principal) {
        User advisor = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return sessionRepository.findByAdvisorId(advisor.getId()).stream()
                .filter(s -> s.getStatus() == SessionStatus.PENDING)
                .toList();
    }

    // Get session history
    @GetMapping("/history")
    public List<ConsultationSession> getSessionHistory(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == User.Role.ADVISOR) {
            return sessionRepository.findByAdvisorId(user.getId());
        } else {
            return sessionRepository.findBySeekerId(user.getId());
        }
    }
}
