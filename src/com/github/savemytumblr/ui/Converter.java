package com.github.savemytumblr.ui;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutorService;

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
import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.exception.RuntimeException;

public class Converter extends TabItem {
    private String path = "";
    private int okPosts = 0;
    private int totalPosts = 0;

    public Converter(TabFolder parent, ExecutorService executor) {
        super("Converter", parent);

        GridLayout gridLayout = new GridLayout();
        getComp().setLayout(gridLayout);
        gridLayout.numColumns = 2;

        Label lblPath = new Label(getComp(), SWT.NONE);
        lblPath.setText("Path:");
        lblPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button btnPathSelect = new Button(getComp(), SWT.PUSH);
        btnPathSelect.setText("Select");

        ProgressBar pbDownload = new ProgressBar(getComp(), SWT.INDETERMINATE);
        pbDownload.setVisible(false);
        pbDownload.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        LogText txtLog = new LogText(getComp());
        txtLog.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        Button btnConvert = new Button(getComp(), SWT.PUSH);
        btnConvert.setText("Convert");
        btnConvert.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 2, 1));

        btnPathSelect.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // Not used
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Converter.this.path = new DirectoryDialog(parent.getShell()).open();
                lblPath.setText("Path: " + Converter.this.path);
            }
        });

        btnConvert.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // Not used
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (Converter.this.path.isEmpty()) {
                    return;
                }

                Converter.this.okPosts = Converter.this.totalPosts = 0;

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        getDisplay().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                pbDownload.setVisible(true);
                            }
                        });

                        try {
                            Files.walkFileTree(Paths.get(Converter.this.path), new SimpleFileVisitor<Path>() {
                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                        throws IOException {
                                    String[] fileParts = file.getFileName().toString().split("\\.");

                                    if ((fileParts.length != 2) || !fileParts[1].equalsIgnoreCase("json")) {
                                        return FileVisitResult.CONTINUE;
                                    }

                                    try {
                                        Long.parseLong(fileParts[0]);

                                        ++Converter.this.totalPosts;

                                        getDisplay().asyncExec(new Runnable() {
                                            @Override
                                            public void run() {
                                                txtLog.appendLine("Convert " + fileParts[0] + "...");
                                            }
                                        });

                                        try (FileOutputStream outputStream = new FileOutputStream(Paths
                                                .get(file.getParent().toString(), fileParts[0] + ".html").toString());
                                                BufferedWriter writer = new BufferedWriter(
                                                        new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {

                                            writer.write(new com.github.savemytumblr.posts.Post.Item(
                                                    new JSONObject(Files.readString(file, StandardCharsets.UTF_8)))
                                                    .toHTML(file.getParent().toString(), fileParts[0]).toString());
                                        }

                                        ++Converter.this.okPosts;

                                    } catch (@SuppressWarnings("unused") NumberFormatException e) {
                                        // Not used
                                    } catch (JSONException | RuntimeException | IOException e) {
                                        e.printStackTrace();

                                        getDisplay().asyncExec(new Runnable() {
                                            @Override
                                            public void run() {
                                                txtLog.appendLine("\tBackup " + fileParts[0] + " failed");
                                            }
                                        });
                                    }

                                    return FileVisitResult.CONTINUE;
                                }
                            });
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        getDisplay().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                pbDownload.setVisible(false);

                                txtLog.appendLine("");
                                txtLog.appendLine("Converted " + String.valueOf(Converter.this.okPosts) + "/"
                                        + String.valueOf(Converter.this.totalPosts) + ", "
                                        + String.valueOf(Converter.this.totalPosts - Converter.this.okPosts)
                                        + " failed");
                            }
                        });
                    }
                });
            }
        });
    }
}
