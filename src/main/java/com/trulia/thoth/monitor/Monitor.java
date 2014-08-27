package com.trulia.thoth.monitor;

import com.trulia.thoth.pojo.ServerDetail;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;

/**
 * User: dbraga - Date: 8/16/14
 */

public abstract class Monitor{
  public ServerDetail serverDetail;
  public String basicQuery;
  public SolrServer realTimeThoth;
  public SolrServer historicalDataThoth;

  public Monitor(ServerDetail serverDetail, SolrServer realTimeThoth, SolrServer historicalDataThoth){
    this.serverDetail = serverDetail;
    this.basicQuery = "hostname_s:\""+ serverDetail.getName() + "\""+" AND " +
            "pool_s:\"" + serverDetail.getPool() + "\""+" AND " +
            "port_i:" + serverDetail.getPort() + " AND " +
            "coreName_s:\"" + serverDetail.getCore() + "\" AND NOT exception_b:true AND source_s:SolrQuery";
    this.realTimeThoth = realTimeThoth;
    this.historicalDataThoth = historicalDataThoth;
  }
  public void execute() throws SolrServerException {}
  public void alert(String body) {}


}