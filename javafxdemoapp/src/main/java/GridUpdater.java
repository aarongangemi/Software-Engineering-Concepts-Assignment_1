/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author 61459
 */
public class GridUpdater {
    private Object gridUpdateLock;
    private JFXArena arena;
    public GridUpdater(JFXArena arena)
    {
        this.arena = arena;
        gridUpdateLock = new Object();
    }
    
    public void updateGridX(Droid d, double updateXValue)
    {
        try
        {
            synchronized(gridUpdateLock)
            {
                if(arena.getGridTracker()[(int) d.getCurrentYCoordinate()][(int)updateXValue] == 1)
                {
                    gridUpdateLock.wait();
                }
                arena.getGridTracker()[(int) d.getCurrentYCoordinate()][(int) d.getCurrentXCoordinate()] = 1;
                arena.getGridTracker()[(int) d.getCurrentYCoordinate()][(int) updateXValue] = 1;
                gridUpdateLock.notify();
            }
        }
        catch(InterruptedException e)
        {}
    }
    
    public void updateGridY(Droid d, double updateYValue)
    {
        try
        {
            synchronized(gridUpdateLock)
            {
                if(arena.getGridTracker()[(int) updateYValue][(int) d.getCurrentXCoordinate()] == 1)
                {
                    gridUpdateLock.wait();
                }
                arena.getGridTracker()[(int) d.getCurrentYCoordinate()][(int) d.getCurrentXCoordinate()] = 1;
                arena.getGridTracker()[(int) updateYValue][(int) d.getCurrentXCoordinate()] = 1;
                gridUpdateLock.notify();
            }
        }
        catch(InterruptedException e)
        { 
        }
    }
    
    public void updateOldYGridCoordinate(Droid d, double updateYValue)
    {
        try
        {
            synchronized(gridUpdateLock)
            {
                if(arena.getGridTracker()[(int) updateYValue][(int) d.getCurrentXCoordinate()] == 0)
                {
                    gridUpdateLock.wait();
                }
                arena.getGridTracker()[(int) updateYValue][(int) d.getCurrentXCoordinate()] = 0;
                gridUpdateLock.notify();
            }
        }
        catch(InterruptedException e)
        { 
        }
    }
    
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
                gridUpdateLock.notify();
            }
        }
        catch(InterruptedException e)
        { }
    }
}
