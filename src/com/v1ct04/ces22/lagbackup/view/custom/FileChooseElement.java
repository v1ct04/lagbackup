package com.v1ct04.ces22.lagbackup.view.custom;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileChooseElement<KeyType> extends HBox {

    private Label mItemNameView;
    private TextField mChosenFilePathView;
    private Button mFileChooserButton;

    private KeyType mKeyObject;
    private SimpleObjectProperty<Path> mChosenFile = new SimpleObjectProperty<>();

    public FileChooseElement(final DirectoryChooser directoryChooser) {
        init(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (mChosenFile != null)
                    directoryChooser.setInitialDirectory(mChosenFile.get().toFile());

                File chosen = directoryChooser.showDialog(getScene().getWindow());
                if (chosen != null) {
                    setChosenFile(chosen.toPath());
                }
            }
        });
    }

    public FileChooseElement(final FileChooser fileChooser) {
        init(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (mChosenFile.get() != null)
                    fileChooser.setInitialDirectory(mChosenFile.get().toFile().getParentFile());

                File chosen = fileChooser.showSaveDialog(getScene().getWindow());
                if (chosen != null)
                    setChosenFile(chosen.toPath());
            }
        });
    }

    private void init(EventHandler<ActionEvent> chooseAction) {
        mItemNameView = LabelBuilder.create()
            .maxWidth(Label.USE_COMPUTED_SIZE)
            .textOverrun(OverrunStyle.CENTER_ELLIPSIS)
            .build();
        mChosenFilePathView = TextFieldBuilder.create()
            .minWidth(150)
            .build();
        mChosenFilePathView.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue,
                                String oldFilename,
                                String filename) {
                if (!filename.isEmpty())
                    mChosenFile.set(Paths.get(filename));
                else
                    mChosenFile.set(null);
            }
        });
        mFileChooserButton = ButtonBuilder.create()
            .text("...")
            .onAction(chooseAction)
            .build();

        setSpacing(5);
        setAlignment(Pos.BASELINE_LEFT);
        getChildren().addAll(mItemNameView, mChosenFilePathView, mFileChooserButton);
        setHgrow(mItemNameView, Priority.ALWAYS);
        setHgrow(mChosenFilePathView, Priority.SOMETIMES);
        setMaxWidth(Double.MAX_VALUE);
    }

    public void setKeyObject(KeyType keyObject) {
        mKeyObject = keyObject;
        mItemNameView.setText(keyObject.toString());
        mItemNameView.setTooltip(new Tooltip(keyObject.toString()));
    }

    public KeyType getKeyObject() {
        return mKeyObject;
    }

    public void setChosenFile(Path chosenFile) {
        mChosenFile.set(chosenFile);
        if (chosenFile != null)
            mChosenFilePathView.setText(mChosenFile.get().toString());
        else
            mChosenFilePathView.setText("");
    }

    public Path getChosenFile() {
        return mChosenFile.get();
    }

    public ReadOnlyObjectProperty<Path> chosenFileProperty() {
        return mChosenFile;
    }
}
