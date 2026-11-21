package tucker;

/**
 * One material on a delivery, e.g. name "Brown Mulch", amount "3 yd", price "$35/yd".
 * Null values become empty strings and surrounding spaces are trimmed.
 */
public record MaterialLine(String name, String amount, String price) {

    public MaterialLine {
        name = name == null ? "" : name.trim();
        amount = amount == null ? "" : amount.trim();
        price = price == null ? "" : price.trim();
    }

    /** Reads like "3 yd Brown Mulch @ $35/yd". */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        if (!amount.isEmpty()) sb.append(amount).append(' ');
        sb.append(name);
        if (!price.isEmpty()) sb.append(" @ ").append(price);
        return sb.toString();
    }
}
