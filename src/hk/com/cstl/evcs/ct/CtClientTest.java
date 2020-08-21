package hk.com.cstl.evcs.ct;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.websocket.server.ServerContainer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import hk.com.cstl.evcs.ocpp.CpWebSocket;

public class CtClientTest {
	public static void main(String[] args) {
		String s = "0123456789ABCD";
		System.out.println(s.substring(0, 10));
		if(1==1)return;
		
		CtClientTest ct = new CtClientTest();
		JFrame f = new JFrame("CT");
		f.getContentPane().add(new JButton("CT"));
		f.setSize(new Dimension(400, 400));
		f.setVisible(true);
		
		ct.startJettyServer();
	}
	
	
	public void startJettyServer() {
		// The Server
        Server server = new Server();

        // HTTP connector
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
        server.addConnector(connector);

        // Set a handler
        //server.setHandler(new DefaultHandler());
        
        // Setup the basic application "context" for this application at "/"
        // This is also known as the handler tree (in jetty speak)
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Start the server
        try {
        	ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);

            // Add WebSocket endpoint to javax.websocket layer
            wscontainer.addEndpoint(CpWebSocket.class);
            
			server.start();
			server.join();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
