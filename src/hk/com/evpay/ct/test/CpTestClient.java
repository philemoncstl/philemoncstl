package hk.com.evpay.ct.test;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import com.ckzone.util.DateUtil;

import hk.com.cstl.evcs.ocpp.eno.ChargePointStatus;

@ClientEndpoint
public class CpTestClient extends JFrame{
	private static Object waitLock = new Object();
	
	private static Session session = null;
	
	private boolean cont = true;
	private String host;
	private String port;
	private String cpNo;
	
	private static JComboBox cboStatus;
	private static JLabel lblLastSend;
	private static JLabel lblLastReceive;
	
	private static JTextArea txaLog;
	

	public static void main(String[] args) {
		//String host = "192.168.1.78";
		String host = "192.168.1.207";
		String port = "8080";
		String cp = "CP10";
		if(args.length > 0) {
			cp = args[0];
		}
		
		if(args.length > 1) {
			host = args[1];
		}
		
		if(args.length > 2) {
			port = args[2];
		}
		
		CpTestClient client = new CpTestClient(host, port, cp);
		client.init();
		//client.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		client.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		client.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e){
                int rtn = JOptionPane.showConfirmDialog(null, "Are you sure to close CP?",
                		"Close CP Confirmation", JOptionPane.YES_NO_OPTION);
                if(rtn == 0) {
                	client.closeApplication();
                }
            }
        });
		client.pack();
		client.setVisible(true);
	}
	
	public CpTestClient() {
		
	}

	public CpTestClient(String host, String port, String cpNo) throws HeadlessException {
		super();
		this.host = host;
		this.port = port;
		this.cpNo = cpNo;
	}
	
	public void closeApplication() {
		cont = false;
		try {
			session.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	private void init() {
		setLayout(new BorderLayout());
		JPanel pnl = new JPanel();
		
		JButton btnClear = new JButton("Clear Log");
		btnClear.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				txaLog.setText("");
			}
		});
		pnl.add(btnClear);
		
		pnl.add(new JLabel("Status:"));
		
		cboStatus = new JComboBox<>(ChargePointStatus.values());
		cboStatus.addActionListener (new ActionListener () {
		    public void actionPerformed(ActionEvent e) {
		    	txaLog.append("\nStatus changed to :" + cboStatus.getSelectedItem().toString());
		    	append("Status changed to :" + cboStatus.getSelectedItem().toString());
		        sendStatus(cboStatus.getSelectedItem().toString());
		    }
		});
		pnl.add(cboStatus);
		
		lblLastReceive = new JLabel("N/A");
		lblLastReceive.setPreferredSize(new Dimension(250, 30));
		pnl.add(lblLastReceive);
		
		lblLastSend = new JLabel("N/A");
		lblLastSend.setPreferredSize(new Dimension(250, 30));
		pnl.add(lblLastSend);
		add(pnl, BorderLayout.NORTH);
		
		txaLog = new JTextArea(30, 100);
		JScrollPane jsp = new JScrollPane(txaLog);
		add(jsp, BorderLayout.CENTER);
		
		connect();
	}
	
	private void connect() {
		new Thread() {
			public void run() {
				while(cont) {
					try {
						if(session == null || !session.isOpen()) {
							connectNow();
							cboStatus.setSelectedIndex(1);
						}
					}
					catch(Exception e) {
						e.printStackTrace();
					}
					
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
		}.start();
	}
	
	private void append(String msg) {
		txaLog.append("\n" + DateUtil.getCurrentDateTimeStr() + " - " + msg);
	}
	
	private void connectNow() {
		append("Connecting");
		WebSocketContainer container = null;//
		
		try {
			// Tyrus is plugged via ServiceLoader API. See notes above
			container = ContainerProvider.getWebSocketContainer();

			// WS1 is the context-root of my web.app
			// ratesrv is the path given in the ServerEndPoint annotation on server
			// implementation
			session = container.connectToServer(CpTestClient.class,
					URI.create("ws://" + host + ":" + port + "/ocpp/" + cpNo));
			//sendStatus(ChargePointStatus.Preparing.toString());
						
			//wait4TerminateSignal();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setStatus(ChargePointStatus status) {
		String msg = "old:" + cboStatus.getSelectedItem() + ", new:" + status;
		append(msg);
		cboStatus.setSelectedItem(status);
	}

	@OnMessage
	public void onMessage(String message) {
		String dttm = DateUtil.getCurrentDateTimeStr();
		txaLog.append("\n" + dttm + " - Received msg:" + message);
		
		lblLastReceive.setText("Received @ " + dttm);
		// the new USD rate arrives from the websocket server side.
		System.out.println("Received msg: " + message);
		
		if(message.indexOf("Heartbeat") != -1) {		
			String resp = "[\r\n" + 
					"		2,\r\n" + 
					"		\"" + UUID.randomUUID().toString() + "\",\r\n" + 
					"		\"Heartbeat\"\r\n" + 
					"	]";
			sendMsg(resp);
		}
		
		if(message.indexOf("RemoteStartTransaction") != -1) {
			try {
				sendRemoteStartStopResp(message);
				Thread.sleep(2000);
				sendStatus("Charging");
				setStatus(ChargePointStatus.Charging);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if(message.indexOf("RemoteStopTransaction") != -1) {
			try {
				sendRemoteStartStopResp(message);
				Thread.sleep(2000);
				sendStatus("Finishing");
				setStatus(ChargePointStatus.Finishing);
				
				Thread.sleep(5000);
				sendStatus(ChargePointStatus.Available.toString());
				setStatus(ChargePointStatus.Available);
				
				Thread.sleep(10000);
				sendStatus(ChargePointStatus.Preparing.toString());
				setStatus(ChargePointStatus.Preparing);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if(message.indexOf("TriggerMessage") != -1) {
			if(message.indexOf("StatusNotification") != -1) {
				sendStatus(cboStatus.getSelectedItem().toString());
			}
		}
	}
	
	private void sendRemoteStartStopResp(String msg) {
		int start = msg.indexOf("\"");
		int end = msg.indexOf("\"", start + 1);
		String id = msg.substring(start + 1, end);
		System.out.println("ID:" + id);
		String resp = "[3,\"" + id + "\",{\"status\":\"Accepted\"}]";
		
		sendMsg(resp);
	}
	
	private void sendStatus(String status) {
		System.out.println("**************Status:" + status);
		String msg = "[2,\""+ System.currentTimeMillis() + "\",\"StatusNotification\",{\"connectorId\":1,\"errorCode\":0,\"vendorId\":\"Cornerstone\",\"vendorErrorCode\":\"NoError\",\"info\":\"\",\"status\":\""+ status + "\",\"timestamp\":\"2017-12-19T08:21:01.826Z\"}]";
		sendMsg(msg);
	}
	
	private void sendMsg(String msg) {
		String dttm = DateUtil.getCurrentDateTimeStr();
		txaLog.append("\n" + dttm + " - Sending msg:" + msg);
		try {
			System.out.println("Send msg:" + msg);
			synchronized (session) {
				session.getBasicRemote().sendText(msg);
				lblLastSend.setText("Send:" + dttm);
			}			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void wait4TerminateSignal() {
		synchronized (waitLock) {
			try {
				waitLock.wait();
			} catch (InterruptedException e) {
			}
		}
	}

}