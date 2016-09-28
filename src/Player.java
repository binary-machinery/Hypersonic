import java.util.*;

// TODO: improve dodging

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

    Position position;
    Type type = Type.Floor;
    int utility = 0;
    int distanceFromPlayer = Integer.MAX_VALUE;
    Cell previousCell; // for pathfinding
    boolean utilityCalculated = false;
    boolean pathCalculated = false;
    int timerToExplosion = 0;

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

    String showDistanceFromPlayer() {
        final StringBuilder sb = new StringBuilder(height * width);
        for (int rowIndex = 0; rowIndex < height; ++rowIndex) {
            for (int columnIndex = 0; columnIndex < width; ++columnIndex) {
                sb.append(cells[columnIndex][rowIndex].distanceFromPlayer == Integer.MAX_VALUE ? "." : cells[columnIndex][rowIndex].distanceFromPlayer);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    String showExplosionMap() {
        final StringBuilder sb = new StringBuilder(height * width);
        for (int rowIndex = 0; rowIndex < height; ++rowIndex) {
            for (int columnIndex = 0; columnIndex < width; ++columnIndex) {
                if (cells[columnIndex][rowIndex].timerToExplosion == Bomb.NO_EXPLOSION) {
                    sb.append(Cell.NONPASSABLE_SUBTYPES.contains(cells[columnIndex][rowIndex].type) ? "X" : ".");
                } else {
                    sb.append(cells[columnIndex][rowIndex].timerToExplosion);
                }
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
        return true;
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

class PlaceBomb extends Action {
    private boolean bombPlaced = false;
    private Position targetPosition;
    private Boomer player;

    PlaceBomb(Position targetPosition, Boomer player) {
        this.player = player;
        this.targetPosition = targetPosition;
        priority = 5;
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

class DestroyedItemsModel {
    EnumSet<Cell.Type> filter;
    Set<Cell> destroyedObjects = new HashSet<>();
    Set<Position> ignoredCells;

    void checkExplosionWave(Cell cell) {
        if (filter.contains(cell.type) && !ignoredCells.contains(cell.position)) {
            destroyedObjects.add(cell);
        }
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

            calculateExplosionMap();

            final Set<Position> ignoredCells = new HashSet<>();
            final DestroyedItemsModel destroyedItemsModel = new DestroyedItemsModel();
            destroyedItemsModel.ignoredCells = ignoredCells;
            destroyedItemsModel.filter = Cell.BOX_SUBTYPES;
            for (Bomb bomb : world.allBombs) {
                modelExplosionOfOneBomb(bomb, destroyedItemsModel, null);
            }
            world.allBombs.forEach(b -> ignoredCells.add(b.position));
            destroyedItemsModel.destroyedObjects.forEach(cell -> ignoredCells.add(cell.position));

            calculateCellsUtilityAndNearestPaths(ignoredCells);

            System.err.println(world.grid.showUtility());
            System.err.println(world.grid.showDistanceFromPlayer());
            System.err.println(world.grid.showExplosionMap());

            if (world.planner.isEmpty()) {
                int scanRange = (world.player.bombsAvailable > 0) ? Bomb.COUNTDOWN / 2 : Bomb.COUNTDOWN;
//                int scanRange = 50; // unlimited
                System.err.println("Scan range = " + scanRange);
                Target target = findNearestCellWithHighestUtility(scanRange, ignoredCells);
//                if (target == null) {
//                    System.err.println("Nothing found");
//                    scanRange *= 2;
//                    System.err.println("Scan range = " + scanRange);
//                    target = findNearestCellWithHighestUtility(scanRange, ignoredCells);
//                }
//                if (target == null) {
//                    System.err.println("Nothing found");
//                    scanRange = 50;
//                    System.err.println("Scan range = " + scanRange);
//                    target = findNearestCellWithHighestUtility(scanRange, ignoredCells);
//                }
                if (target == null) {
                    System.err.println("Empty target!");
                    world.planner.add(new SkipTurn(world.player));
                } else {
                    System.err.println(target);
                    if (target.type == Target.Type.BombPlace) {
                        world.planner.add(new PlaceBomb(target.position, world.player));
                    } else {
                        world.planner.add(new Move(target.position, world.player));
                    }
                }
            }
            checkExplosionsAndDodge();

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
        world.allBombs.forEach(b -> world.grid.cells[b.position.x][b.position.y].type = Cell.Type.Bomb);
    }

    void calculateUtilityForCell(final Cell cell, final Set<Position> ignoredCells) {
        if (Cell.PASSABLE_SUBTYPES.contains(cell.type)) {
            final DestroyedItemsModel destroyedItemsModel = new DestroyedItemsModel();
            destroyedItemsModel.ignoredCells = ignoredCells;
            destroyedItemsModel.filter = Cell.BOX_SUBTYPES;
            modelExplosionOfOneBomb(world.player.createBomb(cell.position), destroyedItemsModel, null);
            final Set<Cell> boxes = destroyedItemsModel.destroyedObjects;
            cell.utility = 0;
            boxes.forEach(c -> {
                switch (c.type) {
                    case Box:
                        cell.utility += 1;
                        break;
                    case BoxWithExtraBomb:
                        cell.utility += 2;
                        break;
                    case BoxWithExtraRange:
                        cell.utility += 2;
                        break;
                    default:
                        break;
                }
            });
//            final List<Position> adjacentPositions = generateAdjacentPositions(cell.position);
//            cell.utility += (int) adjacentPositions
//                    .stream()
//                    .map(p -> world.grid.cells[p.x][p.y].type)
//                    .filter(t -> Cell.PASSABLE_SUBTYPES.contains(t))
//                    .count();
        }
        if (Cell.BONUS_SUBTYPES.contains(cell.type)) {
            if (ignoredCells.contains(cell.position)) {
                return;
            }
            final int distanceToBonus = calculateDistance(world.player.position, cell.position);
            cell.utility = Math.max(6 - distanceToBonus, 0);
        }
    }

    void calculateCellsUtilityAndNearestPaths(final Set<Position> ignoredCells) {
        final PriorityQueue<Cell> queue = new PriorityQueue<>(
                world.grid.width * world.grid.height,
                (Comparator<Cell>) (o1, o2) -> o1.distanceFromPlayer - o2.distanceFromPlayer
        );
        final Cell cells[][] = world.grid.cells;
        final Cell start = cells[world.player.position.x][world.player.position.y];
        start.distanceFromPlayer = 0;
        queue.add(start);
        while (!queue.isEmpty()) {
            final Cell currentCell = queue.poll();
            if (!currentCell.utilityCalculated && !ignoredCells.contains(currentCell.position)) {
                calculateUtilityForCell(currentCell, ignoredCells);
            }
            currentCell.utilityCalculated = true;
            currentCell.pathCalculated = true;

            // add adjacent cells to queue
            final List<Position> adjacentPositions = generateAdjacentPositions(currentCell.position);
            adjacentPositions
                    .forEach(p -> {
                        final Cell adjacentCell = cells[p.x][p.y];
                        if (adjacentCell.pathCalculated) {
                            return;
                        }
                        if (Cell.PASSABLE_SUBTYPES.contains(adjacentCell.type)) {
                            final int newDistance = currentCell.distanceFromPlayer + 1;
                            if (newDistance < adjacentCell.distanceFromPlayer) {
                                adjacentCell.distanceFromPlayer = newDistance;
                                adjacentCell.previousCell = currentCell;
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

    void calculateExplosionMap() {
        final List<Bomb> bombs = world.allBombs;
        bombs.stream()
                .sorted((o1, o2) -> o1.timer - o2.timer)
                .forEach(b -> modelExplosionOfOneBomb(b, null, world.grid.cells));
    }

    void checkExplosionWave(Bomb bomb, Position position) {
        final int stateAtBombPosition = world.grid.cells[bomb.position.x][bomb.position.y].timerToExplosion;
        final int timer = (stateAtBombPosition == Bomb.NO_EXPLOSION) ? bomb.timer : stateAtBombPosition; // check for chain effect
        final int previousValue = world.grid.cells[position.x][position.y].timerToExplosion;
        if (previousValue == Bomb.NO_EXPLOSION || previousValue > timer) {
            world.grid.cells[position.x][position.y].timerToExplosion = timer;
        }
    }

    void modelExplosionOfOneBomb(
            final Bomb bomb,
            final DestroyedItemsModel destroyedItemsModel,
            final Cell[][] explosionMap
    ) {
        final int width = world.grid.width;
        final int height = world.grid.height;

        if (explosionMap != null) {
            checkExplosionWave(bomb, bomb.position);
        }

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
                if ((pos.x < 0) || (pos.y < 0) || (pos.x >= width) || (pos.y >= height)) {
                    break; // end of map
                }
                final Cell cell = world.grid.cells[pos.x][pos.y];
                if (destroyedItemsModel != null) {
                    destroyedItemsModel.checkExplosionWave(cell);
                }
                if (explosionMap != null) {
                    checkExplosionWave(bomb, cell.position);
                }
                if (Cell.EXPLOSION_STOPPERS.contains(cell.type)) {
                    break;
                }
            }
        });
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

    void checkExplosionsAndDodge() {
        final Position playerPos = world.player.position;
        final List<Position> adjacentPositions = generateAdjacentPositions(playerPos);
        final Cell[][] cells = world.grid.cells;
        if (cells[playerPos.x][playerPos.y].timerToExplosion == Bomb.EXPLODE_NEXT_TURN) {
            adjacentPositions
                    .stream()
                    .filter(p -> cells[p.x][p.y].timerToExplosion == Bomb.NO_EXPLOSION)
                    .filter(p -> Cell.PASSABLE_SUBTYPES.contains(world.grid.cells[p.x][p.y].type))
                    .findAny()
                    .ifPresent(p -> {
                        final Move dodge = new Move(p, world.player);
                        dodge.priority = 0;
                        world.planner.add(dodge);
                    });
        } else {
            final long dangerousCells = adjacentPositions
                    .stream()
                    .filter(p -> cells[p.x][p.y].timerToExplosion == Bomb.EXPLODE_NEXT_TURN)
                    .count();
            if (dangerousCells > 0) {
                final SkipTurn skip = new SkipTurn(world.player);
                skip.priority = 0;
                world.planner.add(skip);
            }
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
}