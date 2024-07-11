/*
 * Copyright (c) 2016, 2021, Gluon
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of Gluon, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.samples.notes.views;

import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.samples.notes.model.Hive;
import com.gluonhq.samples.notes.service.Service;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static com.gluonhq.samples.notes.Utils.formatList;

public class EditHivePresenter {

    @Inject private Service service;

    @Inject private ModelHive modelHive;

    @FXML private View edition;

    @FXML private Button submit;
    @FXML private Button cancel;
    @FXML private TextField boxes;
    @FXML private CheckBox aliveFlag;

    @FXML private ResourceBundle resources;

    private boolean editMode;

    public void initialize() {
//        edition.setShowTransitionFactory(BounceInRightTransition::new);

        edition.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                submit.disableProperty().unbind();
                
                Hive activeHive = modelHive.getActiveHive().get();
                if (activeHive != null) {
                    submit.setText(resources.getString("button.submit.text"));
                    boxes.setText(formatList(activeHive.getOccupiedBoxes(), " "));
                    aliveFlag.setSelected(activeHive.isAlive());

                    // Enable submit button if user input differs from activeHive
                    submit.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                        boolean boxesChanged = !boxes.getText().equals(formatList(activeHive.getOccupiedBoxes(), " "));
                        boolean aliveFlagChanged = aliveFlag.isSelected() != activeHive.isAlive();

                        return !boxesChanged && !aliveFlagChanged;
                    }, boxes.textProperty(), aliveFlag.selectedProperty()));

                    editMode = true;
                } else {
                    aliveFlag.setSelected(true);
                    submit.setText(resources.getString("button.submit.text"));

                    // Enable submit button only if boxes is not empty
                    submit.disableProperty().bind(Bindings.createBooleanBinding(() ->
                            boxes.getText().isEmpty(), boxes.textProperty()));

                    editMode = false;
                }
                AppBar appBar = AppManager.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> AppManager.getInstance().getDrawer().open()));
                appBar.setTitleText(resources.getString(editMode ? "appbar.title.edit" : "appbar.title.add"));
            } else {
                boxes.clear();
            }
        });
        
        submit.setOnAction(e -> {
            Hive hive = editMode ? modelHive.getActiveHive().get() : new Hive();
            if(!aliveFlag.isSelected()){
                hive.setOccupiedBoxes(new ArrayList());
                hive.setAlive(false);
            } else {
                hive.setOccupiedBoxes(Arrays.stream(boxes.getText().split(" ")).map(Integer::valueOf).collect(Collectors.toList()));
                hive.setAlive(true);
            }

            if (editMode) {
                List<Hive> hivesWithSameId = service.hivesProperty().stream().filter(el -> el.getId() == hive.getId()).collect(Collectors.toList());
                service.hivesProperty().removeAll(hivesWithSameId);
            }
            service.addHive(hive);
            close();
        });
        cancel.setOnAction(e -> close());
    }

    private void close() {
        boxes.clear();
        modelHive.getActiveHive().set(null);
        AppViewManager.HIVES_VIEW.switchView();
    }
    
}
