package com.rishiandmanisha.photoblog;

/**
 * This class contains constants used in creating Tumblr API requests
 */
public class TumblrConstants
{
  // int types for posts
  public static final int NO_TYPE = 0;
  public static final int PHOTO_TYPE = 1;
  public static final int TEXT_TYPE = 2;
  public static final int VIDEO_TYPE = 3;
  public static final int BODY_TYPE_HTML = 4;
  public static final int BODY_TYPE_MARKDOWN = 5;
  
  // Constants used for string type fields
  public static final String PHOTO = "photo";
  public static final String VIDEO = "video";
  public static final String TEXT = "text";
  public static final String HTML = "html";
  public static final String MARKDOWN = "markdown";
  
  // Constants used as URL parameter names
  public static final String API_KEY = "api_key";
  public static final String LIMIT = "limit";
  public static final String OFFSET = "offset";
  
  // Constants used in posts api request
  public static final String RESPONSE = "response";
  public static final String ID = "id";
  public static final String POSTS = "posts";
  public static final String POST_URL = "post_url";
  public static final String TYPE = "type";
  public static final String TIMESTAMP = "timestamp";
  public static final String CAPTION = "caption";
  public static final String PHOTOS = "photos";
  public static final String ALT_SIZES = "alt_sizes";
  public static final String WIDTH = "width";
  public static final String URL = "url";
  public static final String FORMAT = "format";
  public static final String BODY = "body";
  
  public static int getType(String type) {
    if (type.equals(PHOTO)) {
      return PHOTO_TYPE;
    } else if (type.equals(TEXT)) {
      return TEXT_TYPE;
    } else if (type.equals(VIDEO)) {
      return VIDEO_TYPE;
    }
    return NO_TYPE;
  }
  
  public static int getBodyType(String type) {
    if (type.equals(HTML)) {
      return BODY_TYPE_HTML;
    } else if (type.equals(MARKDOWN)) {
      return BODY_TYPE_MARKDOWN;
    }
    return NO_TYPE;
  }
}
