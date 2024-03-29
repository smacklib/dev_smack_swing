/*
 * smack_swing @ https://github.com/smacklib/dev_smack_swing
 *
 * Copyright © 2001-2022 Michael Binz
 */
module framework.smack_swing
{
    exports org.smack.swing.swingx.border;
    exports org.smack.swing.swingx.image;
    exports org.smack.swing.swingx.geom;
    exports org.smack.swing.swingx.tips;
    exports org.smack.swing.swingx.text;
    exports org.smack.swing.swingx.renderer;
    exports org.smack.swing.application;
    exports org.smack.swing.swingx.sort;
    exports org.smack.swing.swingx.tree;
    exports org.smack.swing.swingx.decorator;
    exports org.smack.swing.swingx.graphics;
    exports org.smack.swing.application.session;
    exports org.smack.swing.swingx.calendar;
    exports org.smack.swing.swingx.search;
    exports org.smack.swing.swingx;
    exports org.smack.swing.swingx.color;
    exports org.smack.swing.swingx.treetable;
    exports org.smack.swing.application.util;
    exports org.smack.swing.swingx.icon;
    exports org.smack.swing.swingx.painter.effects;
    exports org.smack.swing.swingx.multisplitpane;
    exports org.smack.swing.swingx.combobox;
    exports org.smack.swing.beans;
    exports org.smack.swing.swingx.error;
    exports org.smack.swing.swingx.table;
    exports org.smack.swing.swingx.prompt;
    exports org.smack.swing.swingx.event;
    exports org.smack.swing.swingx.painter;
    exports org.smack.swing.swingx.action;

    requires transitive framework.smack;
    requires java.datatransfer;
    requires java.desktop;
    requires java.logging;
    requires java.naming;
    requires java.prefs;

    opens org.smack.swing.application to framework.smack;
}
