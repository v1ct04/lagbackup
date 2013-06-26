package com.v1ct04.ces22.lagbackup.view.main.controllers;

import com.v1ct04.ces22.lagbackup.backup.model.Backup;
import com.v1ct04.ces22.lagbackup.backup.tasks.DeleteBackupTask;
import com.v1ct04.ces22.lagbackup.backup.tasks.LoadBackupTask;
import com.v1ct04.ces22.lagbackup.concurrent.FXThreadTaskListener;
import com.v1ct04.ces22.lagbackup.view.custom.FXDialog;
import com.v1ct04.ces22.lagbackup.view.main.creation.NewBackupWindow;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressUpdate;
import com.v1ct04.ces22.lagbackup.view.main.BackupApplication;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageBuilder;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainWindowController implements Initializable {

    private IncrementTabController mIncrementTabController;
    private RestoreTabController mRestoreTabController;
    private VisualizeTabController mVisualizeTabController;

    @FXML private VBox mLoadingBackupIndicator;
    @FXML private TabPane mTabPane;

    @FXML private MenuItem mDeleteBackupItem;
    @FXML private MenuItem mCloseBackupItem;

    private Stage mStage;
    private FileChooser mBackupFileOpener;

    private ObjectProperty<Backup> mBackup = new SimpleObjectProperty<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mBackupFileOpener = new FileChooser();
        mBackupFileOpener.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Arquivos Lag Backup", "*.lkp"));
        mBackupFileOpener.titleProperty().setValue("Abrir backup");

        mTabPane.disableProperty().bind(mBackup.isNull());
        mCloseBackupItem.disableProperty().bind(mBackup.isNull());
        mDeleteBackupItem.disableProperty().bind(mBackup.isNull());

        mBackup.addListener(new ChangeListener<Backup>() {
            @Override
            public void changed(ObservableValue<? extends Backup> observableValue,
                                Backup oldBackup,
                                Backup backup) {
                String title = "Lag Backup";
                if (backup != null) title += " - " + backup.getBackupFile().getFileName();
                if (mStage != null) mStage.setTitle(title);
            }
        });

        List<Tab> tabs = mTabPane.getTabs();
        FXMLLoader loader;
        loader = loadFXML("increment_tab.fxml");
        mIncrementTabController = loader.getController();
        mIncrementTabController.setParentController(this);
        tabs.get(0).setContent(loader.<Node>getRoot());
        loader = loadFXML("restore_tab.fxml");
        mRestoreTabController = loader.getController();
        mRestoreTabController.setParentController(this);
        tabs.get(1).setContent(loader.<Node>getRoot());
        loader = loadFXML("visualize_tab.fxml");
        mVisualizeTabController = loader.getController();
        mVisualizeTabController.setParentController(this);
        tabs.get(2).setContent(loader.<Node>getRoot());

        mBackup.set(null);
    }

    public void setStage(Stage stage) {
        mStage = stage;
    }

    // Menu items

    public void newBackup() throws IOException {
        Backup backup = NewBackupWindow.createNewBackup(mStage);
        if (backup != null)
            mBackup.set(backup);
    }

    public void openBackup() {
        File chosen = mBackupFileOpener.showOpenDialog(mStage);
        if (chosen != null) {
            mLoadingBackupIndicator.setVisible(true);
            mLoadingBackupIndicator.setOpacity(1);
            LoadBackupTask task = new LoadBackupTask(chosen.toPath());
            task.addTaskCompletionListener(new BackupOpenTaskListener());
            task.start();
        }
    }

    public void closeBackup() {
        mBackup.set(null);
    }

    public void deleteBackup() {
        boolean confirm = FXDialog.showConfirmDialog(mStage, "Excluir backup",
            "Tem certeza que deseja excluir esse backup? Essa operação não é cancelável.");
        if (!confirm)
            return;
        DeleteBackupTask task = new DeleteBackupTask(mBackup.get());
        FXDialog.showProgressDialog(mStage, "Excluindo backup...", task, false);
        task.addTaskCompletionListener(new BackupCloseTaskListener());
        task.start();
    }

    public void exitMenuClick() {
        Platform.exit();
    }

    public void aboutMenuClick() {
        final Stage stage = new Stage();
        StageBuilder.create()
            .title("Ajuda")
            .icons(mStage.getIcons())
            .scene(new Scene(VBoxBuilder.create()
                .children(
                    LabelBuilder.create()
                        .text("Lag Backup").font(Font.font("Arial", FontWeight.BOLD, 16))
                        .build(),
                    new Label("Produzido por Victor Elias"),
                    new Label("Contato: victorgelias@gmail.com"),
                    ButtonBuilder.create().text("Sair").onAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent actionEvent) {
                            stage.close();
                        }
                    }).build())
                .alignment(Pos.CENTER).spacing(10).padding(new Insets(5))
                .build()))
            .applyTo(stage);
        stage.show();
    }

    public void resetTabsContent() {
        if (mRestoreTabController != null)
            mRestoreTabController.reset();
        if (mVisualizeTabController != null)
            mVisualizeTabController.reset();
    }

    // utility methods

    ObservableObjectValue<Backup> backupProperty() {
        return mBackup;
    }

    Stage getStage() {
        return mStage;
    }

    private FXMLLoader loadFXML(String fileName) {
        FXMLLoader loader = new FXMLLoader(BackupApplication.class.getResource(fileName));
        try {
            loader.load();
        } catch (IOException e) {
            FXDialog.showErrorDialog(mStage, "Erro ao carregar telas do programa", e);
            e.printStackTrace();
        }
        return loader;
    }

    private class BackupOpenTaskListener extends FXThreadTaskListener<Backup, Void> {
        @Override
        public void onFXThreadSuccess(Backup backup) {
            mBackup.set(backup);
            FadeTransition transition =
                new FadeTransition(Duration.millis(300), mLoadingBackupIndicator);
            transition.setToValue(0);
            transition.setOnFinished(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    mLoadingBackupIndicator.setVisible(false);
                }
            });
            transition.play();
        }

        @Override
        public void onFXThreadFailure(Throwable throwable) {
            mLoadingBackupIndicator.setVisible(false);
            if (!(throwable instanceof InterruptedException)) {
                FXDialog.showErrorDialog(mStage, "Erro ao carregar backup", throwable);
                throwable.printStackTrace();
            }
        }
    }

    private class BackupCloseTaskListener extends FXThreadTaskListener<Void, ProgressUpdate> {
        @Override
        public void onFXThreadSuccess(Void o) {
            closeBackup();
        }

        @Override
        public void onFXThreadFailure(Throwable throwable) {
            FXDialog.showErrorDialog(mStage, "Erro ao excluir backup", throwable);
            FXDialog.showMessageDialog(mStage, "Erro ao excluir backup", "Houve um erro ao " +
                "tentar excluir o backup, apague o arquivo " + mBackup.get().getBackupFile() +
                " e a pasta " + mBackup.get().getParentBackupFolder() + " manualmente para abrir " +
                "espaço em disco.");
            throwable.printStackTrace();
        }
    }
}
