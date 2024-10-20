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
import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.LogText;
import com.github.savemytumblr.exception.RuntimeException;

public class Converter extends TabItem {
    private String path = "";
    private int okPosts = 0;
    private int totalPosts = 0;

    public Converter(TabFolder parent, Preferences prefs, ExecutorService executor) {
        super(parent, SWT.BORDER);

        Composite comp = new Composite(parent, SWT.BORDER);

        GridLayout gridLayout = new GridLayout();
        comp.setLayout(gridLayout);
        gridLayout.numColumns = 2;

        Label lblPath = new Label(comp, SWT.NONE);
        lblPath.setText("Path:");
        lblPath.setLayoutData(new GridData(GridData.FILL, GridData.VERTICAL_ALIGN_CENTER, true, false));

        Button btnPathSelect = new Button(comp, SWT.PUSH);
        btnPathSelect.setText("Select");

        ProgressBar pbDownload = new ProgressBar(comp, SWT.INDETERMINATE);
        pbDownload.setVisible(false);
        pbDownload.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));

        LogText txtLog = new LogText(comp);
        txtLog.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1));

        Button btnConvert = new Button(comp, SWT.PUSH);
        btnConvert.setText("Convert");
        btnConvert.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        btnPathSelect.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                path = new DirectoryDialog(parent.getShell()).open();
                lblPath.setText("Path: " + path);
            }
        });

        btnConvert.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (path.isEmpty()) {
                    return;
                }

                okPosts = totalPosts = 0;

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
                            Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {

                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                        throws IOException {
                                    String[] fileParts = file.getFileName().toString().split("\\.");

                                    if ((fileParts.length != 2) || !fileParts[1].equalsIgnoreCase("json")) {
                                        return FileVisitResult.CONTINUE;
                                    }

                                    try {
                                        Long.parseLong(fileParts[0]);

                                        ++totalPosts;

                                        getDisplay().asyncExec(new Runnable() {
                                            @Override
                                            public void run() {
                                                txtLog.appendLine("Convert " + fileParts[0] + "...");
                                            }
                                        });

                                        FileOutputStream outputStream = new FileOutputStream(Paths
                                                .get(file.getParent().toString(), fileParts[0] + ".html").toString());
                                        BufferedWriter writer = new BufferedWriter(
                                                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

                                        writer.write(new com.github.savemytumblr.posts.Post.Item(
                                                new JSONObject(Files.readString(file, StandardCharsets.UTF_8)))
                                                .toHTML(file.getParent().toString(), fileParts[0]).toString());
                                        writer.close();

                                        ++okPosts;

                                    } catch (NumberFormatException e) {

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
                                txtLog.appendLine(
                                        "Converted " + String.valueOf(okPosts) + "/" + String.valueOf(totalPosts) + ", "
                                                + String.valueOf(totalPosts - okPosts) + " failed");
                            }
                        });
                    }
                });
            }
        });

        setControl(comp);
        setText("Converter");
    }

    @Override
    protected void checkSubclass() {
        // TODO Auto-generated method stub
    }
}
