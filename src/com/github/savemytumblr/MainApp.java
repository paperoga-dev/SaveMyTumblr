package com.github.savemytumblr;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;

import com.github.savemytumblr.api.Authenticate;
import com.github.savemytumblr.exception.BaseException;
import com.github.savemytumblr.ui.LoginBrowser;
import com.github.savemytumblr.ui.TabItem;

public class MainApp {
    static TabItem backupTab = null;
    static TabItem followTab = null;

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        Preferences prefs = Preferences.userRoot().node(MainApp.class.getName());

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Display display = new Display();
            Shell shell = new Shell(display);
            shell.setText(Constants.APP_NAME);
            shell.setLayout(new FillLayout(SWT.VERTICAL));

            TumblrClient tc = new TumblrClient(new TumblrClient.Executor() {
                @Override
                public void execute(Runnable runnable) {
                    executor.execute(runnable);
                }
            }, new TumblrClient.Storage() {
                @Override
                public void remove(String key) {
                    prefs.remove(key);
                }

                @Override
                public void put(String key, String value) {
                    prefs.put(key, value);
                }

                @Override
                public boolean has(String key) {
                    try {
                        return Arrays.asList(prefs.keys()).contains(key);
                    } catch (BackingStoreException e) {
                        e.printStackTrace();

                        return false;
                    }
                }

                @Override
                public String get(String key, String defValue) {
                    return prefs.get(key, defValue);
                }
            }, new TumblrClient.OnLoginAction() {

                @Override
                public void onAccessRequest(Authenticate authenticator, String authenticationUrl, String state) {
                    display.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            LoginBrowser lb = new LoginBrowser(state, shell);
                            String authCode = lb.open(authenticationUrl);

                            authenticator.getAccessToken(authCode);
                        }
                    });
                }
            });

            tc.addLoginListener(new TumblrClient.LoginListener() {
                @Override
                public void onLoginFailure(BaseException e) {
                    display.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            MainApp.backupTab.setEnabled(false);
                            MainApp.followTab.setEnabled(false);
                        }
                    });
                }

                @Override
                public void onAccessGranted() {
                    display.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            MainApp.backupTab.setEnabled(true);
                            MainApp.followTab.setEnabled(true);
                        }
                    });
                }

                @Override
                public void onAccessDenied() {
                    display.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            MainApp.backupTab.setEnabled(false);
                            MainApp.followTab.setEnabled(false);
                        }
                    });
                }
            });

            TabFolder tf = new TabFolder(shell, SWT.BORDER);
            tf.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (!tc.isLogged()) {
                        e.doit = false;
                        tf.setSelection(0);
                    }
                }
            });

            new com.github.savemytumblr.ui.Tumblr(tc, tf);
            MainApp.backupTab = new com.github.savemytumblr.ui.Backup(tc, tf);
            MainApp.backupTab.setEnabled(false);
            MainApp.followTab = new com.github.savemytumblr.ui.Follow(tc, tf);
            MainApp.followTab.setEnabled(false);
            new com.github.savemytumblr.ui.Converter(tf, executor);

            shell.setSize(700, 600);
            shell.open();

            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }

            executor.shutdown();

            display.dispose();
        }
    }
}
