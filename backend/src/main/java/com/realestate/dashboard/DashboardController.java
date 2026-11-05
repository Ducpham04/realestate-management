package com.realestate.dashboard;

import com.realestate.common.api.ApiResponse;
import com.realestate.dashboard.dto.DashboardSummary;
import com.realestate.dashboard.dto.MonthlyRevenueDto;
import com.realestate.dashboard.dto.StatusCountDto;
import com.realestate.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Aggregated statistics")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Key figures for the dashboard header")
    public ApiResponse<DashboardSummary> summary(@AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(dashboardService.summary(actor));
    }

    @GetMapping("/properties-by-status")
    @Operation(summary = "Number of properties per status")
    public ApiResponse<List<StatusCountDto>> propertiesByStatus() {
        return ApiResponse.ok(dashboardService.propertiesByStatus());
    }

    @GetMapping("/revenue-by-month")
    @Operation(summary = "Closed-deal revenue per month for the last N months")
    public ApiResponse<List<MonthlyRevenueDto>> revenueByMonth(
            @RequestParam(defaultValue = "12") @Min(1) @Max(DashboardService.MAX_MONTHS) int months,
            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(dashboardService.revenueByMonth(months, actor));
    }
}
