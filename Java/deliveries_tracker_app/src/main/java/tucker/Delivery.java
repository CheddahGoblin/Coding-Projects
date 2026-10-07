package tucker;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A single scheduled delivery. The delivered/paid flags are JavaFX properties so the
 * table's checkboxes can edit them directly and the dashboard can react to changes.
 */
public class Delivery {

    private final long id;
    private final LocalDate date;
    private final LocalTime time;
    private final String customerName;
    private final String address;
    private final String materials;
    private final BooleanProperty delivered = new SimpleBooleanProperty();
    private final BooleanProperty paid = new SimpleBooleanProperty();

    public Delivery(long id, LocalDate date, LocalTime time, String customerName,
                    String address, String materials, boolean delivered, boolean paid) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.customerName = customerName;
        this.address = address;
        this.materials = materials;
        this.delivered.set(delivered);
        this.paid.set(paid);
    }

    public long id() { return id; }
    public LocalDate date() { return date; }
    public LocalTime time() { return time; }
    public String customerName() { return customerName; }
    public String address() { return address; }
    public String materials() { return materials; }

    public BooleanProperty deliveredProperty() { return delivered; }
    public BooleanProperty paidProperty() { return paid; }
    public boolean isDelivered() { return delivered.get(); }
    public boolean isPaid() { return paid.get(); }

    /** A delivery is archived once it has been both delivered and paid. */
    public boolean isArchived() { return delivered.get() && paid.get(); }
}
