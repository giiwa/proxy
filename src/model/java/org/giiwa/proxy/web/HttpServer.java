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

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.proxy.web.admin.proxy;

public class HttpServer {

  private static Log     log = LogFactory.getLog(HttpServer.class);

  /**
   * Proxy server instance
   */
  protected ServerSocket server;

  /**
   * Create new ProxyServer instance and listen to a port
   * 
   * @param port
   *          ProxyServer listening port
   */
  public HttpServer(int port) {

    try {
      server = new ServerSocket(port);

      OpLog.info(proxy.class, "startup", "started http on [" + port + "]", null, null);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      OpLog.error(proxy.class, "startup", e.getMessage(), null, null);
    }
  }

  /**
   * Create new socket and request handler object on each request
   * 
   */
  public void start() {

    while (true) {
      try {
        new HttpHandler(server.accept()).schedule(0);
      } catch (IOException e) {
        log.error(e.getMessage(), e);
        OpLog.error(proxy.class, "running", e.getMessage(), null, null);
      }
    }
  }

  /**
   * Main method to fire up proxy server and launch request handlers
   * 
   */
  public static void main(String[] args) {

    System.out.println("ProxyServer is listening to port " + 8080);
    HttpServer proxy = new HttpServer(8080);
    proxy.start();

  }

}
