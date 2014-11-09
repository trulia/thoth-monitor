package com.trulia.thoth.monitor;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * User: dbraga - Date: 11/9/14
 */
@Component
public class AvailableMonitors {

  public List<Class> monitorList;
  //public HashMap<String,MonitorConfiguration> monitorConfiguration;


  @PostConstruct
  public void init() throws ClassNotFoundException {
    monitorList = new ArrayList<Class>();
    //monitorConfiguration = new HashMap<String, MonitorConfiguration>();


    //TODO: temporary
    String[] classNames = new String[]{"com.trulia.thoth.monitor.QTimeMonit"};
        //,"com.trulia.thoth.monitor.ZeroHitsMonitor"};

    for (String monitorClassNames: classNames){
      monitorList.add(Class.forName(monitorClassNames));
      //TODO : externalize
      //Class[] constructors = new Class[]{Class.forName("com.trulia.thoth.pojo.ServerDetail"), Class.forName("org.apache.solr.client.solrj.SolrServer"), Class.forName("org.apache.solr.client.solrj.SolrServer")};
      //monitorConfiguration.put(monitorClassNames,new MonitorConfiguration(constructors));
    }




    //monitorList.add(ZeroHitsMonitor.class);
  }

  public List<Class> getMonitorList(){
    return monitorList;
  }

  //public HashMap<String,MonitorConfiguration> getMonitorConfiguration(){
  //  return monitorConfiguration;
  //}

  public class MonitorConfiguration{
    public Class[] constructors;

    public Class[] getConstructors() {
      return constructors;
    }

    public MonitorConfiguration(Class[] constructors){
      this.constructors = constructors;
    }
  }

}
