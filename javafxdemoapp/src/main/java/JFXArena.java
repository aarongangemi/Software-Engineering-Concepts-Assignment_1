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
        gameOverMutex = new Object();
        firingQueue = new SynchronousQueue<>();
        droidList = new LinkedBlockingQueue<>();
        spawnDroidService = Executors.newScheduledThreadPool(10);
        firingService = new ThreadPoolExecutor(4,8,1000, TimeUnit.MILLISECONDS, firingQueue);
        // Here's how you get an Image object from an image file (which you provide in the 
        // 'resources/' directory).
        for(int row = 0; row < gridTracker.length; row++)
        {
            for(int column = 0; column < gridTracker[row].length;column++)
            {
                gridTracker[row][column] = 0;
            }
        }
        mutex = new Object();
        InputStream is = getClass().getClassLoader().getResourceAsStream(IMAGE_FILE);
        if(is == null)
        {
            throw new AssertionError("Cannot find image file " + IMAGE_FILE);
        }
        robot1 = new Image(is);
        canvas = new Canvas();
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        getChildren().add(canvas);
        spawnDroidService.scheduleAtFixedRate(new SpawnDroid(this) , 1000, 1500, TimeUnit.MILLISECONDS);
        this.logger = logger;
        this.scoreLabel = label;
        game = new GameController(this);
    }
    
    private class SpawnDroid implements Runnable{
        private InputStream robotEnemyInputStream = getClass().getClassLoader().getResourceAsStream("rg1024-robot-carrying-things-4.png");
        private GraphicsContext gfx = canvas.getGraphicsContext2D();
        private JFXArena arena;
        public SpawnDroid(JFXArena arena)
        {
            this.arena = arena;
        }
        
        @Override
        public void run()
        {
            synchronized(mutex)
            {
                if(gridTracker[0][0] == 0 && gridTracker[2][2] != 1)
                {
                    SetDroidCoordinates(0,0);
                    refreshLayout();
                }
                if(gridTracker[4][0] == 0 && gridTracker[2][2] != 1)
                {
                    SetDroidCoordinates(4,0);
                    refreshLayout();
                }
                if(gridTracker[0][4] == 0 && gridTracker[2][2] != 1)
                {
                    SetDroidCoordinates(0,4);
                    refreshLayout();
                }
                if(gridTracker[4][4] == 0 && gridTracker[2][2] != 1)
                {
                    SetDroidCoordinates(4,4);
                    refreshLayout();
                }
                mutex.notify();
            }
        }
        private void SetDroidCoordinates(int XCoordinate, int YCoordinate)
        {
            Droid droid = new Droid(robotCounter, arena);
            droid.setOldXCoordinate(XCoordinate);
            droid.setOldYCoordinate(YCoordinate);
            droid.setCurrentXCoordinate(YCoordinate);
            droid.setCurrentYCoordinate(XCoordinate);
            gridTracker[(int)droid.getCurrentYCoordinate()][(int)droid.getCurrentXCoordinate()] = 1;
            robotCounter++;
            droidList.add(droid);
        }
    }
    
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
    
    public int[][] getGridTracker()
    {
        return gridTracker;
    }
    
    public Object getMutex()
    {
        return mutex;
    }
    
    public Object getGameOverMutex()
    {
        return gameOverMutex;
    }
    
    public Canvas getCanvas()
    {
        return canvas;
    }
    
    public ScheduledExecutorService getSpawnDroidService()
    {
        return spawnDroidService;
    }
    
    public LinkedBlockingQueue<Droid> getDroidList()
    {
        return droidList;
    }
    
    public Label getScoreLabel()
    {
        return scoreLabel;
    }
    
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