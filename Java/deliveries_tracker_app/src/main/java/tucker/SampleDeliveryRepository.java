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
        //  date            time                name                         address                phone           pay      amount   material                price      delivered paid
        add(t,             LocalTime.of(8, 0),  "Greenfield Builders",       "412 Oak Ridge Rd",    "612-555-0101", "Check", "6 yd",  "Top Soil",             "$32/yd",  false, false);
        notes("Gate code 4471. Dump on the driveway, not the lawn.");
        add(t,             LocalTime.of(13, 30),"Maria Alvarez",             "88 Lakeview Ct",      "612-555-0102", "Cash",  "3 yd",  "Brown Mulch",          "$35/yd",  true,  false);
        add(t.plusDays(1), LocalTime.of(9, 15), "Summit Landscaping",        "1500 Industrial Blvd","612-555-0103", "Check", "12 ton","#57 Limestone",        "$28/ton", false, false);
        add(t.plusDays(1), LocalTime.of(11, 0), "Tom Becker",                "27 Birch Lane",       "612-555-0104", "Cash",  "2 yd",  "#8 River Rock",        "$60/yd",  false, false);
        add(t.plusDays(2), LocalTime.of(7, 30), "Hillcrest HOA",             "300 Hillcrest Dr",    "612-555-0105", "Check", "20 yd", "Black Mulch",          "$34/yd",  false, false);
        add(t.plusDays(3), LocalTime.of(14, 0), "Dana Whitfield",            "9 Maple Ave",         "612-555-0106", "Cash",  "4 yd",  "Compost",              "$40/yd",  false, true);
        add(t.plusDays(4), LocalTime.of(10, 45),"Northside Nursery",         "6120 County Rd 12",   "612-555-0107", "Check", "8 yd",  "Concrete Sand",        "$22/yd",  false, false);
        add(t.plusDays(6), LocalTime.of(8, 30), "Alder & Sons Construction", "75 Prairie View Way", "612-555-0108", "Check", "15 ton","#617 Limestone",       "$24/ton", false, false);
        notes("Call 30 minutes before arriving.");
        add(t.plusDays(6), LocalTime.of(15, 15),"Karen Liu",                 "1001 Elm St",         "612-555-0109", "Cash",  "1 yd",  "Mojave Stone",         "$95/yd",  false, false);
        // overdue: delivered but not paid / paid but not delivered (shows in red)
        add(t.minusDays(3),LocalTime.of(9, 30), "Riverbend Homes",           "18 Riverbend Rd",     "612-555-0110", "Check", "7 yd",  "Top Soil",             "$32/yd",  true,  false);
        add(t.minusDays(1),LocalTime.of(15, 0), "Sandra Kowalski",           "340 Pine St",         "612-555-0111", "Cash",  "2 yd",  "Red Mulch",            "$36/yd",  false, true);
        // outside the 7-day window
        add(t.plusDays(12),LocalTime.of(9, 0),  "Future Customer",           "1 Later St",          "612-555-0112", "Cash",  "5 yd",  "Top Soil",             "$32/yd",  false, false);
        // already archived (delivered + paid)
        add(t.minusDays(2),LocalTime.of(9, 0),  "Past Customer",             "2 Earlier St",        "612-555-0113", "Cash",  "5 yd",  "Cinnamon Mulch",       "$38/yd",  true,  true);
        add(t.minusDays(5),LocalTime.of(10, 30),"Oakwood Apartments",        "50 Oakwood Cir",      "612-555-0114", "Check", "10 yd", "Garden Soil",          "$42/yd",  true,  true);
        add(t.minusDays(9),LocalTime.of(14, 0), "Bob Nguyen",                "711 Cedar St",        "612-555-0115", "Cash",  "3 yd",  "Canadian Blue Stone",  "$110/yd", true,  true);
    }

    private void add(LocalDate d, LocalTime time, String name, String addr, String phone, String pay,
                     String amount, String material, String price, boolean delivered, boolean paid) {
        data.add(new Delivery(nextId++, d, time, name, addr, phone, pay,
                List.of(new MaterialLine(material, amount, price)), "", delivered, paid));
    }

    /** Sets the notes on the delivery that was added most recently. */
    private void notes(String text) {
        Delivery d = data.get(data.size() - 1);
        d.edit(d.date(), d.time(), d.customerName(), d.address(), d.phone(), d.paymentType(), d.items(), text);
    }

    @Override
    public List<Delivery> findActiveThrough(LocalDate lastDate) {
        return data.stream()
                .filter(d -> !d.isArchived())
                .filter(d -> !d.date().isAfter(lastDate))
                .toList();
    }

    @Override
    public List<Delivery> findArchived() {
        return data.stream().filter(Delivery::isArchived).toList();
    }

    @Override
    public void add(Delivery delivery) {
        data.add(delivery);
    }

    @Override
    public void update(Delivery delivery) {
        // In-memory objects are already updated. A database version would run an UPDATE here.
    }
}
