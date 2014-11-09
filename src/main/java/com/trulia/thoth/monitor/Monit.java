package com.trulia.thoth.monitor;

import com.trulia.thoth.pojo.ServerDetail;
import org.apache.solr.client.solrj.SolrServer;

import java.util.concurrent.Callable;

/**
 * User: dbraga - Date: 11/9/14
 */
public abstract class Monit implements Callable<MonitorResult> {
  public ServerDetail serverDetail;
  public SolrServer realtimeThoth;
  public SolrServer shrankThoth;

  public abstract void alert(String body);
  public abstract MonitorResult call();
}
