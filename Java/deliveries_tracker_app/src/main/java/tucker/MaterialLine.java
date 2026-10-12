package tucker;

/** One material on a delivery, e.g. name "Brown Mulch", amount "3 yd", price "$35/yd". */
public record MaterialLine(String name, String amount, String price) {

    /** Reads like "3 yd Brown Mulch @ $35/yd". */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        if (!amount.isBlank()) sb.append(amount).append(' ');
        sb.append(name);
        if (!price.isBlank()) sb.append(" @ ").append(price);
        return sb.toString();
    }
}
