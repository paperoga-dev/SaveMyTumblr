package com.github.savemytumblr.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.TabFolder;

import com.github.savemytumblr.TumblrClient;
import com.github.savemytumblr.exception.BaseException;

public class Tumblr extends TabItem {

    public Tumblr(TumblrClient tumblrClient, TabFolder parent) {
        super("Tumblr", parent);

        GridLayout gridLayout = new GridLayout(2, false);
        getComp().setLayout(gridLayout);

        LogText txtTumblrLog = new LogText(getComp());
        txtTumblrLog.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        Button btnSaveLog = new Button(getComp(), SWT.PUSH);
        btnSaveLog.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        btnSaveLog.setText("Save log");

        Button btnLogin = new Button(getComp(), SWT.PUSH);
        btnLogin.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        btnLogin.setText("Login");

        tumblrClient.addLogger(new TumblrClient.Logger() {
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
        });

        tumblrClient.addLoginListener(new TumblrClient.LoginListener() {
            @Override
            public void onLoginFailure(BaseException e) {
                getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        txtTumblrLog.appendLine("Login failed");
                        System.exit(-1);
                    }
                });
            }

            @Override
            public void onAccessGranted() {
                getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        btnLogin.setText("Logout");
                        txtTumblrLog.appendLine("Logged in!");
                    }
                });
            }

            @Override
            public void onAccessDenied() {
                getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        txtTumblrLog.appendLine("Access denied");
                        System.exit(-2);
                    }
                });
            }
        });

        btnSaveLog.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String logFileName = new FileDialog(parent.getShell(), SWT.SAVE).open();

                if (logFileName != null) {
                    try {
                        Files.write(Paths.get(logFileName), txtTumblrLog.getText().getBytes(StandardCharsets.UTF_8),
                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // Not used
            }
        });

        btnLogin.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // Not used
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (tumblrClient.isLogged()) {
                    tumblrClient.logout();
                    txtTumblrLog.appendLine("Logged out");
                    btnLogin.setText("Login");
                } else {
                    tumblrClient.login();
                }
            }
        });
    }
}
