package com.trulia.thoth.quartz;

import com.trulia.thoth.monitor.PredictorModelHealthMonitor;
import com.trulia.thoth.pojo.ServerDetail;
import com.trulia.thoth.util.ServerCache;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.quartz.*;

import java.net.URL;
import java.util.ArrayList;

/**
 * User: dbraga - Date: 8/16/14
 */

public class MonitorJob implements Job {
  private SolrServer realTimeThoth;
  private SolrServer historicalDataThoth;
  private ServerCache serverCache;
  private ArrayList<ServerDetail> ignoredServerDetails;

  private boolean isPredictorMonitoringEnabled = false;
  private String predictorMonitorUrl = "";
  private String predictorMonitorHealthScoreThreshold = "";

  private boolean isIgnored(ServerDetail serverDetail){
    for (ServerDetail toCheck: ignoredServerDetails){
      if ((toCheck.getName().equals(serverDetail.getName())) &&
          (toCheck.getCore().equals(serverDetail.getCore())) &&
          (toCheck.getPool().equals(serverDetail.getPool())) &&
          (toCheck.getPort().equals(serverDetail.getPort()))){
          return true;
      }
    }
    return false;
  }

  private void monitorThothPredictor(SchedulerContext schedulerContext){
    try {
      if (isPredictorMonitoringEnabled){
        predictorMonitorUrl = (String) schedulerContext.get("predictorMonitorUrl");
        predictorMonitorHealthScoreThreshold = (String) schedulerContext.get("predictorMonitorHealthScoreThreshold");

        if (!"".equals(predictorMonitorUrl) && !"".equals(predictorMonitorHealthScoreThreshold)){
          new PredictorModelHealthMonitor(predictorMonitorUrl, Float.parseFloat(predictorMonitorHealthScoreThreshold)).execute();
        }
      }
    } catch (Exception e){
      System.out.println("Error while trying to monitor thoth predictor, exception: " + e );
    }
  }


  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    SchedulerContext schedulerContext = null;
    try {
      schedulerContext = context.getScheduler().getContext();

      realTimeThoth = new HttpSolrServer((String) schedulerContext.get("thothIndexURI") + "collection1/");
      historicalDataThoth = new HttpSolrServer((String) schedulerContext.get("thothIndexURI") + "shrank/");
      serverCache = (ServerCache) schedulerContext.get("serverCache");
      ignoredServerDetails = (ArrayList<ServerDetail>) schedulerContext.get("ignoredServers");
      isPredictorMonitoringEnabled = (Boolean) schedulerContext.get("isPredictorMonitoringEnabled");


      monitorThothPredictor(schedulerContext);

      //ArrayList<ServerDetail> servers = new MonitoredServers(realTimeThoth, serverCache).getList();
      //
      //
      //System.out.println("Fetching information about the servers done. Start the monitoring");
      //for (ServerDetail serverDetail: servers){
      //  if (isIgnored(serverDetail)) continue;
      //  System.out.println("Start monitoring server (" + serverDetail.getName()+") port(" + serverDetail.getPort()+") coreName("+ serverDetail.getCore()+ ")");
      //  new QTimeMonitor(serverDetail, realTimeThoth, historicalDataThoth).execute();
      //
      //}
      //
      System.out.println("Done with monitoring.");

      realTimeThoth.shutdown();
      historicalDataThoth.shutdown();

      } catch (SchedulerException e) {
      e.printStackTrace();
      }

  }

}