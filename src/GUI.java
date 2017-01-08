import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Project Name: KnightsTour
 * Author: Kevin
 * Date: Dec 28, 2016
 * Description:
 */
public class GUI extends Application
{
    public static Thread t = null;

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        /* Convenience variables. */
        final double DEFAULT_STAGE_WIDTH = 900, DEFAULT_STAGE_HEIGHT = 900;
        final Color TILE_BLACK = Color.BLACK, TILE_WHITE = Color.WHITE;
        final int MAX_COLUMNS = 20, MAX_ROWS = 20,
                MIN_COLUMNS = 1, MIN_ROWS = 1,
                DEFAULT_COLUMNS = 6, DEFAULT_ROWS = 6;

        /* Back-end variables. */
        final ObjectProperty<Board> boardProperty = new SimpleObjectProperty<>();

        /* Nodes. */
        final Slider sldColumns = new Slider(MIN_COLUMNS, MAX_COLUMNS, DEFAULT_COLUMNS),
                sldRows = new Slider(MIN_ROWS, MAX_ROWS, DEFAULT_ROWS),
                sldKnightX = new Slider(),
                sldKnightY = new Slider();
        final ImageView imvKnight = new ImageView();
        final Label lblColumns = new Label("Columns"),
                lblRows = new Label("Rows"),
                lblKnightX = new Label("Knight Starting X Coordinate"),
                lblKnightY = new Label("Knight Starting Y Coordinate"),
                lblMessage = new Label("");
        final Button btnStart = new Button("Attempt Simulation");

        /* Panes. */
        final VBox vbxSetup = new VBox(lblColumns, sldColumns, lblRows, sldRows,
                lblKnightX, sldKnightX, lblKnightY, sldKnightY,
                btnStart, lblMessage);
        final Pane panArrows = new Pane();
        final GridPane grdBoard = new GridPane();
        final StackPane stkChessboard = new StackPane(grdBoard, panArrows);
        final Tab tabSetup = new Tab("Setup", vbxSetup),
                tabChessboard = new Tab("Chessboard", stkChessboard);
        final TabPane root = new TabPane(tabSetup, tabChessboard);

        /* Properties. */
        final DoubleProperty cellSizeProperty = new SimpleDoubleProperty();

        /* Basic setup. */
        primaryStage.setWidth(DEFAULT_STAGE_WIDTH);
        primaryStage.setHeight(DEFAULT_STAGE_HEIGHT);
        imvKnight.setPreserveRatio(true);
        grdBoard.setGridLinesVisible(true);
        sldKnightX.setMin(1);
        sldKnightY.setMin(1);

        /* Properties sync. */
        sldKnightX.maxProperty().bind(sldColumns.valueProperty());
        sldKnightY.maxProperty().bind(sldRows.valueProperty());
        sldKnightX.setMin(MIN_COLUMNS);
        sldKnightY.setMinHeight(MIN_ROWS);
        cellSizeProperty.bind(Bindings.max(primaryStage.widthProperty(), primaryStage.heightProperty()).divide(Bindings.max(sldColumns.valueProperty(), sldRows.valueProperty())).multiply(0.85));
        imvKnight.fitHeightProperty().bind(cellSizeProperty);

        /* TabPane settings. */
        root.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        root.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != tabChessboard)
                return;
            final int columns = (int) sldColumns.getValue(),
                    rows = (int) sldRows.getValue();

            /* Clear the prior state of the GridPane. */
            grdBoard.getChildren().clear();
            grdBoard.getColumnConstraints().clear();
            grdBoard.getRowConstraints().clear();
            panArrows.getChildren().clear();

            /* Gridpane setup. */
            for (int x = 0; x < columns; x++)
            {
            /* Add each column to the grid. */
                final ColumnConstraints col = new ColumnConstraints();
                col.prefWidthProperty().bind(cellSizeProperty);
                col.setHalignment(HPos.CENTER);
                grdBoard.getColumnConstraints().add(col);
                for (int y = 0; y < rows; y++)
                {
                /* Each cell must have a Black or White background. */
                    final Rectangle rectCell = new Rectangle();
                    rectCell.heightProperty().bind(cellSizeProperty);
                    rectCell.widthProperty().bind(cellSizeProperty);
                    rectCell.setFill((x + y) % 2 == 0 ? TILE_BLACK : TILE_WHITE);
                    final StackPane stkCell = new StackPane(rectCell);
                    grdBoard.add(stkCell, x, y);
                }
            }
            for (int y = 0; y < rows; y++)
            {
                final RowConstraints row = new RowConstraints();
                row.prefHeightProperty().bind(cellSizeProperty);
                row.setValignment(VPos.CENTER);
                grdBoard.getRowConstraints().add(row);
            }

            /* If this board was not solved, do not attempt to play the animation. */
            if (boardProperty.get() == null || boardProperty.get().movesProperty().get() < 0)
                return;

            final List<Point> path = new ArrayList<>(boardProperty.get().getKnightHistory());
            final Iterator<Point> iterator = path.iterator();
            final ObjectProperty<ObservableList<Node>> prevKnight = new SimpleObjectProperty<>();
            final ObjectProperty<Point> prevLoc = new SimpleObjectProperty<>();
            final AnimationTimer timer = new AnimationTimer()
            {
                @Override
                public void handle(long now)
                {
                    if (!iterator.hasNext() || boardProperty.get().getColumns() != columns || boardProperty.get().getRows() != rows)
                    {
                        stop();
                        return;
                    }

                    try
                    {
                        Thread.sleep(350);
                    }
                    catch (InterruptedException ignored)
                    {
                    }

                    /* Remove the previous Knight image, if one exists. */
                    if (prevKnight.get() != null)
                        prevKnight.get().remove(imvKnight);
                    /* Place the knight in the new location. */
                    final Point next = iterator.next();
                    /* Indicate where the Knight currently is for future updates. */
                    prevKnight.set(((StackPane) grdBoard.getChildren().stream().filter(n -> GridPane.getColumnIndex(n) == next.x && GridPane.getRowIndex(n) == next.y).findAny().get()).getChildren());
                    prevKnight.get().add(imvKnight);
                    /* Draw a line from the previous location to the new one. */
                    final Point prev = prevLoc.get();
                    if (prev != null)
                    {
                        final Line l = new Line();
                        final NumberBinding bndHalf = Bindings.divide(cellSizeProperty, 2);
                        l.startXProperty().bind(Bindings.multiply(prev.x, cellSizeProperty).add(bndHalf));
                        l.startYProperty().bind(Bindings.multiply(prev.y, cellSizeProperty).add(bndHalf));
                        l.endXProperty().bind(Bindings.multiply(next.x, cellSizeProperty).add(bndHalf));
                        l.endYProperty().bind(Bindings.multiply(next.y, cellSizeProperty).add(bndHalf));
                        panArrows.getChildren().add(l);
                    }
                    prevLoc.set(next);
                }
            };
            timer.start();

            root.getSelectionModel().selectedItemProperty().addListener((observable1, oldValue1, newValue1) -> {
                if (oldValue1 == tabChessboard)
                    timer.stop();
            });
        });

        btnStart.setOnAction(e -> {
            final int columns = (int) sldColumns.getValue(),
                    rows = (int) sldRows.getValue(),
                    knightX = (int) sldKnightX.getValue() - 1,
                    knightY = (int) sldKnightY.getValue() - 1;
            boardProperty.set(new Board(columns, rows, knightX, knightY));

            /* Attempt to tour the board. */
            final BooleanProperty successProperty = new SimpleBooleanProperty();
            final LongProperty timeProperty = new SimpleLongProperty(-1l);
            t = new Thread(() ->
            {
                final long time_start = System.nanoTime();
                successProperty.set(boardProperty.get().attemptTour());
                timeProperty.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - time_start));
            });
            t.start();

            /* Disable the controls as long as the program is running. */
            final Node[] binds = new Node[]{sldColumns, sldRows, sldKnightX, sldKnightY, btnStart};
            for (final Node i : binds)
            {
                i.disableProperty().unbind();
                i.disableProperty().bind(boardProperty.get().busyProperty());
            }
            tabChessboard.disableProperty().bind(boardProperty.get().busyProperty());

            final DecimalFormat format = new DecimalFormat("#,###");
            final Random rand = new Random();
            final long increment = 50000l + rand.nextInt(50000);

            final String KNIGHTS_MOVES = "Total Knight Moves: %s.",
                    SOLVED = "Board solved in %d milliseconds.",
                    UNSOLVABLE = "Board is unsolvable.";
            boardProperty.get().movesProperty().addListener((observable, oldValue, newValue) ->
            {
                if ((long) newValue % increment == 0)
                    Platform.runLater(() -> lblMessage.setText(String.format(KNIGHTS_MOVES, format.format(newValue))));
            });
            timeProperty.addListener(observable -> {
                Platform.runLater(() ->
                        lblMessage.setText(successProperty.get()
                                           ? String.format(SOLVED, timeProperty.get()) : UNSOLVABLE));
            });
        });

        final Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setTitle("Knight's Tour");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> end());
        primaryStage.show();
    }

    public void end()
    {
        if (t != null)
            t.interrupt();
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
