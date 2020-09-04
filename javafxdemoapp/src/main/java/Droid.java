import java.util.*;
import javafx.scene.image.Image;
public class Droid
{
    private int id;
    private int delay;
    private double currentXCoordinate;
    private double currentYCoordinate;
    private double newXCoordinate;
    private double newYCoordinate;
    private boolean droidMoving;
    private boolean isAlive;

    public Droid(int id)
    {
        this.id = id;
        this.delay = ((int) (Math.random() * (2000 - 500))) + 500;
        droidMoving = false;
        isAlive = true;
    }
    
    public int getDelay()
    {
        return delay;
    }
    
    public int getId()
    {
        return id;
    }
    
    public void setCurrentXCoordinate(double XCoordinate)
    {
        currentXCoordinate = XCoordinate;
    }
    
    public void setCurrentYCoordinate(double YCoordinate)
    {
        currentYCoordinate = YCoordinate;
    }
    
    public void setDroidId(int id)
    {
        this.id = id;
    }
    
    public double getCurrentXCoordinate()
    {
        return currentXCoordinate;
    }
    
    public double getCurrentYCoordinate()
    {
        return currentYCoordinate;
    }
    
    public double getOldXCoordinate()
    {
        return newXCoordinate;
    }
    
    public double getOldYCoordinate()
    {
        return newYCoordinate;
    }
    
    public void setOldYCoordinate(double YCoordinate)
    {
        this.newYCoordinate = YCoordinate;
    }
    
    public void setOldXCoordinate(double XCoordinate)
    {
        this.newXCoordinate = XCoordinate;
    }
    
    public boolean getDroidStatus()
    {
        return droidMoving;
    }
    
    public void setDroidStatus(boolean droidMoving)
    {
        this.droidMoving = droidMoving;
    }
    
    public boolean getIsAlive()
    {
        return isAlive;
    }
    
    public void setIsAlive(boolean isAlive)
    {
        this.isAlive = isAlive;
    }
}