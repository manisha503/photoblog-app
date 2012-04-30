package com.rishiandmanisha.fetchers;

import org.scribe.model.OAuthRequest;

public class DataRequest {
  public String url;
  public OAuthRequest oauth_request;
  public boolean sign;
  
  public DataRequest(OAuthRequest request, boolean sign_request) {
    this.oauth_request = request;
    this.sign = sign_request;
  }
  
  public DataRequest(String url) {
    this.url = url;
  }
  
}
