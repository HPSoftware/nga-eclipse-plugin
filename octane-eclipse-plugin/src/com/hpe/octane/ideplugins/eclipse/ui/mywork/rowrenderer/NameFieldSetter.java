package com.hpe.octane.ideplugins.eclipse.ui.mywork.rowrenderer;

import static com.hpe.adm.octane.services.util.Util.getUiDataFromModel;

import com.hpe.adm.nga.sdk.model.EntityModel;
import com.hpe.octane.ideplugins.eclipse.ui.entitylist.custom.EntityModelRow;

class NameFieldSetter implements RowFieldSetter {

    private static String fieldName = "name";

    @Override
    public void setField(EntityModelRow row, EntityModel entityModel) {
        row.setEntityName(getUiDataFromModel(entityModel.getValue(fieldName)));
    }

    @Override
    public String[] getFieldNames() {
        return new String[] { fieldName };
    }
}