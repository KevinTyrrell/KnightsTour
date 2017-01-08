import javafx.beans.property.*;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Project Name: KnightsTour
 * Author: Kevin
 * Date: Dec 29, 2016
 * Description:
 * Board of which the Knight mauevers around.
 */
public final class Board
{
    /** The Knight's location on the board. */
    private final ObjectProperty<Point> knightLoc;
    /** The history of the Knight's movement on the board. */
    private final Stack<Point> knightHistory;
    /** Columns and rows of the Board. */
    private final IntegerProperty columnsProperty, rowsProperty;
    /** Array representation of the ChessBoard. */
    private final SlotState[][] states;
    /** Property to track if the Board is currently busy. */
    private final BooleanProperty busyProperty = new SimpleBooleanProperty();
    /** Property to track the number of moves the Knight has performed. */
    private final LongProperty movesProperty = new SimpleLongProperty();

    /** Default locations if none are specified. */
    private static final int DEFAULT_COLUMNS = 8, DEFAULT_ROWS = 8,
            DEFAULT_KNIGHT_X = 0, DEFAULT_KNIGHT_Y = 0;

    /* State of a given slot on the board. */
    public enum SlotState
    {
        KNIGHT,
        VISITED,
        NONE
    }

    public Board(final int columns, final int rows, final int knightX, final int knightY)
    {
        states = new SlotState[columns][rows];
        for (final SlotState[] i : states)
            Arrays.fill(i, SlotState.NONE);
        columnsProperty = new SimpleIntegerProperty(columns);
        rowsProperty = new SimpleIntegerProperty(rows);
        /* Place the knight in its default location. */
        if (knightX < 0 || knightX >= columns || knightY < 0 || knightY >= rows)
            throw new IllegalArgumentException(
                    String.format("Unable to place the Knight at coordinates: %d,%d.", knightX, knightY));
        states[knightX][knightY] = SlotState.KNIGHT;
        final Point loc = new Point(knightX, knightY);
        knightLoc = new SimpleObjectProperty<>(loc);
        knightHistory = new Stack<>();
        knightHistory.push(loc);
    }

    public Board()
    {
        this(DEFAULT_COLUMNS, DEFAULT_ROWS);
    }

    public Board(final int columns, final int rows)
    {
        this(columns, rows, DEFAULT_KNIGHT_X, DEFAULT_KNIGHT_Y);
    }

    /**
     * Moves the Knight to a given location on the board.
     * @param x - X coordinate to move to.
     * @param y - Y coordinate to move to.
     */
    public void moveKnight(final int x, final int y)
    {
        if (states[x][y] != SlotState.NONE)
            throw new IllegalArgumentException("The space: " + x + "," + y + " "
                    + (states[x][y] == SlotState.KNIGHT ? "already contains the Knight"
                                                        : "has already been visited") + ".");
        /* Make sure that the Knight is legally allowed to move to this location. */
        boolean error = x < 0 || x >= columnsProperty.get() || y < 0 || y >= rowsProperty.get();
        for (final Iterator<Point> iter = getKnightsChoices().iterator(); !error && iter.hasNext();)
        {
            final Point choice = iter.next();
            if (choice.x == x && choice.y == y)
                break;
            if (!iter.hasNext())
                error = true;
        }
        if (error)
            throw new IllegalArgumentException(
                    String.format("The Knight is unable to move to the coordinates: %d,%d.", x, y));
        final Point newLoc = new Point(x, y), current = knightLoc.get();
        states[current.x][current.y] = SlotState.VISITED;
        states[newLoc.x][newLoc.y] = SlotState.KNIGHT;
        knightLoc.set(newLoc);
        knightHistory.push(newLoc);
    }

    /**
     * Un-does the last Knight's movement.
     */
    public void undoKnight()
    {
        if (knightHistory.isEmpty())
            throw new IllegalStateException("Cannot undo the Knight as there are no previous moves!");
        final Point knight = knightHistory.pop();
        states[knight.x][knight.y] = SlotState.NONE;
        final Point last = knightHistory.peek();
        states[last.x][last.y] = SlotState.KNIGHT;
        knightLoc.set(last);
    }

    /**
     * Attempt to complete a tour of the board.
     * @return - Whether or not the tour was successful.
     */
    public boolean attemptTour()
    {
        busyProperty.set(true);
        final Stack<Point> stack = new Stack<>();
        stack.push(knightLoc.get());

        while (!stack.isEmpty())
        {
            /* Allow exit of the tour if the Thread is ended. */
            if (Thread.currentThread().isInterrupted())
                break;
            movesProperty.set(movesProperty.get() + 1l);
            final List<Point> available = getKnightsChoices();
            if (available.isEmpty())
            {
                /* Check if we have completed the tour. */
                if (isTourOver())
                    break;
                do
                {
                    /* A stack size of one here means solving is impossible. */
                    if (stack.size() <= 1)
                    {
                        busyProperty.set(false);
                        return false;
                    }
                    /* Delete the current location. */
                    stack.pop();
                    /* Move the Knight back to the previous location. */
                    undoKnight();
                } while (stack.peek().equals(knightLoc.get()));
            }
            else
                stack.addAll(available);

            /* Move the knight to the next location on the stack. */
            final Point next = stack.peek();
            moveKnight(next.x, next.y);
        }

        busyProperty.set(false);
        return true;
    }

    /* List of possible x,y locations that the knight can visit on the unit circle. */
    private final int[] xLoc = new int[] { -1, 1, 2, 2, 1, -1, -2, -2 };
    private final int[] yLoc = new int[] { -2, -2, -1, 1, 2, 2, 1, -1 };

    /**
     * Gets the possible moves that the Knight has available to him.
     * A knight can only move forward two spaces, then left or right once.
     * If the knight has no available moves, an empty list will be returned.
     * @return - Choices the knights have available to them.
     */
    public List<Point> getKnightsChoices()
    {
        final List<Point> lstPossibleMoves = new LinkedList<>();

        /* Location of the Knight. */
        final Point knight = knightLoc.get();

        for (int i = 0; i < xLoc.length; i++)
        {
            final int x = knight.x + xLoc[i],
                    y = knight.y + yLoc[i];
            if (x >= 0 && x < states.length
                    && y >= 0 && y < states[x].length
                    && states[x][y] == SlotState.NONE)
                lstPossibleMoves.add(new Point(x, y));
        }

        return lstPossibleMoves;
    }

    /**
     * Check if all slots on the board are either Visited or Occupied.
     * @return - If the tour is over.
     */
    public boolean isTourOver()
    {
        for (final SlotState[] column : states)
            for (final SlotState slot : column)
                if (slot == SlotState.NONE)
                    return false;
        return true;
    }

    public Stack<Point> getKnightHistory()
    {
        return knightHistory;
    }

    public int getColumns()
    {
        return states.length;
    }

    public int getRows()
    {
        return states[0].length;
    }

    public boolean isBusy()
    {
        return busyProperty.get();
    }

    public BooleanProperty busyProperty()
    {
        return busyProperty;
    }

    public LongProperty movesProperty()
    {
        return movesProperty;
    }
}
