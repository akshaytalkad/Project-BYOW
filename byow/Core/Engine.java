package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import edu.princeton.cs.algs4.StdDraw;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.*;

/** Represents the playing area and all its interactions. */
public class Engine implements Serializable {
    /** Represents a Room within the playing area. */
    private class Room implements Serializable {
        /** X-coordinate of the Room. */
        private int xCoord;
        /** Y-coordinate of the Room. */
        private int yCoord;
        /** Height of the Room. */
        private int height;
        /** Width of the Room. */
        private int width;
        /** Constructor creates a new Room. */
        private Room(int x, int y, int hi, int wi) {
            xCoord = x;
            yCoord = y;
            height = hi;
            width = wi;
        }
    }

    /** Tile renderer used by the Engine. */
    TERenderer ter = new TERenderer();
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The file storing the Engine object. */
    public static final File ENGINE = Utils.join(CWD, "engine.txt");
    /** The directory storing all audio clips. */
    public static final File AUDIO = Utils.join(CWD, "audio");
    /** The directory storing all images. */
    public static final File IMAGES = new File(CWD + File.separator + "images");
    /** Width of playable area. */
    public static final int WIDTH = 80;
    /** Height of playable area. */
    public static final int HEIGHT = 30;
    /** List of all generated rooms in order. */
    private ArrayList<Room> allRooms;
    /** Random object being used by the Engine. */
    private Random currentRandom;
    /** Seed of the world. */
    private long currSeed;
    /** Array representing the playable area. */
    private TETile[][] world;

    private String allInput;
    /** X-coordinate of the avatar. */
    private int playerX;
    /** Y-coordinate of the avatar. */
    private int playerY;

    private char lastKey;

    private boolean inPlay;

    private boolean lineOfSight;

    private boolean jumpScare;

    private int coinsCollected;

    private int coinGoal;

    private int numOfMoves;

    private transient Clip menuMusic;

    private boolean mute;

    private boolean muteMenuMusic;

    public Engine() {
        allRooms = new ArrayList<>();
        currentRandom = null;
        currSeed = 0;
        world = new TETile[WIDTH][HEIGHT];
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                world[i][j] = Tileset.NOTHING;
            }
        }
        playerX = 0;
        playerY = 0;
        inPlay = false;
        lineOfSight = false;
        allInput = "";
        jumpScare = false;
        menuMusic = null;
        mute = true;
        muteMenuMusic = false;
    }

    /** Saves Engine object to engine.txt. */
    public void saveEngine() {
        Utils.writeObject(ENGINE, this);
    }

    /** Reads engine.txt into an object. */
    public void readEngine() {
        Engine toUse = Utils.readObject(ENGINE, Engine.class);
        currSeed = toUse.getCurrSeed();
        allInput = toUse.getAllInput();
        lineOfSight = toUse.getLineOfSight();
        mute = toUse.getMute();
        world = new TETile[WIDTH][HEIGHT];
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                world[i][j] = Tileset.NOTHING;
            }
        }
        allRooms = new ArrayList<>();
        createGameworld(currSeed);
        inPlay = false;
        char[] characters = allInput.toCharArray();
        for (char curr : characters) {
            moveToDirection(Character.toUpperCase(curr));
        }
    }

    public void readEngine(String r) {
        Engine toUse = Utils.readObject(ENGINE, Engine.class);
        currSeed = toUse.getCurrSeed();
        allInput = toUse.getAllInput();
        lineOfSight = toUse.getLineOfSight();
        jumpScare = toUse.getJumpscare();
        //System.out.println(allInput);
        world = new TETile[WIDTH][HEIGHT];
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                world[i][j] = Tileset.NOTHING;
            }
        }
        allRooms = new ArrayList<>();
        createGameworld(currSeed);
        inPlay = false;
    }

    /**
     * Method used for exploring a fresh world. This method should handle all inputs,
     * including inputs from the main menu.
     */
    public void interactWithKeyboard() {
        interactWithKeyboardMenu();

        inPlay = coinsCollected < coinGoal && numOfMoves > 0;

        while (inPlay) {
            char last = lastKey;
            char newCurrent = getKeyPressed();
            if (Character.toUpperCase(newCurrent) == 'P') {
                jumpScare = !jumpScare;
            }
            if (Character.toUpperCase(newCurrent) == 'M') {
                mute = !mute;
            }
            if (Character.toUpperCase(newCurrent) == 'E') {
                lineOfSight = !lineOfSight;
                ter.renderFrame(world, playerX, playerY, lineOfSight);
                displayScreens("In game");
                StdDraw.show();
            }
            if (last == ':' && Character.toUpperCase(newCurrent) == 'Q') {
                saveEngine();
                inPlay = false;
                System.exit(0);
            }
            if (Character.toUpperCase(newCurrent) != 'E'
                    && Character.toUpperCase(newCurrent) != 'Q') {
                //System.out.println(newCurrent);
                moveToDirection(Character.toUpperCase(newCurrent));
                ter.renderFrame(world, playerX, playerY, lineOfSight);
                displayScreens("In game");
                StdDraw.show();
                inPlay = coinsCollected < coinGoal && numOfMoves > 0;
            }
        }
        if (coinsCollected == coinGoal) {
            displayScreens("Win");
            StdDraw.show();
            while (true) {
                char exit = getKeyPressed();
                if (Character.toUpperCase(exit) == 'Q') {
                    saveEngine();
                    System.exit(0);
                }
            }
        }
        if (numOfMoves <= 0) {
            displayScreens("Lose");
            StdDraw.show();
            while (true) {
                char exit = getKeyPressed();
                if (Character.toUpperCase(exit) == 'Q') {
                    saveEngine();
                    System.exit(0);
                }
            }
        }
    }

    public void interactWithKeyboardMenu() {
        Long seedToUse;
        //Starts Main Menu
        displayScreens("Main");
        if (muteMenuMusic) {
            startMenuMusic();
        }
        //Create a input source that users use
        char current = getKeyPressed(); // store the current value that was inputted
        while (Character.toUpperCase(current) != 'N'
                && Character.toUpperCase(current) != 'L'
                && Character.toUpperCase(current) != 'Q'
                && Character.toUpperCase(current) != 'R') {
            //current will equal next input value
            current = getKeyPressed();
        }
        if (Character.toUpperCase(current) == 'N') {
            displayScreens("SeedPrompt");
            StdDraw.setFont(new Font("Monaco", Font.BOLD, 12));
            String seedInput = "";
            char forSeed = getKeyPressed();
            while (Character.toUpperCase(forSeed) != 'S') {
                seedInput += Character.toString(forSeed);
                displayScreens("SeedPrompt");
                StdDraw.setFont(new Font("Monaco", Font.BOLD, 12));
                StdDraw.text(0.5, 0.4, seedInput);
                StdDraw.show();
                forSeed = getKeyPressed();
            }
            seedToUse = Long.valueOf(seedInput);
            currSeed = seedToUse;
            createGameworld(currSeed);
        }
        if (Character.toUpperCase(current) == 'L') {
            readEngine();
        }
        if (muteMenuMusic) {
            menuMusic.stop();
        }
        ter.initialize(WIDTH + 10, HEIGHT + 20, 5, 10); //to initialize the board
        if (Character.toUpperCase(current) == 'R') {
            readEngine("r");
            displayScreens("In game");
            ter.renderFrame(world, playerX, playerY, false);
            StdDraw.pause(1000);
            char[] characters = allInput.toCharArray();
            for (char curr : characters) {
                StdDraw.pause(200);
                moveToDirection(Character.toUpperCase(curr));
                ter.renderFrame(world, playerX, playerY, false);
                displayScreens("In game");
                StdDraw.show();
            }
        }
        if (Character.toUpperCase(current) == 'Q') {
            System.exit(0);
        }
        //set up initial world state
        ter.renderFrame(world, playerX, playerY, lineOfSight);
        displayScreens("In game");
        StdDraw.show();
    }

    public char getKeyPressed() {
        while (true) {
            if (StdDraw.isMousePressed()) {
                int mouseX = (int) StdDraw.mouseX() - 5;
                int mouseY = (int) StdDraw.mouseY() - 10;
                if (mouseX >= 0 && mouseX < WIDTH && mouseY >= 0 && mouseY < HEIGHT) {
                    StdDraw.clear(Color.black);
                    ter.renderFrame(world, playerX, playerY, lineOfSight);
                    displayScreens("In game");
                    displayScreens(world[mouseX][mouseY].description());
                    StdDraw.show();
                }
            }
            if (StdDraw.hasNextKeyTyped()) {
                char newKey = StdDraw.nextKeyTyped();
                lastKey = newKey;
                if (inPlay) {
                    if (Character.toUpperCase(newKey) == 'W'
                            || Character.toUpperCase(newKey) == 'A'
                            || Character.toUpperCase(newKey) == 'S'
                            || Character.toUpperCase(newKey) == 'D'
                            || Character.toUpperCase(newKey) == 'T') {
                        allInput += Character.toString(newKey);
                    }
                }
                return newKey;
            }
        }
    }

    public void displayScreens(String in) {
        if (in.equals("Main")) {
            StdDraw.clear(StdDraw.BLACK);
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.setFont(new Font("Monaco", Font.BOLD, 50));
            StdDraw.text(0.5, 0.8, "CS61B:The Game");
            StdDraw.setFont(new Font("Monaco", Font.PLAIN, 20));
            StdDraw.text(0.5, 0.5, "New Game (N)");
            StdDraw.text(0.5, 0.4, "Load Game (L)");
            StdDraw.text(0.5, 0.3, "Replay Save (R)");
            StdDraw.text(0.5, 0.2, "Quit (Q)");
        } else if (in.equals("SeedPrompt")) {
            StdDraw.clear(StdDraw.BLACK);
            StdDraw.setFont(new Font("Monaco", Font.BOLD, 20));
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.text(0.5, 0.7, "Enter a random seed and then click S");
        } else if (in.equals("Save")) {
            StdDraw.clear(StdDraw.BLACK);
            StdDraw.setFont(new Font("Monaco", Font.BOLD, 20));
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.text(0.5, 0.7, "Click Q to save your world");
        } else if (in.equals("In game")) {
            StdDraw.setFont(new Font("Monaco", Font.PLAIN, 15));
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.text(75, 45, "Press E to toggle lights");
            StdDraw.text(75, 43, "Press M to toggle mute");
            StdDraw.text(35, 43, "Press T to take a chance with teleport: Costs 50 moves");
            StdDraw.text(55, 45, "Coins Collected: " + coinsCollected + "/" + coinGoal);
            StdDraw.text(35, 45, numOfMoves + " moves left");
        } else if (in.equals("Win")) {
            if (!mute) {
                winMusic();
            }
            StdDraw.clear(Color.BLACK);
            StdDraw.setFont(new Font("Monaco", Font.PLAIN, 50));
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.text(45, 25, "YOU WIN");
            StdDraw.setFont(new Font("Monaco", Font.PLAIN, 30));
            StdDraw.text(45, 10, "Click Q to Exit");
        } else if (in.equals("Lose")) {
            if (!mute) {
                loseMusic();
            }
            StdDraw.clear(Color.BLACK);
            StdDraw.setFont(new Font("Monaco", Font.PLAIN, 50));
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.text(45, 25, "YOU LOSE");
            StdDraw.setFont(new Font("Monaco", Font.PLAIN, 30));
            StdDraw.text(45, 10, "Click Q to Exit");
        } else if (in.equals("nothing")) {
            StdDraw.setFont(new Font("Monaco", Font.PLAIN, 15));
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.text(10, 45, "Rock");
        } else if (in.equals("wall")) {
            StdDraw.setFont(new Font("Monaco", Font.PLAIN, 15));
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.text(10, 45, "Tree");
        } else if (in.equals("floor")) {
            StdDraw.setFont(new Font("Monaco", Font.PLAIN, 15));
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.text(10, 45, "Ground");
        } else if (in.equals("you")) {
            StdDraw.setFont(new Font("Monaco", Font.PLAIN, 15));
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.text(10, 45, "You");
        } else if (in.equals("coin")) {
            StdDraw.setFont(new Font("Monaco", Font.PLAIN, 15));
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.text(10, 45, "Coin");
        }
    }

    /**
     * Method used for autograding and testing your code. The input string will be a series
     * of characters (for example, "n123sswwdasdassadwas", "n123sss:q", "lwww". The engine should
     * behave exactly as if the user typed these characters into the engine using
     * interactWithKeyboard.
     *
     * Recall that strings ending in ":q" should cause the game to quite save. For example,
     * if we do interactWithInputString("n123sss:q"), we expect the game to run the first
     * 7 commands (n123sss) and then quit and save. If we then do
     * interactWithInputString("l"), we should be back in the exact same state.
     *
     * In other words, both of these calls:
     *   - interactWithInputString("n123sss:q")
     *   - interactWithInputString("lww")
     *
     * should yield the exact same world state as:
     *   - interactWithInputString("n123sssww")
     *
     * @param input the input string to feed to your program
     * @return the 2D TETile[][] representing the state of the world
     */
    public TETile[][] interactWithInputString(String input) {
        // Fill out this method so that it run the engine using the input
        // passed in as an argument, and return a 2D tile representation of the
        // world that would have been drawn if the same inputs had been given
        // to interactWithKeyboard().
        //
        // See proj3.byow.InputDemo for a demo of how you can make a nice clean interface
        // that works for many different input types.

        // Declaring variables
        char[] characters = input.toCharArray();
        int pos = 0;
        String nToSVal = "";

        // Looping through the input
        while (Character.toUpperCase(characters[pos]) != 'N'
                && Character.toUpperCase(characters[pos]) != 'L') {
            pos += 1;
        }
        if (Character.toUpperCase(characters[pos]) == 'N') {
            pos += 1;
            while (Character.toUpperCase(characters[pos]) != 'S') {
                nToSVal += Character.toString(characters[pos]);
                pos += 1;
            }
            pos += 1;
            currSeed = Long.valueOf(nToSVal);
            createGameworld(currSeed);
        }
        if (pos < characters.length && Character.toUpperCase(characters[pos]) == 'L') {
            readEngine();
        }
        // Performing functions

        inPlay = pos < characters.length;
        while (inPlay) {
            while (pos < characters.length && Character.toUpperCase(characters[pos]) != ':') {
                if (Character.toUpperCase(characters[pos]) == 'W'
                        || Character.toUpperCase(characters[pos]) == 'A'
                        || Character.toUpperCase(characters[pos]) == 'S'
                        || Character.toUpperCase(characters[pos]) == 'D') {
                    allInput += Character.toString(characters[pos]);
                }
                moveToDirection(Character.toUpperCase(characters[pos]));
                pos += 1;
            }
            pos += 1;
            if (pos < characters.length && Character.toUpperCase(characters[pos]) == 'Q') {
                saveEngine();
                inPlay = false;
            }
            if (pos >= characters.length) {
                inPlay = false;
            }
        }
        return world;
    }

    /** Generates all Rooms and hallways in playing area. */
    public void createGameworld(long ourSeed) {
        currentRandom = new Random(ourSeed);
        Room newRoom = createRoom();
        putRoomOnBoard(newRoom);

        for (int i = 0; i < 500; i++) {
            newRoom = createRoom();
            if (checkRoomValid(newRoom)) {
                int randomRoom = currentRandom.nextInt(allRooms.size());
                if (ableToBuildHall(allRooms.get(randomRoom), newRoom)) {
                    putRoomOnBoard(newRoom);
                    buildHall(allRooms.get(randomRoom), newRoom);
                }
            }
        }

        putCharacterInSpot();
        putCoinsInSpots();
    }

    /** Creates a random Room to see if it can be generated. */
    private Room createRoom() {
        // Generates position and height/weight of Room
        int height = currentRandom.nextInt(14);
        int width = currentRandom.nextInt(14);
        int x = currentRandom.nextInt(WIDTH);
        int y = currentRandom.nextInt(HEIGHT);
        while (height < 5) {
            height = currentRandom.nextInt(14);
        }
        while (width < 5) {
            width = currentRandom.nextInt(14);
        }
        while (x + width - 1 > WIDTH - 1) {
            x = currentRandom.nextInt(WIDTH);
        }
        while (y + height - 1 > HEIGHT - 1) {
            y = currentRandom.nextInt(HEIGHT);
        }

        return new Room(x, y, height, width);
    }

    /** Checks whether the randomly generated Room is valid. */
    private boolean checkRoomValid(Room currRoom) {
        // Iterates through all the tiles the Room would take up
        for (int i = currRoom.xCoord; i < currRoom.xCoord + currRoom.width; i++) {
            for (int j = currRoom.yCoord; j < currRoom.yCoord + currRoom.height; j++) {
                if (!world[i][j].equals(Tileset.NOTHING)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Generates Room onto playing area. */
    private void putRoomOnBoard(Room currRoom) {
        allRooms.add(currRoom);
        for (int i = currRoom.xCoord; i < currRoom.xCoord + currRoom.width; i++) {
            for (int j = currRoom.yCoord; j < currRoom.yCoord + currRoom.height; j++) {
                world[i][j] = Tileset.FLOOR;
            }
        }
        for (int i = currRoom.xCoord; i < currRoom.xCoord + currRoom.width; i++) {
            world[i][currRoom.yCoord] = Tileset.WALL;
            world[i][currRoom.yCoord + currRoom.height - 1] = Tileset.WALL;
        }
        for (int i = currRoom.yCoord; i < currRoom.yCoord + currRoom.height; i++) {
            world[currRoom.xCoord][i] = Tileset.WALL;
            world[currRoom.xCoord + currRoom.width - 1][i] = Tileset.WALL;
        }
    }

    /** Checks whether a hall can be built between the two rooms. */
    public boolean ableToBuildHall(Room r1, Room r2) {
        // Determining begin/end halls
        Room[] toStartAndEnd = toStartWith(r1, r2);
        Room beginHall = toStartAndEnd[0];
        Room endHall = toStartAndEnd[1];
        int[] vals = new int[3];
        int startX = 0;
        int startY = 0;
        int endX = 0;
        int endY = 0;

        // Determining whether the endHall is above or below beginHall
        boolean up = false;
        if (beginHall.yCoord <= endHall.yCoord) {
            up = true;
        }

        // Checks where the Rooms are in relation to each other
        if (horizontalCheck(beginHall, endHall)) { //Horizontal only
            vals = valsForHorizontalCheck(beginHall, endHall, up);
            startX = vals[0];
            startY = vals[1];
            endX = vals[2];

            /** Not sure what this was for
            if (up) {
                if (startY == endHall.yCoord + 1) {
                    return false;
                }
            } else if (startY == endHall.yCoord + endHall.height - 1) {
                    return false;
            } */

            for (int i = startX; i <= endX; i++) {
                if (!world[i][startY].equals(Tileset.NOTHING)
                        || !world[i][startY + 1].equals(Tileset.NOTHING)
                        || !world[i][startY - 1].equals(Tileset.NOTHING)) {
                    return false;
                }
            }
        } else if (verticalCheck(beginHall, endHall)) { //Vertical only
            vals = valsForVerticalCheck(beginHall, endHall, up);
            startX = vals[0];
            startY = vals[1];
            endY = vals[2];

            if (startX == endHall.xCoord + endHall.width - 1) {
                return false;
            }

            if (up) {
                for (int i = startY; i <= endY; i++) {
                    if (!world[startX][i].equals(Tileset.NOTHING)
                            || !world[startX + 1][i].equals(Tileset.NOTHING)
                            || !world[startX - 1][i].equals(Tileset.NOTHING)) {
                        return false;
                    }
                }
            } else {
                for (int i = startY; i >= endY; i--) {
                    if (!world[startX][i].equals(Tileset.NOTHING)
                            || !world[startX + 1][i].equals(Tileset.NOTHING)
                            || !world[startX - 1][i].equals(Tileset.NOTHING)) {
                        return false;
                    }
                }
            }
        } else { //Both horizontal and vertical
            vals = valsForHybridHall(beginHall, endHall, up);
            startX = vals[0];
            startY = vals[1];
            endX = vals[2];
            endY = vals[3];
            return hybridHallCheck(startX, startY, endX, endY, up);
        }
        return true;
    }

    /** Generates a hall between the two rooms. */
    public void buildHall(Room r1, Room r2) {
        //Determining begin/end halls
        Room[] toStartAndEnd = toStartWith(r1, r2);
        Room beginHall = toStartAndEnd[0];
        Room endHall = toStartAndEnd[1];
        boolean up = false;
        if (beginHall.yCoord <= endHall.yCoord) {
            up = true;
        }
        int[] vals = new int[3];
        int startX = 0;
        int startY = 0;
        int endX = 0;
        int endY = 0;

        //Horizontal only
        if (horizontalCheck(beginHall, endHall)) {
            vals = valsForHorizontalCheck(beginHall, endHall, up);
            startX = vals[0];
            startY = vals[1];
            endX = vals[2];

            horizontalPlacement(startX, startY, endX, endY);

        //Vertical only
        } else if (verticalCheck(beginHall, endHall)) {
            vals = valsForVerticalCheck(beginHall, endHall, up);
            startX = vals[0];
            startY = vals[1];
            endY = vals[2];

            verticalPlacement(startX, startY, endX, endY, up);

        //Both horizontal and vertical
        } else {
            vals = valsForHybridHall(beginHall, endHall, up);
            startX = vals[0];
            startY = vals[1];
            endX = vals[2];
            endY = vals[3];
            //Going right/left
            //Going up/down
            horizontalVerticalPlacement(startX, startY, endX,
                endY, up);
        }
    }

    /** Determines which Room to being building a hall with. */
    public Room[] toStartWith(Room r1, Room r2) {
        Room[] rv = new Room[2];
        if (r1.xCoord < r2.xCoord) {
            rv[0] = r1;
        } else if (r1.xCoord > r2.xCoord) {
            rv[0] = r2;
        } else if (r1.yCoord < r2.yCoord) {
            rv[0] = r1;
        } else {
            rv[0] = r2;
        }
        if (rv[0] == r1) {
            rv[1] = r2;
        } else {
            rv[1] = r1;
        }
        return rv;
    }

    /** Checks if two rooms that need a hall built are directly horizontal. */
    public boolean horizontalCheck(Room beginHall, Room endHall) {
        return beginHall.yCoord + beginHall.height == endHall.yCoord + endHall.height
                || beginHall.yCoord == endHall.yCoord
                || (beginHall.yCoord < endHall.yCoord
                && beginHall.yCoord + beginHall.height - 2 > endHall.yCoord)
                || (beginHall.yCoord + 1 < endHall.yCoord + endHall.height - 1
                && beginHall.yCoord + beginHall.height - 1 > endHall.yCoord + endHall.height - 1)
                || (beginHall.yCoord > endHall.yCoord && beginHall.yCoord + beginHall.height - 1
                < endHall.yCoord + endHall.height - 1);
    }

    /** Returns startX, startY, and endX of two rooms. */
    public int[] valsForHorizontalCheck(Room beginHall, Room endHall, boolean up) {
        int[] rv = new int[3];

        rv[0] = beginHall.xCoord + beginHall.width;
        // Values for if beginHall's y-boundaries are within endHalls'
        if (beginHall.yCoord > endHall.yCoord && beginHall.yCoord + beginHall.height - 1
                < endHall.yCoord + endHall.height - 1) {
            rv[1] = beginHall.yCoord + 1;
        } else if (up) {
            rv[1] = endHall.yCoord + 1;
        } else {
            rv[1] = endHall.yCoord + endHall.height - 2;
        }
        rv[2] = endHall.xCoord - 1;

        return rv;
    }


    public void horizontalPlacement(int startX, int startY, int endX, int endY) {
        for (int i = startX; i <= endX; i++) {
            world[i][startY] = Tileset.FLOOR;
            world[i][startY + 1] = Tileset.WALL;
            world[i][startY - 1] = Tileset.WALL;
        }
        world[startX - 1][startY] = Tileset.FLOOR;
        world[endX + 1][startY] = Tileset.FLOOR;
    }

    public boolean verticalCheck(Room beginHall, Room endHall) {
        return beginHall.xCoord + beginHall.width == endHall.xCoord + endHall.width
                || beginHall.xCoord == endHall.xCoord
                || (beginHall.xCoord < endHall.xCoord
                && beginHall.xCoord + beginHall.width - 2 > endHall.xCoord)
                || (beginHall.xCoord + 1 < endHall.xCoord + endHall.width - 1
                && endHall.xCoord + endHall.width - 1 > endHall.xCoord + endHall.width - 1);
    }

    public int[] valsForVerticalCheck(Room beginHall, Room endHall, boolean up) {
        int[] rv = new int[3];

        rv[0] = endHall.xCoord + 1;
        if (up) {
            rv[1] = beginHall.yCoord + beginHall.height;
            rv[2] = endHall.yCoord - 1;
        } else {
            rv[1] = beginHall.yCoord - 1;
            rv[2] = endHall.yCoord + endHall.height;
        }

        return rv;
    }

    public void verticalPlacement(int startX, int startY, int endX, int endY, boolean up) {
        if (up) {
            for (int i = startY; i <= endY; i++) {
                world[startX][i] = Tileset.FLOOR;
                world[startX + 1][i] = Tileset.WALL;
                world[startX - 1][i] = Tileset.WALL;
            }
            world[startX][endY + 1] = Tileset.FLOOR;
            world[startX][startY - 1] = Tileset.FLOOR;
        } else {
            for (int i = startY; i >= endY; i--) {
                world[startX][i] = Tileset.FLOOR;
                world[startX + 1][i] = Tileset.WALL;
                world[startX - 1][i] = Tileset.WALL;
            }
            world[startX][endY - 1] = Tileset.FLOOR;
            world[startX][startY + 1] = Tileset.FLOOR;
        }
    }

    public void horizontalVerticalPlacement(int startX, int startY, int endX,
                                  int endY, boolean up) {
        world[startX - 1][startY] = Tileset.FLOOR; //changes + 1 to - 1
        for (int i = startX; i <= endX + 1; i++) {
            world[i][startY] = Tileset.FLOOR;
            world[i][startY + 1] = Tileset.WALL;
            world[i][startY - 1] = Tileset.WALL;
        }
        world[endX + 1][startY] = Tileset.WALL;
        if (up) {
            for (int j = startY + 1; j <= endY; j++) {
                world[endX][j] = Tileset.FLOOR;
                world[endX - 1][j] = Tileset.WALL;
                world[endX + 1][j] = Tileset.WALL;
            }
            world[endX][endY + 1] = Tileset.FLOOR;
        } else {
            for (int j = startY - 1; j >= endY; j--) {
                world[endX][j] = Tileset.FLOOR;
                world[endX + 1][j] = Tileset.WALL;
                world[endX - 1][j] = Tileset.WALL;
            }
            world[endX][endY - 1] = Tileset.FLOOR;
        }
    }

    public boolean hybridHallCheck(int startX, int startY, int endX, int endY, boolean up) {
        for (int i = startX; i <= endX + 1; i++) {
            if (!world[i][startY].equals(Tileset.NOTHING)
                    || !world[i][startY + 1].equals(Tileset.NOTHING)
                    || !world[i][startY - 1].equals(Tileset.NOTHING)) {
                return false;
            }
        }
        //Going up/down
        if (up) {
            for (int i = startY + 1; i <= endY; i++) {
                if (!world[endX][i].equals(Tileset.NOTHING)
                        || !world[endX + 1][i].equals(Tileset.NOTHING)
                        || !world[endX - 1][i].equals(Tileset.NOTHING)) {
                    return false;
                }
            }
        } else {
            if (startY == endY - 1) {
                return false;
            }
            for (int i = startY - 1; i >= endY; i--) {
                if (!world[endX][i].equals(Tileset.NOTHING)
                        || !world[endX + 1][i].equals(Tileset.NOTHING)
                        || !world[endX - 1][i].equals(Tileset.NOTHING)) {
                    return false;
                }
            }
        }
        return true;
    }

    public int[] valsForHybridHall(Room beginHall, Room endHall, boolean up) {
        int[] rv = new int[4];

        rv[0] = beginHall.xCoord + beginHall.width;
        rv[1] = beginHall.yCoord + 1;
        rv[2] = endHall.xCoord + 1;
        if (up) {
            rv[3] = endHall.yCoord - 1;
        } else {
            rv[3] = endHall.yCoord + endHall.height;
        }

        return rv;
    }

    public void putCharacterInSpot() {
        Random randomRoom = new Random(allRooms.size());
        Room toStart = allRooms.get(randomRoom.nextInt(allRooms.size()));
        playerX = toStart.xCoord + toStart.width / 2;
        playerY = toStart.yCoord + toStart.height / 2;
        world[playerX][playerY] = Tileset.AVATAR;
    }

    private void putCoinsInSpots() {
        int coinsInMap = allRooms.size() * 2;
        int count = 0;
        coinGoal = 0;
        numOfMoves = allRooms.size() * 50;
        Random randomRoom = new Random(allRooms.size());
        while (count < coinsInMap) {
            int xVal = randomRoom.nextInt(WIDTH);
            int yVal = randomRoom.nextInt(HEIGHT);
            if (world[xVal][yVal].equals(Tileset.FLOOR)) {
                world[xVal][yVal] = Tileset.COIN;
                count += 1;
                coinGoal += 1;
            }
        }
    }

    public void moveToDirection(char moveToMake) {
        if (moveToMake == 'W') {
            goUp();
        } else if (moveToMake == 'A') {
            goLeft();
        } else if (moveToMake == 'S') {
            goDown();
        } else if (moveToMake == 'D') {
            goRight();
        } else if (moveToMake == 'T') {
            teleport();
        }
    }

    public void goUp() {
        //check if wall is in position [playerX][playerY + 1]: do not move
        //check if floor is in position [playerX][playerY + 1]: move up(set new player coords)
        if (jumpScare) {
            if (playerY + 1 < HEIGHT && world[playerX][playerY + 1].equals(Tileset.WALL)) {
                jumpscareImage();
            }
        }
        if (playerY + 1 < HEIGHT
                && (world[playerX][playerY + 1].equals(Tileset.FLOOR)
                || world[playerX][playerY + 1].equals(Tileset.COIN))) {
            if (world[playerX][playerY + 1].equals(Tileset.COIN)) {
                coinsCollected += 1;
                if (!mute) {
                    coinAudio();
                }
            }
            if (!mute) {
                stepAudio();
            }
            world[playerX][playerY] = Tileset.FLOOR;
            playerY += 1;
            world[playerX][playerY] = Tileset.AVATAR;
            numOfMoves -= 1;
        }
        //might want to render within here
    }

    public void goDown() {
        //check if wall is in position [playerX][playerY - 1]: do not move
        //check if floor is in position [playerX][playerY - 1]: move down(set new player coords)
        if (jumpScare) {
            if (playerY - 1 >= 0 && world[playerX][playerY - 1].equals(Tileset.WALL)) {
                jumpscareImage();
            }
        }
        if (playerY - 1 >= 0
                && (world[playerX][playerY - 1].equals(Tileset.FLOOR)
                || world[playerX][playerY - 1].equals(Tileset.COIN))) {
            if (world[playerX][playerY - 1].equals(Tileset.COIN)) {
                coinsCollected += 1;
                if (!mute) {
                    coinAudio();
                }
            }
            if (!mute) {
                stepAudio();
            }
            world[playerX][playerY] = Tileset.FLOOR;
            playerY -= 1;
            world[playerX][playerY] = Tileset.AVATAR;
            numOfMoves -= 1;
        }
        //might want to render within here
    }

    public void goLeft() {
        //check if wall is in position [playerX - 1][playerY]: do not move
        //check if floor is in position [playerX - 1][playerY]: move left(set new player coords)
        if (jumpScare) {
            if (playerX - 1 >= 0 && world[playerX - 1][playerY].equals(Tileset.WALL)) {
                jumpscareImage();
            }
        }
        if (playerX - 1 >= 0
                && (world[playerX - 1][playerY].equals(Tileset.FLOOR)
                || world[playerX - 1][playerY].equals(Tileset.COIN))) {
            if (world[playerX - 1][playerY].equals(Tileset.COIN)) {
                coinsCollected += 1;
                if (!mute) {
                    coinAudio();
                }
            }
            if (!mute) {
                stepAudio();
            }
            world[playerX][playerY] = Tileset.FLOOR;
            playerX -= 1;
            world[playerX][playerY] = Tileset.AVATAR;
            numOfMoves -= 1;
        }
        //might want to render within here
    }

    public void goRight() {
        //check if wall is in position [playerX + 1][playerY]: do not move
        //check if floor is in position [playerX + 1][playerY]: move right(set new player coords)
        if (jumpScare) {
            if (playerX + 1 < WIDTH && world[playerX + 1][playerY].equals(Tileset.WALL)) {
                jumpscareImage();
            }
        }
        if (playerX + 1 < WIDTH
                && (world[playerX + 1][playerY].equals(Tileset.FLOOR)
                || world[playerX + 1][playerY].equals(Tileset.COIN))) {
            if (world[playerX + 1][playerY].equals(Tileset.COIN)) {
                coinsCollected += 1;
                if (!mute) {
                    coinAudio();
                }
            }
            if (!mute) {
                stepAudio();
            }
            world[playerX][playerY] = Tileset.FLOOR;
            playerX += 1;
            world[playerX][playerY] = Tileset.AVATAR;
            numOfMoves -= 1;
        }
        //might want to render within here
    }

    public void teleport() {
        Room teleportTo = allRooms.get(currentRandom.nextInt(allRooms.size()));
        numOfMoves -= 50;
        if (world[teleportTo.xCoord + teleportTo.width / 2]
                [teleportTo.yCoord + teleportTo.height / 2] == Tileset.COIN) {
            coinsCollected += 1;
            if (!mute) {
                coinAudio();
            }
        }
        world[playerX][playerY] = Tileset.FLOOR;
        playerX = teleportTo.xCoord + teleportTo.width / 2;
        playerY = teleportTo.yCoord + teleportTo.height / 2;
        world[playerX][playerY] = Tileset.AVATAR;
    }

    private void jumpscareImage() {
        StdDraw.clear(Color.black);
        StdDraw.picture(45, 25, IMAGES + File.separator + "jumpscare.jpeg");
        StdDraw.show();
        jumpscareAudio();
        StdDraw.pause(3000);
    }

    private void jumpscareAudio() {
        try {
            AudioInputStream jumpscareAudio
                    = AudioSystem.getAudioInputStream(Utils.join(AUDIO, "jumpscare.wav"));
            Clip clip = AudioSystem.getClip();
            clip.open(jumpscareAudio);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
            ex.printStackTrace();
        }
    }

    private void startMenuMusic() {
        try {
            AudioInputStream menuAudio
                    = AudioSystem.getAudioInputStream(Utils.join(AUDIO, "menu.wav"));
            menuMusic = AudioSystem.getClip();
            menuMusic.open(menuAudio);
            menuMusic.loop(Clip.LOOP_CONTINUOUSLY);
            menuMusic.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
            ex.printStackTrace();
        }
    }

    private void coinAudio() {
        try {
            AudioInputStream coinSound
                    = AudioSystem.getAudioInputStream(Utils.join(AUDIO, "coins.wav"));
            Clip clip = AudioSystem.getClip();
            clip.open(coinSound);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
            ex.printStackTrace();
        }
    }

    private void stepAudio() {
        try {
            AudioInputStream stepSound
                    = AudioSystem.getAudioInputStream(Utils.join(AUDIO, "step.wav"));
            Clip clip = AudioSystem.getClip();
            clip.open(stepSound);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
            ex.printStackTrace();
        }
    }

    private void winMusic() {
        try {
            AudioInputStream winMusic
                    = AudioSystem.getAudioInputStream(Utils.join(AUDIO, "win.wav"));
            Clip clip = AudioSystem.getClip();
            clip.open(winMusic);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
            ex.printStackTrace();
        }
    }

    private void loseMusic() {
        try {
            AudioInputStream loseMusic
                    = AudioSystem.getAudioInputStream(Utils.join(AUDIO, "lose.wav"));
            Clip clip = AudioSystem.getClip();
            clip.open(loseMusic);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
            ex.printStackTrace();
        }
    }

    public String toString() {
        return TETile.toString(world);
    }

    /** Seed of the world. */
    private long getCurrSeed() {
        return currSeed;
    }

    private String getAllInput() {
        return allInput;
    }

    private boolean getLineOfSight() {
        return lineOfSight;
    }

    private boolean getJumpscare() {
        return jumpScare;
    }

    private boolean getMute() {
        return mute;
    }

    public static void main(String[] args) {
        Engine e = new Engine();
        Room room1 = e.new Room(1, 10, 5, 5);
        Room room2 = e.new Room(11, 7, 5, 5);
        e.putRoomOnBoard(room1);
        if (e.checkRoomValid(room2)) {
            if (e.ableToBuildHall(e.allRooms.get(e.allRooms.size() - 1), room2)) {
                e.putRoomOnBoard(room2);
                e.buildHall(e.allRooms.get(e.allRooms.size() - 2), room2);
            }
        }
        System.out.println(e.toString());
    }
}

