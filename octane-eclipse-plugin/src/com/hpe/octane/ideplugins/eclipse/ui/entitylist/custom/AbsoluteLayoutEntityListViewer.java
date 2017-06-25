/*******************************************************************************
 * Copyright 2017 Hewlett-Packard Enterprise Development Company, L.P.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.hpe.octane.ideplugins.eclipse.ui.entitylist.custom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import com.google.gson.internal.Pair;
import com.hpe.adm.nga.sdk.model.EntityModel;
import com.hpe.octane.ideplugins.eclipse.ui.entitylist.EntityListViewer;
import com.hpe.octane.ideplugins.eclipse.ui.entitylist.EntityModelMenuFactory;
import com.hpe.octane.ideplugins.eclipse.ui.entitylist.EntityMouseListener;
import com.hpe.octane.ideplugins.eclipse.util.DelayedRunnable;
import com.hpe.octane.ideplugins.eclipse.util.resource.SWTResourceManager;

/**
 * Composite that can display a list of other composites as a list.<br>
 * Row height is fixed, row width scales with parent width.
 */
public class AbsoluteLayoutEntityListViewer extends ScrolledComposite implements EntityListViewer {

    private static final Color selectionBackgroundColor = SWTResourceManager.getColor(SWT.COLOR_LIST_SELECTION);
    private static final Color selectionForegroundColor = SWTResourceManager.getColor(255, 255, 255);

    private static final Color backgroundColor = SWTResourceManager.getColor(255, 255, 255);
    private static final Color foregroundColor = SWTResourceManager.getColor(0, 0, 0);

    private interface RowProvider {
        Control getRow(int index, Composite parent);

        int getRowCount();
    }

    private static final int ROW_CREATE_THRESHOLD = 20;
    private static final int ROW_DISPOSE_THRESHOLD = 20;
    private static final int ROW_HEIGHT = 50;
    private static final int ROW_MIN_WIDTH = 500;

    private Label spacer;

    private RowProvider rowProvider;
    private Composite rowComposite;

    private List<EntityModel> entityList = new ArrayList<>();

    int selectedIndex = -1;
    int prevSelectedIndex = -1;

    private EntityModelMenuFactory entityModelMenuFactory;
    private List<EntityMouseListener> entityMouseListeners = new ArrayList<>();

    // DelayedRunnable resizeOnParent = new DelayedRunnable(() -> placeRows(),
    // 5);
    DelayedRunnable resizeOnScroll = new DelayedRunnable(() -> placeRows(), 20);

    private Listener displayListener = new Listener() {
        @Override
        public void handleEvent(Event event) {
            handleMouseFilterEvent(event);
        }
    };

    public AbsoluteLayoutEntityListViewer(
            Composite parent,
            int style,
            EntityModelRenderer entityModelRenderer,
            EntityModelMenuFactory entityModelMenuFactory) {

        super(parent, SWT.H_SCROLL | SWT.V_SCROLL);

        this.entityModelMenuFactory = entityModelMenuFactory;

        this.rowProvider = new RowProvider() {
            @Override
            public int getRowCount() {
                return entityList.size();
            }

            @Override
            public Control getRow(int index, Composite parent) {
                Control row = entityModelRenderer.createRow(parent, entityList.get(index));
                if (entityModelMenuFactory != null) {

                    row.addMenuDetectListener(new EntityModelRowMenuDetectListener(
                            row,
                            entityList.get(index),
                            AbsoluteLayoutEntityListViewer.this.entityModelMenuFactory));

                }
                return row;
            }
        };

        setExpandVertical(false);
        setExpandHorizontal(false);

        rowComposite = new Composite(this, SWT.NO_MERGE_PAINTS);

        spacer = new Label(rowComposite, SWT.SEPARATOR | SWT.SHADOW_NONE);
        spacer.setEnabled(false);

        setContent(rowComposite);
        setMinSize(rowComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {

                Rectangle rect = getBounds();
                rect.height = ROW_HEIGHT * rowProvider.getRowCount();
                if (getVerticalBar().isVisible()) {
                    rect.width -= getVerticalBar().getThumbBounds().width;
                }
                if (rect.width < ROW_MIN_WIDTH) {
                    rect.width = ROW_MIN_WIDTH;
                }
                rowComposite.setBounds(rect);

                // Calling resize here won't work, it's very weird, the scroll
                // event changes something inside the ScrolledComposite
                getVerticalBar().notifyListeners(SWT.Selection, null);
            }
        });

        getVerticalBar().addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                resizeOnScroll.execute();
            }
        });

        // Selection
        rowComposite.getDisplay().addFilter(SWT.MouseDown, displayListener);
        rowComposite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                rowComposite.getDisplay().removeFilter(SWT.MouseDown, displayListener);
            }
        });
    }

    private void adjustContainerHeight() {
        spacer.setBounds(0, 0, 0, ROW_HEIGHT * rowProvider.getRowCount());
    }

    private void placeRows() {
        Display.getDefault().asyncExec(() -> {
            disposeRowsIfNeeded();
            createRowsIfNeeded();
            resizeRowsIfNeeded();
        });
    }

    private void createRowsIfNeeded() {
        Pair<Integer, Integer> indexRange = getWithThreshold(getVisibleIndexRange(), ROW_CREATE_THRESHOLD);
        for (int i = indexRange.first; i <= indexRange.second; i++) {
            if (!isRowCreated(i)) {
                Control row = rowProvider.getRow(i, rowComposite);
                row.setData(i);

                if (i == selectedIndex) {
                    paintSelected();
                }
            }
        }
    }

    private void resizeRowsIfNeeded() {

        int compositeWidth = rowComposite.getBounds().width;
        Arrays.stream(rowComposite.getChildren()).forEach(control -> {
            if (control.getData() != null) {
                int index = (int) control.getData();
                int rowY = index * ROW_HEIGHT;
                Rectangle rect = new Rectangle(0, rowY, compositeWidth, ROW_HEIGHT);

                if (!control.getBounds().equals(rect)) {
                    control.setBounds(rect);
                }
            }
        });
    }

    private void disposeRowsIfNeeded() {
        Pair<Integer, Integer> indexRange = getWithThreshold(getVisibleIndexRange(), ROW_DISPOSE_THRESHOLD);

        // Dispose rows that are not in the visible range
        for (Control control : rowComposite.getChildren()) {
            if (control.getData() != null) {
                int currentIndex = (int) control.getData();
                if (currentIndex < indexRange.first || currentIndex > indexRange.second) {
                    control.dispose();
                }
            }
        }
    }

    private boolean isRowCreated(int index) {
        for (Control control : rowComposite.getChildren()) {
            if (control.getData() != null) {
                int currentIndex = (int) control.getData();
                if (currentIndex == index) {
                    return true;
                }
            }
        }
        return false;
    }

    private Pair<Integer, Integer> getVisibleIndexRange() {
        int firstIndex = 0;
        int secondIndex = 0;

        int visibleHeight = getBounds().height;
        int scrollY = getVerticalBar().getSelection();

        firstIndex = scrollY / ROW_HEIGHT;
        int rowCapacity = visibleHeight / ROW_HEIGHT;
        secondIndex = firstIndex + rowCapacity;

        if (secondIndex > rowProvider.getRowCount() - 1) {
            secondIndex = rowProvider.getRowCount() - 1;
        }

        return new Pair<Integer, Integer>(firstIndex, secondIndex);
    }

    private Pair<Integer, Integer> getWithThreshold(Pair<Integer, Integer> indexRange, int treshold) {
        int minIndex = indexRange.first;
        int maxIndex = indexRange.second;
        maxIndex += treshold;
        minIndex -= treshold;

        if (minIndex < 0) {
            minIndex = 0;
        }
        if (maxIndex > rowProvider.getRowCount() - 1) {
            maxIndex = rowProvider.getRowCount() - 1;
        }
        return new Pair<Integer, Integer>(minIndex, maxIndex);
    }

    private void handleMouseFilterEvent(Event event) {
        Control row = getEntityModelRowFromMouseFilter(event);

        if (row != null) {
            int rowIndex = (int) row.getData();
            EntityModel entityModel = entityList.get((int) row.getData());

            row.setFocus();

            // Selection
            changeSelection(rowIndex);

            // Fire listeners
            MouseEvent mouseEvent = new MouseEvent(event);
            for (EntityMouseListener listener : entityMouseListeners) {
                listener.mouseClick(entityModel, mouseEvent);
            }
        } else {
            changeSelection(-1);
        }

    }

    private Control getEntityModelRowFromMouseFilter(Event event) {
        if (event.widget instanceof Control) {
            for (Control row : rowComposite.getChildren()) {
                if (containsControl(row, (Control) event.widget)) {
                    return row;
                }
            }
        }
        return null;
    }

    private static boolean containsControl(Control source, Control target) {
        if (source == target) {
            return true;
        } else if (source instanceof Composite) {
            for (Control control : ((Composite) source).getChildren()) {
                if (containsControl(control, target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void changeSelection(int index) {
        if (selectedIndex != index) {
            this.prevSelectedIndex = selectedIndex;
            this.selectedIndex = index;
            paintSelected();
        }
    }

    private void paintSelected() {
        Control row = findRowByIndex(selectedIndex);
        if (row != null && !row.isDisposed()) {
            row.setBackground(selectionBackgroundColor);
            row.setForeground(selectionForegroundColor);
        }
        row = findRowByIndex(prevSelectedIndex);
        if (row != null && !row.isDisposed()) {
            row.setBackground(backgroundColor);
            row.setForeground(foregroundColor);
        }
    }

    private Control findRowByIndex(int index) {
        for (Control c : rowComposite.getChildren()) {
            if (c.getData() != null) {
                int cData = (int) c.getData();
                if (cData == index) {
                    return c;
                }
            }
        }
        return null;
    }

    @Override
    public void setEntityModels(Collection<EntityModel> entityModels) {
        this.entityList = new ArrayList<>(entityModels);
        adjustContainerHeight();
        placeRows();
        selectedIndex = -1;
        prevSelectedIndex = -1;
    }

    @Override
    public void addEntityMouseListener(EntityMouseListener entityMouseListener) {
        this.entityMouseListeners.add(entityMouseListener);
    }

    @Override
    public void removeEntityMouseListener(EntityMouseListener entityMouseListener) {
        this.entityMouseListeners.remove(entityMouseListener);
    }

    @Override
    public void setEntityModelMenuFatory(EntityModelMenuFactory entityModelMenuFactory) {
        this.entityModelMenuFactory = entityModelMenuFactory;
    }
}
