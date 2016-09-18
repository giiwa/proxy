/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.proxy.web;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.conf.Global;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.web.IListener;
import org.giiwa.framework.web.Module;
import org.giiwa.proxy.web.admin.proxy;

public class ProxyListener implements IListener {

  static Log log = LogFactory.getLog(ProxyListener.class);

  @Override
  public void onStart(Configuration conf, Module m) {
    // TODO Auto-generated method stub
    log.info("proxy is starting ...");

    new Task() {
      public void onExecute() {

        int port = Global.getInt("proxy.port", 0);
        if (port > 0) {
            HttpServer proxy = new HttpServer(port);
            proxy.accept();
        } else {
          OpLog.info(proxy.class, "startup", "disabled", null, null);
        }
      }
    }.schedule(10);
  }

  @Override
  public void onStop() {
    // TODO Auto-generated method stub

  }

  @Override
  public void uninstall(Configuration conf, Module m) {
    // TODO Auto-generated method stub

  }

  @Override
  public void upgrade(Configuration conf, Module m) {
    // TODO Auto-generated method stub

  }

}
