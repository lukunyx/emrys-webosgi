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
 * DES鍔犲瘑鍜岃В瀵嗗伐鍏�,鍙互瀵瑰瓧绗︿覆杩涜鍔犲瘑鍜岃В瀵嗘搷浣� 銆�
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-7-26
 */
public class DesUtils {
	private static String strDefaultKey = "";// 瀛楃涓查粯璁ら敭鍊�
	private Cipher encryptCipher = null;// 鍔犲瘑宸ュ叿
	private Cipher decryptCipher = null;// 瑙ｅ瘑宸ュ叿

	/**
	 * 灏哹yte鏁扮粍杞崲涓鸿〃绀�杩涘埗鍊肩殑瀛楃涓诧紝 濡傦細byte[]{8,18}杞崲涓猴細0813锛� 鍜宲ublic static byte[]
	 * hexStr2ByteArr(String strIn) 浜掍负鍙�嗙殑杞崲杩囩▼
	 * 
	 * @param arrB
	 *            闇�瑕佽浆鎹㈢殑byte鏁扮粍
	 * @return 杞崲鍚庣殑瀛楃涓�
	 * 
	 */
	public static String byteArr2HexStr(byte[] arrB) {
		int iLen = arrB.length;
		StringBuffer sb = new StringBuffer(iLen * 2);// 姣忎釜byte鐢ㄤ袱涓瓧绗︽墠鑳借〃绀猴紝鎵�浠ュ瓧绗︿覆鐨勯暱搴︽槸鏁扮粍闀垮害鐨勪袱鍊�
		for (int i = 0; i < iLen; i++) {
			int intTmp = arrB[i];
			while (intTmp < 0) {// 鎶婅礋鏁拌浆鎹负姝ｆ暟
				intTmp = intTmp + 256;
			}
			if (intTmp < 16) {// 灏忎簬0F鐨勬暟闇�瑕佸湪鍓嶉潰琛�
				sb.append("0");
			}
			sb.append(Integer.toString(intTmp, 16));
		}
		return sb.toString();
	}

	/**
	 * 灏嗚〃绀�杩涘埗鍊肩殑瀛楃涓茶浆鎹负byte鏁扮粍锛� 鍜宲ublic static String byteArr2HexStr(byte[] arrB)
	 * 浜掍负鍙�嗙殑杞崲杩囩▼
	 * 
	 * @param strIn
	 *            闇�瑕佽浆鎹㈢殑瀛楃涓�
	 * @return 杞崲鍚庣殑byte鏁扮粍
	 */
	public static byte[] hexStr2ByteArr(String strIn) {
		byte[] arrB = strIn.getBytes();
		int iLen = arrB.length;
		byte[] arrOut = new byte[iLen / 2];// 涓や釜瀛楃琛ㄧず涓�涓瓧鑺傦紝鎵�浠ュ瓧鑺傛暟缁勯暱搴︽槸瀛楃涓查暱搴﹂櫎浠�
		for (int i = 0; i < iLen; i = i + 2) {
			String strTmp = new String(arrB, i, 2);
			arrOut[i / 2] = (byte) Integer.parseInt(strTmp, 16);
		}
		return arrOut;
	}

	/**
	 * 榛樿鏋勯�犳柟娉曪紝浣跨敤榛樿瀵嗛挜
	 * 
	 * @throws Exception
	 */
	public DesUtils() throws Exception {
		this(strDefaultKey);
	}

	/**
	 * DES瀛楃涓插姞瀵� 鎸囧畾瀵嗛挜鏋勯�犳柟娉�
	 * 
	 * @param strKey
	 *            鎸囧畾鐨勫瘑閽�
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
	 * 鍔犲瘑瀛楄妭鏁扮粍
	 * 
	 * @param arrB
	 *            闇�鍔犲瘑鐨勫瓧鑺傛暟缁�
	 * @return 鍔犲瘑鍚庣殑瀛楄妭鏁扮粍
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws Exception
	 */
	public byte[] encrypt(byte[] arrB) throws IllegalBlockSizeException,
			BadPaddingException {
		return encryptCipher.doFinal(arrB);
	}

	/**
	 * 鍔犲瘑瀛楃涓�
	 * 
	 * @param strIn
	 *            闇�鍔犲瘑鐨勫瓧绗︿覆
	 * @return 鍔犲瘑鍚庣殑瀛楃涓�
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws Exception
	 */
	public String encrypt(String strIn) throws IllegalBlockSizeException,
			BadPaddingException {
		return byteArr2HexStr(encrypt(strIn.getBytes()));
	}

	/**
	 * 瑙ｅ瘑瀛楄妭鏁扮粍
	 * 
	 * @param arrB
	 *            闇�瑙ｅ瘑鐨勫瓧鑺傛暟缁�
	 * @return 瑙ｅ瘑鍚庣殑瀛楄妭鏁扮粍
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws Exception
	 */
	public byte[] decrypt(byte[] arrB) throws IllegalBlockSizeException,
			BadPaddingException {
		return decryptCipher.doFinal(arrB);
	}

	/**
	 * 瑙ｅ瘑瀛楃涓�
	 * 
	 * @param strIn
	 *            闇�瑙ｅ瘑鐨勫瓧绗︿覆
	 * @return 瑙ｅ瘑鍚庣殑瀛楃涓�
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws Exception
	 */
	public String decrypt(String strIn) throws IllegalBlockSizeException,
			BadPaddingException {
		return new String(decrypt(hexStr2ByteArr(strIn.trim())));
	}

	/**
	 * 浠庢寚瀹氬瓧绗︿覆鐢熸垚瀵嗛挜锛屽瘑閽ユ墍闇�鐨勫瓧鑺傛暟缁勯暱搴︿负8浣� 涓嶈冻8浣嶆椂鍚庨潰琛�岃秴鍑�嶅彧鍙栧墠8浣�
	 * 
	 * @param arrBTmp
	 *            鏋勬垚璇ュ瓧绗︿覆鐨勫瓧鑺傛暟缁�
	 * @return 鐢熸垚鐨勫瘑閽�
	 * @throws java.lang.Exception
	 */
	private Key getKey(byte[] arrBTmp) throws Exception {
		byte[] arrB = new byte[8];// 鍒涘缓涓�涓┖鐨�嶅瓧鑺傛暟缁勶紙榛樿鍊间负0锛�
		for (int i = 0; i < arrBTmp.length && i < arrB.length; i++) {// 灏嗗師濮嬪瓧鑺傛暟缁勮浆鎹负8浣�
			arrB[i] = arrBTmp[i];
		}
		Key key = new javax.crypto.spec.SecretKeySpec(arrB, "DES");// 鐢熸垚瀵嗛挜
		return key;
	}
}