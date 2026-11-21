package tucker;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Connects the Add/Edit window to storage: validates the form, then saves it through the
 * repository. Database errors are reported in the result instead of crashing the app.
 */
public class DeliveryService {

    private final DeliveryRepository repository;

    public DeliveryService(DeliveryRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    /** Validates the form and, if it's valid, saves a new delivery (not delivered, not paid). */
    public SaveResult create(DeliveryForm form) {
        List<String> errors = DeliveryValidator.validate(form);
        if (!errors.isEmpty()) return SaveResult.failed(errors);

        Delivery delivery = new Delivery(0, form.date(), form.time(), form.name().trim(),
                form.address().trim(), orEmpty(form.phone()), form.paymentType().trim(),
                form.materials(), orEmpty(form.notes()), false, false);
        try {
            repository.add(delivery);
        } catch (RuntimeException e) {
            return SaveResult.failed(List.of("Could not save the delivery: " + e.getMessage()));
        }
        return SaveResult.ok(delivery);
    }

    /**
     * Validates the form and, if it's valid, updates the existing delivery. Delivered/paid are
     * left alone. If saving fails, the delivery in memory is put back the way it was.
     */
    public SaveResult edit(Delivery existing, DeliveryForm form) {
        if (existing == null) return SaveResult.failed(List.of("No delivery was selected to edit."));
        List<String> errors = DeliveryValidator.validate(form);
        if (!errors.isEmpty()) return SaveResult.failed(errors);

        LocalDate oldDate = existing.date();
        LocalTime oldTime = existing.time();
        String oldName = existing.customerName();
        String oldAddress = existing.address();
        String oldPhone = existing.phone();
        String oldPayment = existing.paymentType();
        List<MaterialLine> oldItems = existing.items();
        String oldNotes = existing.notes();

        try {
            existing.edit(form.date(), form.time(), form.name().trim(), form.address().trim(),
                    orEmpty(form.phone()), form.paymentType().trim(), form.materials(),
                    orEmpty(form.notes()));
            repository.update(existing);
        } catch (RuntimeException e) {
            existing.edit(oldDate, oldTime, oldName, oldAddress, oldPhone, oldPayment, oldItems, oldNotes);
            return SaveResult.failed(List.of("Could not save the changes: " + e.getMessage()));
        }
        return SaveResult.ok(existing);
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
