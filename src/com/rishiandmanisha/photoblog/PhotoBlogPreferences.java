package com.rishiandmanisha.photoblog;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Wrapper around SharedPreferences
 */
public class PhotoBlogPreferences {
  
  /** Name of the file where preferences are stored */
  public static final String PREFS_NAME = "com.rishiandmanisha.photoblog.prefs";
  
  // Key for PHOTO
  private static final String PHOTO_TOK = "PhotoTok";
  private static final String PHOTO_SEC = "PhotoSec";
  private static final String FIRST_WINDOW = "FirstWindow";
  private static final String SEED = "";
  
  private SharedPreferences prefs;
  
  public PhotoBlogPreferences(Context context) {
    if (null == context) {
      throw new IllegalArgumentException("The argument 'context' cannot be null");
    }
    
    this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }
  
  public SharedPreferences getSharedPrefs() {
    return this.prefs;
  }
  
  public void clearAppPrefs() {
    SharedPreferences.Editor editor = prefs.edit();
    editor.clear();
    editor.commit();
  }
  
  public void setPhoto(String photoInfo) {
    if (photoInfo != null && photoInfo.length() > 0) {
      SharedPreferences.Editor editor = prefs.edit();
      String securePhoto = null;
      try {
        securePhoto = encrypt(SEED, photoInfo);
      } catch (Exception e) {
        Log.v(PhotoBlogPreferences.class.getName(), "FAILED to encrypt");
      }
      if (securePhoto != null) {
        editor.putString(PHOTO_TOK, securePhoto);
      }
      editor.commit();
    }
  }
  
  public String getPhoto() {
    String securePhoto = prefs.getString(PHOTO_TOK, null);
    try {
      return decrypt(SEED, securePhoto);
    } catch (Exception e) {
      Log.v(PhotoBlogPreferences.class.getName(), "FAILED to encrypt");
    }
    return null;
  }
  
  public void setPhotoSec(String sec) {
    if (sec != null && sec.length() > 0) {
      SharedPreferences.Editor editor = prefs.edit();
      String securePhoto = null;
      try {
        securePhoto = encrypt(SEED, sec);
      } catch (Exception e) {
        Log.v(PhotoBlogPreferences.class.getName(), "FAILED to encrypt");
      }
      if (securePhoto != null) {
        editor.putString(PHOTO_SEC, securePhoto);
      }
      editor.commit();
    }
  }
  
  public String getPhotoSec() {
    String securePhoto = prefs.getString(PHOTO_SEC, null);
    try {
      return decrypt(SEED, securePhoto);
    } catch (Exception e) {
      Log.v(PhotoBlogPreferences.class.getName(), "FAILED to encrypt");
    }
    return null;
  }
  
  public String getFirstBlogWindow() {
    return prefs.getString(FIRST_WINDOW, null);
  }
  
  public void setFirstBlogWindow(String firstWindow) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(FIRST_WINDOW, firstWindow);
    editor.commit();
  }
  
  /**
   * Encryption helpers
   */
  
  public String encrypt(String seed, String cleartext) throws Exception {
    byte[] rawKey = getRawKey(seed.getBytes());
    byte[] result = encrypt(rawKey, cleartext.getBytes());
    return new String(Base64.encode(result));
  }
  
  public String decrypt(String seed, String encrypted) throws Exception {
    byte[] decodedEncrypted = Base64.decode(encrypted);
    byte[] rawKey = getRawKey(seed.getBytes());
    byte[] clearText = decrypt(rawKey, decodedEncrypted);
    return new String(clearText);
  }
  
  private static byte[] getRawKey(byte[] seed) throws Exception {
    KeyGenerator kgen = KeyGenerator.getInstance("AES");
    SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
    sr.setSeed(seed);
    kgen.init(128, sr); // 192 and 256 bits may not be available
    SecretKey skey = kgen.generateKey();
    byte[] raw = skey.getEncoded();
    return raw;
  }
  
  private static byte[] encrypt(byte[] raw, byte[] clear) throws Exception {
    SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
    byte[] encrypted = cipher.doFinal(clear);
    return encrypted;
  }
  
  private static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
    SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.DECRYPT_MODE, skeySpec);
    byte[] decrypted = cipher.doFinal(encrypted);
    return decrypted;
  }
  
  public static byte[] toByte(String hexString) {
    int len = hexString.length() / 2;
    byte[] result = new byte[len];
    for (int i = 0; i < len; i++)
      result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2), 16).byteValue();
    return result;
  }
  
}
