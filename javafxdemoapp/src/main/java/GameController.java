
import javafx.application.Platform;
import javafx.scene.control.Alert;

public class GameController 
{
    private int score;
    private Object scoreMutex;
    private JFXArena arena;
    private boolean isFiring;
    public GameController(JFXArena arena)
    {
        isFiring = false;
        score = 0;
        scoreMutex = new Object();
        this.arena = arena;
        Thread gameOverThread = new Thread(new GameOver(), "GameOver");
        gameOverThread.start();
        Thread scoreThread = new Thread(new ScoreUpdater(),"ScoreThread");
        scoreThread.start();
    }
        private class GameOver implements Runnable{
        @Override
        public void run()
        {
            while(true)
            {
                synchronized(arena.getGameOverMutex())
                {
                    if(arena.getGridTracker()[2][2]==0)
                    {
                        try
                        {
                           arena.getGameOverMutex().wait(); 
                        }
                        catch(InterruptedException e){}
                    }
                    arena.getSpawnDroidService().shutdown();
                    arena.getDroidList().clear();
                    arena.refreshLayout();
                    Platform.runLater(new Runnable(){
                    @Override
                    public void run()
                    {
                        Alert a = new Alert(Alert.AlertType.INFORMATION);
                        a.setTitle("Game Over: A droid reached coordinates (2,2)");
                        a.setContentText("Final Score: " + arena.getScoreLabel().getText());
                        a.show();
                    }});
                    arena.getGridTracker()[2][2] = 1;
                    break;
                }
            }
        }
    }
        
    public FiringCommand createNewCommand(int gridX, int gridY, long initialTime)
    {
        return new FiringCommand(gridX, gridY, initialTime);
    }
        
    private class FiringCommand implements Runnable{
        private int gridX;
        private int gridY;
        private long initialTime;
        public FiringCommand(int gridX, int gridY, long initialTime)
        {
            this.gridX = gridX;
            this.gridY = gridY;
            this.initialTime = initialTime;
        }
        
        @Override
        public void run() 
        {
            for(Droid d : arena.getDroidList())
            {
                if(d.getCurrentXCoordinate() == gridX && d.getCurrentYCoordinate() == gridY && d.getDroidStatus() == false)
                {
                    try
                    {
                        Thread.sleep(1000);
                        isFiring = true;
                        long t = System.currentTimeMillis() - initialTime;
                        synchronized(scoreMutex)
                        {
                            System.out.println("Score updated");
                            score += (10 + 100*t/d.getDelay());
                            scoreMutex.notify();
                        }
                        Platform.runLater(new Runnable(){
                            @Override
                            public void run()
                            {
                                arena.getLogger().appendText("Shot fired: Droid at coordinates: (" + gridY + "," + gridX + ") was destroyed\n");  
                                arena.getScoreLabel().setText(String.valueOf(score));
                            }
                        });
                        arena.getDroidList().remove(d);
                        d.setIsAlive(false);
                        arena.getGridTracker()[gridY][gridX] = 0;
                        arena.getGridTracker()[(int)d.getCurrentYCoordinate()][(int)d.getCurrentXCoordinate()] = 0;
                        isFiring = false;
                        arena.refreshLayout();
                    }
                    catch(InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        
    }
    
    private class ScoreUpdater implements Runnable{
        
        @Override
        public void run() {
            while(arena.getGridTracker()[2][2] != 1)
            {
                try
                {
                    synchronized(scoreMutex)
                    {
                        Thread.sleep(1000);
                        if(isFiring)
                        {
                            scoreMutex.wait();
                        }
                        score += 10;
                        Platform.runLater(new Runnable(){
                            @Override
                            public void run()
                            {
                                arena.getScoreLabel().setText(String.valueOf(score));
                            }
                        });
                        scoreMutex.notify();
                    }

                }
                catch(InterruptedException e){}
            }
        }
    }
        
        
}
