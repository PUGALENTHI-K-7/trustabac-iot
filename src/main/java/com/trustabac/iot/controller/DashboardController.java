package com.trustabac.iot.controller;

import com.trustabac.iot.dto.BookingResponse;
import com.trustabac.iot.entity.BookingStatus;
import com.trustabac.iot.service.BookingService;
import com.trustabac.iot.simulator.SimulatorConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring MVC controller rendering the real-time smart-rental monitoring dashboard.
 * Purely observational presentation controller with no embedded authorization or business logic.
 */
@Controller
public class DashboardController {

    private final BookingService bookingService;
    private final SimulatorConfiguration simulatorConfig;

    public DashboardController(BookingService bookingService, SimulatorConfiguration simulatorConfig) {
        this.bookingService = bookingService;
        this.simulatorConfig = simulatorConfig;
    }

    @GetMapping("/dashboard")
    public String renderDashboard(Model model) {
        model.addAttribute("appName", "TrustABAC-IoT");
        model.addAttribute("appSubtitle", "Adaptive Trust- and Risk-Aware Smart-Contract Access Control for Resource-Constrained IoT Networks");

        List<BookingResponse> bookings = bookingService.getAllBookings();
        BookingResponse activeBooking = bookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.ACTIVE)
                .findFirst()
                .or(() -> bookings.stream().findFirst())
                .orElse(null);

        String propertyId = activeBooking != null ? activeBooking.getPropertyId() : simulatorConfig.getPropertyId();
        String guestUserId = activeBooking != null ? activeBooking.getGuestUserId() : simulatorConfig.getGuestUserId();
        String bookingId = activeBooking != null ? activeBooking.getBookingReference() : simulatorConfig.getBookingId();

        model.addAttribute("propertyId", propertyId);
        model.addAttribute("guestUserId", guestUserId);
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("serverTime", LocalDateTime.now().toString());
        return "dashboard";
    }

    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/dashboard";
    }
}
