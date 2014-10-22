package com.trulia.thoth.monitor;

import com.trulia.thoth.pojo.ServerDetail;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

/**
 * User: dbraga - Date: 8/16/14
 */
public class QTimeMonitor extends Monitor {
  private Double currentMeanQTime;
  private Double historicalMeanQtime;
  private Double historicalStandardDeviation;
  private Double poolCurrentMeanQTime;
  private Double poolStandardDeviation;
  private int standardDeviationFactor = 3;
  private String alertBody = "";

  private static final String HISTORICAL_QTIME = "avg_qtime_d";
  private static final String CURRENT_QTIME = "qtime_i";

  public QTimeMonitor(ServerDetail serverDetail, SolrServer realTimeThoth, SolrServer historicalDataThoth) {
    super(serverDetail, realTimeThoth, historicalDataThoth);
  }


  public Double[] fetchHistoricalMeanQtime(String timeframe) throws SolrServerException {
    SolrQuery sq = new SolrQuery(basicQuery + " AND masterDocumentMin_b:true AND masterTime_dt:["+ timeframe + " TO *]");
    sq.setGetFieldStatistics(HISTORICAL_QTIME);
    QueryResponse qr = historicalDataThoth.query(sq);
    if (qr.getResults().getNumFound() > 1) return  new Double[]{
            (Double) qr.getFieldStatsInfo().get(HISTORICAL_QTIME).getMean(),
            (Double) qr.getFieldStatsInfo().get(HISTORICAL_QTIME).getStddev()};
    else return null;
  }

  public Double fetchCurrentMeanQTime() throws SolrServerException {
    SolrQuery sq = new SolrQuery(basicQuery);
    sq.setGetFieldStatistics(CURRENT_QTIME);
    QueryResponse qr = realTimeThoth.query(sq);
    if (qr.getResults().getNumFound() > 1 ) return (Double) qr.getFieldStatsInfo().get(CURRENT_QTIME).getMean();
    else return null;
  }

  public Double[] fetchCurrentPoolMeanQTime() throws SolrServerException {
    SolrQuery sq = new SolrQuery(basicPoolQuery);
    sq.setGetFieldStatistics(CURRENT_QTIME);
    QueryResponse qr = realTimeThoth.query(sq);
    if (qr.getResults().getNumFound() > 1 ) return new Double[]{
            (Double) qr.getFieldStatsInfo().get(CURRENT_QTIME).getMean(),
            (Double) qr.getFieldStatsInfo().get(CURRENT_QTIME).getStddev()
    };
    else return null;
  }

  @Override
  public void alert(String body) {
    System.out.println("QTime monitor. Sending alert for " +serverDetail.getName()+"("+ serverDetail.getPort() +")["+serverDetail.getPool()+"]");
    /*new Mailer("Thoth monitor: QTime alert for "+serverDetail.getName()+"("+ serverDetail.getPort() +")["+serverDetail.getPool()+"]",
            body,
            1).sendMail();*/
  }

  @Override
  public MonitorResult call(){
    MonitorResult monitorResult = new MonitorResult();
    try {
      execute();
    }
    catch (SolrServerException e) {
      e.printStackTrace();
    }

    return monitorResult;
  }

  @Override
  public void execute() throws SolrServerException {
    System.out.println("QTime monitoring for server("+serverDetail.getName()+") corename("+serverDetail.getCore()+") port("+serverDetail.getPort()+") pool("+serverDetail.getPool()+")");
    // Fetch current mean QTime
    currentMeanQTime = fetchCurrentMeanQTime();
    if (null == currentMeanQTime) return;

    // Fetch mean Historical QTime for different timeframes and the standard deviation
    for (String timeframe: new String[]{ "NOW/HOUR-1HOUR", "NOW/DAY-1DAY","NOW/DAY-7DAY","NOW/DAY-30DAY" }){
      Double[] stats = fetchHistoricalMeanQtime(timeframe);
      if (stats != null)  {
        historicalMeanQtime = stats[0]; historicalStandardDeviation = stats[1];
        if (currentMeanQTime > historicalMeanQtime + historicalStandardDeviation * standardDeviationFactor ){



          alertBody +=  "<br>";

          alertBody +="<table style=\"text-align:center\">\n" +
                  "<tr>\n" +
                  "  <td><img src=\"http://f.cl.ly/items/3c1U2D2D0f410V0e213C/thoth-logo.png\"></td>\n" +
                  "  <td><h1>Thoth Qtime Monitor</h1></td>\n" +
                  "</tr>\n" +
                  "</table> <hr><br>";

          alertBody += "Current mean QTime for "+serverDetail.getName()+"("+serverDetail.getPort()+") is higher than mean QTime of (NOW - "+timeframe+" <br>";
          alertBody += serverDetail.getName() + " mean QTime:" +"<b style=\"color:red\";> "+""+currentMeanQTime+"" + "</b><br>";
          alertBody += "NOW-" +timeframe + " mean qtime:" +"<b>"+" "+historicalMeanQtime+"" + "</b><br><br><br>";
                  }
      }

    }
    // Fetch mean QTime for the other members of the same pool
    Double[] stats = fetchCurrentPoolMeanQTime();
    if (stats != null)  {
      poolCurrentMeanQTime = stats[0];
      poolStandardDeviation = stats[1];
      if (currentMeanQTime > poolCurrentMeanQTime + poolStandardDeviation * standardDeviationFactor )
      {
        alertBody +=  "<br>";

        alertBody +="<table style=\"text-align:center\">\n" +
                "<tr>\n" +
                "  <td><img src=\"http://f.cl.ly/items/3c1U2D2D0f410V0e213C/thoth-logo.png\"></td>\n" +
                "  <td><h1>Thoth Qtime Monitor</h1></td>\n" +
                "</tr>\n" +
                "</table> <hr><br>";

        alertBody += "Current mean QTime for "+serverDetail.getName()+"("+serverDetail.getPort()+") is higher than mean QTime of the servers of the same pool <br>";
        alertBody += serverDetail.getName() + " mean QTime:" +"<b style=\"color:red\";> "+""+currentMeanQTime+"" + "</b><br>";
        alertBody += "Pool(" + serverDetail.getPool()  +") mean qtime:" +"<b>"+" "+poolCurrentMeanQTime+"" + "</b><br><br><br>";

      }


    }
    if (!"".equals(alertBody)) alert(alertBody);
  }
}
