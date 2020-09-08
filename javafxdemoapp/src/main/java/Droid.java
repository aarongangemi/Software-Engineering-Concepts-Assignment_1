import java.util.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
/***********************************
 * Purpose: The droid class stores all information about a given droid. 
 * Each droid will run in its own thread and will run a new move thread when 
 * a droid is created. Stores accessors and mutators for class fields
 * @author Aaron Gangemi
 * Date Modified: 07/09/2020
 */
public class Droid
{
    private int id;
    private int delay;
    private double currentXCoordinate;
    private double currentYCoordinate;
    private boolean droidMoving;
    private boolean isAlive;
    private JFXArena arena;

    /********************************************
     * Purpose: Constructor for the droid. 
     * Creates a new thread for the droid to move in and sets required fields
     * @param id
     * @param arena 
     */
    public Droid(int id, JFXArena arena)
    {
        this.id = id;
        this.delay = ((int) (Math.random() * (2000 - 500))) + 500;
        droidMoving = false;
        isAlive = true;
        this.arena = arena; // use arena to access mutex 
                            // elements and shared resources
        Thread t = new Thread(new MoveDroid(this, this.arena),
                                String.valueOf(this.id));
        //create a new thread for the robot to move in
        t.start();
    }
    
    /************************************
     * Purpose: get the droids delay
     * @return delay
     */
    public int getDelay()
    {
        return delay;
    }
    
    /***********************************
     * Purpose: get the droids id
     * @return id
     */
    public int getId()
    {
        return id;
    }
    
    /************************************
     * Purpose: set the current XCoordinate of the droid
     * @param XCoordinate 
     */
    public void setCurrentXCoordinate(double XCoordinate)
    {
        currentXCoordinate = XCoordinate;
    }
    
    /************************************
     * Purpose: set the current YCoordinate of the droid
     * @param YCoordinate 
     */
    public void setCurrentYCoordinate(double YCoordinate)
    {
        currentYCoordinate = YCoordinate;
    }
    
    /***********************************
     * Purpose: get the current X coordinate of the droid
     * @return currentXCoordinate
     */
    public double getCurrentXCoordinate()
    {
        return currentXCoordinate;
    }
    
    /***********************************
     * Purpose: get the current Y coordinate of the droid
     * @return currentXCoordinate
     */
    public double getCurrentYCoordinate()
    {
        return currentYCoordinate;
    }
    
    /*******************************
     * Purpose: Get the droid status (if the droid is moving or not)
     * @return droidMoving
     */
    public boolean getDroidStatus()
    {
        return droidMoving;
    }
    
    /********************************
     * Purpose: Change the droid moving status
     * @param droidMoving 
     */
    public void setDroidStatus(boolean droidMoving)
    {
        this.droidMoving = droidMoving;
    }
    
    /************************************
     * Purpose: return the boolean variable indicating if the droid is alive
     * @return isAlive
     */
    public boolean getIsAlive()
    {
        return isAlive;
    }
    
    /***********************************
     * Purpose: Set the alive state of the droid
     * @param isAlive 
     */
    public void setIsAlive(boolean isAlive)
    {
        this.isAlive = isAlive;
    }
    
    /***********************************************
     * Purpose: This class is run in a different thread for each droid.
     * Used to move the droid from one coordinate to another
     * Author: Aaron Gangemi
     * Date Modified: 07/09/2020
     */
    private class MoveDroid implements Runnable
    {
        private GraphicsContext gfx;
        private List<Integer> randomNumbersList = new ArrayList();
        private boolean moveCompleted = false;
        private long currentTimePassed = System.currentTimeMillis();
        private Droid d;
        private GridUpdater updater;
        /**************************************
         * Purpose: The constructor used to move the droid. Requires a droid to
         * move and the arena to access any shared resources required such as
         * mutexes and the grid
         * @param d
         * @param arena 
         */
        public MoveDroid(Droid d, JFXArena arena)
        {
            this.d = d;
            // set canvas using arena
            gfx = arena.getCanvas().getGraphicsContext2D();
            // get the grid updater to update the grid in a thread safe 
            // environment
            updater = new GridUpdater(arena);
        }
        
        /************************************
         * Purpose: The run method is used to move the droid in a different 
         * thread. Used to implement the runnable interface. The method moves
         * the droid based on a random number generated
         */
        @Override
        public void run()
        {
           // add 4 numbers to list - each representing a direction
           randomNumbersList.add(1); // indicates left
           randomNumbersList.add(2); // indicates up
           randomNumbersList.add(3); // indicates down
           randomNumbersList.add(4); // indicates right
           
            while(d.getIsAlive() == true && arena.getGridTracker()[2][2] != 1)
            {
                // run the loop if the droid has been created and the middle
                // square has not been reached by any droid
                try
                {
                   Collections.shuffle(randomNumbersList);
                   // shuffle the random numbers list on each move
                   for(int j = 0; j < randomNumbersList.size(); j++)
                   {
                       // loop through the random numbers list and move based
                       // on the direction of the number
                        int randomNumber = randomNumbersList.get(j);
                        switch(randomNumber)
                        {
                            case 2: //Move Up
                                MoveUp();
                                break;
                            case 1: //Move Left
                                MoveLeft();
                                break;
                            case 3: //Move Right
                                MoveRight();
                                break;
                            case 4: //Move Down
                                MoveDown();
                                break;
                        }
                        if(moveCompleted)
                        {
                            // check if the droid has completed there move
                            if(arena.getGridTracker()[2][2] == 1)
                            {
                                // check for game over
                                synchronized(arena.getGameOverMutex())
                                {
                                    // if game over, then notify game over 
                                    // thread checker
                                   arena.getGameOverMutex().notify();
                                }
                                Thread.currentThread().interrupt();
                                // interrupt each droid when they have
                                // completed there move
                            }
                            moveCompleted = false;
                            // if not gameover, then reset move to not 
                            // completed for next round
                            break;
                        }
                    }

                    // check if any grid corners are empty
                    if(arena.getGridTracker()[0][0] == 0 || 
                            arena.getGridTracker()[4][0] == 0 || 
                            arena.getGridTracker()[0][4] == 0 || 
                            arena.getGridTracker()[4][4] == 0)
                    {
                        synchronized(arena.getMutex())
                        {
                           // wait mutex to allow droids to spawn in corners
                           arena.getMutex().wait();
                        }
                    }
                    // wait for droid delay
                    Thread.sleep(d.getDelay());
                }
                catch(InterruptedException c){
                    // if anything goes wrong while sleeping or with the mutex,
                    // then cancel the current droid out
                    Thread.currentThread().interrupt();
                }
            }
            // once the droid has been clicked, then remove there thread
            // which will remove the droid from the screen
            if(d.getIsAlive() == false)
            {
                Thread.currentThread().interrupt();
            }
        }
        
       /******************************************
        * Purpose: Used to move the droid in the upwards direction
        */ 
       private void MoveUp()
       {
           try
           {
               // check if te coordinate is still within the grid, check if 
               //the next grid space is free and the droid is not currently
               // moving
                if(d.getCurrentYCoordinate() - 1.0 >= 0.0 && 
                arena.getGridTracker()[(int) d.getCurrentYCoordinate()-1][(int) d.getCurrentXCoordinate()] == 0 &&
                !d.getDroidStatus())
                { 
                    // update the current Y coordinate
                    updater.updateGridY(d,d.getCurrentYCoordinate()-1);
                    for(int i = 0; i < 10; i++)
                    {
                        // Make the droid move across the span of 500ms
                        d.setCurrentYCoordinate(d.getCurrentYCoordinate()-0.1);
                        arena.refreshLayout();
                        Thread.sleep(50);
                        // refresh GUI and sleep for 50ms
                    }
                    d.setDroidStatus(true);
                    // droid is moving and set coordinates
                    d.setCurrentYCoordinate(Math.rint(d.getCurrentYCoordinate()));
                    d.setCurrentXCoordinate(d.getCurrentXCoordinate());
                    arena.refreshLayout();
                    // refresh GUI
                    d.setDroidStatus(false);
                    // after moved then droid is not moving anymore
                    updater.updateOldYGridCoordinate(d,
                                    Math.rint(d.getCurrentYCoordinate())+1);
                    // set old grid coordinates to false
                    moveCompleted = true;
                    // set the move to completed
                }
           }
           catch(InterruptedException e)
           { 
               Thread.currentThread().interrupt(); 
               // if anything goes wrong while sleeping, remove the current
               // droid from the screen
           }
       }
       /***********************************
        * Purpose: Used to move the current droid left
        */
       private void MoveLeft()
       {
           try
           {
               // check if the new coordinate is on the grid 
               // and not occupied and the current droid is not moving
                if(d.getCurrentXCoordinate() - 1.0 >= 0.0 && 
                arena.getGridTracker()[(int) d.getCurrentYCoordinate()]
                        [(int) d.getCurrentXCoordinate()-1] == 0 && !d.getDroidStatus())
                {
                    // update the grid tracker to ensure old and new squares
                    // are occupied
                    updater.updateGridX(d, d.getCurrentXCoordinate()-1);
                    // move droid in 500ms
                    for(int i = 0; i < 10; i++)
                    {
                        // move current XCoordinate
                        d.setCurrentXCoordinate(d.getCurrentXCoordinate()-0.1);
                        // refresh GUI
                        arena.refreshLayout();
                        Thread.sleep(50);
                    }
                    // set droid to moving
                    d.setDroidStatus(true);
                    // set new Coordinates
                    // use Math.rint to remove rounding errors
                    d.setCurrentXCoordinate(Math.rint(d.getCurrentXCoordinate()));
                    d.setCurrentYCoordinate(d.getCurrentYCoordinate());
                    // refresh GUI with new coordinates
                    arena.refreshLayout();
                    // droid is not moving anymore
                    d.setDroidStatus(false);
                    //set old grid space to empty
                    updater.updateOldXGridCoordinate(d, 
                            Math.rint(d.getCurrentXCoordinate())+1);
                    // droid has completed the move
                    moveCompleted = true;
                }
           }
           catch(InterruptedException e)
           {
               // if anything goes wrong then remove the droid
               Thread.currentThread().interrupt();
           }
       }
       
       /*****************************************8
        * Purpose: Move the droid right
        */
       private void MoveRight()
       {
           try
           {
               // check if grid space to move to is on grid and new grid space
               // is empty and droid is not moving
                if(d.getCurrentXCoordinate() + 1.0 <= 4.0 && 
                arena.getGridTracker()[(int) d.getCurrentYCoordinate()][(int) d.getCurrentXCoordinate()+1] == 0 
                        && !d.getDroidStatus())
                {
                   // update old and new space to occupied 
                   updater.updateGridX(d, d.getCurrentXCoordinate()+1);
                    for(int i = 0; i < 10; i++)
                    {
                        // used to move the droid in 500ms
                        d.setCurrentXCoordinate(d.getCurrentXCoordinate()+0.1);
                        // change coordinates and refresh GUI constantly
                        arena.refreshLayout();
                        // 500ms
                        Thread.sleep(50);
                    }
                   // set droid to moving
                    d.setDroidStatus(true);
                    // set droids new coordinates
                    // use math.rint to remove rounding errors
                    d.setCurrentXCoordinate(Math.rint(d.getCurrentXCoordinate()));
                    d.setCurrentYCoordinate(d.getCurrentYCoordinate());
                    //refresh GUI layout
                    arena.refreshLayout();
                    // set droid to not moving
                    d.setDroidStatus(false);
                    // update old grid space to empty
                    updater.updateOldXGridCoordinate(d,Math.rint(d.getCurrentXCoordinate())-1);
                    // droid has completed move
                    moveCompleted = true;
                }
           }
           catch(InterruptedException e)
           {
               // if anything goes wrong then remove the droid
               Thread.currentThread().interrupt();
           }
       }
       
       /**************************************
        * Purpose: To move the droid downwards on the grid
        */
       private void MoveDown()
       {
           try
           {
               // check if new grid space is on grid and is empty and droid is
               // not moving
                if(d.getCurrentYCoordinate() + 1.0 <= 4.0 && 
                arena.getGridTracker()[(int) d.getCurrentYCoordinate()+1][(int) d.getCurrentXCoordinate()] == 0
                        && !d.getDroidStatus())
                {
                    // set droid to moving
                   d.setDroidStatus(true);
                   updater.updateGridY(d,d.getCurrentYCoordinate()+1);
                   // update old and new grid space to occupied
                   for(int i = 0; i < 10; i++)
                   {
                       // set coordinates to move droid in 500ms
                       d.setCurrentYCoordinate(d.getCurrentYCoordinate()+0.1);
                       arena.refreshLayout();
                       Thread.sleep(50);
                   }
                   // set droid coordinates and remove rounding errors
                   d.setCurrentXCoordinate(d.getCurrentXCoordinate());
                   d.setCurrentYCoordinate(Math.rint(d.getCurrentYCoordinate()));
                   // refresh GUI layout
                   arena.refreshLayout();
                   // set droid to not moving
                   d.setDroidStatus(false);
                   // update old grid space to empty
                   updater.updateOldYGridCoordinate(d, Math.rint(d.getCurrentYCoordinate())-1);
                   // droid has completed move
                   moveCompleted = true;
                }
           }
           catch(InterruptedException e)
           {
               // if anything goes wrong then remove droid thread
               Thread.currentThread().interrupt();
           }
       }
    }
}