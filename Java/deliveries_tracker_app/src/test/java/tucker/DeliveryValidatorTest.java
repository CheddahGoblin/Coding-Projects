package tucker;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for DeliveryValidator: normal, empty, boundary and invalid form input. */
class DeliveryValidatorTest {

    private static final List<MaterialLine> ONE_MATERIAL =
            List.of(new MaterialLine("Brown Mulch", "3 yd", "$35/yd"));

    /** A fully valid form; each test changes just the field it is checking. */
    private static DeliveryForm form(String name, String address, String phone, String payment,
                                     List<MaterialLine> materials, String notes) {
        return new DeliveryForm(name, address, phone, LocalDate.of(2026, 10, 15),
                LocalTime.of(9, 0), payment, materials, notes);
    }

    private static DeliveryForm valid() {
        return form("Maria Alvarez", "88 Lakeview Ct", "612-555-0102", "Cash", ONE_MATERIAL, "");
    }

    private static DeliveryForm withPhone(String phone) {
        return form("Maria Alvarez", "88 Lakeview Ct", phone, "Cash", ONE_MATERIAL, "");
    }

    @Test
    void validForm_hasNoErrors() {
        assertEquals(List.of(), DeliveryValidator.validate(valid()));
    }

    @Test
    void emptyOrWhitespaceName_isRejected() {
        for (String name : new String[] { "", "   ", null }) {
            List<String> errors = DeliveryValidator.validate(
                    form(name, "88 Lakeview Ct", "", "Cash", ONE_MATERIAL, ""));
            assertEquals(List.of("Name is required."), errors);
        }
    }

    @Test
    void nameLength_boundaryAtMaximum() {
        String atLimit = "a".repeat(DeliveryValidator.MAX_NAME_LENGTH);
        String overLimit = "a".repeat(DeliveryValidator.MAX_NAME_LENGTH + 1);

        assertEquals(List.of(), DeliveryValidator.validate(
                form(atLimit, "88 Lakeview Ct", "", "Cash", ONE_MATERIAL, "")));
        assertEquals(List.of("Name must be 100 characters or fewer."), DeliveryValidator.validate(
                form(overLimit, "88 Lakeview Ct", "", "Cash", ONE_MATERIAL, "")));
    }

    @Test
    void emptyAddress_isRejected() {
        List<String> errors = DeliveryValidator.validate(
                form("Maria Alvarez", "  ", "", "Cash", ONE_MATERIAL, ""));
        assertEquals(List.of("Address is required."), errors);
    }

    @Test
    void emptyPhone_isAllowedBecausePhoneIsOptional() {
        assertEquals(List.of(), DeliveryValidator.validate(withPhone("")));
        assertEquals(List.of(), DeliveryValidator.validate(withPhone(null)));
    }

    @Test
    void phoneDigitCount_boundaries() {
        String tooFew = "Phone number must have 7 to 15 digits.";
        assertEquals(List.of(tooFew), DeliveryValidator.validate(withPhone("555-012")));        // 6 digits
        assertEquals(List.of(), DeliveryValidator.validate(withPhone("555-0123")));             // 7 digits
        assertEquals(List.of(), DeliveryValidator.validate(withPhone("123456789012345")));      // 15 digits
        assertEquals(List.of(tooFew), DeliveryValidator.validate(withPhone("1234567890123456"))); // 16 digits
    }

    @Test
    void phoneWithLetters_isRejected_butCommonFormattingIsAccepted() {
        assertEquals(1, DeliveryValidator.validate(withPhone("555-CALL-NOW")).size());
        assertEquals(List.of(), DeliveryValidator.validate(withPhone("(612) 555-0101")));
        assertEquals(List.of(), DeliveryValidator.validate(withPhone("+1 612.555.0101")));
    }

    @Test
    void missingDateAndTime_areRejected() {
        DeliveryForm noDateOrTime = new DeliveryForm("Maria Alvarez", "88 Lakeview Ct", "", null, null,
                "Cash", ONE_MATERIAL, "");
        assertEquals(List.of("Pick a delivery date.", "Pick a delivery time."),
                DeliveryValidator.validate(noDateOrTime));
    }

    @Test
    void paymentType_mustBeCashOrCheck() {
        for (String bad : new String[] { "", "Credit", "cash", null }) {
            assertEquals(List.of("Choose cash or check."), DeliveryValidator.validate(
                    form("Maria Alvarez", "88 Lakeview Ct", "", bad, ONE_MATERIAL, "")), "payment=" + bad);
        }
        assertEquals(List.of(), DeliveryValidator.validate(
                form("Maria Alvarez", "88 Lakeview Ct", "", "Check", ONE_MATERIAL, "")));
    }

    @Test
    void noMaterials_isRejected_whetherEmptyOrNull() {
        assertEquals(List.of("Select at least one material."), DeliveryValidator.validate(
                form("Maria Alvarez", "88 Lakeview Ct", "", "Cash", List.of(), "")));
        assertEquals(List.of("Select at least one material."), DeliveryValidator.validate(
                form("Maria Alvarez", "88 Lakeview Ct", "", "Cash", null, "")));
    }

    @Test
    void materialMissingAmountOrName_isRejected() {
        List<String> noAmount = DeliveryValidator.validate(form("Maria Alvarez", "88 Lakeview Ct", "", "Cash",
                List.of(new MaterialLine("Top Soil", "", "$32/yd")), ""));
        assertEquals(List.of("Enter an amount for Top Soil."), noAmount);

        List<String> noName = DeliveryValidator.validate(form("Maria Alvarez", "88 Lakeview Ct", "", "Cash",
                List.of(new MaterialLine("", "3 yd", "")), ""));
        assertEquals(List.of("Every material needs a name (describe the \"Other\" material)."), noName);
    }

    @Test
    void notesLength_boundaryAtMaximum() {
        String atLimit = "n".repeat(DeliveryValidator.MAX_NOTES_LENGTH);
        String overLimit = "n".repeat(DeliveryValidator.MAX_NOTES_LENGTH + 1);
        assertEquals(List.of(), DeliveryValidator.validate(
                form("Maria Alvarez", "88 Lakeview Ct", "", "Cash", ONE_MATERIAL, atLimit)));
        assertEquals(List.of("Notes must be 1000 characters or fewer."), DeliveryValidator.validate(
                form("Maria Alvarez", "88 Lakeview Ct", "", "Cash", ONE_MATERIAL, overLimit)));
    }

    @Test
    void severalProblems_areAllReportedInFormOrder() {
        List<String> errors = DeliveryValidator.validate(form("", "", "", "", List.of(), ""));
        assertEquals(List.of("Name is required.", "Address is required.",
                "Choose cash or check.", "Select at least one material."), errors);
    }

    @Test
    void nullForm_isRejected() {
        assertEquals(List.of("No delivery information was provided."), DeliveryValidator.validate(null));
    }
}
