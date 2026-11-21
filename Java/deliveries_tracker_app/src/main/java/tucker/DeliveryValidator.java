package tucker;

import java.util.ArrayList;
import java.util.List;

/** Checks the values from the Add/Edit window. Pure logic: no GUI and no database. */
public final class DeliveryValidator {

    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_ADDRESS_LENGTH = 200;
    public static final int MIN_PHONE_DIGITS = 7;
    public static final int MAX_PHONE_DIGITS = 15;
    public static final int MAX_NOTES_LENGTH = 1000;

    private DeliveryValidator() {}

    /** Returns every problem found, in form order. An empty list means the form is valid. */
    public static List<String> validate(DeliveryForm form) {
        List<String> errors = new ArrayList<>();
        if (form == null) {
            errors.add("No delivery information was provided.");
            return errors;
        }

        String name = clean(form.name());
        if (name.isEmpty()) errors.add("Name is required.");
        else if (name.length() > MAX_NAME_LENGTH)
            errors.add("Name must be " + MAX_NAME_LENGTH + " characters or fewer.");

        String address = clean(form.address());
        if (address.isEmpty()) errors.add("Address is required.");
        else if (address.length() > MAX_ADDRESS_LENGTH)
            errors.add("Address must be " + MAX_ADDRESS_LENGTH + " characters or fewer.");

        String phone = clean(form.phone());
        if (!phone.isEmpty()) { // phone is optional
            if (!phone.matches("[0-9 +()\\-.]+")) {
                errors.add("Phone number can only contain digits, spaces, and + ( ) - .");
            } else {
                long digits = phone.chars().filter(Character::isDigit).count();
                if (digits < MIN_PHONE_DIGITS || digits > MAX_PHONE_DIGITS) {
                    errors.add("Phone number must have " + MIN_PHONE_DIGITS + " to "
                            + MAX_PHONE_DIGITS + " digits.");
                }
            }
        }

        if (form.date() == null) errors.add("Pick a delivery date.");
        if (form.time() == null) errors.add("Pick a delivery time.");

        String payment = clean(form.paymentType());
        if (!payment.equals("Cash") && !payment.equals("Check")) errors.add("Choose cash or check.");

        List<MaterialLine> materials = form.materials();
        if (materials == null || materials.isEmpty()) {
            errors.add("Select at least one material.");
        } else {
            for (MaterialLine m : materials) {
                if (m == null) {
                    errors.add("Invalid material entry.");
                    continue;
                }
                if (m.name().isEmpty())
                    errors.add("Every material needs a name (describe the \"Other\" material).");
                if (m.amount().isEmpty())
                    errors.add(m.name().isEmpty()
                            ? "Enter an amount for each material."
                            : "Enter an amount for " + m.name() + ".");
            }
        }

        if (clean(form.notes()).length() > MAX_NOTES_LENGTH)
            errors.add("Notes must be " + MAX_NOTES_LENGTH + " characters or fewer.");

        return errors;
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }
}
