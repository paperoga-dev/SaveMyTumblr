package com.github.savemytumblr.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
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
    final private Button btnCheck;

    public Follow(TumblrClient tumblrClient, TabFolder parent) {
        super("Follow", parent);

        this.tumblrClient = tumblrClient;
        this.following = new HashSet<>();
        this.followers = new HashSet<>();

        FillLayout fillLayout = new FillLayout(SWT.VERTICAL);
        getComp().setLayout(fillLayout);

        Composite firstRow = new Composite(getComp(), SWT.NONE);
        GridLayout firstRowLayout = new GridLayout();
        firstRow.setLayout(firstRowLayout);
        firstRowLayout.numColumns = 3;

        Label lblFollowingName = new Label(firstRow, SWT.NONE);
        lblFollowingName.setText("Following:");
        lblFollowingName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        Label lblFollowersName = new Label(firstRow, SWT.NONE);
        lblFollowersName.setText("Followers:");
        lblFollowersName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        Label lblMutualsName = new Label(firstRow, SWT.NONE);
        lblMutualsName.setText("Mutuals:");
        lblMutualsName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        this.txtFollowing = new LogText(firstRow, false);
        this.txtFollowing.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.txtFollowers = new LogText(firstRow, false);
        this.txtFollowers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.txtMutuals = new LogText(firstRow, false);
        this.txtMutuals.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite secondRow = new Composite(getComp(), SWT.NONE);
        GridLayout secondRowLayout = new GridLayout();
        secondRow.setLayout(secondRowLayout);
        secondRowLayout.numColumns = 2;

        Label lblFollowingButNotFollowersName = new Label(secondRow, SWT.NONE);
        lblFollowingButNotFollowersName.setText("Following but not followers:");
        lblFollowingButNotFollowersName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        Label lblFollowersButNotFollowingName = new Label(secondRow, SWT.NONE);
        lblFollowersButNotFollowingName.setText("Followers but not following:");
        lblFollowersButNotFollowingName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        this.txtFollowingButNotFollowers = new LogText(secondRow, false);
        this.txtFollowingButNotFollowers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.txtFollowersButNotFollowing = new LogText(secondRow, false);
        this.txtFollowersButNotFollowing.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.btnCheck = new Button(secondRow, SWT.PUSH);
        this.btnCheck.setText("Check");
        this.btnCheck.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));

        this.btnCheck.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // Not used
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Follow.this.btnCheck.setEnabled(false);

                Follow.this.txtFollowers.clear();
                Follow.this.txtFollowing.clear();
                Follow.this.txtMutuals.clear();
                Follow.this.txtFollowersButNotFollowing.clear();
                Follow.this.txtFollowingButNotFollowers.clear();

                fetchFollowing();
            }
        });
    }

    private static List<String> doSort(Collection<String> in) {
        final List<String> out = new ArrayList<>(in);
        out.sort(null);
        return out;
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

                                Set<String> mutuals = new HashSet<>(Follow.this.followers);
                                mutuals.retainAll(Follow.this.following);
                                Follow.this.txtMutuals.appendLines(doSort(mutuals));

                                Set<String> followingButNotFollowers = new HashSet<>();
                                followingButNotFollowers.addAll(Follow.this.following);
                                followingButNotFollowers.removeAll(Follow.this.followers);

                                Set<String> followersButNotFollowing = new HashSet<>();
                                followersButNotFollowing.addAll(Follow.this.followers);
                                followersButNotFollowing.removeAll(Follow.this.following);

                                Follow.this.txtFollowersButNotFollowing.appendLines(doSort(followersButNotFollowing));
                                Follow.this.txtFollowingButNotFollowers.appendLines(doSort(followingButNotFollowers));

                                Follow.this.btnCheck.setEnabled(true);
                            }
                        });
                    }
                });

    }
}
