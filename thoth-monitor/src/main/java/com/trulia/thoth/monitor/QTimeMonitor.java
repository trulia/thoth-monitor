package com.trulia.thoth.monitor;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

/**
 * User: dbraga - Date: 11/9/14
 */
public class QTimeMonitor extends Monitor {
  private Double currentMeanQTime;
  private Double historicalMeanQtime;
  private Double historicalStandardDeviation;
  private Double poolCurrentMeanQTime;
  private Double poolStandardDeviation;
  private int standardDeviationFactor = 3;
  private String alertBody = "";
  private String basicQuery;
  private String basicPoolQuery;
  
  private static final String HISTORICAL_QTIME = "avg_qtime_d";
  private static final String CURRENT_QTIME = "qtime_i";
  private static final String[] timeFrames = new String[]{ "NOW/HOUR-1HOUR", "NOW/DAY-1DAY","NOW/DAY-7DAY","NOW/DAY-30DAY" }; // 1 hour ago, 1 day ago, 7 days ago, 30 days ago


  /**
   * 1) fetch current mean QTime (X) for server
   * 2) fetch historical QTime(Y) for server in different time frames(Z)
   * 3) Alert if X > Y(Z) + stdev
   * 4) fetch current mean QTime for other pool members (K)
   * 5) Alert if  X > K + stdev
   * @return monitor result
   */
  @Override
  public MonitorResult call() {
    try {
      setBasicQueries();
      System.out.println("QTime monitoring for server("+serverDetail.getName()+") corename("+serverDetail.getCore()+") port("+serverDetail.getPort()+") pool("+serverDetail.getPool()+")");
      // Fetch current mean QTime
      currentMeanQTime = fetchCurrentMeanQTime();
      if (null == currentMeanQTime) return null; // Nothing to monitor

      // Fetch mean Historical QTime for different time frames and the standard deviation
      for (String timeFrame: timeFrames){
        Double[] stats = fetchHistoricalMeanQtime(timeFrame);
        if (stats != null)  {
          // Set historical stats
          historicalMeanQtime = stats[0]; historicalStandardDeviation = stats[1];
          if (currentMeanQTime > historicalMeanQtime + historicalStandardDeviation * standardDeviationFactor ){ // Need to alert
            String body = "Current mean QTime for "+serverDetail.getName()+"("+serverDetail.getPort()+") is higher than mean QTime of (NOW - "+timeFrame+" <br>"
            + serverDetail.getName() + " mean QTime:" +"<b style=\"color:red\";> "+""+currentMeanQTime+"" + "</b><br>"
            + "NOW-" +timeFrame + " mean qtime:" +"<b>"+" "+historicalMeanQtime+"" + "</b><br><br><br>";
            appendToAlert(body);
          }
        } else {
          System.out.println("Error while getting stats for historical QTime for " + serverDetail.getName()+"("+serverDetail.getPort());
        }
      }

      // Fetch mean QTime for the other members of the same pool
      Double[] stats = fetchCurrentPoolMeanQTime();
      if (stats != null)  {
        poolCurrentMeanQTime = stats[0]; poolStandardDeviation = stats[1];
        if (currentMeanQTime > poolCurrentMeanQTime + poolStandardDeviation * standardDeviationFactor ){ // Need to alert
          String body = "Current mean QTime for "+serverDetail.getName()+"("+serverDetail.getPort()+") is higher than mean QTime of the servers of the same pool <br>"
          + serverDetail.getName() + " mean QTime:" +"<b style=\"color:red\";> "+""+currentMeanQTime+"" + "</b><br>"
          + "Pool(" + serverDetail.getPool()  +") mean qtime:" +"<b>"+" "+poolCurrentMeanQTime+"" + "</b><br><br><br>";
          appendToAlert(body);
        }
      }

      if (!"".equals(alertBody)) alert(alertBody);
    } catch (SolrServerException e){
      e.printStackTrace();
    }
    return new MonitorResult();
  }
  /**
   * Fetch historical Mean QTime for a specific timeframe
   * @param timeframe
   * @return mean qtime and stddev
   * @throws SolrServerException
   */
  public Double[] fetchHistoricalMeanQtime(String timeframe) throws SolrServerException {
    SolrQuery sq = new SolrQuery(basicQuery + " AND masterDocumentMin_b:true AND masterTime_dt:["+ timeframe + " TO *]");
    sq.setGetFieldStatistics(HISTORICAL_QTIME);
    System.out.println(sq);

    QueryResponse qr = shrankThoth.query(sq);
    if (qr.getResults().getNumFound() > 1) return  new Double[]{
        (Double) qr.getFieldStatsInfo().get(HISTORICAL_QTIME).getMean(),
        (Double) qr.getFieldStatsInfo().get(HISTORICAL_QTIME).getStddev()};
    else return null;
  }

  /**
   * Fetch Mean QTime for current server
   * @return mean qtime
   * @throws SolrServerException
   */
  public Double fetchCurrentMeanQTime() throws SolrServerException {
    SolrQuery sq = new SolrQuery(basicQuery);
    sq.setGetFieldStatistics(CURRENT_QTIME);
    QueryResponse qr = realtimeThoth.query(sq);
    if (qr.getResults().getNumFound() > 1 ) return (Double) qr.getFieldStatsInfo().get(CURRENT_QTIME).getMean();
    else return null;

  }

  /**
   * Fetch Mean QTime for pool members
   * @return mean qtime and stddev
   * @throws SolrServerException
   */
  public Double[] fetchCurrentPoolMeanQTime() throws SolrServerException {
    SolrQuery sq = new SolrQuery(basicPoolQuery);
    sq.setGetFieldStatistics(CURRENT_QTIME);
    QueryResponse qr = realtimeThoth.query(sq);
    if (qr.getResults().getNumFound() > 1 ) return new Double[]{
        (Double) qr.getFieldStatsInfo().get(CURRENT_QTIME).getMean(),
        (Double) qr.getFieldStatsInfo().get(CURRENT_QTIME).getStddev()
    };
    else return null;
  }

  @Override
  public void alert(String body) {
    System.out.println("QTime monitor. Sending alert for " +serverDetail.getName()+"("+ serverDetail.getPort() +")["+serverDetail.getPool()+"]");
    String subject = "Thoth monitor: QTime alert for "+serverDetail.getName()+"("+ serverDetail.getPort() +")["+serverDetail.getPool()+"]";
    String content = body;
    mailer.sendMail(subject, content);
  }

  /**
   * Set basic queries for fetching data about a particular server and the pool that server belongs to
   */
  private void setBasicQueries(){
    basicPoolQuery = "pool_s:\"" + serverDetail.getPool() + "\""+" AND " +
        "port_i:" + serverDetail.getPort() + " AND " +
        "coreName_s:\"" + serverDetail.getCore() + "\" AND NOT exception_b:true "; // AND source_s:SolrQuery"; TODO: source?
    basicQuery = "hostname_s:\""+ serverDetail.getName() + "\""+" AND " + basicPoolQuery;

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
