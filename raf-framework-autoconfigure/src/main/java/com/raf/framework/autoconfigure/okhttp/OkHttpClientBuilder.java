package com.raf.framework.autoconfigure.okhttp;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OkHttpClientBuilder {

  private String url;
  private Map<String, String> headers;
  private Map<String, String> params;
  private TypeReference typeReference;
  private String body;

}
