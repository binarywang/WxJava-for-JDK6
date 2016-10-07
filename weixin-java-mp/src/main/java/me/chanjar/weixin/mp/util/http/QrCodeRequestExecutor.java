package me.chanjar.weixin.mp.util.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;

import me.chanjar.weixin.common.bean.result.WxError;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.common.util.fs.FileUtils;
import me.chanjar.weixin.common.util.http.InputStreamResponseHandler;
import me.chanjar.weixin.common.util.http.RequestExecutor;
import me.chanjar.weixin.common.util.http.Utf8ResponseHandler;
import me.chanjar.weixin.mp.bean.result.WxMpQrCodeTicket;

/**
 * 获得QrCode图片 请求执行器
 * @author chanjarster
 *
 */
public class QrCodeRequestExecutor implements RequestExecutor<File, WxMpQrCodeTicket> {

  @Override
  public File execute(CloseableHttpClient httpclient, HttpHost httpProxy, String uri, 
      WxMpQrCodeTicket ticket) throws WxErrorException, IOException {
    if (ticket != null) {
      if (uri.indexOf('?') == -1) {
        uri += '?';
      }
      uri += uri.endsWith("?") 
          ? "ticket=" + URLEncoder.encode(ticket.getTicket(), "UTF-8") 
          : "&ticket=" + URLEncoder.encode(ticket.getTicket(), "UTF-8");
    }
    
    HttpGet httpGet = new HttpGet(uri);
    if (httpProxy != null) {
      RequestConfig config = RequestConfig.custom().setProxy(httpProxy).build();
      httpGet.setConfig(config);
    }

    CloseableHttpResponse response = httpclient.execute(httpGet);
    InputStream inputStream = InputStreamResponseHandler.INSTANCE.handleResponse(response);
    try {
      Header[] contentTypeHeader = response.getHeaders("Content-Type");
      if (contentTypeHeader != null && contentTypeHeader.length > 0) {
        // 出错
        if (ContentType.TEXT_PLAIN.getMimeType().equals(contentTypeHeader[0].getValue())) {
          String responseContent = Utf8ResponseHandler.INSTANCE.handleResponse(response);
          throw new WxErrorException(WxError.fromJson(responseContent));
        }
      }
      return FileUtils.createTmpFile(inputStream, UUID.randomUUID().toString(), "jpg");
    } finally {
      IOUtils.closeQuietly(inputStream);
      IOUtils.closeQuietly(response);
      httpGet.releaseConnection();
    }

  }

}
