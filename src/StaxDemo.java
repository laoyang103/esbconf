import java.util.HashMap;
import java.util.ArrayList;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

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
      if (desc.length() > 40) desc = desc.substring(0, 41);
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
      if (null == item.get("_fieldType")) item.put("_fieldType", "fixed-field");
      if (null == item.get("_layer")) item.put("_layer", layer);
      tmpStr = (String )item.get("SubName"); 
      if (null != tmpStr && !"".equals(tmpStr)) {
        subName = tmpStr;
      }
    }
    itemCtx.addAll(items);

    System.out.printf("Add format %s num %d real %d\n", fmt.get("FmtName"), fmt.get("_count"), items.size());

    if (null != subName) {
      subFmt = (HashMap<String,Object> )LoadConf.allFmtMap.get(subName);
      if (null != subFmt) getFmtAllitem(itemCtx, subFmt, layer);
    } 
  }

  private static HashMap<String,Object> getFmtByName(String fmtName, String svcName, String messageType, String io) {
    HashMap<String,Object> retFmt = null;

    if ("8583".equals(messageType) || "xml".equals(messageType)) {
      retFmt = (HashMap<String,Object> )LoadConf.allFmtMap.get(fmtName);
      return retFmt;
    }
    if ("in".equals(io) && "common".equals(messageType)) {
      StringBuilder sbl = new StringBuilder(svcName);
      StringBuilder sbo = new StringBuilder(svcName);
      sbl.replace(0, 3, "SCL");
      sbo.replace(0, 3, "SCO");
      String fmtNamel = "FMT_81_" + sbl + "_IN";
      String fmtNameo = "FMT_81_" + sbo + "_IN";
      retFmt = (HashMap<String,Object> )LoadConf.allFmtMap.get(fmtNamel);
      if (null == retFmt) {
        retFmt = (HashMap<String,Object> )LoadConf.allFmtMap.get(fmtNameo);
      }
      return retFmt;
    }
    if ("out".equals(io) && "common".equals(messageType)) {
      String fmtName0 = "FMT_81_" + svcName + "_OUT";
      retFmt = (HashMap<String,Object> )LoadConf.allFmtMap.get(fmtName0);
      return retFmt;
    }
    return null;
  }

  public static void addSystem(String systemCode, String systemName, String systemType, int commType, 
      String messageType, String messageEncoding) {
    int masterReqId = 0, masterResId = 0;

    String itemNameKey = "ElemName";
    if (messageType.equals("xml")) {
      itemNameKey = "XmlName";
    } else if (messageType.equals("common")) {
      itemNameKey = "ElemName";
    } else if (messageType.equals("8583")) {
      itemNameKey = "_name";
    }

    UrlImport.addMockSystem(systemCode, systemName, systemType, commType, messageType, messageEncoding);
    System.out.printf("Add System: [systemName=%s] [systemCode=%s] [systemType=%s] [svcCount=%d] [realSvcCount=%d]\n", 
        systemName, systemCode, systemType, LoadConf.svcCount, LoadConf.allSvcMap.size());

    for (String key : LoadConf.allSvcMap.keySet()) {
      int transReqId = 0, transResId = 0, swapId;
      HashMap<String, Object> svc = (HashMap<String, Object> )LoadConf.allSvcMap.get(key);

      String transCode = (String )svc.get("_svcName");
      String transName = ((String )svc.get("SvcDesc"));
      if (transName.length() > 20) transName = transName.substring(0, 20);

      // if (!"乐益通".equals(transName)) continue;
      System.out.printf("Try Add trans: [transName=%s] [transCode=%s] \n", transName, transCode);

      JSONArray transList = UrlImport.addMockTrans(systemCode, systemType, transCode, transName, messageType, messageEncoding);
      HashMap<String, Integer> idmap = getTemplateIdMap(transList);
      transReqId = idmap.get("transReqId");
      if (0 == masterReqId) masterReqId = idmap.get("masterReqId");
      if ("VC".equals(systemType)) {
        transResId = idmap.get("transResId");
        if (0 == masterResId) masterResId = idmap.get("masterResId");
      }

      System.out.printf("Add trans: [transName=%s] [transCode=%s] [transReqId=%d] [transResId=%d]\n", 
          transName, transCode, transReqId, transResId);

      HashMap<String,Object> inFmt, outFmt, swapFmt;
      inFmt   = getFmtByName((String )svc.get("IFmt"), (String )svc.get("Name"), messageType, "in");
      outFmt  = getFmtByName((String )svc.get("OFmt"), (String )svc.get("Name"), messageType, "out");

      ArrayList<HashMap<String,Object>> inItemList, outItemList;
      inItemList  = new ArrayList<HashMap<String,Object>>();
      outItemList = new ArrayList<HashMap<String,Object>>();

      if (systemCode.endsWith("CLT") || systemCode.endsWith("CGET") || systemCode.endsWith("CPUT")) {
        swapFmt = inFmt;
        inFmt = outFmt;
        outFmt = swapFmt;
      }

      if (null != inFmt) {
        getFmtAllitem(inItemList, inFmt, 1);
        addTemplateItems(transReqId, inItemList, itemNameKey, messageEncoding);
      }
      if (null != outFmt && "VC".equals(systemType)) {
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

  public static void main(String[] args) {
    LoadConf.load(				              
        args[5], 				                    // 格式配置文件，多个用分号隔开，文件名路径不能有分号
        args[6], 				                    // 交易配置文件，多个用分号隔开，文件名路径不能有分号
        args[7],				                    // 交易码提取规则（4,4表示偏移四位截取四位）
        args[8]);				                    // 配置文件编码
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
