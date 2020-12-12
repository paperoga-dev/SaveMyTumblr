package com.github.savemytumblr;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public class LoginBrowser extends Dialog {
    private String authVerifier;

    public LoginBrowser(Shell parentShell) {
        super(parentShell);
    }

    public String open(String url) {
        final Shell parent = getParent();
        final Shell dialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);

        dialog.setSize(600, 600);
        dialog.setText("Tumblr Login");
        dialog.open();

        FillLayout fillLayout = new FillLayout();
        fillLayout.type = SWT.VERTICAL;

        dialog.setLayout(fillLayout);

        try {
            Browser browser = new Browser(dialog, SWT.NONE);

            browser.setJavascriptEnabled(true);
            browser.addLocationListener(new LocationListener() {

                @Override
                public void changed(LocationEvent arg0) {
                    if (!arg0.location.toLowerCase().contains(Constants.CALLBACK_URL.toLowerCase()))
                        return;

                    try {
                        URL newUrl = new URL(arg0.location);

                        String[] tokens = newUrl.getQuery().split("&");

                        for (String tok : tokens) {
                            String[] tokI = tok.split("=");

                            if (tokI[0].equalsIgnoreCase(Constants.OAUTH_VERIFIER)) {
                                authVerifier = tokI[1];
                            }
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }

                    dialog.close();
                }

                @Override
                public void changing(LocationEvent arg0) {
                }
            });

            browser.setUrl(url);
            dialog.layout();
            dialog.redraw();

            Display display = parent.getDisplay();
            while (!dialog.isDisposed()) {
                if (!display.readAndDispatch())
                    display.sleep();
            }

            return authVerifier;

        } catch (SWTError e) {
            MessageBox messageBox = new MessageBox(dialog, SWT.ICON_ERROR | SWT.OK);
            messageBox.setMessage("Browser cannot be initialized.");
            messageBox.setText("Exit");
            messageBox.open();

            return null;
        }
    }
}
