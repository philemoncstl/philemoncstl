package hk.com.evpay.ct.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import hk.com.cstl.evcs.model.TranModel;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONException;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class iUC285Util {
	public enum Status {
		Approved,
		Declined,
		Cancelled,
		CommError,
		Rejected,
		Timeout,
		NotSupported,
		doTranBad,
		noStatus
	}
	
	private static final Logger logger = Logger.getLogger(iUC285Util.class);
	
    private static String lastChecksum;
    
    private static HttpServer httpServer;
    private static int callbackPort;
    private static int servicePort;
    private static String socketResponse;
    private static boolean waitSocketResponse;
    private static boolean requestProcessing;
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    
    private static final HttpHandler myHttpHandler = exchange -> {
    	waitSocketResponse = false;
        if (exchange.getRequestMethod().equals("POST")) {
            boolean fail = false;
            String expectChecksum = exchange.getRequestHeaders().getFirst("X-MD5");

            try (InputStream is = exchange.getRequestBody()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                socketResponse = br.readLine();
                logger.info("iUC285Util socket response: " + socketResponse);
                String ignore;  // We should only have one line of json
                while ((ignore = br.readLine()) != null) {
                    logger.info("iUC285Util socket ignored response: " + ignore);
                }
            }

            // Check checksum
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.update(socketResponse.getBytes(StandardCharsets.ISO_8859_1));
                String actualChecksum = bytesToHexStr(md5.digest());
                if (!actualChecksum.equalsIgnoreCase(expectChecksum)) {
                    fail = true;
                    logger.info("iUC285Util socket response md5 desode fail");
                }
            } catch (NoSuchAlgorithmException e) {
                logger.error("iUC285Util socket NoSuchAlgorithmException: ", e);
            }

            if (!fail) {
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(400, -1);
            }
        } else {
            // Unsupported method
            exchange.sendResponseHeaders(405, -1);
            logger.info("iUC285Util Unsupported response method: " + exchange.getRequestMethod());
        }
        exchange.getResponseBody().close();
    };
   
    private iUC285Util() {}
    
    public static void stopListeningCallback() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }
    
    public static Status getStatus(JSONObject response) {
    	if(response == null || !response.has("STATUS")) {
    		return Status.noStatus;
    	} else {
    		switch(response.getString("STATUS")) {
	    		case "Approved":
	    			return Status.Approved;
	    		case "Declined":
	    			return Status.Declined;
	    		case "Cancelled":
	    			return Status.Cancelled;
	    		case "CommError":
	    			return Status.CommError;
	    		case "Rejected":
	    			return Status.Rejected;
	    		case "Timeout":
	    			return Status.Timeout;
	    		case "NotSupported":
	    			return Status.NotSupported;
	    		case "doTranBad":
	    			return Status.doTranBad;
    			default:
    				return Status.noStatus;
    		}
    	}
    }
    
    public static JSONObject doCardRead() {
    	return asyncSendRequest("{\"AMT\":0,\"ECRREF\":\"\",\"CARDTYPE\":\"\",\"CMD\":\"READ_CARD\",\"TYPE\":\"EDC\"}");
    }
    
    public static JSONObject doSale(TranModel tran, long amount) {
//    	 String request = String.format("{\"AMT\":%d,\"ECRREF\":\"%s\",\"CARDTYPE\":\"\",\"CMD\":\"SALE\",\"TYPE\":\"EDC\"}", amount, "AABB123123");
    	String request = String.format("{\"AMT\":%d,\"ECRREF\":\"%s\",\"CARDTYPE\":\"\",\"CMD\":\"SALE\",\"TYPE\":\"EDC\"}", amount, tran.getReceiptNo());
    	return asyncSendRequest(request);
    }
    
    public static JSONObject doVoid(TranModel tran) {
        String request = String.format("{\"TRACE\":\"%s\",\"CMD\":\"VOID\",\"TYPE\":\"EDC\"}", tran.getReceiptNo());
    	return asyncSendRequest(request);
    }
    
    public static JSONObject doVoid(String receiptNo) {
        String request = String.format("{\"TRACE\":\"%s\",\"CMD\":\"VOID\",\"TYPE\":\"EDC\"}", receiptNo);
    	return asyncSendRequest(request);
    }
    
    // Get LAST Record
    public static JSONObject doRetrieval() {
    	return asyncSendRequest("{\"TRACE\":\"LAST\",\"CMD\":\"RETRIEVAL\",\"TYPE\":\"EDC\"}");
    }
    
    public static JSONObject doRetrieval(TranModel tran) {
        String request = String.format("{\"TRACE\":\"%s\",\"CMD\":\"RETRIEVAL\",\"TYPE\":\"EDC\"}", tran.getReceiptNo());
    	return asyncSendRequest(request);
    }
    
    public static JSONObject doRetrieval(String receiptNo) {
        String request = String.format("{\"TRACE\":\"%s\",\"CMD\":\"RETRIEVAL\",\"TYPE\":\"EDC\"}", receiptNo);
    	logger.info("iUC285Util doRetrieval receiptNo: " + receiptNo);
    	logger.info("iUC285Util doSale request: " + request);
    	return asyncSendRequest(request);
    }
    
    public static JSONObject doGetTotal(String type) {
    	/*
    	 * TYPE:
    	 * "EDC" - for all acquirers
    	 * "VMJ" - for VMJ or VM acquirer only 
    	 * "AE" - for Amex acquirer only
    	 * "CUP" - for CUP acquirer only
    	 */
    	String request = String.format("{\"CMD\":\"TOTAL\",\"TYPE\":\"%s\"}", type);
    	return asyncSendRequest(request);
    }
    
    public static JSONObject doSettlement(String type) {
    	/*
    	 * TYPE:
    	 * "EDC" - for all acquirers
    	 * "VMJ" - for VMJ or VM acquirer only 
    	 * "AE" - for Amex acquirer only
    	 * "CUP" - for CUP acquirer only
    	 */
    	String request = String.format("{\"CMD\":\"SETTLEMENT\",\"TYPE\":\"%s\"}", type);
    	return asyncSendRequest(request);
    }
     
    public static void startListeningCallback(int servicePort, int callbackPort) {
    	waitSocketResponse 		= false;
    	requestProcessing		= false;
    	httpServer 				= null;
    	iUC285Util.servicePort 	= servicePort;
    	iUC285Util.callbackPort	= callbackPort;
    	
        try {
        	logger.info("iUC285Util start create socket server");
			httpServer = HttpServer.create(new InetSocketAddress(callbackPort), 1);
			httpServer.setExecutor(null);
	        httpServer.createContext("/", myHttpHandler);
	        httpServer.start();
		} catch (IOException e) {
			logger.error("iUC285Util create socket with IOException: ", e);
		}
        
    }
    
    private static ArrayList<String> sendRequest(String content) {
    	if(!waitSocketResponse) {
    		waitSocketResponse = true;
    		URL url;
    		String checksum;
			try {
				url = new URL(String.format("http://localhost:%d/", servicePort));
				MessageDigest md5 = MessageDigest.getInstance("MD5");
	    	    md5.update(content.getBytes(StandardCharsets.ISO_8859_1));
	    	    checksum = bytesToHexStr(md5.digest());
			} catch (MalformedURLException e) {
				logger.error("Failed to forme URL... ", e);
	    		waitSocketResponse = false;
	    		iUC285Util.restartUsbAndEftpayment();
				return null;
			} catch (NoSuchAlgorithmException e) {
				logger.error("Failed MD5 encript... ", e);
	    		waitSocketResponse = false;
	    		iUC285Util.restartUsbAndEftpayment();
				return null;
			}

    	    
	        try {
	            HttpURLConnection con = (HttpURLConnection) url.openConnection();
	            con.setReadTimeout(60000); // 60s
	            con.setInstanceFollowRedirects(false);
	            con.setDoOutput(true);
	
	            con.setRequestMethod("POST");
	            con.setRequestProperty("X-MD5", checksum);
	            con.setFixedLengthStreamingMode(content.getBytes(StandardCharsets.ISO_8859_1).length);
	            con.setRequestProperty("Content-Type", "text/plain");
	
	            try (OutputStream os = con.getOutputStream()) {
	                os.write(content.getBytes(StandardCharsets.ISO_8859_1));
	            }
	
	            int respCode 				= con.getResponseCode();
	            ArrayList<String> response 	= new ArrayList<String>();
	            
	            if (respCode == HttpURLConnection.HTTP_OK) {
	                try (InputStream is = con.getInputStream()) {
	                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
	                    String aLine;
	                    while ((aLine = br.readLine()) != null) {
	                        response.add(aLine);
	                    } 
	                } catch (Exception e) {
	    				logger.error("Failed with IOException at send http request  ... ", e);
	                }
					logger.info("iUC285Util HTTP request Success: " + response.toString());
	            } else {
	            	waitSocketResponse = false;
//	                try (InputStream is = con.getErrorStream()) {
	                try (InputStream is = con.getInputStream()) {
	                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
	                    String aLine;
	                    while ((aLine = br.readLine()) != null) {
	                    	response.add(aLine);
	                    }
	                } catch (Exception e) {
	    				logger.error("Failed with IOException at send http request  ... ", e);
	                }
					logger.info("iUC285Util HTTP request Fail: " + response.toString());
					restartUsbAndEftpayment();
	            }
	            con.disconnect();
	            return response;
	        } catch (IOException e) {
				logger.error("Failed at send HTTP request with IOException", e);
	    		waitSocketResponse = false;
				return null;
	        }
    	} else {
    		return null;
    	}
    }
   
    private static JSONObject asyncSendRequest(String request) {
    	int count 		= 0;
    	int maxCount 	= 150; // 150 * 100ms = 15s
		JSONObject response = null;
      	if(!requestProcessing && !waitSocketResponse) {
	    	try {
	      		requestProcessing = true;
	      		logger.info("asyncSendRequest: " + request);
	      		sendRequest(request);
		        while(true) {
		        	count++;
		        	try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						  logger.error("iUC285Util socket request thread sleep with InterruptedException: ", e);
					}
		        	
		        	if(waitSocketResponse == false) {
		        		response		= socketResponse != null ? new JSONObject(socketResponse) : null;
		        		socketResponse	= null;
		            	break;
		        	} else if(count >= maxCount) {
		            	break;
		        	}
		        }
	    	} catch (JSONException e){
	    		logger.error("iUC285Util create JSON Object From " + socketResponse + " with JSONException: ", e);
	    	} finally {
        		requestProcessing = false;
	    	}
      	}
    	return response;
    }
    
    private static String bytesToHexStr(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
    
    public static void restartUsbAndEftpayment() {
		try {
//			Process p = Runtime.getRuntime().exec("./usbreset /dev/bus/usb/002/003");
//			p.waitFor();
			Runtime.getRuntime().exec("systemctl restart eftpayment");
//			p.waitFor();
			logger.debug("restartUsbAndEftpayment");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("restartUsbAndEftpayment: ", e);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			logger.error("restartUsbAndEftpayment: ", e);
		}
    }
    
}