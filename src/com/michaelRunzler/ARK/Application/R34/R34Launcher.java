import javafx.application.Application;
import javafx.stage.Stage;

public class R34Launcher extends Application
{
    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        new R34UI().launch();
    }
}
