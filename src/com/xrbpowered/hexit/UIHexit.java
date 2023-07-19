package com.xrbpowered.hexit;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

import com.xrbpowered.zoomui.DragActor;
import com.xrbpowered.zoomui.GraphAssist;
import com.xrbpowered.zoomui.KeyInputHandler;
import com.xrbpowered.zoomui.UIContainer;
import com.xrbpowered.zoomui.UIElement;
import com.xrbpowered.zoomui.base.UIPanView;
import com.xrbpowered.zoomui.std.UIListItem;
import com.xrbpowered.zoomui.std.text.UITextBox;

public class UIHexit extends UIElement implements KeyInputHandler {

	protected Font font = new Font("Consolas", Font.PLAIN, GraphAssist.ptToPixels(12f));
	protected float fontSizeUnscaled = font.getSize();

	public Color colorBackground = UITextBox.colorBackground;
	public Color colorHighlight = UIListItem.colorHighlight;
	public Color colorText = UITextBox.colorText;
	public Color colorSelection = UITextBox.colorSelection;
	public Color colorSelectedText = UITextBox.colorSelectedText;
	public Color colorMargin = new Color(0xeeeeee);
	public Color colorMarginText = new Color(0xaaaaaa);

	protected DragActor dragSelectActor = new DragActor() {
		private float x, y;
		@Override
		public boolean notifyMouseDown(float x, float y, Button button, int mods) {
			if(button==Button.left) {
				// checkPushHistory(HistoryAction.unspecified);
				this.x = baseToLocalX(x);
				this.y = baseToLocalY(y);
				cursorToMouse(this.x, this.y);
				startSelection();
				return true;
			}
			return false;
		}

		@Override
		public boolean notifyMouseMove(float dx, float dy) {
			x += dx * getPixelScale();
			y += dy * getPixelScale();
			cursorToMouse(this.x, this.y);
			if(cursor<data.size())
				cursor++;
			scrollToCursor();
			modifySelection(true);
			repaint();
			return true;
		}

		@Override
		public boolean notifyMouseUp(float x, float y, Button button, int mods, UIElement target) {
			return true;
		}
	};
	
	protected ByteBuffer data;
	
	public int numCols = 2;
	public int linePaddingTop = 4;
	public int linePaddingBottom = 0;
	
	protected int cursor = 0;
	protected boolean hexCursorLow = false;
	protected Integer selStart = null;
	protected Integer selEnd = null;
	protected Integer selMin = null;
	protected Integer selMax = null;
	
	public boolean insertMode = false;
	public boolean hexMode = true;
	
	protected int displayLine = 0;
	protected float pixelScale = 0;
	protected int lineHeight = 0;
	protected int descent = 0;
	protected int page = 0;
	protected int charWidth = 0;
	protected int y0;
	protected int xmargin, colMargin, byteMargin; 
	protected Rectangle clipBounds = new Rectangle();
	protected int minx, maxx, maxy;
	protected boolean updateSize = false;
	
	protected FontMetrics fm = null;
	protected float fontSize = 0f;
	
	public UIHexit(UIContainer parent) {
		super(parent);
		repaintOnHover = true;
		data = new ByteBuffer();
		getBase().tabIndex().setStickyFocus(this);
	}

	public void updateSize() {
		this.updateSize = true;
	}
	
	public UIPanView panView() {
		return (UIPanView) getParent();
	}
	
	public void scrollToCursor() {
		float panx = panView().getPanX();
		float pany = panView().getPanY();
		
		/*checkCursorLineCache();
		int cx = x0+stringWidth(cursorLine, cursorLineStart, cursorLineStart, cursorLineStart+cursor.col);
		if(cx-x0<minx)
			panx = (cx-x0)*pixelScale;
		else if(cx+x0>maxx)
			panx += (cx+x0-maxx)*pixelScale; // FIXME error in this branch
		*/
		panx = 0;
		
		int curLine = cursorLine();
		if(displayLine>curLine)
			pany = curLine*lineHeight*pixelScale;
		else if(displayLine+page<=curLine) {
			pany = lineHeight*(curLine+1)*pixelScale - getParent().getHeight();
		}
		
		panView().setPan(panx, pany);
	}
	
	public void setData(ByteBuffer data) {
		this.data = data;
		cursor = 0;
		hexCursorLow = false;
		deselect();
	}
	
	public ByteBuffer getData() {
		return data;
	}
	
	public int countLines() {
		return data.size() / numCols / 16 + 1; 
	}
	
	protected int cursorLine() {
		return cursor / numCols / 16;
	}
	
	@Override
	public boolean isVisible(Rectangle clip) {
		return isVisible();
	}

	public void setFont(Font font, float fontSizePt) {
		this.font = font;
		this.fontSizeUnscaled = 96f*fontSizePt/72f;
		this.fm = null;
	}
	
	protected void updateMetrics(GraphAssist g, float fontSize) {
		if(fm==null || fontSize!=this.fontSize) {
			this.fontSize = fontSize;
			fm = g.graph.getFontMetrics(font);
		}
		lineHeight = fm.getAscent()+fm.getDescent()-1+linePaddingTop+linePaddingBottom;
		
		descent = fm.getDescent()+linePaddingBottom;
		charWidth = fm.stringWidth("W");
		y0 = lineHeight*(1+displayLine)-descent;
		g.graph.getClipBounds(clipBounds);
		minx = (int)Math.floor(clipBounds.getMinX());
		maxx = (int)Math.ceil(clipBounds.getMaxX());
		maxy = (int)Math.ceil(clipBounds.getMaxY());
		page = (maxy-y0)/lineHeight;
		
		xmargin = charWidth*8+(int)(8/pixelScale);
		colMargin = charWidth + charWidth/2;
		byteMargin = charWidth/2;
	}
	
	@Override
	public void paint(GraphAssist g) {
		boolean focused = isFocused();
		if(lineHeight>0)
			displayLine = (int)(panView().getPanY() / pixelScale / lineHeight);
		
		pixelScale = g.startPixelMode(this);
		updateMetrics(g, Math.round(fontSizeUnscaled/pixelScale));
		
		int y = y0;
		int pos = displayLine*numCols*16;
		final int lines = countLines();
		final int curLine = cursorLine();
		for(int lineIndex=displayLine; lineIndex<lines && y-lineHeight<maxy; lineIndex++) {
			boolean drawCursor = lineIndex==curLine && focused && !hasSelection();
			Color bg = drawCursor ? colorHighlight : null;
			drawLine(g, lineIndex, pos, y, bg, drawCursor);
			y += lineHeight;
			pos += numCols * 16;
		}
		if(y-lineHeight<maxy) {
			fillRemainder(g, y);
		}
			
		
		float w = getLineWidth()*pixelScale;
		float h = lineHeight*lines*pixelScale;
		if(updateSize || getWidth()!=w || getHeight()!=h) {
			panView().setPanRangeForClient(w, h);
			if(w<getParent().getWidth())
				w = getParent().getWidth();
			if(h<getParent().getHeight())
				h = getParent().getHeight();
			setSize(w, h);
			updateSize = false;
		}
		
		g.finishPixelMode();
	}
	
	protected void fillRemainder(GraphAssist g, int y) {
		y = y-lineHeight+descent;
		int h = maxy-y+lineHeight-descent;
		g.fillRect(xmargin, y, maxx, h, colorBackground);
		g.fillRect(0, y, xmargin, h, colorMargin);
		drawSeparators(g, y, h);
	}
	
	protected void drawSeparators(GraphAssist g, int y, int h) {
		g.setStroke(1/pixelScale);
		g.setColor(colorMarginText);
		int x = xmargin;
		for(int col=0; col<=numCols; col++) {
			g.line(x, y, x, y+h);
			x += getColWidth();
		}
	}
	
	protected void drawLine(GraphAssist g, int lineIndex, int pos, int y, Color bg, boolean drawCursor) {
		g.setFont(font);
		
		g.fillRect(0, y-lineHeight+descent, xmargin, lineHeight, colorMargin);
		g.setColor(colorMarginText);
		g.drawString(String.format("%08X", pos), xmargin-4/pixelScale, y, GraphAssist.RIGHT, GraphAssist.BOTTOM);

		drawRemainder(g, xmargin, y, bg);
		Color fg;

		int x = xmargin;
		int addr = pos;
		int cx = 0;
		for(int col=0; col<numCols; col++) {
			for(int i=0; i<16; i++) {
				int x0 = x;
				x += (i%4==0) ? colMargin : byteMargin;
				if(selMin!=null && addr-1>=selMin && addr<selMax)
					drawBg(g, x0, x, y, colorSelection);
				if(addr==cursor)
					cx = x;
				if(addr>=data.size())
					break;
				int b = data.get(addr);
				bg = null;
				fg = colorText;
				if(selMin!=null && addr>=selMin && addr<selMax) {
					bg = colorSelection;
					fg = colorSelectedText;
				}
				drawString(g, String.format("%02X", b), x, y, bg, fg);
				x += charWidth*2;
				addr++;
			}
			if(selMin!=null && addr-1>=selMin && addr<selMax)
				drawBg(g, x, x+colMargin, y, colorSelection);
			x += colMargin;
			if(addr>=data.size())
				break;
		}
		if(drawCursor) {
			if(hexMode) {
				if(hexCursorLow)
					cx += charWidth;
				drawCursor(g, cx, y);
			}
			else
				drawCursorBlank(g, cx, y, 2);
		}
		

		x = xmargin + numCols*getColWidth() + colMargin;
		
		addr = pos;
		cx = 0;
		for(int col=0; col<numCols*16; col++) {
			if(addr==cursor)
				cx = x;
			if(addr>=data.size())
				break;
			char ch = (char)data.get(addr);
			String s;
			bg = null;
			if(isPrintableChar(ch)) {
				s = Character.toString(ch);
				fg = colorText;
			}
			else {
				s = "\u00b7";
				fg = colorMarginText;
			}
			if(selMin!=null && addr>=selMin && addr<selMax) {
				bg = colorSelection;
				fg = colorSelectedText;
			}
			x = drawString(g, s, x, y, bg, fg);
			addr++;
		}
		if(drawCursor) {
			if(!hexMode)
				drawCursor(g, cx, y);
			else
				drawCursorBlank(g, cx, y, 1);
		}
		
		drawSeparators(g, y-lineHeight+descent, lineHeight);
	}
	
	protected void drawCursor(GraphAssist g, int cx, int y) {
		if(cx>0) {
			g.graph.setXORMode(Color.BLACK);
			float w = insertMode ? 2f/pixelScale : charWidth;
			g.fillRect(cx, y-lineHeight+descent, w, lineHeight, Color.WHITE);
			g.graph.setPaintMode();
		}
	}
	
	protected void drawCursorBlank(GraphAssist g, int cx, int y, int box) {
		if(cx>0) {
			g.graph.setXORMode(Color.BLACK);
			g.setStroke(1/pixelScale);
			g.drawRect(cx, y-lineHeight+descent, charWidth*box-1, lineHeight-1, Color.WHITE);
			g.graph.setPaintMode();
		}
	}

	protected void drawRemainder(GraphAssist g, int x, int y, Color bg) {
		if(x<maxx)
			g.fillRect(x, y-lineHeight+descent, maxx-x, lineHeight, bg==null ? colorBackground : bg);
	}

	protected void drawBg(GraphAssist g, int x0, int x1, int y, Color bg) {
		g.fillRect(x0, y-lineHeight+descent, x1-x0, lineHeight, bg==null ? colorBackground : bg);
	}

	protected int drawString(GraphAssist g, String s, int x, int y, Color bg, Color fg) {
		int w = charWidth * s.length();
		if(x<maxx && x+w>minx) {
			if(bg!=null)
				g.fillRect(x, y-lineHeight+descent, w, lineHeight, bg);
			g.setColor(fg);
			g.setFont(font);
			g.drawString(s, x, y);
		}
		return x + w;
	}
	
	protected int getColWidth() {
		return 12*byteMargin+5*colMargin+32*charWidth;
	}
	
	protected int getLineWidth() {
		return xmargin + numCols*getColWidth() + numCols*16*charWidth + colMargin*2;
	}
	
	protected boolean modeForMouseX(float x) {
		return x / pixelScale < xmargin + numCols*getColWidth();
	}
	
	protected void cursorToMouse(float x, float y) {
		int line = (int)(y / pixelScale / lineHeight);
		if(line<0)
			line = 0;
		if(line>=countLines())
			line = countLines()-1;

		x /= pixelScale;
		int cx;
		if(hexMode) {
			x -= xmargin;
			final int colw = getColWidth();
			int col = (int)(x / colw);
			x -= col*colw;
			final int quadw = colMargin + 3*byteMargin + charWidth*8;
			int quad = (int)(x / quadw);
			if(quad>3)
				quad = 3;
			x -= quad*quadw + colMargin;
			final int bytew = charWidth*2+byteMargin;
			cx = (int)(x / bytew);
			x -= cx*bytew;
			hexCursorLow = (x>charWidth);
			cx += col*16 + quad*4;
		}
		else {
			cx = (int)((x - xmargin - numCols*getColWidth() - colMargin) / charWidth);
		}
		if(cx<0)
			cx = 0;
		if(cx>=numCols*16)
			cx = numCols*16-1;
		
		cursor = line*numCols*16 + cx;
		if(cursor>data.size())
			cursor = data.size();
		if(cursor==data.size())
			hexCursorLow = false;
	}
	
	public void deselect() {
		selStart = null;
		selEnd = null;
		updateSelRange();
	}
	
	public void selectAll() {
		selStart = 0;
		selEnd = data.size();
		cursor = selEnd;
		updateSelRange();
	}
	
	public boolean hasSelection() {
		return selStart!=null && selStart!=selEnd;
	}
	
	/*public String getSelectedText() {
		if(selStart!=null) {
			int start = lines.get(selMin.line).calcStart()+selMin.col;
			int end = lines.get(selMax.line).calcStart()+selMax.col;
			return text.substring(start, end);
		}
		else
			return null;
	}*/
	
	protected void startSelection() {
		if(selStart==null) {
			selStart = cursor;
			selEnd = cursor;
			updateSelRange();
		}
	}
	
	protected void modifySelection(boolean keepStart) {
		if(selStart!=null) {
			selEnd = cursor;
			if(selStart==selEnd && !keepStart)
				deselect();
			updateSelRange();
		}
	}
	
	protected void modifySelection() {
		modifySelection(false);
	}
	
	protected void updateSelRange() {
		if(selStart==null) {
			selMin = null;
			selMax = null;
		}
		else {
			if(selStart>selEnd) {
				selMin = selEnd;
				selMax = selStart;
			}
			else {
				selMin = selStart;
				selMax = selEnd;
			}
		}
	}
	
	protected boolean deleteSelection(boolean pushHistory) {
		if(selStart!=null) {
			// if(pushHistory)
			//	history.push();
			
			cursor = selMin;
			if(insertMode)
				data.modify(selMin, null, selMax);
			else
				data.fill(selMin, selMax-selMin, (byte)0);
			deselect();
			
			if(pushHistory) {
				// history.push();
				// historyAction = HistoryAction.unspecified;
			}
			return true;
		}
		else
			return false;
	}
	
	public void deleteSelection() {
		deleteSelection(true);
	}
	
	protected boolean isCursorAtWordBoundary() {
		if(cursor==0 || cursor==data.size())
			return true;
		else if(hexMode) {
			return cursor%4==0;
		}
		else {
			char ch = cursor==0 ? '\0' : (char)data.get(cursor-1);
			char ch1 = (char)data.get(cursor);
			return isPrintableChar(ch)!=isPrintableChar(ch1) ||
					Character.isWhitespace(ch) && !Character.isWhitespace(ch1) ||
					(Character.isAlphabetic(ch) || Character.isDigit(ch))!=(Character.isAlphabetic(ch1) || Character.isDigit(ch1));
		}
	}
	
	@Override
	public boolean onKeyPressed(char c, int code, int modifiers) {
		boolean shift = (modifiers&UIElement.modShiftMask)>0;
		boolean ctrl = (modifiers&UIElement.modCtrlMask)>0;
		switch(code) {
			case KeyEvent.VK_LEFT:
				// checkPushHistory();
				if(shift)
					startSelection();
				else {
					if(selMin!=null)
						cursor = selMin;
					deselect();
				}
				do {
					if(hexMode && !ctrl && !shift && hexCursorLow)
						hexCursorLow = false;
					else if(cursor>0) {
						cursor--;
						hexCursorLow = !ctrl;
					}
				} while(ctrl && !isCursorAtWordBoundary());
				scrollToCursor();
				if(shift)
					modifySelection();
				break;
				
			case KeyEvent.VK_RIGHT:
				// checkPushHistory();
				if(shift)
					startSelection();
				else {
					if(selMax!=null)
						cursor = selMax;
					deselect();
				}
				do {
					if(hexMode && !ctrl && !shift && !hexCursorLow && cursor<data.size())
						hexCursorLow = true;
					else if(cursor<data.size()) {
						cursor++;
						hexCursorLow = false;
					}
				} while(ctrl && !isCursorAtWordBoundary());
				scrollToCursor();
				if(shift)
					modifySelection();
				break;
				
			case KeyEvent.VK_UP:
				// checkPushHistory();
				if(modifiers==UIElement.modCtrlMask) {
					panView().pan(0, lineHeight);
				}
				else {
					if(modifiers==UIElement.modShiftMask)
						startSelection();
					else
						deselect();
					if(cursorLine()>0) {
						cursor -= numCols*16;
					}
					scrollToCursor();
					if(modifiers==UIElement.modShiftMask)
						modifySelection();
				}
				break;
				
			case KeyEvent.VK_DOWN:
				// checkPushHistory();
				if(modifiers==UIElement.modCtrlMask) {
					panView().pan(0, -lineHeight);
				}
				else {
					if(modifiers==UIElement.modShiftMask)
						startSelection();
					else
						deselect();
					if(cursorLine()<countLines()-1) {
						cursor += numCols*16;
						if(cursor>data.size())
							cursor = data.size();
						if(cursor==data.size())
							hexCursorLow = false;
					}
					scrollToCursor();
					if(modifiers==UIElement.modShiftMask)
						modifySelection();
				}
				break;
/*				
			case KeyEvent.VK_PAGE_UP:
				checkPushHistory();
				if(modifiers==UIElement.modShiftMask)
					startSelection();
				else
					deselect();
				if(cursor.line>page)
					cursor.line -= page;
				else
					cursor.line = 0;
				updateCursor();
				scrollToCursor();
				if(modifiers==UIElement.modShiftMask)
					modifySelection();
				break;
				
			case KeyEvent.VK_PAGE_DOWN:
				checkPushHistory();
				if(modifiers==UIElement.modShiftMask)
					startSelection();
				else
					deselect();
				if(cursor.line+page<=lines.size()-1)
					cursor.line += page;
				else
					cursor.line = lines.size()-1;
				updateCursor();
				scrollToCursor();
				if(modifiers==UIElement.modShiftMask)
					modifySelection();
				break;
				
			case KeyEvent.VK_HOME:
				checkPushHistory();
				if((modifiers&UIElement.modShiftMask)>0)
					startSelection();
				else
					deselect();
				if((modifiers&UIElement.modCtrlMask)>0) {
					cursor.line = 0;
				}
				cursor.col = 0;
				cursorX = -1;
				scrollToCursor();
				if((modifiers&UIElement.modShiftMask)>0)
					modifySelection();
				break;
				
			case KeyEvent.VK_END:
				checkPushHistory();
				if((modifiers&UIElement.modShiftMask)>0)
					startSelection();
				else
					deselect();
				if((modifiers&UIElement.modCtrlMask)>0) {
					cursor.line = lines.size()-1;
				}
				cursor.col = lines.get(cursor.line).length;
				cursorX = -1;
				scrollToCursor();
				if((modifiers&UIElement.modShiftMask)>0)
					modifySelection();
				break;
				
			case KeyEvent.VK_BACK_SPACE:
				if(selStart!=null) {
					deleteSelection();
					scrollToCursor();
				}
				else {
					checkPushHistory(HistoryAction.deleting);
					if(cursor.col>0) {
						modify(cursor.line, cursor.col-1, "", cursor.col);
						cursor.col--;
						scrollToCursor();
					}
					else if(cursor.line>0) {
						cursor.col = lines.get(cursor.line-1).length;
						cursor.line--;
						joinLineWithNext();
						scrollToCursor();
					}
				}
				break;
*/
			case KeyEvent.VK_DELETE:
				if(selStart!=null) {
					deleteSelection();
					scrollToCursor();
				}
				else {
					// checkPushHistory(HistoryAction.deleting);
					if(cursor<data.size()) {
						if(insertMode)
							data.modify(cursor, null, cursor+1);
						else
							data.modifyByte(cursor, (byte)0);
					}
				}
				break;

			case KeyEvent.VK_ESCAPE:
				// checkPushHistory();
				getBase().resetFocus();
				break;
				
			case KeyEvent.VK_INSERT:
				// checkPushHistory();
				insertMode = !insertMode;
				break;
				
			case KeyEvent.VK_TAB:
				// checkPushHistory();
				hexCursorLow = false;
				hexMode = !hexMode;
				scrollToCursor();
				break;
				
			default: {
				if(modifiers==UIElement.modCtrlMask) {
					switch(code) {
						case KeyEvent.VK_A:
							// checkPushHistory();
							selectAll();
							break;
						/*case KeyEvent.VK_X:
							checkPushHistory();
							cutSelection();
							break;
						case KeyEvent.VK_C:
							checkPushHistory();
							copySelection();
							break;
						case KeyEvent.VK_V:
							checkPushHistory();
							pasteAtCursor();
							break;
						case KeyEvent.VK_Z:
							checkPushHistory();
							history.undo();
							break;
						case KeyEvent.VK_Y:
							checkPushHistory();
							history.redo();
							break;*/
						default:
							return false;
					}
				}
				else {
					if(hexMode) {
						int hex = charToHex(c);
						if(hex>=0) {
							if(insertMode)
								deleteSelection();
							else {
								if(selMin!=null)
									cursor = selMin;
								deselect();
							}
							// checkPushHistory(HistoryAction.typing);
							if(insertMode && !hexCursorLow || cursor>=data.size())
								data.modify(cursor, new byte[] {(byte)(hex<<4)}, cursor);
							else
								data.modifyByte(cursor, hex, hexCursorLow);
							if(hexCursorLow)
								cursor++;
							hexCursorLow = !hexCursorLow;
							scrollToCursor();
						}
					}
					else if(isPrintableChar(c)) {
						if(insertMode)
							deleteSelection();
						else {
							if(selMin!=null)
								cursor = selMin;
							deselect();
						}
						// checkPushHistory(HistoryAction.typing);
						if(insertMode || cursor>=data.size())
							data.modify(cursor, new byte[] {(byte)c}, cursor);
						else
							data.modifyByte(cursor, (byte)c);
						cursor++;
						scrollToCursor();
					}
				}
			}
		}
		repaint();
		return true;
	}
	
	@Override
	public void onMouseIn() {
		getBase().getWindow().setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		super.onMouseIn();
	}
	
	@Override
	public void onMouseOut() {
		getBase().getWindow().setCursor(Cursor.getDefaultCursor());
		super.onMouseOut();
	}
	
	@Override
	public boolean onMouseDown(float x, float y, Button button, int mods) {
		if(button==Button.left) {
			if(!isFocused())
				getBase().setFocus(this);
			// else 
			// 	checkPushHistory();
			deselect();
			hexMode = modeForMouseX(x);
			cursorToMouse(x, y);
			repaint();
			return true;
		}
		else
			return false;
	}
	
	@Override
	public DragActor acceptDrag(float x, float y, Button button, int mods) {
		if(dragSelectActor.notifyMouseDown(x, y, button, mods))
			return dragSelectActor;
		else
			return null;
	}
	
	@Override
	public void onFocusGained() {
		repaint();
	}

	@Override
	public void onFocusLost() {
		// checkPushHistory();
		repaint();
	}
	
	public static int charToHex(char c) {
		if(c>='0' && c<='9')
			return c-'0';
		else if(c>='A' && c<='F')
			return c-'A'+10;
		else if(c>='a' && c<='f')
			return c-'a'+10;
		else
			return -1;
	}
	
	public static boolean isPrintableChar(char c) {
		return c>=32 && c<127;
	}

}
