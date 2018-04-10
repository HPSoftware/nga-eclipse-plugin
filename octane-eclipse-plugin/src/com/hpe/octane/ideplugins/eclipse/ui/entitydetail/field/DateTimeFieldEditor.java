/*******************************************************************************
 * © 2017 EntIT Software LLC, a Micro Focus company, L.P.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.hpe.octane.ideplugins.eclipse.ui.entitydetail.field;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;

import com.hpe.adm.nga.sdk.model.DateFieldModel;
import com.hpe.adm.nga.sdk.model.ReferenceFieldModel;
import com.hpe.octane.ideplugins.eclipse.ui.entitydetail.model.EntityModelWrapper;

public class DateTimeFieldEditor extends Composite implements FieldEditor {

    protected EntityModelWrapper entityModelWrapper;
    protected String fieldName;

    private DateTime dtDate;
    private DateTime dtTime;
    private Label btnNull;

    private FieldMessageComposite fieldMessageComposite;
    private Label lblEmptyText;

    public DateTimeFieldEditor(Composite parent, int style) {
        super(parent, style);
        GridLayout gridLayout = new GridLayout(5, false);
        gridLayout.marginWidth = 0;
        setLayout(gridLayout);

        dtDate = new DateTime(this, SWT.NONE);
        dtDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

        dtTime = new DateTime(this, SWT.NONE | SWT.TIME);
        dtTime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

        btnNull = new Label(this, SWT.NONE);
        btnNull.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnNull.setText("X");

        lblEmptyText = new Label(this, SWT.NONE);
        lblEmptyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        lblEmptyText.setText("no date set");

        // Init
        setDateTimeVisible(false);

        // Nullify
        btnNull.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                setDateTimeVisible(false);
                entityModelWrapper.setValue(new ReferenceFieldModel(fieldName, null));
            }
        });

        // De-nullify
        lblEmptyText.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                setDateTimeVisible(true);
                ZonedDateTime now = ZonedDateTime.now();
                setZonedDateTime(now);
                entityModelWrapper.setValue(new DateFieldModel(fieldName, now));
            }
        });

        fieldMessageComposite = new FieldMessageComposite(this, SWT.NONE);
        fieldMessageComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true, 1, 1));

        SelectionAdapter selectionListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                entityModelWrapper.setValue(new DateFieldModel(fieldName, getZonedDateTime()));
            }
        };
        dtDate.addSelectionListener(selectionListener);
        dtTime.addSelectionListener(selectionListener);
    }

    private void setDateTimeVisible(boolean isDateTimeVisible) {
        dtDate.setVisible(isDateTimeVisible);
        ((GridData) dtDate.getLayoutData()).exclude = !isDateTimeVisible;

        dtTime.setVisible(isDateTimeVisible);
        ((GridData) dtTime.getLayoutData()).exclude = !isDateTimeVisible;

        btnNull.setVisible(isDateTimeVisible);
        ((GridData) btnNull.getLayoutData()).exclude = !isDateTimeVisible;

        lblEmptyText.setVisible(!isDateTimeVisible);
        ((GridData) lblEmptyText.getLayoutData()).exclude = isDateTimeVisible;

        layout(true);
    }

    private boolean isDateTimeVisible() {
        return dtDate.isVisible() && dtTime.isVisible() && btnNull.isVisible();
    }

    private void setZonedDateTime(ZonedDateTime zonedDateTime) {
        if (zonedDateTime != null) {
            dtDate.setYear(zonedDateTime.getYear());
            dtDate.setMonth(zonedDateTime.getMonthValue());
            dtDate.setDay(zonedDateTime.getDayOfMonth());
            dtTime.setHours(zonedDateTime.getHour());
            dtTime.setMinutes(zonedDateTime.getMinute());
            dtTime.setSeconds(zonedDateTime.getSecond());
            setDateTimeVisible(true);
        } else {
            setDateTimeVisible(false);
        }
    }

    private ZonedDateTime getZonedDateTime() {
        if (!isDateTimeVisible()) {
            return null;
        } else {
            return ZonedDateTime.of(
                    dtDate.getYear(),
                    dtDate.getMonth(),
                    dtDate.getDay(),
                    dtTime.getHours(),
                    dtTime.getMinutes(),
                    dtTime.getSeconds(),
                    0,
                    ZoneId.systemDefault());
        }
    }

    @Override
    public void setField(EntityModelWrapper entityModel, String fieldName) {
        this.entityModelWrapper = entityModel;
        this.fieldName = fieldName;

        DateFieldModel fieldModel = (DateFieldModel) entityModel.getValue(fieldName);

        if (fieldModel == null || fieldModel.getValue() == null) {
            setZonedDateTime(null);
        } else {
            setZonedDateTime(fieldModel.getValue());
        }
    }

    @Override
    public void setFieldMessage(FieldMessage fieldMessage) {
        fieldMessageComposite.setFieldMessage(fieldMessage);
    }

    @Override
    public FieldMessage getFieldMessage() {
        return fieldMessageComposite.getFieldMessage();
    }

}
