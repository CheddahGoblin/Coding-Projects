package tucker;

import java.time.LocalDate;
import java.util.List;

/**
 * The dashboard only talks to this interface. Later, write a JDBC/SQLite/PostgreSQL
 * implementation and swap it in DashboardApp -- no GUI changes needed.
 */
public interface DeliveryRepository {

    /**
     * Every delivery that is NOT archived and is dated on or before the given date.
     * This includes overdue deliveries (past dates that aren't yet delivered AND paid).
     */
    List<Delivery> findActiveThrough(LocalDate lastDate);

    /** Every archived delivery (delivered AND paid), any date. */
    List<Delivery> findArchived();

    /** Called whenever a delivery's delivered/paid flags change. Persist it here. */
    void update(Delivery delivery);
}
