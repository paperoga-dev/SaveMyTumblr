package com.github.savemytumblr.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;

public class TabItem extends org.eclipse.swt.widgets.TabItem {
    private Composite comp;

    protected TabItem(String tabName, TabFolder parent) {
        super(parent, SWT.BORDER);

        this.comp = new Composite(parent, SWT.NONE);

        setControl(this.comp);
        setText(tabName);
    }

    protected Composite getComp() {
        return this.comp;
    }

    public void setEnabled(boolean enabled) {
        this.comp.setEnabled(enabled);
    }

    public boolean isEnabled() {
        return this.comp.isEnabled();
    }

    @Override
    protected void checkSubclass() {
        // Not used
    }
}
