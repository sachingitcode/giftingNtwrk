package com.glocks.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import org.apache.commons.lang.StringUtils;

class tester {

    public static void main(String[] args) {

        String a = new String("HI");
        String b = new String("HI");

        String c = a;

        if (c == a) {
            System.out.println(" = =");
        } else {
            System.out.println(" =!! =");
        }

        a = a.concat("ypp");
         
        System.out.println("" + a);

//        String basePath = "/home/user/Music/testDataFiles/aa";
//
//        File logDir = new File(basePath);
//        File[] logFiles = logDir.listFiles();
//          // System.out.println(logFiles.length);
//        long oldestDate = Long.MAX_VALUE;
//        File oldestFile = null;
//        for (File f : logFiles) {
//            if (f.lastModified() < oldestDate) {
//                oldestDate = f.lastModified();
//                oldestFile = f;
//            }
//        }
//        if (oldestFile != null) {
//             // System.out.println(oldestFile);
//        }n
//        String str = "P00MSC02AP420190501235700917387.dat";
//
//      String str1  =  str.substring(0, str.length() - 10)   ;
//      String str2  =  str1.substring(str1.length() -14, str1.length() );
        // System.out.println("" + str2);
//        Date date = new Date();
//        SimpleDateFormat DateFor = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        String stringDate = DateFor.format(date);
//        System.out.println(stringDate);
// String query = "operator" + "," + "file_name" + "," + "record_time" + "," + "status" + ", created_on , ";
//                        String     values =  "?,?, ,'Init',lllll,";
//                            query = query.substring(0, query.length() - 1) + ") "
//                                    + values.substring(0, values.length() - 1) + ")";
//                            System.out.println("" + query );
//
//        List<String> list = Arrays.asList("1", "2", "2", " 3", "1", "2", "2", " 3", "5", "8", "13", "21", "34", "32", "234", "34", "12",
//                " 3", "5", "8", "13", "8");
//        // we can also use Function.identity() instead of c->c
//        Map<String, Long> map = list.stream()
//                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
//
//        map.forEach((k, v) -> System.out.println(k + " : " + v));
//                  String p2Endtime = java.time.LocalDateTime.now().toString();
//                      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        String P2StartTime = sdf.format(p2Endtime);
//        
//         System.out.println(""+ P2StartTime);
//         
//         
//         DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
//Date dateobj = new Date();
//System.out.println(df.format(dateobj));
//
// File file = new File("/home/maverick/CEIR/Desktop/Docz/extraTacs");
//               int fileCount = 0;
//               try (Stream<String> lines = Files.lines(file.toPath())) {
//                    System.out.println("File Count: " + lines.count());
//               } catch (Exception e) {fileName
//                    System.out.println("" + e);
//               }
//          List<String> sourceTacList = new ArrayList<String>();
//          sourceTacList.add("1");
//          sourceTacList.add("2");
//          sourceTacList.add("3");
//          sourceTacList.add("5");
//          sourceTacList.add("3");
//          sourceTacList.add("6");
//          sourceTacList.add("4");
//          sourceTacList.add("2");
//          sourceTacList.add("3");
//          sourceTacList.add("2");
//          sourceTacList.add("2");
//          sourceTacLi       String formatted = String.format("%07d", number);
//          st.add("2");
//          sourceTacList.add("2");
//          sourceTacList.add("1");
//          sourceTacList.add("1");
//          sourceTacList.add("1");
//          sourceTacList.add("1");
//          sourceTacList.add("2");
//          Map<String, Integer> hm = new HashMap<String, Integer>();
//          for (String ii : sourceTacList) {
//               Integer j = hm.get(ii);
//               hm.put(ii, (j == null) ? 1 : j + 1);
//          }
//          for (Map.Entry<String, Integer> val : hm.entrySet()) {
//               // System.out.println("Element " + val.getKey() + " " + "occurs" + ": " + val.getValue() + " times");
//               if (val.getValue() > 4) {
//                    System.out.println("Element " + val.getKey() + " " + "occurs" + ": " + val.getValue() + " times");
//               }
//          }
//          String str = "/u01/cdr/cellcard/cc_zmsc71/output/abcd.txt";
//          String[] arrOfStr = str.split("/", 0);
//          String fileName = null;
//          String source = null;
//              String operator = null;
//          int val = 0;
//          for (int i = (arrOfStr.length - 1); i >= 0; i--) {
//               if (val == 0) {
//                    fileName = arrOfStr[i];
//               }
//                 if (val == 2) {
//                    source = arrOfStr[i] ;
//               }
//                  if (val == 3) {
//                    operator = arrOfStr[i]  ;
//               }
//               val++;
//          }
//          System.out.println("---------" + fileName);
//          System.out.println("---------" + source);
//           System.out.println("---------" + operator);
//          String intermPath = str.replace(fileName, "");
//          System.out.println(intermPath);
//          String imei_tac = "0123456";
//          if (imei_tac.length() == 7) {
//               imei_tac = "0" + imei_tac;
//          }
//          System.out.println("" + imei_tac);
//          String filePathWithName = "/u01/ceirapp/cdrprocessor/smart/2/process/";
//          String[] arrOfStr = filePathWithName.split("/", 0);
//          String fileName = null;
//          String source = null;
//          String operator = null;
//          String filePath = null;
//
//          int val = 0;
//          for (int i = (arrOfStr.length - 1); i >= 0; i--) {
//               if (val == 1) {
//                    source = arrOfStr[i];
//               }
//               if (val == 2) {
//                    operator = arrOfStr[i];
//               }
//               val++;
//          }
////          filePath = filePathWithName.replace(fileName, "");
//
//          System.out.println("source : (1,2,3,4,5) : " + source);
//          System.out.println(" operaotr SAMRT ::" + operator);
//          System.out.println("filname : XYZ.file " + filePath);
//          new ErrorFileGenrator().gotoErrorFile(conn,  .getString("txn_id"), "  Something went Wrong. Please Contact to Ceir Admin.  ");
//          new CEIRFeatureFileFunctions().UpdateStatusViaApi(conn,  txn_id, 1,  feature);       //1 for reject
//          new CEIRFeatureFileFunctions().updateFeatureFileStatus(conn, txn_id, 5,feature,sub_feature); // update web_action_db          
//          conn.commit();
//          String str = "http://$LOCAL_IP:9503/CEIR/accept-reject/stock @ {  \"action\": 0 ,  \"remarks\":\"\",  \"roleType\": \"CEIRSystem\",  \"txnId\": \"S20200723143721870\"  ,\"featureId\" : 4 }";
//
//          String[] arrOfStr = str.split("@", 2);
//
//          for (String a : arrOfStr) {
//               System.out.println(a);
//          }
//          String line = ",IMEI,Yes,H5Y0J,787654123321451,31-07-2020,New";
//  String[] arrOfFile = line.trim().split(",", 8);
//          
//          System.out.println("" +arrOfFile.length);
//          String MyString = "tHiS is SomE Statement";
//          String MyString1 = StringUtils.capitalize(MyString);
//          System.out.println("" + MyString1);
//          SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd-MMM-yy"); //
//          Calendar cal = Calendar.getInstance();
//          cal.add(Calendar.DATE, -45);
//          String date = dateFormat1.format(cal.getTime());
//          System.out.println("Date from which Data is calculated" + date)
//  "{"list":["ruleName":"USER_REG", "ruleName":"EXISTS_IN_GSMA_TAC_DB"]}"Type a message
//String a = "xyz";
//String ad  =  " \"sachin \" " + ":" +  "\"" + a +   "\""   ;
//          System.out.println(ad);
//     }
//          String created_on = "2020-11-11.0.0. 0. 0";
//          String modified_on = "2020-9-21.11.0. 9. 471000000";
//          SimpleDateFormat format = new SimpleDateFormat("yyyy-M-yy.HH.mm.ss");
//          Date d1 = null;
//          Date d2 = null;
//          long diff = 0L;
//          try {
//               d1 = format.parse(created_on);
//               d2 = format.parse(modified_on);
//               diff = d2.getTime() - d1.getTime();
//               System.out.println("d!!!!  " + d1);
//
//          } catch (Exception e) {
//               e.printStackTrace();
//          }
//          System.out.println("diff// " + diff);
//     }
//          String a = "BTKK20200401000002161625.dat";
//          System.out.println(a.substring(a.indexOf("202"),   a.indexOf("202") + 8 ));
//          
//           SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//  Date date = new Date();  
//    System.out.println(sdf.format(date));  
//          Date date = new Date();
//          LocalDate currentDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
//          int day = currentDate.getDayOfMonth();
//          Month month = currentDate.getMonth();
//          int year = currentDate.getYear();
//          System.out.println("Day: " + day);
//          System.out.println("Month: " + month);
//          System.out.println("Year: " + year);
//          SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yy");
////    Date date = new Date();  
//          System.out.println(formatter.format(date).toUpperCase());
//          String fromDate = DateUtil.nextDate(-5, "yyyy-MM-dd");
//          String testdate = "5";
//          String fromDate = DateUtil.nextDate(Integer.parseInt(testdate.ge) * -1);
//
//          System.out.println(" fromDate  " + fromDate);
//         LocalDateTime myObj = LocalDateTime.now();
//           System.out.println("   ...   " + myObj);
//            String timeSec = myObj.toString().substring ((myObj.toString().length()-3)   ,  (myObj.toString().length())  );
//            System.out.println(";;;;;;;;;;;;;;; " + timeSec);
//        String a = "49328483d5";
//        if (a.matches("^[0-9]+$")) {
//            System.out.println("true ");
//        } else {
//            System.out.println("fasle");
//        }
    }

}
