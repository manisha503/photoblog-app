package com.rishiandmanisha.fetchers;

public interface ImageFetcherCallback {
  public void imageReceived(String imageUrl, DataResponse response);
}
