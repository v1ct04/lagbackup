package com.v1ct04.ces22.lagbackup.view.main;

import com.v1ct04.ces22.lagbackup.view.main.controllers.MainWindowController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class BackupApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main_window.fxml"));
        Parent root = (Parent) loader.load();
        loader.<MainWindowController>getController().setStage(primaryStage);
        primaryStage.setMinWidth(450);
        primaryStage.setMinHeight(350);
        primaryStage.setTitle("Lag Backup");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("app_icon.png")));
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
