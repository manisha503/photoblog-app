package com.rishiandmanisha.model;

import android.text.Html;

import com.rishiandmanisha.photoblog.TumblrConstants;

public class PhotoBlogEntry {
  
  public long id;
  public String postUrl;
  public long timestamp;
  public int type;
  public int format;
  // for photo types
  public String thumbUrl;
  public String detailsUrl;
  public String caption;
  
  // for text types
  // body should be set using setBody so that we can convert HTML text upfront
  private String body;
  
  public void setBody(String text) {
    if (text == null) {
      body = null;
    } else {
      if (format == TumblrConstants.BODY_TYPE_HTML) {
        body = Html.fromHtml(text).toString();
      } else {
        body = new String(text);
      }
    }
  }
  
  public String getBody() {
    return body;
  }
  
}
