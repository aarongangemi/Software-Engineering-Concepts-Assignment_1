import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/****************************************
 * Purpose: Stores the main function to start the application
 * @author Aaron Gangemi
 * Date Modified: 07/09/2020
 */
public class App extends Application 
{
    /****************************************
    * Purpose: Launch the application
    * @author Aaron Gangemi
    * @param args
    */
    public static void main(String[] args) 
    {
        launch();        
    }
    
    /****************************************
    * Purpose: Create the GUI elements required for the program.
    *          Also makes a call to JFX arena
    * @author Aaron Gangemi
    * @param stage
    */
    @Override
    public void start(Stage stage) 
    {
        TextArea logger = new TextArea();
        Label label = new Label("Score: 0");
        stage.setTitle("Example App (JavaFX)");
        JFXArena arena = new JFXArena(logger, label);
        arena.addListener((x, y) ->
        {
            System.out.println("Arena click at (" + x + "," + y + ")");
        });
        
        ToolBar toolbar = new ToolBar();
//         Button btn1 = new Button("My Button 1");
//         Button btn2 = new Button("My Button 2");
//         toolbar.getItems().addAll(btn1, btn2, label);
        toolbar.getItems().addAll(label);
        
//         btn1.setOnAction((event) ->
//         {
//             System.out.println("Button 1 pressed");
//         });
                    
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(arena, logger);
        arena.setMinWidth(300.0);
        
        BorderPane contentPane = new BorderPane();
        contentPane.setTop(toolbar);
        contentPane.setCenter(splitPane);
        
        Scene scene = new Scene(contentPane, 800, 800);
        stage.setScene(scene);
        stage.show();
    }
}
