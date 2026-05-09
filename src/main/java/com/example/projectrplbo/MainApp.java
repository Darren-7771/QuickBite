package com.example.projectrplbo;

import com.example.projectrplbo.db.DatabaseConnector;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        DatabaseConnector.getInstance();

        loadScene("login-view.fxml", "QuickBite - Login", 900, 600);
    }

    public static void loadScene(String fxmlFile, String title, double width, double height) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxmlFile));
        Scene scene = new Scene(loader.load(), width, height);
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(width);
        primaryStage.setMinHeight(height);
        primaryStage.show();
    }

    public static <T> T loadSceneWithController(String fxmlFile, String title, double width, double height) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxmlFile));
        Scene scene = new Scene(loader.load(), width, height);
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(width);
        primaryStage.setMinHeight(height);
        primaryStage.show();
        return loader.getController();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void stop() {
        DatabaseConnector.getInstance().closeConnection();
    }
}
