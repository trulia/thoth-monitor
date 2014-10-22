package com.trulia.thoth.monitor;

import com.trulia.thoth.pojo.ServerDetail;
import com.trulia.thoth.utility.Mailer;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

/**
 * User: dbraga - Date: 8/17/14
 */
public class ZeroHitsMonitor extends Monitor {

  private Long currentMeanZeroHits;
  private Double historicalMeanQtime;
  private Double historicalStandardDeviation;
  private Double poolCurrentMeanQTime;
  private Double poolStandardDeviation;
  private int standardDeviationFactor = 1;
  private String alertBody = "";

  public ZeroHitsMonitor(ServerDetail serverDetail, SolrServer realTimeThoth, SolrServer historicalDataThoth) {
    super(serverDetail, realTimeThoth, historicalDataThoth);
  }


  public Double[] fetchHistoricalMeanZeroHits(String timeframe) throws SolrServerException {
    SolrQuery sq = new SolrQuery(basicQuery + " AND masterDocumentMin_b:true AND masterTime_dt:["+ timeframe + " TO *]");
    sq.setGetFieldStatistics("zeroHits-count_i");
    QueryResponse qr = historicalDataThoth.query(sq);
    if (qr.getResults().getNumFound() > 1) return  new Double[]{
            (Double) qr.getFieldStatsInfo().get("zeroHits-count_i").getMean(),
            (Double) qr.getFieldStatsInfo().get("zeroHits-count_i").getStddev()};
    else return null;
  }

  public Long fetchCurrentMeanZeroHits() throws SolrServerException {
    SolrQuery sq = new SolrQuery(basicQuery + " AND hits_i:0");
    sq.setGetFieldStatistics("qtime_i");
    QueryResponse qr = realTimeThoth.query(sq);
    if (qr.getResults().getNumFound() > 1 ) return (Long) qr.getFieldStatsInfo().get("qtime_i").getCount();
    else return null;
  }

  public Double[] fetchCurrentPoolMeanQTime() throws SolrServerException {
    SolrQuery sq = new SolrQuery("*:* AND hits_i:0 AND NOT " + basicQuery);
    sq.setGetFieldStatistics("qtime_i");
    QueryResponse qr = realTimeThoth.query(sq);
    if (qr.getResults().getNumFound() > 1 ) return new Double[]{
            (Double) qr.getFieldStatsInfo().get("qtime_i").getMean(),
            (Double) qr.getFieldStatsInfo().get("qtime_i").getStddev()
    };
    else return null;
  }

  @Override
  public void alert(String body) {
    System.out.println("ZeroHitsMonitor . Sending alert for " +serverDetail.getName()+"("+ serverDetail.getPort() +")["+serverDetail.getPool()+"]");
    /*new Mailer("Thoth monitor: ZeroHitsMonitor alert for "+serverDetail.getName()+"("+ serverDetail.getPort() +")["+serverDetail.getPool()+"]",
            "Thoth monitor, ZeroHitsMonitor alert for " +serverDetail.getName()+"("+ serverDetail.getPort() +")["+serverDetail.getPool()+"]. " + body,
            1).sendMail();*/
  }

  @Override
  public MonitorResult call() throws SolrServerException {
    MonitorResult monitorResult = new MonitorResult();
    execute();
    return monitorResult;
  }

  @Override
  public void execute() throws SolrServerException {
    System.out.println("ZeroHits monitoring for server("+serverDetail.getName()+") corename("+serverDetail.getCore()+") port("+serverDetail.getPort()+") pool("+serverDetail.getPool()+")");

    // Fetch current mean ZeroHits
    currentMeanZeroHits = fetchCurrentMeanZeroHits();
    if (null == currentMeanZeroHits) return;

    // Fetch mean Historical ZeroHits for different timeframes and the standard deviation
    for (String timeframe: new String[]{ "NOW/HOUR-1HOUR", "NOW/DAY-1DAY","NOW/DAY-7DAY","NOW/DAY-30DAY" }){
      Double[] stats = fetchHistoricalMeanZeroHits(timeframe);
      if (stats != null)  {
        historicalMeanQtime = stats[0]; historicalStandardDeviation = stats[1];
        if (currentMeanZeroHits > historicalMeanQtime + historicalStandardDeviation * standardDeviationFactor ){
          alertBody += "Current mean QTime ("+ currentMeanZeroHits +") is higher than (NOW-"+timeframe+") mean QTime ("+historicalMeanQtime+") + stdev("+historicalStandardDeviation+"). ";
        }
      }

    }
    // Fetch mean QTime for the other members of the same pool
    Double[] stats = fetchCurrentPoolMeanQTime();
    if (stats != null)  {
      poolCurrentMeanQTime = stats[0];
      poolStandardDeviation = stats[1];
      if (currentMeanZeroHits > poolCurrentMeanQTime + poolStandardDeviation * standardDeviationFactor )
        alertBody+=" Current mean QTime ("+ currentMeanZeroHits +") is higher than same pool mean QTime ("+poolCurrentMeanQTime+")";

    }
    if (!"".equals(alertBody)) alert(alertBody);
  }
}
