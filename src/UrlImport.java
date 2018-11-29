import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.http.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class UrlImport {

  private static String HTTP_HOST = "192.168.99.67";
  private static String HTTP_PORT = "9288";

  public static JSONObject doPost(String url, JSONArray array) {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpPost post = new HttpPost("http://" + HTTP_HOST + ":" + HTTP_PORT + url);
    JSONObject response = null;

    JSONObject body = new JSONObject();
    body.put("uid", "null");
    body.put("sid", "null");
    body.put("token", "null");
    body.put("state", "null");
    body.put("statedesc", "null");
    body.put("timestamp", "null");
    String es = array.toString().replaceAll("\"","\\\"");
    body.put("content", "\"" + es + "\"");

    try {
      String urlStr = URLEncoder.encode(body.toString(), "UTF-8");
      StringEntity s = new StringEntity("param=" + urlStr);
      s.setContentType("application/x-www-form-urlencoded");
      post.setEntity(s);
      HttpResponse res = client.execute(post);
      if (res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
        HttpEntity entity = res.getEntity();
        String result = EntityUtils.toString(res.getEntity());
        response = JSONObject.fromObject(result);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return response;
  }

  public static int addMockSystem(String systemCode, String systemName, String systemType) {
    JSONObject content = new JSONObject();
    JSONArray array = new JSONArray();

    content.put("systemCode", systemCode);
    content.put("systemName", systemName);
    content.put("mockType", systemType);
    content.put("cmType", "2");
    content.put("messageType", "xml");
    content.put("messageEncoding", "GB2312");
    content.put("testStatus", "0");
    array.add(content);

    JSONObject ret = null;
    ret = UrlImport.doPost("/mock-server/mockSystemTrans/mockSystemAdd", array);
    return 0;
  }

  public static JSONArray addMockTrans(String systemCode, String systemType, String transCode, String transName) {
    JSONObject content = new JSONObject();
    JSONArray array = new JSONArray();

    content.put("systemCode", systemCode);
    content.put("transName", transName);
    content.put("transCode", transCode);
    content.put("transType", systemType);
    content.put("cmType", "2");
    content.put("messageType", "xml");
    content.put("messageEncoding", "GB2312");
    content.put("testStatus", "0");
    array.add(content);

    JSONObject ret = null;
    ret = UrlImport.doPost("/mock-server/mockSystemTrans/mockTransAddForMat", array);
    return JSONArray.fromObject(ret.get("content"));
  }

  public static JSONArray addTemplateField(int templateId, int idx, int level, String code, 
      String name, String fieldType, String dataType, String required, String param1) {
    JSONObject content = new JSONObject();
    JSONArray array = new JSONArray();

    content.put("templateId", templateId);
    content.put("fieldIndex", idx);
    content.put("fieldLevel", String.valueOf(level));
    content.put("fieldCode", code);
    content.put("fieldName", name);
    content.put("fieldType", fieldType);
    content.put("dataType", dataType);
    content.put("required", required);
    content.put("param1", param1);
    content.put("param2", "");
    content.put("param3", "");
    content.put("format", "");
    content.put("fieldSystemCode", "");
    content.put("dataEncoding", "GB2312");
    content.put("fieldPermission", "W");
    content.put("postParseEventType", "");
    content.put("postParseEventCode", "");
    content.put("postParseEvent", "");
    content.put("postPackEventType", "");
    content.put("postPackEventCode", "");
    content.put("postPackEvent", "");
    content.put("updateStr", "");
    array.add(content);

    JSONObject ret = null;
    ret = UrlImport.doPost("/mock-server/model/field/add", array);
    if (null == ret) {
      System.out.println(array.toString());
    }
    return JSONArray.fromObject(ret.get("content"));
  }
}

