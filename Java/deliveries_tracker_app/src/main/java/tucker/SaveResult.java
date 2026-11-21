package tucker;

import java.util.List;

/** Outcome of creating or editing a delivery: either the saved delivery, or a list of problems. */
public record SaveResult(boolean success, List<String> errors, Delivery delivery) {
    
    public static SaveResult ok(Delivery delivery) {
        return new SaveResult(true, List.of(), delivery);
    }

    public static SaveResult failed(List<String> errors) {
        return new SaveResult(false, List.copyOf(errors), null);
    }
}
