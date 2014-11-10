package com.trulia.thoth.monitor;

import com.trulia.thoth.pojo.MonitorList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User: dbraga - Date: 11/9/14
 */
@Component
public class AvailableMonitors {
  public List<Class> monitorList;
  @Value("${thoth.monitor.configuration.file}")
  private String configurationFile;

  @PostConstruct
  public void init() throws ClassNotFoundException, JAXBException {
    monitorList = new ArrayList<Class>();
    // Reading monitor classes from configuration file
    JAXBContext jc = JAXBContext.newInstance(MonitorList.class);
    Unmarshaller unmarshaller = jc.createUnmarshaller();
    MonitorList obj = (MonitorList)unmarshaller.unmarshal(new File(configurationFile));
    for (com.trulia.thoth.pojo.Monitor monitor: obj.getMonitors()){
      System.out.println("Adding Monitor("+monitor.getName()+") , className("+monitor.getClassName()+") to available monitor list");
      //classNames.add(monitor.getClassName());
      monitorList.add(Class.forName(monitor.getClassName()));
    }
 }

  public List<Class> getMonitorList(){
    return monitorList;
  }

}
