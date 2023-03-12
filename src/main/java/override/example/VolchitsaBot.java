package override.example;


import override.logic.Direction;
import override.logic.GameState;
import override.logic.LabyrinthPlayer;

import java.util.*;

public class VolchitsaBot implements LabyrinthPlayer {
    private int number;
    private boolean ready = false;
    private int maxSum = Integer.MIN_VALUE;
    private PriorityQueue<Node> pq;
    private Map<Integer, Integer> routes; // next: current
    private List<Integer> path;
    private Mover mover;
    private boolean onStart = true;

    private static class Node implements Comparable<Node> {
        public int currentValue;
        public boolean visited = false;
        public int absoluteValue;
        public int i = 0;
        public int j = 0;

        public Node(int absoluteValue) {
            this.absoluteValue = absoluteValue;
            this.currentValue = absoluteValue;
        }

        public void setAbsoluteValue(int v) {
            absoluteValue = v;
            currentValue = absoluteValue;
        }

        public boolean isValid() {
            return currentValue >= 0;
        }

        public void setPosition(int i, int j) {
            this.i = i;
            this.j = j;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return currentValue == node.currentValue && visited == node.visited && absoluteValue == node.absoluteValue && i == node.i && j == node.j;
        }

        @Override
        public int hashCode() {
            return Objects.hash(currentValue, visited, absoluteValue, i, j);
        }


        @Override
        public int compareTo(Node o) {
            return -Integer.compare(currentValue, o.currentValue);
        }
    }

    private static class Mover {
        private final List<Integer> path;
        private int currMoveCounter = 0;
        private final List<Direction> moves = new ArrayList<>();
        private final int botNum;

        public Mover(List<Integer> path, int botNum) {
            this.path = path;
            this.botNum = botNum;
            calcMoves();
        }

        private void calcMoves() {
            if (path.size() == 0) {
                return;
            }

            int currPos = path.get(0);
            for (int i = 1; i < path.size(); i++) {
                int currPosCol = convertNumberToCol(currPos, GameState.WIDTH);
                int currPosRow = convertNumberToRow(currPos, GameState.HEIGHT);

                int pathCol = convertNumberToCol(path.get(i), GameState.WIDTH);
                int pathRow = convertNumberToRow(path.get(i), GameState.HEIGHT);

                if (currPosRow == pathRow && currPosCol == pathCol) {
                    moves.add(Direction.NONE);
                } else if (currPosRow == pathRow && currPosCol - 1 == pathCol) {
                    moves.add(Direction.LEFT);
                } else if (currPosRow == pathRow && currPosCol + 1 == pathCol) {
                    moves.add(Direction.RIGHT);
                } else if (currPosRow - 1 == pathRow && currPosCol == pathCol) {
                    moves.add(Direction.UP);
                } else if (currPosRow + 1 == pathRow && currPosCol == pathCol) {
                    moves.add(Direction.BOTTOM);
                } else {
                    moves.add(Direction.NONE);
                }

                currPos = path.get(i);
            }
        }

        private boolean isCollision(GameState state, Direction move) {
            int currPos = path.get(currMoveCounter);
            int opponentNumber = botNum == -1 ? -2 : -1;
            switch (move) {
                case RIGHT:
                    return state.getMap()[convertNumberToRow(currPos, GameState.HEIGHT)][convertNumberToCol(currPos, GameState.WIDTH) + 1] == opponentNumber;
                case LEFT:
                    return state.getMap()[convertNumberToRow(currPos, GameState.HEIGHT)][convertNumberToCol(currPos, GameState.WIDTH) - 1] == opponentNumber;
                case UP:
                    return state.getMap()[convertNumberToRow(currPos, GameState.HEIGHT) - 1][convertNumberToCol(currPos, GameState.WIDTH)] == opponentNumber;
                case BOTTOM:
                    return state.getMap()[convertNumberToRow(currPos, GameState.HEIGHT) + 1][convertNumberToCol(currPos, GameState.WIDTH)] == opponentNumber;
                default:
                    return false;
            }
        }

        public Direction getNextMove(GameState state) {
            if (currMoveCounter >= moves.size()) {
                return Direction.NONE;
            }
            Direction move = moves.get(currMoveCounter);
            if (isCollision(state, move)) {
                return Direction.NONE;
            }
            currMoveCounter++;
            return move;
        }

        public int getCurrPosition() {
            return path.get(currMoveCounter);
        }

        public boolean needRecalculate(GameState state) {
            return state.getRoundsToEnd() > 0 && currMoveCounter >= moves.size();
        }
    }

    @Override
    public void takeYourNumber(int number) {
        this.number = number;
    }

    @Override
    public Direction step(GameState gameState) {
        if (!ready || mover.needRecalculate(gameState) || findPathSum(gameState.getMap()) == 0) {
            int pos = onStart ? getStartingPos() : mover.getCurrPosition();
            prepareBFS(gameState, pos);
            mover = new Mover(path, number);
            ready = true;
            onStart = false;
        }

        return mover.getNextMove(gameState);
    }

    @Override
    public String getTelegramNick() {
        return "@Irena_kup";
    }

    private int getStartingPos() {
        return number == -1 ? 0 : (GameState.WIDTH * GameState.HEIGHT) - 1;
    }

    public void prepareBFS(GameState state, int startingPos) {
        routes = new HashMap<>();
        path = new ArrayList<>();
        pq = new PriorityQueue<>();
        maxSum = Integer.MIN_VALUE;
        Node[][] adj = generateAdjacencyMatrix(state.getMap());
        path = bfs(state.getMap(), state.getRoundsToEnd(), adj, adj[startingPos][startingPos]);
    }

    private Node[][] generateAdjacencyMatrix(int[][] map) {
        int c = GameState.WIDTH * GameState.HEIGHT;
        Node[][] adj = new Node[c][c];
        for (int i = 0; i < c; i++) {
            for (int j = 0; j < c; j++) {
                adj[i][j] = new Node(-1);
                // node is visited for itself
                if (i == j) {
                    adj[i][j].visited = true;
                }
                adj[i][j].setPosition(i, j);
            }
        }
        for (int i = 0; i < GameState.HEIGHT; i++) {
            for (int j = 0; j < GameState.WIDTH; j++) {
                if (map[i][j] == -16) { // TODO add const
                    continue;
                }
                int v = -1;
                if (j + 1 < GameState.WIDTH) {
                    if (map[i][j + 1] >= 0) {
                        v = map[i][j + 1];
                    }
                    adj[convertIndexesToNumber(i, j, GameState.WIDTH)][convertIndexesToNumber(i, j + 1, GameState.WIDTH)].setAbsoluteValue(v);
                    v = -1;
                }
                if (j - 1 >= 0) {
                    if (map[i][j - 1] >= 0) {
                        v = map[i][j - 1];
                    }
                    adj[convertIndexesToNumber(i, j, GameState.WIDTH)][convertIndexesToNumber(i, j - 1, GameState.WIDTH)].setAbsoluteValue(v);
                    v = -1;
                }
                if (i + 1 < GameState.HEIGHT) {
                    if (map[i + 1][j] >= 0) {
                        v = map[i + 1][j];
                    }
                    adj[convertIndexesToNumber(i, j, GameState.WIDTH)][convertIndexesToNumber(i + 1, j, GameState.WIDTH)].setAbsoluteValue(v);
                    v = -1;
                }
                if (i - 1 >= 0) {
                    if (map[i - 1][j] >= 0) {
                        v = map[i - 1][j];
                    }
                    adj[convertIndexesToNumber(i, j, GameState.WIDTH)][convertIndexesToNumber(i - 1, j, GameState.WIDTH)].setAbsoluteValue(v);
                }
            }
        }
        return adj;
    }

    private static int convertIndexesToNumber(int row, int col, int colNum) {
        return col + colNum * row;
    }

    private static int convertNumberToRow(int n, int rowNumber) {
        return n / rowNumber;
    }

    private static int convertNumberToCol(int n, int colNumber) {
        return n % colNumber;
    }

    private List<Integer> bfs(int[][] map, int moves, Node[][] adj, Node start) {
        pq.add(start);
        routes.put(start.i, -1);
        while (!pq.isEmpty()) {
            Node curr = pq.poll();
            for (int j = 0; j < adj[curr.i].length; j++) {
                if (!routes.containsKey(j) && adj[curr.i][j].isValid()) {
                    pq.add(adj[j][curr.i]);
                    routes.put(j, curr.i);
                }
            }
        }
        List<List<Integer>> paths = new ArrayList<>();
        int c = 0;
        int minLen = Integer.MAX_VALUE;
        int minIdx = 0;
        List<Integer> sums = new ArrayList<>();
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                Integer curr = convertIndexesToNumber(i, j, map[i].length);
                if (!routes.containsKey(curr)) {
                    // skip walls
                    continue;
                }
                paths.add(new ArrayList<>());
                int sum = 0;
                paths.get(c).add(curr);
                curr = routes.get(curr);
                while (curr != -1) {
                    paths.get(c).add(curr);
                    sum += map[convertNumberToRow(curr, map.length)][convertNumberToCol(curr, map[i].length)];
                    curr = routes.get(curr);
                }

                if (sum >= maxSum && paths.get(c).size() <= moves) {
                    maxSum = sum;
                    sums.add(sum);
                    c++;
                } else {
                    paths.remove(c);
                }
            }
        }

        for (int i = 0; i < paths.size(); i++) {
            if (sums.get(i) == maxSum && paths.get(i).size() < minLen) {
                minLen = paths.get(i).size();
                minIdx = i;
            }
        }

        Collections.reverse(paths.get(minIdx));
        return paths.get(minIdx);
    }

    private int findPathSum(int[][] m) {
        int s = 0;
        for (Integer integer : path) {
            s += m[convertNumberToRow(integer, GameState.HEIGHT)][convertNumberToCol(integer, GameState.WIDTH)];
        }
        return s;
    }
}