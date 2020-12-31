package com.github.savemytumblr;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.scribe.model.Token;

import com.github.savemytumblr.api.Authenticate;
import com.github.savemytumblr.exception.BaseException;

public class MainApp {
    private static boolean loggedIn = false;
    private static Backup backup = null;

    public static void main(String[] args) {
        final Preferences prefs = Preferences.userRoot().node(MainApp.class.getName());
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        Display display = new Display();
        final Shell shell = new Shell(display);
        shell.setText("SaveMyTumblr");

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        shell.setLayout(gridLayout);

        Label lblBlogName = new Label(shell, SWT.NONE);
        lblBlogName.setText("Blog:");

        Text txtBlogName = new Text(shell, SWT.SINGLE);
        txtBlogName.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        ProgressBar pbDownload = new ProgressBar(shell, SWT.SMOOTH);
        pbDownload.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));

        final Text txtLog = new Text(shell, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
        txtLog.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1));

        final Text txtTumblrLog = new Text(shell, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
        txtTumblrLog.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1));

        Button btnLogin = new Button(shell, SWT.PUSH);
        btnLogin.setText("Login");
        Button btnBackup = new Button(shell, SWT.PUSH);
        btnBackup.setText("Backup");
        btnBackup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        shell.setSize(500, 500);
        shell.open();

        final TumblrClient tc = new TumblrClient(new TumblrClient.Executor() {
            @Override
            public void execute(Runnable runnable) {
                executor.execute(runnable);
            }
        }, new TumblrClient.Logger() {
            @Override
            public void warning(String msg) {
                display.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        txtTumblrLog.append("WARNING: " + msg + "\n");
                    }

                });
            }

            @Override
            public void info(String msg) {
                display.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        txtTumblrLog.append("INFO: " + msg + "\n");
                    }

                });
            }

            @Override
            public void error(String msg) {
                display.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        txtTumblrLog.append("ERROR: " + msg + "\n");
                    }

                });
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
                    return false;
                }
            }

            @Override
            public String get(String key, String defValue) {
                return prefs.get(key, defValue);
            }
        });

        tc.setOnLoginListener(new TumblrClient.OnLoginListener() {
            @Override
            public void onLoginFailure(BaseException e) {
                display.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        txtLog.append("Login failed\n");
                        System.exit(-1);
                    }

                });
            }

            @Override
            public void onAccessRequest(Authenticate authenticator, Token requestToken, String authenticationUrl) {
                display.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        LoginBrowser lb = new LoginBrowser(shell);
                        String authVerifier = lb.open(authenticationUrl);

                        authenticator.verify(requestToken, authVerifier);
                    }

                });
            }

            @Override
            public void onAccessGranted() {
                display.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        loggedIn = true;
                        btnLogin.setText("Logout");
                        txtLog.append("Logged in!\n");
                    }
                });
            }

            @Override
            public void onAccessDenied() {
                display.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        txtLog.append("Access denied\n");
                        System.exit(-2);
                    }
                });
            }
        });

        btnLogin.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (loggedIn) {
                    tc.logout();
                    loggedIn = false;
                    txtLog.append("Logged out\n");
                    btnLogin.setText("Login");
                } else {
                    tc.login();
                }
            }
        });

        btnBackup.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if ((backup != null) && backup.isRunning()) {
                    backup.stop();
                    btnBackup.setText("Backup");
                } else {
                    Path mainPath = Paths
                            .get(MainApp.class.getProtectionDomain().getCodeSource().getLocation().getPath());
                    backup = new Backup(mainPath, tc, txtBlogName.getText(), new Backup.Progress() {
                        @Override
                        public void progress(int current, int total) {
                            display.asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    pbDownload.setMaximum(total);
                                    pbDownload.setSelection(current);
                                }
                            });
                        }

                        @Override
                        public void log(String msg) {
                            display.asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    txtLog.append(msg + "\n");
                                }
                            });
                        }
                    });
                    backup.start();
                    btnBackup.setText("Stop backup");
                }
            }
        });

        tc.login();

        while (shell != null && !shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }

        display.dispose();
    }
}
