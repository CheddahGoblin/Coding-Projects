package tucker;

import javafx.application.Application;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardApp extends Application {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, MMM d");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    // Swap this for a database-backed implementation later.
    private final DeliveryRepository repository = new SampleDeliveryRepository();

    private boolean archiveMode = false;

    // Pipeline: loaded deliveries -> filter (active vs archived) -> sort -> table.
    // The extractor makes the list notice when a checkbox changes.
    private final ObservableList<Delivery> deliveries = FXCollections.observableArrayList(
            d -> new Observable[] { d.deliveredProperty(), d.paidProperty() });
    private final FilteredList<Delivery> visible =
            new FilteredList<>(deliveries, d -> d.isArchived() == archiveMode);

    private final TableView<Delivery> table = new TableView<>();
    private TableColumn<Delivery, LocalDate> dateCol;
    private TableColumn<Delivery, LocalTime> timeCol;
    private TableColumn<Delivery, String> nameCol;
    private TableColumn<Delivery, String> addressCol;

    private final Label titleLabel = new Label();
    private final Label rangeLabel = new Label();
    private final Label countLabel = new Label();
    private final Button archiveButton = new Button();
    private final ComboBox<TableColumn<Delivery, ?>> sortBy = new ComboBox<>();
    private final ToggleButton descending = new ToggleButton("Descending");

    @Override
    public void start(Stage stage) {
        buildTable();

        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setCenter(table);
        root.setBottom(buildFooter());
        BorderPane.setMargin(table, new Insets(0, 16, 0, 16));

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

        setArchiveMode(false);

        stage.setTitle("Delivery Dashboard");
        stage.setScene(new Scene(root, 1050, 600));
        stage.show();
    }

    // ---------- Header: title, archive button, sort controls ----------

    private VBox buildHeader() {
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        rangeLabel.setStyle("-fx-text-fill: #666;");
        VBox titleBox = new VBox(2, titleLabel, rangeLabel);

        archiveButton.setOnAction(e -> setArchiveMode(!archiveMode));

        sortBy.getItems().addAll(dateCol, timeCol, nameCol, addressCol);
        sortBy.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(TableColumn<Delivery, ?> c) { return c == null ? "" : c.getText(); }
            @Override public TableColumn<Delivery, ?> fromString(String s) { return null; }
        });
        sortBy.setOnAction(e -> applySort());
        descending.setOnAction(e -> applySort());

        HBox controls = new HBox(8, new Label("Sort by:"), sortBy, descending, archiveButton);
        controls.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(titleBox, spacer, controls);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16));
        return new VBox(header);
    }

    private void applySort() {
        TableColumn<Delivery, ?> col = sortBy.getValue();
        if (col == null) return;
        col.setSortType(descending.isSelected()
                ? TableColumn.SortType.DESCENDING : TableColumn.SortType.ASCENDING);
        table.getSortOrder().setAll(col);
    }

    // ---------- Table ----------

    private void buildTable() {
        table.setEditable(true); // needed for the checkboxes

        dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().date()));
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(DATE_FMT));
            }
        });
        dateCol.setPrefWidth(120);

        timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().time()));
        timeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalTime t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : t.format(TIME_FMT));
            }
        });
        timeCol.setPrefWidth(90);

        nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().customerName()));
        nameCol.setComparator(String.CASE_INSENSITIVE_ORDER);
        nameCol.setPrefWidth(200);

        addressCol = new TableColumn<>("Address");
        addressCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().address()));
        addressCol.setComparator(String.CASE_INSENSITIVE_ORDER);
        addressCol.setPrefWidth(220);

        TableColumn<Delivery, String> materialsCol = new TableColumn<>("Materials");
        materialsCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().materials()));
        materialsCol.setPrefWidth(200);
        materialsCol.setSortable(false);

        TableColumn<Delivery, Boolean> deliveredCol = new TableColumn<>("Delivered");
        deliveredCol.setCellValueFactory(c -> c.getValue().deliveredProperty());
        deliveredCol.setCellFactory(CheckBoxTableCell.forTableColumn(deliveredCol));
        deliveredCol.setEditable(true);
        deliveredCol.setSortable(false);
        deliveredCol.setPrefWidth(90);

        TableColumn<Delivery, Boolean> paidCol = new TableColumn<>("Paid");
        paidCol.setCellValueFactory(c -> c.getValue().paidProperty());
        paidCol.setCellFactory(CheckBoxTableCell.forTableColumn(paidCol));
        paidCol.setEditable(true);
        paidCol.setSortable(false);
        paidCol.setPrefWidth(70);

        table.getColumns().addAll(List.of(dateCol, timeCol, nameCol, addressCol,
                materialsCol, deliveredCol, paidCol));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        SortedList<Delivery> sorted = new SortedList<>(visible);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        // Dashboard only: red = overdue (past date, not yet delivered AND paid), green = today
        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Delivery d, boolean empty) {
                super.updateItem(d, empty);
                String style = "";
                if (!archiveMode && !empty && d != null) {
                    LocalDate today = LocalDate.now();
                    if (d.date().isBefore(today)) {
                        style = "-fx-background-color: #ffcdd2;";
                    } else if (d.date().equals(today)) {
                        style = "-fx-background-color: #e8f5e9;";
                    }
                }
                setStyle(style);
            }
        });
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
            table.setPlaceholder(new Label("No archived deliveries yet."));
            deliveries.setAll(repository.findArchived());
        } else {
            titleLabel.setText("Upcoming Deliveries");
            rangeLabel.setText("Overdue + next 7 days (through " + today.plusDays(6).format(DATE_FMT) + ")");
            archiveButton.setText("Archive");
            table.setPlaceholder(new Label("No open or upcoming deliveries."));
            deliveries.setAll(repository.findActiveThrough(today.plusDays(6)));
        }

        // Dashboard: soonest first. Archive: most recent first.
        descending.setSelected(on);
        sortBy.setValue(dateCol);
        applySort();
        updateCount();
        table.refresh();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
