package com.v1ct04.ces22.lagbackup.view.main.creation;

import com.v1ct04.ces22.lagbackup.backup.model.Backup;
import com.v1ct04.ces22.lagbackup.backup.tasks.CompleteBackupTask;
import com.v1ct04.ces22.lagbackup.concurrent.FXThreadTaskListener;
import com.v1ct04.ces22.lagbackup.view.custom.FXDialog;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.stage.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.ResourceBundle;

public class NewBackupWindow implements Initializable {

    public static Backup createNewBackup(Window window) throws IOException {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(window);
        if (window instanceof Stage)
            stage.getIcons().setAll(((Stage) window).getIcons());
        stage.setTitle("Novo backup");
        FXMLLoader loader = new FXMLLoader(NewBackupWindow.class.getResource(
            "new_backup_window.fxml"));
        Parent root = (Parent) loader.load();
        stage.setScene(new Scene(root));

        NewBackupWindow backupWindow = loader.getController();
        backupWindow.setStage(stage);
        stage.showAndWait();
        return backupWindow.mBackup;
    }

    @FXML private ListView<Path> mBackupFoldersListView;
    private ObservableList<Path> mBackupFoldersList;

    private LinkedHashSet<Path> mBackupFolders = new LinkedHashSet<>();

    private Stage mStage;
    private Backup mBackup;

    private FileChooser mBackupFileOpener;
    private DirectoryChooser mDirectoryChooser;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mBackupFileOpener = new FileChooser();
        mBackupFileOpener.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Arquivos Lag Backup", "*.lkp"));
        mBackupFileOpener.titleProperty().setValue("Selecionar onde salvar backup");

        mDirectoryChooser = new DirectoryChooser();
        mDirectoryChooser.setTitle("Adicionar pasta");
        mDirectoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        mBackupFoldersListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        mBackupFoldersList = mBackupFoldersListView.getItems();
    }

    private void setStage(Stage stage) {
        mStage = stage;
    }

    public void removeSelectedFolders() {
        Collection<Path> selected = mBackupFoldersListView.getSelectionModel().getSelectedItems();
        mBackupFolders.removeAll(selected);
        mBackupFoldersList.setAll(mBackupFolders);
    }

    public void addNewFolder() {
        File chosen = mDirectoryChooser.showDialog(mStage);
        if (chosen != null) {
            mDirectoryChooser.setInitialDirectory(chosen.getParentFile());
            mBackupFolders.add(chosen.toPath());
            mBackupFoldersList.setAll(mBackupFolders);
        }
    }

    public void cancelCreation() {
        mStage.close();
    }

    public void createBackup() {
        File backupFile = mBackupFileOpener.showSaveDialog(mStage);
        if (backupFile == null)
            return;
        Path filePath;
        if (!backupFile.toString().endsWith(".lkp"))
            filePath = Paths.get(backupFile.toString().concat(".lkp"));
        else
            filePath = backupFile.toPath();
        CompleteBackupTask task = new CompleteBackupTask(filePath, mBackupFolders);
        FXDialog.showProgressDialog(mStage, "Criando backup...", task);
        task.addTaskCompletionListener(new BackupCreationTaskListener());
        task.start();
    }

    private class BackupCreationTaskListener extends FXThreadTaskListener<Backup, Void> {
        @Override
        public void onFXThreadSuccess(Backup backup) {
            mBackup = backup;
            mStage.close();
        }

        @Override
        public void onFXThreadFailure(Throwable throwable) {
            if (!(throwable instanceof InterruptedException)) {
                FXDialog.showErrorDialog(mStage, "Erro ao criar backup", throwable);
                throwable.printStackTrace();
            }
        }
    }
}
