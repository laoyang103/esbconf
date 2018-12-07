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
  private static DefaultHttpClient client = null;

  static {
    client = new DefaultHttpClient();
  }

  public static JSONObject doPost(String url, JSONArray array) {
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

  public static int addMockSystem(String systemCode, String systemName, String systemType, 
      int commType, String messageType, String messageEncoding) {
    JSONObject content = new JSONObject();
    JSONArray array = new JSONArray();

    content.put("systemCode", systemCode);
    content.put("systemName", systemName);
    content.put("mockType", systemType);
    content.put("cmType", commType);
    content.put("messageType", messageType);
    content.put("messageEncoding", messageEncoding);
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
    content.put("mockType", systemType);
    content.put("testStatus", "0");
    array.add(content);

    JSONObject ret = null;
    ret = UrlImport.doPost("/mock-server/mockSystemTrans/mockTransAddForMat", array);
    return JSONArray.fromObject(ret.get("content"));
  }

  public static void addTemplateField(JSONArray ctx, int templateId, int idx, int level, int len, 
      String code, String name, String fieldType, String dataType, String required, 
      String param1, String param2, String param3, String dataEncoding) {
    JSONObject content = new JSONObject();

    content.put("templateId", templateId);
    content.put("fieldIndex", idx);
    content.put("fieldLevel", String.valueOf(level));
    content.put("fieldCode", code);
    content.put("fieldName", name);
    content.put("fieldType", fieldType);
    content.put("dataType", dataType);
    content.put("fieldLength", len);
    content.put("required", required);
    content.put("param1", param1);
    content.put("param2", param2);
    content.put("param3", param3);
    content.put("format", "");
    content.put("fieldSystemCode", "");
    content.put("dataEncoding", dataEncoding);
    content.put("fieldPermission", "W");
    content.put("postParseEventType", "");
    content.put("postParseEventCode", "");
    content.put("postParseEvent", "");
    content.put("postPackEventType", "");
    content.put("postPackEventCode", "");
    content.put("postPackEvent", "");
    content.put("updateStr", "");
    ctx.add(content);
  }

  public static JSONArray commitTemplateField(JSONArray ctx) {
    JSONObject ret = null;
    ret = UrlImport.doPost("/mock-server/model/fieldFormat/add", ctx);
    if (null == ret) {
      System.out.println(ctx.toString());
    }
    return JSONArray.fromObject(ret.get("content"));
  }
}

