/*********************************************
 * Purpose: The grid updater is used to update the grid tracker 
 * so it is thread safe and avoids race conditions successfully
 * @author Aaron Gangemi
 */
public class GridUpdater {
    private Object gridUpdateLock;
    private JFXArena arena;
    /*********************************
     * Purpose: Instantiate an object that can be used to update the grid
     * @param arena 
     */
    public GridUpdater(JFXArena arena)
    {
        // set arena and grid locker
        this.arena = arena;
        gridUpdateLock = new Object();
    }
    /********************************
     * Purpose: Ensure that only one grid square can be updated at a time
     * @param d
     * @param updateXValue 
     */
    public void updateGridX(Droid d, double updateXValue)
    {
        try
        {
            synchronized(gridUpdateLock)
            {
                // use mutex lock to only allow one thread
                if(arena.getGridTracker()[(int) d.getCurrentYCoordinate()][(int)updateXValue] == 1)
                {
                    gridUpdateLock.wait();
                    // if current grid is being updated elsewhere then wait
                }
                // once lock has been recieved then update grid to occupied
                arena.getGridTracker()[(int) d.getCurrentYCoordinate()][(int) d.getCurrentXCoordinate()] = 1;
                arena.getGridTracker()[(int) d.getCurrentYCoordinate()][(int) updateXValue] = 1;
                // release grid locker
                gridUpdateLock.notify();
            }
        }
        catch(InterruptedException e)
        {}
    }
    /***********************************
     * Purpose: To update the grid Y value to a new value indicating it is
     * occupied
     * @param d
     * @param updateYValue 
     */
    public void updateGridY(Droid d, double updateYValue)
    {
        try
        {
            synchronized(gridUpdateLock)
            {
                // use mutex lock to only allow one thread
                if(arena.getGridTracker()[(int) updateYValue][(int) d.getCurrentXCoordinate()] == 1)
                {
                    gridUpdateLock.wait();
                    // if current grid is being updated elsewhere then wait
                }
                // once lock has been recieved then update grid to occupied
                arena.getGridTracker()[(int) d.getCurrentYCoordinate()][(int) d.getCurrentXCoordinate()] = 1;
                arena.getGridTracker()[(int) updateYValue][(int) d.getCurrentXCoordinate()] = 1;
                gridUpdateLock.notify();
                // release grid locker
            }
        }
        catch(InterruptedException e)
        { 
        }
    }
    /************************************
     * Purpose: To update the old coordinate to empty after a droid has moved
     * @param d
     * @param updateYValue 
     */
    public void updateOldYGridCoordinate(Droid d, double updateYValue)
    {
        try
        {
            synchronized(gridUpdateLock)
            {
                // only allow one thread to update the coordinate at a time
                if(arena.getGridTracker()[(int) updateYValue][(int) d.getCurrentXCoordinate()] == 0)
                {
                    // if grid space is occupied then wait 
                    gridUpdateLock.wait();
                }
                arena.getGridTracker()[(int) updateYValue][(int) d.getCurrentXCoordinate()] = 0;
                // update value
                // release lock
                gridUpdateLock.notify();
            }
        }
        catch(InterruptedException e)
        { 
        }
    }
    /************************************
     * Purpose: To update the old coordinate to empty after a droid has moved
     * @param d
     * @param updateYValue 
     */
    public void updateOldXGridCoordinate(Droid d, double updateXValue)
    {
        try
        {
            synchronized(gridUpdateLock)
            {
                if(arena.getGridTracker()[(int) d.getCurrentYCoordinate()][(int) updateXValue] == 0)
                {
                    gridUpdateLock.wait();
                }
                arena.getGridTracker()[(int) d.getCurrentYCoordinate()][(int) updateXValue] = 0;
                // update value
                // release lock
                gridUpdateLock.notify();
            }
        }
        catch(InterruptedException e)
        { }
    }
}
