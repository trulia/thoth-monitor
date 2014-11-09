package com.trulia.thoth.quartz;

import com.trulia.thoth.monitor.AvailableMonitors;
import com.trulia.thoth.monitor.Monit;
import com.trulia.thoth.monitor.MonitorResult;
import com.trulia.thoth.monitor.PredictorModelHealthMonitor;
import com.trulia.thoth.pojo.ServerDetail;
import com.trulia.thoth.util.ServerCache;
import com.trulia.thoth.util.ThothServers;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.quartz.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * User: dbraga - Date: 8/16/14
 */

public class MonitorJob implements Job {
  private SolrServer realTimeThoth;
  private SolrServer historicalDataThoth;
  private ServerCache serverCache;
  private ArrayList<ServerDetail> ignoredServerDetails;

  public AvailableMonitors availableMonitors;



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

  /**
   * Retrieve list of available Monitors
   * @param serverDetail server to monitor
   * @return list of monitors ready to be executed
   */
  public List<Monit> retrieveMonitorsForServer(ServerDetail serverDetail) {
    List<Monit> monitors = new ArrayList<Monit>();
    ArrayList<Class> monitorClasses = (ArrayList<Class>) availableMonitors.getMonitorList();
    try{
      for (Class klass: monitorClasses){
        Monit obj = (Monit) klass.newInstance();
        // Set default variables for Monitors
        obj.serverDetail = serverDetail;
        obj.realtimeThoth = realTimeThoth;
        obj.shrankThoth = historicalDataThoth;
        monitors.add(obj);
      }
    } catch (Exception e){
      e.printStackTrace();
    }
    return monitors;
  }

  /**
   * Execute all the monitors on the server , concurrently
   * @param serverDetail  server to monitor
   * @throws InterruptedException
   */
  public void executeMonitorsConcurrently(ServerDetail serverDetail) throws InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, ClassNotFoundException {
    List<Monit> monitorList = retrieveMonitorsForServer(serverDetail);

    if (monitorList.size() == 0) {
      System.out.println("No monitors found for server (" + serverDetail.getName() + ") port(" + serverDetail.getPort() + ") coreName(" + serverDetail.getCore() + "). Skipping ...");

    } else {
      System.out.println("Found "+monitorList.size()+" monitor/s for server (" + serverDetail.getName() + ") port(" + serverDetail.getPort() + ") coreName(" + serverDetail.getCore() + ")");
      List<Future<MonitorResult>> futures = new ArrayList<Future<MonitorResult>>();

    // Create a pool of threads, numberOfMonitors max jobs will execute in parallel
    ExecutorService monitorsThreadPool = Executors.newFixedThreadPool(monitorList.size());
    for (Monit monitor : monitorList) {
      futures.add(monitorsThreadPool.submit(monitor));
    }

    for (Future f : futures) {
      try {
        // Check if all the threads are finished.
        MonitorResult monitorResult = (MonitorResult) f.get();  // TODO
      } catch (ExecutionException e) {
        System.out.println("Exception in executeMonitorsConcurrently, while checking the threads status");
        e.getCause().printStackTrace();
      }
    }
    }
    System.out.println("Finished monitoring server (" + serverDetail.getName() + ") port(" + serverDetail.getPort() + ") coreName(" + serverDetail.getCore() + ")");
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    SchedulerContext schedulerContext = null;

    try {
      schedulerContext = context.getScheduler().getContext();

      realTimeThoth = new HttpSolrServer((String) schedulerContext.get("thothIndexURI") + "collection1");
      historicalDataThoth = new HttpSolrServer((String) schedulerContext.get("thothIndexURI") + "shrank/");
      serverCache = (ServerCache) schedulerContext.get("serverCache");
      ignoredServerDetails = (ArrayList<ServerDetail>) schedulerContext.get("ignoredServers");
      isPredictorMonitoringEnabled = (Boolean) schedulerContext.get("isPredictorMonitoringEnabled");
      availableMonitors = (AvailableMonitors) schedulerContext.get("availableMonitors");

      //TODO remove?
      monitorThothPredictor(schedulerContext);

      // Get the list of servers to Monitor from Thoth
      List<ServerDetail> serversToMonitor = new ThothServers().getList(realTimeThoth);
      System.out.println("Fetching information about the servers done. Start the monitoring");
      for (ServerDetail serverDetail: serversToMonitor){
        if (isIgnored(serverDetail)) continue;  // Skip server if ignored
        System.out.println("Start monitoring server (" + serverDetail.getName()+") port(" + serverDetail.getPort()+") coreName("+ serverDetail.getCore()+ ")");
        executeMonitorsConcurrently(serverDetail);
      }

      if (serversToMonitor.size() == 0) System.out.println("No suitable thoth documents found for monitoring. Skipping...");
      System.out.println("Done with monitoring.");

      realTimeThoth.shutdown();
      historicalDataThoth.shutdown();

      } catch (SchedulerException e) {
      e.printStackTrace();
      } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (SolrServerException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

  }

}