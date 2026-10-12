package tucker;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A single scheduled delivery. The delivered/paid flags are JavaFX properties so the
 * table's checkboxes can edit them directly and the dashboard can react to changes.
 */
public class Delivery {

    private final long id;
    private LocalDate date;
    private LocalTime time;
    private String customerName;
    private String address;
    private String phone;
    private String paymentType; // "Cash" or "Check"
    private List<MaterialLine> items;
    private final BooleanProperty delivered = new SimpleBooleanProperty();
    private final BooleanProperty paid = new SimpleBooleanProperty();

    public Delivery(long id, LocalDate date, LocalTime time, String customerName, String address,
                    String phone, String paymentType, List<MaterialLine> items,
                    boolean delivered, boolean paid) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.customerName = customerName;
        this.address = address;
        this.phone = phone;
        this.paymentType = paymentType;
        this.items = List.copyOf(items);
        this.delivered.set(delivered);
        this.paid.set(paid);
    }

    /** Replaces the editable details (used by the Edit window). Delivered/paid are left alone. */
    public void edit(LocalDate date, LocalTime time, String customerName, String address,
                     String phone, String paymentType, List<MaterialLine> items) {
        this.date = date;
        this.time = time;
        this.customerName = customerName;
        this.address = address;
        this.phone = phone;
        this.paymentType = paymentType;
        this.items = List.copyOf(items);
    }

    public long id() { return id; }
    public LocalDate date() { return date; }
    public LocalTime time() { return time; }
    public String customerName() { return customerName; }
    public String address() { return address; }
    public String phone() { return phone; }
    public String paymentType() { return paymentType; }
    public List<MaterialLine> items() { return items; }

    /** One-line summary for the table, e.g. "3 yd Brown Mulch @ $35/yd, 2 yd Top Soil @ $30/yd". */
    public String materials() {
        return items.stream().map(MaterialLine::summary).collect(Collectors.joining(", "));
    }

    public BooleanProperty deliveredProperty() { return delivered; }
    public BooleanProperty paidProperty() { return paid; }
    public boolean isDelivered() { return delivered.get(); }
    public boolean isPaid() { return paid.get(); }

    /** A delivery is archived once it has been both delivered and paid. */
    public boolean isArchived() { return delivered.get() && paid.get(); }
}
