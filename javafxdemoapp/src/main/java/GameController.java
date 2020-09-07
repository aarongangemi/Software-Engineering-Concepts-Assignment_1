
import javafx.application.Platform;
import javafx.scene.control.Alert;

/***********************************
 * Purpose: The game controller is used to control all inputs and outputs 
 * that the program may experience
 * @author 61459
 */
public class GameController 
{
    private int score;
    private Object scoreMutex;
    private JFXArena arena;
    private boolean isFiring;
    /*********************************
     * Purpose: The game controller is used to set values that will be required
     * for the inputs and outputs. Once initialized it fires off a new thread
     * which constantly checks for game over and updates the score
     * @param arena 
     */
    public GameController(JFXArena arena)
    {
        isFiring = false; // droid is not currently firing
        score = 0; // start score is 0
        scoreMutex = new Object(); // used to control score race conditions
        this.arena = arena; // use jfxarena
        Thread gameOverThread = new Thread(new GameOver(), "GameOver");
        gameOverThread.start();
        Thread scoreThread = new Thread(new ScoreUpdater(),"ScoreThread");
        scoreThread.start();
        // fire new threads for score and game over checking
    }
    /*************************************
     * Purpose: Used to check if the game has finished if a droid reaches the
     * fortress. If a droid reaches the fortress, the GUI thread is accessed
     * to display an alert
     * Author: Aaron Gangemi
     */
    private class GameOver implements Runnable{
        @Override
        public void run()
        {
            while(true)
            {
                // run for life of program
                synchronized(arena.getGameOverMutex())
                {
                    // if the fortress has not been reached, let other thread 
                    //use game over mutex
                    if(arena.getGridTracker()[2][2]==0)
                    {
                        try
                        {
                           arena.getGameOverMutex().wait(); 
                        }
                        catch(InterruptedException e){}
                    }
                    arena.getSpawnDroidService().shutdown();
                    // when mutex gets released, game is finished and scheduled
                    // droids are removed from the game and list
                    arena.getDroidList().clear();
                    // refresh the GUI to be empty
                    arena.refreshLayout();
                    Platform.runLater(new Runnable(){
                    @Override
                    public void run()
                    {
                        // access GUI thread and alert user that game is over
                        Alert a = new Alert(Alert.AlertType.INFORMATION);
                        a.setTitle("Game Over: A droid reached coordinates "
                                + "(2,2)");
                        a.setContentText("Final Score: " + 
                                arena.getScoreLabel().getText());
                        a.show();
                        // display alert information
                    }});
                    // set middle of grid to occupied so all droids stop moving
                    break;
                    // break will end the while loop and thread
                }
            }
        }
    }
    
    /********************************
     * Purpose: Used to create a new firing command when the user clicks on a
     * grid square. This is a wrapper method
     * @param gridX
     * @param gridY
     * @param initialTime
     * @return new Firing Command
     */
    public FiringCommand createNewCommand(int gridX, int gridY, long initialTime)
    {
        return new FiringCommand(gridX, gridY, initialTime);
    }
    /*******************************
     * Purpose: Create a new firing command in a new thread
     * Author: Aaron Gangemi
     */    
    private class FiringCommand implements Runnable{
        private int gridX;
        private int gridY;
        private long initialTime;
        /*************************************
         * Purpose: To create a new firing command on the given coordinates
         * @param gridX
         * @param gridY
         * @param initialTime 
         */
        public FiringCommand(int gridX, int gridY, long initialTime)
        {
            this.gridX = gridX;
            this.gridY = gridY;
            this.initialTime = initialTime;
            // initial time is time that command was fired
        }
        /**************************************
         * Purpose: To issue a new firing command in a new thread
         */
        @Override
        public void run() 
        {
            for(Droid d : arena.getDroidList())
            {
                if(d.getCurrentXCoordinate() == gridX &&
                        d.getCurrentYCoordinate() == gridY 
                        && d.getDroidStatus() == false)
                {
                    // find the droid that is in the current coordinates
                    try
                    {
                        Thread.sleep(1000);
                        // wait 1 second
                        isFiring = true;
                        // set droid to firing
                        long t = System.currentTimeMillis() - initialTime;
                        // get the t bonus score
                        synchronized(scoreMutex)
                        {
                            // update the scorer in a thread safe environment
                            // so it doesn't interfere with 10 second addition
                            score += (10 + 100*t/d.getDelay());
                            // update score
                            // release mutex to allow 10 second add on to
                            // continue
                            scoreMutex.notify();
                        }
                        // Access GUI thread to update score and logger
                        Platform.runLater(new Runnable(){
                            @Override
                            public void run()
                            {
                                // update log and score
                                arena.getLogger().appendText("Shot fired: Droid at coordinates:"
                                        + " (" + gridY + "," + gridX + ") was destroyed\n");  
                                arena.getScoreLabel().setText(String.valueOf(score));
                            }
                        });
                        // remove droid from list if clicked on
                        arena.getDroidList().remove(d);
                        // droid is not alive to end thread
                        d.setIsAlive(false);
                        // update grid space to empty
                        arena.getGridTracker()[gridY][gridX] = 0;
                        // ensure grid space is set to empty
                        arena.getGridTracker()[(int)d.getCurrentYCoordinate()][(int)d.getCurrentXCoordinate()] = 0;
                        // set is firing to false
                        isFiring = false;
                        //refresh GUI again
                        arena.refreshLayout();
                    }
                    catch(InterruptedException e)
                    {}
                }
            }
        }
        
    }
    /*******************************************
     * Purpose: Update the score every 10 seconds
     */
    private class ScoreUpdater implements Runnable{
        
        @Override
        public void run() {
            // run while game over has not been reached
            while(arena.getGridTracker()[2][2] != 1)
            {
                try
                {
                    synchronized(scoreMutex)
                    {
                        // wait one second
                        Thread.sleep(1000);
                        if(isFiring)
                        {
                            // if the user is firing at a droid then 
                            // do not increment the score
                            scoreMutex.wait();
                        }
                        // if user is not firing then update score
                        score += 10;
                        Platform.runLater(new Runnable(){
                            @Override
                            public void run()
                            {
                                // update the score
                                arena.getScoreLabel().setText(String.valueOf(score));
                            }
                        });
                        // finished with score incase user fires
                        scoreMutex.notify();
                    }

                }
                catch(InterruptedException e){}
            }
        }
    }
        
        
}
