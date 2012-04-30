package com.rishiandmanisha.photoblog;

import org.scribe.model.Token;

import com.rishiandmanisha.adapter.PhotoBlogListAdapter;
import com.rishiandmanisha.fetchers.DataFetcher;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class PhotoBlog extends Activity {
  PhotoBlogApp app;
  PhotoBlogPreferences prefs;
  
  RelativeLayout main;
  ListView listView;
  PhotoBlogListAdapter adapter;
  ProgressBar progress;
  
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    
    app = (PhotoBlogApp) getApplication();
    prefs = app.prefs;
    DataFetcher networkMgr = app.networkMgr;
    
    LayoutInflater inflater = LayoutInflater.from(app);
    RelativeLayout shell = (RelativeLayout) inflater.inflate(R.layout.generic_shell, null);
    RelativeLayout shellContent = (RelativeLayout) shell.findViewById(R.id.generic_content);
    main = (RelativeLayout) inflater.inflate(R.layout.main, shellContent);
    
    listView = (ListView) main.findViewById(R.id.photoblog_list);
    progress = (ProgressBar) main.findViewById(R.id.photoblog_progress);

    adapter = new PhotoBlogListAdapter(app, this, listView, progress);
    
    String tok = prefs.getPhoto();
    if (tok == null) {
      Token accessToken = app.service.getXAuthAccessToken("", "");
      networkMgr.setAccessToken(accessToken);
      if (prefs != null) {
        prefs.setPhoto(accessToken.getToken());
        prefs.setPhotoSec(accessToken.getSecret());
      }
      adapter.fetchBlogPosts(0, PhotoBlogListAdapter.LIMIT);
    } else {
      networkMgr.setAccessToken(new Token(tok, prefs.getPhotoSec()));
      adapter.fetchBlogPosts(0, PhotoBlogListAdapter.LIMIT);
    }
    
    listView.setAdapter(adapter);
    setContentView(shell);
  }
  
  @Override
  protected void onDestroy() {
    adapter.destroy();
    super.onDestroy();
  }
  
}