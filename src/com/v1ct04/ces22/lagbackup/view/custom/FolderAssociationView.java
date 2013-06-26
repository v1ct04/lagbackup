package com.v1ct04.ces22.lagbackup.view.custom;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FolderAssociationView extends HBox {

    private Label mItemNameView;
    private TextField mChosenFilePathView;
    private Button mFileChooserButton;

    private DirectoryChooser mDirectoryChooser;

    private Path mKeyFolder;
    private Path mChosenFile;

    public FolderAssociationView(DirectoryChooser directoryChooser) {
        mDirectoryChooser = directoryChooser;

        mItemNameView = LabelBuilder.create()
            .maxWidth(Label.USE_COMPUTED_SIZE)
            .textOverrun(OverrunStyle.CENTER_ELLIPSIS)
            .build();
        mChosenFilePathView = TextFieldBuilder.create()
            .minWidth(200)
            .build();
        mFileChooserButton = ButtonBuilder.create()
            .text("...")
            .onAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    if (mChosenFile != null)
                        mDirectoryChooser.setInitialDirectory(mChosenFile.toFile());

                    File chosen = mDirectoryChooser.showDialog(getScene().getWindow());
                    if (chosen != null) {
                        setChosenFile(chosen.toPath());
                    }
                }
            }).build();

        setSpacing(5);
        setAlignment(Pos.BASELINE_LEFT);
        getChildren().addAll(mItemNameView, mChosenFilePathView, mFileChooserButton);
        setHgrow(mItemNameView, Priority.ALWAYS);
        setHgrow(mChosenFilePathView, Priority.SOMETIMES);
    }

    public void setKeyFolder(Path keyFolder) {
        mKeyFolder = keyFolder;
        mItemNameView.setText(keyFolder.toString());
        mItemNameView.setTooltip(new Tooltip(keyFolder.toString()));
    }

    public void setChosenFile(Path chosenFile) {
        mChosenFile = chosenFile;
        mChosenFilePathView.setText(mChosenFile.toString());
    }

    public Path getKeyFolder() {
        return mKeyFolder;
    }

    public Path getChosenFolder() {
        if (mChosenFile == null || !mChosenFile.toString().equals(mChosenFilePathView.getText()))
            mChosenFile = Paths.get(mChosenFilePathView.getText());
        return mChosenFile;
    }
}
