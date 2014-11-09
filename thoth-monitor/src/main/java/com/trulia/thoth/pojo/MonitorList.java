package com.trulia.thoth.pojo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * User: dbraga - Date: 11/9/14
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Monitors")
public class MonitorList {
  @XmlElement(name="Monitor")
  private List<Monitor> monitors = new ArrayList<Monitor>();

  public List<Monitor> getMonitors() {
    return monitors;
  }
  public void setMonitors(List<Monitor> monitors) {
    this.monitors = monitors;
  }
}