package com.rishiandmanisha.fetchers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * The ImageFetcher handles requests for image URLs. It
 * collapses requests so that multiple callers asking for the same url will
 * result in only one network fetch.
 * Before calling the callback, the ImageFetcher will parse and construct a
 * Bitmap from the server response. If a Bitmap cannot be constructed, a
 * ClientError is returned.
 */
public class ImageFetcher implements DataCallback {
  DataFetcher networkManager;
  ImageCache imageCache;
  
  HashMap<String, DataRequest> urlRequestMap;
  HashMap<DataRequest, String> requestUrlMap;
  HashMap<DataRequest, ArrayList<ImageFetcherCallback>> requestCallbackMap;
  
  public ImageFetcher(DataFetcher netMgr, ImageCache imageCache) {
    this.networkManager = netMgr;
    this.imageCache = imageCache;
    this.urlRequestMap = new HashMap<String, DataRequest>();
    this.requestUrlMap = new HashMap<DataRequest, String>();
    this.requestCallbackMap = new HashMap<DataRequest, ArrayList<ImageFetcherCallback>>();
  }
  
  public Bitmap findBitmapInCache(String url) {
    if (url == null) {
      return null;
    }
    
    // check for an outstanding NetworkRequest for this url
    DataRequest request = urlRequestMap.get(url);
    if (request != null) {
      // if there is a request in flight, there's no way the image is cached, so
      // don't look it up
      return null;
    } else {
      return findImageInCache(url);
    }
  }
  
  /**
   * Fetch a particular image, specified by the given url. If multiple callers
   * request the same url, the ImageManager handles collapsing all their
   * requests into a single network request.
   */
  public void fetch(String url, ImageFetcherCallback callback) {
    if (url == null || callback == null) {
      return;
    }
    
    Log.v(ImageFetcher.class.getName(), url);
    
    // check for an outstanding NetworkRequest for this url
    DataRequest request = urlRequestMap.get(url);
    if (request != null) {
      // if one exists, just add this callback to the list of callbacks (as long
      // as it hasn't already been added)
      ArrayList<ImageFetcherCallback> callbacks = requestCallbackMap.get(request);
      boolean addToList = true;
      for (ImageFetcherCallback existingCallback : callbacks) {
        if (existingCallback.equals(callback)) {
          addToList = false;
          break;
        }
      }
      if (addToList) {
        callbacks.add(callback);
        requestCallbackMap.put(request, callbacks);
      }
    } else {
      // first check for the image in our cache
      Bitmap cached = findImageInCache(url);
      if (cached != null) {
        DataResponse resp = new DataResponse(true, 200, null, null, cached);
        callback.imageReceived(url, resp);
        return;
      }
      
      // otherwise, create a new request and tell the NetworkManager to fetch
      // asynchronously
      request = new DataRequest(url);
      urlRequestMap.put(url, request);
      requestUrlMap.put(request, url);
      ArrayList<ImageFetcherCallback> callbacks = new ArrayList<ImageFetcherCallback>();
      callbacks.add(callback);
      requestCallbackMap.put(request, callbacks);
      
      networkManager.fetch(request, this);
    }
  }
  
  private Bitmap findImageInCache(String convertedUrl) {
    // first check in-memory cache
    
    Bitmap bitmap = null;
    if (imageCache != null) {
      bitmap = imageCache.imageForKey(convertedUrl);
    }
    /*
     * boolean addToImageCache = false;
     * byte[] bytes = null;
     * if (bitmap == null) {
     * // check disk cache
     * bytes = findBytesInCache(convertedUrl);
     * bitmap = bitmapFactory.decodeByteArray(bytes);
     * addToImageCache = true;
     * }
     * // if found, add to image cache if necessary
     * if (bitmap != null && addToImageCache) {
     * imageCache.setImage(convertedUrl, bitmap, bitmap.getRowBytes() * bitmap.getHeight());
     * }
     * if (bitmap == null && bundleCache != null) {
     * bitmap = bundleCache.imageForKey(convertedUrl);
     * }
     * return bitmap;
     */
    return bitmap;
  }
  
  /**
   * The cancel method cancels a pending network request for this url AND
   * callback, if one exists.
   */
  public void cancel(String url, ImageFetcherCallback callback) {
    if (url == null || callback == null) {
      return;
    }
    DataRequest request = urlRequestMap.get(url);
    if (request == null) {
      return;
    }
    
    ArrayList<ImageFetcherCallback> callbacks = requestCallbackMap.get(request);
    if (callbacks == null) {
      return;
    }
    
    if (callbacks.size() == 1 && callback.equals(callbacks.get(0))) {
      // if this was the only request for this image, cancel the network fetch
      networkManager.cancel(request);
      requestCallbackMap.remove(request);
      requestUrlMap.remove(request);
      urlRequestMap.remove(url);
    } else {
      // if there are other requests for this image, just remove this callback
      // from the list
      ImageFetcherCallback toRemove = null;
      for (ImageFetcherCallback existingCallback : callbacks) {
        if (existingCallback.equals(callback)) {
          toRemove = existingCallback;
          break;
        }
      }
      if (toRemove != null) {
        boolean removed = callbacks.remove(toRemove);
        if (removed) {
          requestCallbackMap.put(request, callbacks);
        }
      }
    }
  }
  
  /**
   * This cancel method cancels all pending network requests that this callback
   * had initiated
   */
  @SuppressWarnings("unchecked")
  public void cancel(ImageFetcherCallback callback) {
    if (callback == null) {
      return;
    }
    // go through all our URLs and try to cancel this url for this callback
    Set<String> urls = ((HashMap<String, DataRequest>) urlRequestMap.clone()).keySet();
    for (String url : urls) {
      cancel(url, callback);
    }
  }
  
  /**
   * Handler for the response from the NetworkManager. This method is run back
   * in the main thread. It attempts to create a Bitmap out of the server
   * response and then responds to all callbacks.
   */
  @Override
  @SuppressWarnings("unchecked")
  public void receivedResponse(DataRequest request, DataResponse response) {
    // if the fetch was successful, create a Bitmap from the raw response data
    // if we can, otherwise create a new ClientError.
    String url = requestUrlMap.get(request);
    
    // make a shallow copy of the callback list so that we can use it to process
    // this response
    ArrayList<ImageFetcherCallback> callbacks = requestCallbackMap.get(request);
    ArrayList<ImageFetcherCallback> callbacksCopy = null;
    if (callbacks != null) {
      callbacksCopy = (ArrayList<ImageFetcherCallback>) callbacks.clone();
    }
    
    byte[] responseBytes = null;
    Bitmap responseBitmap = null;
    if (response.success) {
      if (response.rawData != null) {
        responseBytes = (byte[]) response.rawData;
        
        responseBitmap = BitmapFactory.decodeByteArray(responseBytes, 0, responseBytes.length);
        if (responseBitmap != null) {
          // add to our caches
          if (imageCache != null) {
            imageCache.setImage(url, responseBitmap,
                responseBitmap.getRowBytes() * responseBitmap.getHeight());
          }
          
        } else {
          response.success = false;
          response.err = ClientError.INCOMPATIBLE_DATA_ERROR;
          response.responseData = null;
        }
      } else {
        response.success = false;
        response.err = ClientError.INCOMPATIBLE_DATA_ERROR;
      }
    }
    
    DataResponse bitmapResponse = new DataResponse(response.success, response.err, null, null,
        responseBitmap);
    
    // we're done processing this request so remove it from the maps.
    requestUrlMap.remove(request);
    urlRequestMap.remove(url);
    
    // notify callbacks if required
    if (callbacksCopy != null) {
      for (ImageFetcherCallback existingCallback : callbacksCopy) {
        existingCallback.imageReceived(url, bitmapResponse);
      }
      requestCallbackMap.remove(request);
    }
  }
}
