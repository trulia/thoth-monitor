package com.trulia.thoth.monitor;

import com.trulia.thoth.pojo.ServerDetail;
import com.trulia.thoth.utility.Mailer;
import org.apache.solr.client.solrj.SolrServer;

import java.util.concurrent.Callable;

/**
 * User: dbraga - Date: 11/9/14
 */
public abstract class Monitor implements Callable<MonitorResult> {
  public ServerDetail serverDetail;
  public SolrServer realtimeThoth;
  public SolrServer shrankThoth;
  public Mailer mailer;
  public static final String alertHeader = ""+
      "<br>"
      + "<table style=\"text-align:center\">\n" +
      "<tr>\n" +
      "  <td><img src=\"http://f.cl.ly/items/3c1U2D2D0f410V0e213C/thoth-logo.png\"></td>\n" +
      "  <td><h1>Thoth Qtime Monitor</h1></td>\n" +
      "</tr>\n" +
      "</table> <hr>" +
      "<br>";

  public abstract void alert(String body);
  public abstract MonitorResult call();
}
