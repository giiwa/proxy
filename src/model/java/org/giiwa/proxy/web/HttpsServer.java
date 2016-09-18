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
import org.apache.commons.ssl.KeyMaterial;
import org.apache.commons.ssl.SSLServer;
import org.apache.commons.ssl.TrustMaterial;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.proxy.web.admin.proxy;

public class HttpsServer {

  private static Log     log = LogFactory.getLog(HttpsServer.class);

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
  public HttpsServer(int port) {

    try {

      SSLServer ssl = new SSLServer();

      // Server needs some key material. We'll use an OpenSSL/PKCS8 style key
      // (possibly encrypted).
      String certificateChain = Global.getString("proxy.server.crt", ""); // server.crt
      String privateKey = Global.getString("proxy.server.key", "");// server.key;
      String password = Global.getString("proxy.server.password", "");
      KeyMaterial km = new KeyMaterial(certificateChain.getBytes(), privateKey.getBytes(), password.toCharArray());

      ssl.setKeyMaterial(km);

      // These settings have to do with how we'll treat client certificates that
      // are presented
      // to us. If the client doesn't present any client certificate, then these
      // are ignored.
      ssl.setCheckHostname(false); // default setting is "false" for
                                   // SSLServer
      ssl.setCheckExpiry(true); // default setting is "true" for SSLServer
      ssl.setCheckCRL(true); // default setting is "true" for SSLServer

      // This server trusts all client certificates presented (usually people
      // won't present
      // client certs, but if they do, we'll give them a socket at the very
      // least).
      ssl.addTrustMaterial(TrustMaterial.TRUST_ALL);
      server = ssl.createServerSocket(port);

      OpLog.info(proxy.class, "startup", "started https on [" + port + "]", null, null);

    } catch (Throwable e) {
      log.error(e.getMessage(), e);
      OpLog.error(proxy.class, "startup", e.getMessage(), null, null);
    }
  }

  /**
   * Create new socket and request handler object on each request
   * 
   */
  public void accept() {

    while (true) {
      try {
        new RequestHandler(server.accept()).schedule(0);
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
    HttpsServer proxy = new HttpsServer(8080);
    proxy.accept();

  }

}
