package com.trulia.thoth.util;

import com.trulia.thoth.pojo.ServerDetail;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;

import java.util.ArrayList;
import java.util.List;

/**
 * User: dbraga - Date: 8/23/14
 */
public class MonitoredServers {

  private SolrServer realTimeThoth;
  private ServerCache serverCache;
  private static final String FACET_PIVOT_FIELDS = "hostname_s,coreName_s";
  private static final String POOL = "pool_s";
  private static final String PORT = "port_i";
  //TODO : externalize this
  private static final int FACET_LIMIT = 1000; // Bump this if you are monitoring more than 1K Servers


  public MonitoredServers(SolrServer realTimeThoth, ServerCache serverCache){
    this.realTimeThoth = realTimeThoth;
    this.serverCache = serverCache;
  }


  private ServerDetail fetchServerDetails(String hostname, String coreName) throws SolrServerException {
    System.out.println("Fetching server details for hostname("+hostname+") coreName("+coreName+")");
    QueryResponse qr = realTimeThoth.query(new SolrQuery("hostname_s:\"" + hostname + "\"" +" AND " + "coreName_s:\"" +coreName + "\" AND NOT exception_b:true AND NOT slowQuery_b:true").setRows(1));
    SolrDocumentList solrDocumentList = qr.getResults();
    String pool = (String)solrDocumentList.get(0).getFieldValue(POOL);
    String port = solrDocumentList.get(0).getFieldValue(PORT).toString();
    return new ServerDetail(hostname, pool, port, coreName);
  }


  public ArrayList<ServerDetail> getList() throws SolrServerException {
    ArrayList<ServerDetail> serverDetails = new ArrayList<ServerDetail>();
    // Using HierarchicalFaceting to fetch server details .http://wiki.apache.org/solr/HierarchicalFaceting
    QueryResponse qr = realTimeThoth.query(new SolrQuery("*:*").addFacetPivotField(FACET_PIVOT_FIELDS).setRows(0).setFacetLimit(FACET_LIMIT));
    NamedList<List<PivotField>> pivots = qr.getFacetPivot();
    System.out.println("Found " + pivots.get(FACET_PIVOT_FIELDS).size()+" servers to monitor. Fetching information for these servers. Please wait");
    for (PivotField pivot: pivots.get(FACET_PIVOT_FIELDS)){
      String hostname = (String) pivot.getValue();
      for (PivotField pf: pivot.getPivot()){
        String coreName = (String) pf.getValue();
        if (serverCache.contains(hostname, coreName)){
          serverDetails.add(serverCache.fetchDetails(hostname,coreName));
        } else{
          ServerDetail detail = fetchServerDetails(hostname,coreName);
          serverCache.add(detail);
          serverDetails.add(detail);
        }
      }
    }
    return serverDetails;
  }

}
