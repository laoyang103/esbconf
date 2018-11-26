import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConfImport {

  private static Connection con;
  private static String driver = "com.mysql.jdbc.Driver";
  private static String url = "jdbc:mysql://192.168.99.67:3308/mocksystem?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8";
  private static String user = "root";
  private static String password = "admin@123#";

  private static String MOCK_TYPE = "VC";
  private static String MOCK_CM_TYPE = "2";
  private static String MOCK_MESSAGE_TYPE = "xml";
  private static String MOCK_MESSAGE_ENCODING = "GB2312";

  static {
    try {
      Class.forName(driver);
      con = DriverManager.getConnection(url,user,password);
    } catch (Exception e) {
      e.printStackTrace();
    } 
  }

  public static int addMockSystem(String systemCode, String systemName) {
    String sql = null;
    sql = String.format("INSERT INTO mock_system VALUES(NULL, \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"NULL\", \"0\", \"0\")", 
        systemCode, systemName, MOCK_TYPE, MOCK_CM_TYPE, MOCK_MESSAGE_TYPE, MOCK_MESSAGE_ENCODING);
    System.out.println(sql);
    try {
      Statement statement = con.createStatement();
      statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
      ResultSet rs = statement.getGeneratedKeys();
      if (rs.next()) return rs.getInt(1);
      else return -1;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  public static int addMockTrans(String transCode, String transName, int mockId) {
    String sql = null;
    sql = String.format("INSERT INTO mock_trans VALUES(NULL, \"%s\", \"%s\", %d, \"%s\", \"%s\", \"NULL\", \"0\", \"0\")", 
        transCode, transName, mockId, MOCK_MESSAGE_TYPE, MOCK_MESSAGE_ENCODING);
    System.out.println(sql);
    try {
      Statement statement = con.createStatement();
      statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
      ResultSet rs = statement.getGeneratedKeys();
      if (rs.next()) return rs.getInt(1);
      else return -1;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  public static int addMasterTemplate(int mockId, String io, String systemName) {
    String sql = null;
    String fieldStr = "id,system_id,trans_code,message_code,message_name,message_type,message_encoding,message_io,version";
    sql = String.format("INSERT INTO trans_message_template (%s) VALUES(NULL, %d, \"NULL\", \"master\", \"%s\", \"%s\", \"%s\", \"%s\", \"1.0\")", 
        fieldStr, mockId, systemName, MOCK_MESSAGE_TYPE, MOCK_MESSAGE_ENCODING, io);
    System.out.println(sql);
    try {
      Statement statement = con.createStatement();
      statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
      ResultSet rs = statement.getGeneratedKeys();
      if (rs.next()) return rs.getInt(1);
      else return -1;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  public static int addTemplate(int mockId, String io, String transCode, String transName) {
    String sql = null;
    String fieldStr = "id,system_id,trans_code,message_code,message_name,message_type,message_encoding,message_io,version";
    sql = String.format("INSERT INTO trans_message_template (%s) VALUES(NULL, %d, \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"1.0\")", 
        fieldStr, mockId, transCode, transCode, transName, MOCK_MESSAGE_TYPE, MOCK_MESSAGE_ENCODING, io);
    System.out.println(sql);
    try {
      Statement statement = con.createStatement();
      statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
      ResultSet rs = statement.getGeneratedKeys();
      if (rs.next()) return rs.getInt(1);
      else return -1;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  public static int addTemplateField(int templateId, int idx, int level, 
      String code, String name, String fieldType, String dataType, String required) {
    String sql = null;
    String fieldStr = "id, template_id, field_index, field_level, field_code, field_system_code, field_name, field_type, data_type, required";
    sql = String.format("INSERT INTO trans_message_field (%s) VALUES(NULL, %d, %d, \"%d\", \"%s\", \"NULL\", \"%s\", \"%s\", \"%s\", \"%s\")", 
        fieldStr, templateId, idx, level, code, name, fieldType, dataType, required);
    System.out.println(sql);
    try {
      Statement statement = con.createStatement();
      statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
      ResultSet rs = statement.getGeneratedKeys();
      if (rs.next()) return rs.getInt(1);
      else return -1;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  public static int addMasterTemplateField(int templateId, int idx, int level, 
      String fieldType, String dataType, String required, String procd) {
    String sql = null;
    String fieldStr = "id, template_id, field_index, field_level, field_code, field_system_code, field_name, field_type, data_type, param_1, required";
    sql = String.format("INSERT INTO trans_message_field (%s) VALUES(NULL, %d, %d, \"%d\", \"ref_trancode\", \"NULL\", \"交易码引用\", \"%s\", \"%s\", \"${%s}\", \"%s\")", 
        fieldStr, templateId, idx, level, fieldType, dataType, procd, required);
    System.out.println(sql);
    try {
      Statement statement = con.createStatement();
      statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
      ResultSet rs = statement.getGeneratedKeys();
      if (rs.next()) return rs.getInt(1);
      else return -1;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }
}
