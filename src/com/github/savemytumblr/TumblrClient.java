/*
 * SaveMyTumblr
 * Copyright (C) 2020

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.savemytumblr;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.savemytumblr.api.AuthInterface;
import com.github.savemytumblr.api.Authenticate;
import com.github.savemytumblr.api.array.ContentInterface;
import com.github.savemytumblr.api.simple.CompletionInterface;
import com.github.savemytumblr.exception.BaseException;
import com.github.savemytumblr.exception.NetworkException;
import com.github.savemytumblr.user.simple.Info;

public final class TumblrClient {

    public interface Executor {
        void execute(Runnable runnable);
    }

    public interface Logger {
        void info(String msg);

        void warning(String msg);

        void error(String msg);
    }

    public interface Storage {
        boolean has(String key);

        String get(String key, String defValue);

        void put(String key, String value);

        void remove(String key);
    }

    public interface OnLoginListener {
        void onAccessGranted();

        void onAccessRequest(Authenticate authenticator, String authenticationUrl, String state);

        void onAccessDenied();

        void onLoginFailure(BaseException e);
    }

    private String appName;
    private String appVersion;

    private AccessToken accessToken;
    private OnLoginListener onLoginListener;

    private Executor executor;
    private Logger logger;
    private Storage storage;

    private Info.Data me;

    public TumblrClient(Executor executor, Logger logger, Storage storage) {
        super();

        this.executor = executor;
        this.logger = logger;
        this.storage = storage;

        onLoginListener = null;

        this.me = null;

        this.appName = Constants.APP_NAME;
        this.appVersion = "0.0.1";
    }

    protected Executor getExecutor() {
        return executor;
    }

    private Logger getLogger() {
        return logger;
    }

    private void doLogin() {
        final Authenticate auth = new Authenticate(getExecutor(), getLogger());

        auth.setOnAuthenticationListener(new Authenticate.OnAuthenticationListener() {
            @Override
            public void onAuthenticationRequest(String authenticationUrl, String state) {
                onLoginListener.onAccessRequest(auth, authenticationUrl, state);
            }

            @Override
            public void onAuthenticationGranted(AccessToken accessToken) {
                // redo user request, this time should work
                login(accessToken);
            }

            @Override
            public void onFailure(Exception exception) {
                onLoginListener.onAccessDenied();
            }
        });
        auth.request();
    }

    private AuthInterface makeAuthInterface() {
        return new AuthInterface() {
            @Override
            public AccessToken getAccessToken() {
                return accessToken;
            }

            @Override
            public String getUserAgent() {
                return appName + "/" + appVersion;
            }

            @Override
            public void onUpdateToken(AccessToken aToken) {
                logger.info("Stored Access Token: " + aToken.toJSON().toString());
                accessToken = aToken;
                storage.put(Constants.TOKEN, accessToken.toJSON().toString());
            }

            @Override
            public void onClearToken() {
                accessToken = null;
                storage.remove(Constants.TOKEN);
            }
        };
    }

    private void login(AccessToken aToken) {
        this.accessToken = aToken;

        getExecutor().execute(new Info.Api().call(getExecutor(), getLogger(), new HashMap<>(), makeAuthInterface(),
                new CompletionInterface<Info.Data>() {
                    @Override
                    public void onSuccess(Info.Data result) {
                        me = result;

                        if (onLoginListener != null) {
                            onLoginListener.onAccessGranted();
                        }

                        storage.put(Constants.TOKEN, accessToken.toJSON().toString());
                    }

                    @Override
                    public void onFailure(BaseException e) {
                        if (!(e instanceof NetworkException)) {
                            // we can reach Tumblr, but we cannot access it. So, throw away our tokens, and
                            // let's ask a new authentication
                            logger.warning("Auth token not valid");

                            logout();
                        }

                        // else we cannot reach Tumblr, fail but do not remove our tokens

                        if (onLoginListener != null) {
                            onLoginListener.onLoginFailure(e);
                        }
                    }
                }));
    }

    public void login() {
        if (onLoginListener == null) {
            throw new NullPointerException("onLoginListener is null");
        }

        if (storage.has(Constants.TOKEN)) {

            // ok, we already have authentication tokens, let's try them first
            accessToken = AccessToken.fromJSON(storage.get(Constants.TOKEN, ""));
            logger.info("Stored Access Token: " + accessToken.toJSON().toString());

            login(accessToken);
        } else {
            // never logged in before, do that
            doLogin();
        }
    }

    public void logout() {
        storage.remove(Constants.TOKEN);
        accessToken = null;
    }

    /* **** SINGLE ITEM API CALL **** */
    private <T> void doCall(final com.github.savemytumblr.api.simple.ApiInterface<T> obj,
            final Map<String, String> queryParams,
            final com.github.savemytumblr.api.simple.CompletionInterface<T> onCompletion) {
        if (accessToken == null) {
            if (onLoginListener != null) {
                onLoginListener.onLoginFailure(new com.github.savemytumblr.exception.RuntimeException("Not logged"));
            }

            return;
        }

        getExecutor().execute(obj.call(getExecutor(), getLogger(), queryParams, makeAuthInterface(), onCompletion));
    }
    /* **** SINGLE ITEM API CALL **** */

    /* **** ARRAY BASED API CALL **** */
    private <T, W extends ContentInterface<T>> void doCall(final List<T> resultList,
            final com.github.savemytumblr.api.array.ApiInterface<T, W> obj, final Integer offset, final Integer limit,
            final Map<String, String> queryParams,
            final com.github.savemytumblr.api.array.CompletionInterface<T, W> onCompletion) {

        if (accessToken == null) {
            if (onLoginListener != null) {
                onLoginListener.onLoginFailure(new com.github.savemytumblr.exception.RuntimeException("Not logged"));
            }

            return;
        }

        int newLimit = (limit == -1) ? 20 : Math.min(20, limit);

        getExecutor().execute(obj.call(getExecutor(), getLogger(), resultList, queryParams, makeAuthInterface(), offset,
                newLimit, new com.github.savemytumblr.api.array.CompletionInterface<T, W>() {

                    @Override
                    public void onSuccess(List<T> result, int offset, int limit, int count) {
                        resultList.addAll(result);
                        if (result.isEmpty()) {
                            // This is a Tumblr bug, some array-based APIs return less items
                            // than expected. In this case, we return earlier, with the real
                            // number of items.

                            if (onCompletion != null) {
                                onCompletion.onSuccess(resultList, obj.getOffset(), obj.getLimit(), resultList.size());
                            }
                            return;
                        }

                        int newOffset = offset + resultList.size();
                        int newLimit;

                        if (count == -1) {
                            // cannot get the end of the list

                            if (obj.getLimit() == -1) {
                                // the caller did not specify a limit, so the first content
                                // is fine, we're done
                                if (onCompletion != null) {
                                    onCompletion.onSuccess(resultList, obj.getOffset(), obj.getLimit(),
                                            resultList.size());
                                }
                                return;
                            } else {
                                if (resultList.size() >= obj.getLimit()) {
                                    // fetched enough content, we're done
                                    if (onCompletion != null) {
                                        onCompletion.onSuccess(resultList, obj.getOffset(), obj.getLimit(),
                                                resultList.size());
                                    }
                                    return;
                                }

                                newLimit = obj.getLimit() - resultList.size();
                            }

                        } else {

                            newLimit = ((obj.getLimit() == -1) ? count : obj.getLimit()) - resultList.size();

                            if (newLimit <= 0) {
                                // fetched enough content, we're done
                                if (onCompletion != null) {
                                    onCompletion.onSuccess(resultList, obj.getOffset(), obj.getLimit(),
                                            resultList.size());
                                }
                                return;
                            }
                        }

                        doCall(resultList, obj, newOffset, newLimit, queryParams, onCompletion);
                    }

                    @Override
                    public void onFailure(BaseException e) {
                        onCompletion.onFailure(e);
                    }
                }));
    }
    /* **** ARRAY BASED API CALL **** */

    /* **** USER BASED API CALLS **** */
    public <T> void call(final Class<? extends com.github.savemytumblr.api.simple.ApiInterface<T>> clazz,
            final Map<String, String> queryParams,
            final com.github.savemytumblr.api.simple.CompletionInterface<T> onCompletion) {

        Class<?>[] cArg = new Class<?>[] {};

        try {
            doCall(clazz.getDeclaredConstructor(cArg).newInstance(), queryParams, onCompletion);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public <T> void call(final Class<? extends com.github.savemytumblr.api.simple.ApiInterface<T>> clazz,
            final CompletionInterface<T> onCompletion) {
        call(clazz, new HashMap<>(), onCompletion);
    }

    public <T, W extends ContentInterface<T>> void call(
            final Class<? extends com.github.savemytumblr.api.array.ApiInterface<T, W>> clazz, final int offset,
            final int limit, final Map<String, String> queryParams,
            final com.github.savemytumblr.api.array.CompletionInterface<T, W> onCompletion) {

        Class<?>[] cArg = new Class<?>[] { Integer.class, Integer.class };

        try {
            doCall(new ArrayList<T>(), clazz.getDeclaredConstructor(cArg).newInstance(offset, limit), offset, limit,
                    queryParams, onCompletion);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public <T, W extends ContentInterface<T>> void call(
            final Class<? extends com.github.savemytumblr.api.array.ApiInterface<T, W>> clazz, final int offset,
            final Map<String, String> queryParams,
            final com.github.savemytumblr.api.array.CompletionInterface<T, W> onCompletion) {
        call(clazz, offset, 20, queryParams, onCompletion);
    }

    public <T, W extends ContentInterface<T>> void call(
            final Class<? extends com.github.savemytumblr.api.array.ApiInterface<T, W>> clazz, final int offset,
            final int limit, final com.github.savemytumblr.api.array.CompletionInterface<T, W> onCompletion) {
        call(clazz, offset, limit, new HashMap<>(), onCompletion);
    }

    public <T, W extends ContentInterface<T>> void call(
            final Class<? extends com.github.savemytumblr.api.array.ApiInterface<T, W>> clazz, final int offset,
            final com.github.savemytumblr.api.array.CompletionInterface<T, W> onCompletion) {
        call(clazz, offset, 20, new HashMap<>(), onCompletion);
    }
    /* **** USER BASED API CALLS **** */

    /* **** BLOG BASED API CALLS **** */
    public <T> void call(final Class<? extends com.github.savemytumblr.blog.simple.ApiInterface<T>> clazz,
            final String blogId, final Map<String, String> queryParams, final CompletionInterface<T> onCompletion) {

        Class<?>[] cArg = new Class<?>[] { String.class };

        try {
            doCall(clazz.getDeclaredConstructor(cArg).newInstance(blogId), queryParams, onCompletion);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public <T> void call(final Class<? extends com.github.savemytumblr.blog.simple.ApiInterface<T>> clazz,
            final String blogId, final CompletionInterface<T> onCompletion) {
        call(clazz, blogId, new HashMap<>(), onCompletion);
    }

    public <T, W extends ContentInterface<T>> void call(
            final Class<? extends com.github.savemytumblr.blog.array.ApiInterface<T, W>> clazz, final String blogId,
            final int offset, final int limit, final Map<String, String> queryParams,
            final com.github.savemytumblr.api.array.CompletionInterface<T, W> onCompletion) {

        Class<?>[] cArg = new Class<?>[] { Integer.class, Integer.class, String.class };

        try {
            doCall(new ArrayList<T>(), clazz.getDeclaredConstructor(cArg).newInstance(offset, limit, blogId), offset,
                    limit, queryParams, onCompletion);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public <T, W extends ContentInterface<T>> void call(
            final Class<? extends com.github.savemytumblr.blog.array.ApiInterface<T, W>> clazz, final String blogId,
            final int offset, final Map<String, String> queryParams,
            final com.github.savemytumblr.api.array.CompletionInterface<T, W> onCompletion) {
        call(clazz, blogId, offset, 20, queryParams, onCompletion);
    }

    public <T, W extends ContentInterface<T>> void call(
            final Class<? extends com.github.savemytumblr.blog.array.ApiInterface<T, W>> clazz, final String blogId,
            final int offset, final int limit,
            final com.github.savemytumblr.api.array.CompletionInterface<T, W> onCompletion) {
        call(clazz, blogId, offset, limit, new HashMap<>(), onCompletion);
    }

    public <T, W extends ContentInterface<T>> void call(
            final Class<? extends com.github.savemytumblr.blog.array.ApiInterface<T, W>> clazz, final String blogId,
            final int offset, final com.github.savemytumblr.api.array.CompletionInterface<T, W> onCompletion) {
        call(clazz, blogId, offset, 20, new HashMap<>(), onCompletion);
    }
    /* **** BLOG BASED API CALLS **** */

    /* **** POST BASED API CALLS **** */
    public <T> void call(final Class<? extends com.github.savemytumblr.blog.simple.ApiInterface<T>> clazz,
            final String blogId, final String postId, final Map<String, String> queryParams,
            final CompletionInterface<T> onCompletion) {

        Class<?>[] cArg = new Class<?>[] { String.class, String.class };

        try {
            doCall(clazz.getDeclaredConstructor(cArg).newInstance(blogId, postId), queryParams, onCompletion);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public <T> void call(final Class<? extends com.github.savemytumblr.blog.simple.ApiInterface<T>> clazz,
            final String blogId, final String postId, final CompletionInterface<T> onCompletion) {
        call(clazz, blogId, postId, new HashMap<>(), onCompletion);
    }
    /* **** POST BASED API CALLS **** */

    public void setOnLoginListener(OnLoginListener onLoginListener) {
        this.onLoginListener = onLoginListener;
    }

    public Info.Data getMe() {
        return me;
    }

    public boolean isLogged() {
        return accessToken != null;
    }
}
