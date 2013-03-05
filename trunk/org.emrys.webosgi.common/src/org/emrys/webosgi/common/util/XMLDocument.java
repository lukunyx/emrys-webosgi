package org.emrys.webosgi.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author Leo Chang
 * @version 2010-8-6
 */
public class XMLDocument {
	private String xmlString;
	private String targetCharset;
	private String originalCharset = null;

	public XMLDocument(File file) throws IllegalArgumentException,
			FileNotFoundException, IOException {
		this(new FileInputStream(file));
	}

	public XMLDocument(String xmlString) throws IllegalArgumentException {
		if (xmlString == null || xmlString.length() == 0)
			throw new IllegalArgumentException("Input string orrer!");
		this.xmlString = xmlString.replaceAll("\\s", " ");
		originalCharset = getCharset();
		if (originalCharset == null)
			throw new IllegalArgumentException("Input not xml format error!");
	}

	public XMLDocument(InputStream xmlStream) throws IllegalArgumentException,
			IOException {
		// Copy byte data at first.
		ByteArrayOutputStream bot = new ByteArrayOutputStream();
		while (xmlStream.available() > 0)
			bot.write(xmlStream.read());
		xmlStream.close();

		StringBuffer content = FileUtil.getContent(new ByteArrayInputStream(bot
				.toByteArray()), "ISO-8859-1");
		xmlString = content.toString();
		originalCharset = getCharset();
		if (originalCharset == null)
			throw new IllegalArgumentException("Input not xml format error!");

		if (!"ISO-8859-1".equalsIgnoreCase(originalCharset)) {
			content = FileUtil.getContent(new ByteArrayInputStream(bot
					.toByteArray()), originalCharset);
			xmlString = content.toString();
		}

		if (xmlString == null || xmlString.length() == 0)
			throw new IllegalArgumentException("Input load orrer!");

		this.xmlString = xmlString.replaceAll("\\s", " ");
	}

	public void setCharset(String charset) {
		this.targetCharset = charset;
	}

	public String getCharset() {
		if (!xmlString.startsWith("<?xml"))
			return null;

		if (originalCharset == null) {
			// <?xml version="1.0" encoding="UTF-8"?>
			try {
				Pattern p = Pattern.compile("encoding=\"([^\"]*)\"");
				Matcher m = p.matcher(this.xmlString);
				if (m.find()) {
					originalCharset = m.group(1);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return originalCharset;
	}

	public void clearNSDefine(String nsPrefix) {
		String m = nsPrefix;
		// (xsi:[^\\s]*|xmlns:xsi[^\\s]*|:jpdl|jpdl:)
		xmlString = xmlString.replaceAll("(" + m + ":|xmlns:" + m
				+ "[^\\s>]*|:" + m + "|" + m + ":)", "");
	}

	/**
	 * Remove all given namespace's prefix to make it as default.
	 * 
	 * @param nsPrefix
	 */
	public void makeNSAsDefault(String nsPrefix) {
		// If has default ns yet, not revert them temporarily.
		if (xmlString.contains("xmlns="))
			return;

		String m = nsPrefix;
		// (xsi:[^\\s]*|xmlns:xsi[^\\s]*|:jpdl|jpdl:)
		xmlString = xmlString.replaceAll(m + ":", "");
		xmlString = xmlString.replace("xmlns:" + m, "xmlns");
	}

	/**
	 * 在文档中搜索指定的元素,返回符合条件的元素数组.
	 * 
	 * @param tagName
	 *            String include the prefix of namespace, like: ti:template-def
	 * @return String[]
	 */
	public String[] getElementsByTag(String tagName) {
		Pattern p = Pattern.compile("<" + tagName + "[^>]*?((>.*?</" + tagName
				+ ">)|(/>))");
		Matcher m = p.matcher(this.xmlString);
		ArrayList<String> al = new ArrayList<String>();
		while (m.find())
			al.add(m.group());
		String[] arr = al.toArray(new String[al.size()]);
		al.clear();
		return arr;
	}

	public String removeChildElement(String childElementStr,
			String parentElementStr) {
		Pattern p = Pattern.compile(childElementStr);
		Matcher m = p.matcher(parentElementStr);
		String newParentElementStr = m.replaceAll(" ");

		p = Pattern.compile(parentElementStr);
		m = p.matcher(xmlString);
		xmlString = m.replaceAll(newParentElementStr);
		return newParentElementStr;
	}

	public String setAttribute(String attrName, String newValue,
			String elementStr) {
		Pattern p = Pattern.compile("<[^>]+>");
		Matcher m = p.matcher(elementStr);

		String orginalEleHead = m.find() ? m.group() : "";
		String tmp = new String(orginalEleHead);

		p = Pattern.compile("\\s+" + attrName + "\\s*=\\s*\"([^\"]+)\"");
		m = p.matcher(tmp);

		if (m.find())
			tmp = m.replaceAll(" " + attrName + "=\"" + newValue + "\"");
		else {
			if (tmp.endsWith("/>"))
				tmp = tmp.substring(0, tmp.length() - 2) + " " + attrName
						+ "=\"" + newValue + "\"/>";
			else
				tmp = tmp.substring(0, tmp.length() - 1) + " " + attrName
						+ "=\"" + newValue + "\">";
		}

		// Replace
		p = Pattern.compile(orginalEleHead);
		m = p.matcher(elementStr);
		tmp = m.replaceAll(tmp);

		/*
		 * elementStr.replaceAll("\\.", "\\."); elementStr.replaceAll("\\?",
		 * "\\?"); elementStr.replaceAll("\\+", "\\+");
		 * elementStr.replaceAll("\\+", "\\+"); p = Pattern.compile(elementStr);
		 * m = p.matcher(xmlString); xmlString = m.replaceAll(tmp);
		 */
		xmlString = xmlString.replace(elementStr, tmp);
		return tmp;
	}

	/**
	 * 用xpath模式提取元素,以#为分隔符 如 ROOT#PARENT#CHILD表示提取ROOT元素下的PARENT元素下的CHILD元素
	 * 
	 * @param singlePath
	 *            String
	 * @return String
	 */
	public String getElementBySinglePath(String singlePath) {
		String[] path = singlePath.split("#");
		String lastTag = path[path.length - 1];
		String tmp = "(<" + lastTag + "[^>]*?((>.*?</" + lastTag + ">)|(/>)))";
		// 最后一个元素,可能是<x>v</x>形式或<x/>形式
		for (int i = path.length - 2; i >= 0; i--) {
			lastTag = path[i];
			tmp = "<" + lastTag + ">.*" + tmp + ".*</" + lastTag + ">";
		}
		Pattern p = Pattern.compile(tmp);
		Matcher m = p.matcher(this.xmlString);
		if (m.find()) {
			return m.group(1);
		}
		return "";
	}

	/**
	 * 用xpath模式提取元素从多重元素中获取指批定元素,以#为分隔符 元素后无索引序号则默认为0: ROOT#PARENT[2]#CHILD[1]
	 * 
	 * @param singlePath
	 *            String
	 * @return String
	 */
	public String getElementByMultiPath(String singlePath) {
		try {
			String[] path = singlePath.split("#");
			String input = this.xmlString;
			String[] ele = null;
			for (int i = 0; i < path.length; i++) {
				Pattern p = Pattern.compile("(\\w+)(\\[(\\d+)\\])?");
				Matcher m = p.matcher(path[i]);
				if (m.find()) {
					String tagName = m.group(1);
					System.out.println(input + "----" + tagName);
					int index = (m.group(3) == null) ? 0 : new Integer(m
							.group(3)).intValue();
					ele = getElementsByTag(input, tagName);
					input = ele[index];
				}
			}
			return input;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 在给定的元素中搜索指定的元素,返回符合条件的元素数组.对于不同级别的同名元素限制作用,即可以
	 * 搜索元素A中的子元素C.而对于元素B中子元素C则过虑,通过多级限定可以准确定位.
	 * 
	 * @param parentElementString
	 *            String
	 * @param tagName
	 *            String
	 * @return String[]
	 */
	public static String[] getElementsByTag(String parentElementString,
			String tagName) {
		Pattern p = Pattern.compile("<" + tagName + "[^>]*?((>.*?</" + tagName
				+ ">)|(/>))");
		Matcher m = p.matcher(parentElementString);
		ArrayList<String> al = new ArrayList<String>();
		while (m.find())
			al.add(m.group());
		String[] arr = al.toArray(new String[al.size()]);
		al.clear();
		return arr;
	}

	/**
	 * 从指定的父元素中根据xpath模式获取子元素,singlePath以#为分隔符 如
	 * ROOT#PARENT#CHILD表示提取ROOT元素下的PARENT元素下的CHILD元素
	 * 
	 * @param parentElementString
	 *            String
	 * @param singlePath
	 *            String
	 * @return String
	 */
	public static String getElementBySinglePath(String parentElementString,
			String singlePath) {
		String[] path = singlePath.split("#");
		String lastTag = path[path.length - 1];
		String tmp = "(<" + lastTag + "[^>]*?((>.*?</" + lastTag + ">)|(/>)))";
		// 最后一个元素,可能是<x>v</x>形式或<x/>形式
		for (int i = path.length - 2; i >= 0; i--) {
			lastTag = path[i];
			tmp = "<" + lastTag + ">.*" + tmp + ".*</" + lastTag + ">";
		}
		Pattern p = Pattern.compile(tmp);
		Matcher m = p.matcher(parentElementString);
		if (m.find()) {
			return m.group(1);
		}
		return "";
	}

	/**
	 * 用xpath模式提取元素从指定的多重元素中获取指批定元素,以#为分隔符
	 * 
	 * @param parentElementString
	 *            String
	 * @param singlePath
	 *            String
	 * @return String
	 */
	public static String getElementByMultiPath(String parentElementString,
			String singlePath) {
		try {
			String[] path = singlePath.split("#");
			String input = parentElementString;
			String[] ele = null;
			for (int i = 0; i < path.length; i++) {
				Pattern p = Pattern.compile("(\\w+)(\\[(\\d+)\\])?");
				Matcher m = p.matcher(path[i]);
				if (m.find()) {
					String tagName = m.group(1);
					int index = (m.group(3) == null) ? 0 : new Integer(m
							.group(3)).intValue();
					ele = getElementsByTag(input, tagName);
					input = ele[index];
				}
			}
			return input;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 在给定的元素中获取所有属性的集合.该元素应该从getElementsByTag方法中获取
	 * 
	 * @param elementString
	 *            String
	 * @return HashMap
	 */
	public HashMap<String, String> getAttributes(String elementString) {
		HashMap hm = new HashMap<String, String>();
		Pattern p = Pattern.compile("<[^>]+>");
		Matcher m = p.matcher(elementString);
		String tmp = m.find() ? m.group() : "";
		p = Pattern.compile("(\\w+)\\s*=\\s*\"([^\"]+)\"");
		m = p.matcher(tmp);
		while (m.find()) {
			hm.put(m.group(1).trim(), m.group(2).trim());
		}
		return hm;
	}

	/**
	 * 在给定的元素中获取指定属性的值.该元素应该从getElementsByTag方法中获取
	 * 
	 * @param elementString
	 *            String
	 * @param attributeName
	 *            String
	 * @return String
	 */
	public static String getAttribute(String elementString, String attributeName) {
		Pattern p = Pattern.compile("<[^>]+>");
		Matcher m = p.matcher(elementString);
		String tmp = m.find() ? m.group() : "";
		p = Pattern.compile("(\\w+)\\s*=\\s*\"([^\"]+)\"");
		m = p.matcher(tmp);
		while (m.find()) {
			if (m.group(1).trim().equals(attributeName))
				return m.group(2).trim();
		}
		return "";
	}

	/**
	 * 获取指定元素的文本内容
	 * 
	 * @param elementString
	 *            String
	 * @return String
	 */
	public static String getElementText(String elementString) {
		Pattern p = Pattern.compile(">([^<>]*)<");
		Matcher m = p.matcher(elementString);
		if (m.find()) {
			return m.group(1).trim();
		}
		return "";
	}

	public static String getElementName(String elementString) {
		Pattern p = Pattern.compile("<[^>]+>");
		Matcher m = p.matcher(elementString);
		String tmp = m.find() ? m.group() : "";
		p = Pattern.compile("(<\\s*)([\\:\\-\\w]+)([^>]*)");
		m = p.matcher(tmp);

		if (m.find())
			return m.group(2).trim();

		return "";
	}

	public static String[] getElementChildrenByTag(String tagName,
			String parentElementStr) {
		List<String> result = new ArrayList<String>();
		String[] children;
		try {
			children = getElementChildren(parentElementStr);
			for (String child : children) {
				if (tagName.equals(getElementName(child)))
					result.add(child);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result.toArray(new String[result.size()]);
	}

	public static String[] getElementChildren(String elementString)
			throws Exception {
		List<String> children = new ArrayList<String>();

		String eleName = getElementName(elementString);
		// Remove all comments.
		elementString = elementString.replaceAll("<!--[^>]*>", "");
		String content = elementString.replaceAll("<\\s*" + eleName + "\\s*>",
				"").replaceAll("</\\s*" + eleName + "\\s*>", "");
		// System.out.println(content);

		Stack<Integer> stack = new Stack<Integer>();
		for (int i = 0; i < content.length(); i++) {
			if (content.charAt(i) == '<' && content.charAt(i + 1) != '/') {
				stack.push(i);
				continue;
			}

			if (content.charAt(i) == '/'
					&& (content.charAt(i + 1) == '>' || content.charAt(i - 1) == '<')) {
				do {
					i++;
				} while (content.charAt(i) != '>');
				if (stack.size() == 1)
					children.add(content.substring(
							stack.firstElement().intValue(), i + 1).trim());
				stack.pop();
			}
		}

		return children.toArray(new String[children.size()]);
	}

	public String getXmlString() {
		return xmlString.replaceAll(">\\s*", ">"
				+ System.getProperty("line.separator"));
	}
}
