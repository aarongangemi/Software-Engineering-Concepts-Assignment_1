import javafx.scene.canvas.*;
import javafx.geometry.VPos;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javafx.animation.PathTransition;
import javafx.scene.Node;
import javafx.scene.shape.Circle;

/**
 * A JavaFX GUI element that displays a grid on which you can draw images, text and lines.
 */
public class JFXArena extends Pane
{
    // Represents the image to draw. You can modify this to introduce multiple images.
    private static final String IMAGE_FILE = "1554047213.png";
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
    private LinkedBlockingQueue<Droid> droidList = new LinkedBlockingQueue<>();
    private List<ArenaListener> listeners = null;
    private boolean isStarted = false;
    
    /**
     * Creates a new arena object, loading the robot image and initialising a drawing surface.
     */
    public JFXArena()
    {
        // Here's how you get an Image object from an image file (which you provide in the 
        // 'resources/' directory).
        for(int row = 0; row < gridTracker.length; row++)
        {
            for(int column = 0; column < gridTracker[row].length;column++)
            {
                gridTracker[row][column] = 0;
            }
        }
        gridTracker[2][2] = 1;
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
        ScheduleSpawn();
        
    }
    
    private class SpawnDroid implements Runnable{
        private InputStream robotEnemyInputStream = getClass().getClassLoader().getResourceAsStream("rg1024-robot-carrying-things-4.png");
        private GraphicsContext gfx = canvas.getGraphicsContext2D();
        @Override
        public void run()
        {
            synchronized(mutex)
            {
                if(gridTracker[0][0] == 0)
                {
                    SetDroidCoordinates(0,0);
                    setRobotPosition(2,2);
                    System.out.println("0,0 is empty");
                }
                if(gridTracker[4][0] == 0)
                {
                    SetDroidCoordinates(4,0);
                    setRobotPosition(2,2);
                    System.out.println("4,0 is empty");
                }
                if(gridTracker[0][4] == 0)
                {
                    SetDroidCoordinates(0,4);
                    setRobotPosition(2,2);
                    System.out.println("0,4 is empty");
                }
                if(gridTracker[4][4] == 0)
                {
                    SetDroidCoordinates(4,4);
                    setRobotPosition(2,2);
                    System.out.println("4,4 is empty");
                }
                mutex.notify();
            }
            ScheduleDroidMove();
        }
        private void SetDroidCoordinates(int XCoordinate, int YCoordinate)
        {
            Droid droid = new Droid(robotCounter);
            droid.setOldXCoordinate(XCoordinate);
            droid.setOldYCoordinate(YCoordinate);
            droid.setCurrentXCoordinate(YCoordinate);
            droid.setCurrentYCoordinate(XCoordinate);
            gridTracker[XCoordinate][YCoordinate] = 1;
            robotCounter++;
            droidList.add(droid);
        }
    }
    
    private class MoveDroid implements Runnable{
        private GraphicsContext gfx = canvas.getGraphicsContext2D();
        private List<Integer> randomNumbersList = new ArrayList();
        private boolean moveCompleted = false;
        private long currentTimePassed = System.currentTimeMillis();
        private Droid d;
        public MoveDroid(Droid d)
        {
            this.d = d;
        }
        
        @Override
        public void run() {
           randomNumbersList.add(1);
           randomNumbersList.add(2);
           randomNumbersList.add(3);
           randomNumbersList.add(4);
               synchronized(mutex)
               {
                   
                   try
                   {
                           Collections.shuffle(randomNumbersList);
                           for(int j = 0; j < randomNumbersList.size(); j++)
                           {
                                  
                                int randomNumber = randomNumbersList.get(j);
                                
                                switch(randomNumber)
                                {
                                    case 2: //Move Up
                                        if(d.getCurrentYCoordinate() - 1.0 >= 0.0 && 
                                                gridTracker[(int) d.getCurrentYCoordinate()-1][(int) d.getCurrentXCoordinate()] == 0 &&
                                                !d.getDroidStatus())
                                        { 
                                            
                                                gridTracker[(int) d.getCurrentYCoordinate()][(int) d.getCurrentXCoordinate()] = 1;
                                                gridTracker[(int)d.getCurrentYCoordinate()-1][(int) d.getCurrentXCoordinate()] = 1;
                                                for(int row = 0; row < gridTracker.length; row++)
                                                    {
                                                         for(int column = 0; column < gridTracker[row].length;column++)
                                                         {
                                                             System.out.print(gridTracker[row][column]);
                                                         }
                                                         System.out.println("");
                                                    }
                                                d.setDroidStatus(true);
                                                d.setOldXCoordinate(d.getCurrentXCoordinate());
                                                d.setOldYCoordinate(d.getCurrentYCoordinate());
                                                d.setCurrentYCoordinate(d.getCurrentYCoordinate() - 1.0);
                                                d.setCurrentXCoordinate(d.getCurrentXCoordinate());
                                                setRobotPosition(d.getCurrentXCoordinate(), d.getCurrentYCoordinate());
                                                if(d.getOldYCoordinate() - 1.0 == d.getCurrentYCoordinate())
                                                {
                                                    gridTracker[(int) d.getCurrentYCoordinate()+1][(int) d.getCurrentXCoordinate()] = 0;
                                                    d.setDroidStatus(false);
                                                    System.out.println("...................GRID UPDATED........................");
                                                    for(int row = 0; row < gridTracker.length; row++)
                                                    {
                                                         for(int column = 0; column < gridTracker[row].length;column++)
                                                         {
                                                             System.out.print(gridTracker[row][column]);
                                                         }
                                                         System.out.println("");
                                                    }
                                                    System.out.println(".....................Move 2.............");
                                                    System.out.println("Current X Coordinate = " + d.getCurrentXCoordinate());
                                                    System.out.println("Current Y Coordinate = " + d.getCurrentYCoordinate());
                                                    System.out.println("Old X Coordinate = " + d.getOldXCoordinate());
                                                    System.out.println("Old Y Coordinate = " + d.getOldYCoordinate());
                                                    System.out.println("Droid Status: " + d.getDroidStatus());
                                                    moveCompleted = true;
                                                }
                                        }
                                        break;
                                    case 1: //Move Left
                                        if(d.getCurrentXCoordinate() - 1.0 >= 0.0 && 
                                                gridTracker[(int) d.getCurrentYCoordinate()][(int) d.getCurrentXCoordinate()-1] == 0 && !d.getDroidStatus())
                                        {
                                            gridTracker[(int) d.getCurrentYCoordinate()][(int) d.getCurrentXCoordinate()] = 1;
                                            gridTracker[(int)d.getCurrentYCoordinate()][(int) d.getCurrentXCoordinate()-1] = 1;
                                            for(int row = 0; row < gridTracker.length; row++)
                                                {
                                                     for(int column = 0; column < gridTracker[row].length;column++)
                                                     {
                                                         System.out.print(gridTracker[row][column]);
                                                     }
                                                     System.out.println("");
                                                }
                                            d.setDroidStatus(true);
                                            d.setOldXCoordinate(d.getCurrentXCoordinate());
                                            d.setOldYCoordinate(d.getCurrentYCoordinate());
                                            d.setCurrentXCoordinate(d.getCurrentXCoordinate() - 1.0);
                                            d.setCurrentYCoordinate(d.getCurrentYCoordinate());
                                            setRobotPosition(d.getCurrentXCoordinate(), d.getCurrentYCoordinate());
                                            if(d.getOldXCoordinate() - 1.0 == d.getCurrentXCoordinate())
                                            {
                                                gridTracker[(int) d.getCurrentYCoordinate()][(int) d.getCurrentXCoordinate()+1] = 0;
                                                d.setDroidStatus(false);
                                                System.out.println("...................GRID UPDATED........................");
                                                for(int row = 0; row < gridTracker.length; row++)
                                                {
                                                     for(int column = 0; column < gridTracker[row].length;column++)
                                                     {
                                                         System.out.print(gridTracker[row][column]);
                                                     }
                                                     System.out.println("");
                                                }
                                            }
                                            System.out.println(".....................Move 1.............");
                                            System.out.println("Current X Coordinate = " + d.getCurrentXCoordinate());
                                            System.out.println("Current Y Coordinate = " + d.getCurrentYCoordinate());
                                            System.out.println("Old X Coordinate = " + d.getOldXCoordinate());
                                            System.out.println("Old Y Coordinate = " + d.getOldYCoordinate());
                                            System.out.println("Droid Status: " + d.getDroidStatus());
                                            moveCompleted = true;
                                        }
                                        break;
                                    case 3: //Move Right
                                        
                                        if(d.getCurrentXCoordinate() + 1.0 <= 4.0 && 
                                                gridTracker[(int) d.getCurrentYCoordinate()][(int) d.getCurrentXCoordinate()+1] == 0 && !d.getDroidStatus())
                                        {
                                            d.setDroidStatus(true);
                                            gridTracker[(int) d.getCurrentYCoordinate()][(int) d.getCurrentXCoordinate()] = 1;
                                            gridTracker[(int)d.getCurrentYCoordinate()][(int) d.getCurrentXCoordinate()+1] = 1;
                                            d.setOldXCoordinate(d.getCurrentXCoordinate());
                                            d.setOldYCoordinate(d.getCurrentYCoordinate());
                                            d.setCurrentXCoordinate(d.getCurrentXCoordinate() +1);
                                            d.setCurrentYCoordinate(d.getCurrentYCoordinate());
                                            setRobotPosition(d.getCurrentXCoordinate(), d.getCurrentYCoordinate());
                                            if(d.getOldXCoordinate() + 1.0 == d.getCurrentXCoordinate())
                                            {
                                                d.setDroidStatus(false);
                                                gridTracker[(int) d.getCurrentYCoordinate()][(int) d.getCurrentXCoordinate()-1] = 0;
                                                System.out.println("...................GRID UPDATED........................");
                                                for(int row = 0; row < gridTracker.length; row++)
                                                {
                                                     for(int column = 0; column < gridTracker[row].length;column++)
                                                     {
                                                         System.out.print(gridTracker[row][column]);
                                                     }
                                                     System.out.println("");
                                                }
                                            }
                                            System.out.println(".....................Move 3.............");
                                            System.out.println("Current X Coordinate = " + d.getCurrentXCoordinate());
                                            System.out.println("Current Y Coordinate = " + d.getCurrentYCoordinate());
                                            System.out.println("Old X Coordinate = " + d.getOldXCoordinate());
                                            System.out.println("Old Y Coordinate = " + d.getOldYCoordinate());
                                            System.out.println("Droid Status: " + d.getDroidStatus());
                                            moveCompleted = true;
                                        }
                                        /*else
                                        {
                                            while(d.getOldXCoordinate()+1 != d.getCurrentXCoordinate())
                                            {
                                                long timePassed = System.currentTimeMillis();
                                                long timePassedSince = timePassed - currentTimePassed;
                                                currentTimePassed = timePassed;
                                                double gridSpaceMoved = timePassedSince/500;
                                                d.setCurrentXCoordinate(d.getCurrentXCoordinate() + gridSpaceMoved);
                                            }
                                            d.setDroidStatus(false);
                                            gridTracker[(int)d.getOldYCoordinate()][(int)d.getCurrentXCoordinate()] = 0;
                                        }*/
                                        break;
                                    case 4: //Move Down
                                        if(d.getCurrentYCoordinate() + 1.0 <= 4.0 && 
                                                gridTracker[(int) d.getCurrentYCoordinate()+1][(int) d.getCurrentXCoordinate()] == 0 && !d.getDroidStatus())
                                        {
                                            
                                            d.setDroidStatus(true);
                                            gridTracker[(int)d.getCurrentYCoordinate()][(int)d.getCurrentXCoordinate()] = 1;
                                            gridTracker[(int)d.getCurrentYCoordinate()+1][(int)d.getCurrentXCoordinate()] = 1;
                                            for(int row = 0; row < gridTracker.length; row++)
                                                {
                                                     for(int column = 0; column < gridTracker[row].length;column++)
                                                     {
                                                         System.out.print(gridTracker[row][column]);
                                                     }
                                                     System.out.println("");
                                                }
                                            d.setOldXCoordinate(d.getCurrentXCoordinate());
                                            d.setOldYCoordinate(d.getCurrentYCoordinate());
                                            d.setCurrentXCoordinate(d.getCurrentXCoordinate());
                                            d.setCurrentYCoordinate(d.getCurrentYCoordinate()+1);
                                            setRobotPosition(d.getCurrentXCoordinate(), d.getCurrentYCoordinate());
                                            if(d.getOldYCoordinate() + 1.0 == d.getCurrentYCoordinate())
                                            {
                                                d.setDroidStatus(false);
                                                gridTracker[(int) d.getCurrentYCoordinate()-1][(int) d.getCurrentXCoordinate()] = 0;
                                                System.out.println("...................GRID UPDATED........................");
                                                for(int row = 0; row < gridTracker.length; row++)
                                                {
                                                     for(int column = 0; column < gridTracker[row].length;column++)
                                                     {
                                                         System.out.print(gridTracker[row][column]);
                                                     }
                                                     System.out.println("");
                                                }
                                            }
                                            System.out.println(".....................Move 4.............");
                                            System.out.println("Current X Coordinate = " + d.getCurrentXCoordinate());
                                            System.out.println("Current Y Coordinate = " + d.getCurrentYCoordinate());
                                            System.out.println("Old X Coordinate = " + d.getOldXCoordinate());
                                            System.out.println("Old Y Coordinate = " + d.getOldYCoordinate());
                                            System.out.println("Droid Status: " + d.getDroidStatus());
                                            moveCompleted = true;
                                        }
                                        break;
                                }
                                if(moveCompleted)
                                {
                                    moveCompleted = false;
                                    break;
                                }
                            }
                            if(gridTracker[0][0] == 0 || gridTracker[4][0] == 0 || gridTracker[0][4] == 0 || gridTracker[4][4] == 0 )
                            {
                                mutex.wait();
                            }
                   }
                   catch(ArrayIndexOutOfBoundsException e)
                   {}
                   catch(InterruptedException c){}
                }   
        }
    }   
    public void ScheduleDroidMove()
    {
        ScheduledExecutorService service = Executors.newScheduledThreadPool(6);
        for(Droid d : droidList)
        {
            MoveDroid md = new MoveDroid(d);
            service.schedule(md, d.getDelay(), TimeUnit.MILLISECONDS);
        }
    }
    
    public void ScheduleSpawn(){
        ScheduledExecutorService service = Executors.newScheduledThreadPool(4);
        service.scheduleAtFixedRate(new SpawnDroid() , 1000, 2000, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Moves a robot image to a new grid position. This is highly rudimentary, as you will need
     * many different robots in practice. This method currently just serves as a demonstration.
     */
    public void setRobotPosition(double x, double y)
    {
        robotX = 2;
        robotY = 2;
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
                }
            });
        }
        listeners.add(newListener);
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
    
    /** 
     * Draws a (slightly clipped) line between two grid coordinates.
     *     
     * You shouldn't need to modify this method.
     */
    private void drawLine(GraphicsContext gfx, double gridX1, double gridY1, 
                                               double gridX2, double gridY2)
    {
        gfx.setStroke(Color.RED);
        
        // Recalculate the starting coordinate to be one unit closer to the destination, so that it
        // doesn't overlap with any image appearing in the starting grid cell.
        final double radius = 0.5;
        double angle = Math.atan2(gridY2 - gridY1, gridX2 - gridX1);
        double clippedGridX1 = gridX1 + Math.cos(angle) * radius;
        double clippedGridY1 = gridY1 + Math.sin(angle) * radius;
        
        gfx.strokeLine((clippedGridX1 + 0.5) * gridSquareSize, 
                       (clippedGridY1 + 0.5) * gridSquareSize, 
                       (gridX2 + 0.5) * gridSquareSize, 
                       (gridY2 + 0.5) * gridSquareSize);
    }
}
