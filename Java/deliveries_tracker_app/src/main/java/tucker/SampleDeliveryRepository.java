package tucker;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Temporary in-memory stand-in with fake data so we can design the GUI. */
public class SampleDeliveryRepository implements DeliveryRepository {

    private final List<Delivery> data = new ArrayList<>();
    private long nextId = 1;

    public SampleDeliveryRepository() {
        LocalDate t = LocalDate.now();
        //  date            time               name                        address                   materials                delivered paid
        add(t,             LocalTime.of(8, 0),  "Greenfield Builders",      "412 Oak Ridge Rd",       "6 yd Topsoil",           false, false);
        add(t,             LocalTime.of(13, 30),"Maria Alvarez",            "88 Lakeview Ct",         "3 yd Cedar Mulch",       true,  false);
        add(t.plusDays(1), LocalTime.of(9, 15), "Summit Landscaping",       "1500 Industrial Blvd",   "12 ton Crushed Granite", false, false);
        add(t.plusDays(1), LocalTime.of(11, 0), "Tom Becker",               "27 Birch Lane",          "2 yd Pea Gravel",        false, false);
        add(t.plusDays(2), LocalTime.of(7, 30), "Hillcrest HOA",            "300 Hillcrest Dr",       "20 yd Black Mulch",      false, false);
        add(t.plusDays(3), LocalTime.of(14, 0), "Dana Whitfield",           "9 Maple Ave",            "4 yd Compost Blend",     false, true);
        add(t.plusDays(4), LocalTime.of(10, 45),"Northside Nursery",        "6120 County Rd 12",      "8 yd Sand",              false, false);
        add(t.plusDays(6), LocalTime.of(8, 30), "Alder & Sons Construction","75 Prairie View Way",    "15 ton Road Base",       false, false);
        add(t.plusDays(6), LocalTime.of(15, 15),"Karen Liu",                "1001 Elm St",            "1 yd River Rock",        false, false);
        // outside the 7-day window
        add(t.plusDays(12),LocalTime.of(9, 0),  "Future Customer",          "1 Later St",             "5 yd Topsoil",           false, false);
        // already archived (delivered + paid)
        add(t.minusDays(2),LocalTime.of(9, 0),  "Past Customer",            "2 Earlier St",           "5 yd Mulch",             true,  true);
        add(t.minusDays(5),LocalTime.of(10, 30),"Oakwood Apartments",       "50 Oakwood Cir",         "10 yd Topsoil",          true,  true);
        add(t.minusDays(9),LocalTime.of(14, 0), "Bob Nguyen",               "711 Cedar St",           "3 yd Gravel",            true,  true);
    }

    private void add(LocalDate d, LocalTime t, String name, String addr, String mat, boolean delivered, boolean paid) {
        data.add(new Delivery(nextId++, d, t, name, addr, mat, delivered, paid));
    }

    @Override
    public List<Delivery> findActiveBetween(LocalDate from, LocalDate to) {
        return data.stream()
                .filter(d -> !d.isArchived())
                .filter(d -> !d.date().isBefore(from) && !d.date().isAfter(to))
                .toList();
    }

    @Override
    public List<Delivery> findArchived() {
        return data.stream().filter(Delivery::isArchived).toList();
    }

    @Override
    public void update(Delivery delivery) {
        // In-memory objects are already updated. A database version would run an UPDATE here.
    }
}
