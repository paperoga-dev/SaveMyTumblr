package com.github.savemytumblr.ui;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.json.JSONObject;

import com.github.savemytumblr.TumblrClient;
import com.github.savemytumblr.blog.array.Blocks;
import com.github.savemytumblr.blog.simple.Info;
import com.github.savemytumblr.blog.simple.actions.blocks.Add;
import com.github.savemytumblr.blog.simple.actions.blocks.Remove;
import com.github.savemytumblr.exception.BaseException;

public class Block extends TabItem {
    final private TumblrClient tumblrClient;
    final private Set<String> blocked;
    final private org.eclipse.swt.widgets.List lstBlocked;
    final private Label lblBlockedValue;
    final private org.eclipse.swt.widgets.List lstUnblocked;
    final private Label lblUnblockedValue;
    final private Button btnLoad;
    final private Button btnLoadFrom;
    final private Button btnSaveTo;
    final private Button btnToUnblock;
    final private Button btnToBlock;
    final private List<Button> buttons;

    public Block(TumblrClient cTumblrClient, TabFolder parent) {
        super("Block", parent);

        this.tumblrClient = cTumblrClient;
        this.blocked = new HashSet<>();
        this.buttons = new ArrayList<>();

        GridLayout layout = new GridLayout(1, false);
        getComp().setLayout(layout);

        Composite mainPanel = new Composite(getComp(), SWT.NONE);
        mainPanel.setLayout(new GridLayout(3, false));
        mainPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite firstColumn = new Composite(mainPanel, SWT.NONE);
        firstColumn.setLayout(new GridLayout(1, false));
        firstColumn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.lblBlockedValue = new Label(firstColumn, SWT.NONE);
        this.lblBlockedValue.setText("Blocked");
        this.lblBlockedValue.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        this.lstBlocked = new org.eclipse.swt.widgets.List(firstColumn, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        this.lstBlocked.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite secondColumn = new Composite(mainPanel, SWT.NONE);
        secondColumn.setLayout(new GridLayout(1, false));
        secondColumn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

        this.btnToBlock = new Button(secondColumn, SWT.PUSH);
        this.btnToBlock.setText("Block");
        this.btnToBlock.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        this.buttons.add(this.btnToBlock);

        this.btnToBlock.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // Not used
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Block.this.enableButtons(false);

                Block.this.blockAll(Arrays.asList(Block.this.lstUnblocked.getItems()));
            }
        });

        this.btnToUnblock = new Button(secondColumn, SWT.PUSH);
        this.btnToUnblock.setText("Unblock");
        this.btnToUnblock.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        this.buttons.add(this.btnToUnblock);

        this.btnToUnblock.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // Not used
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Block.this.enableButtons(false);

                Block.this.unblockAll(Arrays.asList(Block.this.lstBlocked.getItems()));
            }
        });

        Composite thirdColumn = new Composite(mainPanel, SWT.NONE);
        thirdColumn.setLayout(new GridLayout(1, false));
        thirdColumn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.lblUnblockedValue = new Label(thirdColumn, SWT.NONE);
        this.lblUnblockedValue.setText("Unblocked");
        this.lblUnblockedValue.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        this.lstUnblocked = new org.eclipse.swt.widgets.List(thirdColumn, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        this.lstUnblocked.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite buttonsPanel = new Composite(getComp(), SWT.NONE);
        buttonsPanel.setLayout(new GridLayout(4, false));
        buttonsPanel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label lblSpacer = new Label(buttonsPanel, SWT.NONE);
        lblSpacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        this.btnLoadFrom = new Button(buttonsPanel, SWT.PUSH);
        this.btnLoadFrom.setText("Load from...");
        this.btnLoadFrom.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        this.buttons.add(this.btnLoadFrom);

        this.btnLoadFrom.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // Not used
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                String fileName = new FileDialog(parent.getShell(), SWT.OPEN).open();
                if (fileName.isEmpty()) {
                    return;
                }

                try {
                    String content = new String(Files.readAllBytes(Paths.get(fileName)));
                    JSONObject jsonObject = new JSONObject(content);

                    Block.this.lstBlocked.setItems(jsonObject.getJSONArray("blocked").toList().toArray(new String[0]));
                    Block.this.lstUnblocked
                            .setItems(jsonObject.getJSONArray("unblocked").toList().toArray(new String[0]));

                    updateCounter(Block.this.lblBlockedValue, Block.this.lstBlocked.getItemCount());
                    updateCounter(Block.this.lblUnblockedValue, Block.this.lstUnblocked.getItemCount());

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        this.btnSaveTo = new Button(buttonsPanel, SWT.PUSH);
        this.btnSaveTo.setText("Save to...");
        this.btnSaveTo.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        this.buttons.add(this.btnSaveTo);

        this.btnSaveTo.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // Not used
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                String fileName = new FileDialog(parent.getShell(), SWT.SAVE).open();
                if (fileName.isEmpty()) {
                    return;
                }

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("blocked", Block.this.lstBlocked.getItems());
                jsonObject.put("unblocked", Block.this.lstUnblocked.getItems());

                try (FileWriter file = new FileWriter(fileName)) {
                    file.write(jsonObject.toString(2));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        this.btnLoad = new Button(buttonsPanel, SWT.PUSH);
        this.btnLoad.setText("Load");
        this.btnLoad.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        this.buttons.add(this.btnLoad);

        this.btnLoad.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // Not used
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Block.this.enableButtons(false);

                clearData();

                fetchBlocked();
            }
        });

        clearData();
    }

    private static List<String> doSort(Collection<String> in) {
        final List<String> out = new ArrayList<>(in);
        out.sort(null);
        return out;
    }

    private void clearData() {
        this.lstBlocked.removeAll();
        this.lstUnblocked.removeAll();
        this.blocked.clear();
        updateCounter(this.lblBlockedValue, 0);
        updateCounter(this.lblUnblockedValue, 0);
    }

    static private void updateCounter(Label label, Integer value) {
        if (label.getData("text") == null) {
            label.setData("text", label.getText());
        }
        label.setText(label.getData("text").toString() + ": " + String.valueOf(value));
        label.pack(true);
    }

    private void fetchBlocked() {
        this.tumblrClient.call(Blocks.Api.class, this.tumblrClient.getMe().getBlogs().get(0).getName(), 0, -1,
                new com.github.savemytumblr.api.array.CompletionInterface<Info.Base, Blocks.Data>() {
                    @Override
                    public void onFailure(BaseException e) {
                        getDisplay().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                Block.this.enableButtons(true);
                            }
                        });
                    }

                    @Override
                    public void onSuccess(List<Info.Base> result, Integer offset, Integer limit, int count) {
                        for (Info.Base blog : result) {
                            Block.this.blocked.add(blog.getName());
                        }

                        getDisplay().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                Block.this.lstBlocked.setItems(doSort(Block.this.blocked).toArray(new String[0]));
                                updateCounter(Block.this.lblBlockedValue, Block.this.lstBlocked.getItemCount());
                                updateCounter(Block.this.lblUnblockedValue, Block.this.lstUnblocked.getItemCount());

                                Block.this.enableButtons(true);
                            }
                        });
                    }
                });
    }

    private void blockAll(List<String> blogs) {
        if (blogs.isEmpty()) {
            Block.this.enableButtons(true);
            updateCounter(Block.this.lblBlockedValue, Block.this.lstBlocked.getItemCount());
            updateCounter(Block.this.lblUnblockedValue, Block.this.lstUnblocked.getItemCount());
            return;
        }

        this.tumblrClient.call(Add.class, this.tumblrClient.getMe().getBlogs().get(0).getName(),
                Map.of("blocked_tumblelog", blogs.getFirst()),
                new com.github.savemytumblr.api.actions.CompletionInterface() {
                    @Override
                    public void onFailure(BaseException e) {
                        getDisplay().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                Block.this.enableButtons(true);
                            }
                        });
                    }

                    @Override
                    public void onSuccess() {
                        getDisplay().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                Block.this.lstBlocked.add(blogs.getFirst());
                                Block.this.lstUnblocked.remove(blogs.getFirst());
                                Block.this.blockAll(blogs.subList(1, blogs.size()));
                            }
                        });
                    }
                });
    }

    private void unblockAll(List<String> blogs) {
        if (blogs.isEmpty()) {
            Block.this.enableButtons(true);
            updateCounter(Block.this.lblBlockedValue, Block.this.lstBlocked.getItemCount());
            updateCounter(Block.this.lblUnblockedValue, Block.this.lstUnblocked.getItemCount());
            return;
        }

        this.tumblrClient.call(Remove.class, this.tumblrClient.getMe().getBlogs().get(0).getName(),
                Map.of("blocked_tumblelog", blogs.getFirst()),
                new com.github.savemytumblr.api.actions.CompletionInterface() {
                    @Override
                    public void onFailure(BaseException e) {
                        getDisplay().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                Block.this.enableButtons(true);
                            }
                        });
                    }

                    @Override
                    public void onSuccess() {
                        getDisplay().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                Block.this.lstUnblocked.add(blogs.getFirst());
                                Block.this.lstBlocked.remove(blogs.getFirst());
                                Block.this.unblockAll(blogs.subList(1, blogs.size()));
                            }
                        });
                    }
                });
    }

    private void enableButtons(boolean enabled) {
        for (Button btn : Block.this.buttons) {
            btn.setEnabled(enabled);
        }
    }
}
