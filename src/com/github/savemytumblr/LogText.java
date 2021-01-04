package com.github.savemytumblr;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class LogText extends Text {
    public LogText(Composite parent) {
        super(parent, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
    }

    public void appendLine(String line) {
        if (getLineCount() > 10000) {
            setText("");
        }

        append(line + "\n");
    }
}
