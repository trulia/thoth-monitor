//package com;
//
//import com.trulia.thoth.utility.Mailer;
//
///**
// * User: dbraga - Date: 8/26/14
// */
//public class Test {
//
//  public static void main(String[] args){
//
//    String body =  "<br>";
//
//    body +="<table style=\"text-align:center\">\n" +
//            "<tr>\n" +
//            "  <td><img src=\"http://f.cl.ly/items/3c1U2D2D0f410V0e213C/thoth-logo.png\"></td>\n" +
//            "  <td><h1>Thoth Qtime Monitor</h1></td>\n" +
//            "</tr>\n" +
//            "</table> <hr><br>";
//
//    body += "Current mean Qtime for search211(8050) is higher than mean Qtime of the same pool <br>";
//    body += "search211 mean QTime:" +"<b style=\"color:red\";> "+"153.88059701492537" + "</b><br>";
//    body += "Pool user mean qtime:" +"<b>"+" 16.290605546123373" + "</b><br>";
//    //
//    //body +="<table style=\"text-align:center;border:\"1\";\">\n" +
//    //        "<th>search211(8050) mean QTime</th>\n" + "<th>pool() mean QTime</th>\n" +
//    //        "<tr>\n" +
//    //        "  <td></td>\n" +
//    //        "  <td></td> \n" +
//    //        "</tr>\n" +
//    //        "</table>";
//
//
//    new Mailer("Thoth monitor: QTime alert for search501(8050)[active]",
//            body,
//            1).sendMail();
//  }
//}
