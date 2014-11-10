package com.trulia.thoth.monitor;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

/**
 * User: dbraga - Date: 11/9/14
 */
public class ZeroHitsMonitor extends Monitor {

  private Long currentZeroHitsQueriesRatio;
  private Double historicalZeroHitsRatio;
  private Double historicalZeroHitsStandardDeviation;
  private Double poolCurrentMeanQTime;
  private Double poolStandardDeviation;
  private int standardDeviationFactor = 1;
  private String alertBody = "";
  private String basicQuery;
  private String basicPoolQuery;
  
  @Override
  public void alert(String body) {
    System.out.println("ZeroHitsMonitor . Sending alert for " +serverDetail.getName()+"("+ serverDetail.getPort() +")["+serverDetail.getPool()+"]");
    String subject = "Thoth monitor, ZeroHitsMonitor alert for " +serverDetail.getName()+"("+ serverDetail.getPort() +")["+serverDetail.getPool()+"]. ";
    String content = body;
    mailer.sendMail(subject, content);
  }


  public Double[] fetchHistoricalZeroHitsQueriesRatio(String timeframe) throws SolrServerException {
    SolrQuery sq = new SolrQuery(basicQuery + " AND masterDocumentMin_b:true AND masterTime_dt:["+ timeframe + " TO *]");
    sq.setGetFieldStatistics("zeroHits-count_i");
    QueryResponse qr = shrankThoth.query(sq);

    if (qr.getResults().getNumFound() > 1)
      return new Double[]{
        (Double) qr.getFieldStatsInfo().get("zeroHits-count_i").getMean()/qr.getFieldStatsInfo().get("zeroHits-count_i").getCount(),
        (Double) qr.getFieldStatsInfo().get("zeroHits-count_i").getStddev()};
    else return null;
  }

  /**
   * Fetch current ratio of zero hits queries
   * @return ratio
   * @throws SolrServerException
   */
  public Long fetchCurrentZeroHitsQueriesRatio() throws SolrServerException {
    SolrQuery sq = new SolrQuery(basicQuery + " AND hits_i:0");
    QueryResponse qr = realtimeThoth.query(sq);
    long numberOfZeroHitsQueries = qr.getResults().getNumFound();
    sq = new SolrQuery(basicQuery);
    qr = realtimeThoth.query(sq);
    long numberOfQueries = qr.getResults().getNumFound();
    return numberOfZeroHitsQueries/numberOfQueries;
  }
  
  @Override
  public MonitorResult call() {
    basicPoolQuery = "pool_s:\"" + serverDetail.getPool() + "\""+" AND " +
        "port_i:" + serverDetail.getPort() + " AND " +
        "coreName_s:\"" + serverDetail.getCore() + "\" AND NOT exception_b:true "; // AND source_s:SolrQuery"; TODO: source?
    basicQuery = "hostname_s:\""+ serverDetail.getName() + "\""+" AND " + basicPoolQuery;

    
    System.out.println("ZeroHits monitoring for server("+serverDetail.getName()+") corename("+serverDetail.getCore()+") port("+serverDetail.getPort()+") pool("+serverDetail.getPool()+")");

    try{
      // Fetch current Ratio of  Zero Hits queries
      currentZeroHitsQueriesRatio = fetchCurrentZeroHitsQueriesRatio();
      if (null == currentZeroHitsQueriesRatio) return null;

      // Fetch mean Historical ZeroHits for different timeframes and the standard deviation
      for (String timeframe: new String[]{ "NOW/HOUR-1HOUR", "NOW/DAY-1DAY","NOW/DAY-7DAY","NOW/DAY-30DAY" }){
        Double[] stats = fetchHistoricalZeroHitsQueriesRatio(timeframe);
        if (stats != null)  {
          historicalZeroHitsRatio = stats[0]; historicalZeroHitsStandardDeviation = stats[1];
          if (currentZeroHitsQueriesRatio > historicalZeroHitsRatio + historicalZeroHitsStandardDeviation * standardDeviationFactor ){
            String body = "Current Zero hits ratio ("+ currentZeroHitsQueriesRatio +") is higher than (NOW-"+timeframe+") mean QTime ("+ historicalZeroHitsRatio +") + stdev("+ historicalZeroHitsStandardDeviation +"). ";
            appendToAlert(body);

          }
        }
      }

      if (!"".equals(alertBody)) alert(alertBody);
    } catch (Exception e){
     e.printStackTrace();
    }
   return new MonitorResult(); //TODO
  }

  /**
   * Utility used to append thoth header to each alert message
   * @param body fixed body
   */
  private void appendToAlert(String body){
    alertBody += alertHeader;
    alertBody += body;
  }

}
