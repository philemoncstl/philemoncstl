package hk.com.evpay.ct.i18n;

import hk.com.cstl.evcs.ct.PayMethodModel;

public class PayButton extends I18nButtonLabel{
	private PayMethodModel pmModel;
	
	public PayButton(PayMethodModel pm) {
		this("", 0, "img/" + pm.getIconName());
		pmModel = pm;
	}

	public PayButton(String msgCode, int leftPadding, String iconPath) {
		super(msgCode, leftPadding, iconPath);
	}
	
	@Override
	public String toString() {
		return this.pmModel.getPayMethod().toString();
	}

	public PayMethodModel getPmModel() {
		return pmModel;
	}

	public void setPmModel(PayMethodModel pmModel) {
		this.pmModel = pmModel;
	}
}
