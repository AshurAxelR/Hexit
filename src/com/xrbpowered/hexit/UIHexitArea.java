package com.xrbpowered.hexit;

import java.awt.Color;

import com.xrbpowered.zoomui.GraphAssist;
import com.xrbpowered.zoomui.UIContainer;
import com.xrbpowered.zoomui.std.UIScrollContainer;

public class UIHexitArea extends UIScrollContainer {

	public final UIHexit editor;
	
	public UIHexitArea(UIContainer parent) {
		super(parent);
		this.editor = new UIHexit(this.getView());
	}
	
	@Override
	protected void paintSelf(GraphAssist g) {
		g.fill(this, Color.WHITE);
	}
	
	@Override
	protected void paintBorder(GraphAssist g) {
		g.hborder(this, GraphAssist.TOP, colorBorder);
		g.hborder(this, GraphAssist.BOTTOM, colorBorder);
	}
	
	@Override
	protected float layoutView() {
		editor.setLocation(0, 0);
		editor.updateSize();
		return editor.getHeight();
	}
}
