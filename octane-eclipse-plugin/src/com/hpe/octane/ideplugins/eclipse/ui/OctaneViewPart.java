/*******************************************************************************
 * © 2017 EntIT Software LLC, a Micro Focus company, L.P.
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
package com.hpe.octane.ideplugins.eclipse.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.ViewPart;

import com.hpe.octane.ideplugins.eclipse.Activator;
import com.hpe.octane.ideplugins.eclipse.preferences.PluginPreferencePage;
import com.hpe.octane.ideplugins.eclipse.ui.util.LoadingComposite;
import com.hpe.octane.ideplugins.eclipse.ui.util.StackLayoutComposite;
import com.hpe.octane.ideplugins.eclipse.ui.util.WelcomeComposite;

/**
 * ViewPart intended for controls for the Octane plugin <br>
 * Will show WelcomeComposite if connection settings is missing. <br>
 * Has support for masking the whole widget with a LoadingComposite
 */
public abstract class OctaneViewPart extends ViewPart {

    private WelcomeComposite welcomeComposite;
    private LoadingComposite loadingComposite;
    private Control octaneViewControl;

    private StackLayoutComposite rootContainer;

    @Override
    public void createPartControl(Composite parent) {
        rootContainer = new StackLayoutComposite(parent, SWT.NONE);

        welcomeComposite = new WelcomeComposite(rootContainer, SWT.NONE, () -> {
            PreferencesUtil.createPreferenceDialogOn(parent.getShell(),
                    PluginPreferencePage.ID,
                    null,
                    null).open();
        });

        loadingComposite = new LoadingComposite(rootContainer, SWT.NONE);
        octaneViewControl = createOctanePartControl(rootContainer);

        Activator.addConnectionSettingsChangeHandler(() -> {
            showDefaultView();
        });
    }

    private void showDefaultView() {
        if (Activator.getConnectionSettings().isEmpty()) {
            showWelcome();
        } else {
            showContent();
        }
    }

    public abstract Control createOctanePartControl(Composite octaneControlParent);

    public void showLoading() {
        showControl(loadingComposite);
    }

    public void showContent() {
        showControl(octaneViewControl);
    }

    public void showWelcome() {
        showControl(welcomeComposite);
    }

    public void showControl(Control control) {
        Display.getDefault().asyncExec(() -> rootContainer.showControl(control));
    }
}
