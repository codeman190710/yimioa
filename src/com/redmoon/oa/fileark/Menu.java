package com.redmoon.oa.fileark;

import java.util.Vector;
import java.util.Iterator;

public class Menu {
    Vector menuItem = new Vector();

    public Menu() {
    }

    public Iterator Iterator() {
        return menuItem.iterator();
    }

    public void addItem(MenuItem mi) {
        menuItem.addElement(mi);
    }
}