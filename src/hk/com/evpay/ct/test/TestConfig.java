package hk.com.evpay.ct.test;

import hk.com.evpay.ct.CtConfig;
import hk.com.evpay.ct.util.CtUtil;

public class TestConfig {
	public static void main(String[] args) {
		CtConfig cfg = new CtConfig();
		CtUtil.saveConfig(cfg);
		System.out.println("Done");
	}
}
