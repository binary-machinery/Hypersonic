import java.util.*;

class Boomer {
    static final int ENTITY_TYPE = 0;
    int id;
    int x;
    int y;
    int bombsAvailable;
    int explosionRange;

    @Override
    public String toString() {
        return "Boomer {" + x + "," + y + "," + bombsAvailable + "," + explosionRange + "}";
    }
}

class Bomb {
    static final int ENTITY_TYPE = 1;
    static final int COUNTDOWN = 8;
    int x;
    int y;
    int timer;
    int explosionRange;

    @Override
    public String toString() {
        return "Bomb {" + x + "," + y + "," + timer + "," + explosionRange + "}";
    }
}

class Cell {
    enum Type {
        Floor('.'),
        Box('0');

        char symbol;

        Type(char symbol) {
            this.symbol = symbol;
        }
    }

    Type type = Type.Floor;
    int utility = 0;

    @Override
    public String toString() {
        return "" + type.symbol;
    }
}

class Grid {
    int height;
    int width;
    Cell cells[][];

    void clear() {
        cells = new Cell[width][height];
    }

    String showTypes() {
        final StringBuilder sb = new StringBuilder(height * width);
        for (int rowIndex = 0; rowIndex < height; ++rowIndex) {
            for (int columnIndex = 0; columnIndex < width; ++columnIndex) {
                sb.append(cells[columnIndex][rowIndex].type.symbol);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    String showUtility() {
        final StringBuilder sb = new StringBuilder(height * width);
        for (int rowIndex = 0; rowIndex < height; ++rowIndex) {
            for (int columnIndex = 0; columnIndex < width; ++columnIndex) {
                sb.append(cells[columnIndex][rowIndex].utility);
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}

class Position {
    int x;
    int y;

    Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "{" + x + "," + y + "}";
    }
}

class World {
    Grid grid;
    Boomer player = new Boomer();
    Boomer enemy = new Boomer();
    Bomb playerBomb;
    Bomb enemyBomb;
}

// main class must be Player
class Player {

    private final Scanner in = new Scanner(System.in);
    private final World world = new World();

    public static void main(String args[]) {
        final Player game = new Player();
        game.initWorld();
        game.run();
    }

    void run() {
        // game loop
        while (true) {
            updateWorldState();
            in.nextLine();
            calculateCellsWeight();

            System.err.println(world.grid.showTypes());
            System.err.println(world.grid.showUtility());
            System.err.println(world.player);
            System.err.println(world.enemy);
            System.err.println(world.playerBomb);
            System.err.println(world.enemyBomb);

            final Position target = findNearestCellWithHighestUtility(Bomb.COUNTDOWN + 1);

            System.out.println("MOVE " + target.x + " " + target.y);
        }
    }

    void initWorld() {
        world.grid = new Grid();
        world.grid.width = in.nextInt();
        world.grid.height = in.nextInt();
        world.grid.clear();
        world.player.id = in.nextInt();
        in.nextLine();
    }

    void updateWorldState() {
        world.playerBomb = null;
        world.enemyBomb = null;
        world.grid.clear();
        for (int rowIndex = 0; rowIndex < world.grid.height; rowIndex++) {
            final String row = in.nextLine();
            for (int columnIndex = 0; columnIndex < world.grid.width; ++columnIndex) {
                final Cell cell = new Cell();
                final char typeSymbol = row.charAt(columnIndex);
                if (typeSymbol == Cell.Type.Box.symbol) {
                    cell.type = Cell.Type.Box;
                }
                world.grid.cells[columnIndex][rowIndex] = cell;
            }
        }
        int entities = in.nextInt();
        for (int i = 0; i < entities; i++) {
            int entityType = in.nextInt();
            int owner = in.nextInt();
            int x = in.nextInt();
            int y = in.nextInt();
            int param1 = in.nextInt();
            int param2 = in.nextInt();
            switch (entityType) {
                case Boomer.ENTITY_TYPE:
                    Boomer boomer = (owner == world.player.id) ? world.player : world.enemy;
                    boomer.x = x;
                    boomer.y = y;
                    boomer.bombsAvailable = param1;
                    boomer.explosionRange = param2;
                    break;
                case Bomb.ENTITY_TYPE:
                    Bomb bomb = new Bomb();
                    if (owner == world.player.id) {
                        world.playerBomb = bomb;
                    } else {
                        world.enemyBomb = bomb;
                    }
                    bomb.x = x;
                    bomb.y = y;
                    bomb.timer = param1;
                    bomb.explosionRange = param2;
                    break;
                default:
                    break;
            }
        }
    }

    void calculateCellsWeight() {
        final int width = world.grid.width;
        final int height = world.grid.height;
        final int bombRange = world.player.explosionRange - 1;
        for (int columnIndex = 0; columnIndex < width; ++columnIndex) {
            for (int rowIndex = 0; rowIndex < height; ++rowIndex) {
                final Cell cell = world.grid.cells[columnIndex][rowIndex];
                if (cell.type != Cell.Type.Floor) {
                    // TODO: process non floor cells
                    continue;
                }
                final int explosionColumnLeft = Math.max(columnIndex - bombRange, 0);
                final int explosionColumnRight = Math.min(columnIndex + bombRange, width - 1);
                final int explosionRowTop = Math.max(rowIndex - bombRange, 0);
                final int explosionRowBottom = Math.min(rowIndex + bombRange, height - 1);
                int crashedBoxCount = 0;
                for (int explosionColumnIndex = columnIndex - 1; explosionColumnIndex >= explosionColumnLeft; --explosionColumnIndex) {
                    if (world.grid.cells[explosionColumnIndex][rowIndex].type == Cell.Type.Box) {
                        ++crashedBoxCount;
                        break;
                    }
                }
                for (int explosionColumnIndex = columnIndex + 1; explosionColumnIndex <= explosionColumnRight; ++explosionColumnIndex) {
                    if (world.grid.cells[explosionColumnIndex][rowIndex].type == Cell.Type.Box) {
                        ++crashedBoxCount;
                        break;
                    }
                }
                for (int explosionRowIndex = rowIndex - 1; explosionRowIndex >= explosionRowTop; --explosionRowIndex) {
                    if (world.grid.cells[columnIndex][explosionRowIndex].type == Cell.Type.Box) {
                        ++crashedBoxCount;
                        break;
                    }
                }
                for (int explosionRowIndex = rowIndex + 1; explosionRowIndex <= explosionRowBottom; ++explosionRowIndex) {
                    if (world.grid.cells[columnIndex][explosionRowIndex].type == Cell.Type.Box) {
                        ++crashedBoxCount;
                        break;
                    }
                }
                cell.utility = crashedBoxCount;
            }
        }
    }

    Position findNearestCellWithHighestUtility(int scanRange) {
        final int width = world.grid.width;
        final int height = world.grid.height;
        final int playerX = world.player.x;
        final int playerY = world.player.y;
        final int columnDeltaMin = Math.max(-scanRange, 0 - playerX);
        final int columnDeltaMax = Math.min(scanRange, width - 1 - playerX);
        int distanceToMax = 0;
        int maxX = 0;
        int maxY = 0;
        int max = 0;
        for (int columnDelta = columnDeltaMin; columnDelta <= columnDeltaMax; ++columnDelta) {
            final int columnIndex = playerX + columnDelta;
            final int rowDeltaMin = Math.max(-scanRange + columnDelta, 0 - playerY);
            final int rowDeltaMax = Math.min(scanRange - columnDelta, height - 1 - playerY);
            for (int rowDelta = rowDeltaMin; rowDelta <= rowDeltaMax; ++rowDelta) {
                final int rowIndex = playerY + rowDelta;
                final int currentUtility = world.grid.cells[columnIndex][rowIndex].utility;
                if (currentUtility < max) {
                    continue;
                }
                final int currentDistance = calculateDistance(playerX, playerY, columnIndex, rowIndex);
                if (currentUtility > max || (currentUtility == max && currentDistance < distanceToMax)) {
                    maxX = columnIndex;
                    maxY = rowIndex;
                    max = currentUtility;
                    distanceToMax = currentDistance;
                }
            }
        }
        return new Position(maxX, maxY);
    }

    int calculateDistance(int x1, int y1, int x2, int y2) {
        // manhattan distance without obstacles
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }
}