package com.rishiandmanisha.fetchers;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Process;
import android.util.Log;

/**
 * This class dispatches HTTP requests using a
 * background thread. If a callback object is provided, it will be notified when
 * the dispatching is complete.
 */
public class DataFetcher {
  /** HTTP status codes */
  public static final int HTTP_STATUS_SUCCESSFUL = 200;
  public static final int HTTP_STATUS_NOT_MODIFIED = 304;
  public static final int HTTP_STATUS_CLIENT_ERROR = 400;
  public static final int HTTP_STATUS_FORBIDDEN = 403;
  public static final int HTTP_STATUS_SERVER_ERROR = 500;
  
  /**
   * Error Codes
   */
  public static final int NO_ERROR = 0;
  /** Generic error code */
  public static final int GENERIC_ERROR = 1;
  /** Error code if we don't have a network connection */
  public static final int NO_NETWORK_ERROR = 2;
  /** Error code if our servers are down */
  public static final int SERVICE_ERROR = 3;
  
  /** Milliseconds to wait for connection to be established before timing out */
  private static final int MILLIS_CONNECTION_TIMEOUT = 5000;
  /** Milliseconds to wait for next data packet before timing out */
  private static final int MILLIS_SOCKET_TIMEOUT = 30000;
  /** Maximum number of connections allowed overall */
  private static final int MAX_TOTAL_CONNECTIONS = 200;
  /** Maximum number of connections allowed per host */
  private static final int MAX_CONNECTIONS_PER_ROUTE = 30;
  /** Size of the socket's internal buffer (in bytes) */
  private static final int SOCKET_BUFFER_SIZE = 4096;
  
  private final HttpClient httpClient;
  private final ConnectivityManager connMgr;
  /** Thread pool from which to get threads to make a synchronous HTTP request */
  private final ExecutorService threadPool;
  /** Handler to put callbacks back on the main thread */
  private final Handler mainThreadHandler;
  /** OAuthService that knows how to make synchronous oauth requests */
  private final OAuthService service;
  /** Access Token for making oauth calls */
  private Token accessToken;
  
  private HashMap<DataRequest, Future<?>> requestFutureMap;
  private HashMap<DataRequest, DataCallback> requestCallbackMap;
  
  public DataFetcher(HttpClient httpClient, OAuthService service, ConnectivityManager connMgr,
      ExecutorService threadPool, Handler handler) {
    this.httpClient = httpClient;
    this.service = service;
    this.threadPool = threadPool;
    this.connMgr = connMgr;
    this.mainThreadHandler = handler;
    this.requestFutureMap = new HashMap<DataRequest, Future<?>>();
    this.requestCallbackMap = new HashMap<DataRequest, DataCallback>();
  }
  
  public void setAccessToken(Token accessToken) {
    this.accessToken = accessToken;
  }
  
  /**
   * Executes the given request on a background thread. Returns true if the
   * request could be executed.
   */
  public void fetch(DataRequest request, DataCallback callback) {
    if (request == null || callback == null) {
      return;
    }
    if (requestFutureMap.get(request) != null) {
      // If already running, don't schedule a second run.
      return;
    }
    
    // keep track of the callback for this request
    requestCallbackMap.put(request, callback);
    
    // execute the request on one of the thread pool's threads
    Future<?> future = threadPool.submit(new NetworkWorker(request));
    requestFutureMap.put(request, future);
  }
  
  /**
   * Cancels a running request. If no request is running or if the request could
   * not be canceled, this returns false.
   */
  public void cancel(DataRequest request) {
    if (request == null) {
      return;
    }
    Future<?> future = requestFutureMap.get(request);
    if (future == null) {
      return;
    }
    
    future.cancel(true);
    
    requestFutureMap.remove(request);
    requestCallbackMap.remove(request);
  }
  
  /**
   * Class to encapsulate the method that runs on the background thread.
   */
  private class NetworkWorker implements Runnable {
    private DataRequest request;
    
    public NetworkWorker(DataRequest request) {
      super();
      this.request = request;
    }
    
    @Override
    public void run() {
      Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
      executeInBackground(request, mainThreadHandler);
    }
  }
  
  /**
   * Makes a synchronous HTTP request corresponding to the given IAPIObject
   * request. Within the NetworkManager, this method is executed on the
   * background thread. Any external caller should make sure they are not
   * calling this method from the main thread!
   */
  public DataResponse executeInBackground(DataRequest request, Handler handlerToPostTo) {
    String resp = null;
    byte[] rawBytes = null;
    int err = 0;
    
    // Fail fast if the request is guaranteed to fail because the network is not
    // connected.
    if (connMgr != null) {
      NetworkInfo activeNetInfo = connMgr.getActiveNetworkInfo();
      if (activeNetInfo == null || !activeNetInfo.isConnected()) {
        err = NO_NETWORK_ERROR;
      }
    }
    
    if (err == NO_ERROR) {
      try {
        if (request.oauth_request != null) {
          if (request.sign) {
            service.signRequest(accessToken, request.oauth_request);
          }
          Response response = request.oauth_request.send();
          resp = response.getBody();
        } else if (request.url != null) {
          // Create HTTP requests from the service request
          HttpUriRequest httpRequest = new HttpGet(request.url);
          
          HttpResponse httpResponse = httpClient.execute(httpRequest);
          
          HttpEntity httpEntity = httpResponse.getEntity();
          
          if (httpEntity != null) {
            BufferedHttpEntity bufferedHttpEntity = new BufferedHttpEntity(httpEntity);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
              bufferedHttpEntity.writeTo(outputStream);
              rawBytes = outputStream.toByteArray();
            } finally {
              outputStream.close();
            }
          }
        }
      } catch (Throwable t) {
        Log.e(DataFetcher.class.getName(), t.toString());
        StackTraceElement[] elements = t.getStackTrace();
        for (StackTraceElement element : elements) {
          Log.e(DataFetcher.class.getName(),
              element.getFileName() + " " + element.getMethodName() + " " + element.getLineNumber());
        }
        err = GENERIC_ERROR;
      }
    }
    
    DataResponse response;
    if (err != NO_ERROR) {
      response = new DataResponse(false, err, null, null, null);
    } else {
      response = new DataResponse(true, err, resp, rawBytes, null);
    }
    if (handlerToPostTo != null) {
      handlerToPostTo.post(new MainThreadCallback(request, response));
    }
    
    return response;
  }
  
  /**
   * Class to encapsulate the method that runs on the main thread after the
   * background thread is complete.
   */
  private class MainThreadCallback implements Runnable {
    DataRequest request;
    DataResponse response;
    
    public MainThreadCallback(DataRequest request, DataResponse response) {
      this.request = request;
      this.response = response;
    }
    
    public void run() {
      onPostExecute(request, response);
    }
  }
  
  /**
   * Called after the request is complete.
   */
  protected void onPostExecute(DataRequest request, DataResponse response) {
    DataCallback callback = requestCallbackMap.get(request);
    if (callback != null) {
      callback.receivedResponse(request, response);
      requestFutureMap.remove(request);
      requestCallbackMap.remove(request);
    }
  }
  
  /**
   * Creates a NetworkManager instance using the default configuration settings
   */
  public static DataFetcher createDefaultNetworkManager(Context appContext, OAuthService service) {
    ExecutorService threadPool = Executors.newCachedThreadPool();
    
    ConnectivityManager connMgr = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    Handler handler = new Handler();
    
    // Create and initialize HTTP parameters
    HttpParams params = new BasicHttpParams();
    ConnManagerParams.setMaxTotalConnections(params, MAX_TOTAL_CONNECTIONS);
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
    
    ConnPerRouteBean connPerRoute = new ConnPerRouteBean(MAX_CONNECTIONS_PER_ROUTE);
    ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);
    
    // Create and initialize scheme registry
    SchemeRegistry schemeRegistry = new SchemeRegistry();
    schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
    schemeRegistry.register(new Scheme("https", sslSocketFactory, 443));
    
    // Create an HttpClient with the ThreadSafeClientConnManager. This
    // connection manager must be used if more
    // than one thread will be using the HttpClient.
    ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
    
    DefaultHttpClient httpClient = new DefaultHttpClient(cm, params);
    httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
        MILLIS_CONNECTION_TIMEOUT);
    httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, MILLIS_SOCKET_TIMEOUT);
    httpClient.getParams().setParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, SOCKET_BUFFER_SIZE);
    
    return new DataFetcher(httpClient, service, connMgr, threadPool, handler);
  }
}
