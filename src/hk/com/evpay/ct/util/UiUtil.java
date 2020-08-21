package hk.com.evpay.ct.util;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Window;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JComponent;

import org.apache.log4j.Logger;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.platform.unix.X11;

public class UiUtil {
	private static final Logger logger = Logger.getLogger(UiUtil.class);
	
	private static final int _NET_WM_STATE_REMOVE = 0;
	
    private static final int _NET_WM_STATE_ADD = 1;

    
    public static void setEmptyCursor(Window f) {
		f.setCursor(f.getToolkit().createCustomCursor(
	            new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), new Point(0, 0),
	            "null"));
	}
    
	public static boolean setFullScreenWindow(Window w, boolean fullScreen) {
		X11 x = X11.INSTANCE;
		X11.Display display = null;
		try {
			display = x.XOpenDisplay(null);
			int result = sendClientMessage(
					display,
					Native.getWindowID(w),
					"_NET_WM_STATE",
					new NativeLong[] {
							new NativeLong(fullScreen ? _NET_WM_STATE_ADD
									: _NET_WM_STATE_REMOVE),
							x.XInternAtom(display, "_NET_WM_STATE_FULLSCREEN",
									false), new NativeLong(0L),
							new NativeLong(0L), new NativeLong(0L),
							new NativeLong(0L) });
			return (result != 0);
		} finally {
			if (display != null) {
				x.XCloseDisplay(display);
			}
		}
	}
	
	
	private static int sendClientMessage(X11.Display display, long wid,
			String msg, NativeLong[] data) {
		assert (data.length == 5);
		X11 x = X11.INSTANCE;
		X11.XEvent event = new X11.XEvent();
		event.type = X11.ClientMessage;
		event.setType(X11.XClientMessageEvent.class);
		event.xclient.type = X11.ClientMessage;
		event.xclient.serial = new NativeLong(0L);
		event.xclient.send_event = 1;
		event.xclient.message_type = x.XInternAtom(display, msg, false);
		event.xclient.window = new X11.Window(wid);
		event.xclient.format = 32;
		event.xclient.data.setType(NativeLong[].class);
		System.arraycopy(data, 0, event.xclient.data.l, 0, 5);
		NativeLong mask = new NativeLong(X11.SubstructureRedirectMask
				| X11.SubstructureNotifyMask);
		int result = x.XSendEvent(display, x.XDefaultRootWindow(display), 0,
				mask, event);
		x.XFlush(display);
		return result;
	}
	
	public static boolean isEmpty(String text){
		return text == null || "".equals(text);
	}
	
	public static void debugUi(JComponent comp) {
		if(CtUtil.getConfig().isDebugUi()) {
			comp.setBorder(BorderFactory.createLineBorder(Color.RED));
		}
	}
	
	
	public static void printFontList() {
		String fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

	    for ( int i = 0; i < fonts.length; i++ ){
	      System.out.println(fonts[i]);
	      logger.info("***" + fonts[i] );
	    }
	}
}
