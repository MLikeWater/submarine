/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.submarine.server.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import org.apache.submarine.commons.cluster.ClusterServer;
import org.apache.submarine.commons.cluster.meta.ClusterMeta;
import org.apache.submarine.commons.cluster.meta.ClusterMetaType;
import org.apache.submarine.commons.utils.SubmarineConfiguration;
import org.apache.submarine.server.utils.response.JsonResponse;
import org.apache.submarine.server.rest.workbench.annotation.SubmarineApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * clusters Rest api.
 */
@Path(RestConstants.V1 + "/" + RestConstants.CLUSTER)
@Produces("application/json")
public class ClusterRestApi {
  private static final Logger LOG = LoggerFactory.getLogger(ClusterRestApi.class);
  Gson gson = new Gson();

  private ClusterServer clusterServer = ClusterServer.getInstance();
  @VisibleForTesting
  public void setClusterServer(ClusterServer clusterServer) {
    this.clusterServer = clusterServer;
  }

  @GET
  @Path("/" + RestConstants.ADDRESS)
  @SubmarineApi
  public Response getClusterAddress() {
    SubmarineConfiguration sConf = SubmarineConfiguration.getInstance();
    String clusterAddr = sConf.getClusterAddress();
    String[] arrAddr = clusterAddr.split(",");
    List<String> listAddr = Arrays.asList(arrAddr);

    return new JsonResponse.Builder<List<String>>(Response.Status.OK)
        .success(true).result(listAddr).build();
  }

  /**
   * get all nodes of clusters
   */
  @GET
  @Path("/" + RestConstants.NODES)
  @SubmarineApi
  public Response getClusterNodes(){
    ArrayList<HashMap<String, Object>> nodes = new ArrayList<>();

    Map<String, HashMap<String, Object>> clusterMeta = null;
    Map<String, HashMap<String, Object>> intpMeta = null;
    clusterMeta = clusterServer.getClusterMeta(ClusterMetaType.SERVER_META, "");
    intpMeta = clusterServer.getClusterMeta(ClusterMetaType.INTP_PROCESS_META, "");

    // Number of interpreter processes
    for (Map.Entry<String, HashMap<String, Object>> serverMetaEntity : clusterMeta.entrySet()) {
      if (!serverMetaEntity.getValue().containsKey(ClusterMeta.NODE_NAME)) {
        continue;
      }
      String serverNodeName = (String) serverMetaEntity.getValue().get(ClusterMeta.NODE_NAME);

      ArrayList<String> arrIntpProcess = new ArrayList<>();
      int intpProcCount = 0;
      for (Map.Entry<String, HashMap<String, Object>> intpMetaEntity : intpMeta.entrySet()) {
        if (!intpMetaEntity.getValue().containsKey(ClusterMeta.NODE_NAME)
            && !intpMetaEntity.getValue().containsKey(ClusterMeta.INTP_PROCESS_NAME)) {
          continue;
        }
        String intpNodeName = (String) intpMetaEntity.getValue().get(ClusterMeta.NODE_NAME);

        if (serverNodeName.equals(intpNodeName)) {
          intpProcCount++;
          String intpName = (String) intpMetaEntity.getValue().get(ClusterMeta.INTP_PROCESS_NAME);
          arrIntpProcess.add(intpName);
        }
      }
      serverMetaEntity.getValue().put(ClusterMeta.INTP_PROCESS_COUNT, intpProcCount);
      serverMetaEntity.getValue().put(ClusterMeta.INTP_PROCESS_LIST, arrIntpProcess);
    }

    for (Map.Entry<String, HashMap<String, Object>> entry : clusterMeta.entrySet()) {
      String nodeName = entry.getKey();
      Map<String, Object> properties = entry.getValue();

      Map<String, Object> sortProperties = new HashMap<>();

      if (properties.containsKey(ClusterMeta.CPU_USED)
          && properties.containsKey(ClusterMeta.CPU_CAPACITY)) {
        float cpuUsed = (long) properties.get(ClusterMeta.CPU_USED) / (float) 100.0;
        float cpuCapacity = (long) properties.get(ClusterMeta.CPU_CAPACITY) / (float) 100.0;
        float cpuRate = cpuUsed / cpuCapacity * 100;

        String cpuInfo = String.format("%.2f / %.2f = %.2f", cpuUsed, cpuCapacity, cpuRate);
        sortProperties.put(ClusterMeta.CPU_USED + " / " + ClusterMeta.CPU_CAPACITY, cpuInfo + "%");
      }

      if (properties.containsKey(ClusterMeta.MEMORY_USED)
          && properties.containsKey(ClusterMeta.MEMORY_CAPACITY)) {
        float memoryUsed = (long) properties.get(ClusterMeta.MEMORY_USED) / (float) (1024 * 1024 * 1024);
        float memoryCapacity
            = (long) properties.get(ClusterMeta.MEMORY_CAPACITY) / (float) (1024 * 1024 * 1024);
        float memoryRate = memoryUsed / memoryCapacity * 100;

        String memoryInfo = String.format("%.2fGB / %.2fGB = %.2f",
            memoryUsed, memoryCapacity, memoryRate);
        sortProperties.put(ClusterMeta.MEMORY_USED + " / " + ClusterMeta.MEMORY_CAPACITY, memoryInfo + "%");
      }

      if (properties.containsKey(ClusterMeta.SERVER_START_TIME)) {
        // format LocalDateTime
        Object serverStartTime = properties.get(ClusterMeta.SERVER_START_TIME);
        if (serverStartTime instanceof LocalDateTime) {
          LocalDateTime localDateTime = (LocalDateTime) serverStartTime;
          String dateTime = formatLocalDateTime(localDateTime);
          sortProperties.put(ClusterMeta.SERVER_START_TIME, dateTime);
        } else {
          sortProperties.put(ClusterMeta.SERVER_START_TIME, "Wrong time type!");
        }
      }
      if (properties.containsKey(ClusterMeta.STATUS)) {
        sortProperties.put(ClusterMeta.STATUS, properties.get(ClusterMeta.STATUS));
      }
      if (properties.containsKey(ClusterMeta.LATEST_HEARTBEAT)) {
        // format LocalDateTime
        Object latestHeartbeat = properties.get(ClusterMeta.LATEST_HEARTBEAT);
        if (latestHeartbeat instanceof LocalDateTime) {
          LocalDateTime localDateTime = (LocalDateTime) latestHeartbeat;
          String dateTime = formatLocalDateTime(localDateTime);
          sortProperties.put(ClusterMeta.LATEST_HEARTBEAT, dateTime);
        } else {
          sortProperties.put(ClusterMeta.LATEST_HEARTBEAT, "Wrong time type!");
        }
      }
      if (properties.containsKey(ClusterMeta.INTP_PROCESS_LIST)) {
        sortProperties.put(ClusterMeta.INTP_PROCESS_LIST, properties.get(ClusterMeta.INTP_PROCESS_LIST));
      }

      HashMap<String, Object> node = new HashMap<>();
      node.put(ClusterMeta.NODE_NAME, nodeName);
      node.put(ClusterMeta.PROPERTIES, sortProperties);

      nodes.add(node);
    }

    return new JsonResponse.Builder<ArrayList<HashMap<String, Object>>>(Response.Status.OK)
        .success(true).result(nodes).build();
  }

  /**
   * get node info by id
   */
  @GET
  @Path("/" + RestConstants.NODE + "/{nodeName}/{intpName}")
  @SubmarineApi
  public Response getClusterNode(@PathParam("nodeName") String nodeName,
                                 @PathParam("intpName") String intpName){
    ArrayList<HashMap<String, Object>> intpProcesses = new ArrayList<>();

    Map<String, HashMap<String, Object>> intpMeta = null;
    intpMeta = clusterServer.getClusterMeta(ClusterMetaType.INTP_PROCESS_META, "");

    // Number of calculation processes
    for (Map.Entry<String, HashMap<String, Object>> intpMetaEntity : intpMeta.entrySet()) {
      String intpNodeName = (String) intpMetaEntity.getValue().get(ClusterMeta.NODE_NAME);

      if (null != intpNodeName && intpNodeName.equals(nodeName)) {
        HashMap<String, Object> node = new HashMap<String, Object>();
        node.put(ClusterMeta.NODE_NAME, intpNodeName);
        node.put(ClusterMeta.PROPERTIES, intpMetaEntity.getValue());

        // format LocalDateTime
        HashMap<String, Object> properties = intpMetaEntity.getValue();
        if (properties.containsKey(ClusterMeta.INTP_START_TIME)) {
          Object intpStartTime = properties.get(ClusterMeta.INTP_START_TIME);
          if (intpStartTime instanceof LocalDateTime) {
            LocalDateTime localDateTime = (LocalDateTime) intpStartTime;
            String dateTime = formatLocalDateTime(localDateTime);
            properties.put(ClusterMeta.INTP_START_TIME, dateTime);
          } else {
            properties.put(ClusterMeta.INTP_START_TIME, "Wrong time type!");
          }
        }
        if (properties.containsKey(ClusterMeta.LATEST_HEARTBEAT)) {
          Object latestHeartbeat = properties.get(ClusterMeta.LATEST_HEARTBEAT);
          if (latestHeartbeat instanceof LocalDateTime) {
            LocalDateTime localDateTime = (LocalDateTime) latestHeartbeat;
            String dateTime = formatLocalDateTime(localDateTime);
            properties.put(ClusterMeta.LATEST_HEARTBEAT, dateTime);
          } else {
            properties.put(ClusterMeta.LATEST_HEARTBEAT, "Wrong time type!");
          }
        }

        intpProcesses.add(node);
      }
    }

    return new JsonResponse.Builder<ArrayList<HashMap<String, Object>>>(Response.Status.OK)
        .success(true).result(intpProcesses).build();
  }

  private String formatLocalDateTime(LocalDateTime localDateTime) {
    DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
    String strDate = localDateTime.format(dtf);
    return strDate;
  }
}
