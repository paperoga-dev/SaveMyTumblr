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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;

import com.github.savemytumblr.TumblrClient;
import com.github.savemytumblr.api.array.CompletionInterface;
import com.github.savemytumblr.blog.array.Followers;
import com.github.savemytumblr.blog.simple.Info;
import com.github.savemytumblr.exception.BaseException;
import com.github.savemytumblr.user.array.Following;

public class Follow extends TabItem {
    final private TumblrClient tumblrClient;
    final private Set<String> following;
    final private Set<String> followers;
    final private LogText txtFollowing;
    final private LogText txtFollowers;
    final private LogText txtMutuals;
    final private LogText txtFollowingButNotFollowers;
    final private LogText txtFollowersButNotFollowing;
    final private Label lblFollowingValue;
    final private Label lblFollowersValue;
    final private Label lblMutualsValue;
    final private Label lblFollowingButNotFollowersValue;
    final private Label lblFollowersButNotFollowingValue;
    final private Button btnCheck;

    public Follow(TumblrClient cTumblrClient, TabFolder parent) {
        super("Follow", parent);

        this.tumblrClient = cTumblrClient;
        this.following = new HashSet<>();
        this.followers = new HashSet<>();

        getComp().setLayout(new GridLayout(1, false));

        Composite firstRow = new Composite(getComp(), SWT.NONE);
        firstRow.setLayout(new GridLayout(3, true));
        firstRow.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.lblFollowingValue = new Label(firstRow, SWT.NONE);
        this.lblFollowingValue.setText("Following");
        this.lblFollowingValue.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        this.lblFollowersValue = new Label(firstRow, SWT.NONE);
        this.lblFollowersValue.setText("Followers");
        this.lblFollowersValue.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        this.lblMutualsValue = new Label(firstRow, SWT.NONE);
        this.lblMutualsValue.setText("Mutuals");
        this.lblMutualsValue.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        this.txtFollowing = new LogText(firstRow, false);
        this.txtFollowing.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.txtFollowers = new LogText(firstRow, false);
        this.txtFollowers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.txtMutuals = new LogText(firstRow, false);
        this.txtMutuals.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite secondRow = new Composite(getComp(), SWT.NONE);
        secondRow.setLayout(new GridLayout(2, true));
        secondRow.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.lblFollowingButNotFollowersValue = new Label(secondRow, SWT.NONE);
        this.lblFollowingButNotFollowersValue.setText("Following but not followers");
        this.lblFollowingButNotFollowersValue.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        this.lblFollowersButNotFollowingValue = new Label(secondRow, SWT.NONE);
        this.lblFollowersButNotFollowingValue.setText("Followers but not following");
        this.lblFollowersButNotFollowingValue.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        this.txtFollowingButNotFollowers = new LogText(secondRow, false);
        this.txtFollowingButNotFollowers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.txtFollowersButNotFollowing = new LogText(secondRow, false);
        this.txtFollowersButNotFollowing.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite thirdRow = new Composite(getComp(), SWT.NONE);
        thirdRow.setLayout(new GridLayout(1, true));
        thirdRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        this.btnCheck = new Button(thirdRow, SWT.PUSH);
        this.btnCheck.setText("Check");
        this.btnCheck.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

        this.btnCheck.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // Not used
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Follow.this.btnCheck.setEnabled(false);

                clearData();

                fetchFollowing();
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
        this.txtFollowers.clear();
        this.txtFollowing.clear();
        this.txtMutuals.clear();
        this.txtFollowersButNotFollowing.clear();
        this.txtFollowingButNotFollowers.clear();

        updateCounter(this.lblFollowingValue, 0);
        updateCounter(this.lblFollowersValue, 0);
        updateCounter(this.lblMutualsValue, 0);
        updateCounter(this.lblFollowingButNotFollowersValue, 0);
        updateCounter(this.lblFollowersButNotFollowingValue, 0);
    }

    static private void updateCounter(Label label, Integer value) {
        if (label.getData("text") == null) {
            label.setData("text", label.getText());
        }
        label.setText(label.getData("text").toString() + ": " + String.valueOf(value));
        label.pack(true);
    }

    private void fetchFollowing() {
        this.tumblrClient.call(Following.Api.class, 0, -1, new CompletionInterface<Info.Base, Following.Data>() {
            @Override
            public void onFailure(BaseException e) {
                getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        Follow.this.btnCheck.setEnabled(true);
                    }
                });
            }

            @Override
            public void onSuccess(List<Info.Base> result, Integer offset, Integer limit, int count) {
                for (Info.Base blog : result) {
                    Follow.this.following.add(blog.getName());
                }

                getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        Follow.this.txtFollowing.appendLines(doSort(Follow.this.following));
                        updateCounter(Follow.this.lblFollowingValue, Follow.this.following.size());

                        fetchFollowers();
                    }
                });
            }
        });
    }

    private void fetchFollowers() {
        this.tumblrClient.call(Followers.Api.class, this.tumblrClient.getMe().getBlogs().get(0).getName(), 0, -1,
                new CompletionInterface<Followers.User, Followers.Data>() {
                    @Override
                    public void onFailure(BaseException e) {
                        getDisplay().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                Follow.this.btnCheck.setEnabled(true);
                            }
                        });
                    }

                    @Override
                    public void onSuccess(List<Followers.User> result, Integer offset, Integer limit, int count) {
                        for (Followers.User user : result) {
                            Follow.this.followers.add(user.getName());
                        }

                        getDisplay().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                Follow.this.txtFollowers.appendLines(doSort(Follow.this.followers));
                                updateCounter(Follow.this.lblFollowersValue, Follow.this.followers.size());

                                Set<String> mutuals = new HashSet<>(Follow.this.followers);
                                mutuals.retainAll(Follow.this.following);
                                Follow.this.txtMutuals.appendLines(doSort(mutuals));
                                updateCounter(Follow.this.lblMutualsValue, mutuals.size());

                                Set<String> followingButNotFollowers = new HashSet<>();
                                followingButNotFollowers.addAll(Follow.this.following);
                                followingButNotFollowers.removeAll(Follow.this.followers);

                                Set<String> followersButNotFollowing = new HashSet<>();
                                followersButNotFollowing.addAll(Follow.this.followers);
                                followersButNotFollowing.removeAll(Follow.this.following);

                                Follow.this.txtFollowersButNotFollowing.appendLines(doSort(followersButNotFollowing));
                                updateCounter(Follow.this.lblFollowersButNotFollowingValue,
                                        followersButNotFollowing.size());
                                Follow.this.txtFollowingButNotFollowers.appendLines(doSort(followingButNotFollowers));
                                updateCounter(Follow.this.lblFollowingButNotFollowersValue,
                                        followingButNotFollowers.size());

                                Follow.this.btnCheck.setEnabled(true);
                            }
                        });
                    }
                });
    }
}
