package com.rishiandmanisha.fetchers;

import android.graphics.Bitmap;

public class DataResponse {
  public boolean success;
  public int err;
  public String responseData;
  public byte[] rawData;
  public Bitmap bitmapData;
  
  public DataResponse(boolean success, int clientError, String response, byte[] rawData,
      Bitmap bitmapData) {
    this.success = success;
    this.err = clientError;
    this.responseData = response;
    this.rawData = rawData;
    this.bitmapData = bitmapData;
  }
  
}
