package hk.com.evpay.ct;

import java.util.Date;

import org.apache.log4j.Logger;

import com.ckzone.octopus.ExtraInfo;
import com.ckzone.octopus.OctReturn;
import com.ckzone.octopus.PollDeductReturn;
import com.ckzone.octopus.PollEx;
import com.ckzone.octopus.util.OctUtil;
import com.ckzone.util.StringUtil;

import hk.com.cstl.evcs.model.TranModel;
import hk.com.evpay.ct.util.CtUtil;

public class CommonPanelOctopus extends CommonPanel{
	private static final Logger logger = Logger.getLogger(CommonPanelOctopus.class);
	
	protected boolean polling = false;
	protected String error100022CardNo = null;

	public CommonPanelOctopus(CtrlPanel pnlCtrl) {
		super(pnlCtrl);
	}
	
	public void setOctopusExtraInfo(TranModel tran, ExtraInfo info) {
		if(tran == null || info == null) {
			logger.warn("tran:" + (tran == null) + ", info:" + (info == null));
			return;
		}
		info.processData();
		tran.setOctopusLastAddValueDate(info.getLastAddValueDate());
		tran.setOctopusLastAddValueType(info.getLastAddValueType());
		logger.info("LastAddValueDate:" + tran.getOctopusLastAddValueDate() + ", LastAddValueType:" + info.getLastAddValueType());
	}
	
	public int setReceiptNo(TranModel tran) {
		int receiptNo = CtUtil.getReceiptNo();
		tran.setReceiptNo(String.valueOf(receiptNo));
		tran.setTranId(receiptNo);
		
		return receiptNo;
	}
	
	public boolean checkPolling() {
		polling = isShowing() || pnlCtrl.isShowingErrorMessage();
		return polling;
	}
	
	public synchronized PollDeductReturn poll(int deductAmt, String startCardNo, boolean getExtraInfo, TranModel tran) throws Exception{
		logger.info("polling now, deductAmt:" + deductAmt);
		PollDeductReturn res = new PollDeductReturn(deductAmt);

		long timeout = System.currentTimeMillis() + CtUtil.getConfig().getOctopusPollTimeoutSec() * 1000;
		int count = 0;
		OctReturn octReturn = null;
		
		do {
			if(OctUtil.isOnline()) {
				logger.info("polling:" + (++count) + ", amt:" + deductAmt + ", startCardNo:" + startCardNo);			
				octReturn = OctUtil.pollEx();
				if (octReturn.getReturnCode() > 100000) {
					handleOctopusError(octReturn.getReturnCode());
				} 
				else {
					res.setPollReturn(octReturn);
					PollEx pollData = (PollEx) octReturn.getReturnData();
					pollData.processData();
					
					if(!StringUtil.isEmpty(error100022CardNo)) {
						//tap diff card after retry for error 100022
						if(!error100022CardNo.equals(pollData.getNewCardId())) {
							showErrorMessage("ERR100022AfterRetry", error100022CardNo);
							continue;
						}
					}
					
					//check for same card
					boolean sameCard = StringUtil.isEmpty(startCardNo) || pollData.getNewCardId().equals(startCardNo) || pollData.getOldCardId().equals(startCardNo);		
					res.setSameCard(sameCard);
					logger.info("Old Card No.:" + pollData.getOldCardId() + ", Old Card IDm:" + pollData.getOldIdm() + 
							", " +  "New Card No.:" + pollData.getNewCardId() + ", New Card IDm:" + pollData.getNewIdm() + 
							", Acc Balance:" + CtUtil.getAmountStr(octReturn.getReturnCode()) + ", Same Card:" + sameCard + 
							", Smart Oct:" + pollData.isSmartOctopus());
					
					//start charging
					if(StringUtil.isEmpty(startCardNo)) {
						tran.setOctopusNo(pollData.getNewCardId());
					}
					//2018-05-04, also record old Octopus no if card replaced after charging started
					//stop charging
					else {
						if(sameCard) {
							if(!StringUtil.isEmpty(pollData.getOldCardId())) {
								tran.setOctopusNoOld(startCardNo);
								tran.setOctopusNo(pollData.getNewCardId());
							}
						}
					}
					
					
					if(sameCard) {
						if(deductAmt >= 0) {	//CK @ 20161108, also handle case with amt=0
							String receiptNo = "";
							if(StringUtil.isEmpty(tran.getReceiptNo())) {
								int no = setReceiptNo(tran);
								receiptNo = String.valueOf(no % 10000);
							}
							else {
								receiptNo = tran.getReceiptNo().substring(tran.getReceiptNo().length() - 4);
							}
		
							logger.info("Deduct Amt:" + deductAmt + ", returnCode:" + octReturn.getReturnCode());
							long timeMs = System.currentTimeMillis();
							octReturn = OctUtil.deduct(deductAmt, receiptNo, pollData.getNewCardId(), tran.getTranId());
							
							if(octReturn.getReturnCode() > 100000) {								
								if(octReturn.getReturnCode() == 100022 || octReturn.getReturnCode() == 100025) {
									error100022CardNo = pollData.getNewCardId();
									
									long timeLeft = timeout - System.currentTimeMillis();
									if(timeLeft < 28000) {
										timeout = Math.max(timeout, System.currentTimeMillis() + 28000);	//extend for 28 sec
										logger.info("Extend the timeout to 28 seconds for retry.");
									}
									else {
										logger.info("No need to extend timeout");
									}
									logger.info("timeout after (ms):" + (timeout - System.currentTimeMillis()));
									
									//lock go to home
									pnlCtrl.goHomeLock(28000);
								}
								
								handleOctopusError(octReturn.getReturnCode());
								
								continue;
							}
							else {
								tran.setTranDttm(new Date(timeMs));
								res.setDeductReturn(octReturn);
							}
						}
						
						//also get extra info if poll or deduct successfully
						if(getExtraInfo) {
							octReturn = OctUtil.getExtraInfo();
							if(octReturn.getReturnCode() > 100000) {
								logger.info("Failed to get extra info:" + octReturn.getReturnCode() + ", try again now");
								octReturn = OctUtil.getExtraInfo();						
							}
							
							if(octReturn.getReturnCode() <= 100000){
								res.setExtraInfoReturn(octReturn);
							}
							else {
								logger.info("Failed to get extra info again:" + octReturn.getReturnCode());
							}
						}
						
						break;
					}
					else {
						showPresentSameCard(startCardNo);
						timeout = Math.max(timeout, System.currentTimeMillis() + 10000);	//extend for 10 sec
						logger.info("timeout after (ms):" + (timeout - System.currentTimeMillis()));
						pnlCtrl.goHomeCountDownExtend(10000);
					}
								
				}
			}
			else {
				showErrorMessage("ERROctopusOthers");
				logger.info("Octopus offline, retry after 1000 ms");
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
				}
			}
			checkPolling();
		} while (polling && timeout >= System.currentTimeMillis());
		
		
		//No card present, pass the last poll return
		if(res.getPollReturn() == null) {
			res.setPollReturn(octReturn);		
		}
		
		if(deductAmt > 0 && res.getDeductReturn() == null && error100022CardNo != null) {
			logger.info("No card present, 100022card no:" + error100022CardNo);
			if(error100022CardNo != null) {
				OctUtil.logger.info("Cancel by the system");
			}
		}
		
		return res;
	}
	

	private synchronized void handleOctopusError(int returnCode) {
		switch (returnCode) {		
		case 100019:	//Card is blocked
		case 100021:	//The last add value date is greater than 1000 days
		case 100024:	//This card not accepted
		case 100035:	//Card recover error
			showErrorMessageOctopus(returnCode);
			//OctUtil.playFailTone();
			break;
		case 100016:	//Card read error
		case 100017:	//Card write error
		case 100020:	//Card is not found after Poll (deduct/add)
		case 100034:	//Card authentication error	
			showErrorMessageOctopus(returnCode);
			break;
		case 100022:	//Incomplete transaction.
		case 100025:	//Incomplete transaction.
			showErrorMessageOctopus(returnCode);			
			break;
		case 100048:	//Insufficient fund
			showErrorMessageOctopus(returnCode);
			break;
		case 100049:	//Remaining value exceeds limit
			showErrorMessageOctopus(returnCode);
			break;			
		case 100032:	//No card is detected. //Do Nothing
			break;			
		case 100023:	//Transaction Log full
			OctUtil.xFile();
			break;
		case 100050:	//Quota exceeded, SP to contact Octopus representative for assistance
			break;
		case 100051:	//Invalid Controller ID
			showErrorMessageOctopus(returnCode);
			break;
		case 100066: 	// System time error
			OctUtil.syncTime();
			break;
		default:
			showErrorMessage("ERROctopusOthers");
		}
		
		try {
			Thread.sleep(100);
		} catch (Exception e) {
			logger.error(e);
		}
		
		if(returnCode == 100001 || returnCode == 100005) {
			logger.info("handleOctopusError:" + returnCode);
			OctUtil.initComm(0, 0, 0);
		}
	}
	
	public void showErrorMessageOctopus(int msgCode) {
		if(StringUtil.isEmpty(error100022CardNo)) {
			showErrorMessage("ERR" + msgCode);
		}
		else {
			showErrorMessage("ERR" + msgCode + "AfterRetry", error100022CardNo);
		}
	}
	
	public void showErrorMessage(String msgCode) {
		pnlCtrl.showErrorMessage(msgCode, null);
	}
	
	public void showErrorMessage(String msgCode, String parm) {
		pnlCtrl.showErrorMessage(msgCode, parm);
	}
	
	public void showPresentSameCard(String msgCode, String cardNo) {
		String displayedCardNo = cardNo.length() > 5 ? cardNo.substring(cardNo.length() - 5) : cardNo;
		displayedCardNo = "XXXXXXXXXXXX".substring(0, cardNo.length() - 5) + displayedCardNo;
		logger.info("cardNo:" + cardNo + ", displayedCardNo:" + displayedCardNo);
		
		pnlCtrl.showErrorMessage(msgCode, displayedCardNo);
	}
	
	public void showPresentSameCardContactless(String cardNo) {
		pnlCtrl.showErrorMessage("presentSameCardContactless", cardNo);
	}
	
	public void showPresentSameCard(String cardNo) {
		showPresentSameCard("presentSameCard", cardNo);
	}

	public static void showPresentSameCard2(String cardNo) {
		String displayedCardNo = cardNo.length() > 5 ? cardNo.substring(cardNo.length() - 5) : cardNo;
		displayedCardNo = "XXXXXXXXXXXX".substring(0, cardNo.length() - 5) + displayedCardNo;
		System.out.println("cardNo:" + cardNo + ", displayedCardNo:" + displayedCardNo + ", leng:" + displayedCardNo.length());
		
	}
	
	public static void main(String[] args) {
		showPresentSameCard2("38027652");
		showPresentSameCard2("938027652");
		showPresentSameCard2("1017454666");	
		showPresentSameCard2("1234567890123456");
	}
}
