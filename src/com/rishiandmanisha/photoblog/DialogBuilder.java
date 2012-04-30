package com.rishiandmanisha.photoblog;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;

/**
 * This class provides methods to create and show alert dialogs
 */
public class DialogBuilder {
  Resources resources;
  
  ArrayList<Dialog> dialogs = new ArrayList<Dialog>();
  
  private OnClickListener dialogClickListener = new OnClickListener() {
    
    @Override
    public void onClick(DialogInterface dialog, int which) {
      dialog.dismiss();
      dialogs.remove(dialog);
    }
  };
  
  public DialogBuilder(Resources resources) {
    this.resources = resources;
    dialogs = new ArrayList<Dialog>();
  }
  
  public void clearAlerts() {
    for (Dialog d : dialogs) {
      d.dismiss();
    }
    dialogs.clear();
  }
  
  public void showGenericError(Activity activity) {
    String message = resources.getString(R.string.generic_error_message);
    showCustomAlert(activity, message);
  }
  
  public Dialog showCustomAlert(Activity activity, String message) {
    String title = resources.getString(R.string.error_title);
    String positiveButton = resources.getString(R.string.ok);
    return showCustomAlert(activity, title, message, false, positiveButton, null);
  }
  
  public Dialog showCustomAlert(Activity activity, String title, String message,
      boolean cancelable, String positiveButton, String negativeButton) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    if (title != null) {
      builder = builder.setTitle(title);
    }
    if (message != null) {
      builder = builder.setMessage(message);
    }
    builder = builder.setCancelable(cancelable);
    
    if (positiveButton != null) {
      builder = builder.setPositiveButton(positiveButton, dialogClickListener);
    }
    if (negativeButton != null) {
      builder = builder.setNegativeButton(negativeButton, dialogClickListener);
    }
    
    Dialog d = builder.create();
    dialogs.add(d);
    d.show();
    
    return d;
  }
}
