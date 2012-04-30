package com.rishiandmanisha.photoblog;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TumblrApi;
import org.scribe.oauth.OAuthService;

import com.rishiandmanisha.fetchers.DataFetcher;
import com.rishiandmanisha.fetchers.ImageCache;
import com.rishiandmanisha.fetchers.ImageFetcher;

import android.app.Application;
import android.content.Context;

public class PhotoBlogApp extends Application {
  public PhotoBlogPreferences prefs;
  public OAuthService service;
  public DataFetcher networkMgr;
  public ImageFetcher imageFetcher;
  public ImageCache imageCache;
  public DialogBuilder dialogBuilder;
  
  public static final String TUMBLR_API_KEY = "";
  public static final String TUMBLR_API_SECRET = "";
  
  @Override
  public void onCreate() {
    super.onCreate();
    Context context = getApplicationContext();
    service = new ServiceBuilder().provider(TumblrApi.class).apiKey(TUMBLR_API_KEY).apiSecret(
        TUMBLR_API_SECRET).build();
    prefs = new PhotoBlogPreferences(context);
    networkMgr = DataFetcher.createDefaultNetworkManager(context, service);
    imageCache = new ImageCache(3*1024*1024); // 3MB
    imageFetcher = new ImageFetcher(networkMgr, imageCache);
    dialogBuilder = new DialogBuilder(getResources());
  }
}
