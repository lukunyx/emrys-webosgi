/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.license;

import java.security.Key;
import java.security.Provider;
import java.security.Security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

/**
 * DES加密和解密工兄1�7,可以对字符串进行加密和解密操佄1�7 〄1�7
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-7-26
 */
public class DesUtils {
	private static String strDefaultKey = "";// 字符串默认键倄1�7
	private Cipher encryptCipher = null;// 加密工具
	private Cipher decryptCipher = null;// 解密工具

	/**
	 * 将byte数组转换为表礄1�7进制值的字符串， 如：byte[]{8,18}转换为：0813＄1�7 和public static byte[]
	 * hexStr2ByteArr(String strIn) 互为可�1�7�的转换过程
	 * 
	 * @param arrB
	 *            霄1�7要转换的byte数组
	 * @return 转换后的字符丄1�7
	 * 
	 */
	public static String byteArr2HexStr(byte[] arrB) {
		int iLen = arrB.length;
		StringBuffer sb = new StringBuffer(iLen * 2);// 每个byte用两个字符才能表示，扄1�7以字符串的长度是数组长度的两倄1�7
		for (int i = 0; i < iLen; i++) {
			int intTmp = arrB[i];
			while (intTmp < 0) {// 把负数转换为正数
				intTmp = intTmp + 256;
			}
			if (intTmp < 16) {// 小于0F的数霄1�7要在前面衄1�7
				sb.append("0");
			}
			sb.append(Integer.toString(intTmp, 16));
		}
		return sb.toString();
	}

	/**
	 * 将表礄1�7进制值的字符串转换为byte数组＄1�7 和public static String byteArr2HexStr(byte[] arrB)
	 * 互为可�1�7�的转换过程
	 * 
	 * @param strIn
	 *            霄1�7要转换的字符丄1�7
	 * @return 转换后的byte数组
	 */
	public static byte[] hexStr2ByteArr(String strIn) {
		byte[] arrB = strIn.getBytes();
		int iLen = arrB.length;
		byte[] arrOut = new byte[iLen / 2];// 两个字符表示丄1�7个字节，扄1�7以字节数组长度是字符串长度除仄1�7
		for (int i = 0; i < iLen; i = i + 2) {
			String strTmp = new String(arrB, i, 2);
			arrOut[i / 2] = (byte) Integer.parseInt(strTmp, 16);
		}
		return arrOut;
	}

	/**
	 * 默认构�1�7�方法，使用默认密钥
	 * 
	 * @throws Exception
	 */
	public DesUtils() throws Exception {
		this(strDefaultKey);
	}

	/**
	 * DES字符串加寄1�7 指定密钥构�1�7�方泄1�7
	 * 
	 * @param strKey
	 *            指定的密钄1�7
	 * @throws Exception
	 * @throws Exception
	 */
	public DesUtils(String strKey) throws Exception {
		// NOTE: com.sun.crypto.provider.SunJCE class not exists in IBM JVM.
		Class desAlgorithmProvider = null;
		try {
			desAlgorithmProvider = Class.forName("com.sun.crypto.provider.SunJCE");
		} catch (ClassNotFoundException e) {
			desAlgorithmProvider = Class.forName("com.ibm.crypto.provider.IBMJCE");
		}
		if (desAlgorithmProvider != null) {
			Security.addProvider((Provider) desAlgorithmProvider.newInstance());
			try {
				Key key = getKey(strKey.getBytes());
				encryptCipher = Cipher.getInstance("DES");
				encryptCipher.init(Cipher.ENCRYPT_MODE, key);
				decryptCipher = Cipher.getInstance("DES");
				decryptCipher.init(Cipher.DECRYPT_MODE, key);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 加密字节数组
	 * 
	 * @param arrB
	 *            霄1�7加密的字节数组1�7
	 * @return 加密后的字节数组
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws Exception
	 */
	public byte[] encrypt(byte[] arrB) throws IllegalBlockSizeException,
			BadPaddingException {
		return encryptCipher.doFinal(arrB);
	}

	/**
	 * 加密字符丄1�7
	 * 
	 * @param strIn
	 *            霄1�7加密的字符串
	 * @return 加密后的字符丄1�7
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws Exception
	 */
	public String encrypt(String strIn) throws IllegalBlockSizeException,
			BadPaddingException {
		return byteArr2HexStr(encrypt(strIn.getBytes()));
	}

	/**
	 * 解密字节数组
	 * 
	 * @param arrB
	 *            霄1�7解密的字节数组1�7
	 * @return 解密后的字节数组
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws Exception
	 */
	public byte[] decrypt(byte[] arrB) throws IllegalBlockSizeException,
			BadPaddingException {
		return decryptCipher.doFinal(arrB);
	}

	/**
	 * 解密字符丄1�7
	 * 
	 * @param strIn
	 *            霄1�7解密的字符串
	 * @return 解密后的字符丄1�7
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws Exception
	 */
	public String decrypt(String strIn) throws IllegalBlockSizeException,
			BadPaddingException {
		return new String(decrypt(hexStr2ByteArr(strIn.trim())));
	}

	/**
	 * 从指定字符串生成密钥，密钥所霄1�7的字节数组长度为8佄1�7 不足8位时后面衄1�7�超凄1�7�只取前8佄1�7
	 * 
	 * @param arrBTmp
	 *            构成该字符串的字节数组1�7
	 * @return 生成的密钄1�7
	 * @throws java.lang.Exception
	 */
	private Key getKey(byte[] arrBTmp) throws Exception {
		byte[] arrB = new byte[8];// 创建丄1�7个空的1�7�字节数组（默认值为0＄1�7
		for (int i = 0; i < arrBTmp.length && i < arrB.length; i++) {// 将原始字节数组转换为8佄1�7
			arrB[i] = arrBTmp[i];
		}
		Key key = new javax.crypto.spec.SecretKeySpec(arrB, "DES");// 生成密钥
		return key;
	}
}