import java.util.*;
import javafx.scene.canvas.GraphicsContext;
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
    private JFXArena arena;

    public Droid(int id, JFXArena arena)
    {
        this.id = id;
        this.delay = ((int) (Math.random() * (2000 - 500))) + 500;
        droidMoving = false;
        isAlive = true;
        this.arena = arena;
        Thread t = new Thread(new MoveDroid(this, this.arena), String.valueOf(this.id));
        t.start();
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
    
    private class MoveDroid implements Runnable
    {
        private GraphicsContext gfx;
        private List<Integer> randomNumbersList = new ArrayList();
        private boolean moveCompleted = false;
        private long currentTimePassed = System.currentTimeMillis();
        private Droid d;
        private GridUpdater updater;
        public MoveDroid(Droid d, JFXArena arena)
        {
            this.d = d;
            gfx = arena.getCanvas().getGraphicsContext2D();
            updater = new GridUpdater(arena);
        }
        
        @Override
        public void run(){
           randomNumbersList.add(1);
           randomNumbersList.add(2);
           randomNumbersList.add(3);
           randomNumbersList.add(4);
           
            while(d.getIsAlive() == true && arena.getGridTracker()[2][2] != 1)
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
                             if(arena.getGridTracker()[2][2] == 1)
                             {
                                 synchronized(arena.getGameOverMutex())
                                 {
                                    arena.getGameOverMutex().notify();
                                 }
                                 Thread.currentThread().interrupt();
                             }
                             moveCompleted = false;
                             break;
                         }
                     }

                     if(arena.getGridTracker()[0][0] == 0 || arena.getGridTracker()[4][0] == 0 || arena.getGridTracker()[0][4] == 0 || arena.getGridTracker()[4][4] == 0 )
                     {
                         synchronized(arena.getMutex())
                         {
                            arena.getMutex().wait();
                         }

                     }
                     Thread.sleep(d.getDelay());
            }
            catch(InterruptedException c){
                Thread.currentThread().interrupt();
            }
         }
         if(d.getIsAlive() == false)
         {
             Thread.currentThread().interrupt();
         }
       }
        
        
       private void MoveUp()
       {
           try
           {
                if(d.getCurrentYCoordinate() - 1.0 >= 0.0 && 
                arena.getGridTracker()[(int) d.getCurrentYCoordinate()-1][(int) d.getCurrentXCoordinate()] == 0 &&
                !d.getDroidStatus())
                { 
                    updater.updateGridY(d,d.getCurrentYCoordinate()-1);
                    for(int i = 0; i < 10; i++)
                    {
                        d.setCurrentYCoordinate(d.getCurrentYCoordinate()-0.1);
                        arena.refreshLayout();
                        Thread.sleep(50);
                    }
                    d.setDroidStatus(true);
                    d.setCurrentYCoordinate(Math.rint(d.getCurrentYCoordinate()));
                    d.setCurrentXCoordinate(d.getCurrentXCoordinate());
                    arena.refreshLayout();
                    d.setDroidStatus(false);
                    updater.updateOldYGridCoordinate(d, Math.rint(d.getCurrentYCoordinate())+1);
                    moveCompleted = true;
                }
           }
           catch(InterruptedException e)
           { 
               Thread.currentThread().interrupt(); 
           }
       }
       
       private void MoveLeft()
       {
           try
           {
                if(d.getCurrentXCoordinate() - 1.0 >= 0.0 && 
                arena.getGridTracker()[(int) d.getCurrentYCoordinate()][(int) d.getCurrentXCoordinate()-1] == 0 
                && !d.getDroidStatus())
                {
                    updater.updateGridX(d, d.getCurrentXCoordinate()-1);
                    for(int i = 0; i < 10; i++)
                    {
                        d.setCurrentXCoordinate(d.getCurrentXCoordinate()-0.1);
                        arena.refreshLayout();
                        Thread.sleep(50);
                    }
                    d.setDroidStatus(true);
                    d.setCurrentXCoordinate(Math.rint(d.getCurrentXCoordinate()));
                    d.setCurrentYCoordinate(d.getCurrentYCoordinate());
                    arena.refreshLayout();
                    d.setDroidStatus(false);
                    updater.updateOldXGridCoordinate(d, Math.rint(d.getCurrentXCoordinate())+1);
                    moveCompleted = true;
                }
           }
           catch(InterruptedException e)
           {
               Thread.currentThread().interrupt();
           }
       }
       
       private void MoveRight()
       {
           try
           {
                if(d.getCurrentXCoordinate() + 1.0 <= 4.0 && 
                arena.getGridTracker()[(int) d.getCurrentYCoordinate()][(int) d.getCurrentXCoordinate()+1] == 0 && !d.getDroidStatus())
                {
                   updater.updateGridX(d, d.getCurrentXCoordinate()+1);
                   for(int i = 0; i < 10; i++)
                    {
                        d.setCurrentXCoordinate(d.getCurrentXCoordinate()+0.1);
                        arena.refreshLayout();
                        Thread.sleep(50);
                    }
                    d.setDroidStatus(true);
                    d.setCurrentXCoordinate(Math.rint(d.getCurrentXCoordinate()));
                    d.setCurrentYCoordinate(d.getCurrentYCoordinate());
                    arena.refreshLayout();
                    d.setDroidStatus(false);
                    updater.updateOldXGridCoordinate(d,Math.rint(d.getCurrentXCoordinate())-1);
                    moveCompleted = true;
                }
           }
           catch(InterruptedException e)
           {
               Thread.currentThread().interrupt();
           }
       }
       
       private void MoveDown()
       {
           try
           {
                if(d.getCurrentYCoordinate() + 1.0 <= 4.0 && 
                arena.getGridTracker()[(int) d.getCurrentYCoordinate()+1][(int) d.getCurrentXCoordinate()] == 0
                        && !d.getDroidStatus())
                {
                   d.setDroidStatus(true);
                   updater.updateGridY(d,d.getCurrentYCoordinate()+1);
                   for(int i = 0; i < 10; i++)
                   {
                       d.setCurrentYCoordinate(d.getCurrentYCoordinate()+0.1);
                       arena.refreshLayout();
                       Thread.sleep(50);
                   }
                   d.setCurrentXCoordinate(d.getCurrentXCoordinate());
                   d.setCurrentYCoordinate(Math.rint(d.getCurrentYCoordinate()));
                   arena.refreshLayout();
                   d.setDroidStatus(false);
                   updater.updateOldYGridCoordinate(d, Math.rint(d.getCurrentYCoordinate())-1);
                   moveCompleted = true;

                }
           }
           catch(InterruptedException e)
           {
               Thread.currentThread().interrupt();
           }
       }
       
    }
}