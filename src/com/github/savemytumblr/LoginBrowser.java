package com.github.savemytumblr;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

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
    private String authCode;
    final private String state;

    public LoginBrowser(String state, Shell parentShell) {
        super(parentShell);
        this.state = state;
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
                }

                @Override
                public void changing(LocationEvent arg0) {
                    if (!arg0.location.startsWith(Constants.CALLBACK_URL)) {
                        arg0.doit = true;
                        return;
                    }

                    Map<String, String> queryParams = new HashMap<>();

                    URI uri = URI.create(arg0.location);
                    String[] items = uri.getQuery().split("&");

                    for (String item : items) {
                        String[] parts = item.split("=");

                        queryParams.put(parts[0], parts[1]);
                    }

                    if (queryParams.getOrDefault("state", "").equals(state) && queryParams.containsKey("code")) {
                        authCode = queryParams.get("code");
                        arg0.doit = false;
                        dialog.close();
                    }
                }
            });

            browser.setUrl(url);
            dialog.layout();
            dialog.redraw();

            Display display = parent.getDisplay();
            while (!dialog.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }

            return authCode;

        } catch (SWTError e) {
            MessageBox messageBox = new MessageBox(dialog, SWT.ICON_ERROR | SWT.OK);
            messageBox.setMessage("Browser cannot be initialized.");
            messageBox.setText("Exit");
            messageBox.open();

            e.printStackTrace();

            return null;
        }
    }
}
