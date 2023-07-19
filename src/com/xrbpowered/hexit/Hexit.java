package com.xrbpowered.hexit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.xrbpowered.zoomui.UIModalWindow;
import com.xrbpowered.zoomui.UIModalWindow.ResultHandler;
import com.xrbpowered.zoomui.std.file.UIFileBrowser;
import com.xrbpowered.zoomui.std.menu.UIMenu;
import com.xrbpowered.zoomui.std.menu.UIMenuBar;
import com.xrbpowered.zoomui.std.menu.UIMenuItem;
import com.xrbpowered.zoomui.std.menu.UIMenuSeparator;
import com.xrbpowered.zoomui.swing.SwingFrame;
import com.xrbpowered.zoomui.swing.SwingWindowFactory;

public class Hexit {

	private static SwingFrame frame;
	private static UIHexitArea hexit;
	private static UIModalWindow<File> openDlg;
	private static UIModalWindow<File> saveDlg;
	
	private static File documentFile = null;
	
	public static byte[] loadBytes(InputStream s) throws IOException {
		DataInputStream in = new DataInputStream(s);
		byte[] bytes = new byte[in.available()];
		in.readFully(bytes);
		in.close();
		return bytes;
	}
	
	public static ByteBuffer loadByteBuffer(File file) {
		try {
			return new ByteBuffer(loadBytes(new FileInputStream(file)));
		} catch(IOException e) {
			e.printStackTrace();
			return new ByteBuffer();
		}
	}
	
	public static void saveBytes(byte[] bytes, OutputStream s) throws IOException {
		DataOutputStream out = new DataOutputStream(s);
		out.write(bytes);
		out.close();
	}
	
	public static void saveByteBuffer(ByteBuffer buf, File file) {
		try {
			saveBytes(buf.getBytes(), new FileOutputStream(file));
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void setDocument(File file) {
		documentFile = file;
		if(documentFile!=null)
			frame.frame.setTitle("Hexit: "+documentFile.getName());
		else
			frame.frame.setTitle("Hexit");
	}
	
	private static void createOpenDialog() {
		openDlg = UIFileBrowser.createDialog("Open file", new ResultHandler<File>() {
			@Override
			public void onResult(File result) {
				setDocument(result);
				hexit.editor.setData(loadByteBuffer(result));
			}
			@Override
			public void onCancel() {
			}
		});
	}
	
	private static void createSaveDialog() {
		saveDlg = UIFileBrowser.createDialog("Save file", new ResultHandler<File>() {
			@Override
			public void onResult(File result) {
				if(result!=null) {
					setDocument(result);
					saveByteBuffer(hexit.editor.getData(), documentFile);
				}
			}
			@Override
			public void onCancel() {
			}
		});
	}
	
	private static void addFileMenuItems(final UIMenu menu) {
		new UIMenuItem(menu, "New") {
			@Override
			public void onAction() {
				hexit.editor.setData(new ByteBuffer());
				hexit.repaint();
			}
		};
		new UIMenuItem(menu, "Open...") {
			@Override
			public void onAction() {
				openDlg.show();
			}
		};
		new UIMenuSeparator(menu);
		new UIMenuItem(menu, "Save"){
			@Override
			public void onAction() {
				if(documentFile==null)
					saveDlg.show();
				else {
					saveByteBuffer(hexit.editor.getData(), documentFile);
				}
			}
		};
		new UIMenuItem(menu, "Save As...") {
			@Override
			public void onAction() {
				saveDlg.show();
			}
		};
		new UIMenuSeparator(menu);
		new UIMenuItem(menu, "Exit") {
			@Override
			public void onAction() {
				frame.requestClosing();
			}
		};
	}
	
	/*private static void addEditMenuItems(final UIMenu menu) {
		new UIMenuItem(menu, "Undo") {
			@Override
			public boolean isEnabled() {
				return text.editor.history.canUndo();
			}
			@Override
			public void onAction() {
				text.editor.history.undo();
				text.repaint();
			}
		};
		new UIMenuItem(menu, "Redo") {
			@Override
			public boolean isEnabled() {
				return text.editor.history.canRedo();
			}
			@Override
			public void onAction() {
				text.editor.history.redo();
				text.repaint();
			}
		};
		new UIMenuSeparator(menu);
		new UIMenuItem(menu, "Cut") {
			@Override
			public boolean isEnabled() {
				return text.editor.hasSelection();
			}
			@Override
			public void onAction() {
				text.editor.cutSelection();
				text.repaint();
			}
		};
		new UIMenuItem(menu, "Copy") {
			@Override
			public boolean isEnabled() {
				return text.editor.hasSelection();
			}
			@Override
			public void onAction() {
				text.editor.copySelection();
				text.repaint();
			}
		};
		new UIMenuItem(menu, "Paste") {
			@Override
			public boolean isEnabled() {
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				return clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor);
			}
			@Override
			public void onAction() {
				text.editor.pasteAtCursor();
				text.repaint();
			}
		};
		new UIMenuSeparator(menu);
		new UIMenuItem(menu, "Delete") {
			@Override
			public boolean isEnabled() {
				return text.editor.hasSelection();
			}
			@Override
			public void onAction() {
				text.editor.deleteSelection();
				text.repaint();
			}
		};
		new UIMenuItem(menu, "Select All") {
			@Override
			public void onAction() {
				text.editor.selectAll();
				text.repaint();
			}
		};
	}*/
	
	private static void populateMenus(UIMenuBar menuBar) {
		UIMenu fileMenu = menuBar.addMenu("File");
		addFileMenuItems(fileMenu);
		// UIMenu editMenu = menuBar.addMenu("Edit");
		// addEditMenuItems(editMenu);

		// addEditMenuItems(new UIMenu(popup.getContainer()));
		// popup.setClientSizeToContent();
	}
	
	public static void main(String[] args) {
		frame = new SwingFrame(SwingWindowFactory.use(), "Hexit", 1200, 600, true, false) {
			@Override
			public boolean onClosing() {
				confirmClosing();
				return false;
			}
		};
		
		UIMenuBar menuBar = new UIMenuBar(frame.getContainer());
		hexit = new UIHexitArea(menuBar.content);
		createOpenDialog();
		createSaveDialog();
		populateMenus(menuBar);
		
		
		frame.show();
	}

}
