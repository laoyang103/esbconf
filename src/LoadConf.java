import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;;
import java.io.IOException;;

import java.util.HashMap;
import java.util.ArrayList;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;

public class LoadConf {

  public static HashMap<String,Object> allFmtMap = null;
  public static HashMap<String,Object> allSvcMap = null;
  public static ArrayList<HashMap<String,Object>> allFmtMap8583 = null;
  public static HashMap<String,Integer> enumNumMap = null;

  private static String currStart = null;
  private static HashMap<String,Object> currFmt = null;
  private static HashMap<String,Object> currItem = null;

  private static String[] ibmCharList = new String[]{"ATOEA", "ETOAA", "ATOEO", "ETOAO"};
  private static String[] ibmPackList = new String[]{"enum_num_to_pack", "enum_pack_to_num", "NumToPack_zero", "PackToNum_zero"};

  static {
    allFmtMap = new HashMap<String,Object>();
    allSvcMap = new HashMap<String,Object>();
    allFmtMap8583 = new ArrayList<HashMap<String,Object>>();
    enumNumMap = new HashMap<String,Integer>();
  }

  private static void processFmtStart(XMLStreamReader reader) {
    int i, nattr;
    String key, val;
    ArrayList<HashMap<String,Object>> itemsList = null;

    currStart = reader.getLocalName();

    if (reader.getLocalName().equals("Format")) {
      currFmt = new HashMap<String,Object>();
      currFmt.put("items", new ArrayList<HashMap<String,Object>>());
      nattr = reader.getAttributeCount();
      for(i = 0; i < nattr; i++) {
        key = reader.getAttributeLocalName(i);
        val = reader.getAttributeValue(i);
        if ("".equals(val)) continue;
        if ("FmtName".equals(key)) {
          allFmtMap.put(val, currFmt);
        }
        currFmt.put(key, val);
      }
    }
    if (reader.getLocalName().equals("Item")) {
      currItem = new HashMap<String,Object>();
      nattr = reader.getAttributeCount();
      for(i = 0; i < nattr; i++) {
        key = reader.getAttributeLocalName(i);
        val = reader.getAttributeValue(i);
        if ("".equals(val)) continue;
        currItem.put(key, val);
      }
      currItem.put("_length", 0);
      currItem.put("_dataType", "str");
      itemsList = (ArrayList<HashMap<String,Object>> )currFmt.get("items");
      itemsList.add(currItem);
    }
  }

  private static void processFmtCDATA(XMLStreamReader reader) {
    int i, len;
    String cdata;
    cdata = reader.getText();
    if ("".equals(cdata)) return;
    if (currStart.equals("ItemExpr")) {
      currItem.put("_dataType", "str");
      for (i = 0; i < ibmCharList.length; i++) {
        if (-1 != cdata.indexOf(ibmPackList[i])) {
          currItem.put("_dataType", "without-point-decima");
          break;
        }
      }
    }
    if (currStart.equals("LenDataExpr")) {
      len = 0;
      if (-1 != cdata.indexOf("enum_get_fld_len")) {
        String lenStr = cdata.split("\"")[3];
        len = enumNumMap.get(lenStr);
      } else if (StringUtils.isNumeric(cdata)) {
        len = Integer.parseInt(cdata);
      }
      currItem.put("_length", len);
    }
  }

  public static void loadFmt(String confFile) throws FileNotFoundException, XMLStreamException {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
    factory.setProperty("http://java.sun.com/xml/stream/properties/report-cdata-event", Boolean.TRUE);
    XMLStreamReader reader = factory.createXMLStreamReader(new FileReader(confFile));

    while (reader.hasNext()) {
      switch (reader.next()) {
        case XMLStreamConstants.START_ELEMENT:
          processFmtStart(reader);
          break;
        case XMLStreamConstants.CDATA:
          processFmtCDATA(reader);
          break;
        default:
          break;
      }
    }
  }

  public static void loadSvc(String confFile, String transCodeRule) throws FileNotFoundException, XMLStreamException {
    String key, val; 
    int i, nattr, transStart, transOffset;
    XMLInputFactory factory = XMLInputFactory.newFactory();
    XMLStreamReader reader = factory.createXMLStreamReader(new FileReader(confFile));
    HashMap<String,Object> currSvc = null;

    String[] ruleArray = transCodeRule.split(",");
    transStart = Integer.parseInt(ruleArray[0]);
    transOffset = Integer.parseInt(ruleArray[1]);

    while (reader.hasNext()) {
      if (XMLStreamConstants.START_ELEMENT != reader.next()) continue;
      if (!reader.getLocalName().equals("Service")) continue;

      currSvc = new HashMap<String,Object>();
      nattr = reader.getAttributeCount();
      for(i = 0; i < nattr; i++) {
        key = reader.getAttributeLocalName(i);
        val = reader.getAttributeValue(i);
        if ("".equals(val)) continue;
        if ("Name".equals(key)) {
          int vallen = val.length();
          String serviceStr = val.substring(transStart, transStart + transOffset);
          allSvcMap.put(serviceStr, currSvc);
          currSvc.put(key, serviceStr);
        } else {
          currSvc.put(key, val);
        }
      }
    }
  }

  public static void loadEnumNum(String enumFile) throws FileNotFoundException, IOException {
    String line = null;
    FileReader reader = new FileReader(enumFile);
    BufferedReader br = new BufferedReader(reader);
    while ((line = br.readLine()) != null) {
      String[] lineSplit = line.split(",");
      enumNumMap.put(lineSplit[0], Integer.parseInt(lineSplit[1]));
    }
    br.close();
    reader.close();
  }

  public static void loadFmt8583(String csvFile) throws FileNotFoundException, IOException {
    String line = null;
    FileReader reader = new FileReader(csvFile);
    BufferedReader br = new BufferedReader(reader);
    while ((line = br.readLine()) != null) {
      HashMap<String,Object> fieldMap = new HashMap<String,Object>();
      String[] lineSplit = line.split(",");
      fieldMap.put("_name" 			, lineSplit[0]);
      fieldMap.put("ItemDesc"   , lineSplit[1]);
      fieldMap.put("_dataType"  , lineSplit[2]);
      fieldMap.put("_length" 		, Integer.parseInt(lineSplit[3]));
      fieldMap.put("IsMust" 	  , lineSplit[4]);
      fieldMap.put("_fieldType" , lineSplit[5]);
      fieldMap.put("param1" 		, lineSplit[6]);
      fieldMap.put("param2" 		, lineSplit[7]);
      fieldMap.put("param3" 		, lineSplit[8]);
      fieldMap.put("encode" 		, lineSplit[9]);
      fieldMap.put("_layer" 		, 1);
      allFmtMap8583.add(fieldMap);
    }
    br.close();
    reader.close();
  }

  public static void load(String formatFiles, String serviceFiles, String transCodeRule) {
    int i;
    String[] strList = null;
    try {
      if ("None".equals(serviceFiles) && "None".equals(transCodeRule)) {
        loadFmt8583(formatFiles);
        return ;
      }
      loadEnumNum("origin/enum_number.txt");
      strList = serviceFiles.split(";");
      for (i = 0; i < strList.length; i++) {
        LoadConf.loadSvc(strList[i], transCodeRule);
      }
      strList = formatFiles.split(";");
      for (i = 0; i < strList.length; i++) {
        LoadConf.loadFmt(strList[i]);
      }
      // System.out.println(allFmtMap);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
