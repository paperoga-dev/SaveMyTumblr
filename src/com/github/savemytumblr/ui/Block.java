package com.github.savemytumblr.ui;

import java.util.ArrayList;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;

import com.github.savemytumblr.TumblrClient;
import com.github.savemytumblr.blog.array.Blocks;
import com.github.savemytumblr.blog.simple.Info;
import com.github.savemytumblr.blog.simple.actions.blocks.Add;
import com.github.savemytumblr.blog.simple.actions.blocks.Remove;
import com.github.savemytumblr.exception.BaseException;

public class Block extends TabItem {
    final private TumblrClient tumblrClient;
    final private Set<String> blocked;
    final private LogText txtBlocked;
    final private Label lblBlockedValue;
    final private Button btnCheck;
    final private Button btnTestBlock;
    final private Button btnTestUnblock;
    final private List<Button> buttons;

    public Block(TumblrClient cTumblrClient, TabFolder parent) {
        super("Block", parent);

        this.tumblrClient = cTumblrClient;
        this.blocked = new HashSet<>();
        this.buttons = new ArrayList<>();

        GridLayout layout = new GridLayout(3, true);
        getComp().setLayout(layout);

        this.lblBlockedValue = new Label(getComp(), SWT.NONE);
        this.lblBlockedValue.setText("Blocked");
        this.lblBlockedValue.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        this.txtBlocked = new LogText(getComp(), false);
        this.txtBlocked.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.btnCheck = new Button(getComp(), SWT.PUSH);
        this.buttons.add(this.btnCheck);
        this.btnCheck.setText("Check");
        this.btnCheck.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        this.btnCheck.addSelectionListener(new SelectionListener() {
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

        this.btnTestBlock = new Button(getComp(), SWT.PUSH);
        this.buttons.add(this.btnTestBlock);
        this.btnTestBlock.setText("Test block");
        this.btnTestBlock.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        this.btnTestBlock.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // Not used
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Block.this.enableButtons(false);

                testBlock();
            }
        });

        this.btnTestUnblock = new Button(getComp(), SWT.PUSH);
        this.buttons.add(this.btnTestUnblock);
        this.btnTestUnblock.setText("Test unblock");
        this.btnTestUnblock.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        this.btnTestUnblock.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // Not used
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Block.this.enableButtons(false);

                testUnblock();
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
        this.txtBlocked.clear();
        updateCounter(this.lblBlockedValue, 0);
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
                                Block.this.txtBlocked.appendLines(doSort(Block.this.blocked));
                                updateCounter(Block.this.lblBlockedValue, Block.this.blocked.size());

                                Block.this.enableButtons(true);
                            }
                        });
                    }
                });
    }

    private void testBlock() {
        this.tumblrClient.call(Add.class, this.tumblrClient.getMe().getBlogs().get(0).getName(),
                Map.of("blocked_tumblelog", "resource-thing"),
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
                                Block.this.enableButtons(true);
                            }
                        });
                    }
                });
    }

    private void testUnblock() {
        this.tumblrClient.call(Remove.class, this.tumblrClient.getMe().getBlogs().get(0).getName(),
                Map.of("blocked_tumblelog", "resource-thing"),
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
                                Block.this.enableButtons(true);
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
