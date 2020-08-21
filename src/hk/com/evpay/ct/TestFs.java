package hk.com.evpay.ct;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Window;

public class TestFs {
	public static void main(String[] args) {
		/*String no = "100001234";
		System.out.println(no.substring(no.length() - 4));
		if(1==1) {
			return;
		}*/
		
		
		test1();
		
		new Thread() {
			public void run() {
				System.out.println("Stop in 10 seconds");
				try {
					Thread.sleep(10000);
				} catch(Exception e) {
					e.printStackTrace();
				}
				System.exit(0);
			};
		}.start();
	}
	
	
	private static void test1() {
		GraphicsDevice gs = GraphicsEnvironment.getLocalGraphicsEnvironment().
                getDefaultScreenDevice(); 
                //or initialize this for a specific display
		Frame frame = new Frame(gs.getDefaultConfiguration());
		
		Window win = new Window(frame);
		Canvas c = new Canvas();
		c.setBackground(Color.RED);
		win.add(c);
		win.show();  //or setVisible(true);
		
		//Enter full-screen mode
		gs.setFullScreenWindow(win);
		win.validate();
		win.setAlwaysOnTop(true);
	}
}


