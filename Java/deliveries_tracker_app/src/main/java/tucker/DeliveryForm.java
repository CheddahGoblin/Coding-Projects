package tucker;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** The raw values typed into the Add/Edit window, before they are validated. */
public record DeliveryForm(
        String name,
        String address,
        String phone,
        LocalDate date,
        LocalTime time,
        String paymentType,
        List<MaterialLine> materials,
        String notes) {
}
