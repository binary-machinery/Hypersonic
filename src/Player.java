import java.util.*;
import java.util.stream.Collectors;

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

    @Override
    public String toString() {
        return "" + position;
    }

    @Override
    public int hashCode() {
        return position.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Cell)) {
            return false;
        }
        final Cell other = (Cell) obj;
        return position.equals(other.position);
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

    String showTypes(TypeMap typeMap) {
        final StringBuilder sb = new StringBuilder(height * width);
        for (int rowIndex = 0; rowIndex < height; ++rowIndex) {
            for (int columnIndex = 0; columnIndex < width; ++columnIndex) {
                sb.append(typeMap.values[columnIndex][rowIndex].value.symbol);
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

    String showExplosionMap(IntegerMap explosionMap, TypeMap typeMap) {
        final StringBuilder sb = new StringBuilder(height * width);
        sb.append("Explosions").append("\n");
        for (int rowIndex = 0; rowIndex < height; ++rowIndex) {
            for (int columnIndex = 0; columnIndex < width; ++columnIndex) {
                final int timeToExplosion = explosionMap.values[columnIndex][rowIndex].value;
                if (timeToExplosion == Bomb.NO_EXPLOSION) {
                    sb.append(Cell.NONPASSABLE_SUBTYPES.contains(typeMap.values[columnIndex][rowIndex].value) ? "X" : ".");
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

class TypeParameter {
    Cell.Type value = Cell.Type.Floor;
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

    IntegerParameter at(Position pos) {
        return values[pos.x][pos.y];
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

    BooleanParameter at(Position pos) {
        return values[pos.x][pos.y];
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

    PathParameter at(Position pos) {
        return values[pos.x][pos.y];
    }

    static PathMap createPathMap(int width, int height) {
        return new PathMap(width, height);
    }
}

class TypeMap {
    final TypeParameter[][] values;
    final List<TypeParameter> asList;

    private TypeMap(int width, int height) {
        values = new TypeParameter[width][height];
        asList = new ArrayList<>(width * height);
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                final TypeParameter p = new TypeParameter();
                values[x][y] = p;
                asList.add(p);
            }
        }
    }

    private TypeMap(int width, int height, TypeMap other) {
        values = new TypeParameter[width][height];
        asList = new ArrayList<>(width * height);
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                final TypeParameter p = new TypeParameter();
                p.value = other.values[x][y].value;
                values[x][y] = p;
                asList.add(p);
            }
        }
    }

    TypeParameter at(Position pos) {
        return values[pos.x][pos.y];
    }

    TypeMap getDeepCopy(int width, int height) {
        return new TypeMap(width, height, this);
    }

    ;

    static TypeMap createTypeMap(int width, int height) {
        return new TypeMap(width, height);
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
    private String comment = "";

//    SkipTurn(Boomer player) {
//        this.player = player;
//    }

    SkipTurn(Boomer player, String comment) {
        this.player = player;
        this.comment = comment;
    }

    @Override
    void execute() {
        System.out.println("MOVE " + player.position.x + " " + player.position.y + " " + comment);
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
        return "SkipTurn at" + player.position + " (" + comment + ")";
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
    private Boomer player;
    private boolean done = false;

    PlaceBombAndGoTo(Position target, Boomer player) {
        this.target = target;
        this.player = player;
    }

    @Override
    void execute() {
        if (player.bombsAvailable > 0) {
            System.out.println("BOMB " + target.x + " " + target.y);
            done = true;
        } else {
            System.out.println("MOVE " + player.position.x + " " + player.position.y);
        }
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

            final TypeMap typeMap = TypeMap.createTypeMap(world.grid.width, world.grid.height);
            final IntegerMap utilityMap = IntegerMap.createUtilityMap(world.grid.width, world.grid.height);
            final PathMap pathMap = PathMap.createPathMap(world.grid.width, world.grid.height);
            final IntegerMap explosionMap = IntegerMap.createExplosionMap(world.grid.width, world.grid.height);
            final IntegerMap safetyMap = IntegerMap.createSafetyMap(world.grid.width, world.grid.height);
            timeCalculator.showTime("Maps allocation");

            updateWorldState(typeMap);
            timeCalculator.showTime("Update world");

            in.nextLine();

            world.planner.clearFinished();

            calculateExplosionMap(world.allBombs, typeMap, explosionMap);
            timeCalculator.showTime("Explosion map");

            final Set<Cell> willBeDestroyedObjects = new HashSet<>();
            willBeDestroyedObjects.addAll(
                    world.grid.asList
                            .stream()
                            .filter(c -> Cell.DESTROYABLE_OBJECTS.contains(typeMap.at(c.position).value))
                            .filter(c -> explosionMap.at(c.position).value >= Bomb.ALREADY_EXPLODED)
                            .collect(Collectors.toSet())
            );
            timeCalculator.showTime("Model destroyed objects");

            calculateCellsUtilityAndPathsAndSafetyMap(
                    world.player.position,
                    willBeDestroyedObjects,
                    typeMap,
                    explosionMap,
                    utilityMap,
                    pathMap,
                    safetyMap
            );
            timeCalculator.showTime("Utility, paths and safety");

            System.err.println(world.grid.showTypes(typeMap));
            System.err.println(world.grid.showUtility(utilityMap));

            if (world.planner.isEmpty()) {
                Cell targetCell = null;
                int modelIterationCount = 5;
                while (modelIterationCount-- > 0) {
                    targetCell = findNearestCellWithHighestUtility(4, utilityMap, pathMap);
                    System.err.println("Target cell: " + targetCell);
                    if (targetCell != null) {
                        final Position targetPosition = targetCell.position;
                        final List<Position> adjacentPositions = generateAdjacentPositions(targetPosition, Cell.PASSABLE_SUBTYPES, typeMap);
                        int maxSafetyCellCount = 0;
                        Cell cellToRetreat = null;
                        for (int i = 0; i < adjacentPositions.size(); ++i) {
                            final Position adjacentPosition = adjacentPositions.get(i);
                            System.err.println("Check adjacent position: " + adjacentPosition);
                            final TypeMap typeMapModel = typeMap.getDeepCopy(world.grid.width, world.grid.height);
                            final IntegerMap utilityMapModel = IntegerMap.createUtilityMap(world.grid.width, world.grid.height);
                            final PathMap pathMapModel = PathMap.createPathMap(world.grid.width, world.grid.height);
                            final IntegerMap explosionMapModel = IntegerMap.createExplosionMap(world.grid.width, world.grid.height);
                            final IntegerMap safetyMapModel = IntegerMap.createSafetyMap(world.grid.width, world.grid.height);
                            modelNewBomb(
                                    world.player.createBomb(targetPosition),
                                    adjacentPosition,
                                    typeMapModel,
                                    utilityMapModel,
                                    pathMapModel,
                                    explosionMapModel,
                                    safetyMapModel
                            );
                            final int safetyCellCount = getSafetyCellCount(safetyMapModel);
                            System.err.println("Safety cells: " + safetyCellCount);
                            if (safetyCellCount > maxSafetyCellCount) {
                                maxSafetyCellCount = safetyCellCount;
                                cellToRetreat = world.grid.cells[adjacentPosition.x][adjacentPosition.y];
                            }
                        }
                        System.err.println("Cell to retreat: " + cellToRetreat);
                        if (cellToRetreat != null) {
                            final List<Cell> path = getPathTo(targetCell, pathMap);
                            path.forEach(c -> world.planner.add(new Move(c.position, world.player)));
                            world.planner.add(new PlaceBombAndGoTo(cellToRetreat.position, world.player));
                            break;
                        } else {
                            utilityMap.at(targetPosition).value = 0;
                        }
                    } else {
                        // go to safety point
                        System.err.println("No target found, go to safety point");
                        final Cell nearestSafetyPoint = findNearestSafetyPoint(safetyMap, pathMap);
                        System.err.println("Nearest safety point: " + nearestSafetyPoint);
                        if (nearestSafetyPoint != null) {
                            final List<Cell> path = getPathTo(nearestSafetyPoint, pathMap);
                            if (path.isEmpty()) {
                                System.err.println("Already in safe");
                                world.planner.add(new SkipTurn(world.player, "Wait in safety"));
                            } else {
                                path.forEach(c -> world.planner.add(new Move(c.position, world.player)));
                            }
                        } else {
                            world.planner.add(new SkipTurn(world.player, "Goodbye cruel world"));
                        }
                        break;
                    }
                }
            }
            if (world.planner.isEmpty()) {
                world.planner.add(new SkipTurn(world.player, "wtf"));
            }
            timeCalculator.showTime("Target search");

            checkExplosionsAndDodge(world.player.position, typeMap, safetyMap);
            timeCalculator.showTime("Check explosions and dodge");

            System.err.println(world.grid.showUtility(utilityMap));
            System.err.println(world.grid.showDistanceFromPlayer(pathMap));
            System.err.println(world.grid.showExplosionMap(explosionMap, typeMap));
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

    void updateWorldState(final TypeMap typeMap) {
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
                final TypeParameter typeParameter = typeMap.values[columnIndex][rowIndex];
                if (typeSymbol == Cell.Type.Box.symbol) {
                    typeParameter.value = Cell.Type.Box;
                } else if (typeSymbol == Cell.Type.BoxWithExtraRange.symbol) {
                    typeParameter.value = Cell.Type.BoxWithExtraRange;
                } else if (typeSymbol == Cell.Type.BoxWithExtraBomb.symbol) {
                    typeParameter.value = Cell.Type.BoxWithExtraBomb;
                } else if (typeSymbol == Cell.Type.Wall.symbol) {
                    typeParameter.value = Cell.Type.Wall;
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
                        typeMap.values[x][y].value = Cell.Type.ExtraRange;
                    } else if (param1 == Item.Type.ExtraBomb.code) {
                        item.type = Item.Type.ExtraBomb;
                        typeMap.values[x][y].value = Cell.Type.ExtraBomb;
                    }
                    break;
                default:
                    break;
            }
        }
        world.allBombs.addAll(world.playerBombs);
        world.allBombs.addAll(world.enemyBombs);
        world.allBombs.forEach(b -> typeMap.at(b.position).value = Cell.Type.Bomb);
    }

    void calculateUtilityForCell(
            final Cell cell,
            final TypeMap typeMap,
            final Set<Cell> willBeDestroyedObjects,
            final IntegerMap utilityMap
    ) {
        final IntegerParameter utility = utilityMap.at(cell.position);
        final Cell.Type cellType = typeMap.at(cell.position).value;
        if (Cell.PASSABLE_SUBTYPES.contains(cellType)) {
            final Set<Cell> boxes = calculateDestroyedObjects(
                    cell.position,
                    world.player.explosionRange,
                    typeMap,
                    Cell.BOX_SUBTYPES,
                    willBeDestroyedObjects
            );
            utility.value = 0;
            boxes.forEach(c -> {
                switch (typeMap.at(c.position).value) {
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
        if (Cell.BONUS_SUBTYPES.contains(cellType)) {
            if (willBeDestroyedObjects.contains(cell)) {
                utility.value += 1; // danger
            } else {
                utility.value += 2;
            }
//            final int distanceToBonus = calculateDistance(world.player.position, cell.position);
//            utility.value = Math.max(6 - distanceToBonus, 0);
        }
        utility.value = Math.max(0, utility.value);
    }

    void calculateCellsUtilityAndPathsAndSafetyMap(
            final Position startPosition,
            final Set<Cell> willBeDestroyedObjects,
            final TypeMap typeMap,
            final IntegerMap explosionMap,
            final IntegerMap utilityMap,
            final PathMap pathMap,
            final IntegerMap safetyMap
    ) {
        final PriorityQueue<Cell> queue = new PriorityQueue<>(
                world.grid.width * world.grid.height,
                (Comparator<Cell>) (o1, o2) -> pathMap.at(o1.position).distance - pathMap.at(o2.position).distance
        );
//        System.err.println("===========================================");
//        System.err.println("calculateCellsUtilityAndPathsAndSafetyMap");
//        System.err.println("Start: " + startPosition);
        final BooleanMap utilityCalculated = BooleanMap.createFalseMap(world.grid.width, world.grid.height);
        final BooleanMap pathCalculated = BooleanMap.createFalseMap(world.grid.width, world.grid.height);
        final Cell cells[][] = world.grid.cells;
        final Cell startCell = cells[startPosition.x][startPosition.y];
        pathMap.at(startPosition).distance = 0;
        safetyMap.at(startPosition).value = explosionMap.at(startPosition).value;
        queue.add(startCell);
        while (!queue.isEmpty()) {
            final Cell currentCell = queue.poll();
//            System.err.println("Current cell: " + currentCell);
            final PathParameter currentPathParameter = pathMap.at(currentCell.position);
            final BooleanParameter currentCellUtilityCalculated = utilityCalculated.at(currentCell.position);
            if (!currentCellUtilityCalculated.value) {
                calculateUtilityForCell(currentCell, typeMap, willBeDestroyedObjects, utilityMap);
//                System.err.println("Utility = " + utilityMap.at(currentCell.position).value);
            }

            currentCellUtilityCalculated.value = true;
            pathCalculated.at(currentCell.position).value = true;

            // add adjacent cells to queue
            final List<Position> adjacentPositions = generateAdjacentPositions(currentCell.position, null, typeMap);
//            System.err.println("Check adjacent positions");
            adjacentPositions
                    .forEach(p -> {
                        final Cell adjacentCell = cells[p.x][p.y];
//                        System.err.println("Adjacent cell: " + adjacentCell);
                        if (pathCalculated.at(p).value) {
//                            System.err.println("Path already calculated, ignore");
                            return;
                        }
                        final PathParameter adjacentPathParameter = pathMap.at(p);
                        final int explosionTime = explosionMap.at(p).value;
                        if (Cell.PASSABLE_SUBTYPES.contains(typeMap.at(p).value)) {
                            final int newDistance = currentPathParameter.distance + 1;
//                            System.err.println("New distance = " + newDistance);
//                            System.err.println("Explosion time = " + explosionTime);
                            final IntegerParameter adjacentSafety = safetyMap.at(p);
                            if ((explosionTime != Bomb.NO_EXPLOSION)
                                    && (explosionTime - newDistance) == Bomb.ALREADY_EXPLODED) {
                                // player will be dead if go this way, ignore it
//                                System.err.println("Player will be dead if go this way, ignore it");
                                adjacentSafety.value = Bomb.ALREADY_EXPLODED;
                                return;
                            }
                            if (newDistance < adjacentPathParameter.distance) {
//                                System.err.println("New distance is better than old one " + adjacentPathParameter.distance);
                                adjacentPathParameter.distance = newDistance;
                                adjacentPathParameter.previousCell = currentCell;
                                if (explosionTime == Bomb.NO_EXPLOSION) {
                                    adjacentSafety.value = Bomb.NO_EXPLOSION;
                                } else {
                                    adjacentSafety.value = Math.max(0, explosionTime - newDistance);
                                }
//                                System.err.println("New safety = " + adjacentSafety.value);
                            } else {
//                                System.err.println("Old distance " + adjacentPathParameter.distance + " is better than new one");
                            }
                            // remove and add -> force to recalculate priority
                            if (queue.contains(adjacentCell)) { // O(n) :(
                                queue.remove(adjacentCell); // O(n) :(
                            }
                            queue.add(adjacentCell);
                        }
//                        else {
//                            System.err.println("Nonpassable cell, ignore");
//                        }
                    });
        }
//        System.err.println("===========================================");
    }

    void calculateExplosionMap(final List<Bomb> bombs, final TypeMap typeMap, final IntegerMap explosionMap) {
        final Set<Cell> destroyedObjects = new HashSet<>();
        bombs.stream()
                .sorted((o1, o2) -> o1.timer - o2.timer)
                .forEach(b -> calculateExplosionMapForBomb(b, destroyedObjects, typeMap, explosionMap));
    }

    void checkExplosionWaveFromBomb(
            final Bomb bomb,
            final Position position,
            final IntegerMap explosionMap
    ) {
        final int stateAtBombPosition = explosionMap.at(bomb.position).value;
        final int timer = (stateAtBombPosition == Bomb.NO_EXPLOSION) ? bomb.timer : stateAtBombPosition; // check for chain effect
        final int previousValue = explosionMap.at(position).value;
        if (previousValue == Bomb.NO_EXPLOSION || previousValue > timer) {
            explosionMap.at(position).value = timer;
        }
    }

    Set<Cell> calculateDestroyedObjects(
            final Position bombPosition,
            final int explosionRange,
            final TypeMap typeMap,
            final EnumSet<Cell.Type> filter,
            final Set<Cell> willBeDestroyedObjects
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
                final Cell.Type cellType = typeMap.at(pos).value;
                if (filter.contains(cellType) && !willBeDestroyedObjects.contains(cell)) {
                    destroyedObjects.add(cell);
                }
                if (Cell.EXPLOSION_STOPPERS.contains(cellType)) {
                    break;
                }
            }
        });
        return destroyedObjects;
    }

    void calculateExplosionMapForBomb(
            final Bomb bomb,
            final Set<Cell> destroyedObjects,
            final TypeMap typeMap,
            final IntegerMap explosionMap) {
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
                final Cell.Type cellType = typeMap.at(pos).value;
                checkExplosionWaveFromBomb(bomb, cell.position, explosionMap);
                if (Cell.EXPLOSION_STOPPERS.contains(cellType)) {
                    if (Cell.DESTROYABLE_OBJECTS.contains(cellType) && !destroyedObjects.contains(cell)) {
//                        System.err.println("Destroyed: " + cell);
                        destroyedObjects.add(cell);
                    }
                    break;
                }
            }
        });
    }

    Cell findNearestCellWithHighestUtility(int scanRange, final IntegerMap utilityMap, final PathMap pathMap) {
        return world.grid.asList.stream()
                .filter(c -> pathMap.at(c.position).distance <= scanRange)
                .filter(c -> utilityMap.at(c.position).value != 0)
                .max((o1, o2) -> utilityMap.at(o1.position).value - utilityMap.at(o2.position).value)
                .orElse(null);
    }

    void checkExplosionsAndDodge(final Position playerPos, final TypeMap typeMap, final IntegerMap safetyMap) {
        final List<Position> adjacentPositions = generateAdjacentPositions(playerPos, Cell.PASSABLE_SUBTYPES, typeMap);
        final Cell[][] cells = world.grid.cells;
        final Cell playersCell = cells[playerPos.x][playerPos.y];
        final IntegerParameter playerSafety = safetyMap.at(playerPos);
        if (playerSafety.value == Bomb.EXPLODE_NEXT_TURN) {
            System.err.println("Player's position will explode next turn!");
            final Cell dodgeCell = adjacentPositions
                    .stream()
                    .map(p -> cells[p.x][p.y])
                    .max((o1, o2) -> {
                        int safety1 = safetyMap.at(o1.position).value;
                        if (safety1 == Bomb.NO_EXPLOSION) {
                            safety1 = 100500;
                        }
                        int safety2 = safetyMap.at(o2.position).value;
                        if (safety2 == Bomb.NO_EXPLOSION) {
                            safety2 = 100500;
                        }
                        return safety1 - safety2;
                    })
                    .orElse(playersCell);
            System.err.println("Cell to dodge: " + dodgeCell);
            final Move dodge = new Move(dodgeCell.position, world.player);
            dodge.priority = Action.HIGH_PRIORITY;
            world.planner.add(dodge);
        }
//        else {
//            adjacentPositions
//                    .stream()
//                    .filter(p -> safetyMap.at(p).value == Bomb.EXPLODE_NEXT_TURN)
//                    .findAny()
//                    .ifPresent(c -> {
//                        final SkipTurn skip = new SkipTurn(world.player, "Dodge explosion");
//                        skip.priority = Action.HIGH_PRIORITY;
//                        world.planner.add(skip);
//                    });
//        }
    }

    void modelNewBomb(
            final Bomb bomb,
            final Position playerPosition,
            final TypeMap typeMap,
            final IntegerMap utilityMap,
            final PathMap pathMap,
            final IntegerMap explosionMap,
            final IntegerMap safetyMap
    ) {
        final List<Bomb> bombs = new ArrayList<>(world.allBombs.size() + 1);
        world.allBombs.forEach(bombs::add);
        bombs.add(bomb);
        typeMap.at(bomb.position).value = Cell.Type.Bomb;
        calculateExplosionMap(bombs, typeMap, explosionMap);
        final Set<Cell> willBeDestroyedObjects = new HashSet<>();
        willBeDestroyedObjects.addAll(
                world.grid.asList
                        .stream()
                        .filter(c -> Cell.DESTROYABLE_OBJECTS.contains(typeMap.at(c.position).value))
                        .filter(c -> explosionMap.at(c.position).value >= Bomb.ALREADY_EXPLODED)
                        .collect(Collectors.toSet())
        );
        calculateCellsUtilityAndPathsAndSafetyMap(
                playerPosition,
                willBeDestroyedObjects,
                typeMap,
                explosionMap,
                utilityMap,
                pathMap,
                safetyMap
        );
        System.err.println(world.grid.showSafetyMap(safetyMap));
    }

    List<Position> generateAdjacentPositions(final Position center, final EnumSet<Cell.Type> filter, final TypeMap typeMap) {
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
        if (filter == null) {
            return adjacentPositions;
        } else {
            return adjacentPositions.stream()
                    .filter(p -> filter.contains(typeMap.at(p).value))
                    .collect(Collectors.toList());
        }
    }

    List<Cell> getPathTo(final Cell targetCell, final PathMap pathMap) {
        final PathParameter targetPathParameter = pathMap.at(targetCell.position);
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
            nextPathParameter = pathMap.at(nextCell.position);
        } while (nextPathParameter.previousCell != null);
        Collections.reverse(path);
        return path;
    }

    int getSafetyCellCount(final IntegerMap safetyMap) {
        return (int) safetyMap.asList
                .stream()
                .filter(p -> p.value == Bomb.NO_EXPLOSION)
                .count();
    }

    Cell findNearestSafetyPoint(final IntegerMap safetyMap, final PathMap pathMap) {
        return world.grid.asList
                .stream()
                .filter(c -> safetyMap.at(c.position).value == Bomb.NO_EXPLOSION)
                .min((o1, o2) -> pathMap.at(o1.position).distance - pathMap.at(o2.position).distance)
                .orElse(null);
    }
}