package com.octileinstall.controller;

import com.octileinstall.dto.PublicInquiryCreateRequest;
import com.octileinstall.dto.PublicInquiryResponse;
import com.octileinstall.exception.TooManyRequestsException;
import com.octileinstall.service.InquiryService;
import com.octileinstall.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public site's quote form (index.html) posts here directly, closing the loop that used to
 * dead-end at a placeholder Formspree endpoint - see TEST_PLAN.md §8 defect #1 and #10. Every
 * submission lands straight in the same {@code inquiries} table the admin dashboard reads,
 * with {@code status=NEW} and no {@code createdByUser}.
 *
 * <p>Unlike every other controller in this app, this endpoint is reachable by anyone on the
 * internet with no token (see SecurityConfig's explicit {@code permitAll} for this exact path
 * and method - nothing else is opened up). That changes the threat model: two defenses apply
 * here that the authenticated {@link InquiryController} doesn't need:
 * <ul>
 *   <li>a per-IP rate limit ({@link RateLimiterService}), and
 *   <li>a honeypot field ({@link PublicInquiryCreateRequest#getCompany()}) that silently
 *       drops obvious bot submissions while still returning a normal-looking 201, so a bot
 *       probing the endpoint can't tell detection from acceptance.
 * </ul>
 */
@RestController
@RequestMapping("/api/public/inquiry")
@RequiredArgsConstructor
@Slf4j
public class PublicInquiryController {

    private static final String ACK_MESSAGE =
            "Thanks! Your quote request has been received. We'll be in touch soon.";

    private final InquiryService inquiryService;
    private final RateLimiterService rateLimiter;

    @PostMapping
    public ResponseEntity<PublicInquiryResponse> submit(
            @Valid @RequestBody PublicInquiryCreateRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = clientIp(httpRequest);

        if (!rateLimiter.tryAcquire(clientIp)) {
            log.info("Public inquiry submission rate-limited (ip={})", clientIp);
            throw new TooManyRequestsException(
                    "Too many requests. Please try again later, or call/text us directly.");
        }

        if (request.getCompany() != null && !request.getCompany().isBlank()) {
            // Honeypot tripped: log it and stop, but still return the normal success shape
            // below - see the class javadoc for why.
            log.info("Public inquiry submission dropped (honeypot filled, ip={})", clientIp);
        } else {
            inquiryService.createPublic(request);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(
                PublicInquiryResponse.builder()
                        .received(true)
                        .message(ACK_MESSAGE)
                        .build());
    }

    /**
     * {@code request.getRemoteAddr()} is the direct TCP peer, which is correct as long as this
     * app is reached directly. If it's ever put behind a reverse proxy or load balancer, that
     * peer becomes the proxy itself and every caller would share one rate-limit bucket - at
     * that point, read the real client from a header the proxy sets (e.g.
     * {@code X-Forwarded-For}) instead. Not done preemptively: blindly trusting that header
     * with no proxy in front to strip a client-supplied value would let any caller pick their
     * own rate-limit key.
     */
    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
