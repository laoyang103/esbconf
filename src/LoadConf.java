import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;;
import java.io.IOException;;
import java.io.FileNotFoundException;

import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.text.DecimalFormat;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;

public class LoadConf {

  public static int svcCount = 0;
  public static HashMap<String,Object> allFmtMap = null;
  public static HashMap<String,Object> allSvcMap = null;
  public static HashMap<String,Object> enum8583Map = null;
  public static HashMap<String,Integer> enumNumMap = null;

  private static String currStart = null;
  private static HashMap<String,Object> currFmt = null;
  private static HashMap<String,Object> currItem = null;
  private static HashMap<String,Object> lastTagItem = null;

  private static HashMap<String,Integer> svcNameMap = null;

  private static String[] ibmCharList = new String[]{"ATOEA", "ETOAA", "ATOEO", "ETOAO"};
  private static String[] ibmPackList = new String[]{"enum_num_to_pack", "enum_pack_to_num", "NumToPack_zero", "PackToNum_zero"};

  static {
    allFmtMap = new HashMap<String,Object>();
    allSvcMap = new HashMap<String,Object>();
    enum8583Map = new HashMap<String,Object>();
    enumNumMap = new HashMap<String,Integer>();
    svcNameMap = new HashMap<String,Integer>();
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

    if (reader.getLocalName().equals("ItemTab")) {
      nattr = reader.getAttributeCount();
      for(i = 0; i < nattr; i++) {
        key = reader.getAttributeLocalName(i);
        val = reader.getAttributeValue(i);
        if ("RecNum".equals(key)) {
          currFmt.put("_count", Integer.parseInt(val));
          break;
        }
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
      String elemName = (String )currItem.get("ElemName");
      if (null != elemName && 0 == elemName.indexOf("ISO_8583_")) {
        itemsList = (ArrayList<HashMap<String,Object>> )currFmt.get("items");
        itemsList.add((HashMap<String,Object> )enum8583Map.get(elemName.substring(9, 12)));
      } else if ("tag".equals((String )currItem.get("XmlType"))) {
        lastTagItem = currItem;
        itemsList = (ArrayList<HashMap<String,Object>> )currFmt.get("items");
        itemsList.add(currItem);
      } else {
        itemsList = (ArrayList<HashMap<String,Object>> )currFmt.get("items");
        itemsList.add(currItem);
      }
    }
  }

  private static void processFmtCDATA(XMLStreamReader reader) {
    int i, len;
    String cdata, newName;
    cdata = reader.getText();
    if (null == currItem || null == lastTagItem) return;
    if ("attr".equals(currItem.get("XmlType"))) {
      if (currStart.equals("ConstData")) {
        newName = String.format("%s[%s=%s]", lastTagItem.get("XmlName"), currItem.get("XmlName"), cdata.replace("\"", ""));
        lastTagItem.put("XmlName", newName);
      }
    }
    if ("tag".equals(currItem.get("XmlType")) && !"".equals(cdata)) {
      if (currStart.equals("ItemExpr")) {
        currItem.put("_dataType", "str");
        for (i = 0; i < ibmCharList.length; i++) {
          if (-1 != cdata.indexOf(ibmPackList[i])) {
            currItem.put("_dataType", "without-point-decima");
            break;
          }
        }
      }
      if (0 == (Integer)currItem.get("_length") && currStart.equals("LenDataExpr")) {
        len = 0;
        if (-1 != cdata.indexOf("enum_get_fld_len")) {
          String lenStr = cdata.split("\"")[3];
          if (enumNumMap.containsKey(lenStr)) {
            len = enumNumMap.get(lenStr);
            len = (len / 2) + 1;
          }
        } else if (StringUtils.isNumeric(cdata)) {
          len = Integer.parseInt(cdata);
        }
        currItem.put("_length", len);
      }
    }
  }

  private static void loadFmt(String confFile) throws FileNotFoundException, XMLStreamException {
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

  private static String getSvcNameFromMap(String svcName) {
    Integer num = svcNameMap.get(svcName);
    if (null == num) {
      svcNameMap.put(svcName, 0);
    } else {
      svcNameMap.put(svcName, num + 1);
      svcName += num;
    }
    return svcName;
  }

  private static void loadSvc(String confFile, String conflictRule) throws FileNotFoundException, XMLStreamException {
    String key, val; 
    int i, nattr, transStart, transOffset;
    XMLInputFactory factory = XMLInputFactory.newFactory();
    XMLStreamReader reader = factory.createXMLStreamReader(new FileReader(confFile));
    HashMap<String,Object> currSvc, existSvc;

    while (reader.hasNext()) {
      if (XMLStreamConstants.START_ELEMENT != reader.next()) continue;
      if (reader.getLocalName().equals("ServiceTab")) {
        nattr = reader.getAttributeCount();
        for(i = 0; i < nattr; i++) {
          key = reader.getAttributeLocalName(i);
          val = reader.getAttributeValue(i);
          if ("RecNum".equals(key)) {
            svcCount += Integer.parseInt(val);
            break;
          }
        }
      }
      if (reader.getLocalName().equals("Service")) {
        String serviceKey = null;
        currSvc = new HashMap<String,Object>();
        nattr = reader.getAttributeCount();
        for(i = 0; i < nattr; i++) {
          key = reader.getAttributeLocalName(i);
          val = reader.getAttributeValue(i);
          if ("".equals(val)) continue;
          if ("SvcDesc".equals(key)) {
            val = getSvcNameFromMap(val);
          }
          if ("Name".equals(key)) {
            serviceKey = val;
          }
          currSvc.put(key, val);
        }
        existSvc = (HashMap<String,Object> )allSvcMap.get(serviceKey);
        if (null != existSvc && "drop".equals(conflictRule)) {
          // System.out.printf("drop service %s form %s\n", serviceKey, confFile);
        } else if (null != existSvc && "fillIn".equals(conflictRule)) {
          existSvc.put("IFmt", currSvc.get("IFmt"));
        } else if (null != existSvc && "fillOut".equals(conflictRule)) {
          existSvc.put("OFmt", currSvc.get("OFmt"));
        } else {
          currSvc.put("_svcName", serviceKey);
          allSvcMap.put(serviceKey, currSvc);
        }
      }
    }
  }

  private static void loadEnumNum(String enumFile) throws FileNotFoundException, IOException {
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

  private static void loadEnum8583(String csvFile) throws FileNotFoundException, IOException {
    String line = null;
    FileReader reader = new FileReader(csvFile);
    BufferedReader br = new BufferedReader(reader);
    DecimalFormat df  = new DecimalFormat("000");
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
      enum8583Map.put(df.format(Integer.parseInt(lineSplit[0])), fieldMap);
    }
    br.close();
    reader.close();
  }

  private static String iconvFile(String formatFile, String encode) {
    String outFile = formatFile + ".utf";
    if ("utf-8".equals(encode)) {
      return formatFile;
    }
    try {
      List<String> cmdList = Arrays.asList("iconv", "-f", encode, "-t", "utf-8", "-c", formatFile);
      ProcessBuilder builder = new ProcessBuilder(cmdList);
      builder.redirectOutput(new File(outFile));
      Process p = builder.start(); 
    } catch (Exception e) {   
      e.printStackTrace();  
      return null;
    }   
    return outFile;
  }

  private static String getSvcFileConflictRule(String svcFileName) {
    String rule = "drop";
    if (svcFileName.contains("CGET") || svcFileName.contains("CPUT")) {
      if (svcFileName.contains("CGET")) {
        rule = "fillIn";
      }
    }
    if (svcFileName.contains("SGET") || svcFileName.contains("SPUT")) {
      if (svcFileName.contains("SPUT")) {
        rule = "fillOut";
      }
    }
    return rule;
  }

  public static void load(String formatFiles, String serviceFiles, String encode) {
    int i;
    String fileName, conflictRule;
    String[] strList = null; 
    try {
      loadEnum8583("origin/8583.csv");
      loadEnumNum("origin/enum_number.txt");
      strList = serviceFiles.split(";");
      for (i = 0; i < strList.length; i++) {
        fileName = iconvFile(strList[i], encode);
        conflictRule = getSvcFileConflictRule(strList[i]);
        LoadConf.loadSvc(fileName, conflictRule);
      }
      strList = formatFiles.split(";");
      for (i = 0; i < strList.length; i++) {
        fileName = iconvFile(strList[i], encode);
        LoadConf.loadFmt(fileName);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

/*
   for (String key : allSvcMap.keySet()) {
   HashMap<String, Object> svc = (HashMap<String, Object> )allSvcMap.get(key);
   System.out.println(svc.get("_svcName") + " " + svc.get("IFmt") + "   " + svc.get("OFmt"));
   }
 */
