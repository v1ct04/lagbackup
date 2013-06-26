package com.v1ct04.ces22.lagbackup.view.custom;

import com.v1ct04.ces22.lagbackup.concurrent.ProgressUpdate;
import com.v1ct04.ces22.lagbackup.concurrent.AsyncTask;
import com.v1ct04.ces22.lagbackup.concurrent.FXThreadTaskListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.VBoxBuilder;
import javafx.stage.*;

public class FXDialog {

    public static void showErrorDialog(Window window, String title, Throwable throwable) {
        showMessageDialog(window, title,
            throwable.getClass().getName() + "\n" + throwable.getMessage());
    }

    public static void showMessageDialog(Window window, String title, String message) {
        final Stage dialogStage = createDefaultStage(window, title);
        dialogStage.setScene(new Scene(VBoxBuilder.create()
            .children(
                LabelBuilder.create()
                    .text(message)
                    .maxWidth(400)
                    .wrapText(true)
                    .build(),
                ButtonBuilder.create()
                    .text("OK")
                    .onAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent actionEvent) {
                            dialogStage.close();
                        }
                    })
                    .build())
            .spacing(10).alignment(Pos.CENTER).padding(new Insets(5)).build()));
        dialogStage.show();
    }

    public static boolean showConfirmDialog(Window window, String title, String message) {
        final boolean[] result = {false};
        final Stage dialogStage = createDefaultStage(window, title);
        setNotCloseable(dialogStage);
        dialogStage.setScene(new Scene(VBoxBuilder.create()
            .children(
                LabelBuilder.create()
                    .text(message)
                    .maxWidth(400)
                    .wrapText(true)
                    .build(),
                HBoxBuilder.create()
                    .children(
                        ButtonBuilder.create()
                            .text("Sim")
                            .onAction(new EventHandler<ActionEvent>() {
                                @Override
                                public void handle(ActionEvent actionEvent) {
                                    result[0] = true;
                                    dialogStage.close();
                                }
                            })
                            .build(),
                        ButtonBuilder.create()
                            .text("Não")
                            .onAction(new EventHandler<ActionEvent>() {
                                @Override
                                public void handle(ActionEvent actionEvent) {
                                    result[0] = false;
                                    dialogStage.close();
                                }
                            })
                            .build())
                    .spacing(10).alignment(Pos.CENTER).build())
            .spacing(10).padding(new Insets(5)).build()));
        dialogStage.showAndWait();
        return result[0];
    }


    public static <Type> void showProgressDialog(Window window,
                                                 String title,
                                                 AsyncTask<Type, ProgressUpdate> task) {
        showProgressDialog(window, title, task, true);
    }

    public static <Type> void showProgressDialog(final Window window,
                                                 String title,
                                                 final AsyncTask<Type, ProgressUpdate> task,
                                                 boolean cancelable) {
        final Stage dialogStage = createDefaultStage(window, title);
        setNotCloseable(dialogStage);

        final Label messageLabel = new Label("...");
        final ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        Button cancelButton = ButtonBuilder.create()
            .text("Cancelar")
            .onAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    if (showConfirmDialog(window, "Cancelar", "Tem certeza que deseja cancelar?")) {
                        messageLabel.setText("Cancelando...");
                        ((Button) actionEvent.getSource()).setDisable(true);
                        task.cancel();
                    }
                }
            }).visible(cancelable).build();
        dialogStage.setScene(new Scene(VBoxBuilder.create()
            .children(messageLabel,
                HBoxBuilder.create()
                    .children(progressBar, cancelButton)
                    .spacing(10).alignment(Pos.CENTER).build())
            .spacing(5).padding(new Insets(5)).build()));

        FXThreadTaskListener<Type, ProgressUpdate> listener =
            new FXThreadTaskListener<Type, ProgressUpdate>() {
                @Override
                public void onFinish() {
                    dialogStage.close();
                }
                @Override
                public void onFXThreadProgressUpdate(ProgressUpdate value) {
                    messageLabel.setText(value.getMessage());
                    progressBar.setProgress(value.getCompletionRatio());
                }
            };
        task.addProgressListener(listener);
        task.addTaskCompletionListener(listener);
        dialogStage.show();
    }

    private static Stage createDefaultStage(Window window, String title) {
        Stage stage = StageBuilder.create().title(title).resizable(false).build();

        if (window != null) {
            stage.initOwner(window);
            if (window instanceof Stage)
                stage.getIcons().setAll(((Stage) window).getIcons());
            stage.initModality(Modality.WINDOW_MODAL);
        } else
            stage.initModality(Modality.APPLICATION_MODAL);
        return stage;
    }

    private static void setNotCloseable(Stage stage) {
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                windowEvent.consume();
            }
        });
    }
}
