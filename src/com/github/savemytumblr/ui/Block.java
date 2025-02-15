package com.github.savemytumblr.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
import com.github.savemytumblr.api.array.CompletionInterface;
import com.github.savemytumblr.blog.array.Blocks;
import com.github.savemytumblr.blog.simple.Info;
import com.github.savemytumblr.exception.BaseException;

public class Block extends TabItem {
    final private TumblrClient tumblrClient;
    final private Set<String> blocked;
    final private LogText txtBlocked;
    final private Label lblBlockedValue;
    final private Button btnCheck;

    public Block(TumblrClient cTumblrClient, TabFolder parent) {
        super("Block", parent);

        this.tumblrClient = cTumblrClient;
        this.blocked = new HashSet<>();

        GridLayout layout = new GridLayout(3, true);
        getComp().setLayout(layout);

        this.lblBlockedValue = new Label(getComp(), SWT.NONE);
        this.lblBlockedValue.setText("Blocked");
        this.lblBlockedValue.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        this.txtBlocked = new LogText(getComp(), false);
        this.txtBlocked.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.btnCheck = new Button(getComp(), SWT.PUSH);
        this.btnCheck.setText("Check");
        this.btnCheck.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 3, 1));

        this.btnCheck.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // Not used
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Block.this.btnCheck.setEnabled(false);

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
                new CompletionInterface<Info.Base, Blocks.Data>() {
                    @Override
                    public void onFailure(BaseException e) {
                        getDisplay().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                Block.this.btnCheck.setEnabled(true);
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

                                Block.this.btnCheck.setEnabled(true);
                            }
                        });
                    }
                });
    }
}
