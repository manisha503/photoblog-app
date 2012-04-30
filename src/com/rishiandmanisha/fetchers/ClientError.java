package com.rishiandmanisha.fetchers;

public class ClientError {
  public String message;
  public int code;
  
  public static final String NO_ERROR_MSG = "No Error";
  
  /** Generic error code */
  public static final int GENERIC_ERROR = 1;
  /** Error code if we don't have a network connection */
  public static final int NO_NETWORK_ERROR = 2;
  /** Error code if our servers are down */
  public static final int SERVICE_ERROR = 3;
  /**
   * Error code if the data we got back from the server was in a format we
   * didn't expect
   */
  public static final int INCOMPATIBLE_DATA_ERROR = 4;
  
  public ClientError(int code, String message) {
    this.code = code;
    this.message = message;
  }
}
