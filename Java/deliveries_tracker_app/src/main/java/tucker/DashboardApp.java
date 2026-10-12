package tucker;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DashboardApp extends Application {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, MMM d");
    private static final DateTimeFormatter FULL_DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    private enum SortKey {
        DATE("Date"), TIME("Time"), NAME("Name"), ADDRESS("Address");
        final String label;
        SortKey(String label) { this.label = label; }
    }

    // Swap this for a database-backed implementation later.
    private final DeliveryRepository repository = new SampleDeliveryRepository();

    private boolean archiveMode = false;
    private String emptyMessage = "";
    private boolean rebuildPending = false;
    private Stage stage;

    // Pipeline: loaded deliveries -> filter (active vs archived) -> sort -> rows on screen.
    // The extractor makes the list notice when a delivered/paid checkbox changes.
    private final ObservableList<Delivery> deliveries = FXCollections.observableArrayList(
            d -> new Observable[] { d.deliveredProperty(), d.paidProperty() });
    private final FilteredList<Delivery> visible =
            new FilteredList<>(deliveries, d -> d.isArchived() == archiveMode);
    private final SortedList<Delivery> sorted = new SortedList<>(visible);

    // Which deliveries are currently expanded (Delivery has no equals(), so this is by identity)
    private final Set<Delivery> expanded = new HashSet<>();

    private final VBox rowsBox = new VBox();
    private final ScrollPane scroll = new ScrollPane(rowsBox);
    private final HBox columnHeaderWrap = new HBox();
    private final Map<SortKey, Label> headerLabels = new EnumMap<>(SortKey.class);

    private final Label titleLabel = new Label();
    private final Label rangeLabel = new Label();
    private final Label countLabel = new Label();
    private final Button archiveButton = new Button();
    private final Button addButton = new Button("+ Add Delivery");
    private final ComboBox<SortKey> sortBy = new ComboBox<>();
    private final ToggleButton descendingToggle = new ToggleButton("Descending");

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        rowsBox.setStyle("-fx-background-color: white;");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: white; -fx-background-color: transparent; -fx-padding: 0;");

        VBox center = new VBox(buildColumnHeader(), scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setCenter(center);
        root.setBottom(buildFooter());
        BorderPane.setMargin(center, new Insets(0, 16, 0, 16));

        // Persist checkbox changes (delivered / paid)
        deliveries.addListener((ListChangeListener<Delivery>) c -> {
            while (c.next()) {
                if (c.wasUpdated()) {
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        repository.update(c.getList().get(i));
                    }
                }
            }
        });
        // Keep the footer count in sync as deliveries move in/out of view
        visible.addListener((ListChangeListener<Delivery>) c -> updateCount());
        // Redraw the rows when deliveries are added/removed/re-ordered.
        // (Pure checkbox updates don't need a redraw: the checkboxes are bound to the data.)
        sorted.addListener((ListChangeListener<Delivery>) c -> {
            boolean structural = false;
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved() || c.wasPermutated()) structural = true;
            }
            if (structural) scheduleRebuild();
        });

        setArchiveMode(false);

        stage.setTitle("Delivery Dashboard");
        stage.setScene(new Scene(root, 1100, 650));
        stage.show();

        Platform.runLater(this::alignHeaderWithScrollBar);
    }

    // ---------- Top bar: title, sort controls, archive + add buttons ----------

    private VBox buildHeader() {
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        rangeLabel.setStyle("-fx-text-fill: #666;");
        VBox titleBox = new VBox(2, titleLabel, rangeLabel);

        archiveButton.setOnAction(e -> setArchiveMode(!archiveMode));
        addButton.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold;");
        addButton.setOnAction(e -> openAddDialog());

        sortBy.getItems().addAll(SortKey.values());
        sortBy.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(SortKey k) { return k == null ? "" : k.label; }
            @Override public SortKey fromString(String s) { return null; }
        });
        sortBy.setOnAction(e -> applySort());
        descendingToggle.setOnAction(e -> applySort());

        HBox controls = new HBox(8, new Label("Sort by:"), sortBy, descendingToggle, archiveButton, addButton);
        controls.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(titleBox, spacer, controls);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16));
        return new VBox(header);
    }

    // ---------- Sorting ----------

    private void applySort() {
        SortKey key = sortBy.getValue() == null ? SortKey.DATE : sortBy.getValue();
        boolean desc = descendingToggle.isSelected();
        sorted.setComparator(buildComparator(key, desc));

        for (Map.Entry<SortKey, Label> e : headerLabels.entrySet()) {
            String arrow = e.getKey() == key ? (desc ? "  ↓" : "  ↑") : "";
            e.getValue().setText(e.getKey().label + arrow);
        }
    }

    private Comparator<Delivery> buildComparator(SortKey key, boolean desc) {
        Comparator<Delivery> byDateTime =
                Comparator.comparing(Delivery::date).thenComparing(Delivery::time);
        Comparator<Delivery> c = switch (key) {
            case DATE -> byDateTime;
            case TIME -> Comparator.comparing(Delivery::time).thenComparing(Delivery::date);
            case NAME -> Comparator.comparing(Delivery::customerName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(byDateTime);
            case ADDRESS -> Comparator.comparing(Delivery::address, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(byDateTime);
        };
        return desc ? c.reversed() : c;
    }

    private void headerClicked(SortKey key) {
        if (sortBy.getValue() == key) {
            descendingToggle.setSelected(!descendingToggle.isSelected());
        } else {
            sortBy.setValue(key);
            descendingToggle.setSelected(false);
        }
        applySort();
    }

    // ---------- Column layout shared by the header row and every delivery row ----------

    private static void applyColumns(GridPane g) {
        g.setHgap(10);
        g.setPadding(new Insets(8, 8, 8, 4));
        g.getColumnConstraints().addAll(
                fixedColumn(40),   // expand arrow
                fixedColumn(110),  // date
                fixedColumn(80),   // time
                flexColumn(180),   // name
                flexColumn(230),   // address
                flexColumn(280),   // materials
                fixedColumn(80),   // delivered checkbox
                fixedColumn(60));  // paid checkbox
    }

    private static ColumnConstraints fixedColumn(double width) {
        return new ColumnConstraints(width, width, width);
    }

    private static ColumnConstraints flexColumn(double pref) {
        ColumnConstraints cc = new ColumnConstraints(60, pref, Double.MAX_VALUE);
        cc.setHgrow(Priority.ALWAYS);
        return cc;
    }

    // ---------- Column header row (click Date / Time / Name / Address to sort) ----------

    private HBox buildColumnHeader() {
        GridPane g = new GridPane();
        applyColumns(g);
        g.setStyle("-fx-background-color: #eeeeee; -fx-border-color: transparent transparent #bdbdbd transparent;");

        int col = 1;
        for (SortKey key : SortKey.values()) {
            Label l = new Label(key.label);
            l.setStyle("-fx-font-weight: bold;");
            l.setCursor(Cursor.HAND);
            l.setMaxWidth(Double.MAX_VALUE);
            l.setOnMouseClicked(e -> headerClicked(key));
            headerLabels.put(key, l);
            g.add(l, col++, 0);
        }
        Label materials = new Label("Materials");
        materials.setStyle("-fx-font-weight: bold;");
        g.add(materials, col++, 0);
        for (String text : new String[] { "Delivered", "Paid" }) {
            Label l = new Label(text);
            l.setStyle("-fx-font-weight: bold;");
            l.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHalignment(l, HPos.CENTER);
            g.add(l, col++, 0);
        }

        HBox.setHgrow(g, Priority.ALWAYS);
        columnHeaderWrap.getChildren().add(g);
        return columnHeaderWrap;
    }

    /** Keeps the header columns lined up with the rows when the vertical scrollbar appears. */
    private void alignHeaderWithScrollBar() {
        Node node = scroll.lookup(".scroll-bar:vertical");
        if (node instanceof ScrollBar bar) {
            columnHeaderWrap.paddingProperty().bind(Bindings.createObjectBinding(
                    () -> new Insets(0, bar.isVisible() ? bar.getWidth() : 0, 0, 0),
                    bar.visibleProperty(), bar.widthProperty()));
        }
    }

    // ---------- Delivery rows ----------

    private void scheduleRebuild() {
        if (rebuildPending) return;
        rebuildPending = true;
        Platform.runLater(() -> {
            rebuildPending = false;
            rebuildRows();
        });
    }

    private void rebuildRows() {
        rowsBox.getChildren().clear();
        if (sorted.isEmpty()) {
            Label empty = new Label(emptyMessage);
            empty.setStyle("-fx-text-fill: #777; -fx-padding: 30;");
            rowsBox.getChildren().add(empty);
            return;
        }
        for (Delivery d : sorted) {
            rowsBox.getChildren().add(buildRow(d));
        }
    }

    private Node buildRow(Delivery d) {
        boolean isExpanded = expanded.contains(d);

        // The summary line: arrow, date, time, name, address, materials
        Button arrow = new Button(isExpanded ? "▲" : "▼");
        arrow.setStyle("-fx-background-color: transparent; -fx-font-size: 11px; -fx-cursor: hand;");
        arrow.setFocusTraversable(false);

        GridPane summary = new GridPane();
        applyColumns(summary);
        summary.add(arrow, 0, 0);
        summary.add(new Label(d.date().format(DATE_FMT)), 1, 0);
        summary.add(new Label(d.time().format(TIME_FMT)), 2, 0);
        summary.add(wrapLabel(d.customerName()), 3, 0);
        summary.add(wrapLabel(d.address()), 4, 0);
        summary.add(wrapLabel(d.materials()), 5, 0);

        // Delivered / Paid checkboxes live right on the row. Checking both archives the delivery.
        CheckBox deliveredBox = new CheckBox();
        deliveredBox.selectedProperty().bindBidirectional(d.deliveredProperty());
        CheckBox paidBox = new CheckBox();
        paidBox.selectedProperty().bindBidirectional(d.paidProperty());
        summary.add(deliveredBox, 6, 0);
        summary.add(paidBox, 7, 0);
        GridPane.setHalignment(deliveredBox, HPos.CENTER);
        GridPane.setHalignment(paidBox, HPos.CENTER);

        for (Node n : summary.getChildren()) {
            GridPane.setValignment(n, VPos.TOP);
        }

        // The expanded section
        VBox details = buildDetails(d);
        details.setVisible(isExpanded);
        details.setManaged(isExpanded);

        arrow.setOnAction(e -> {
            boolean nowExpanded = !details.isVisible();
            details.setVisible(nowExpanded);
            details.setManaged(nowExpanded);
            arrow.setText(nowExpanded ? "▲" : "▼");
            if (nowExpanded) expanded.add(d); else expanded.remove(d);
        });

        VBox row = new VBox(summary, details);
        row.setStyle(rowStyle(d));
        return row;
    }

    private VBox buildDetails(Delivery d) {
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(8);

        VBox materialLines = new VBox(2);
        for (MaterialLine m : d.items()) {
            materialLines.getChildren().add(new Label("•  " + m.summary()));
        }

        addDetail(g, 0, "Date & time",
                new Label(d.date().format(FULL_DATE_FMT) + " at " + d.time().format(TIME_FMT)));
        addDetail(g, 1, "Phone", new Label(orDash(d.phone())));
        addDetail(g, 2, "Payment type", new Label(orDash(d.paymentType())));
        addDetail(g, 3, "Materials", materialLines);

        Button edit = new Button("Edit Delivery");
        edit.setOnAction(e -> openEditDialog(d));
        VBox box = new VBox(12, g, edit);
        box.setPadding(new Insets(6, 12, 14, 48));
        box.setStyle("-fx-background-color: rgba(0,0,0,0.04);");
        return box;
    }

    private static void addDetail(GridPane g, int row, String key, Node value) {
        Label k = new Label(key);
        k.setStyle("-fx-text-fill: #666; -fx-font-weight: bold;");
        k.setMinWidth(100);
        g.add(k, 0, row);
        g.add(value, 1, row);
        GridPane.setValignment(k, VPos.TOP);
        GridPane.setValignment(value, VPos.TOP);
    }

    private static Label wrapLabel(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private static String orDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    /** Dashboard only: red = overdue (past date, not yet delivered AND paid), green = today. */
    private String rowStyle(Delivery d) {
        String bg = "transparent";
        if (!archiveMode) {
            LocalDate today = LocalDate.now();
            if (d.date().isBefore(today)) bg = "#ffcdd2";
            else if (d.date().equals(today)) bg = "#e8f5e9";
        }
        return "-fx-background-color: " + bg + "; -fx-border-color: transparent transparent #dcdcdc transparent;";
    }

    // ---------- Footer ----------

    private HBox buildFooter() {
        HBox footer = new HBox(countLabel);
        footer.setPadding(new Insets(10, 16, 10, 16));
        return footer;
    }

    private void updateCount() {
        int n = visible.size();
        countLabel.setText(archiveMode
                ? n + " archived deliveries"
                : n + " open deliveries (overdue + next 7 days)");
    }

    // ---------- Mode switching / data ----------

    private void setArchiveMode(boolean on) {
        archiveMode = on;
        LocalDate today = LocalDate.now();

        if (on) {
            titleLabel.setText("Archived Deliveries");
            rangeLabel.setText("Delivered and paid");
            archiveButton.setText("← Back to Dashboard");
            emptyMessage = "No archived deliveries yet.";
        } else {
            titleLabel.setText("Upcoming Deliveries");
            rangeLabel.setText("Overdue + next 7 days (through " + today.plusDays(6).format(DATE_FMT) + ")");
            archiveButton.setText("Archive");
            emptyMessage = "No open or upcoming deliveries.";
        }
        loadDeliveries();

        // Dashboard: soonest first. Archive: most recent first.
        descendingToggle.setSelected(on);
        sortBy.setValue(SortKey.DATE);
        applySort();
        updateCount();
        scheduleRebuild();
    }

    /** (Re)loads the current view's deliveries from the repository. */
    private void loadDeliveries() {
        if (archiveMode) {
            deliveries.setAll(repository.findArchived());
        } else {
            deliveries.setAll(repository.findActiveThrough(LocalDate.now().plusDays(6)));
        }
    }

    private void openAddDialog() {
        new DeliveryDialog(stage, delivery -> {
            repository.add(delivery);
            loadDeliveries();
        }).show();
    }

    private void openEditDialog(Delivery delivery) {
        new DeliveryDialog(stage, delivery, edited -> {
            repository.update(edited);
            loadDeliveries(); // re-sorts / re-filters in case the date changed
        }).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
