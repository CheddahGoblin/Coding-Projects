package tucker;

import java.time.LocalDate;
import java.util.List;

/**
 * The dashboard only talks to this interface. Later, write a JDBC/SQLite/PostgreSQL
 * implementation and swap it in DashboardApp -- no GUI changes needed.
 */
public interface DeliveryRepository {

    /** Deliveries dated between from and to (inclusive) that are NOT archived. */
    List<Delivery> findActiveBetween(LocalDate from, LocalDate to);

    /** Every archived delivery (delivered AND paid), any date. */
    List<Delivery> findArchived();

    /** Called whenever a delivery's delivered/paid flags change. Persist it here. */
    void update(Delivery delivery);
}
