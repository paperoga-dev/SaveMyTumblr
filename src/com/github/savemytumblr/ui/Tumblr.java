package com.github.savemytumblr.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.TabFolder;

import com.github.savemytumblr.TumblrClient;
import com.github.savemytumblr.exception.BaseException;

public class Tumblr extends TabItem {

    public Tumblr(TumblrClient tumblrClient, TabFolder parent) {
        super("Tumblr", parent);

        GridLayout gridLayout = new GridLayout(1, false);
        getComp().setLayout(gridLayout);

        LogText txtTumblrLog = new LogText(getComp());
        txtTumblrLog.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Button btnLogin = new Button(getComp(), SWT.PUSH);
        btnLogin.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
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
