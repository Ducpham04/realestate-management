package com.realestate.unit;

import static com.realestate.support.TestData.AGENT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.realestate.appointment.AppointmentRepository;
import com.realestate.customer.CustomerRepository;
import com.realestate.dashboard.DashboardService;
import com.realestate.dashboard.dto.MonthlyRevenueDto;
import com.realestate.deal.DealRepository;
import com.realestate.deal.DealRepository.MonthlyRevenueRow;
import com.realestate.deal.DealStatus;
import com.realestate.property.PropertyRepository;
import com.realestate.property.PropertyRepository.StatusCount;
import com.realestate.property.PropertyStatus;
import com.realestate.support.TestData;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-15T08:00:00Z");

    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private DealRepository dealRepository;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(propertyRepository, customerRepository, appointmentRepository,
                dealRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static StatusCount count(PropertyStatus status, long count) {
        return new StatusCount() {
            public PropertyStatus getStatus() {
                return status;
            }

            public long getCount() {
                return count;
            }
        };
    }

    private static MonthlyRevenueRow row(String period, long deals, String revenue, String commission) {
        return new MonthlyRevenueRow() {
            public String getPeriod() {
                return period;
            }

            public long getDeals() {
                return deals;
            }

            public BigDecimal getRevenue() {
                return new BigDecimal(revenue);
            }

            public BigDecimal getCommission() {
                return new BigDecimal(commission);
            }
        };
    }

    @Test
    void propertiesByStatusIncludesEveryStatus() {
        when(propertyRepository.countByStatus()).thenReturn(List.of(count(PropertyStatus.SOLD, 4)));

        var result = service.propertiesByStatus();

        assertThat(result).hasSize(PropertyStatus.values().length);
        assertThat(result).filteredOn(s -> s.status() == PropertyStatus.SOLD).singleElement()
                .extracting(s -> s.count()).isEqualTo(4L);
        assertThat(result).filteredOn(s -> s.status() != PropertyStatus.SOLD).allMatch(s -> s.count() == 0);
    }

    @Test
    void revenueByMonthZeroFillsMissingMonthsInOrder() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-07-01T00:00:00Z");
        when(dealRepository.revenueByMonth(from, to, null)).thenReturn(List.of(
                row("2026-02", 2, "3000", "60"),
                row("2026-06", 1, "1000", "20")));

        List<MonthlyRevenueDto> result = service.revenueByMonth(6, TestData.admin());

        assertThat(result).extracting(MonthlyRevenueDto::month)
                .containsExactly("2026-01", "2026-02", "2026-03", "2026-04", "2026-05", "2026-06");
        assertThat(result.get(0).revenue()).isEqualByComparingTo("0");
        assertThat(result.get(1).deals()).isEqualTo(2);
        assertThat(result.get(5).commission()).isEqualByComparingTo("20");
    }

    @Test
    void revenueIsScopedForAgentsAndMonthsAreClamped() {
        when(dealRepository.revenueByMonth(any(), any(), eq(AGENT_ID))).thenReturn(List.of());

        assertThat(service.revenueByMonth(500, TestData.agent())).hasSize(DashboardService.MAX_MONTHS);
        assertThat(service.revenueByMonth(0, TestData.agent())).hasSize(1);
    }

    @Test
    void summaryAggregatesScopedFigures() {
        when(propertyRepository.countByStatus()).thenReturn(List.of(
                count(PropertyStatus.AVAILABLE, 3), count(PropertyStatus.SOLD, 2)));
        when(dealRepository.revenueByMonth(Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-07-01T00:00:00Z"), AGENT_ID))
                .thenReturn(List.of(row("2026-06", 2, "500", "10")));
        when(customerRepository.countByAgentId(AGENT_ID)).thenReturn(7L);
        when(appointmentRepository.countScheduledBetween(eq(AGENT_ID), eq(NOW), any())).thenReturn(4L);
        when(dealRepository.countByStatuses(AGENT_ID, DealStatus.ACTIVE)).thenReturn(1L);

        var summary = service.summary(TestData.agent());

        assertThat(summary.totalProperties()).isEqualTo(5);
        assertThat(summary.availableProperties()).isEqualTo(3);
        assertThat(summary.totalCustomers()).isEqualTo(7);
        assertThat(summary.upcomingAppointments()).isEqualTo(4);
        assertThat(summary.activeDeals()).isEqualTo(1);
        assertThat(summary.closedDealsThisMonth()).isEqualTo(2);
        assertThat(summary.revenueThisMonth()).isEqualByComparingTo("500");
        assertThat(summary.commissionThisMonth()).isEqualByComparingTo("10");
        verify(customerRepository, never()).count();
    }

    @Test
    void adminSummaryIsUnscoped() {
        when(propertyRepository.countByStatus()).thenReturn(List.of());
        when(dealRepository.revenueByMonth(any(), any(), isNull())).thenReturn(List.of());
        when(customerRepository.count()).thenReturn(10L);

        var summary = service.summary(TestData.admin());

        assertThat(summary.totalCustomers()).isEqualTo(10);
        assertThat(summary.revenueThisMonth()).isEqualByComparingTo("0");
    }
}
