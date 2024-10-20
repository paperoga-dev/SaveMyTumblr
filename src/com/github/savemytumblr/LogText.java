package com.github.savemytumblr;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class LogText extends Text {
    int lines;

    public LogText(Composite parent) {
        super(parent, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);

        this.lines = 0;
    }

    public void appendLine(String line) {
        if (lines > 1000) {
            setText("");
            lines = 0;
        }

        append(line + "\n");
        ++lines;
    }

    @Override
    protected void checkSubclass() {
        // TODO Auto-generated method stub
    }
}
