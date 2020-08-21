package hk.com.evpay.ct.i18n;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.UiUtil;

public class I18nImageLabel extends JLabel implements I18nSupport {
	public static String IMG_PATH = "img/";
	private String imageName;
	private String imageType;
	private Icon iconEn;
	private Icon iconZh;

	public I18nImageLabel(String imageName, String imageType) {
		super();
		this.imageName = imageName;
		this.imageType = imageType;
		iconEn = new ImageIcon(IMG_PATH + imageName + "_en." + imageType);
		iconZh = new ImageIcon(IMG_PATH + imageName + "_zh." + imageType);
		setHorizontalAlignment(SwingConstants.CENTER);
		updateIcon();
		
		UiUtil.debugUi(this);
	}

	private void updateIcon() {
		setIcon(LangUtil.isEnglish() ? iconEn : iconZh);
	}

	@Override
	public void langChanged() {
		updateIcon();
	}

	public String getImageName() {
		return imageName;
	}

	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

	public String getImageType() {
		return imageType;
	}

	public void setImageType(String imageType) {
		this.imageType = imageType;
	}

	public Icon getIconEn() {
		return iconEn;
	}

	public void setIconEn(Icon iconEn) {
		this.iconEn = iconEn;
	}

	public Icon getIconZh() {
		return iconZh;
	}

	public void setIconZh(Icon iconZh) {
		this.iconZh = iconZh;
	}

}
