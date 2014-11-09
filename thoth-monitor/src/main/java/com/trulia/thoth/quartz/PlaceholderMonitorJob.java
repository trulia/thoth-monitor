package com.trulia.thoth.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * User: dbraga - Date: 10/22/14
 */
public class PlaceholderMonitorJob  implements Job{

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    System.out.println("I'm placeholder monitor job");
  }

}
