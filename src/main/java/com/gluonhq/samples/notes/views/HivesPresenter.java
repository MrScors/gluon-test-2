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

import com.gluonhq.charm.glisten.afterburner.GluonView;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.FloatingActionButton;
import com.gluonhq.charm.glisten.layout.layer.SidePopupView;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.samples.notes.Main;
import com.gluonhq.samples.notes.model.Hive;
import com.gluonhq.samples.notes.model.Settings;
import com.gluonhq.samples.notes.service.Service;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ResourceBundle;

public class HivesPresenter {


    @FXML private View hives;

    @Inject private ModelHive modelHive;
    
    @FXML private CharmListView<Hive, LocalDate> lstHives;

    @FXML private ResourceBundle resources;

    @Inject private Service service;

    private FilteredList<Hive> filteredList;
    
    public void initialize() {
        Button filterButton = MaterialDesignIcon.FILTER_LIST.button(e ->
                AppManager.getInstance().showLayer(Main.POPUP_FILTER_HIVES));
        filterButton.getStyleClass().add("filter-button");

//        hives.setShowTransitionFactory(BounceInLeftTransition::new);
        hives.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                AppBar appBar = AppManager.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> 
                        AppManager.getInstance().getDrawer().open()));
                appBar.setTitleText(resources.getString("appbar.title"));
                appBar.getActionItems().add(filterButton);
            }
        });
        
        lstHives.setCellFactory(p -> new HiveCell(service, this::edit, this::remove, this::addInspection));
        lstHives.setHeadersFunction(t -> LocalDateTime.ofEpochSecond(t.getCreationDate(), 0, ZoneOffset.UTC).toLocalDate());
        lstHives.setHeaderCellFactory(p -> new HeaderHiveCell());
        lstHives.setPlaceholder(new Label("Nothing found"));

        service.hivesProperty().addListener((ListChangeListener.Change<? extends Hive> c) -> {
            ObservableList<Hive> hives = FXCollections.observableArrayList(new ArrayList<Hive>(c.getList()));
            filteredList = new FilteredList<>(hives);
            lstHives.setItems(filteredList);
            lstHives.setComparator(Comparator.comparing(Hive::getId));
        });
        
        final FloatingActionButton floatingActionButton = new FloatingActionButton();
        floatingActionButton.setOnAction(e -> edit(null));
        floatingActionButton.showOn(hives);
        
        AppManager.getInstance().addLayerFactory(Main.POPUP_FILTER_HIVES, () -> {
            GluonView filterView = new GluonView(FilterHivePresenter.class);
            FilterHivePresenter filterHivePresenter = (FilterHivePresenter) filterView.getPresenter();
            
            SidePopupView sidePopupView = new SidePopupView(filterView.getView(), Side.TOP, true);
            sidePopupView.showingProperty().addListener((obs, ov, nv) -> {
                if (ov && !nv) {
                    filteredList.setPredicate(filterHivePresenter.getPredicate());
                }
            });
            
            return sidePopupView; 
        });
        
        service.retrieveHives();
        
        service.settingsProperty().addListener((obs, ov, nv) -> updateSettings());
        
        updateSettings();
    }
    
    private void edit(Hive hive) {
        modelHive.getActiveHive().set(hive);
        AppViewManager.EDITHIVE_VIEW.switchView();
    }
    
    private void remove(Hive hive) {
        service.removeHive(hive);
    }
    private void addInspection(Hive hive) {
        modelHive.getActiveHive().set(hive);
//        AppViewManager.EDIT_INSPECTION_VIEW.switchView();
    }

    private void updateSettings() {
        Settings settings = service.settingsProperty().get();
        if (settings.isAscending()) {
            lstHives.setHeaderComparator(Comparator.naturalOrder());
        } else {
            lstHives.setHeaderComparator(Comparator.reverseOrder());
        }
        
        switch (settings.getSorting()) {
            case DATE:  
                if (settings.isAscending()) {
                    lstHives.setComparator(Comparator.comparing(Hive::getCreationDate));
                } else {
                    lstHives.setComparator((n1, n2) -> Long.compare(n2.getCreationDate(), n1.getCreationDate()));
                }
                break;
            case TITLE: 
                if (settings.isAscending()) {
                    lstHives.setComparator(Comparator.comparing(Hive::getId));
                } else {
                    lstHives.setComparator((n1, n2) -> Integer.compare(n2.getId(), n1.getId()));
                }
                break;
            case CONTENT:
                if (settings.isAscending()) {
                    lstHives.setComparator(Comparator.comparing(Hive::getId));
                } else {
                    lstHives.setComparator((n1, n2) -> Integer.compare(n2.getId(), n1.getId()));
                }
                break;
        }
    }
}
