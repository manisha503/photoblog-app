package com.rishiandmanisha.fetchers;

import java.util.HashMap;
import java.util.LinkedList;

import android.graphics.Bitmap;

public class ImageCache {
  /** maxSize in bytes of the cache */
  private int maxSize;
  /** current size of all the bitmaps */
  private int curSize;
  
  /** linked list used to track LRU. LRU items are at the head of the list */
  private LinkedList<ImageCacheEntry> list;
  /** image cache */
  private HashMap<String, ImageCacheEntry> imageCache;
  
  private class ImageCacheEntry {
    public String key;
    public int size;
    public Bitmap bitmap;
  }
  
  public ImageCache(int maxSize) {
    this.maxSize = maxSize;
    list = new LinkedList<ImageCacheEntry>();
    imageCache = new HashMap<String, ImageCacheEntry>();
  }
  
  /**
   * Lookup the corresponding Bitmap for this key, and set the used bit for this
   * ImageCacheEntry
   * Returns null if this key is not present in the cache
   */
  public Bitmap imageForKey(String key) {
    if (key == null) {
      return null;
    }
    
    ImageCacheEntry entry = imageCache.get(key);
    if (entry != null) {
      // when the image is used, move it to the end of the LRU list
      list.remove(entry);
      list.add(entry);
      
      return entry.bitmap;
    }
    
    return null;
  }
  
  /**
   * Insert this Bitmap into the cache, if it doesn't exceed the max size and is
   * not already there.
   * Returns true if insert was successful
   */
  public boolean setImage(String key, Bitmap bitmap, int size) {
    if (key == null || bitmap == null || size == 0)
      return false;
    
    // if the bitmap is larger than our cache, bail
    if (size > maxSize) {
      return false;
    }
    
    // if we already stored this key in our cache, bail
    if (imageForKey(key) != null) {
      return false;
    }
    
    // create an ImageCacheEntry
    ImageCacheEntry entry = new ImageCacheEntry();
    entry.bitmap = bitmap;
    entry.size = size;
    
    // make room in the cache and insert
    kickItemsOutToMakeRoom(entry);
    list.add(entry);
    imageCache.put(key, entry);
    curSize += size;
    
    return true;
  }
  
  /**
   * This method takes the entry we wish to add to the cache and kicks entries out of the LRU lists
   * (by removing from the beginning) until we have enough space to insert the new item.
   */
  private void kickItemsOutToMakeRoom(ImageCacheEntry entry) {
    while ((curSize + entry.size > maxSize) && !list.isEmpty()) {
      // Kick the first entry out
      ImageCacheEntry first = list.remove(0);
      imageCache.remove(first.key);
      // subtract its size
      curSize -= first.size;
    }
  }
  
}
