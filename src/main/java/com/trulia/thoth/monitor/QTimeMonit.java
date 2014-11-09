package com.trulia.thoth.monitor;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

/**
 * User: dbraga - Date: 11/9/14
 */
public class QTimeMonit extends Monit {
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

  public Double fetchCurrentMeanQTime() throws SolrServerException {
    SolrQuery sq = new SolrQuery(basicQuery);
    sq.setGetFieldStatistics(CURRENT_QTIME);
    QueryResponse qr = realtimeThoth.query(sq);
    if (qr.getResults().getNumFound() > 1 ) return (Double) qr.getFieldStatsInfo().get(CURRENT_QTIME).getMean();
    else return null;
    
  }

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
  public void execute() {

  }

  @Override
  public void alert(String body) {
    System.out.println("Alert!"  + body);

  }

  @Override
  public MonitorResult call() {
    basicPoolQuery = "pool_s:\"" + serverDetail.getPool() + "\""+" AND " +
        "port_i:" + serverDetail.getPort() + " AND " +
        "coreName_s:\"" + serverDetail.getCore() + "\" AND NOT exception_b:true "; // AND source_s:SolrQuery"; TODO: source?
    basicQuery = "hostname_s:\""+ serverDetail.getName() + "\""+" AND " + basicPoolQuery;


    try {
      System.out.println("QTime monitoring for server("+serverDetail.getName()+") corename("+serverDetail.getCore()+") port("+serverDetail.getPort()+") pool("+serverDetail.getPool()+")");
      // Fetch current mean QTime
      currentMeanQTime = fetchCurrentMeanQTime();
      if (null == currentMeanQTime) return null; //TODO

      // Fetch mean Historical QTime for different time frames and the standard deviation
      for (String timeFrame: new String[]{ "NOW/HOUR-1HOUR", "NOW/DAY-1DAY","NOW/DAY-7DAY","NOW/DAY-30DAY" }){
        Double[] stats = fetchHistoricalMeanQtime(timeFrame);
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

            alertBody += "Current mean QTime for "+serverDetail.getName()+"("+serverDetail.getPort()+") is higher than mean QTime of (NOW - "+timeFrame+" <br>";
            alertBody += serverDetail.getName() + " mean QTime:" +"<b style=\"color:red\";> "+""+currentMeanQTime+"" + "</b><br>";
            alertBody += "NOW-" +timeFrame + " mean qtime:" +"<b>"+" "+historicalMeanQtime+"" + "</b><br><br><br>";
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
    } catch (SolrServerException e){
      e.printStackTrace();
    }
    
    
   
    
    
    return new MonitorResult();
  }
}
