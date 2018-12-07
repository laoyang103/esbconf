import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Attribute;

public class StaxDemo {

  private static HashMap<String, Integer> getTemplateIdMap(JSONArray transList) {
    int i;
    HashMap<String, Integer> idmap = new HashMap<String, Integer>();
    for (i = 0; i < transList.size(); i++) {
      JSONObject trans = transList.getJSONObject(i);
      if ("master".equals(trans.get("messageCode"))) {
        if ("I".equals(trans.get("messageIo"))) {
          idmap.put("masterReqId", (Integer )trans.get("id"));
        } else {
          idmap.put("masterResId", (Integer )trans.get("id"));
        }
      } else {
        if ("I".equals(trans.get("messageIo"))) {
          idmap.put("transReqId", (Integer )trans.get("id"));
        } else {
          idmap.put("transResId", (Integer )trans.get("id"));
        }
      } 
    }
    return idmap;
  }

  private static void addTemplateItems(int templateId, ArrayList<HashMap<String,Object>> itemList, 
      String itemNameKey, String dataEncoding) {
    int layer, idx = 0, len;
    String param1 = "", param2 = "", param3 = "";
    String fieldType, IsMust, code, desc, dataType, align; 
    JSONArray fieldArray = new JSONArray();
    for (HashMap<String,Object> item: itemList) {
      // 字段名称
      code = (String )item.get(itemNameKey); 
      if (null == code) {
        System.out.println("Can not add item: " + item);
        continue;
      } 
      // 字段描述
      desc = (String )item.get("ItemDesc"); 
      if (null == desc) desc = "无描述";
      // 域类型
      fieldType = (String )item.get("_fieldType");
      // 字段类型
      dataType = (String )item.get("_dataType");
      // 字段长度
      len  = (Integer )item.get("_length"); 
      // 字段层级
      layer = (Integer )item.get("_layer"); 
      // 必填标志
      IsMust = (String )item.get("IsMust");
      if ("no".equals(IsMust)) IsMust = "N";
      else IsMust = "Y";
      // 参数1
      param1 = (String )item.get("param1");
      if (null == param1) param1 = "";
      align = (String )item.get("ItemAdj");
      if (null != align && ("left".equals(align) || "right".equals(align))) {
        param1 = align;
      }
      // 参数2
      param2 = (String )item.get("param2");
      if (null == param2) param2 = "";
      // 参数3
      param3 = (String )item.get("param3");
      if (null == param3) param3 = "";
      // 添加字段
      UrlImport.addTemplateField(fieldArray, templateId, ++idx, layer, len, code, desc, 
          fieldType, dataType, IsMust, param1, param2, param3, dataEncoding);
    }
    UrlImport.commitTemplateField(fieldArray);
  }

  private static void getFmtAllitem(ArrayList<HashMap<String,Object>> itemCtx, HashMap<String,Object> fmt, int layer) {
    boolean haveNested = false;
    String tmpStr, subName = null;
    HashMap<String,Object> subFmt = null;
    ArrayList<HashMap<String,Object>> items = null;

    items = (ArrayList<HashMap<String,Object>> )fmt.get("items");
    for (HashMap<String,Object> item: items) {
      if ("yes".equals((String )item.get("ItemIgnr"))) {
        item.put("_fieldType", "nesting-field");
        item.put("_layer", layer);
        layer += 1;
        continue;
      }
      item.put("_fieldType", "fixed-field");
      item.put("_layer", layer);
      tmpStr = (String )item.get("SubName"); 
      if (null != tmpStr && !"".equals(tmpStr)) {
        subName = tmpStr;
      }
    }
    itemCtx.addAll(items);

    if (null != subName) {
      subFmt = (HashMap<String,Object> )LoadConf.allFmtMap.get(subName);
      if (null != subFmt) getFmtAllitem(itemCtx, subFmt, layer);
    } 
  }

  public static void addSystem(String systemCode, String systemName, String systemType, int commType, 
      String messageType, String messageEncoding) {
    int masterReqId, masterResId;

    if ("8583".equals(messageType)) {
      add8583System(systemCode, systemName, systemType, commType, messageType, messageEncoding);
      return ;
    }

    String itemNameKey = "ElemName";
    if (messageType.equals("xml")) {
      itemNameKey = "XmlName";
    } else if (messageType.equals("common")) {
      itemNameKey = "ElemName";
    }

    UrlImport.addMockSystem(systemCode, systemName, systemType, commType, messageType, messageEncoding);
    JSONArray transList = UrlImport.addMockTrans(systemCode, systemType, "Default", "Default");
    HashMap<String, Integer> idmap = getTemplateIdMap(transList);
    masterReqId = idmap.get("masterReqId");
    masterResId = idmap.get("masterResId");

    System.out.printf("Add System: [systemName=%s] [systemCode=%s] [systemType=%s] [masterReqId=%d] [masterResId=%d]\n", 
        systemName, systemCode, systemType, masterReqId, masterResId);

    for (String key : LoadConf.allSvcMap.keySet()) {
      int transReqId = 0, transResId = 0, swapId;
      HashMap<String, Object> svc = (HashMap<String, Object> )LoadConf.allSvcMap.get(key);

      String transCode = (String )svc.get("Name");
      String transName = (String )svc.get("SvcDesc");
      transList = UrlImport.addMockTrans(systemCode, systemType, transCode, transName);
      idmap = getTemplateIdMap(transList);
      transReqId = idmap.get("transReqId");
      if ("VC".equals(systemType)) {
        transResId = idmap.get("transResId");
      }

      System.out.printf("Add trans: [transName=%s] [transCode=%s] [transReqId=%d] [transResId=%d]\n", 
          transName, transCode, transReqId, transResId);

      HashMap<String,Object> inFmt, outFmt;
      inFmt  = (HashMap<String,Object> )LoadConf.allFmtMap.get((String )svc.get("IFmt"));
      outFmt = (HashMap<String,Object> )LoadConf.allFmtMap.get((String )svc.get("OFmt"));
      if (null == inFmt || null == outFmt) {
        System.out.printf("No format for svc: %s(%s)\n", transName, transCode);
        continue;
      }

      ArrayList<HashMap<String,Object>> inItemList, outItemList, swapItemList;
      inItemList  = new ArrayList<HashMap<String,Object>>();
      outItemList = new ArrayList<HashMap<String,Object>>();

      if (systemCode.endsWith("CLT") || systemCode.endsWith("CGET")) {
        swapItemList = inItemList;
        inItemList = outItemList;
        outItemList = swapItemList;
      }

      getFmtAllitem(inItemList, inFmt, 1);
      addTemplateItems(transReqId, inItemList, itemNameKey, messageEncoding);
      if ("VC".equals(systemType)) {
        getFmtAllitem(outItemList, outFmt, 1);
        addTemplateItems(transResId, outItemList, itemNameKey, messageEncoding);
      }
    }

    JSONArray masterReqCtx = new JSONArray();
    JSONArray masterResCtx = new JSONArray();
    UrlImport.addTemplateField(masterReqCtx, masterReqId, 2, 1, 0, "ref_transcode", "交易码引用", "reference-field", "str", "Y", "", "", "", messageEncoding);
    UrlImport.addTemplateField(masterResCtx, masterResId, 2, 1, 0, "ref_transcode", "交易码引用", "reference-field", "str", "Y", "", "", "", messageEncoding);
    UrlImport.commitTemplateField(masterReqCtx);
    UrlImport.commitTemplateField(masterResCtx);
  }

  public static void add8583System(String systemCode, String systemName, String systemType, int commType, 
      String messageType, String messageEncoding) {
    String itemNameKey = "name";
    int masterReqId, masterResId, transReqId = 0, transResId = 0;
    String transCode = "Default", transName = "Default";

    UrlImport.addMockSystem(systemCode, systemName, systemType, commType, messageType, messageEncoding);
    JSONArray transList = UrlImport.addMockTrans(systemCode, systemType, transCode, transName);
    HashMap<String, Integer> idmap = getTemplateIdMap(transList);
    masterReqId = idmap.get("masterReqId");
    masterResId = idmap.get("masterResId");
    transReqId = idmap.get("transReqId");
    if ("VC".equals(systemType)) {
      transResId = idmap.get("transResId");
    }

    System.out.printf("Add System: [systemName=%s] [systemCode=%s] [systemType=%s] [masterReqId=%d] [masterResId=%d]\n", 
        systemName, systemCode, systemType, masterReqId, masterResId);
    System.out.printf("Add trans: [transName=%s] [transCode=%s] [transReqId=%d] [transResId=%d]\n", 
        transName, transCode, transReqId, transResId);

    addTemplateItems(transReqId, LoadConf.allFmtMap8583, "_name", messageEncoding);
    if ("VC".equals(systemType)) {
      addTemplateItems(transResId, LoadConf.allFmtMap8583, "_name", messageEncoding);
    }

    JSONArray masterReqCtx = new JSONArray();
    JSONArray masterResCtx = new JSONArray();
    UrlImport.addTemplateField(masterReqCtx, masterReqId, 2, 1, 0, "ref_transcode", "交易码引用", "reference-field", "str", "Y", "", "", "", messageEncoding);
    UrlImport.addTemplateField(masterResCtx, masterResId, 2, 1, 0, "ref_transcode", "交易码引用", "reference-field", "str", "Y", "", "", "", messageEncoding);
    UrlImport.commitTemplateField(masterReqCtx);
    UrlImport.commitTemplateField(masterResCtx);
  }

  public static void main(String[] args) {
    LoadConf.load(				              
        args[5], 				                    // 格式配置文件，多个用分号隔开，文件名路径不能有分号
        args[6], 				                    // 交易配置文件，多个用分号隔开，文件名路径不能有分号
        args[7]);				                    // 交易码提取规则（4,4表示偏移四位截取四位）
    StaxDemo.addSystem(				              
        args[0], 				                    // 系统名称
        args[1], 				                    // 系统编码
        "VC", 				                      // 系统编码
        Integer.parseInt(args[2]),          // 通信类型（2：SOCKET短连接， 3：SOCKET长连接）
        args[3], 				                    // 报文类型
        args[4]); 				                  // 编码类型
    StaxDemo.addSystem(				              
        args[0], 				                    // 系统名称
        args[1], 				                    // 系统编码
        "VS", 				                      // 系统编码
        Integer.parseInt(args[2]),          // 通信类型（2：SOCKET短连接， 3：SOCKET长连接）
        args[3], 				                    // 报文类型
        args[4]); 				                  // 编码类型
  }
}

// System.out.printf("[Name=%s] [SvcDesc=%s] [IFmt=%s] [OFmt=%s]\n", name, desc, ifmt, ofmt);
