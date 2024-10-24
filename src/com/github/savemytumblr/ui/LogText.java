package com.github.savemytumblr.ui;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class LogText extends Text {
    private boolean clip;
    private int lines;

    public LogText(Composite parent, boolean bClip) {
        super(parent, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);

        this.clip = bClip;
        this.lines = 0;
    }

    public LogText(Composite parent) {
        this(parent, true);
    }

    public void appendLine(String line) {
        if (this.clip && this.lines > 1000) {
            setText("");
            this.lines = 0;
        }

        append(line + "\n");
        ++this.lines;
    }

    public void appendLines(List<String> lLines) {
        for (final String line : lLines) {
            this.appendLine(line);
        }
    }

    public void clear() {
        setText("");
        this.lines = 0;
    }

    @Override
    protected void checkSubclass() {
        // Not used
    }
}
