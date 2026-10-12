package tucker;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Window for creating a delivery, or editing an existing one (pre-filled with its saved info).
 * Customer info, payment type, and materials with "amount @ price".
 */
public class DeliveryDialog extends Stage {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    /** Materials grouped by type, in display order. Edit this list to add/rename materials. */
    private static final Map<String, List<String>> MATERIAL_GROUPS = new LinkedHashMap<>();
    static {
        MATERIAL_GROUPS.put("Mulch", List.of("Brown Mulch", "Black Mulch", "Red Mulch", "Cinnamon Mulch"));
        MATERIAL_GROUPS.put("Compost & Soil", List.of("Compost", "Garden Soil", "Top Soil", "Unprocessed Soil"));
        MATERIAL_GROUPS.put("Sand", List.of("Concrete Sand", "Limestone Sand"));
        MATERIAL_GROUPS.put("Limestone", List.of("#617 Limestone", "#57 Limestone"));
        MATERIAL_GROUPS.put("River Rock", List.of("#4 River Rock", "#57 River Rock", "#8 River Rock"));
        MATERIAL_GROUPS.put("Decorative Stone", List.of("Mojave Stone", "Canadian Blue Stone"));
        MATERIAL_GROUPS.put("Other", List.of("Other"));
    }

    /** A checkbox plus the "amount @ price" fields that appear only when it's checked. */
    private static class MaterialRow {
        final String name;
        final CheckBox box;
        final TextField description = new TextField(); // only shown for "Other"
        final TextField amount = new TextField();
        final TextField price = new TextField();
        final HBox node;

        MaterialRow(String name) {
            this.name = name;
            box = new CheckBox(name);
            box.setMinWidth(170);
            box.setPrefWidth(170);

            amount.setPromptText("amount (e.g. 3 yd)");
            amount.setPrefWidth(130);
            price.setPromptText("price (e.g. $35/yd)");
            price.setPrefWidth(130);

            List<Node> details = new ArrayList<>();
            if (name.equals("Other")) {
                description.setPromptText("what material?");
                description.setPrefWidth(130);
                details.add(description);
            }
            details.add(amount);
            details.add(new Label("@"));
            details.add(price);

            // Hidden (and takes no space) until the box is checked
            for (Node n : details) {
                n.visibleProperty().bind(box.selectedProperty());
                n.managedProperty().bind(box.selectedProperty());
            }

            node = new HBox(8, box);
            node.setAlignment(Pos.CENTER_LEFT);
            node.getChildren().addAll(details);
        }
    }

    private final TextField nameField = new TextField();
    private final TextField addressField = new TextField();
    private final TextField phoneField = new TextField();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final ComboBox<LocalTime> timeBox = new ComboBox<>();
    private final RadioButton cashRadio = new RadioButton("Cash");
    private final RadioButton checkRadio = new RadioButton("Check");
    private final List<MaterialRow> rows = new ArrayList<>();
    private final Label errorLabel = new Label();
    private final TextArea notesArea = new TextArea();
    private final Delivery existing; // null when creating a new delivery

    /** Opens an empty form to create a new delivery. */
    public DeliveryDialog(Window owner, Consumer<Delivery> onSave) {
        this(owner, null, onSave);
    }

    /** Opens the form pre-filled with an existing delivery; saving updates that delivery. */
    public DeliveryDialog(Window owner, Delivery existing, Consumer<Delivery> onSave) {
        this.existing = existing;
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle(existing == null ? "New Delivery" : "Edit Delivery");

        // ----- Customer / delivery info -----
        ToggleGroup payGroup = new ToggleGroup();
        cashRadio.setToggleGroup(payGroup);
        checkRadio.setToggleGroup(payGroup);

        for (LocalTime t = LocalTime.of(6, 0); !t.isAfter(LocalTime.of(18, 0)); t = t.plusMinutes(15)) {
            timeBox.getItems().add(t);
        }
        timeBox.setValue(LocalTime.of(8, 0));
        timeBox.setConverter(new StringConverter<>() {
            @Override public String toString(LocalTime t) { return t == null ? "" : t.format(TIME_FMT); }
            @Override public LocalTime fromString(String s) { return null; }
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        ColumnConstraints labelCol = new ColumnConstraints();
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(labelCol, fieldCol);

        nameField.setPromptText("Customer name");
        addressField.setPromptText("Delivery address");
        phoneField.setPromptText("Phone number");

        form.addRow(0, new Label("Name"), nameField);
        form.addRow(1, new Label("Address"), addressField);
        form.addRow(2, new Label("Phone number"), phoneField);
        form.addRow(3, new Label("Date of delivery"), datePicker);
        form.addRow(4, new Label("Time"), timeBox);
        form.addRow(5, new Label("Payment type"), new HBox(16, cashRadio, checkRadio));

        // ----- Materials, grouped by type -----
        VBox materialsBox = new VBox(6);
        Label materialsTitle = new Label("Materials");
        materialsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        materialsBox.getChildren().add(materialsTitle);

        for (Map.Entry<String, List<String>> group : MATERIAL_GROUPS.entrySet()) {
            Label groupLabel = new Label(group.getKey());
            groupLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");
            VBox.setMargin(groupLabel, new Insets(8, 0, 0, 0));
            materialsBox.getChildren().add(groupLabel);

            for (String material : group.getValue()) {
                MaterialRow row = new MaterialRow(material);
                rows.add(row);
                materialsBox.getChildren().add(row.node);
            }
        }

        // ----- Notes (bottom of the form) -----
        Label notesTitle = new Label("Notes");
        notesTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        notesArea.setPromptText("Gate code, where to drop the load, special instructions...");
        notesArea.setWrapText(true);
        notesArea.setPrefRowCount(4);
        VBox notesBox = new VBox(6, notesTitle, notesArea);

        VBox content = new VBox(14, form, new Separator(), materialsBox, new Separator(), notesBox);
        content.setPadding(new Insets(16));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);

        // ----- Buttons -----
        errorLabel.setStyle("-fx-text-fill: #c62828;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> close());
        Button save = new Button(existing == null ? "Save Delivery" : "Save Changes");
        save.setDefaultButton(true);
        save.setOnAction(e -> save(onSave));
        HBox buttons = new HBox(10, errorLabel, spacer, cancel, save);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setPadding(new Insets(10, 16, 10, 16));

        BorderPane root = new BorderPane();
        root.setCenter(scroll);
        root.setBottom(buttons);

        setScene(new Scene(root, 720, 760));

        if (existing != null) prefill(existing);
    }

    /** Fills the form with a delivery's saved info. */
    private void prefill(Delivery d) {
        nameField.setText(d.customerName());
        addressField.setText(d.address());
        phoneField.setText(d.phone() == null ? "" : d.phone());
        notesArea.setText(d.notes());
        datePicker.setValue(d.date());

        if (!timeBox.getItems().contains(d.time())) { // a time that isn't on the 15-minute list
            timeBox.getItems().add(d.time());
            FXCollections.sort(timeBox.getItems());
        }
        timeBox.setValue(d.time());

        if ("Check".equals(d.paymentType())) checkRadio.setSelected(true);
        else if ("Cash".equals(d.paymentType())) cashRadio.setSelected(true);

        MaterialRow other = rows.stream().filter(r -> r.name.equals("Other")).findFirst().orElseThrow();
        for (MaterialLine m : d.items()) {
            MaterialRow match = rows.stream()
                    .filter(r -> !r.name.equals("Other") && r.name.equals(m.name()))
                    .findFirst().orElse(null);
            if (match != null) {
                match.box.setSelected(true);
                match.amount.setText(m.amount());
                match.price.setText(m.price());
            } else if (!other.box.isSelected()) {
                // A material that isn't on the checkbox list: show it under "Other"
                other.box.setSelected(true);
                other.description.setText(m.name());
                other.amount.setText(m.amount());
                other.price.setText(m.price());
            } else {
                other.description.appendText(" / " + m.name());
                other.amount.appendText(" / " + m.amount());
                other.price.appendText(" / " + m.price());
            }
        }
    }

    private void save(Consumer<Delivery> onSave) {
        String name = nameField.getText().trim();
        String address = addressField.getText().trim();
        LocalDate date = datePicker.getValue();

        if (name.isEmpty()) { error("Name is required."); return; }
        if (address.isEmpty()) { error("Address is required."); return; }
        if (date == null) { error("Pick a delivery date."); return; }
        if (!cashRadio.isSelected() && !checkRadio.isSelected()) { error("Choose cash or check."); return; }

        List<MaterialLine> lines = new ArrayList<>();
        for (MaterialRow r : rows) {
            if (!r.box.isSelected()) continue;

            String label = r.name;
            if (r.name.equals("Other")) {
                label = r.description.getText().trim();
                if (label.isEmpty()) { error("Describe the \"Other\" material."); return; }
            }
            String amount = r.amount.getText().trim();
            if (amount.isEmpty()) { error("Enter an amount for " + label + "."); return; }

            lines.add(new MaterialLine(label, amount, r.price.getText().trim()));
        }
        if (lines.isEmpty()) { error("Select at least one material."); return; }

        String payment = cashRadio.isSelected() ? "Cash" : "Check";
        String phone = phoneField.getText().trim();
        String notes = notesArea.getText().trim();
        if (existing == null) {
            // id 0 = "not assigned yet"; the database will generate the real one later.
            onSave.accept(new Delivery(0, date, timeBox.getValue(), name, address,
                    phone, payment, lines, notes, false, false));
        } else {
            existing.edit(date, timeBox.getValue(), name, address, phone, payment, lines, notes);
            onSave.accept(existing);
        }
        close();
    }

    private void error(String message) {
        errorLabel.setText(message);
    }
}
