package com.trulia.thoth.monitor;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * User: dbraga - Date: 10/18/14
 */
public class PredictorModelHealthMonitor {
  /**
   * Thoth predictor urls
   */
  private String thothPredictorURL;
  /**
   * Health score threshold, current health score needs to be under this threshold in order for the model to be healthy
   */
  private float healthScoreThreshold;

  // Actions
  private static final String ACTION_GET_HEALTH_MODEL_SCORE = "/?action=getModelHealthScore";
  private static final String ACTION_INVALIDATE_CURRENT_MODEL = "/?action=invalidateModel";
  private static final String ACTION_TRIGGER_TRAINING_NEW_MODEL = "/?action=trainModel";

  private String response;
  private GetMethod getMethod;
  private HttpClient httpClient;

  public PredictorModelHealthMonitor(String thothPredictorURL, float healthScoreThreshold) throws MalformedURLException {
    this.healthScoreThreshold = healthScoreThreshold;
    this.thothPredictorURL = thothPredictorURL;
    this.httpClient = new HttpClient();
  }

  public void execute() {
    try {
      getMethod = new GetMethod(thothPredictorURL + ACTION_GET_HEALTH_MODEL_SCORE);
      httpClient.executeMethod(getMethod);
      response =  getMethod.getResponseBodyAsString();
      getMethod.releaseConnection();
      float currentHealthScore = Float.parseFloat(response);
      if (currentHealthScore < healthScoreThreshold) {
        System.out.println("Predictor model is healthy");
      } else{
        System.out.println("Predictor model is *NOT* healthy");
        getMethod = new GetMethod(thothPredictorURL + ACTION_INVALIDATE_CURRENT_MODEL);
        httpClient.executeMethod(getMethod);
        getMethod.getResponseBodyAsString();
        getMethod.releaseConnection();
        getMethod = new GetMethod(thothPredictorURL + ACTION_TRIGGER_TRAINING_NEW_MODEL);
        httpClient.executeMethod(getMethod);
        getMethod.releaseConnection();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }


}
