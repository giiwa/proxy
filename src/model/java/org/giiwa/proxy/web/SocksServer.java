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

import java.nio.channels.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.proxy.web.admin.proxy;

public class SocksServer {

  private static Log             log     = LogFactory.getLog(SocksServer.class);

  static ArrayList<SocksHandler> clients = new ArrayList<SocksHandler>();

  // utility function
  public SocksHandler addClient(SocketChannel s) {
    SocksHandler cl;
    try {
      cl = new SocksHandler(s);
      OpLog.warn(proxy.class, "request", "client=" + s.getRemoteAddress(), null, null);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      OpLog.warn(proxy.class, "request", "error=" + e.getMessage(), null, null);
      return null;
    }
    clients.add(cl);
    return cl;
  }

  public SocksServer(int port) {
    try {
      ServerSocketChannel socks = ServerSocketChannel.open();
      socks.socket().bind(new InetSocketAddress(port));
      socks.configureBlocking(false);
      OpLog.info(proxy.class, "startup", "started socks on [" + port + "]", null, null);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      OpLog.warn(proxy.class, "startup", "failed to socks on [" + port + "], error=" + e.getMessage(), null, null);
    }
  }

  ServerSocketChannel socks = null;

  public void start() {
    if (socks == null) {
      return;
    }

    try {
      Selector select = Selector.open();
      socks.register(select, SelectionKey.OP_ACCEPT);

      int lastClients = clients.size();
      // select loop
      while (true) {
        select.select(1000);

        Set keys = select.selectedKeys();
        Iterator iterator = keys.iterator();
        while (iterator.hasNext()) {
          SelectionKey k = (SelectionKey) iterator.next();

          if (!k.isValid())
            continue;

          // new connection?
          if (k.isAcceptable() && k.channel() == socks) {
            // server socket
            SocketChannel csock = socks.accept();
            if (csock == null)
              continue;
            addClient(csock);
            csock.register(select, SelectionKey.OP_READ);
          } else if (k.isReadable()) {
            // new data on a client/remote socket
            for (int i = 0; i < clients.size(); i++) {
              SocksHandler cl = clients.get(i);
              try {
                if (k.channel() == cl.client) // from client (e.g. socks client)
                  cl.newClientData(select, k);
                else if (k.channel() == cl.remote) { // from server client is
                                                     // connected to (e.g.
                                                     // website)
                  cl.newRemoteData(select, k);
                }
              } catch (IOException e) { // error occurred - remove client
                cl.client.close();
                if (cl.remote != null)
                  cl.remote.close();
                k.cancel();
                clients.remove(cl);
              }

            }
          }
        }

        // client timeout check
        for (int i = 0; i < clients.size(); i++) {
          SocksHandler cl = clients.get(i);
          if ((System.currentTimeMillis() - cl.lastData) > 30000L) {
            cl.client.close();
            if (cl.remote != null)
              cl.remote.close();
            clients.remove(cl);
          }
        }
        if (clients.size() != lastClients) {
          // System.out.println(clients.size());
          lastClients = clients.size();
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      OpLog.warn(proxy.class, "request", "error=" + e.getMessage(), null, null);
    }
  }

  public static void main(String[] args) throws IOException {
    new SocksServer(8000);
  }
}
