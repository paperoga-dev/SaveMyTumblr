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

    public interface LoginListener {
        void onAccessGranted();

        void onAccessDenied();

        void onLoginFailure(BaseException e);
    }

    public interface OnLoginAction {
        void onAccessRequest(Authenticate authenticator, String authenticationUrl, String state);
    }

    private String appName;
    private String appVersion;

    private AccessToken accessToken;
    private List<LoginListener> loginListeners;
    private OnLoginAction loginAction;

    private Executor executor;
    private List<Logger> loggers;
    private Storage storage;

    private Info.Data me;

    public TumblrClient(Executor cExecutor, Storage cStorage, OnLoginAction iLoginAction) {
        super();

        this.executor = cExecutor;
        this.loggers = new ArrayList<>();
        this.storage = cStorage;

        this.loginAction = iLoginAction;
        this.loginListeners = new ArrayList<>();

        this.me = null;

        this.appName = Constants.APP_NAME;
        this.appVersion = "0.0.1";
    }

    protected Executor getExecutor() {
        return this.executor;
    }

    private Logger getLogger() {
        return new Logger() {
            @Override
            public void info(String msg) {
                for (final Logger logger : TumblrClient.this.loggers) {
                    logger.info(msg);
                }
            }

            @Override
            public void warning(String msg) {
                for (final Logger logger : TumblrClient.this.loggers) {
                    logger.warning(msg);
                }
            }

            @Override
            public void error(String msg) {
                for (final Logger logger : TumblrClient.this.loggers) {
                    logger.error(msg);
                }
            }
        };
    }

    private void doLogin() {
        final Authenticate auth = new Authenticate(getExecutor(), getLogger());

        auth.setOnAuthenticationListener(new Authenticate.OnAuthenticationListener() {
            @Override
            public void onAuthenticationRequest(String authenticationUrl, String state) {
                TumblrClient.this.loginAction.onAccessRequest(auth, authenticationUrl, state);
            }

            @Override
            public void onAuthenticationGranted(AccessToken cAccessToken) {
                // redo user request, this time should work
                login(cAccessToken);
            }

            @Override
            public void onFailure(Exception exception) {
                for (final LoginListener listener : TumblrClient.this.loginListeners) {
                    listener.onAccessDenied();
                }
            }
        });

        auth.request();
    }

    private AuthInterface makeAuthInterface() {
        return new AuthInterface() {
            @Override
            public AccessToken getAccessToken() {
                return TumblrClient.this.accessToken;
            }

            @Override
            public String getUserAgent() {
                return TumblrClient.this.appName + "/" + TumblrClient.this.appVersion;
            }

            @Override
            public void onUpdateToken(AccessToken aToken) {
                for (final Logger logger : TumblrClient.this.loggers) {
                    logger.info("Stored Access Token: " + aToken.toJSON().toString());
                }

                TumblrClient.this.accessToken = aToken;
                TumblrClient.this.storage.put(Constants.TOKEN, TumblrClient.this.accessToken.toJSON().toString());
            }

            @Override
            public void onClearToken() {
                TumblrClient.this.accessToken = null;
                TumblrClient.this.storage.remove(Constants.TOKEN);
            }
        };
    }

    private void login(AccessToken aToken) {
        this.accessToken = aToken;

        getExecutor().execute(new Info.Api().call(getExecutor(), getLogger(), new HashMap<>(), makeAuthInterface(),
                new CompletionInterface<Info.Data>() {
                    @Override
                    public void onSuccess(Info.Data result) {
                        TumblrClient.this.me = result;

                        for (final LoginListener listener : TumblrClient.this.loginListeners) {
                            listener.onAccessGranted();
                        }

                        TumblrClient.this.storage.put(Constants.TOKEN,
                                TumblrClient.this.accessToken.toJSON().toString());
                    }

                    @Override
                    public void onFailure(BaseException e) {
                        if (!(e instanceof NetworkException)) {
                            // we can reach Tumblr, but we cannot access it. So, throw away our tokens, and
                            // let's ask a new authentication
                            for (final Logger logger : TumblrClient.this.loggers) {
                                logger.warning("Auth token not valid");
                            }

                            logout();
                        }

                        // else we cannot reach Tumblr, fail but do not remove our tokens
                        for (final LoginListener listener : TumblrClient.this.loginListeners) {
                            listener.onLoginFailure(e);
                        }
                    }
                }));
    }

    public void addLogger(Logger logger) {
        this.loggers.add(logger);
    }

    public void remove(Logger logger) {
        this.loggers.remove(logger);
    }

    public void addLoginListener(LoginListener loginListener) {
        this.loginListeners.add(loginListener);
    }

    public void removeLoginListener(LoginListener loginListener) {
        this.loginListeners.remove(loginListener);
    }

    public void login() {
        if (this.loginAction == null) {
            throw new NullPointerException("loginAction is null");
        }

        if (this.storage.has(Constants.TOKEN)) {
            // ok, we already have authentication tokens, let's try them first
            this.accessToken = AccessToken.fromJSON(this.storage.get(Constants.TOKEN, ""));
            for (final Logger logger : this.loggers) {
                logger.info("Stored Access Token: " + this.accessToken.toJSON().toString());
            }

            login(this.accessToken);
        } else {
            // never logged in before, do that
            doLogin();
        }
    }

    public void logout() {
        this.storage.remove(Constants.TOKEN);
        this.accessToken = null;
    }

    /* **** SINGLE ITEM API CALL **** */
    private <T> void doCall(final com.github.savemytumblr.api.simple.ApiInterface<T> obj,
            final Map<String, String> queryParams,
            final com.github.savemytumblr.api.simple.CompletionInterface<T> onCompletion) {
        if (this.accessToken == null) {
            for (final LoginListener listener : this.loginListeners) {
                listener.onLoginFailure(new com.github.savemytumblr.exception.RuntimeException("Not logged"));
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

        if (this.accessToken == null) {
            for (final LoginListener listener : this.loginListeners) {
                listener.onLoginFailure(new com.github.savemytumblr.exception.RuntimeException("Not logged"));
            }

            return;
        }

        final int newLimit = (limit == -1) ? 20 : Math.min(20, limit);

        getExecutor().execute(obj.call(getExecutor(), getLogger(), resultList, queryParams, makeAuthInterface(), offset,
                newLimit, new com.github.savemytumblr.api.array.CompletionInterface<T, W>() {

                    @Override
                    public void onSuccess(List<T> result, Integer iOffset, Integer iLimit, int iCount) {
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

                        Integer inNewOffset = iOffset + resultList.size();
                        Integer inNewLimit;

                        if (iCount == -1) {
                            // cannot get the end of the list

                            if (obj.getLimit() == -1) {
                                // the caller did not specify a limit, so the first content
                                // is fine, we're done
                                if (onCompletion != null) {
                                    onCompletion.onSuccess(resultList, obj.getOffset(), obj.getLimit(),
                                            resultList.size());
                                }
                                return;
                            }

                            if (resultList.size() >= obj.getLimit()) {
                                // fetched enough content, we're done
                                if (onCompletion != null) {
                                    onCompletion.onSuccess(resultList, obj.getOffset(), obj.getLimit(),
                                            resultList.size());
                                }
                                return;
                            }

                            inNewLimit = obj.getLimit() - resultList.size();

                        } else {

                            inNewLimit = ((obj.getLimit() == -1) ? iCount : obj.getLimit()) - resultList.size();

                            if (inNewLimit <= 0) {
                                // fetched enough content, we're done
                                if (onCompletion != null) {
                                    onCompletion.onSuccess(resultList, obj.getOffset(), obj.getLimit(),
                                            resultList.size());
                                }
                                return;
                            }
                        }

                        doCall(resultList, obj, inNewOffset, inNewLimit, queryParams, onCompletion);
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
            final Class<? extends com.github.savemytumblr.api.array.ApiInterface<T, W>> clazz, final Integer offset,
            final Integer limit, final Map<String, String> queryParams,
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
            final Class<? extends com.github.savemytumblr.api.array.ApiInterface<T, W>> clazz, final Integer offset,
            final Map<String, String> queryParams,
            final com.github.savemytumblr.api.array.CompletionInterface<T, W> onCompletion) {
        call(clazz, offset, 20, queryParams, onCompletion);
    }

    public <T, W extends ContentInterface<T>> void call(
            final Class<? extends com.github.savemytumblr.api.array.ApiInterface<T, W>> clazz, final Integer offset,
            final Integer limit, final com.github.savemytumblr.api.array.CompletionInterface<T, W> onCompletion) {
        call(clazz, offset, limit, new HashMap<>(), onCompletion);
    }

    public <T, W extends ContentInterface<T>> void call(
            final Class<? extends com.github.savemytumblr.api.array.ApiInterface<T, W>> clazz, final Integer offset,
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
            final Integer offset, final Integer limit, final Map<String, String> queryParams,
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
            final Integer offset, final Map<String, String> queryParams,
            final com.github.savemytumblr.api.array.CompletionInterface<T, W> onCompletion) {
        call(clazz, blogId, offset, 20, queryParams, onCompletion);
    }

    public <T, W extends ContentInterface<T>> void call(
            final Class<? extends com.github.savemytumblr.blog.array.ApiInterface<T, W>> clazz, final String blogId,
            final Integer offset, final Integer limit,
            final com.github.savemytumblr.api.array.CompletionInterface<T, W> onCompletion) {
        call(clazz, blogId, offset, limit, new HashMap<>(), onCompletion);
    }

    public <T, W extends ContentInterface<T>> void call(
            final Class<? extends com.github.savemytumblr.blog.array.ApiInterface<T, W>> clazz, final String blogId,
            final Integer offset, final com.github.savemytumblr.api.array.CompletionInterface<T, W> onCompletion) {
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

    public Info.Data getMe() {
        return this.me;
    }

    public boolean isLogged() {
        return this.accessToken != null;
    }
}