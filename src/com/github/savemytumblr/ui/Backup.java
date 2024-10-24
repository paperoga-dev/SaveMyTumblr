package com.github.savemytumblr.ui;

import java.nio.file.Paths;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Text;

import com.github.savemytumblr.TumblrClient;

public class Backup extends TabItem {
    private String backupPath = "";
    private com.github.savemytumblr.Backup backup = null;

    public Backup(TumblrClient tumblrClient, TabFolder parent) {
        super("Backup", parent);

        this.backupPath = System.getProperty("user.home");

        GridLayout gridLayout = new GridLayout();
        getComp().setLayout(gridLayout);
        gridLayout.numColumns = 2;

        Label lblBlogName = new Label(getComp(), SWT.NONE);
        lblBlogName.setText("Blog:");
        lblBlogName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

        Text txtBlogName = new Text(getComp(), SWT.SINGLE);
        txtBlogName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label lblBackupPath = new Label(getComp(), SWT.NONE);
        lblBackupPath.setText("Backup path: " + this.backupPath);
        lblBackupPath.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        Button btnBackupPathSelect = new Button(getComp(), SWT.PUSH);
        btnBackupPathSelect.setText("Select");
        btnBackupPathSelect.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

        ProgressBar pbDownload = new ProgressBar(getComp(), SWT.SMOOTH);
        pbDownload.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        LogText txtLog = new LogText(getComp());
        txtLog.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        Button btnBackup = new Button(getComp(), SWT.PUSH);
        btnBackup.setText("Backup");
        btnBackup.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));

        btnBackupPathSelect.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // Not used
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Backup.this.backupPath = new DirectoryDialog(parent.getShell()).open();
                lblBackupPath.setText("Backup path: " + Backup.this.backupPath);
            }
        });

        btnBackup.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // Not used
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (Backup.this.backupPath.isEmpty()) {
                    return;
                }

                if ((Backup.this.backup != null) && Backup.this.backup.isRunning()) {
                    Backup.this.backup.stop();
                    btnBackup.setText("Backup");
                } else {
                    Backup.this.backup = new com.github.savemytumblr.Backup(Paths.get(Backup.this.backupPath),
                            tumblrClient, txtBlogName.getText(), new com.github.savemytumblr.Backup.Progress() {
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

                    Backup.this.backup.start();
                    btnBackup.setText("Stop backup");
                }
            }
        });
    }
}
