import javafx.scene.canvas.*;
import javafx.geometry.VPos;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

/**
 * A JavaFX GUI element that displays a grid on which you can draw images, text and lines.
 */
public class JFXArena extends Pane
{
    // Represents the image to draw. You can modify this to introduce multiple images.
    private static final String IMAGE_FILE = "rg1024-isometric-tower.png";
    private Image robot1;
    private Image robot2;
    // The following values are arbitrary, and you may need to modify them according to the 
    // requirements of your application.
    private int gridWidth = 5;
    private int gridHeight = 5;
    private double robotX = 2.0;
    private double robotY = 2.0;
    private int[][] gridTracker = new int[5][5];
    private double gridSquareSize; // Auto-calculated
    private Canvas canvas; // Used to provide a 'drawing surface'.
    private Object mutex;
    private int robotCounter = 1;
    private LinkedBlockingQueue<Droid> droidList;
    private List<ArenaListener> listeners = null;
    private ScheduledExecutorService spawnDroidService;
    private SynchronousQueue firingQueue;
    private ExecutorService firingService;
    private TextArea logger;
    private Object gameOverMutex;
    private Label scoreLabel;
    private GameController game;
    /**
     * Creates a new arena object, loading the robot image and initialising a drawing surface.
     */
    public JFXArena(TextArea logger, Label label)
    {
        // create a mutex to control game over
        gameOverMutex = new Object();
        // used to queue commands
        firingQueue = new SynchronousQueue<>();
        // used to queue list of droids
        droidList = new LinkedBlockingQueue<>();
        // used to schedule droids
        spawnDroidService = Executors.newScheduledThreadPool(10);
        // thread pool to store commands
        firingService = new ThreadPoolExecutor(4,8,1000, TimeUnit.MILLISECONDS, firingQueue);
        // initially set all grid spaces to not occupied
        for(int row = 0; row < gridTracker.length; row++)
        {
            for(int column = 0; column < gridTracker[row].length;column++)
            {
                gridTracker[row][column] = 0;
            }
        }
        // use mutex to stop grid race conditions
        mutex = new Object();
        InputStream is = getClass().getClassLoader().getResourceAsStream(IMAGE_FILE);
        if(is == null)
        {
            throw new AssertionError("Cannot find image file " + IMAGE_FILE);
        }
        // set image and canvas
        robot1 = new Image(is);
        canvas = new Canvas();
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        getChildren().add(canvas);
        // schedule droid service to run after time specified
        spawnDroidService.scheduleAtFixedRate(new SpawnDroid(this) , 1000, 2000, TimeUnit.MILLISECONDS);
        // set logged and label
        this.logger = logger;
        this.scoreLabel = label;
        // create new game controller to control inputs and outputs
        game = new GameController(this);
    }
    
    /**********************************
     * Purpose: Used to spawn a droid in each corner of the grid
     * Author: Aaron Gangemi
     */
    private class SpawnDroid implements Runnable
    {
        private InputStream robotEnemyInputStream = getClass().getClassLoader().getResourceAsStream("rg1024-robot-carrying-things-4.png");
        private GraphicsContext gfx = canvas.getGraphicsContext2D();
        private JFXArena arena;
        public SpawnDroid(JFXArena arena)
        {
            // uses the arena
            this.arena = arena;
        }
        
        /***************************************
         * Purpose: To schedule the droids to spawn in a new thread every
         * 2 seconds if either of the 4 corners are empty
         */
        @Override
        public void run()
        {
            // if the mutex is not being used by move, then spawning droids 
            // will be run.
            
            synchronized(mutex)
            {
                if(gridTracker[0][0] == 0 && gridTracker[2][2] != 1)
                {
                    // check if grid space is empty and game is not over
                    SetDroidCoordinates(0,0);
                    // create droid and set coordinates
                    // refresh GUI
                    refreshLayout();
                }
                if(gridTracker[4][0] == 0 && gridTracker[2][2] != 1)
                {
                    // check if grid space is empty and game is not over
                    SetDroidCoordinates(4,0);
                    // create droid and set coordinates
                    // refresh GUI
                    refreshLayout();
                }
                if(gridTracker[0][4] == 0 && gridTracker[2][2] != 1)
                {
                    // check if grid space is empty and game is not over
                    SetDroidCoordinates(0,4);
                    // create droid and set coordinates
                    // refresh GUI
                    refreshLayout();
                }
                if(gridTracker[4][4] == 0 && gridTracker[2][2] != 1)
                {
                    // check if grid space is empty and game is not over
                    SetDroidCoordinates(4,4);
                    // create droid and set coordinates
                    // refresh GUI
                    refreshLayout();
                }
                // release mutex after corners are filled
                mutex.notify();
            }
        }
        /*********************************
         * Purpose: create a new droid, set its coordinates and add it to the
         * list for the GUI
         * @param XCoordinate
         * @param YCoordinate 
         */
        private void SetDroidCoordinates(int XCoordinate, int YCoordinate)
        {
            Droid droid = new Droid(robotCounter, arena);
            droid.setCurrentXCoordinate(YCoordinate);
            droid.setCurrentYCoordinate(XCoordinate);
            // set coordinates of new droid
            gridTracker[(int)droid.getCurrentYCoordinate()][(int)droid.getCurrentXCoordinate()] = 1;
            // set grid space to occupied
            // dont need to check for race condition as handled by mutex above
            robotCounter++;
            // add to list for GUI Draw
            droidList.add(droid);
        }
    }
    
    /*********************************
     * Purpose: Refresh GUI layout after manipulation
     */
    public void refreshLayout()
    {
        requestLayout();
    }
    
    /**
     * Adds a callback for when the user clicks on a grid square within the arena. The callback 
     * (of type ArenaListener) receives the grid (x,y) coordinates as parameters to the 
     * 'squareClicked()' method.
     */
    public void addListener(ArenaListener newListener)
    {
        if(listeners == null)
        {
            listeners = new LinkedList<>();
            setOnMouseClicked(event ->
            {
                int gridX = (int)(event.getX() / gridSquareSize);
                int gridY = (int)(event.getY() / gridSquareSize);
                
                if(gridX < gridWidth && gridY < gridHeight)
                {
                    for(ArenaListener listener : listeners)
                    {   
                        listener.squareClicked(gridX, gridY);
                    }
                    if(gridTracker[gridY][gridX] == 1)
                    {
                        long initialTime = System.currentTimeMillis();
                        firingService.execute(game.createNewCommand(gridX, gridY, initialTime));
                    }
                    else
                    {
                        Platform.runLater(new Runnable(){
                            @Override
                            public void run()
                            {
                                logger.appendText("Shot missed: No droid at coordinates (" + gridY + "," + gridX + ")\n");          
                            }
                        });
                    }
                }
                listeners.add(newListener);
                });
        }
    }
    
    /*******************************
     * Purpose: Used to get grid tracker
     * @return gridTracker
     */
    public int[][] getGridTracker()
    {
        return gridTracker;
    }
    
    /********************************
     * Purpose: used to get mutex for spawning and moving to avoid race
     * @return mutex
     */
    public Object getMutex()
    {
        return mutex;
    }
    
    /**********************************
     * Purpose: Used to retrieve game over mutex
     * @return gameOverMutex
     */
    public Object getGameOverMutex()
    {
        return gameOverMutex;
    }
    
    /*********************************
     * Purpose: used to retrieve current canvas
     * @return canvas
     */
    public Canvas getCanvas()
    {
        return canvas;
    }
    
    /**********************************
     * Purpose: Used to get executor droid spawn after schedule
     * @return spawnDroidService
     */
    public ScheduledExecutorService getSpawnDroidService()
    {
        return spawnDroidService;
    }
    
    /****************************************
     * Purpose: Used to get queue of droids
     * @return droidList
     */
    public LinkedBlockingQueue<Droid> getDroidList()
    {
        return droidList;
    }
    
    /**********************************
     * Purpose: Used to get score label
     * @return scoreLabel
     */
    public Label getScoreLabel()
    {
        return scoreLabel;
    }
    
    /**********************************
     * Purpose: Used to get logger
     * @return logger
     */
    public TextArea getLogger()
    {
        return logger;
    }
        
    
        
    /**
     * This method is called in order to redraw the screen, either because the user is manipulating 
     * the window, OR because you've called 'requestLayout()'.
     *
     * You will need to modify the last part of this method; specifically the sequence of calls to
     * the other 'draw...()' methods. You shouldn't need to modify anything else about it.
     */
    @Override
    public void layoutChildren()
    {
        super.layoutChildren(); 
        GraphicsContext gfx = canvas.getGraphicsContext2D();
        gfx.clearRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight());
        
        // First, calculate how big each grid cell should be, in pixels. (We do need to do this
        // every time we repaint the arena, because the size can change.)
        gridSquareSize = Math.min(
            getWidth() / (double) gridWidth,
            getHeight() / (double) gridHeight);
            
        double arenaPixelWidth = gridWidth * gridSquareSize;
        double arenaPixelHeight = gridHeight * gridSquareSize;
            
            
        // Draw the arena grid lines. This may help for debugging purposes, and just generally
        // to see what's going on.
        gfx.setStroke(Color.DARKGREY);
        gfx.strokeRect(0.0, 0.0, arenaPixelWidth - 1.0, arenaPixelHeight - 1.0); // Outer edge

        for(int gridX = 1; gridX < gridWidth; gridX++) // Internal vertical grid lines
        {
            double x = (double) gridX * gridSquareSize;
            gfx.strokeLine(x, 0.0, x, arenaPixelHeight);
        }
        
        for(int gridY = 1; gridY < gridHeight; gridY++) // Internal horizontal grid lines
        {
            double y = (double) gridY * gridSquareSize;
            gfx.strokeLine(0.0, y, arenaPixelWidth, y);
        }

        // Invoke helper methods to draw things at the current location.
        // ** You will need to adapt this to the requirements of your application. **
        InputStream robotEnemyInputStream = getClass().getClassLoader().getResourceAsStream("rg1024-robot-carrying-things-4.png");
        robot2 = new Image(robotEnemyInputStream);
        for(Droid d : droidList)
        {
            drawImage(gfx, robot2, d.getCurrentXCoordinate(), d.getCurrentYCoordinate());
            drawLabel(gfx, "Robot " + d.getId(), d.getCurrentXCoordinate(),d.getCurrentYCoordinate());
        }        
        drawImage(gfx, robot1, robotX, robotY);
        drawLabel(gfx, "Robot Name", robotX, robotY);
    }
    
    
    /** 
     * Draw an image in a specific grid location. *Only* call this from within layoutChildren(). 
     *
     * Note that the grid location can be fractional, so that (for instance), you can draw an image 
     * at location (3.5,4), and it will appear on the boundary between grid cells (3,4) and (4,4).
     *     
     * You shouldn't need to modify this method.
     */
    private void drawImage(GraphicsContext gfx, Image image, double gridX, double gridY)
    {
        // Get the pixel coordinates representing the centre of where the image is to be drawn. 
        double x = (gridX + 0.5) * gridSquareSize;
        double y = (gridY + 0.5) * gridSquareSize;
        
        // We also need to know how "big" to make the image. The image file has a natural width 
        // and height, but that's not necessarily the size we want to draw it on the screen. We 
        // do, however, want to preserve its aspect ratio.
        double fullSizePixelWidth = robot1.getWidth();
        double fullSizePixelHeight = robot1.getHeight();
        
        double displayedPixelWidth, displayedPixelHeight;
        if(fullSizePixelWidth > fullSizePixelHeight)
        {
            // Here, the image is wider than it is high, so we'll display it such that it's as 
            // wide as a full grid cell, and the height will be set to preserve the aspect 
            // ratio.
            displayedPixelWidth = gridSquareSize;
            displayedPixelHeight = gridSquareSize * fullSizePixelHeight / fullSizePixelWidth;
        }
        else
        {
            // Otherwise, it's the other way around -- full height, and width is set to 
            // preserve the aspect ratio.
            displayedPixelHeight = gridSquareSize;
            displayedPixelWidth = gridSquareSize * fullSizePixelWidth / fullSizePixelHeight;
        }

        // Actually put the image on the screen.
        gfx.drawImage(image,
            x - displayedPixelWidth / 2.0,  // Top-left pixel coordinates.
            y - displayedPixelHeight / 2.0, 
            displayedPixelWidth,              // Size of displayed image.
            displayedPixelHeight);
    }
    
    
    /**
     * Displays a string of text underneath a specific grid location. *Only* call this from within 
     * layoutChildren(). 
     *     
     * You shouldn't need to modify this method.
     */
    private void drawLabel(GraphicsContext gfx, String label, double gridX, double gridY)
    {
        gfx.setTextAlign(TextAlignment.CENTER);
        gfx.setTextBaseline(VPos.TOP);
        gfx.setStroke(Color.BLUE);
        gfx.strokeText(label, (gridX + 0.5) * gridSquareSize, (gridY + 1.0) * gridSquareSize);
    }
    
    
}