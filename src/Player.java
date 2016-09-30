import com.sun.org.apache.xpath.internal.operations.Bool;

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

    Position add(Position other) {
        return new Position(x + other.x, y + other.y);
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

    Bomb createBomb(Position position) {
        Bomb b = new Bomb();
        b.position = position;
        b.timer = Bomb.COUNTDOWN;
        b.explosionRange = explosionRange;
        return b;
    }
}

class Bomb {
    static final int EXPLODE_NEXT_TURN = 2;
    static final int ALREADY_EXPLODED = 1;
    static final int NO_EXPLOSION = 0;

    static final int ENTITY_TYPE = 1;
    static final int COUNTDOWN = 8;

    Position position = new Position(0, 0);
    int timer;
    int explosionRange;

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
        Wall('X'),
        Bomb('*');

        char symbol;

        Type(char symbol) {
            this.symbol = symbol;
        }
    }

    static EnumSet<Type> BOX_SUBTYPES = EnumSet.of(Type.Box, Type.BoxWithExtraBomb, Type.BoxWithExtraRange);
    static EnumSet<Type> BONUS_SUBTYPES = EnumSet.of(Type.ExtraRange, Type.ExtraBomb);
    static EnumSet<Type> PASSABLE_SUBTYPES = EnumSet.of(Type.Floor, Type.ExtraRange, Type.ExtraBomb);
    static EnumSet<Type> NONPASSABLE_SUBTYPES = EnumSet.of(Type.Box, Type.BoxWithExtraRange, Type.BoxWithExtraBomb, Type.Wall, Type.Bomb);
    static EnumSet<Type> EXPLOSION_STOPPERS = EnumSet.of(Type.Box, Type.BoxWithExtraRange, Type.BoxWithExtraBomb, Type.Wall, Type.ExtraBomb, Type.ExtraRange, Type.Bomb);
    static EnumSet<Type> DESTROYABLE_OBJECTS = EnumSet.of(Type.Box, Type.BoxWithExtraRange, Type.BoxWithExtraBomb, Type.ExtraBomb, Type.ExtraRange);

    Position position;
    Type type = Type.Floor;

    @Override
    public String toString() {
        return "" + position + ", " + type;
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
    List<Cell> asList;

    void clear() {
        cells = new Cell[width][height];
        asList = new ArrayList<>(width * height);
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

    String showUtility(IntegerMap utilityMap) {
        final StringBuilder sb = new StringBuilder(height * width);
        sb.append("Utility").append("\n");
        for (int rowIndex = 0; rowIndex < height; ++rowIndex) {
            for (int columnIndex = 0; columnIndex < width; ++columnIndex) {
                sb.append(utilityMap.values[columnIndex][rowIndex].value);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    String showDistanceFromPlayer(PathMap pathMap) {
        final StringBuilder sb = new StringBuilder(height * width);
        sb.append("Distance").append("\n");
        for (int rowIndex = 0; rowIndex < height; ++rowIndex) {
            for (int columnIndex = 0; columnIndex < width; ++columnIndex) {
                final int distance = pathMap.values[columnIndex][rowIndex].distance;
                sb.append(
                        (distance == Integer.MAX_VALUE)
                                ? "."
                                : Math.min(9, distance));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    String showExplosionMap(IntegerMap explosionMap) {
        final StringBuilder sb = new StringBuilder(height * width);
        sb.append("Explosions").append("\n");
        for (int rowIndex = 0; rowIndex < height; ++rowIndex) {
            for (int columnIndex = 0; columnIndex < width; ++columnIndex) {
                final int timeToExplosion = explosionMap.values[columnIndex][rowIndex].value;
                if (timeToExplosion == Bomb.NO_EXPLOSION) {
                    sb.append(Cell.NONPASSABLE_SUBTYPES.contains(cells[columnIndex][rowIndex].type) ? "X" : ".");
                } else {
                    sb.append(timeToExplosion);
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    String showSafetyMap(IntegerMap safetyMap) {
        final StringBuilder sb = new StringBuilder(height * width);
        sb.append("Safety").append("\n");
        for (int rowIndex = 0; rowIndex < height; ++rowIndex) {
            for (int columnIndex = 0; columnIndex < width; ++columnIndex) {
                final IntegerParameter p = safetyMap.values[columnIndex][rowIndex];
                sb.append(p.value);
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}

class IntegerParameter {
    int value;
}

class BooleanParameter {
    boolean value;
}

class PathParameter {
    int distance = Integer.MAX_VALUE;
    Cell previousCell;
}

class IntegerMap {
    final IntegerParameter[][] values;
    final List<IntegerParameter> asList;

    private IntegerMap(int width, int height, int defaultValue) {
        values = new IntegerParameter[width][height];
        asList = new ArrayList<>(width * height);
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                final IntegerParameter p = new IntegerParameter();
                p.value = defaultValue;
                values[x][y] = p;
                asList.add(p);
            }
        }
    }

    static IntegerMap createUtilityMap(int width, int height) {
        return new IntegerMap(width, height, 0);
    }

    static IntegerMap createExplosionMap(int width, int height) {
        return new IntegerMap(width, height, 0);
    }

    static IntegerMap createSafetyMap(int width, int height) {
        return new IntegerMap(width, height, Bomb.ALREADY_EXPLODED);
    }
}

class BooleanMap {
    final BooleanParameter[][] values;
    final List<BooleanParameter> asList;

    private BooleanMap(int width, int height, boolean defaultValue) {
        values = new BooleanParameter[width][height];
        asList = new ArrayList<>(width * height);
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                final BooleanParameter p = new BooleanParameter();
                p.value = defaultValue;
                values[x][y] = p;
                asList.add(p);
            }
        }
    }

    static BooleanMap createTrueMap(int width, int height) {
        return new BooleanMap(width, height, true);
    }

    static BooleanMap createFalseMap(int width, int height) {
        return new BooleanMap(width, height, false);
    }
}

class PathMap {
    final PathParameter[][] values;
    final List<PathParameter> asList;

    private PathMap(int width, int height) {
        values = new PathParameter[width][height];
        asList = new ArrayList<>(width * height);
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                final PathParameter p = new PathParameter();
                values[x][y] = p;
                asList.add(p);
            }
        }
    }

    static PathMap createPathMap(int width, int height) {
        return new PathMap(width, height);
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

    static final int LOW_PRIORITY = 3;
    static final int NORMAL_PRIORITY = 2;
    static final int HIGH_PRIORITY = 1;

    int priority = NORMAL_PRIORITY;
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
    private boolean done = false;

    SkipTurn(Boomer player) {
        this.player = player;
    }

    @Override
    void execute() {
        System.out.println("MOVE " + player.position.x + " " + player.position.y);
        done = true;
    }

    @Override
    boolean checkPreconditions() {
        return true;
    }

    @Override
    boolean checkPostconditions() {
        return done;
    }

    @Override
    public String toString() {
        return "SkipTurn at" + player.position;
    }
}

class PlaceBomb extends Action {
    private boolean bombPlaced = false;
    private Position targetPosition;
    private Boomer player;

    PlaceBomb(Position targetPosition, Boomer player) {
        this.player = player;
        this.targetPosition = targetPosition;
    }

    @Override
    void execute() {
        if (player.position.equals(targetPosition)) {
            System.out.println("BOMB " + targetPosition.x + " " + targetPosition.y);
            bombPlaced = true;
        } else {
            System.out.println("MOVE " + targetPosition.x + " " + targetPosition.y);
        }
    }

    @Override
    boolean checkPreconditions() {
        return !bombPlaced;
    }

    @Override
    boolean checkPostconditions() {
        return bombPlaced;
    }

    @Override
    public String toString() {
        return "PlaceBomb " + targetPosition;
    }
}

class PlaceBombAndGoTo extends Action {

    private Position target;
    private boolean done = false;

    PlaceBombAndGoTo(Position target) {
        this.target = target;
    }

    @Override
    void execute() {
        System.out.println("BOMB " + target.x + " " + target.y);
        done = true;
    }

    @Override
    boolean checkPreconditions() {
        return true;
    }

    @Override
    boolean checkPostconditions() {
        return done;
    }

    @Override
    public String toString() {
        return "PlaceBombAndGoTo " + target;
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

class TimeCalculator {
    private long value;
    boolean enabled = true;

    void start() {
        value = System.nanoTime();
    }

    double getTime_ms() {
        double ms = (System.nanoTime() - value) / 1000000.0;
        value = System.nanoTime();
        return ms;
    }

    void showTime(String prefix) {
        if (enabled) {
            System.err.println(prefix + ": " + getTime_ms() + " ms");
        }
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
        final TimeCalculator timeCalculator = new TimeCalculator();
        timeCalculator.enabled = false;
        while (true) {
            timeCalculator.start();
            updateWorldState();
            timeCalculator.showTime("Update world");

            in.nextLine();

            world.planner.clearFinished();

            final IntegerMap utilityMap = IntegerMap.createUtilityMap(world.grid.width, world.grid.height);
            final PathMap pathMap = PathMap.createPathMap(world.grid.width, world.grid.height);
            final IntegerMap explosionMap = IntegerMap.createExplosionMap(world.grid.width, world.grid.height);
            final IntegerMap safetyMap = IntegerMap.createSafetyMap(world.grid.width, world.grid.height);
            timeCalculator.showTime("Maps allocation");

            calculateExplosionMap(world.allBombs, explosionMap);
            timeCalculator.showTime("Explosion map");

            final Set<Position> ignoredCells = new HashSet<>();
            final Set<Cell> destroyedObjects = new HashSet<>();
            for (Bomb bomb : world.allBombs) {
                destroyedObjects.addAll(
                        calculateDestroyedObjects(
                                bomb.position,
                                bomb.explosionRange,
                                Cell.DESTROYABLE_OBJECTS,
                                ignoredCells
                        )
                );
            }
            world.allBombs.forEach(b -> ignoredCells.add(b.position));
            destroyedObjects.forEach(cell -> ignoredCells.add(cell.position));
            timeCalculator.showTime("Model destroyed objects");

            calculateCellsUtilityAndPathsAndSafetyMap(ignoredCells, explosionMap, utilityMap, pathMap, safetyMap);
            timeCalculator.showTime("Utility, paths and safety");

            if (world.planner.isEmpty()) {
                Cell targetCell = null;
                int modelIterationCount = 3;
                while (modelIterationCount-- > 0) {
                    targetCell = findNearestCellWithHighestUtility(4, ignoredCells, utilityMap, pathMap);
                    if (targetCell != null) {
                        if (targetCell.position.equals(world.player.position)) {
                            final IntegerMap utilityMapModel = IntegerMap.createUtilityMap(world.grid.width, world.grid.height);
                            final PathMap pathMapModel = PathMap.createPathMap(world.grid.width, world.grid.height);
                            final IntegerMap explosionMapModel = IntegerMap.createExplosionMap(world.grid.width, world.grid.height);
                            final IntegerMap safetyMapModel = IntegerMap.createSafetyMap(world.grid.width, world.grid.height);
                            modelNewBomb(
                                    world.player.createBomb(world.player.position),
                                    ignoredCells,
                                    utilityMapModel,
                                    pathMapModel,
                                    explosionMapModel,
                                    safetyMapModel
                            );
                            if (!isSafetyMapIsEmpty(safetyMapModel)) {
                                world.planner.add(new PlaceBombAndGoTo(targetCell.position));
                                break;
                            } else {
                                utilityMap.values[targetCell.position.x][targetCell.position.y].value = 0;
                            }
                        } else {
                            final List<Cell> path = getPathTo(targetCell, pathMap);
                            path.forEach(c -> world.planner.add(new Move(c.position, world.player)));
                            world.planner.add(new PlaceBombAndGoTo(targetCell.position));
                            break;
                        }
                    } else {
                        world.planner.add(new SkipTurn(world.player));
                        break;
                    }
                }
            }
            if (world.planner.isEmpty()) {
                world.planner.add(new SkipTurn(world.player));
            }
            timeCalculator.showTime("Target search");

            checkExplosionsAndDodge(safetyMap);
            timeCalculator.showTime("Check explosions and dodge");

            System.err.println(world.grid.showUtility(utilityMap));
            System.err.println(world.grid.showDistanceFromPlayer(pathMap));
            System.err.println(world.grid.showExplosionMap(explosionMap));
            System.err.println(world.grid.showSafetyMap(safetyMap));
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
                world.grid.asList.add(cell);
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
        world.allBombs.forEach(b -> world.grid.cells[b.position.x][b.position.y].type = Cell.Type.Bomb);
    }

    void calculateUtilityForCell(
            final Cell cell,
            final Set<Position> ignoredCells,
            final IntegerMap utilityMap
    ) {
        final IntegerParameter utility = utilityMap.values[cell.position.x][cell.position.y];
        if (Cell.PASSABLE_SUBTYPES.contains(cell.type)) {
            final Set<Cell> boxes = calculateDestroyedObjects(cell.position, world.player.explosionRange, Cell.BOX_SUBTYPES, ignoredCells);
            utility.value = 0;
            boxes.forEach(c -> {
                switch (c.type) {
                    case Box:
                        utility.value += 1;
                        break;
                    case BoxWithExtraBomb:
                        utility.value += 2;
                        break;
                    case BoxWithExtraRange:
                        utility.value += 2;
                        break;
                    default:
                        break;
                }
            });
//            if (cell.timerToExplosion != Bomb.NO_EXPLOSION) {
//                cell.utility -= Math.max(0, 5 - cell.timerToExplosion);
//            }
//            final int safety = safetyMap.values[cell.position.x][cell.position.y].value;
//            if (safety != Bomb.NO_EXPLOSION) {
//                cell.utility -= Math.max(0, 7 - safety);
//            }
//            final List<Position> adjacentPositions = generateAdjacentPositions(cell.position);
//            cell.utility += (int) adjacentPositions
//                    .stream()
//                    .map(p -> world.grid.cells[p.x][p.y].type)
//                    .filter(t -> Cell.PASSABLE_SUBTYPES.contains(t))
//                    .count();
        }
//        if (Cell.BONUS_SUBTYPES.contains(cell.type)) {
//            if (ignoredCells.contains(cell.position)) {
//                return;
//            }
//            final int distanceToBonus = calculateDistance(world.player.position, cell.position);
//            utility.value = Math.max(6 - distanceToBonus, 0);
//        }
        utility.value = Math.max(0, utility.value);
    }

    void calculateCellsUtilityAndPathsAndSafetyMap(
            final Set<Position> ignoredCells,
            final IntegerMap explosionMap,
            final IntegerMap utilityMap,
            final PathMap pathMap,
            final IntegerMap safetyMap
    ) {
        final PriorityQueue<Cell> queue = new PriorityQueue<>(
                world.grid.width * world.grid.height,
                (Comparator<Cell>) (o1, o2) -> pathMap.values[o1.position.x][o1.position.y].distance - pathMap.values[o2.position.x][o2.position.y].distance
        );
        final BooleanMap utilityCalculated = BooleanMap.createFalseMap(world.grid.width, world.grid.height);
        final BooleanMap pathCalculated = BooleanMap.createFalseMap(world.grid.width, world.grid.height);
        final Cell cells[][] = world.grid.cells;
        final Cell start = cells[world.player.position.x][world.player.position.y];
        pathMap.values[start.position.x][start.position.y].distance = 0;
        safetyMap.values[start.position.x][start.position.y].value = explosionMap.values[start.position.x][start.position.y].value;
        queue.add(start);
        while (!queue.isEmpty()) {
            final Cell currentCell = queue.poll();
            final PathParameter currentPathParameter = pathMap.values[currentCell.position.x][currentCell.position.y];
            final BooleanParameter currentCellUtilityCalculated = utilityCalculated.values[currentCell.position.x][currentCell.position.y];
            if (!currentCellUtilityCalculated.value && !ignoredCells.contains(currentCell.position)) {
                calculateUtilityForCell(currentCell, ignoredCells, utilityMap);
            }

            currentCellUtilityCalculated.value = true;
            pathCalculated.values[currentCell.position.x][currentCell.position.y].value = true;

            // add adjacent cells to queue
            final List<Position> adjacentPositions = generateAdjacentPositions(currentCell.position);
            adjacentPositions
                    .forEach(p -> {
                        final Cell adjacentCell = cells[p.x][p.y];
                        if (pathCalculated.values[p.x][p.y].value) {
                            return;
                        }
                        final PathParameter adjacentPathParameter = pathMap.values[p.x][p.y];
                        if (Cell.PASSABLE_SUBTYPES.contains(adjacentCell.type)) {
                            final int newDistance = currentPathParameter.distance + 1;
                            final int explosionTime = explosionMap.values[p.x][p.y].value;
                            final IntegerParameter adjacentSafety = safetyMap.values[p.x][p.y];
                            if ((explosionTime != Bomb.NO_EXPLOSION)
                                    && (explosionTime - newDistance) == Bomb.ALREADY_EXPLODED) {
                                // player will be dead if go this way, ignore it
                                adjacentSafety.value = Bomb.ALREADY_EXPLODED;
                                return;
                            }
                            if (newDistance < adjacentPathParameter.distance) {
                                adjacentPathParameter.distance = newDistance;
                                adjacentPathParameter.previousCell = currentCell;
                                if (explosionTime == Bomb.NO_EXPLOSION) {
                                    adjacentSafety.value = Bomb.NO_EXPLOSION;
                                } else {
                                    adjacentSafety.value = Math.max(0, explosionTime - newDistance);
                                }
                            }
                            // remove and add -> force to recalculate priority
                            if (queue.contains(adjacentCell)) { // O(n) :(
                                queue.remove(adjacentCell); // O(n) :(
                            }
                            queue.add(adjacentCell);
                        }
                    });
        }
    }

    void calculateExplosionMap(final List<Bomb> bombs, final IntegerMap explosionMap) {
        bombs.stream()
                .sorted((o1, o2) -> o1.timer - o2.timer)
                .forEach(b -> calculateExplosionMapForBomb(b, explosionMap));
    }

    void checkExplosionWaveFromBomb(final Bomb bomb, final Position position, final IntegerMap explosionMap) {
        final int stateAtBombPosition = explosionMap.values[bomb.position.x][bomb.position.y].value;
        final int timer = (stateAtBombPosition == Bomb.NO_EXPLOSION) ? bomb.timer : stateAtBombPosition; // check for chain effect
        final int previousValue = explosionMap.values[position.x][position.y].value;
        if (previousValue == Bomb.NO_EXPLOSION || previousValue > timer) {
            explosionMap.values[position.x][position.y].value = timer;
        }
    }

    Set<Cell> calculateDestroyedObjects(
            final Position bombPosition,
            final int explosionRange,
            final EnumSet<Cell.Type> filter,
            final Set<Position> ignoredCells
    ) {
        final int width = world.grid.width;
        final int height = world.grid.height;

        final Set<Cell> destroyedObjects = new HashSet<>(4);
        final List<Position> directions = new ArrayList<>(4);
        directions.add(new Position(1, 0));
        directions.add(new Position(-1, 0));
        directions.add(new Position(0, 1));
        directions.add(new Position(0, -1));
        directions.forEach(dir -> {
            int explosionRadius = explosionRange - 1;
            Position pos = bombPosition;
            while (explosionRadius > 0) {
                --explosionRadius;
                pos = pos.add(dir);
                if ((pos.x < 0) || (pos.y < 0) || (pos.x >= width) || (pos.y >= height)) {
                    break; // end of map
                }
                final Cell cell = world.grid.cells[pos.x][pos.y];
                if (filter.contains(cell.type) && !ignoredCells.contains(cell.position)) {
                    destroyedObjects.add(cell);
                }
                if (Cell.EXPLOSION_STOPPERS.contains(cell.type)) {
                    break;
                }
            }
        });
        return destroyedObjects;
    }

    void calculateExplosionMapForBomb(final Bomb bomb, final IntegerMap explosionMap) {
        checkExplosionWaveFromBomb(bomb, bomb.position, explosionMap);
        final List<Position> directions = new ArrayList<>(4);
        directions.add(new Position(1, 0));
        directions.add(new Position(-1, 0));
        directions.add(new Position(0, 1));
        directions.add(new Position(0, -1));
        directions.forEach(dir -> {
            int explosionRadius = bomb.explosionRange - 1;
            Position pos = bomb.position;
            while (explosionRadius > 0) {
                --explosionRadius;
                pos = pos.add(dir);
                if ((pos.x < 0) || (pos.y < 0) || (pos.x >= world.grid.width) || (pos.y >= world.grid.height)) {
                    break; // end of map
                }
                final Cell cell = world.grid.cells[pos.x][pos.y];
                checkExplosionWaveFromBomb(bomb, cell.position, explosionMap);
                if (Cell.EXPLOSION_STOPPERS.contains(cell.type)) {
                    break;
                }
            }
        });
    }

    Cell findNearestCellWithHighestUtility(int scanRange, final Set<Position> ignoredCells, final IntegerMap utilityMap, final PathMap pathMap) {
        return world.grid.asList.stream()
                .filter(c -> pathMap.values[c.position.x][c.position.y].distance <= scanRange)
                .filter(c -> !ignoredCells.contains(c.position))
                .max((o1, o2) -> utilityMap.values[o1.position.x][o1.position.y].value - utilityMap.values[o2.position.x][o2.position.y].value)
                .orElse(null);
    }

    void checkExplosionsAndDodge(final IntegerMap safetyMap) {
        final Position playerPos = world.player.position;
        final List<Position> adjacentPositions = generateAdjacentPositions(playerPos);
        final Cell[][] cells = world.grid.cells;
        final Cell playersCell = cells[playerPos.x][playerPos.y];
        final IntegerParameter safety = safetyMap.values[playerPos.x][playerPos.y];
        if (safety.value == Bomb.EXPLODE_NEXT_TURN) {
            System.err.println("Player's position will explode next turn!");
            final Cell dodgeCell = adjacentPositions
                    .stream()
                    .map(p -> cells[p.x][p.y])
                    .max((o1, o2) -> safetyMap.values[o1.position.x][o1.position.y].value - safetyMap.values[o2.position.x][o2.position.y].value)
                    .orElse(playersCell);
            System.err.println("Cell to dodge: " + dodgeCell);
            final Move dodge = new Move(dodgeCell.position, world.player);
            dodge.priority = Action.HIGH_PRIORITY;
            world.planner.add(dodge);
        } else {
            adjacentPositions
                    .stream()
                    .filter(p -> safetyMap.values[p.x][p.y].value == Bomb.EXPLODE_NEXT_TURN)
                    .findAny()
                    .ifPresent(c -> {
                        final SkipTurn skip = new SkipTurn(world.player);
                        skip.priority = Action.HIGH_PRIORITY;
                        world.planner.add(skip);
                    });
        }
    }

    void modelNewBomb(
            Bomb bomb,
            Set<Position> ignoredCells,
            final IntegerMap utilityMap,
            final PathMap pathMap,
            final IntegerMap explosionMap,
            final IntegerMap safetyMap
    ) {
        final List<Bomb> bombs = new ArrayList<>(world.allBombs.size() + 1);
        world.allBombs.forEach(bombs::add);
        bombs.add(bomb);
        calculateExplosionMap(bombs, explosionMap);
        calculateCellsUtilityAndPathsAndSafetyMap(ignoredCells, explosionMap, utilityMap, pathMap, safetyMap);
    }

    List<Position> generateAdjacentPositions(Position center) {
        final List<Position> adjacentPositions = new ArrayList<>(4);
        if (center.x - 1 >= 0) {
            adjacentPositions.add(new Position(center.x - 1, center.y));
        }
        if (center.x + 1 < world.grid.width) {
            adjacentPositions.add(new Position(center.x + 1, center.y));
        }
        if (center.y - 1 >= 0) {
            adjacentPositions.add(new Position(center.x, center.y - 1));
        }
        if (center.y + 1 < world.grid.height) {
            adjacentPositions.add(new Position(center.x, center.y + 1));
        }
        return adjacentPositions;
    }

    List<Cell> getPathTo(final Cell targetCell, final PathMap pathMap) {
        final PathParameter targetPathParameter = pathMap.values[targetCell.position.x][targetCell.position.y];
        final List<Cell> path = new ArrayList<>(targetPathParameter.distance);
        if (targetPathParameter.previousCell == null) {
            // player is already on the target cell
            return path; // empty path
        }
        Cell nextCell = targetCell;
        PathParameter nextPathParameter = targetPathParameter;
        do {
            path.add(nextCell);
            nextCell = nextPathParameter.previousCell;
            nextPathParameter = pathMap.values[nextCell.position.x][nextCell.position.y];
        } while (nextPathParameter.previousCell != null);
        Collections.reverse(path);
        return path;
    }

    boolean isCellWithoutExplosionExists(IntegerMap explosionMap) {
        return explosionMap.asList
                .stream()
                .filter(c -> c.value == Bomb.NO_EXPLOSION)
                .map(c -> true)
                .findAny()
                .orElse(false);
    }

    boolean isSafetyMapIsEmpty(IntegerMap safetyMap) {
        return safetyMap.asList
                .stream()
                .filter(c -> c.value == Bomb.NO_EXPLOSION)
                .map(c -> false)
                .findAny()
                .orElse(true);
    }
}