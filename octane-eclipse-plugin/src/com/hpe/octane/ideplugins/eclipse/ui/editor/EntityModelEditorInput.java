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
package com.hpe.octane.ideplugins.eclipse.ui.editor;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import com.hpe.adm.nga.sdk.model.EntityModel;
import com.hpe.adm.octane.ideplugins.services.filtering.Entity;
import com.hpe.octane.ideplugins.eclipse.ui.activeitem.ImageDataImageDescriptor;
import com.hpe.octane.ideplugins.eclipse.util.EntityIconFactory;

public class EntityModelEditorInput implements IElementFactory, IEditorInput {

    private static final String FACTORY_ID = "com.hpe.octane.ideplugins.eclipse.ui.editor.EntityModelEditorInput";
    private static final EntityIconFactory entityIconFactory = new EntityIconFactory(20, 20, 7);

    private long id;
    private Entity entityType;
    private String title = "";

    // Default constructor needed because of IElementFactory
    public EntityModelEditorInput() {
    }

    public EntityModelEditorInput(EntityModel entityModel) {
        this.id = Long.parseLong(entityModel.getValue("id").getValue().toString());
        // Not all entities have a name, this field is optional
        if (entityModel.getValue("name") != null) {
            this.title = entityModel.getValue("name").getValue().toString();
        }
        this.entityType = Entity.getEntityType(entityModel);
    }

    public EntityModelEditorInput(long id, Entity entityType) {
        this.id = id;
        this.entityType = entityType;
    }

    public EntityModelEditorInput(long id, Entity entityType, String title) {
        this.id = id;
        this.entityType = entityType;
        this.title = title;
    }

    public long getId() {
        return id;
    }

    public Entity getEntityType() {
        return entityType;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return new ImageDataImageDescriptor(
                entityIconFactory.getImageIcon(entityType).getImageData());
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }

    @Override
    public String getToolTipText() {
        return String.valueOf(id);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EntityModelEditorInput other = (EntityModelEditorInput) obj;
        if (entityType != other.entityType)
            return false;
        if (id != other.id)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "EntityModelEditorInput [id=" + id + ", entityType=" + entityType + ", title = " + title + "]";
    }

    @Override
    public IPersistableElement getPersistable() {
        IPersistableElement persistableElement = new IPersistableElement() {
            @Override
            public void saveState(IMemento memento) {
                memento.putString("id", id + "");
                memento.putString("entityType", entityType.name());
                memento.putString("title", title);
            }

            @Override
            public String getFactoryId() {
                return FACTORY_ID;
            }
        };
        return persistableElement;
    }

    @Override
    public IAdaptable createElement(IMemento memento) {
        long id = Long.valueOf(memento.getString("id"));
        Entity entityType = Entity.valueOf(memento.getString("entityType"));
        String title = memento.getString("title");
        return new EntityModelEditorInput(id, entityType, title);
    }

}
