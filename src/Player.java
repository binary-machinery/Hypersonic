import java.util.*;

class Position {
    int x;
    int y;

    Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    Position(Position other) {
        this.x = other.x;
        this.y = other.y;
    }

    @Override
    public String toString() {
        return "{" + x + "," + y + "}";
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Position)) {
            return false;
        }
        final Position other = (Position) obj;
        return x == other.x
                && y == other.y;
    }
}

class Boomer {
    static final int ENTITY_TYPE = 0;
    int id;
    Position position = new Position(0, 0);
    int bombsAvailable;
    int explosionRange;

    @Override
    public String toString() {
        return "Boomer {" + position + "," + bombsAvailable + "," + explosionRange + "}";
    }
}

class Bomb {
    static final int ENTITY_TYPE = 1;
    static final int COUNTDOWN = 8;
    Position position = new Position(0, 0);
    int timer;
    int explosionRange;
    boolean dangerCalculated = false;

    @Override
    public String toString() {
        return "Bomb {" + position + "," + timer + "," + explosionRange + "}";
    }
}

class Item {
    static final int ENTITY_CODE = 2;

    enum Type {
        ExtraRange(1),
        ExtraBomb(2);

        int code;

        Type(int code) {
            this.code = code;
        }
    }

    Position position = new Position(0, 0);
    Type type;
}

class Cell {
    enum Type {
        Floor('.'),
        Box('0'),
        BoxWithExtraRange('1'),
        BoxWithExtraBomb('2'),
        ExtraRange('R'),
        ExtraBomb('B'),
        Wall('X');

        char symbol;

        Type(char symbol) {
            this.symbol = symbol;
        }
    }

    static EnumSet<Type> BOX_SUBTYPES = EnumSet.of(Type.Box, Type.BoxWithExtraBomb, Type.BoxWithExtraRange);
    static EnumSet<Type> BONUS_SUBTYPES = EnumSet.of(Type.ExtraRange, Type.ExtraBomb);
    static EnumSet<Type> PASSABLE_SUBTYPES = EnumSet.of(Type.Floor, Type.ExtraRange, Type.ExtraBomb);
    static EnumSet<Type> NONPASSABLE_SUBTYPES = EnumSet.of(Type.Box, Type.BoxWithExtraRange, Type.BoxWithExtraBomb, Type.Wall);

    Position position;
    Type type = Type.Floor;
    int utility;
    boolean utilityCalculated = false;

    @Override
    public String toString() {
        return "" + type.symbol;
    }

    @Override
    public int hashCode() {
        // ignore utility
        int res = 0;
        res = 31 * res + position.hashCode();
        res = 31 * res + type.hashCode();
        return res;
    }

    @Override
    public boolean equals(Object obj) {
        // ignore utility
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Cell)) {
            return false;
        }
        final Cell other = (Cell) obj;
        return position.equals(other.position)
                && type == other.type;
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

class Target {
    enum Type {
        BombPlace,
        Bonus
    }

    Position position;
    int distance;
    int utility;
    Type type;

    @Override
    public String toString() {
        return "Target {" + position + "," + distance + "," + utility + "," + type + "}";
    }
}

class World {
    final Grid grid = new Grid();
    final Boomer player = new Boomer();
    final Map<Integer, Boomer> enemies = new HashMap<>(3);
    final List<Bomb> playerBombs = new ArrayList<>();
    final List<Bomb> enemyBombs = new ArrayList<>();
    final List<Bomb> allBombs = new ArrayList<>();
    final Planner planner = new Planner();
}

abstract class Action implements Comparable<Action> {
    int priority;
    int order;

    @Override
    public int compareTo(Action o) {
        final int priorityDelta = priority - o.priority;
        if (priorityDelta != 0) {
            return priorityDelta;
        }
        return (order - o.order);
    }

    abstract void execute();

    abstract boolean checkPreconditions();

    abstract boolean checkPostconditions();
}

class Move extends Action {
    private Boomer player;
    private Position targetPosition;

    Move(Position targetPosition, Boomer player) {
        this.player = player;
        this.targetPosition = targetPosition;
        priority = 5;
    }

    @Override
    void execute() {
        System.out.println("MOVE " + targetPosition.x + " " + targetPosition.y);
    }

    @Override
    boolean checkPreconditions() {
        return !player.position.equals(targetPosition);
    }

    @Override
    boolean checkPostconditions() {
        return player.position.equals(targetPosition);
    }

    @Override
    public String toString() {
        return "Move " + targetPosition;
    }
}

class SkipTurn extends Action {
    private Boomer player;

    SkipTurn(Boomer player) {
        this.player = player;
        priority = 5;
    }

    @Override
    void execute() {
        System.out.println("MOVE " + player.position.x + " " + player.position.y);
    }

    @Override
    boolean checkPreconditions() {
        return false;
    }

    @Override
    boolean checkPostconditions() {
        return true;
    }

    @Override
    public String toString() {
        return "SkipTurn at" + player.position;
    }
}

class PlaceBombAndMove extends Action {
    private boolean done = false;
    private Position targetPosition;
    private Boomer player;

    PlaceBombAndMove(Position targetPosition, Boomer player) {
        this.player = player;
        this.targetPosition = targetPosition;
        priority = 5;
    }

    @Override
    void execute() {
        System.out.println("BOMB " + targetPosition.x + " " + targetPosition.y);
        done = true;
    }

    @Override
    boolean checkPreconditions() {
        return player.position.equals(targetPosition);
    }

    @Override
    boolean checkPostconditions() {
        return done;
    }

    @Override
    public String toString() {
        return "PlaceBombAndMove " + targetPosition;
    }
}

class Planner {
    private final PriorityQueue<Action> actions = new PriorityQueue<>(10);
    private int orderCounter;

    void executeNext() {
        final List<Action> actionsToRemove = new ArrayList<>();
        for (Action action : actions) {
            if (action.checkPreconditions()) {
                System.err.println("Execute action: " + action);
                action.execute();
                if (action.checkPostconditions()) {
                    actionsToRemove.add(action);
                    System.err.println("Remove action: " + action);
                }
                break;
            }
        }
        actionsToRemove.forEach(actions::remove);
    }

    void clearFinished() {
        // for delayed consequences
        final List<Action> actionsToRemove = new ArrayList<>();
        for (Action action : actions) {
            if (action.checkPostconditions()) {
                actionsToRemove.add(action);
                System.err.println("Remove action: " + action);
            } else {
                break;
            }
        }
        actionsToRemove.forEach(actions::remove);
    }

    void add(Action action) {
        action.order = orderCounter++;
        actions.add(action);
        System.err.println("Action added: " + action);
    }

    boolean isEmpty() {
        return actions.isEmpty();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (Action action : actions) {
            sb.append(action.toString()).append("\n");
        }
        return sb.toString();
    }
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
        long time = System.nanoTime();
        while (true) {
            System.err.println("Time: " + (System.nanoTime() - time) / 1000000.0 + " ms");
            time = System.nanoTime();
            updateWorldState();
            in.nextLine();

            world.planner.clearFinished();

            final Set<Position> ignoredCells = new HashSet<>(2);
            for (Bomb bomb : world.playerBombs) {
                ignoredCells.add(bomb.position);
                getBoxesAffectedByExplosion(bomb.position, bomb.explosionRange, null).forEach(c -> ignoredCells.add(c.position));
                getItemsAffectedByExplosion(bomb.position, bomb.explosionRange, null).forEach(c -> ignoredCells.add(c.position));
            }
            for (Bomb bomb : world.enemyBombs) {
                ignoredCells.add(bomb.position);
                getBoxesAffectedByExplosion(bomb.position, bomb.explosionRange, null).forEach(c -> ignoredCells.add(c.position));
                getItemsAffectedByExplosion(bomb.position, bomb.explosionRange, null).forEach(c -> ignoredCells.add(c.position));
            }

            calculateCellsUtility(ignoredCells);
            System.err.println(world.grid.showUtility());
            final boolean[][] cells = calculateDangerousCells();
            {
                final int width = world.grid.width;
                final int height = world.grid.height;
                final StringBuilder sb = new StringBuilder(height * width);
                for (int rowIndex = 0; rowIndex < height; ++rowIndex) {
                    for (int columnIndex = 0; columnIndex < width; ++columnIndex) {
                        sb.append(cells[columnIndex][rowIndex] ? '*' : '.');
                    }
                    sb.append("\n");
                }
                System.err.println(sb.toString());
            }

            if (world.planner.isEmpty()) {
                int scanRange = (world.player.bombsAvailable > 0) ? Bomb.COUNTDOWN / 2 : Bomb.COUNTDOWN;
//                int scanRange = 50; // unlimited
                Target target = findNearestCellWithHighestUtility(scanRange, ignoredCells);
//            if (target == null) {
//                scanRange *= 2;
//                target = findNearestCellWithHighestUtility(scanRange, ignoredCells);
//            }
//            if (target == null) {
//                scanRange = 50;
//                target = findNearestCellWithHighestUtility(scanRange, ignoredCells);
//            }
                if (target == null) {
                    System.err.println("Empty target!");
                    world.planner.add(new SkipTurn(world.player));
                } else {
                    System.err.println(target);
                    world.planner.add(new Move(target.position, world.player));
                    if (target.type == Target.Type.BombPlace) {
                        world.planner.add(new PlaceBombAndMove(target.position, world.player));
                    }
                }
            }
            System.err.println(world.planner);

            world.planner.executeNext();
        }
    }

    void initWorld() {
        world.grid.width = in.nextInt();
        world.grid.height = in.nextInt();
        world.grid.clear();
        world.player.id = in.nextInt();
        in.nextLine();
    }

    void updateWorldState() {
        world.playerBombs.clear();
        world.enemyBombs.clear();
        world.allBombs.clear();
        world.grid.clear();
        for (int rowIndex = 0; rowIndex < world.grid.height; rowIndex++) {
            final String row = in.nextLine();
            for (int columnIndex = 0; columnIndex < world.grid.width; ++columnIndex) {
                final Cell cell = new Cell();
                cell.position = new Position(columnIndex, rowIndex);
                final char typeSymbol = row.charAt(columnIndex);
                if (typeSymbol == Cell.Type.Box.symbol) {
                    cell.type = Cell.Type.Box;
                } else if (typeSymbol == Cell.Type.BoxWithExtraRange.symbol) {
                    cell.type = Cell.Type.BoxWithExtraRange;
                } else if (typeSymbol == Cell.Type.BoxWithExtraBomb.symbol) {
                    cell.type = Cell.Type.BoxWithExtraBomb;
                } else if (typeSymbol == Cell.Type.Wall.symbol) {
                    cell.type = Cell.Type.Wall;
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
                    if (owner == world.player.id) {
                        world.player.position.x = x;
                        world.player.position.y = y;
                        world.player.bombsAvailable = param1;
                        world.player.explosionRange = param2;
                    } else {
                        if (!world.enemies.containsKey(owner)) {
                            world.enemies.put(owner, new Boomer());
                        }
                        final Boomer enemy = world.enemies.get(owner);
                        enemy.position.x = x;
                        enemy.position.y = y;
                        enemy.bombsAvailable = param1;
                        enemy.explosionRange = param2;
                    }
                    break;
                case Bomb.ENTITY_TYPE:
                    final Bomb bomb = new Bomb();
                    if (owner == world.player.id) {
                        world.playerBombs.add(bomb);
                    } else {
                        world.enemyBombs.add(bomb);
                    }
                    bomb.position.x = x;
                    bomb.position.y = y;
                    bomb.timer = param1;
                    bomb.explosionRange = param2;
                    break;
                case Item.ENTITY_CODE:
                    final Item item = new Item();
                    item.position.x = x;
                    item.position.y = y;
                    if (param1 == Item.Type.ExtraRange.code) {
                        item.type = Item.Type.ExtraRange;
                        world.grid.cells[x][y].type = Cell.Type.ExtraRange;
                    } else if (param1 == Item.Type.ExtraBomb.code) {
                        item.type = Item.Type.ExtraBomb;
                        world.grid.cells[x][y].type = Cell.Type.ExtraBomb;
                    }
                    break;
                default:
                    break;
            }
        }
        world.allBombs.addAll(world.playerBombs);
        world.allBombs.addAll(world.enemyBombs);
    }

    void calculateUtilityForCell(final Cell cell, final Set<Position> ignoredCells) {
        if (Cell.PASSABLE_SUBTYPES.contains(cell.type)) {
            final Set<Cell> boxes = getBoxesAffectedByExplosion(cell.position, world.player.explosionRange - 1, ignoredCells);
            cell.utility = boxes.size();
        }
        if (Cell.BONUS_SUBTYPES.contains(cell.type)) {
            if (ignoredCells.contains(cell.position)) {
                return;
            }
            final int distanceToBonus = calculateDistance(world.player.position, cell.position);
            cell.utility += Math.max(4 - distanceToBonus, 0);
//            if (cell.type == Cell.Type.ExtraRange && world.player.explosionRange > 4) {
//                cell.utility = 0;
//            }
//            if (cell.type == Cell.Type.ExtraBomb && (world.player.bombsAvailable + world.playerBombs.size()) > 2) {
//                cell.utility = 0;
//            }
        }
    }

    void calculateCellsUtility(final Set<Position> ignoredCells) {
        final Queue<Cell> queue = new LinkedList<>();
        final Cell cells[][] = world.grid.cells;
        final Cell start = cells[world.player.position.x][world.player.position.y];
        queue.add(start);
        while (!queue.isEmpty()) {
            final Cell cell = queue.poll();
            if (cell.utilityCalculated) {
                continue;
            }
            if (!ignoredCells.contains(cell.position)) {
                calculateUtilityForCell(cell, ignoredCells);
            }
            cell.utilityCalculated = true;

            // add adjacent cells to queue
            final Position pos = cell.position;
            if (pos.x - 1 >= 0) {
                final Cell adjacentCell = cells[pos.x - 1][pos.y];
                if (Cell.PASSABLE_SUBTYPES.contains(adjacentCell.type) && !adjacentCell.utilityCalculated) {
                    queue.add(adjacentCell);
                }
            }
            if (pos.x + 1 < world.grid.width) {
                final Cell adjacentCell = cells[pos.x + 1][pos.y];
                if (Cell.PASSABLE_SUBTYPES.contains(adjacentCell.type) && !adjacentCell.utilityCalculated) {
                    queue.add(adjacentCell);
                }
            }
            if (pos.y - 1 >= 0) {
                final Cell adjacentCell = cells[pos.x][pos.y - 1];
                if (Cell.PASSABLE_SUBTYPES.contains(adjacentCell.type) && !adjacentCell.utilityCalculated) {
                    queue.add(adjacentCell);
                }
            }
            if (pos.y + 1 < world.grid.height) {
                final Cell adjacentCell = cells[pos.x][pos.y + 1];
                if (Cell.PASSABLE_SUBTYPES.contains(adjacentCell.type) && !adjacentCell.utilityCalculated) {
                    queue.add(adjacentCell);
                }
            }
        }
    }

    boolean calculateExplosionAreaForBomb(Bomb bomb, boolean explosionMap[][]) {
        final int width = world.grid.width;
        final int height = world.grid.height;
        final int columnMin = Math.max(bomb.position.x - bomb.explosionRange + 1, 0);
        final int columnMax = Math.min(bomb.position.x + bomb.explosionRange - 1, width - 1);
        final int rowMin = Math.max(bomb.position.y - bomb.explosionRange + 1, 0);
        final int rowMax = Math.min(bomb.position.y + bomb.explosionRange - 1, height - 1);
        boolean newExplodedCellsAdded = false;
        for (int columnIndex = columnMin; columnIndex <= columnMax; ++columnIndex) {
            if (!explosionMap[columnIndex][bomb.position.y]) {
                newExplodedCellsAdded = true;
                explosionMap[columnIndex][bomb.position.y] = true;
            }
        }
        for (int rowIndex = rowMin; rowIndex <= rowMax; ++rowIndex) {
            if (!explosionMap[bomb.position.x][rowIndex]) {
                newExplodedCellsAdded = true;
                explosionMap[bomb.position.x][rowIndex] = true;
            }
        }
        bomb.dangerCalculated = true;
        return newExplodedCellsAdded;
    }

    boolean[][] calculateDangerousCells() {
        final int width = world.grid.width;
        final int height = world.grid.height;
        boolean explosionMap[][] = new boolean[width][height];
        final List<Bomb> bombs = world.allBombs;
        bombs.stream()
                .filter(b -> b.timer == 2) // get bomb which will explode in next turn
                .forEach(b -> calculateExplosionAreaForBomb(b, explosionMap));
        long cellsChanged;
        do {
            cellsChanged = bombs.stream()
                    .filter(b -> !b.dangerCalculated)
                    .filter(b -> explosionMap[b.position.x][b.position.y]) // filter bombs affected by another explosions
                    .map(b -> calculateExplosionAreaForBomb(b, explosionMap))
                    .filter(changed -> changed == true)
                    .count();
        } while (cellsChanged > 0);
        return explosionMap;
    }

    Set<Cell> getObjectsAffectedByExplosion(Position bombPosition, int bombRange, final Set<Position> ignoredCells, final EnumSet<Cell.Type> filter) {
        final int width = world.grid.width;
        final int height = world.grid.height;
        final int explosionColumnLeft = Math.max(bombPosition.x - bombRange, 0);
        final int explosionColumnRight = Math.min(bombPosition.x + bombRange, width - 1);
        final int explosionRowTop = Math.max(bombPosition.y - bombRange, 0);
        final int explosionRowBottom = Math.min(bombPosition.y + bombRange, height - 1);
        final Set<Cell> boxes = new HashSet<>(4);
        for (int explosionColumnIndex = bombPosition.x - 1; explosionColumnIndex >= explosionColumnLeft; --explosionColumnIndex) {
            final Cell cell = world.grid.cells[explosionColumnIndex][bombPosition.y];
            if (filter.contains(cell.type)) {
                if (ignoredCells == null || !ignoredCells.contains(cell.position)) {
                    boxes.add(cell);
                }
            }
            if (cell.type != Cell.Type.Floor) {
                break;
            }
        }
        for (int explosionColumnIndex = bombPosition.x + 1; explosionColumnIndex <= explosionColumnRight; ++explosionColumnIndex) {
            final Cell cell = world.grid.cells[explosionColumnIndex][bombPosition.y];
            if (filter.contains(cell.type)) {
                if (ignoredCells == null || !ignoredCells.contains(cell.position)) {
                    boxes.add(cell);
                }
            }
            if (cell.type != Cell.Type.Floor) {
                break;
            }
        }
        for (int explosionRowIndex = bombPosition.y - 1; explosionRowIndex >= explosionRowTop; --explosionRowIndex) {
            final Cell cell = world.grid.cells[bombPosition.x][explosionRowIndex];
            if (filter.contains(cell.type)) {
                if (ignoredCells == null || !ignoredCells.contains(cell.position)) {
                    boxes.add(cell);
                }
            }
            if (cell.type != Cell.Type.Floor) {
                break;
            }
        }
        for (int explosionRowIndex = bombPosition.y + 1; explosionRowIndex <= explosionRowBottom; ++explosionRowIndex) {
            final Cell cell = world.grid.cells[bombPosition.x][explosionRowIndex];
            if (filter.contains(cell.type)) {
                if (ignoredCells == null || !ignoredCells.contains(cell.position)) {
                    boxes.add(cell);
                }
            }
            if (cell.type != Cell.Type.Floor) {
                break;
            }
        }
        return boxes;
    }

    Set<Cell> getBoxesAffectedByExplosion(Position bombPosition, int bombRange, final Set<Position> ignoredCells) {
        return getObjectsAffectedByExplosion(bombPosition, bombRange, ignoredCells, Cell.BOX_SUBTYPES);
    }

    Set<Cell> getItemsAffectedByExplosion(Position bombPosition, int bombRange, final Set<Position> ignoredCells) {
        return getObjectsAffectedByExplosion(bombPosition, bombRange, ignoredCells, Cell.BONUS_SUBTYPES);
    }

    Target findNearestCellWithHighestUtility(int scanRange, final Set<Position> ignoredCells) {
        final int width = world.grid.width;
        final int height = world.grid.height;
        final int playerX = world.player.position.x;
        final int playerY = world.player.position.y;
        final int columnDeltaMin = Math.max(-scanRange, 0 - playerX);
        final int columnDeltaMax = Math.min(scanRange, width - 1 - playerX);
        boolean targetFound = false;
        int distanceToMax = 0;
        Position positionOfMax = new Position(0, 0);
        int max = 0;
        for (int columnDelta = columnDeltaMin; columnDelta <= columnDeltaMax; ++columnDelta) {
            final int columnIndex = playerX + columnDelta;
            final int rowDeltaMin = Math.max(-scanRange + columnDelta, 0 - playerY);
            final int rowDeltaMax = Math.min(scanRange - columnDelta, height - 1 - playerY);
            for (int rowDelta = rowDeltaMin; rowDelta <= rowDeltaMax; ++rowDelta) {
                final int rowIndex = playerY + rowDelta;
                final Position currentPosition = new Position(columnIndex, rowIndex);
                if (ignoredCells.contains(currentPosition)) {
                    continue;
                }
                final int currentUtility = world.grid.cells[columnIndex][rowIndex].utility;
                if (currentUtility < max) {
                    continue;
                }
                final int currentDistance = calculateDistance(playerX, playerY, columnIndex, rowIndex);
                if (currentUtility > max || (currentUtility == max && currentDistance < distanceToMax)) {
                    positionOfMax = currentPosition;
                    max = currentUtility;
                    distanceToMax = currentDistance;
                    targetFound = true;
                }
            }
        }
        if (targetFound) {
            final Target target = new Target();
            target.position = positionOfMax;
            target.distance = distanceToMax;
            target.utility = max;
            if (Cell.BONUS_SUBTYPES.contains(world.grid.cells[target.position.x][target.position.y].type)) {
                target.type = Target.Type.Bonus;
            } else {
                target.type = Target.Type.BombPlace;
            }
            return target;
        } else {
            return null;
        }
    }

    int calculateDistance(int x1, int y1, int x2, int y2) {
        // manhattan distance without obstacles
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    int calculateDistance(Position pos1, Position pos2) {
        // manhattan distance without obstacles
        return Math.abs(pos1.x - pos2.x) + Math.abs(pos1.y - pos2.y);
    }

//    double findCosBetweenVectors(Position origin, Position vec1, Position vec2) {
//
//    }
}