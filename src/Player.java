import java.util.*;

class Boomer {
    static final int ENTITY_TYPE = 0;
    int id;
    int x;
    int y;
    int bombsAvailable;
    int explosionRange;

    public String toString() {
        return "Boomer {" + x + "," + y + "," + bombsAvailable + "," + explosionRange + "}";
    }
}

class Bomb {
    static final int ENTITY_TYPE = 1;
    int x;
    int y;
    int roundsLeft;
    int explosionRange;

    public String toString() {
        return "Bomb {" + x + "," + y + "," + roundsLeft + "," + explosionRange + "}";
    }
}

class World {
    int height;
    int width;
    Boomer player = new Boomer();
    Boomer enemy = new Boomer();
    Bomb playerBomb;
    Bomb enemyBomb;
}

// main class must be Player
public class Player {

    private static final char FLOOR = '.';
    private static final char BOX = '0';

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
            System.err.println(world.player);
            System.err.println(world.enemy);
            System.err.println(world.playerBomb);
            System.err.println(world.enemyBomb);

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");

            System.out.println("BOMB 6 5");
        }
    }

    void initWorld() {
        world.width = in.nextInt();
        world.height = in.nextInt();
        world.player.id = in.nextInt();
        in.nextLine();
    }

    void updateWorldState() {
        world.playerBomb = null;
        world.enemyBomb = null;
        for (int i = 0; i < world.height; i++) {
            String row = in.nextLine();
            System.err.println(row);
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
                    bomb.roundsLeft = param1;
                    bomb.explosionRange = param2;
                    break;
                default:
                    break;
            }
        }
    }
}