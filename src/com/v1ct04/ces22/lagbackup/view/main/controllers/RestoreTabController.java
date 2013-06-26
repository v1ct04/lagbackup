package com.v1ct04.ces22.lagbackup.view.main.controllers;

import com.v1ct04.ces22.lagbackup.backup.model.Backup;
import com.v1ct04.ces22.lagbackup.backup.model.BackupDiff;
import com.v1ct04.ces22.lagbackup.backup.tasks.RestoreBackupTask;
import com.v1ct04.ces22.lagbackup.concurrent.FXThreadTaskListener;
import com.v1ct04.ces22.lagbackup.view.custom.FXDialog;
import com.v1ct04.ces22.lagbackup.view.custom.FileChooseElement;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressUpdate;
import com.v1ct04.ces22.lagbackup.view.custom.ScenePagerController;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class RestoreTabController implements Initializable, SubController<MainWindowController> {

    private MainWindowController mMainWindowController;

    @FXML private HBox mRestoreHBox;
    @FXML private AnchorPane mRestorePagerParent;

    @FXML private Button mChooseBackupButton;
    @FXML private ListView<BackupDiff> mRestoreDiffListView;
    @FXML private ListView<Path> mRestoreFoldersListView;
    @FXML private VBox mRestoreFolderDestinationContainer;

    private ScenePagerController mRestoreScenePager;
    private DirectoryChooser mDirectoryChooser;

    private ObjectProperty<Backup> mBackup = new SimpleObjectProperty<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mDirectoryChooser = new DirectoryChooser();
        mDirectoryChooser.setTitle("Escolher pasta");

        MultipleSelectionModel<Path> selectionModel = mRestoreFoldersListView.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);
        selectionModel.getSelectedItems().addListener(new ListChangeListener<Path>() {
            @Override
            public void onChanged(Change<? extends Path> change) {
                mChooseBackupButton.setDisable(change.getList().isEmpty());
            }
        });

        mRestoreScenePager = new ScenePagerController(mRestoreHBox, mRestorePagerParent);

        mBackup.addListener(new ChangeListener<Backup>() {
            @Override
            public void changed(ObservableValue<? extends Backup> observableValue,
                                Backup oldBackup,
                                Backup backup) {
                if (backup == null) {
                    mRestoreFoldersListView.getItems().clear();
                    mRestoreScenePager.animateToPage(0);
                    return;
                }
                mRestoreFoldersListView.getItems().setAll(backup.getBackedUpFolders());
                mRestoreFoldersListView.requestFocus();
                mChooseBackupButton.setDisable(true);
            }
        });
    }

    public void nextRestorePage() {
        mRestoreScenePager.animateNextPage();
        switch (mRestoreScenePager.getCurrentPage()) {
            case 0:
                mRestoreFoldersListView.requestFocus();
                break;
            case 1:
                List<Path> selectedItems =
                    mRestoreFoldersListView.getSelectionModel().getSelectedItems();
                List<BackupDiff> diffsModifying = mBackup.get().getDiffsModifying(selectedItems);
                mRestoreDiffListView.getItems().setAll(diffsModifying);
                mRestoreDiffListView.getSelectionModel().selectFirst();
                mRestoreDiffListView.requestFocus();
                break;
            case 2:
                List<Node> children = mRestoreFolderDestinationContainer.getChildren();
                children.clear();
                for (Path path : mRestoreFoldersListView.getSelectionModel().getSelectedItems()) {
                    FileChooseElement<Path> chooser = new FileChooseElement<>(mDirectoryChooser);
                    chooser.setKeyObject(path);
                    chooser.setChosenFile(path);
                    children.add(chooser);
                }
                break;
        }
    }

    public void previousRestorePage() {
        mRestoreScenePager.animatePreviousPage();
    }

    public void selectAllFolders() {
        mRestoreFoldersListView.getSelectionModel().selectAll();
        mRestoreFoldersListView.requestFocus();
    }

    public void restoreBackup() throws IOException {
        boolean askConfirmation = false;
        Map<Path, Path> backupToDestination = new HashMap<Path, Path>();
        for (Node node : mRestoreFolderDestinationContainer.getChildren()) {
            if (node instanceof FileChooseElement) {
                Path key = (Path) ((FileChooseElement)node).getKeyObject();
                Path chosen = ((FileChooseElement) node).getChosenFile();
                backupToDestination.put(key, chosen);
                if (Files.exists(chosen) && Files.isDirectory(chosen) &&
                    Files.newDirectoryStream(chosen).iterator().hasNext()) {
                    askConfirmation = true;
                }
            }
        }
        if (askConfirmation) {
            boolean confirm = FXDialog.showConfirmDialog(mMainWindowController.getStage(),
                "Restaurar backup",
                "Algumas das pastas escolhidas não estão vazias, tem certeza que deseja " +
                    "continuar?\n\n" +
                    "Arquivos serão sobrescritos sem confirmações adicionais.");
            if (!confirm)
                return;
        }
        RestoreBackupTask task = new RestoreBackupTask(
            mRestoreDiffListView.getSelectionModel().getSelectedItem(), backupToDestination);
        FXDialog.showProgressDialog(mMainWindowController.getStage(), "Restaurando backup...",task);
        task.addTaskCompletionListener(new BackupRestoreTaskListener());
        task.start();
    }

    public void reset() {
        if (mRestoreScenePager != null)
            mRestoreScenePager.setCurrentPage(0);
    }

    @Override
    public void setParentController(MainWindowController parent) {
        mMainWindowController = parent;
        mBackup.bind(mMainWindowController.backupProperty());
    }

    private class BackupRestoreTaskListener extends FXThreadTaskListener<Void, ProgressUpdate> {
        @Override
        public void onFXThreadFailure(Throwable throwable) {
            if (!(throwable instanceof InterruptedException)) {
                FXDialog.showErrorDialog(mMainWindowController.getStage(),
                    "Erro ao restaurar backup", throwable);
                throwable.printStackTrace();
            } else {
                mRestoreScenePager.animateToPage(0);
                mRestoreFoldersListView.getSelectionModel().clearSelection();
            }
        }
        @Override
        public void onFXThreadSuccess(Void o) {
            mRestoreScenePager.animateToPage(0);
            mRestoreFoldersListView.getSelectionModel().clearSelection();
            FXDialog.showMessageDialog(mMainWindowController.getStage(), "Restauração",
                "Restauração de backup realizada com sucesso.");
        }
    }
}