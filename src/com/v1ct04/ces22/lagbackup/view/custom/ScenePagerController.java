package com.v1ct04.ces22.lagbackup.view.custom;

import javafx.animation.TranslateTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

public class ScenePagerController {

    private final Pane mParent;
    private final HBox mHBox;

    private int mCurrentPage = 0;

    public ScenePagerController(HBox hBox, Pane parent) {
        mHBox = hBox;
        mParent = parent;
        init();
    }

    private void init() {
        mCurrentPage = 0;
        mHBox.setTranslateX(0);
        mHBox.setFillHeight(true);

        for (Node child : mHBox.getChildren()) {
            if (child instanceof Region)
                ((Region) child).prefWidthProperty().bind(mParent.widthProperty());
        }
        mParent.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue <? extends Number> observableValue,
                                Number number,
                                Number number2) {
                setCurrentPage(mCurrentPage);
            }
        });
        mHBox.getChildren().addListener(new ListChangeListener<Node>() {
            @Override
            public void onChanged(Change<? extends Node> change) {
                for (Node child : change.getAddedSubList()) {
                    if (child instanceof Region)
                        ((Region) child).prefWidthProperty().bind(mParent.widthProperty());
                }
            }
        });
    }

    public void animateToPage(int index) {
        mCurrentPage = index;
        TranslateTransition trans = new TranslateTransition(Duration.millis(500), mHBox);
        trans.setToX(-mParent.getWidth() * index);
        trans.play();
    }

    public void animateNextPage() {
        animateToPage((mCurrentPage + 1) % mHBox.getChildren().size());
    }

    public void animatePreviousPage() {
        int size = mHBox.getChildren().size();
        animateToPage((mCurrentPage - 1 + size) % size);
    }

    public void setCurrentPage(int index) {
        mCurrentPage = index;
        mHBox.setTranslateX(-mParent.getWidth() * index);
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }
}
