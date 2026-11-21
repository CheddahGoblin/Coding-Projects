package tucker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for MaterialLine (one material on a delivery). */
class MaterialLineTest {

    @Test
    void summary_withAmountNameAndPrice_readsAmountNameAtPrice() {
        MaterialLine line = new MaterialLine("Brown Mulch", "3 yd", "$35/yd");
        assertEquals("3 yd Brown Mulch @ $35/yd", line.summary());
    }

    @Test
    void summary_withEmptyPrice_leavesOutTheAtSign() {
        MaterialLine line = new MaterialLine("Top Soil", "6 yd", "");
        assertEquals("6 yd Top Soil", line.summary());
    }

    @Test
    void summary_withEmptyAmount_startsWithTheName() {
        MaterialLine line = new MaterialLine("Compost", "", "$40/yd");
        assertEquals("Compost @ $40/yd", line.summary());
    }

    @Test
    void nullAndWhitespaceValues_becomeEmptyStrings() {
        MaterialLine line = new MaterialLine(null, "   ", null);
        assertEquals("", line.name());
        assertEquals("", line.amount());
        assertEquals("", line.price());
        assertEquals("", line.summary());
    }
}
