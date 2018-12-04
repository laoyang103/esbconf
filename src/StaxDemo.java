import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Attribute;

public class StaxDemo {

  private static HashMap<String,Object> allFmtMap = null;
  private static HashMap<String,Object> allSvcMap = null;

  static {
    allFmtMap = new HashMap<String,Object>();
    allSvcMap = new HashMap<String,Object>();
  }

  private static void staxService(String confFile) throws FileNotFoundException, XMLStreamException {
    String key, val;
    Attribute attr = null;
    XMLInputFactory factory = XMLInputFactory.newFactory();
    XMLEventReader reader = factory.createXMLEventReader(new FileReader(confFile));
    HashMap<String,Object> currSvc = null;

    while (reader.hasNext()) {
      XMLEvent en = reader.nextEvent();
      if (!en.isStartElement()) continue;
      StartElement se = en.asStartElement();
      if (se.getName().getLocalPart().equals("Service")) {
        currSvc = new HashMap<String,Object>();
        Iterator iter = se.getAttributes();
        while (iter.hasNext()) {
          attr = (Attribute )iter.next();
          key = attr.getName().toString();
          val = attr.getValue();
          if ("".equals(val)) continue;
          if ("Name".equals(key)) {
            int vallen = val.length();
            String serviceStr = val.substring(vallen - 4, vallen);
            allSvcMap.put(serviceStr, currSvc);
            currSvc.put(key, serviceStr);
          } else {
            currSvc.put(key, val);
          }
        }
      }
    }
  }

  private static void staxFmt(String confFile) throws FileNotFoundException, XMLStreamException {
    String key, val;
    Attribute attr = null;
    XMLInputFactory factory = XMLInputFactory.newFactory();
    XMLEventReader reader = factory.createXMLEventReader(new FileReader(confFile));
    HashMap<String,Object> currFmt = null;
    ArrayList<HashMap<String,Object>> itemsList = null;

    while (reader.hasNext()) {
      XMLEvent en = reader.nextEvent();
      if (!en.isStartElement()) continue;

      StartElement se = en.asStartElement();
      if (se.getName().getLocalPart().equals("Format")) {
        currFmt = new HashMap<String,Object>();
        currFmt.put("items", new ArrayList<HashMap<String,Object>>());

        Iterator iter = se.getAttributes();
        while (iter.hasNext()) {
          attr = (Attribute )iter.next();
          key = attr.getName().toString();
          val = attr.getValue();
          if ("".equals(val)) continue;
          if ("FmtName".equals(key)) {
            allFmtMap.put(val, currFmt);
          }
          currFmt.put(key, val);
        }
      }

      if (se.getName().getLocalPart().equals("Item")) {
        Iterator iter = se.getAttributes();
        HashMap<String,Object> itemMap = new HashMap<String,Object>();
        while (iter.hasNext()) {
          attr = (Attribute )iter.next();
          key = attr.getName().toString();
          val = attr.getValue();
          if ("".equals(val)) continue;
          itemMap.put(key, val);
        }
        itemsList = (ArrayList<HashMap<String,Object>> )currFmt.get("items");
        itemsList.add(itemMap);
      }
    }
  }

  private static HashMap<String, Integer> getTemplateIdMap(JSONArray transList, String transCode) {
    int i;
    HashMap<String, Integer> idmap = new HashMap<String, Integer>();
    for (i = 0; i < transList.size(); i++) {
      JSONObject trans = transList.getJSONObject(i);
      if ("master".equals(transCode) && "master".equals(trans.get("messageCode"))) {
        if ("I".equals(trans.get("messageIo"))) {
          idmap.put("masterReqId", (Integer )trans.get("id"));
        } else {
          idmap.put("masterResId", (Integer )trans.get("id"));
        }
      } else if (transCode.equals(trans.get("messageCode"))) {
        if ("I".equals(trans.get("messageIo"))) {
          idmap.put("transReqId", (Integer )trans.get("id"));
        } else {
          idmap.put("transResId", (Integer )trans.get("id"));
        }
      } 
    }
    return idmap;
  }

  private static void addTemplateItems(int templateId, ArrayList<HashMap<String,Object>> itemList) {
    int level, idx = 0;
    String fieldType, IsMust, code, desc;
    JSONArray fieldArray = new JSONArray();
    for (HashMap<String,Object> item: itemList) {
      desc = (String )item.get("ItemDesc"); 
      code = (String )item.get("XmlName"); 
      IsMust = (String )item.get("IsMust");
      if ("no".equals(IsMust)) IsMust = "N";
      else IsMust = "Y";
      if ("yes".equals((String )item.get("ItemIgnr"))) {
        level = 1; fieldType = "nesting-field";
      } else {
        level = 2; fieldType = "fixed-field";
      }
      if (null == code) {
        System.out.println("Can not add item: " + item);
        continue;
      } 
      if (null == desc) {
        desc = "无描述";
      }
      UrlImport.addTemplateField(fieldArray, templateId, ++idx, level, code, desc, fieldType, "str", IsMust, "");
    }
    UrlImport.commitTemplateField(fieldArray);
  }

  private static void getFmtAllitem(ArrayList<HashMap<String,Object>> itemCtx, HashMap<String,Object> fmt) {
    String tmpStr, subName = null;
    HashMap<String,Object> subFmt = null;
    ArrayList<HashMap<String,Object>> items = null;

    items = (ArrayList<HashMap<String,Object>> )fmt.get("items");
    for (HashMap<String,Object> item: items) {
      tmpStr = (String )item.get("SubName"); 
      if (null != tmpStr && !"".equals(tmpStr)) {
        subName = tmpStr;
      }
    }

    if (null != subName) {
      subFmt = (HashMap<String,Object> )allFmtMap.get(subName);
      if (null != subFmt) getFmtAllitem(itemCtx, subFmt);
    } 
    itemCtx.addAll(items);
  }

  public static void addSystem(String systemCode, String systemName, int commType, String messageType, 			
      String messageEncoding, String formatFiles, String serviceFiles,String transCodeRule) {
    String fileName;
    String[] strList = null;
    int i, masterReqId, masterResId;

    try {
      strList = serviceFiles.split(";");
      for (i = 0; i < strList.length; i++) {
        StaxDemo.staxService(strList[i]);
      }
      strList = formatFiles.split(";");
      for (i = 0; i < strList.length; i++) {
        StaxDemo.staxFmt(strList[i]);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    String systemType = "VC";
    UrlImport.addMockSystem(systemCode, systemName, systemType, commType, messageType, messageEncoding);
    JSONArray transList = UrlImport.addMockTrans(systemCode, systemType, "Default", "Default");
    HashMap<String, Integer> idmap = getTemplateIdMap(transList, "master");
    masterReqId = idmap.get("masterReqId");
    masterResId = idmap.get("masterResId");

    System.out.printf("Add System: [systemName=%s] [systemCode=%s] [masterReqId=%d] [masterResId=%d]\n", 
        systemName, systemCode, masterReqId, masterResId);

    for (String key : allSvcMap.keySet()) {
      int transReqId, transResId;
      HashMap<String, Object> svc = (HashMap<String, Object> )allSvcMap.get(key);

      String transCode = (String )svc.get("Name");
      String transName = (String )svc.get("SvcDesc");
      transList = UrlImport.addMockTrans(systemCode, "VC", transCode, transName);
      idmap = getTemplateIdMap(transList, transCode);
      transReqId = idmap.get("transReqId");
      transResId = idmap.get("transResId");

      System.out.printf("Add trans: [transName=%s] [transCode=%s] [transReqId=%d] [transResId=%d]\n", 
          transName, transCode, transReqId, transResId);

      HashMap<String,Object> inFmt, outFmt;
      inFmt  = (HashMap<String,Object> )allFmtMap.get((String )svc.get("IFmt"));
      outFmt = (HashMap<String,Object> )allFmtMap.get((String )svc.get("OFmt"));
      if (null == inFmt || null == outFmt) {
        System.out.printf("No format for svc: %s(%s)", transName, transCode);
        continue;
      }

      ArrayList<HashMap<String,Object>> inItemList, outItemList;
      inItemList  = new ArrayList<HashMap<String,Object>>();
      outItemList = new ArrayList<HashMap<String,Object>>();
      getFmtAllitem(inItemList,  inFmt);
      getFmtAllitem(outItemList, outFmt);

      addTemplateItems(transReqId, inItemList);
      addTemplateItems(transResId, outItemList);
    }

    JSONArray masterReqCtx = new JSONArray();
    JSONArray masterResCtx = new JSONArray();
    UrlImport.addTemplateField(masterReqCtx, masterReqId, 2, 1, "ref_transcode", "交易码引用", "reference-field", "str", "Y", "");
    UrlImport.addTemplateField(masterResCtx, masterResId, 2, 1, "ref_transcode", "交易码引用", "reference-field", "str", "Y", "");
    UrlImport.commitTemplateField(masterReqCtx);
    UrlImport.commitTemplateField(masterResCtx);
  }

  public static void main(String[] args) {
    StaxDemo.addSystem(				              
        args[0], 				                    // 系统名称
        args[1], 				                    // 系统编码
        Integer.parseInt(args[2]),          // 通信类型（2：SOCKET短连接， 3：SOCKET长连接）
        args[3], 				                    // 报文类型
        args[4], 				                    // 编码类型
        args[5], 				                    // 格式配置文件，多个用分号隔开，文件名路径不能有分号
        args[6], 				                    // 交易配置文件，多个用分号隔开，文件名路径不能有分号
        args[7]);				                    // 交易码提取规则（4,4表示偏移四位截取四位）
  }
}

// System.out.printf("[Name=%s] [SvcDesc=%s] [IFmt=%s] [OFmt=%s]\n", name, desc, ifmt, ofmt);
