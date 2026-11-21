package tucker;

import java.util.Comparator;

/** Builds the comparators the dashboard uses to order deliveries. */
public final class DeliverySorting {

    private DeliverySorting() {}

    /**
     * @param key        which column to sort by
     * @param descending true for Z-A / newest-first
     * Ties are broken by date and time, so equal names/addresses stay in chronological order.
     */
    public static Comparator<Delivery> comparator(SortKey key, boolean descending) {
        Comparator<Delivery> byDateTime =
                Comparator.comparing(Delivery::date).thenComparing(Delivery::time);
        Comparator<Delivery> c = switch (key) {
            case DATE -> byDateTime;
            case TIME -> Comparator.comparing(Delivery::time).thenComparing(Delivery::date);
            case NAME -> Comparator.comparing(Delivery::customerName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(byDateTime);
            case ADDRESS -> Comparator.comparing(Delivery::address, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(byDateTime);
        };
        return descending ? c.reversed() : c;
    }
}
