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
package com.hpe.octane.ideplugins.eclipse.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import com.hpe.adm.octane.services.filtering.Entity;
import com.hpe.octane.ideplugins.eclipse.util.resource.ImageResources;

public class EntityIconFactory {

    private static final IconDetail undefinedIconDetail = new IconDetail(0, 0, 0, "N/A");

    // map to color and short text
    private final Map<Entity, IconDetail> iconDetailMap = new HashMap<>();

    // cache
    private final Map<Entity, ImageData> imageDataCache = new HashMap<>();

    private int iconHeight = 35;
    private int iconWidth = 35;
    private Color fontColor = new Color(Display.getCurrent(), 255, 255, 255);
    private int fontSize = 11;

    private static Image activeImg = ImageResources.ACTIVEITEM.getImage();

    public EntityIconFactory() {
        init();
    }

    public EntityIconFactory(int iconHeight, int iconWidth, int fontSize, Color fontColor) {
        this.iconHeight = iconHeight;
        this.iconWidth = iconWidth;
        this.fontColor = fontColor;
        this.fontSize = fontSize;
        init();
    }

    public EntityIconFactory(int iconHeight, int iconWidth, int fontSize) {
        this.iconHeight = iconHeight;
        this.iconWidth = iconWidth;
        this.fontSize = fontSize;
        init();
    }

    private void init() {
        iconDetailMap.put(Entity.USER_STORY, new IconDetail(218, 199, 120, "US"));
        iconDetailMap.put(Entity.QUALITY_STORY, new IconDetail(95, 112, 118, "QS"));
        iconDetailMap.put(Entity.DEFECT, new IconDetail(190, 102, 92, "D"));
        iconDetailMap.put(Entity.EPIC, new IconDetail(202, 170, 209, "E"));
        iconDetailMap.put(Entity.FEATURE, new IconDetail(226, 132, 90, "F"));

        iconDetailMap.put(Entity.TASK, new IconDetail(137, 204, 174, "T"));

        iconDetailMap.put(Entity.TEST_SUITE, new IconDetail(133, 114, 147, "TS"));
        iconDetailMap.put(Entity.MANUAL_TEST, new IconDetail(96, 121, 141, "MT"));
        iconDetailMap.put(Entity.GHERKIN_TEST, new IconDetail(120, 196, 192, "GT"));
        iconDetailMap.put(Entity.AUTOMATED_TEST, new IconDetail(135, 123, 117, "AT"));

        iconDetailMap.put(Entity.MANUAL_TEST_RUN, new IconDetail(133, 169, 188, "MR"));
        iconDetailMap.put(Entity.TEST_SUITE_RUN, new IconDetail(133, 169, 188, "SR"));

        iconDetailMap.put(Entity.COMMENT, new IconDetail(234, 179, 124, "C"));
    }

    private void loadImageData(Entity entity) {
        IconDetail iconDetail = iconDetailMap.containsKey(entity) ? iconDetailMap.get(entity) : undefinedIconDetail;

        Display display = Display.getDefault();
        Image img = new Image(display, iconWidth, iconHeight);
        GC gc = new GC(img);

        gc.setBackground(iconDetail.getColor());
        gc.fillRectangle(0, 0, iconWidth, iconHeight);

        gc.setForeground(fontColor);
        gc.setFont(new Font(display, "Arial", fontSize, SWT.BOLD));

        int fontX = (iconHeight - gc.textExtent(iconDetail.getDisplayLabelText()).y) / 2;
        int fontY = (iconWidth - gc.textExtent(iconDetail.getDisplayLabelText()).x) / 2;

        gc.drawText(iconDetail.getDisplayLabelText(), fontY, fontX);
        imageDataCache.put(entity, img.getImageData());

        gc.dispose();
        img.dispose();
    }

    private ImageData overlayActiveImage(ImageData imgData) {
        Image img = new Image(Display.getDefault(), imgData);
        GC gc = new GC(img);

        int xpercent = 60 * iconWidth / 100;
        int ypercent = 60 * iconWidth / 100;

        gc.drawImage(activeImg,
                0, 0, activeImg.getBounds().width, activeImg.getBounds().height,
                xpercent, ypercent, iconWidth - xpercent, iconWidth - ypercent);

        return img.getImageData();
    }

    public Image getImageIcon(Entity entity) {
        return getImageIcon(entity, false);
    }

    public Image getImageIcon(Entity entity, boolean isActive) {
        if (!imageDataCache.containsKey(entity)) {
            loadImageData(entity);
        }

        ImageData imageData = imageDataCache.get(entity);

        if (isActive) {
            imageData = overlayActiveImage(imageData);
        }

        return new Image(Display.getDefault(), imageData);
    }

}
