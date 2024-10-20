package com.github.savemytumblr.ui;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import com.github.savemytumblr.LogText;
import com.github.savemytumblr.LoginBrowser;
import com.github.savemytumblr.TumblrClient;
import com.github.savemytumblr.api.Authenticate;
import com.github.savemytumblr.exception.BaseException;

public class Backup extends TabItem {
    private String backupPath = "";
    private boolean loggedIn = false;
    private com.github.savemytumblr.Backup backup = null;

    public Backup(TabFolder parent, Preferences prefs, ExecutorService executor) {
        super(parent, SWT.BORDER);

        this.backupPath = System.getProperty("user.home");

        Composite comp = new Composite(parent, SWT.BORDER);

        GridLayout gridLayout = new GridLayout();
        comp.setLayout(gridLayout);
        gridLayout.numColumns = 2;

        Label lblBlogName = new Label(comp, SWT.NONE);
        lblBlogName.setText("Blog:");

        Text txtBlogName = new Text(comp, SWT.SINGLE);
        txtBlogName.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        Label lblBackupPath = new Label(comp, SWT.NONE);
        lblBackupPath.setText("Backup path: " + this.backupPath);
        lblBackupPath.setLayoutData(new GridData(GridData.FILL, GridData.VERTICAL_ALIGN_CENTER, true, false));

        Button btnBackupPathSelect = new Button(comp, SWT.PUSH);
        btnBackupPathSelect.setText("Select");
        btnBackupPathSelect.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        ProgressBar pbDownload = new ProgressBar(comp, SWT.SMOOTH);
        pbDownload.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));

        LogText txtLog = new LogText(comp);
        txtLog.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1));

        LogText txtTumblrLog = new LogText(comp);
        txtTumblrLog.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1));

        Button btnLogin = new Button(comp, SWT.PUSH);
        btnLogin.setText("Login");
        Button btnBackup = new Button(comp, SWT.PUSH);
        btnBackup.setText("Backup");
        btnBackup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        btnBackupPathSelect.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                backupPath = new DirectoryDialog(parent.getShell()).open();
                lblBackupPath.setText("Backup path: " + backupPath);
            }
        });

        TumblrClient tc = new TumblrClient(new TumblrClient.Executor() {
            @Override
            public void execute(Runnable runnable) {
                executor.execute(runnable);
            }
        }, new TumblrClient.Logger() {
            @Override
            public void warning(String msg) {
                getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        txtTumblrLog.appendLine("WARNING: " + msg);
                    }
                });
            }

            @Override
            public void info(String msg) {
                getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        txtTumblrLog.appendLine("INFO: " + msg);
                    }
                });
            }

            @Override
            public void error(String msg) {
                getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        txtTumblrLog.appendLine("ERROR: " + msg);
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
                    e.printStackTrace();

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
                getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        txtLog.appendLine("Login failed");
                        System.exit(-1);
                    }
                });
            }

            @Override
            public void onAccessRequest(Authenticate authenticator, String authenticationUrl, String state) {
                getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        LoginBrowser lb = new LoginBrowser(state, parent.getShell());
                        String authCode = lb.open(authenticationUrl);

                        authenticator.getAccessToken(authCode);
                    }
                });
            }

            @Override
            public void onAccessGranted() {
                getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        loggedIn = true;
                        btnLogin.setText("Logout");
                        txtLog.appendLine("Logged in!");
                    }
                });
            }

            @Override
            public void onAccessDenied() {
                getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        txtLog.appendLine("Access denied");
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
                    txtLog.appendLine("Logged out");
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
                if (backupPath.isEmpty()) {
                    return;
                }

                if ((backup != null) && backup.isRunning()) {
                    backup.stop();
                    btnBackup.setText("Backup");
                } else {
                    backup = new com.github.savemytumblr.Backup(Paths.get(backupPath), tc, txtBlogName.getText(),
                            new com.github.savemytumblr.Backup.Progress() {
                                @Override
                                public void progress(int current, int total) {
                                    getDisplay().asyncExec(new Runnable() {
                                        @Override
                                        public void run() {
                                            pbDownload.setMaximum(total);
                                            pbDownload.setSelection(current);
                                        }
                                    });
                                }

                                @Override
                                public void log(String msg) {
                                    getDisplay().asyncExec(new Runnable() {
                                        @Override
                                        public void run() {
                                            txtLog.appendLine(msg);
                                        }
                                    });
                                }

                                @Override
                                public void onCompleted(boolean ok) {
                                    getDisplay().asyncExec(new Runnable() {
                                        @Override
                                        public void run() {
                                            btnBackup.setText("Backup");
                                        }
                                    });
                                }
                            });

                    backup.start();
                    btnBackup.setText("Stop backup");
                }
            }
        });

        setControl(comp);
        setText("Backup");

        tc.login();
    }

    @Override
    protected void checkSubclass() {
        // TODO Auto-generated method stub
    }
}
