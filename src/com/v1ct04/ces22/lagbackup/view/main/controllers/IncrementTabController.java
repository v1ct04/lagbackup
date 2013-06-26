package com.v1ct04.ces22.lagbackup.view.main.controllers;

import com.v1ct04.ces22.lagbackup.backup.model.Backup;
import com.v1ct04.ces22.lagbackup.backup.model.BackupDiff;
import com.v1ct04.ces22.lagbackup.backup.tasks.IncrementBackupTask;
import com.v1ct04.ces22.lagbackup.concurrent.FXThreadTaskListener;
import com.v1ct04.ces22.lagbackup.view.custom.FXDialog;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressUpdate;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;

import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class IncrementTabController implements Initializable, SubController<MainWindowController> {

    private MainWindowController mMainWindowController;

    @FXML private Button mIncrementButton;
    @FXML private ListView<Path> mBackupIncrementListView;

    private ObjectProperty<Backup> mBackup = new SimpleObjectProperty<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        MultipleSelectionModel<Path> selectionModel = mBackupIncrementListView.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);
        selectionModel.getSelectedItems().addListener(new ListChangeListener<Path>() {
            @Override
            public void onChanged(Change<? extends Path> change) {
                mIncrementButton.setDisable(change.getList().isEmpty());
            }
        });
        mIncrementButton.setDisable(true);

        mBackup.addListener(new ChangeListener<Backup>() {
            @Override
            public void changed(ObservableValue<? extends Backup> observableValue,
                                Backup oldBackup,
                                Backup backup) {
                if (backup != null)
                    mBackupIncrementListView.getItems().setAll(backup.getBackedUpFolders());
                else
                    mBackupIncrementListView.getItems().clear();
            }
        });
    }

    public void selectIncrementAllFolders() {
        mBackupIncrementListView.getSelectionModel().selectAll();
    }

    public void incrementBackup() {
        IncrementBackupTask task = new IncrementBackupTask(mBackup.get(),
            mBackupIncrementListView.getSelectionModel().getSelectedItems());
        FXDialog.showProgressDialog(mMainWindowController.getStage(), "Incrementando backup...", task);
        task.addTaskCompletionListener(new BackupIncrementTaskListener());
        task.start();
    }

    @Override
    public void setParentController(MainWindowController parent) {
        mMainWindowController = parent;
        mBackup.bind(mMainWindowController.backupProperty());
    }

    private class BackupIncrementTaskListener
        extends FXThreadTaskListener<BackupDiff, ProgressUpdate> {
        @Override
        public void onFXThreadSuccess(BackupDiff diff) {
            String message = (diff == null) ?
                "Nenhuma alteração detectada desde o último backup incremental." :
                "Backup incremental realizado com sucesso.";
            FXDialog.showMessageDialog(mMainWindowController.getStage(), "Backup incremental", message);
        }

        @Override
        public void onFXThreadFailure(Throwable throwable) {
            if (!(throwable instanceof InterruptedException)) {
                FXDialog.showErrorDialog(mMainWindowController.getStage(), "Erro ao incrementar backup", throwable);
                throwable.printStackTrace();
            }
        }
    }
}