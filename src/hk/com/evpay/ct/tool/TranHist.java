package hk.com.evpay.ct.tool;

import java.util.Vector;

import hk.com.cstl.evcs.model.TranModel;

public class TranHist {
	private Vector<TranModel> trans;

	public Vector<TranModel> getTrans() {
		if(trans == null) {
			trans = new Vector<TranModel>();
		}
		return trans;
	}

	public void setTrans(Vector<TranModel> trans) {
		this.trans = trans;
	}
	
	
}
