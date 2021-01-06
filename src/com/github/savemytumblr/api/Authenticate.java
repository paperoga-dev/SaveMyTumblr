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

package com.github.savemytumblr.api;

import java.io.IOException;
import java.io.OutputStream;

import org.scribe.builder.ServiceBuilder;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import com.github.savemytumblr.Constants;
import com.github.savemytumblr.Secrets;
import com.github.savemytumblr.TumblrClient.Executor;
import com.github.savemytumblr.TumblrClient.Logger;

public class Authenticate {

    public interface OnAuthenticationListener {
        void onAuthenticationRequest(Authenticate authenticator, Token requestToken, String authenticationUrl);

        void onAuthenticationGranted(Token accessToken);

        void onFailure(OAuthException exception);
    }

    private static class RequestTokenTask implements Runnable {
        private final Executor executor;
        private final Logger logger;
        private final Authenticate authenticator;
        private final OAuthService oAuthService;
        private final OnAuthenticationListener onAuthenticationListener;

        RequestTokenTask(Executor executor, Logger logger, Authenticate authenticator, OAuthService oAuthService,
                OnAuthenticationListener onAuthenticationListener) {
            super();

            this.executor = executor;
            this.logger = logger;
            this.authenticator = authenticator;
            this.oAuthService = oAuthService;
            this.onAuthenticationListener = onAuthenticationListener;
        }

        @Override
        public void run() {
            if (onAuthenticationListener == null)
                return;

            try {
                final Token requestToken = oAuthService.getRequestToken();
                logger.info("Request Token: " + requestToken.toString());

                final String authenticationUrl = oAuthService.getAuthorizationUrl(requestToken);
                logger.info("Authentication URL: " + authenticationUrl);

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        onAuthenticationListener.onAuthenticationRequest(authenticator, requestToken,
                                authenticationUrl);
                    }
                });

            } catch (final OAuthException e) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        onAuthenticationListener.onFailure(e);
                    }
                });
            }
        }
    }

    private static class AccessTokenTask implements Runnable {
        private final Executor executor;
        private final Logger logger;
        private final String authVerifier;
        private final Token requestToken;
        private final OAuthService oAuthService;
        private final OnAuthenticationListener onAuthenticationListener;

        AccessTokenTask(Executor executor, Logger logger, String authVerifier, Token requestToken,
                OAuthService oAuthService, OnAuthenticationListener onAuthenticationListener) {
            super();

            this.executor = executor;
            this.logger = logger;
            this.authVerifier = authVerifier;
            this.requestToken = requestToken;
            this.oAuthService = oAuthService;
            this.onAuthenticationListener = onAuthenticationListener;
        }

        @Override
        public void run() {
            if (onAuthenticationListener == null)
                return;

            try {
                final Token authToken = oAuthService.getAccessToken(requestToken, new Verifier(authVerifier));
                logger.info("Access Token: " + authToken.toString());

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        onAuthenticationListener.onAuthenticationGranted(authToken);
                    }
                });

            } catch (final OAuthException e) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        onAuthenticationListener.onFailure(e);
                    }
                });
            }
        }
    }

    public class LogOutputStream extends OutputStream {
        private String mem;

        public LogOutputStream(Logger logger) {
            super();

            mem = "";
        }

        @Override
        public void write(int b) throws IOException {
            char c = (char) (b & 0xff);
            if (c == '\n') {
                flush();
            } else {
                mem += c;
            }
        }

        @Override
        public void flush() {
            logger.info(mem);
            mem = "";
        }
    }

    private final OAuthService oAuthService;
    private OnAuthenticationListener onAuthenticationListener;
    private final Executor executor;
    private final Logger logger;

    public Authenticate(Executor executor, Logger logger) {
        super();

        this.executor = executor;
        this.logger = logger;
        this.oAuthService = new ServiceBuilder().apiKey(Secrets.CONSUMER_KEY).apiSecret(Secrets.CONSUMER_SECRET)
                .provider(OAuthApi.class).callback(Constants.CALLBACK_URL).debugStream(new LogOutputStream(logger))
                .build();
        this.onAuthenticationListener = null;
    }

    public void request() {
        if (onAuthenticationListener == null)
            return;

        executor.execute(new RequestTokenTask(executor, logger, this, oAuthService, onAuthenticationListener));
    }

    public void verify(Token requestToken, String authVerifier) {
        logger.info("Verify: requestToken = " + requestToken.toString());
        logger.info("Verify: authVerifier = " + authVerifier);

        executor.execute(new AccessTokenTask(executor, logger, authVerifier, requestToken, oAuthService,
                onAuthenticationListener));
    }

    public void setOnAuthenticationListener(OnAuthenticationListener onAuthenticationListener) {
        this.onAuthenticationListener = onAuthenticationListener;
    }
}
