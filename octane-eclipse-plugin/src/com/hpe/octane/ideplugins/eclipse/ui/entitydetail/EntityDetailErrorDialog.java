package com.hpe.octane.ideplugins.eclipse.ui.entitydetail;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;

import com.hpe.adm.nga.sdk.exception.OctaneException;
import com.hpe.octane.ideplugins.eclipse.ui.util.resource.PlatformResourcesManager;

public class EntityDetailErrorDialog extends Dialog {

    private Shell shell;
    private Composite compositeBtns;
    private Text exceptionText;

    public EntityDetailErrorDialog(Shell parent) {
        super(parent);
        shell = new Shell(getParent(), SWT.CLOSE | SWT.RESIZE | SWT.TITLE | SWT.PRIMARY_MODAL);
        shell.setSize(600, 300);
        shell.setLayout(new GridLayout(1, false));
        shell.setImage(PlatformResourcesManager.getPlatformImage(ISharedImages.IMG_DEC_FIELD_ERROR));
        
        exceptionText = new Text(shell, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
        exceptionText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
      
        compositeBtns = new Composite(shell, SWT.NONE);
        FillLayout fl_compositeBtns = new FillLayout(SWT.HORIZONTAL);
        fl_compositeBtns.spacing = 10;
        compositeBtns.setLayout(fl_compositeBtns);
        compositeBtns.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
    }
    
    private static void positionShell(Shell shell) {
        Monitor primary = shell.getMonitor();
        Rectangle bounds = primary.getBounds();
        Rectangle rect = shell.getBounds();
        int x = bounds.x + (bounds.width - rect.width) / 2;
        int y = bounds.y + (bounds.height - rect.height) / 2;
        shell.setLocation(x, y);
    }

    public String open(OctaneException exception, String title) {
        positionShell(shell);
        shell.setText(title);
        exceptionText.setText(exception.getMessage());   
        shell.open();
        
        Display display = getParent().getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        return null;
    }
    
    public void addButton(String btnText, Runnable clickedRunnable) {
        Button btn = new Button(compositeBtns, SWT.NONE);
        btn.setText(btnText);
        btn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                clickedRunnable.run();
            }
        });
    }
    
    public void close() {
        shell.close();
    }
}
