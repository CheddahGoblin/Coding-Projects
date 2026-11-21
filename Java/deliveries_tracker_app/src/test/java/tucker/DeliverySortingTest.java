package tucker;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for DeliverySorting (the comparators behind the dashboard's sort options). */
class DeliverySortingTest {

    private static final LocalDate D1 = LocalDate.of(2026, 10, 15);

    private static Delivery d(long id, LocalDate date, LocalTime time, String name, String address) {
        return new Delivery(id, date, time, name, address, "", "Cash",
                List.of(new MaterialLine("Sand", "1 yd", "")), "", false, false);
    }

    /** Sorts and returns the ids in the resulting order, e.g. [2, 1, 3]. */
    private static List<Long> sortedIds(List<Delivery> deliveries, SortKey key, boolean descending) {
        List<Delivery> copy = new ArrayList<>(deliveries);
        copy.sort(DeliverySorting.comparator(key, descending));
        return copy.stream().map(Delivery::id).toList();
    }

    @Test
    void byDate_ordersOldestFirst_andUsesTimeToBreakTies() {
        List<Delivery> list = List.of(
                d(1, D1.plusDays(1), LocalTime.of(8, 0), "A", "1 A St"),
                d(2, D1, LocalTime.of(14, 0), "B", "2 B St"),
                d(3, D1, LocalTime.of(9, 0), "C", "3 C St"));
        assertEquals(List.of(3L, 2L, 1L), sortedIds(list, SortKey.DATE, false));
    }

    @Test
    void byTime_ordersByTimeOfDayAcrossDifferentDates() {
        List<Delivery> list = List.of(
                d(1, D1, LocalTime.of(15, 15), "A", "1 A St"),
                d(2, D1.plusDays(3), LocalTime.of(7, 30), "B", "2 B St"),
                d(3, D1.plusDays(1), LocalTime.of(9, 15), "C", "3 C St"));
        assertEquals(List.of(2L, 3L, 1L), sortedIds(list, SortKey.TIME, false));
    }

    @Test
    void byNameAndAddress_ignoreUpperAndLowerCase() {
        List<Delivery> list = List.of(
                d(1, D1, LocalTime.NOON, "bob", "20 elm st"),
                d(2, D1, LocalTime.NOON, "Alice", "100 Oak St"),
                d(3, D1, LocalTime.NOON, "Carl", "3 Birch Ln"));
        assertEquals(List.of(2L, 1L, 3L), sortedIds(list, SortKey.NAME, false));     // Alice, bob, Carl
        assertEquals(List.of(2L, 1L, 3L), sortedIds(list, SortKey.ADDRESS, false));  // "100 Oak", "20 elm", "3 Birch"
    }

    @Test
    void sameName_fallsBackToDateOrder() {
        List<Delivery> list = List.of(
                d(1, D1.plusDays(2), LocalTime.NOON, "Smith", "1 A St"),
                d(2, D1, LocalTime.NOON, "Smith", "2 B St"));
        assertEquals(List.of(2L, 1L), sortedIds(list, SortKey.NAME, false));
    }

    @Test
    void descending_reversesTheOrder() {
        List<Delivery> list = List.of(
                d(1, D1, LocalTime.NOON, "A", "1 A St"),
                d(2, D1.plusDays(1), LocalTime.NOON, "B", "2 B St"),
                d(3, D1.plusDays(2), LocalTime.NOON, "C", "3 C St"));
        assertEquals(List.of(3L, 2L, 1L), sortedIds(list, SortKey.DATE, true));
    }

    @Test
    void emptyAndSingleItemLists_sortWithoutError() {
        assertEquals(List.of(), sortedIds(List.of(), SortKey.NAME, false));
        assertEquals(List.of(7L), sortedIds(List.of(d(7, D1, LocalTime.NOON, "A", "1 A St")), SortKey.TIME, true));
    }
}
