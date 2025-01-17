/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.submarine.server.workbench.database.entity;

import java.util.ArrayList;
import java.util.List;

// Corresponding to the workbench-web front-end tree select control
public class SysDeptSelectEntity {
  // TreeSelect key
  private String key;

  // TreeSelect value
  private String value;

  // TreeSelect title
  private String title;

  // TreeSelect disabled
  private Boolean disabled = false;

  List<SysDeptSelectEntity> children = new ArrayList<>();

  public SysDeptSelectEntity convert(SysDeptTree treeModel) {
    this.key = treeModel.getDeptCode();
    this.value = treeModel.getDeptCode();
    this.title = "(" + treeModel.getDeptCode() + ") " + treeModel.getDeptName();
    return this;
  }

  public List<SysDeptSelectEntity> getChildren() {
    return children;
  }

  public void setChildren(List<SysDeptSelectEntity> children) {
    this.children = children;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Boolean getDisabled() {
    return disabled;
  }

  public void setDisabled(Boolean disabled) {
    this.disabled = disabled;
  }
}
