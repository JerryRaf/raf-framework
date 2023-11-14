package com.raf.framework.autoconfigure.okhttp;

import okhttp3.OkHttpClient;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class SslOkHttp {

  private OkHttpClient okHttpClient;

  public SslOkHttp(OkHttpClient okHttpClient){
    this.okHttpClient = okHttpClient;
  }

  public OkHttpClient okHttpClient(){
    return this.okHttpClient;
  }

}
