package com.v1ct04.ces22.lagbackup.view.main.controllers;

import com.v1ct04.ces22.lagbackup.backup.model.Backup;
import com.v1ct04.ces22.lagbackup.backup.model.BackupDiff;
import com.v1ct04.ces22.lagbackup.backup.model.BackupDiffFolder;
import com.v1ct04.ces22.lagbackup.backup.model.BackupFile;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class VisualizeTabController implements Initializable, SubController<MainWindowController> {

    private MainWindowController mMainWindowController;

    @FXML private ListView<BackupDiff> mDiffVisualListView;
    @FXML private TableView<BackupFile> mBackupFileTable;
    @FXML private TableColumn<BackupFile, String> mModificationTypeColumn;
    @FXML private TableColumn<BackupFile, Path> mFilePathColumn;

    private ObservableList<BackupFile> mTableItems = FXCollections.observableArrayList();

    private ObjectProperty<Backup> mBackup = new SimpleObjectProperty<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mDiffVisualListView.getSelectionModel().getSelectedItems()
            .addListener(new DiffVisualSelectionListener());

        mBackupFileTable.setItems(mTableItems);
        mBackupFileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        mModificationTypeColumn.setCellValueFactory(
            new Callback<TableColumn.CellDataFeatures<BackupFile, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<BackupFile, String> p) {
                    final BackupFile file = p.getValue();
                    return new SimpleObjectProperty<String>(file.getModificationType().toDisplayString());
                }
            });
        mFilePathColumn.setCellValueFactory(
            new PropertyValueFactory<BackupFile, Path>("originalFile"));

        mBackup.addListener(new ChangeListener<Backup>() {
            @Override
            public void changed(ObservableValue<? extends Backup> observableValue,
                                Backup oldBackup,
                                Backup backup) {
                if (backup != null)
                    mDiffVisualListView.getItems().setAll(backup.getDiffList());
                else
                    mDiffVisualListView.getItems().clear();
            }
        });
    }

    public void reset() {
        if (mDiffVisualListView != null)
            mDiffVisualListView.getItems().setAll(mBackup.get().getDiffList());
    }

    @Override
    public void setParentController(MainWindowController parent) {
        mMainWindowController = parent;
        mBackup.bind(mMainWindowController.backupProperty());
    }

    private class DiffVisualSelectionListener implements ListChangeListener<BackupDiff> {

        @Override
        public void onChanged(Change<? extends BackupDiff> change) {
            BackupDiff diff = mDiffVisualListView.getSelectionModel().getSelectedItem();
            mTableItems.clear();
            if (diff == null)
                return;
            for (BackupDiffFolder folder : diff.getDiffFolders())
                mTableItems.addAll(folder.getFileModifications());
        }
    }
}