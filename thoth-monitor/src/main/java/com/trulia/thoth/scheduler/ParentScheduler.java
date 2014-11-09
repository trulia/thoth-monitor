package com.trulia.thoth.scheduler;

import com.trulia.thoth.quartz.PlaceholderMonitorJob;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

/**
 * User: dbraga - Date: 10/22/14
 */
public class ParentScheduler {

  public void init(){
    System.out.println("Parent scheduler");

    // Schedule 1 job for Monitor .

  }

  public void scheduleMonitor() throws SchedulerException {

    JobDetail workerJob = JobBuilder.newJob(PlaceholderMonitorJob.class)
        .withIdentity("pendingWorkJob2", "group2").build();
    Trigger workerTrigger = TriggerBuilder
        .newTrigger()
        .withIdentity("pendingWorkTrigger2", "group2")
        .withSchedule(
            CronScheduleBuilder.cronSchedule("0 * * * * ?")) // execute this every day at midnight
        .build();

    //Schedule it
    org.quartz.Scheduler scheduler = new StdSchedulerFactory().getScheduler();
    scheduler.start();
    scheduler.scheduleJob(workerJob, workerTrigger);
  }
}
