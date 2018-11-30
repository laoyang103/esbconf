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

  private static String inprocdKey = null, inprocdVal = null;
  private static String outprocdKey = null, outprocdVal = null;
  private static HashMap<String,Object> inprocd = null, outprocd = null;

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
        if (null == inprocd && inprocdVal.equals(itemMap.get(inprocdKey))) {
          inprocd = itemMap;
        } else if (null == outprocd && outprocdVal.equals(itemMap.get(outprocdKey))) {
          outprocd = itemMap;
        } else {
          itemsList = (ArrayList<HashMap<String,Object>> )currFmt.get("items");
          itemsList.add(itemMap);
        }
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
      UrlImport.addTemplateField(templateId, ++idx, level, code, desc, fieldType, "str", IsMust, "");
    }
  }

  public static void main(String[] args) {
    int i, masterReqId, masterResId;
    String systemCode, systemName;
    long nowts = System.currentTimeMillis() / 1000;

    inprocdKey = args[0];
    inprocdVal = args[1];
    outprocdKey = args[2];
    outprocdVal = args[3];

    try {
      StaxDemo.staxService("./origin/ABC2_SVR_2005400/service.xml");
      StaxDemo.staxFmt("./origin/ABC2_SVR_2005400/format.xml");
    } catch (Exception e) {
      e.printStackTrace();
    }

    systemCode = "ABC2-SVR" + nowts;
    systemName = "ABC2-SVR" + nowts;
    UrlImport.addMockSystem(systemName, systemName, "VC");
    JSONArray transList = UrlImport.addMockTrans(systemName, "VC", "Default", "Default");
    HashMap<String, Integer> idmap = getTemplateIdMap(transList, "master");
    masterReqId = idmap.get("masterReqId");
    masterResId = idmap.get("masterResId");

    for (String key : allSvcMap.keySet()) {
      int transReqId, transResId;
      HashMap<String, Object> svc = (HashMap<String, Object> )allSvcMap.get(key);

      String transCode = (String )svc.get("Name");
      String transName = (String )svc.get("SvcDesc");
      transList = UrlImport.addMockTrans(systemName, "VC", transCode, transName);
      idmap = getTemplateIdMap(transList, transCode);
      transReqId = idmap.get("transReqId");
      transResId = idmap.get("transResId");

      HashMap<String,Object> inFmt, outFmt;
      ArrayList<HashMap<String,Object>> inItemList, outItemList;

      inFmt  = (HashMap<String,Object> )allFmtMap.get((String )svc.get("IFmt"));
      outFmt = (HashMap<String,Object> )allFmtMap.get((String )svc.get("OFmt"));
      if (null == inFmt || null == outFmt) {
        System.out.println("No format for svc: " + key);
        continue;
      }

      inItemList  = (ArrayList<HashMap<String,Object>> )inFmt.get("items");
      outItemList = (ArrayList<HashMap<String,Object>> )outFmt.get("items");

      addTemplateItems(transReqId, inItemList);
      addTemplateItems(transResId, outItemList);
    }
    UrlImport.addTemplateField(masterReqId, 1, 1, (String )inprocd.get("XmlName"), (String )inprocd.get("ItemDesc"), "fixed-field", "str", "Y", "");
    UrlImport.addTemplateField(masterReqId, 2, 1, "ref_transcode", "交易码引用", "reference-field", "str", "Y", "${" + inprocd.get("XmlName") + "}");
    UrlImport.addTemplateField(masterResId, 1, 1, (String )outprocd.get("XmlName"), (String )outprocd.get("ItemDesc"), "fixed-field", "str", "Y", "");
    UrlImport.addTemplateField(masterResId, 2, 1, "ref_transcode", "交易码引用", "reference-field", "str", "Y", "${" + outprocd.get("XmlName") + "}");
  }
}

// System.out.printf("[Name=%s] [SvcDesc=%s] [IFmt=%s] [OFmt=%s]\n", name, desc, ifmt, ofmt);
