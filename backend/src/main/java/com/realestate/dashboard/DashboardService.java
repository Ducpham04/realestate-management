package com.realestate.dashboard;

import com.realestate.appointment.AppointmentRepository;
import com.realestate.customer.CustomerRepository;
import com.realestate.dashboard.dto.DashboardSummary;
import com.realestate.dashboard.dto.MonthlyRevenueDto;
import com.realestate.dashboard.dto.StatusCountDto;
import com.realestate.deal.DealRepository;
import com.realestate.deal.DealRepository.MonthlyRevenueRow;
import com.realestate.deal.DealStatus;
import com.realestate.property.PropertyRepository;
import com.realestate.property.PropertyRepository.StatusCount;
import com.realestate.property.PropertyStatus;
import com.realestate.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregated statistics. Property figures are agency-wide; customer, appointment and deal
 * figures are scoped to the current agent unless the caller is an admin.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    public static final int MAX_MONTHS = 36;
    private static final Duration UPCOMING_WINDOW = Duration.ofDays(7);

    private final PropertyRepository propertyRepository;
    private final CustomerRepository customerRepository;
    private final AppointmentRepository appointmentRepository;
    private final DealRepository dealRepository;
    private final Clock clock;

    public DashboardSummary summary(UserPrincipal actor) {
        Long agentId = scope(actor);
        Instant now = clock.instant();
        List<StatusCountDto> byStatus = propertiesByStatus();
        long total = byStatus.stream().mapToLong(StatusCountDto::count).sum();
        long available = byStatus.stream()
                .filter(s -> s.status() == PropertyStatus.AVAILABLE)
                .mapToLong(StatusCountDto::count)
                .sum();

        YearMonth current = YearMonth.now(clock);
        List<MonthlyRevenueRow> thisMonth = dealRepository.revenueByMonth(
                startOf(current), startOf(current.plusMonths(1)), agentId);
        long closedThisMonth = thisMonth.stream().mapToLong(MonthlyRevenueRow::getDeals).sum();
        BigDecimal revenue = thisMonth.stream().map(MonthlyRevenueRow::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commission = thisMonth.stream().map(MonthlyRevenueRow::getCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardSummary(
                total,
                available,
                agentId == null ? customerRepository.count() : customerRepository.countByAgentId(agentId),
                appointmentRepository.countScheduledBetween(agentId, now, now.plus(UPCOMING_WINDOW)),
                dealRepository.countByStatuses(agentId, DealStatus.ACTIVE),
                closedThisMonth,
                revenue,
                commission);
    }

    /** Count of properties for every status, including statuses with no properties. */
    public List<StatusCountDto> propertiesByStatus() {
        Map<PropertyStatus, Long> counts = propertyRepository.countByStatus().stream()
                .collect(Collectors.toMap(StatusCount::getStatus, StatusCount::getCount));
        return Arrays.stream(PropertyStatus.values())
                .map(s -> new StatusCountDto(s, counts.getOrDefault(s, 0L)))
                .toList();
    }

    /** Revenue for the last {@code months} calendar months (including the current one), zero-filled. */
    public List<MonthlyRevenueDto> revenueByMonth(int months, UserPrincipal actor) {
        int span = Math.clamp(months, 1, MAX_MONTHS);
        YearMonth end = YearMonth.now(clock);
        YearMonth start = end.minusMonths(span - 1L);

        Map<String, MonthlyRevenueRow> rows = dealRepository
                .revenueByMonth(startOf(start), startOf(end.plusMonths(1)), scope(actor)).stream()
                .collect(Collectors.toMap(MonthlyRevenueRow::getPeriod, Function.identity()));

        List<MonthlyRevenueDto> result = new ArrayList<>(span);
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            String key = ym.toString();
            MonthlyRevenueRow row = rows.get(key);
            result.add(row == null
                    ? new MonthlyRevenueDto(key, 0, BigDecimal.ZERO, BigDecimal.ZERO)
                    : new MonthlyRevenueDto(key, row.getDeals(), row.getRevenue(), row.getCommission()));
        }
        return result;
    }

    private static Long scope(UserPrincipal actor) {
        return actor.isAdmin() ? null : actor.getId();
    }

    private static Instant startOf(YearMonth month) {
        return month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
