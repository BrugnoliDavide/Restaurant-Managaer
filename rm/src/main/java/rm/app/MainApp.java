package rm.app;

import rm.service.ConnectionManager;
import rm.view.LoginController;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;


public class MainApp extends Application {


    private static final Logger logger = Logger.getLogger(MainApp.class.getName());

    @Override
    public void start(Stage primaryStage) throws IOException {
        ConnectionManager.loadFromPreferences();
        boolean dbOk = ConnectionManager.testConnection();
        AppStatus.setDbConnectionOk(dbOk);

        Parent root = LoginController.getFXMLView();
        SceneManager.init(primaryStage);
        primaryStage.setScene(new Scene(root));

        primaryStage.setTitle("Restaurant Manager");
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(500);
        primaryStage.setResizable(true);
        setLogo(primaryStage);
        primaryStage.show();

        loadEnvFile();



    }


    public static void setLogo(Stage stage) {
        try {
            Image icon = new Image(MainApp.class.getResourceAsStream("/logo.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            logger.warning("Logo non trovato.");
        }
    }

    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$-7s] %5$s %n");
        launch();
    }

    private static void loadEnvFile() {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return;
        }
        try (var lines = Files.lines(envPath)) {
            lines.filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .forEach(line -> {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            System.setProperty(
                                    parts[0].trim(),
                                    parts[1].trim()
                            );
                        }
                    });
        } catch (IOException e) {
            // File .env non obbligatorio: ignora se non presente
        }
    }


}

