package tucker;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for Delivery (the model: archived/overdue rules, editing, material summary). */
class DeliveryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    private static Delivery delivery(LocalDate date, boolean delivered, boolean paid) {
        return new Delivery(1, date, LocalTime.of(9, 0), "Maria Alvarez", "88 Lakeview Ct",
                "612-555-0102", "Cash", List.of(new MaterialLine("Brown Mulch", "3 yd", "$35/yd")),
                "", delivered, paid);
    }

    @Test
    void isArchived_isTrueOnlyWhenBothDeliveredAndPaid() {
        assertFalse(delivery(TODAY, false, false).isArchived());
        assertFalse(delivery(TODAY, true, false).isArchived());
        assertFalse(delivery(TODAY, false, true).isArchived());
        assertTrue(delivery(TODAY, true, true).isArchived());
    }

    @Test
    void isOverdue_boundaryAroundToday() {
        assertTrue(delivery(TODAY.minusDays(1), false, false).isOverdue(TODAY));   // yesterday
        assertFalse(delivery(TODAY, false, false).isOverdue(TODAY));               // today
        assertFalse(delivery(TODAY.plusDays(1), false, false).isOverdue(TODAY));   // tomorrow
    }

    @Test
    void isOverdue_isFalseForArchivedDeliveryEvenIfInThePast() {
        assertFalse(delivery(TODAY.minusDays(30), true, true).isOverdue(TODAY));
        assertTrue(delivery(TODAY.minusDays(30), true, false).isOverdue(TODAY));
    }

    @Test
    void edit_replacesDetailsButKeepsIdAndDeliveredPaidFlags() {
        Delivery d = delivery(TODAY, true, false);
        d.edit(TODAY.plusDays(2), LocalTime.of(14, 30), "New Name", "9 New St", "612-555-9999",
                "Check", List.of(new MaterialLine("Sand", "2 yd", "")), "Call first");

        assertEquals(TODAY.plusDays(2), d.date());
        assertEquals(LocalTime.of(14, 30), d.time());
        assertEquals("New Name", d.customerName());
        assertEquals("Check", d.paymentType());
        assertEquals("Call first", d.notes());
        assertEquals("2 yd Sand", d.materials());
        assertEquals(1, d.id());
        assertTrue(d.isDelivered());
        assertFalse(d.isPaid());
    }

    @Test
    void notes_nullBecomesEmptyString() {
        Delivery d = new Delivery(1, TODAY, LocalTime.NOON, "A", "B", "", "Cash",
                List.of(new MaterialLine("Sand", "1 yd", "")), null, false, false);
        assertEquals("", d.notes());
    }

    @Test
    void materials_joinsLinesWithCommas_andIsEmptyWithNoItems() {
        Delivery two = new Delivery(1, TODAY, LocalTime.NOON, "A", "B", "", "Cash",
                List.of(new MaterialLine("Brown Mulch", "3 yd", "$35/yd"),
                        new MaterialLine("Top Soil", "2 yd", "")), "", false, false);
        assertEquals("3 yd Brown Mulch @ $35/yd, 2 yd Top Soil", two.materials());

        Delivery none = new Delivery(2, TODAY, LocalTime.NOON, "A", "B", "", "Cash",
                List.of(), "", false, false);
        assertEquals("", none.materials());
    }

    @Test
    void items_returnedListCannotBeModified() {
        Delivery d = delivery(TODAY, false, false);
        List<MaterialLine> items = d.items();
        assertThrows(UnsupportedOperationException.class,
                () -> items.add(new MaterialLine("Sand", "1 yd", "")));
        assertEquals(1, d.items().size());
    }
}
