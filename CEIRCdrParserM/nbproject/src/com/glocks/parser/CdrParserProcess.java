package com.glocks.parser;

import com.glocks.constants.PropertyReader;
import com.glocks.dao.MessageConfigurationDbDao;
import com.glocks.dao.PolicyBreachNotificationDao;
import com.glocks.files.FileList;
import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import com.glocks.pojo.MessageConfigurationDb;
import com.glocks.pojo.PolicyBreachNotification;
import com.glocks.util.Util;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

public class CdrParserProcess {

    static Logger logger = Logger.getLogger(CdrParserProcess.class);
    static StackTraceElement l = new Exception().getStackTrace()[0];
    public static PropertyReader propertyReader;

    public static void main(String args[]) {    // OPERATOR   FilePath
        Connection conn = null;
        logger.info(" CdrParserProcess.class");
        conn = (Connection) new com.glocks.db.MySQLConnection().getConnection();
//          String operator = args[0];
        String filePath = null;
        if (args[0] == null) {
            logger.error("Enter the Correct File Path");
        } else {
            filePath = args[0].trim();
        }
        if (!filePath.endsWith("/")) {
            filePath += "/";
        }
        try {
            CdrParserProces(conn, filePath);
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                logger.error("" + e);
            }
        } finally {
            try {
                conn.commit();
                conn.close();
            } catch (SQLException ex) {
                logger.error(ex);
            }

            System.exit(0);
        }
    }

    public static void CdrParserProces(Connection conn, String filePath) {
        logger.debug(" FilePath :" + filePath);
        String source = null;
        String operator = null;
        if (filePath != null) {
            String[] arrOfStr = filePath.split("/", 0);
            int val = 0;
            for (int i = (arrOfStr.length - 1); i >= 0; i--) {
                if (val == 1) {
                    source = arrOfStr[i];
                }
                if (val == 2) {
                    operator = arrOfStr[i].toUpperCase();
                }
                val++;
            }
        }
        String fileName = new FileList().readOldestOneFile(filePath);
        if (fileName == null) {
            logger.debug(" No File Found");
            return;
        }
        logger.debug(" FilePath :" + filePath + "; FileName:" + fileName + ";source : " + source + " ; Operator : " + operator);
        String operator_tag = getOperatorTag(conn, operator);
        logger.debug("Operator tag is [" + operator_tag + "] ");
        ArrayList rulelist = new ArrayList<Rule>();
        String period = checkGraceStatus(conn);
        logger.debug("Period is [" + period + "] ");
        rulelist = getRuleDetails(operator, conn, operator_tag, period);
        addCDRInProfileWithRule(operator, conn, rulelist, operator_tag, period, filePath, source, fileName);

    }

    private static void addCDRInProfileWithRule(String operator, Connection conn, ArrayList<Rule> rulelist, String operator_tag, String period, String filePath, String source, String fileName) {
        int output = 0;
        propertyReader = new PropertyReader();
        String my_query = null;
        HashMap<String, String> my_rule_detail;
        String failed_rule_name = null;
        String failed_rule_id = null;
        String finalAction = "";
        int usageInsert = 0;
        int usageUpdate = 0;
        int duplicateInsert = 0;
        int duplicateUpdate = 0;
        int nullInsert = 0;
        int nullUpdate = 0;
        File file = null;
        String line = null;
        String[] data = null;
        BufferedReader br = null;
        FileReader fr = null;
        BufferedWriter bw1 = null;
        int counter = 1;
        int foreignMsisdn = 0;
        int fileParseLimit = 1;
        Statement stmt = null;
        try {
            String server_origin = propertyReader.getPropValue("serverName").trim();
            logger.info("  serverName   " + server_origin);
//             
            //   stmt = conn.createStatement();
            file = new File(filePath + fileName);
            int fileCount = 0;
            try (Stream<String> lines = Files.lines(file.toPath())) {
                fileCount = (int) lines.count();
                logger.debug("File Count: " + fileCount);
            } catch (Exception e) {
                logger.warn("" + e);
            }
            // if fileName present in SQlFolder , get count of file else make countt 1 
            fileParseLimit = getExsistingSqlFileDetails(conn, operator, source, fileName);
            fr = new FileReader(file);
            br = new BufferedReader(fr);
            bw1 = getSqlFileWriter(conn, operator, source, fileName);
//               new com.glocks.files.FileList().moveCDRFile(conn, fileName, operator, filePath, source);
            Date p2Starttime = new Date();
            HashMap<String, String> device_info = new HashMap<String, String>();
            List<String> sourceTacList = new ArrayList<String>();
            RuleFilter rule_filter = new RuleFilter();
            // CDR File Writer
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date();
            String sdfTime = sdf.format(date);
            boolean isOracle = conn.toString().contains("oracle");
            String dateFunction = Util.defaultDateNow(isOracle);
            logger.debug("fileParseLimit " + fileParseLimit);
//               for (int i = 0; i <= fileParseLimit; i++) {
            br.readLine();
//                    counter++;
//               }
            logger.debug("...,. ");
            while ((line = br.readLine()) != null) {
                stmt = conn.createStatement();
                logger.debug(" Line Started ");
//                    logger.debug("Line  started  " + line);
                data = line.split(",", -1);
                logger.debug(Arrays.toString(data));
                device_info.put("file_name", fileName.trim());
                if ((((data[2].trim().startsWith("19") || data[2].trim().startsWith("00")) ? data[2].substring(2) : data[2])).startsWith("855") && data[1].trim().startsWith("456")) {
                    device_info.put("IMEI", data[0].trim());
                    device_info.put("IMSI", data[1].trim());
                    device_info.put("MSISDN", ((data[2].trim().startsWith("19") || data[2].trim().startsWith("00")) ? data[2].substring(2) : data[2]));
                    device_info.put("record_type", data[3].trim());
                    device_info.put("system_type", data[4].trim());
                    device_info.put("source", data[5].trim());
                    device_info.put("raw_cdr_file_name", data[6].trim());
//                         device_info.put("raw_cdr_file_name", data[6].trim());
//                         device_info.put("imei_arrival_time",     data [7].trim().substring(data [7].trim().indexOf("202"), data [7].trim().indexOf("202") + 8));
                    String imei_arrivalTime = null;
                    if (data[7].trim().startsWith("20")) {
                        imei_arrivalTime = data[7].trim().startsWith("202") ? data[7].trim() : "20" + data[7].trim();
                    } else if (data[7].trim().startsWith("21")) {
                        imei_arrivalTime = "20" + data[7].trim();
                    } else {
                        imei_arrivalTime = data[7].trim().substring(data[7].trim().indexOf("202"), (data[7].trim().indexOf("202") + 8));
                    }
                    imei_arrivalTime= imei_arrivalTime.substring(0,8);
                    device_info.put("imei_arrival_time", imei_arrivalTime);
                    device_info.put("operator", operator.trim());
                    device_info.put("record_time", sdfTime);
                    device_info.put("operator_tag", operator_tag);
                    logger.debug("   sarted " + device_info.get("IMEI"));
                    // add for foreign db .. foreign msisdn is not handled , dsicard them , but make entry of them in reporting_db
                    if (device_info.get("IMEI") == null || device_info.get("IMEI").equals("") || device_info.get("IMEI") == "" || device_info.get("IMEI").length() < 8) {
//                         logger.debug("Imei Null");
//                         if (device_inrule_namefo.get("MSISDN") != null) {
                        output = checkDeviceNullDB(conn, device_info.get("MSISDN"));
                        logger.debug(" Null Output " + output);
                        if (output == 0) {
                            my_query = "insert into device_null_db (msisdn,imsi,create_filename,update_filename,"
                                    + "updated_on,modified_on , created_on,record_type,system_type , operator ,record_time  ) "
                                    + "values('" + device_info.get("MSISDN") + "',"
                                    + "'" + device_info.get("IMSI") + "',"
                                    + "'" + device_info.get("file_name") + "',"
                                    + "'" + device_info.get("file_name") + "',"
                                    + "" + dateFunction + ","
                                    + "" + dateFunction + ","
                                    + "" + dateFunction + ","
                                    + "'" + device_info.get("record_type") + "',"
                                    + "'" + device_info.get("record_type") + "',"
                                    + "'" + device_info.get("operator") + "',"
                                    + "'" + device_info.get("record_time") + "'"
                                    + ")";
                            nullInsert++;
                        } else {
                            my_query = "update device_null_db set "
                                    + "update_filename = '" + device_info.get("file_name")
                                    // +"',updated_on='"+device_info.get("record_time")+
                                    + "',MODIFIED_ON=" + dateFunction
                                    + " where msisdn= '" + device_info.get("MSISDN") + "'";
                            logger.debug("need to update");
                            nullUpdate++;
//                              }
                        }
                    } else {  // IMEI NOT NULL
                        String failedRuleDate = null;
                        sourceTacList.add(device_info.get("IMEI").substring(0, 8));
                        device_info.put("tac", device_info.get("IMEI").substring(0, 8));
                        my_rule_detail = rule_filter.getMyRule(conn, device_info, rulelist);
                        logger.debug("getMyRule done");
                        if (my_rule_detail.get("rule_name") != null) {
                            failed_rule_name = my_rule_detail.get("rule_name");
                            failed_rule_id = my_rule_detail.get("rule_id");
//                        action = my_rule_detail.get("action");          
                            period = my_rule_detail.get("period");
                            failedRuleDate = dateFunction;
                        }
                        logger.debug("FailedRule Not Null done" + failed_rule_id + " :: " + failed_rule_name);

                        if (failed_rule_id == null || failed_rule_name == null || failed_rule_name.equalsIgnoreCase("EXISTS_IN_ALL_ACTIVE_DB")) {
                            finalAction = "ALLOWED";
                            failed_rule_name = null;
                            failed_rule_id = null;
                        } else {
                            logger.debug("FailedRule EXIST_IN_GSMABLACKLIST_DB");
                            if (failed_rule_name.equalsIgnoreCase("EXIST_IN_GSMABLACKLIST_DB") || failed_rule_name.equalsIgnoreCase("EXIST_IN_BLACKLIST_DB")) {
                                finalAction = "BLOCKED";
                            } else if (period.equalsIgnoreCase("Grace")) {
                                finalAction = "SYS_REG";
                            } else if (period.equalsIgnoreCase("Post_Grace")) {
                                finalAction = "USER_REG";
                                sendMessageToMsisdn(conn, device_info.get("MSISDN"), device_info.get("IMEI"));
                            }
                        }
                        logger.debug("Failed Condition Success");
                        String gsmaTac = getValidInvalidTac(conn, device_info.get("IMEI").substring(0, 8));
                        output = checkDeviceUsageDB(conn, device_info.get("IMEI").substring(0, 14), device_info.get("MSISDN"), device_info.get("imei_arrival_time"));
                        if (output == 0) {      // imei not found in usagedb
                            my_query = " insert into device_usage_db (actual_imei,msisdn,imsi,create_filename,update_filename,"
                                    + "updated_on,created_on,system_type,failed_rule_id,failed_rule_name,tac,period,action "
                                    + " , mobile_operator , record_type , failed_rule_date,  modified_on ,record_time, imei , raw_cdr_file_name , imei_arrival_time , source, feature_name , server_origin , update_imei_arrival_time) "
                                    + " values('" + device_info.get("IMEI") + "',"
                                    + "'" + device_info.get("MSISDN") + "',"
                                    + "'" + device_info.get("IMSI") + "',"
                                    + "'" + device_info.get("file_name") + "',"
                                    + "'" + device_info.get("file_name") + "',"
                                    + "" + dateFunction + ","
                                    + "" + dateFunction + ","
                                    + "'" + device_info.get("system_type") + "',"
                                    + "'" + failed_rule_id + "',"
                                    + "'" + failed_rule_name + "',"
                                    + "'" + device_info.get("IMEI").substring(0, 8) + "',"
                                    + "'" + period + "',"
                                    + "'" + finalAction + "' , "
                                    + "'" + device_info.get("operator") + "' , "
                                    + "'" + device_info.get("record_type") + "',"
                                    + "" + failedRuleDate + ","
                                    + "" + dateFunction + ","
                                    + "'" + device_info.get("record_time") + "' , "
                                    + "'" + device_info.get("IMEI").substring(0, 14) + "', "
                                    + "'" + device_info.get("raw_cdr_file_name") + "',"
                                    + "'" + device_info.get("imei_arrival_time") + "',"
                                    + "'" + device_info.get("source") + "' , "
                                    + "'" + gsmaTac + "' , "
                                    + "'" + server_origin + "' , "
                                    + "'" + device_info.get("imei_arrival_time") + "'"
                                    + ")";
                            usageInsert++;
                        }
                        if (output == 1) {  //  new ArrivalTime  came  from file  >  arrival time in db already                      // imei found with same msisdn  update_raw_cdr_file_name , update_imei_arrival_time  
                            my_query = "update device_usage_db set "
                                    + "update_filename = '" + device_info.get("file_name")
                                      + "', updated_on=" + dateFunction + ""
                                    + ", modified_on=" + dateFunction + ""
                                    + ", failed_rule_date=" + failedRuleDate + ""
                                    + ", failed_rule_id='" + failed_rule_id
                                    + "', failed_rule_name='" + failed_rule_name
                                    + "',period='" + period
                                    + "',update_raw_cdr_file_name='" + device_info.get("raw_cdr_file_name")
                                    + "',update_imei_arrival_time='" + device_info.get("imei_arrival_time")
                                    + "',update_source ='" + device_info.get("source")
                                    + "',server_origin ='" + server_origin
                                    + "',action='" + finalAction
                                    + "' where imei ='" + device_info.get("IMEI").substring(0, 14) + "'";
                            usageUpdate++;
                        }

                        if (output == 3) {                        // imei found with same msisdn  update_raw_cdr_file_name , update_imei_arrival_time  
                            my_query = "update device_usage_db set "
                                    + "update_filename = '" + device_info.get("file_name")
                                    //								+"', updated_on=TO_DATE('"+device_info.get("record_time")+"','yyyy/mm/dd hh24:mi:ss')"
                                    + "', updated_on=" + dateFunction + ""
                                    + ", modified_on=" + dateFunction + ""
                                    + ", failed_rule_date=" + failedRuleDate + ""
                                    + ", failed_rule_id='" + failed_rule_id
                                    + "', failed_rule_name='" + failed_rule_name
                                    + "',period='" + period
                                    + "',update_source ='" + device_info.get("source")
                                    + "',server_origin ='" + server_origin
                                    + "',action='" + finalAction
                                    + "' where imei ='" + device_info.get("IMEI").substring(0, 14) + "'";
                            usageUpdate++;
                        }

                        if (output == 2) {                                 // imei found with different msisdn
                            output = checkDeviceDuplicateDB(conn, device_info.get("IMEI").substring(0, 14), device_info.get("MSISDN") , device_info.get("imei_arrival_time") );
                            if (output == 0) {
                                my_query = "insert into device_duplicate_db (actual_imei,msisdn,imsi,create_filename,update_filename,"
                                        + "updated_on,created_on,system_type,failed_rule_id,failed_rule_name,tac,period,action  "
                                        + " , mobile_operator , record_type , failed_rule_date,  modified_on  ,record_time, imei ,raw_cdr_file_name , imei_arrival_time , source , feature_name ,server_origin "
                                        + "  , update_raw_cdr_file_name ,update_imei_arrival_time  ) "
                                        + "values('" + device_info.get("IMEI") + "',"
                                        + "'" + device_info.get("MSISDN") + "',"
                                        + "'" + device_info.get("IMSI") + "',"
                                        + "'" + device_info.get("file_name") + "',"
                                        + "'" + device_info.get("file_name") + "',"
                                        + "" + dateFunction + ","
                                        + "" + dateFunction + ","
                                        + "'" + device_info.get("system_type") + "',"
                                        + "'" + failed_rule_id + "',"
                                        + "'" + failed_rule_name + "',"
                                        + "'" + device_info.get("IMEI").substring(0, 8) + "',"
                                        + "'" + period + "',"
                                        + "'" + finalAction + "' , "
                                        + "'" + device_info.get("operator") + "' , "
                                        + "'" + device_info.get("record_type") + "' , "
                                        + "" + failedRuleDate + " , "
                                        + "" + dateFunction + ",  "
                                        + "'" + device_info.get("record_time") + "', "
                                        + "'" + device_info.get("IMEI").substring(0, 14) + "', "
                                        + "'" + device_info.get("raw_cdr_file_name") + "',"
                                        + "'" + device_info.get("imei_arrival_time") + "',"
                                        + "'" + device_info.get("source") + "' , "
                                        + "'" + gsmaTac + "' , "
                                        + "'" + server_origin + "' , "
                                         + "'" + device_info.get("raw_cdr_file_name") + "',"
                                        + "'" + device_info.get("imei_arrival_time") + "' "
                                        + ")";
                                duplicateInsert++;
                            }   else if(output == 1)  {
                                my_query = "update device_duplicate_db set "
                                        + "update_filename = '" + device_info.get("file_name")
                                        //	+"', updated_on=TO_DATE('"+device_info.get("record_time")+"','yyyy/mm/dd hh24:mi:ss')"
                                        + "', updated_on=" + dateFunction + ""
                                        + ", modified_on=" + dateFunction + ""
                                        + ", failed_rule_id='" + failed_rule_id
                                        + "', failed_rule_name='" + failed_rule_name
                                        + "',period='" + period
                                        + "',update_raw_cdr_file_name='" + device_info.get("raw_cdr_file_name")
                                        + "',update_source ='" + device_info.get("source")
                                        + "',update_imei_arrival_time='" + device_info.get("imei_arrival_time")
                                        + "',server_origin='" + server_origin
                                        + "',action='" + finalAction
                                        + "' where msisdn='" + device_info.get("MSISDN") + "'  and imei='" + device_info.get("IMEI").substring(0, 14) + "'";
                                duplicateUpdate++;
                            }else{
                                
                                my_query = "update device_duplicate_db set "
                                + "update_filename = '" + device_info.get("file_name")
                                + "', updated_on=" + dateFunction + ""
                                + ", modified_on=" + dateFunction + ""
                                + ", failed_rule_id='" + failed_rule_id
                                + "', failed_rule_name='" + failed_rule_name
                                + "',period='" + period
                                + "',update_source ='" + device_info.get("source")
                                + "',server_origin='" + server_origin
                                + "',action='" + finalAction
                                + "' where msisdn='" + device_info.get("MSISDN") + "' and imei='" + device_info.get("IMEI").substring(0, 14) + "'";

                                     duplicateUpdate++;
                            }
                        }
                    }
                    logger.info("query : " + my_query);
                    if (my_query.contains("insert")) {
                        stmt.executeUpdate(my_query);
                        try {
                            conn.commit();
                        } catch (Exception e) {
                            logger.info("Exception in insert :: : " + e);
                        }
                    } else {
                        bw1.write(my_query + ";");
                        bw1.newLine();
                    }
                    counter++;
                    logger.info("Remaining List :: " + (fileCount - counter));
                } else {
                    logger.info(" foreign msisdn added");
                    foreignMsisdn++;
                }
                stmt.close();  //

            }   //While End   
            Date p2Endtime = new Date();
            cdrFileDetailsUpdate(conn, operator, device_info.get("file_name"), usageInsert, usageUpdate, duplicateInsert, duplicateUpdate, nullInsert, nullUpdate, p2Starttime, p2Endtime, "all", counter, device_info.get("raw_cdr_file_name"), foreignMsisdn , server_origin);
            try {
                Map<String, Long> map = sourceTacList.stream()
                        .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
                map.forEach((k, v) -> new HexFileReader().insertSourceTac(conn, k, device_info.get("file_name"), v, "source_tac_info"));
            } catch (Exception e) {
                logger.error("sourceTac Error " + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
            }
            new com.glocks.files.FileList().moveCDRFile(conn, fileName, operator, filePath, source);
        } catch (Exception e) {
            logger.error("Errors " + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);

        } finally {
            try {
                stmt.close();
                br.close();
                bw1.close();
                conn.commit();
            } catch (Exception e) {
                logger.error(".. " + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
            }
        }

    }

    private static int checkDeviceDuplicateDB(Connection conn, String imei, String msisdn , String imeiArrivalTime) {
        String query = null;
        ResultSet rs1 = null;
        Statement stmt = null;
        int status = 0;
        try {
            query = "select * from device_duplicate_db where imei ='" + imei + "' and msisdn = '" + msisdn + "'";
            logger.debug("device_dupliate db" + query);
            stmt = conn.createStatement();
            rs1 = stmt.executeQuery(query);
            while (rs1.next()) {
                if ((rs1.getString("UPDATE_IMEI_ARRIVAL_TIME") == null) || (Integer.parseInt(rs1.getString("UPDATE_IMEI_ARRIVAL_TIME")) < Integer.parseInt(imeiArrivalTime))) {     // imei found with same msisdn 
                    status = 1;   //   update_raw_cdr_file_name='" + device_info.get("raw_cdr_file_name")
                }else{
                    status = 3;
                }
            }
        } catch (Exception e) {
            logger.error("" + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
        } finally {
            try {
                rs1.close();
                stmt.close();

            } catch (SQLException e) {
                logger.error("" + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
            }
        }
        return status;
    }

    private static int checkDeviceUsageDB(Connection conn, String imeiIndex, String msisdn, String imeiArrivalTime) {
        String query = null;
        ResultSet rs1 = null;
        Statement stmt = null;
        int status = 0;    // imei not found 
        try {
            query = "select * from device_usage_db where imei ='" + imeiIndex + "'     ";
            logger.debug("device usage db" + query);
            stmt = conn.createStatement();
            rs1 = stmt.executeQuery(query);
            while (rs1.next()) {
                logger.debug("UPDATE_IMEI_ARRIVAL_TIME " + rs1.getString("UPDATE_IMEI_ARRIVAL_TIME"));
                logger.debug("msisdn " + rs1.getString("msisdn"));

                if (rs1.getString("msisdn").equalsIgnoreCase(msisdn)) {
                    if ((rs1.getString("UPDATE_IMEI_ARRIVAL_TIME") == null) || (Integer.parseInt(rs1.getString("UPDATE_IMEI_ARRIVAL_TIME")) < Integer.parseInt(imeiArrivalTime))) {     // imei found with same msisdn 
                        status = 1;   //   update_raw_cdr_file_name='" + device_info.get("raw_cdr_file_name")
                    } else {
                        status = 3;   // not to update as UPDATE_IMEI_ARRIVAL_TIME is  greater already
                    }
                } else {
                    status = 2;                   //  // imei found with different msisdn
                }
            }
            rs1.close();
            stmt.close();
        } catch (Exception e) {
            logger.error("" + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
        } finally {
            try {
                rs1.close();
                stmt.close();

            } catch (Exception e) {
                logger.error("  :/ " + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
            }
        }
        return status;
    }

    public static int checkDeviceNullDB(Connection conn, String msisdn) {
        String query = null;
        ResultSet rs1 = null;
        Statement stmt = null;
        int status = 0;
        try {
            query = "select * from device_null_db where msisdn='" + msisdn + "'";
            logger.debug("device usage db" + query);
            stmt = conn.createStatement();
            rs1 = stmt.executeQuery(query);
            while (rs1.next()) {
                status = 1;
            }
        } catch (Exception e) {
            logger.error("" + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
        } finally {
            try {
                stmt.close();
                rs1.close();
            } catch (SQLException e) {
                logger.error("" + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
            }
        }
        return status;
    }

    private static ArrayList getRuleDetails(String operator, Connection conn, String operator_tag, String period) {
        ArrayList rule_details = new ArrayList<Rule>();
        String query = null;
        ResultSet rs1 = null;
        Statement stmt = null;
        try {
            query = "select a.id as rule_id,a.name as rule_name,b.output as output,b.grace_action, b.post_grace_action, b.failed_rule_action_grace, b.failed_rule_action_post_grace from rule_engine a, rule_engine_mapping b where  a.name=b.name  and a.state='Enabled' and b.feature='CDR' and   b." + period + "_action !='NA'         order by b.rule_order asc";
            logger.debug("Query is " + query);
            stmt = conn.createStatement();
            rs1 = stmt.executeQuery(query);
            while (rs1.next()) {
                if (rs1.getString("rule_name").equalsIgnoreCase("IMEI_LENGTH")) {
                    if (operator_tag.equalsIgnoreCase("GSM")) {
//						Rule rule = new Rule(rs1.getString("rule_name"),rs1.getString("output"),rs1.getString("rule_id"),period, rs1.getString(period+"_action"));
                        Rule rule = new Rule(rs1.getString("rule_name"), rs1.getString("output"), rs1.getString("rule_id"), period, rs1.getString(period + "_action"), rs1.getString("failed_rule_action_" + period));
                        rule_details.add(rule);
                    }
                } else {
//					Rule rule = new Rule(rs1.getString("rule_name"),rs1.getString("output"),rs1.getString("rule_id"),period, rs1.getString(period+"_action"));
                    Rule rule = new Rule(rs1.getString("rule_name"), rs1.getString("output"), rs1.getString("rule_id"), period, rs1.getString(period + "_action"), rs1.getString("failed_rule_action_" + period));
                    rule_details.add(rule);
                }
            }
            rs1.close();
            stmt.close();

        } catch (Exception e) {
            logger.error("" + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
        } finally {
            try {
                rs1.close();
                stmt.close();

            } catch (SQLException e) {
                logger.error("" + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
            }
        }
        return rule_details;
    }

//     static ResultSet operatorDetails(Connection conn, String operator) {
//          Statement stmt = null;
//          ResultSet rs = null;
//          String query = null;
//          try {
//               query = "select * from r ep_schedule_config_db where operator='" + operator + "'";
//               stmt = conn.createStatement();
//               return rs = stmt.executeQuery(query);
//          } catch (Exception e) {
//               logger.error("  Error operatorDetails::" + e);
//          }
//          return rs;
//     }
    static void cdrFileDetailsUpdate(Connection conn, String operator, String fileName, int usageInsert, int usageUpdate, int duplicateInsert, int duplicateUpdate, int nullInsert, int nullUpdate, Date P2StartTime, Date P2EndTime, String source, int counter, String raw_cdr_file_name, int foreignMsisdn , String server_origin) {
        String query = null;
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Statement stmt = null;
        query = "insert into  cdr_file_details_db (created_on ,MODIFIED_ON ,total_inserts_in_usage_db,total_updates_in_usage_db ,total_insert_in_dup_db , total_updates_in_dup_db , total_insert_in_null_db , total_update_in_null_db , P2StartTime , P2EndTime ,operator , file_name, total_records_count , raw_cdr_file_name  ,source  ,foreignMsisdn  , STATUS , server_origin) "
                + "values(    " + Util.defaultDateNow(true) + "  ,    " + Util.defaultDateNow(true) + " ,'" + usageInsert + "' , '" + usageUpdate + "'  , '" + duplicateInsert + "' , '" + duplicateUpdate + "' "
                + " ,'" + nullInsert + "' ,'" + nullUpdate + "', TO_DATE('" + df.format(P2StartTime) + "','YYYY-MM-DD HH24:MI:SS') , TO_DATE('" + df.format(P2EndTime) + "','YYYY-MM-DD HH24:MI:SS') ,   '" + operator + "', '" + fileName + "' , '" + (counter - 1) + "' , '" + raw_cdr_file_name + "' , '" + source + "'  , '" + foreignMsisdn + "' , 'End' ,  '" + server_origin + "')  ";
        logger.info(" qury is " + query);

//        query = "update     cdr_file_details_db  set  "
//                + "created_on = current_timestamp ,MODIFIED_ON=  current_timestamp,total_inserts_in_usage_db ='" + usageInsert + "' ,total_updates_in_usage_db= '" + usageUpdate + "'  ,total_insert_in_dup_db  = '" + duplicateInsert + "' ,total_updates_in_dup_db= '" + duplicateUpdate + "' "
//                + " , total_insert_in_null_db = '" + nullInsert + "' ,total_update_in_null_db= '" + nullUpdate + "',P2StartTime=  TO_DATE('" + df.format(P2StartTime) + "','YYYY-MM-DD HH24:MI:SS') ,P2EndTime=  TO_DATE('" + df.format(P2EndTime) + "','YYYY-MM-DD HH24:MI:SS') , operator =  '" + operator + "' ,total_records_count = '" + (counter - 3) + "' ,raw_cdr_file_name= '" + raw_cdr_file_name + "' ,source= '" + source + "'  , foreignMsisdn='" + foreignMsisdn + "' where file_name= '" + fileName + "'    ";
//        logger.info(" qury is " + query);            
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(query);
            stmt.close();
        } catch (SQLException e) {
            logger.error("" + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                logger.error("" + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
            }
        }

    }

    private static void sendMessageToMsisdn(Connection conn, String msisdn, String imei) {

        MessageConfigurationDbDao messageConfigurationDbDao = new MessageConfigurationDbDao();
        PolicyBreachNotificationDao policyBreachNotificationDao = new PolicyBreachNotificationDao();
        MessageConfigurationDb messageDb = null;

        try {
            Optional<MessageConfigurationDb> messageDbOptional = messageConfigurationDbDao.getMessageDbTag(conn, "USER_REG_MESSAGE");

            if (messageDbOptional.isPresent()) {
                messageDb = messageDbOptional.get();
                String message = messageDb.getValue().replace("<imei>", imei);
                PolicyBreachNotification policyBreachNotification = new PolicyBreachNotification("SMS", message, "", msisdn, imei);
                policyBreachNotificationDao.insertNotification(conn, policyBreachNotification);

            }

        } catch (Exception e) {
            logger.error("" + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
        }

    }

    private static String checkGraceStatus(Connection conn) {
        String period = "";
        String query = null;
        ResultSet rs1 = null;
        Statement stmt = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date currentDate = new Date();
        Date graceDate = null;
        try {
            query = "select value from system_configuration_db where tag='GRACE_PERIOD_END_DATE'";
            logger.info("Check Grace End Date [" + query + "]");
            stmt = conn.createStatement();
            rs1 = stmt.executeQuery(query);
            while (rs1.next()) {
                graceDate = sdf.parse(rs1.getString("value"));
                if (currentDate.compareTo(graceDate) > 0) {
                    period = "post_grace";
                } else {
                    period = "grace";
                }
            }
            logger.info("Period is " + period);
        } catch (Exception e) {
            logger.error("" + e);
        } finally {
            try {
                rs1.close();
                stmt.close();
            } catch (SQLException e) {
                logger.error("" + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
            }

        }
        return period;
    }

    private static String getOperatorTag(Connection conn, String operator) {
        String operator_tag = "";
        String query = null;
        ResultSet rs1 = null;
        Statement stmt = null;
        try {
            query = "select * from system_config_list_db where tag='OPERATORS' and interp='" + operator + "'";
            logger.debug("get operator tag [" + query + "]");
            stmt = conn.createStatement();
            rs1 = stmt.executeQuery(query);
            while (rs1.next()) {
                operator_tag = rs1.getString("tag_id");
            }
        } catch (Exception e) {
            logger.error("" + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
            operator_tag = "GSM";  // if no opertor found
        } finally {
            try {
                rs1.close();
                stmt.close();
            } catch (SQLException e) {
                logger.error("" + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
//                    e.printStackTrace();
            }
        }
        return operator_tag;
    }

    private static BufferedWriter getSqlFileWriter(Connection conn, String operator, String source, String file) {

//          operator = operator.toLowerCase();
        HexFileReader hfr = new HexFileReader();
        BufferedWriter bw1 = null;
        try {
            String foldrName = hfr.getFilePath(conn, "Sql_Query_Folder") + "/" + operator.toLowerCase() + "/"; //   
            File file1 = new File(foldrName);
            if (!file1.exists()) {
                file1.mkdir();
            }
            foldrName += source + "/";
            file1 = new File(foldrName);
            if (!file1.exists()) {
                file1.mkdir();
            }
            String fileNameInput1 = foldrName + file + ".sql";
            logger.info("SQL_LOADER NAME ..  " + fileNameInput1);
            File fout1 = new File(fileNameInput1);
            FileOutputStream fos1 = new FileOutputStream(fout1, true);
            bw1 = new BufferedWriter(new OutputStreamWriter(fos1));
        } catch (Exception e) {
            logger.error("e " + e);
        }
        return bw1;
    }

    public static void renameSqlFile(Connection conn, String operator, String source, String file) {

        HexFileReader hfr = new HexFileReader();
        String foldrName = hfr.getFilePath(conn, "Sql_Query_Folder") + "/" + operator.toLowerCase() + "/"; //   
        foldrName += source + "/";
        String fileNameInput1 = foldrName + file + ".sql";
        logger.info("SQL_    " + fileNameInput1);

        File oldName = new File(fileNameInput1);
        File newName = new File(foldrName + file + ".tmp");

        if (oldName.renameTo(newName)) {
            logger.debug("SQL    Renamed successfully");
        } else {
            logger.debug("Error while    Renaming");
        }

    }

    public static BufferedWriter getRuleFileWriter(Connection conn) {
        HexFileReader hfr = new HexFileReader();
        BufferedWriter bw1 = null;
        try {
            String foldrName = hfr.getFilePath(conn, "Sql_Query_Folder") + "/";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            File file1 = new File(foldrName);
            file1.mkdir();
            String fileNameInput1 = foldrName + "Rule_" + sdf.format(Calendar.getInstance().getTime()) + ".txt";
            logger.info("Rule NAME   " + fileNameInput1);
            File fout1 = new File(fileNameInput1);
            FileOutputStream fos1 = new FileOutputStream(fout1, true);
            bw1 = new BufferedWriter(new OutputStreamWriter(fos1));
        } catch (Exception e) {
            logger.error("e " + e);
        }
        return bw1;
    }

    private static String getFileDetaiRecords(Connection conn, String operator) {
        HexFileReader hfr = new HexFileReader();
        String basePath = "";
        String intermPath = "";

        BufferedReader br = null;
        try {
            basePath = hfr.getFilePath(conn, "smart_file_path");
            if (!basePath.endsWith("/")) {
                basePath += "/";
            }
            intermPath = basePath + operator.toLowerCase() + "/";

        } catch (Exception e) {
            logger.error("E " + e);
        }
        return intermPath;
    }

    private static String getFolderNameByOpertor(Connection conn, String intermPath, String opertor) {
        String query = null;
        String mainFolder = null;
        String folderList = null;
        Statement stmt = null;
        ResultSet rs = null;
        File fldr = null;
        try {
            query = "select value from system_configuration_db where tag= '" + opertor + "_folder_list'  ";
            logger.debug("query: " + query);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);
            while (rs.next()) {
                folderList = rs.getString("value");
            }
            rs.close();
            stmt.close();
            logger.debug("folderList: " + folderList);
            String folderArr[] = folderList.split(",");
            for (String val : folderArr) {
                fldr = new File(intermPath + val.trim() + "/output/");
                logger.debug("fldr : " + fldr);
                logger.debug("fldr.listFiles().length : " + fldr.listFiles().length);
                if (fldr.listFiles().length > 0) {
                    mainFolder = val;
                    break;
                }
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            logger.error("Error : " + e);
            e.printStackTrace();
        }
        return mainFolder + "/";
    }

    private static int getExsistingSqlFileDetails(Connection conn, String operator, String source, String file) {
        int fileCount = 1;

        HexFileReader hfr = new HexFileReader();
        File file1 = null;
        try {
            String foldrName = hfr.getFilePath(conn, "Sql_Query_Folder") + "/" + operator.toLowerCase() + "/"; //   
            foldrName += source + "/";
            String fileNameInput1 = foldrName + file + ".sql";
            try {
                logger.info("SQL " + fileNameInput1);
                file1 = new File(fileNameInput1);

//                    BufferedReader reader = new BufferedReader(new FileReader(fileNameInput1));
//                    int lines = 0;
//                    while (reader.readLine() != null) {
//                         lines++;
//                    }
//                    reader.close();
                File myObj = new File(fileNameInput1);
                if (myObj.delete())  ;

//                    try (Stream<String> lines = Files.lines(file1.toPath())) {
//                         fileCount = (int) lines.count();
//                         logger.info("File Count of Sql File: " + fileCount);
//                    }
            } catch (Exception e) {
                logger.error("File not   exist : " + e);
            }

        } catch (Exception e) {
            logger.error("Err0r : " + e);
        }
        return fileCount;
    }

    private static String getValidInvalidTac(Connection conn, String imeiTac) {

        String rsltTac = "U";  // undefined
        String query = null;
        ResultSet rs1 = null;
        Statement stmt = null;

        ResultSet rs2 = null;
        Statement stmt2 = null;
        int counts = 0;

        try {
            query = "select count(*) from gsma_tac_db  where  DEVICE_ID='" + imeiTac + "'";
            logger.debug("get [" + query + "]");
            stmt = conn.createStatement();
            rs1 = stmt.executeQuery(query);
            while (rs1.next()) {
                counts = rs1.getInt(1);
            }
            if (counts != 0) {
                rsltTac = "V";
            } else {
                query = "select count(*) from gsma_invalid_tac_db  where  tac ='" + imeiTac + "'";
                logger.debug("get [" + query + "]");
                stmt2 = conn.createStatement();
                rs2 = stmt.executeQuery(query);
                while (rs2.next()) {
                    counts = rs2.getInt(1);
                }
                if (counts != 0) {
                    rsltTac = "I";
                } else {
                    rsltTac = "U";
                }
                rs2.close();
                stmt2.close();
            }

            rs1.close();
            stmt.close();
        } catch (Exception e) {
            logger.error("" + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
        } finally {
            try {
                rs1.close();
                stmt.close();

                rs2.close();
                stmt2.close();
            } catch (Exception e) {
//                    logger.error("This  Error in try Catch Can Be Ignored :" + l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + e);
//                    e.printStackTrace();
            }
        }
        return rsltTac;
    }

}
