package com.v1ct04.ces22.lagbackup.view.main.creation;

import com.v1ct04.ces22.lagbackup.backup.model.Backup;
import com.v1ct04.ces22.lagbackup.backup.tasks.CompleteBackupTask;
import com.v1ct04.ces22.lagbackup.concurrent.FXThreadTaskListener;
import com.v1ct04.ces22.lagbackup.view.custom.FXDialog;
import com.v1ct04.ces22.lagbackup.view.custom.FileChooseElement;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.AnchorPane;
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
        stage.setTitle("Novo backup completo");
        stage.setMinWidth(300);
        stage.setMinHeight(300);
        FXMLLoader loader = new FXMLLoader(NewBackupWindow.class.getResource(
            "new_backup_window.fxml"));
        Parent root = (Parent) loader.load();
        stage.setScene(new Scene(root));

        NewBackupWindow backupWindow = loader.getController();
        backupWindow.setStage(stage);
        stage.showAndWait();
        return backupWindow.mBackup;
    }

    @FXML private AnchorPane mFileChooserHolder;

    @FXML private ListView<Path> mBackupFoldersListView;
    private ObservableList<Path> mBackupFoldersList;

    private FileChooseElement<String> mFileChooseElement;

    @FXML private Button mCreateBackupButton;

    private LinkedHashSet<Path> mBackupFolders = new LinkedHashSet<>();

    private Stage mStage;
    private Backup mBackup;

    private FileChooser mBackupFileChooser;
    private DirectoryChooser mDirectoryChooser;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mBackupFileChooser = new FileChooser();
        mBackupFileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Arquivos Lag Backup", "*.lkp"));
        mBackupFileChooser.titleProperty().setValue("Selecionar onde salvar backup");

        mDirectoryChooser = new DirectoryChooser();
        mDirectoryChooser.setTitle("Adicionar pasta");
        mDirectoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        mBackupFoldersListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        mBackupFoldersList = mBackupFoldersListView.getItems();
        mBackupFoldersList.addListener(new ListChangeListener<Path>() {
            @Override
            public void onChanged(Change<? extends Path> change) {
                refreshDisabledViews();
            }
        });

        mFileChooseElement = new FileChooseElement<>(mBackupFileChooser);
        mFileChooseElement.setKeyObject("Localização:");
        mFileChooseElement.chosenFileProperty().isNull().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue,
                                Boolean aBoolean,
                                Boolean aBoolean2) {
                refreshDisabledViews();
            }
        });

        mFileChooserHolder.getChildren().add(mFileChooseElement);
        AnchorPane.setLeftAnchor(mFileChooseElement, 0.0);
        AnchorPane.setRightAnchor(mFileChooseElement, 0.0);

        refreshDisabledViews();
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
        Path backupFile = mFileChooseElement.getChosenFile();
        if (backupFile == null)
            return;
        if (!backupFile.toString().endsWith(".lkp")) {
            backupFile = Paths.get(backupFile.toString().concat(".lkp"));
            mFileChooseElement.setChosenFile(backupFile);
        }
        CompleteBackupTask task = new CompleteBackupTask(backupFile, mBackupFolders);
        FXDialog.showProgressDialog(mStage, "Criando backup...", task);
        task.addTaskCompletionListener(new BackupCreationTaskListener());
        task.start();
    }

    private void refreshDisabledViews() {
        boolean dis =  mBackupFoldersList.isEmpty() || mFileChooseElement.getChosenFile() == null;
        mCreateBackupButton.setDisable(dis);
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
