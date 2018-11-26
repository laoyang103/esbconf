import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Attribute;

public class StaxDemo {

  public static HashMap<String,Object> allFmtMap = null;
  public static HashMap<String,Object> allSvcMap = null;

  static {
    allFmtMap = new HashMap<String,Object>();
    allSvcMap = new HashMap<String,Object>();
  }

  public static void staxService(String confFile) throws FileNotFoundException, XMLStreamException {
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

  public static void staxFmt(String confFile) throws FileNotFoundException, XMLStreamException {
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

  public static void main(String[] args) {
    String systemCode, systemName;
    int idx, systemId = -1, transId = -1;
    int masterReqTemplateId = -1, masterResTemplateId= -1;
    long nowts = System.currentTimeMillis() / 1000;
    HashMap<String,Object> inprocd = null, outprocd = null;

    systemCode = "ABC2-SVR" + nowts;
    systemName = "ABC2-SVR" + nowts;
    systemId = ConfImport.addMockSystem(systemCode, systemName);

    try {
      StaxDemo.staxService("./conf/service.xml");
      StaxDemo.staxFmt("./conf/format.xml");
    } catch (Exception e) {
      e.printStackTrace();
    }

    for (String key : allSvcMap.keySet()) {
      HashMap<String,Object> inFmt, outFmt;
      ArrayList<HashMap<String,Object>> inItemList, outItemList;
      HashMap<String, Object> svc = (HashMap<String, Object> )allSvcMap.get(key);

      inFmt  = (HashMap<String,Object> )allFmtMap.get((String )svc.get("IFmt"));
      outFmt = (HashMap<String,Object> )allFmtMap.get((String )svc.get("OFmt"));

      if (null == inFmt || null == outFmt) {
        System.out.println("No format for svc: " + key);
        continue;
      }

      inItemList  = (ArrayList<HashMap<String,Object>> )inFmt.get("items");
      outItemList = (ArrayList<HashMap<String,Object>> )outFmt.get("items");

      String transCode = (String )svc.get("Name");
      String transName = (String )svc.get("SvcDesc");
      transId = ConfImport.addMockTrans(transCode, transName, systemId);

      int reqTemplateId = ConfImport.addTemplate(systemId, "I", transCode, transName);
      int resTemplateId = ConfImport.addTemplate(systemId, "O", transCode, transName);

      idx = 0;
      for (HashMap<String,Object> inItem: inItemList) {
        String IsMust = (String )inItem.get("IsMust");
        if ("no".equals(IsMust)) IsMust = "N";
        else IsMust = "Y";
        if ("交易码".equals((String )inItem.get("ItemDesc"))) {
          inprocd = inItem;
        } else {
          ConfImport.addTemplateField(reqTemplateId, idx + 1, 1, (String )inItem.get("ElemName"), 
              (String )inItem.get("ItemDesc"), "fixed-field", "str", IsMust);
        }
      }
      idx = 0;
      for (HashMap<String,Object> outItem: outItemList) {
        String IsMust = (String )outItem.get("IsMust");
        if ("no".equals(IsMust)) IsMust = "N";
        else IsMust = "Y";
        if ("核心交易码".equals((String )outItem.get("ItemDesc"))) {
          outprocd = outItem;
        } else {
          ConfImport.addTemplateField(resTemplateId, idx + 1, 1, (String )outItem.get("ElemName"), 
              (String )outItem.get("ItemDesc"), "fixed-field", "str", IsMust);
        }
      }
    }

    masterReqTemplateId = ConfImport.addMasterTemplate(systemId, "I", systemName);
    if (null != inprocd) {
      ConfImport.addTemplateField(masterReqTemplateId, 1, 1, (String )inprocd.get("ElemName"), 
          (String )inprocd.get("ItemDesc"), "fixed-field", "str", "Y");
      ConfImport.addMasterTemplateField(masterReqTemplateId, 2, 1, 
          "fixed-field", "str", "Y", (String )inprocd.get("ElemName"));
    }
    masterResTemplateId = ConfImport.addMasterTemplate(systemId, "O", systemName);
    if (null != outprocd) {
      ConfImport.addTemplateField(masterResTemplateId, 1, 1, (String )outprocd.get("ElemName"), 
          (String )outprocd.get("ItemDesc"), "fixed-field", "str", "Y");
      ConfImport.addMasterTemplateField(masterReqTemplateId, 2, 1, 
          "fixed-field", "str", "Y", (String )outprocd.get("ElemName"));
    }
  }
}

// System.out.printf("[Name=%s] [SvcDesc=%s] [IFmt=%s] [OFmt=%s]\n", name, desc, ifmt, ofmt);
