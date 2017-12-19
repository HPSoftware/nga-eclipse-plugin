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
package com.hpe.octane.ideplugins.eclipse;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.hpe.adm.octane.ideplugins.services.connection.BasicConnectionSettingProvider;
import com.hpe.adm.octane.ideplugins.services.connection.ConnectionSettings;
import com.hpe.adm.octane.ideplugins.services.connection.HttpClientProvider;
import com.hpe.adm.octane.ideplugins.services.di.ServiceModule;
import com.hpe.adm.octane.ideplugins.services.util.UrlParser;
import com.hpe.octane.ideplugins.eclipse.preferences.PluginPreferenceStorage;
import com.hpe.octane.ideplugins.eclipse.preferences.PluginPreferenceStorage.PreferenceConstants;
import com.hpe.octane.ideplugins.eclipse.ui.editor.EntityModelEditor;
import com.hpe.octane.ideplugins.eclipse.ui.editor.EntityModelEditorInput;
import com.hpe.octane.ideplugins.eclipse.ui.editor.snake.KonamiCodeListener;
import com.hpe.octane.ideplugins.eclipse.ui.editor.snake.SnakeEditor;
import com.hpe.octane.ideplugins.eclipse.ui.search.SearchEditor;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "octane.eclipse.plugin"; //$NON-NLS-1$

    // The shared instance
    private static Activator plugin;

    private static BasicConnectionSettingProvider settingsProviderInstance = new BasicConnectionSettingProvider();
    private static ServiceModule serviceModuleInstance = new ServiceModule(settingsProviderInstance);

    /**
     * The constructor
     */
    public Activator() {
    }

    public static void setConnectionSettings(ConnectionSettings connectionSettings) {
        settingsProviderInstance.setConnectionSettings(connectionSettings);
    }

    public static ConnectionSettings getConnectionSettings() {
        return settingsProviderInstance.getConnectionSettings();
    }

    public static void addConnectionSettingsChangeHandler(Runnable changeHandler) {
        settingsProviderInstance.addChangeHandler(changeHandler);
    }

    public static HttpClientProvider geOctaneHttpClient() {
        return serviceModuleInstance.geOctaneHttpClient();
    }

    public static <T> T getInstance(Class<T> type) {
        return serviceModuleInstance.getInstance(type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
     * BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);

        configureLogbackInBundle(context.getBundle());

        plugin = this;

        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        try {
            ISecurePreferences securePrefs = PluginPreferenceStorage.getSecurePrefs();
            String baseUrl = securePrefs.get(PreferenceConstants.OCTANE_SERVER_URL, "");
            String username = securePrefs.get(PreferenceConstants.USERNAME, "");
            String password = securePrefs.get(PreferenceConstants.PASSWORD, "");
            if (StringUtils.isNotEmpty(baseUrl)) {
                ConnectionSettings loadedConnectionSettings = UrlParser.resolveConnectionSettings(baseUrl, username, password);
                settingsProviderInstance.setConnectionSettings(loadedConnectionSettings);
            } else {
                settingsProviderInstance.setConnectionSettings(new ConnectionSettings());
            }

        } catch (Exception e) {
            getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, Status.ERROR,
                    "An exception has occured when loading the Octane connection details", e));
        }

        settingsProviderInstance.addChangeHandler(() -> {
            // Clear active item
            PluginPreferenceStorage.setActiveItem(null);

            // Close active entity editors and search editors
            for (IEditorReference editor : page.getEditorReferences()) {
                if (EntityModelEditor.ID.equals(editor.getId()) ||
                        SearchEditor.ID.equals(editor.getId())) {
                    page.closeEditor(editor.getEditor(false), false);
                }
            }
        });

        // Restore all entity model editors from their references, this is a
        // silly fix to properly set the editor part icon and tooltip
        for (IEditorReference editorReference : page.getEditorReferences()) {
            if (editorReference.getEditorInput() instanceof EntityModelEditorInput) {
                editorReference.getEditor(true);
            }
        }

        // Easter egg
        KonamiCodeListener konamiCodeListener = new KonamiCodeListener(() -> {
            try {
                // Unfortunately the game explodes on mac os, causing the ide to
                // not respond, don't have time to fix now
                String os = System.getProperty("os.name").toLowerCase();
                if (os != null && os.indexOf("win") >= 0) {
                    IWorkbenchPage currentPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    currentPage.openEditor(SnakeEditor.snakeEditorInput, SnakeEditor.ID);
                }
            } catch (PartInitException ignored) {
            }
        });
        PlatformUI.getWorkbench().getDisplay().addFilter(SWT.KeyDown, konamiCodeListener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.
     * BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in
     * relative path
     *
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    private void configureLogbackInBundle(Bundle bundle) {

    }
}
