/*******************************************************************************
 * GNU Lesser General Public License (LGPL) version 2.1
 * 
 * Copyright (c) 2014 Chamber of Security Industry.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package hk.com.evpay.ct.i18n;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;

import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

/**
 * 11-01-2008
 * @author ck
 *
 */
public class I18NGenerator {
	private static final Logger logger = Logger.getLogger(I18NGenerator.class);
	
	private static String I18N_TEMPLATE = null;
	
	private File source;
	
	private String destination;
	
	private final WorkbookSettings wbs = new WorkbookSettings();

	private final Workbook workbook;

	private static final String NEW_LINE = System.getProperty("line.separator");

	
	public I18NGenerator(File source, String destination) throws BiffException, FileNotFoundException, IOException{
		this.source = source;
		this.destination = destination;
		wbs.setEncoding("UTF8");
		workbook = Workbook.getWorkbook(new BufferedInputStream(new FileInputStream(this.source)), wbs);
	}
	
	private void generateXml(Sheet sheet, int keyCol, int msgCol, String dest) {

		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append(NEW_LINE);
		sb.append("<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">");
		sb.append(NEW_LINE);
		sb.append("<properties>");
		sb.append(NEW_LINE);
		
		String category = null;
		//read the content start from 2nd row
		for(int r = 1; r < sheet.getRows(); r ++) {
			category = sheet.getCell(0, r).getContents();
			String key = sheet.getCell(keyCol, r).getContents();
			
			if(category != null && !"".equals(category)) {
				sb.append(NEW_LINE);
			}				
			
			if(key == null || "".equals(key)) {
				continue;
			}
			//<entry key="key">Value</entry>
			String content = sheet.getCell(msgCol, r).getContents();
			content = StringEscapeUtils.escapeXml(content);
			sb.append("<entry key=\"" + key + "\">" + content + "</entry>" + NEW_LINE);
		}
		
		//end tag
		sb.append("</properties>");
	
		//write the default properties files
		try {
			writeProperty(dest, sb);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

	public void generate() {
		Sheet sheet = workbook.getSheet(0);
		logger.debug("Columns:" + sheet.getColumns());
		logger.debug("Rows:" + sheet.getRows());
		
		generateXml(sheet, 1, 2, destination + "msg_en.xml");
		generateXml(sheet, 1, 3, destination + "msg_zh.xml");
	}

	private void writeProperty(String fileName, StringBuffer content) throws IOException {
		//write the properties file
		PrintWriter pw = new PrintWriter(fileName, "UTF8");
		pw.write(content.toString());
		pw.flush();
		pw.close();
		System.out.println("Msg saved to:" + fileName);
	}
		
	public static void main(String[] args) {
		//String path = System.getProperty("user.dir") + "\\src\\hk\\com\\evpay\\ct\\i18n\\";
		String path = System.getProperty("user.dir") + "/src/hk/com/evpay/ct/i18n/";
		logger.debug("path:" + path);
		args = new String[]{
				path + "CT_MSG.xls",
				path
		};
		
		/*if(args.length != 2) {
			logger.debug("Usage: I18NGenerator [source] [destination]");
			return;
		}*/
		
		
		try {
			I18NGenerator generator = new I18NGenerator(new File(args[0]), args[1]);
			generator.generate();

			logger.debug("Done.");
			System.out.println("Done");
		} catch (BiffException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
