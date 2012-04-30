package com.rishiandmanisha.adapter;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Verb;

import com.rishiandmanisha.fetchers.DataCallback;
import com.rishiandmanisha.fetchers.DataFetcher;
import com.rishiandmanisha.fetchers.DataRequest;
import com.rishiandmanisha.fetchers.DataResponse;
import com.rishiandmanisha.fetchers.ImageFetcher;
import com.rishiandmanisha.fetchers.ImageFetcherCallback;
import com.rishiandmanisha.model.PhotoBlogEntry;
import com.rishiandmanisha.photoblog.DialogBuilder;
import com.rishiandmanisha.photoblog.PhotoBlogApp;
import com.rishiandmanisha.photoblog.PhotoBlogPreferences;
import com.rishiandmanisha.photoblog.R;
import com.rishiandmanisha.photoblog.TumblrConstants;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PhotoBlogListAdapter extends BaseAdapter implements ImageFetcherCallback,
    DataCallback, OnClickListener {
  ArrayList<PhotoBlogRow> rows;
  Activity activity;
  DataFetcher networkMgr;
  ImageFetcher imageFetcher;
  DialogBuilder dialogBuilder;
  PhotoBlogPreferences prefs;
  ListView listView;
  ProgressBar progress;
  RelativeLayout loadMoreSection;
  Button loadMoreButton;
  boolean loadButtonEnabled = true;
  
  DataRequest blogListRequest;
  
  private static final int NUM_CELLS_PER_ROW = 4;
  int lastOffsetRequested = 0;
  public static final String POSTS_URL = "http://api.tumblr.com/v2/blog/manliman.tumblr.com/posts";
  public static final int LIMIT = 20;
  
  public PhotoBlogListAdapter(PhotoBlogApp app, Activity activity, ListView listView,
      ProgressBar progress) {
    this.activity = activity;
    this.listView = listView;
    this.progress = progress;
    this.imageFetcher = app.imageFetcher;
    this.networkMgr = app.networkMgr;
    this.dialogBuilder = app.dialogBuilder;
    this.prefs = app.prefs;
    
    String firstWindow = prefs.getFirstBlogWindow();
    if (firstWindow != null) {
      JSONObject json = null;
      JSONObject responseJson = null;
      try {
        json = new JSONObject(firstWindow);
        responseJson = json.getJSONObject(TumblrConstants.RESPONSE);
      } catch (JSONException e) {
      }
      setData(responseJson);
    }
  }
  
  private static final class PhotoBlogRow {
    PhotoBlogEntry[] rowEntries = null;
    
    public PhotoBlogRow() {
      rowEntries = new PhotoBlogEntry[NUM_CELLS_PER_ROW];
    }
  }
  
  private static final class ViewHolder {
    ImageView thumb1;
    ImageView thumb2;
    ImageView thumb3;
    ImageView thumb4;
    
    TextView text1;
    TextView text2;
    TextView text3;
    TextView text4;
    
    void populate(View rowLayout) {
      View entry1 = rowLayout.findViewById(R.id.thumb_1);
      thumb1 = (ImageView) entry1.findViewById(R.id.thumbnail);
      text1 = (TextView) entry1.findViewById(R.id.text_thumbnail);
      View entry2 = rowLayout.findViewById(R.id.thumb_2);
      thumb2 = (ImageView) entry2.findViewById(R.id.thumbnail);
      text2 = (TextView) entry2.findViewById(R.id.text_thumbnail);
      View entry3 = rowLayout.findViewById(R.id.thumb_3);
      thumb3 = (ImageView) entry3.findViewById(R.id.thumbnail);
      text3 = (TextView) entry3.findViewById(R.id.text_thumbnail);
      View entry4 = rowLayout.findViewById(R.id.thumb_4);
      thumb4 = (ImageView) entry4.findViewById(R.id.thumbnail);
      text4 = (TextView) entry4.findViewById(R.id.text_thumbnail);
    }
  }
  
  public void destroy() {
    if (blogListRequest != null) {
      networkMgr.cancel(blogListRequest);
    }
    dialogBuilder.clearAlerts();
  }
  
  public void fetchBlogPosts(int offset, int limit) {
    OAuthRequest req = new OAuthRequest(Verb.GET, POSTS_URL);
    req.addQuerystringParameter(TumblrConstants.API_KEY, PhotoBlogApp.TUMBLR_API_KEY);
    req.addQuerystringParameter(TumblrConstants.OFFSET, new Integer(offset).toString());
    req.addQuerystringParameter(TumblrConstants.LIMIT, new Integer(limit).toString());
    blogListRequest = new DataRequest(req, true);
    networkMgr.fetch(blogListRequest, this);
    lastOffsetRequested = offset;
  }
  
  private void setData(JSONObject data) {
    try {
      flattenData(data);
    } catch (JSONException e) {
      Log.e(PhotoBlogListAdapter.class.getName(), "JSON EXCEPTION!!!! " + e.getMessage());
      rows = null;
    }
  }
  
  private void flattenData(JSONObject latestWindow) throws JSONException {
    if (latestWindow == null) {
      return;
    }
    
    JSONArray posts = latestWindow.getJSONArray(TumblrConstants.POSTS);
    PhotoBlogRow row = null;
    // if we're at offset 0, create a new list of rows. Otherwise, keep appending to the current
    // rows
    if (lastOffsetRequested == 0) {
      rows = new ArrayList<PhotoBlogRow>();
    }
    int i;
    if (posts.length() > 0) {
      for (i = 0; i < posts.length(); i++) {
        JSONObject post = posts.getJSONObject(i);
        long id = post.getLong(TumblrConstants.ID);
        String postUrl = post.getString(TumblrConstants.POST_URL);
        long timestamp = post.getLong(TumblrConstants.TIMESTAMP);
        int type = TumblrConstants.getType(post.getString(TumblrConstants.TYPE));
        String caption = null;
        String thumbUrl = null;
        String detailsUrl = null;
        int format = TumblrConstants.getBodyType(post.getString(TumblrConstants.FORMAT));
        String body = null;
        if (type == TumblrConstants.PHOTO_TYPE) {
          caption = post.optString(TumblrConstants.CAPTION);
          JSONArray photos = post.getJSONArray(TumblrConstants.PHOTOS);
          
          thumbUrl = null;
          detailsUrl = null;
          for (int j = 0; j < photos.length(); j++) {
            JSONObject photo = photos.getJSONObject(j);
            JSONArray altSizes = photo.getJSONArray(TumblrConstants.ALT_SIZES);
            for (int k = 0; k < altSizes.length(); k++) {
              JSONObject altSize = altSizes.getJSONObject(k);
              int width = altSize.getInt(TumblrConstants.WIDTH);
              if (width > 100 && width < 320) {
                detailsUrl = altSize.getString(TumblrConstants.URL);
              } else if (width < 100) {
                thumbUrl = altSize.getString(TumblrConstants.URL);
              }
            }
          }
        } else if (type == TumblrConstants.TEXT_TYPE) {
          body = post.getString(TumblrConstants.BODY);
        }
        PhotoBlogEntry entry = new PhotoBlogEntry();
        entry.id = id;
        entry.postUrl = postUrl;
        entry.timestamp = timestamp;
        entry.type = type;
        entry.caption = caption;
        entry.thumbUrl = thumbUrl;
        entry.detailsUrl = detailsUrl;
        entry.format = format;
        entry.setBody(body);
        
        if ((i % NUM_CELLS_PER_ROW) == 0) {
          if (row != null) {
            rows.add(row);
          }
          row = new PhotoBlogRow();
        }
        row.rowEntries[i % NUM_CELLS_PER_ROW] = entry;
      }
      rows.add(row);
    }
    
    // if we didn't get any more posts or if we didn't get a full window's worth of posts, that
    // means there are no more. So disable the button.
    if (posts.length() == 0 || posts.length() < LIMIT) {
      if (loadMoreButton != null) {
        loadMoreButton.setBackgroundColor(activity.getResources().getColor(R.color.transparentBlack));
        loadButtonEnabled = false;
      }
    }
  }
  
  @Override
  public int getCount() {
    if (rows == null) {
      return 0;
    } else {
      // add one for the loadMore button
      return rows.size() + 1;
    }
  }
  
  @Override
  public int getViewTypeCount() {
    return 2;
  }
  
  @Override
  public int getItemViewType(int position) {
    if (rows == null || position < rows.size()) {
      return 0;
    } else {
      return 1;
    }
  }
  
  @Override
  public Object getItem(int arg0) {
    return null;
  }
  
  @Override
  public long getItemId(int arg0) {
    return 0;
  }
  
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View viewToReturn = null;
    LayoutInflater inflater = LayoutInflater.from(activity);
    
    if (position == rows.size()) {
      if (loadMoreSection == null) {
        loadMoreSection = (RelativeLayout) inflater.inflate(R.layout.blog_list_load_more_row, null);
        loadMoreButton = (Button) loadMoreSection.findViewById(R.id.load_more);
        loadMoreButton.setOnClickListener(this);
      }
      viewToReturn = loadMoreSection;
    } else {
      ViewHolder viewHolder = null;
      if (convertView == null) {
        viewToReturn = inflater.inflate(R.layout.blog_list_row, null);
        viewHolder = new ViewHolder();
        viewHolder.populate(viewToReturn);
        viewToReturn.setTag(viewHolder);
      } else {
        viewToReturn = convertView;
        viewHolder = (ViewHolder) convertView.getTag();
      }
      
      if (viewHolder != null) {
        PhotoBlogRow data = rows.get(position);
        
        if (data == null) {
          return viewToReturn;
        }
        
        populateDataIntoViewHolder(data, viewHolder, position);
      }
    }
    
    return viewToReturn;
  }
  
  @Override
  public void imageReceived(String imageUrl, DataResponse response) {
    if (imageUrl != null && response.success) {
      ImageView img = (ImageView) listView.findViewWithTag(imageUrl);
      img.setImageBitmap(response.bitmapData);
      img.setTag(null);
    }
  }
  
  private void populateDataIntoViewHolder(PhotoBlogRow data, ViewHolder viewHolder, int position) {
    PhotoBlogEntry entry1 = data.rowEntries[0];
    if (entry1.type == TumblrConstants.PHOTO_TYPE) {
      populatePhoto(entry1, viewHolder.thumb1, position);
      viewHolder.text1.setVisibility(View.GONE);
      viewHolder.thumb1.setVisibility(View.VISIBLE);
    } else if (entry1.type == TumblrConstants.TEXT_TYPE) {
      populateText(entry1, viewHolder.text1);
      viewHolder.text1.setVisibility(View.VISIBLE);
      viewHolder.thumb1.setVisibility(View.GONE);
    }
    
    PhotoBlogEntry entry2 = data.rowEntries[1];
    if (entry2 != null) {
      if (entry2.type == TumblrConstants.PHOTO_TYPE) {
        populatePhoto(entry2, viewHolder.thumb2, position);
        viewHolder.text2.setVisibility(View.GONE);
        viewHolder.thumb2.setVisibility(View.VISIBLE);
      } else if (entry2.type == TumblrConstants.TEXT_TYPE) {
        populateText(entry2, viewHolder.text2);
        viewHolder.text2.setVisibility(View.VISIBLE);
        viewHolder.thumb2.setVisibility(View.GONE);
      }
    } else {
      viewHolder.text2.setVisibility(View.GONE);
      viewHolder.thumb2.setVisibility(View.GONE);
    }
    
    PhotoBlogEntry entry3 = data.rowEntries[2];
    if (entry3 != null) {
      if (entry3.type == TumblrConstants.PHOTO_TYPE) {
        populatePhoto(entry3, viewHolder.thumb3, position);
        viewHolder.text3.setVisibility(View.GONE);
        viewHolder.thumb3.setVisibility(View.VISIBLE);
      } else if (entry3.type == TumblrConstants.TEXT_TYPE) {
        populateText(entry3, viewHolder.text3);
        viewHolder.text3.setVisibility(View.VISIBLE);
        viewHolder.thumb3.setVisibility(View.GONE);
      }
    } else {
      viewHolder.text3.setVisibility(View.GONE);
      viewHolder.thumb3.setVisibility(View.GONE);
    }
    
    PhotoBlogEntry entry4 = data.rowEntries[3];
    if (entry4 != null) {
      if (entry4.type == TumblrConstants.PHOTO_TYPE) {
        populatePhoto(entry4, viewHolder.thumb4, position);
        viewHolder.text4.setVisibility(View.GONE);
        viewHolder.thumb4.setVisibility(View.VISIBLE);
      } else if (entry4.type == TumblrConstants.TEXT_TYPE) {
        populateText(entry4, viewHolder.text4);
        viewHolder.text4.setVisibility(View.VISIBLE);
        viewHolder.thumb4.setVisibility(View.GONE);
      }
    } else {
      viewHolder.text4.setVisibility(View.GONE);
      viewHolder.thumb4.setVisibility(View.GONE);
    }
  }
  
  private void populateText(PhotoBlogEntry entry, TextView textView) {
    textView.setText(entry.getBody());
  }
  
  private void populatePhoto(PhotoBlogEntry entry, ImageView imageView, int position) {
    Bitmap b = imageFetcher.findBitmapInCache(entry.thumbUrl);
    if (b != null) {
      imageView.setImageBitmap(b);
      imageView.setTag(null);
    } else {
      imageView.setTag(entry.thumbUrl);
      imageFetcher.fetch(entry.thumbUrl, this);
    }
  }
  
  @Override
  public void receivedResponse(DataRequest request, DataResponse response) {
    if (request == blogListRequest) {
      if (response.success) {
        // do this first so that handling of data can reset the loadButtonEnabled state if it needs
        // to
        progress.setVisibility(View.GONE);
        loadButtonEnabled = true;
        
        if (lastOffsetRequested == 0) {
          prefs.setFirstBlogWindow(response.responseData);
        }
        JSONObject jsonData = null;
        JSONObject responseJson = null;
        try {
          jsonData = new JSONObject(response.responseData);
          responseJson = jsonData.getJSONObject(TumblrConstants.RESPONSE);
        } catch (JSONException e) {
        }
        this.setData(responseJson);
        this.notifyDataSetChanged();
      } else {
        dialogBuilder.showGenericError(activity);
      }
    }
  }
  
  @Override
  public void onClick(View v) {
    if (v == loadMoreButton && loadButtonEnabled) {
      fetchBlogPosts(lastOffsetRequested + LIMIT, LIMIT);
      progress.setVisibility(View.VISIBLE);
      loadButtonEnabled = false;
    }
    
  }
  
}
