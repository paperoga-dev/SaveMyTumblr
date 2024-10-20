package com.github.savemytumblr;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;

public class MainApp {

    public static void main(String[] args) {
        Preferences prefs = Preferences.userRoot().node(MainApp.class.getName());
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setText(Constants.APP_NAME);
        shell.setLayout(new FillLayout());

        TabFolder tf = new TabFolder(shell, SWT.BORDER);

        new com.github.savemytumblr.ui.Backup(tf, prefs, executor);
        new com.github.savemytumblr.ui.Converter(tf, prefs, executor);

        shell.setSize(700, 700);
        shell.open();

        while (shell != null && !shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        executor.shutdown();

        display.dispose();
    }
}
