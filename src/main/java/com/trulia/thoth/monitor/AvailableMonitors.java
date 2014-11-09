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

  @PostConstruct
  public void init() throws ClassNotFoundException {
    monitorList = new ArrayList<Class>();
    //TODO: temporary
    String[] classNames = new String[]{"com.trulia.thoth.monitor.QTimeMonitor","com.trulia.thoth.monitor.ZeroHitsMonitor"};
    for (String monitorClassNames: classNames){
      monitorList.add(Class.forName(monitorClassNames));
    }
 }

  public List<Class> getMonitorList(){
    return monitorList;
  }

}
