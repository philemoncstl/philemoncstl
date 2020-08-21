package hk.com.cstl.evcs.ct;

import java.util.ArrayList;
import java.util.List;

import hk.com.cstl.evcs.ocpp.eno.PayMethod;

public class PayConfig {
	private List<PayMethodModel> methods;

	public PayConfig() {
		methods = new ArrayList<PayMethodModel>();
	}
	
	public List<PayMethodModel> getMethods() {
		return methods;
	}

	public void setMethods(List<PayMethodModel> methods) {
		this.methods = methods;
	}
	
	public List<PayMethodModel> getEnabledMethods(){
		List<PayMethodModel> list = new ArrayList<PayMethodModel>();
		for(PayMethodModel pm : methods) {
			if(pm.isEnabled()) {
				list.add(pm);
			}
		}
		
		return list;
	}
	
	public boolean isQrMethodIncluded() {
		List<PayMethodModel> list = getEnabledMethods();
		PayMethod m = null;
		for(PayMethodModel pm : list) {
			m = pm.getPayMethod();
			if(m == PayMethod.QR || m == PayMethod.WeChatPay || m == PayMethod.AliPay) {
				return true;
			}
		}
		
		return false;
	}
}
