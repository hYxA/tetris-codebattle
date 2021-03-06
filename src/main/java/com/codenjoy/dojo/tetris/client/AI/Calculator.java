package com.codenjoy.dojo.tetris.client.AI;

import com.codenjoy.dojo.services.FigureRotator;
import com.codenjoy.dojo.services.Point;
import com.codenjoy.dojo.services.PointImpl;
import com.codenjoy.dojo.services.Rotation;
import com.codenjoy.dojo.tetris.client.Board;
import com.codenjoy.dojo.tetris.client.GlassBoard;
import com.codenjoy.dojo.tetris.model.Elements;

import java.util.*;

/**
 * @author Aydar Rafikov
 */
public class Calculator {

    public static BoardSimulator simulator;

    public static int betterRotate;
    public static Point betterCenter = new PointImpl(0,0);
    public static double betterCost;

    private static int firstElementX;
    private static int firstElementY;
    private static int firstElementRotate;

    private static Point[] getPoints(int x, int y, int rotate, Elements type) {
        switch (rotate) {
            case 1:
                return FigureRotator.predictCurrentFigurePoints(Rotation.CLOCKWIZE_90, new PointImpl(x, y), type);
            case 2:
                return FigureRotator.predictCurrentFigurePoints(Rotation.CLOCKWIZE_180, new PointImpl(x, y),  type);
            case 3:
                return FigureRotator.predictCurrentFigurePoints(Rotation.CLOCKWIZE_270, new PointImpl(x, y),  type);
            default:
                return FigureRotator.predictCurrentFigurePoints( Rotation.CLOCKWIZE_0, new PointImpl(x, y), type);
        }
    }

    public static void calculateBetterCenter(String layer, Queue<Elements> elements, Point[] points) {
        if (!Bot.allOMode) {
            simulator = new BoardSimulator();
        } else {
            simulator = new BoardSimulatorWithAllOLogic();
        }

        simulator.setLayers(layer, points);
        betterCost = 100000;
        calculateCost(elements, true, 0);
    }

    private static void calculateCost(Queue<Elements> elements, boolean first, double startCost) {
        Elements myElement = elements.poll();

        for (int rotate = 0; rotate < myElement.getCountRotates(); rotate++) {

            List<Integer> possibleCenterX = new ArrayList<>();
            for (int x = 0; x < 18; x++) {
                if (isCanPlace(x, 17, rotate, myElement)) {
                    possibleCenterX.add(x);
                }
            }

            for (Integer x : possibleCenterX) {
                int y = getMinimalCenterY(x, 17, rotate, myElement);

                if (first) {
                    firstElementX = x;
                    firstElementY = y;
                    firstElementRotate = rotate;
                }

                Point[] points = getPoints(x, y, rotate, myElement);

                String backup = simulator.getBackup();
                simulator.put(points);

                if (elements.isEmpty()) {
                    double cost = simulator.getCost(startCost + getMyCost(points));

                    if (cost < betterCost) {
                        betterCenter.move(firstElementX, firstElementY);
                        betterRotate = firstElementRotate;
                        betterCost = cost;
                    }
                } else {
                    calculateCost(new LinkedList<>(elements), false, startCost + getMyCost(points));
                }

                simulator.restore(backup);
            }
        }
    }

    private static boolean isCanPlace(int x, int y, int rotate, Elements element) {
        Point[] points = getPoints(x, y, rotate, element);
        for (Point point : points) {
            if (point.getY() < 0 ||
                    point.getX() < 0 || point.getX() > 17) {
                return false;
            }
            // Если не (клетка_свободна или клетка_за_экраном)
            if (!(simulator.isFree(point.getX(), point.getY()) || point.getY() > 17)) {
                return false;
            }
        }
        return true;
    }

    private static int getMinimalCenterY(int x, int y, int rotate, Elements element) {
        while (isCanPlace(x, y, rotate, element)) {
            y--;
        }
        return y+1;
    }

    private static int getMinimumY(Point[] points) {
        int minimumY = 100;
        for (Point point : points) {
            if (point.getY() < minimumY) {
                minimumY = point.getY();
            }
        }
        return minimumY;
    }

    private static double getMyCost(Point[] points) {
        double myCost = 0;

        // Общая высота установки фигур
        myCost += getMinimumY(points) * 10; // 12.88

        return myCost;
    }
}
