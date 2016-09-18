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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Socket;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.X;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.proxy.web.admin.proxy;

public class RequestHandler extends Task {

  private static Log                log = LogFactory.getLog(RequestHandler.class);

  protected DataInputStream         clientInputStream;

  protected OutputStream            clientOutputStream;

  protected OutputStream            remoteOutputStream;

  protected InputStream             remoteInputStream;

  protected Socket                  clientSocket;

  protected Socket                  remoteSocket;

  protected String                  requestType;

  protected String                  url;

  protected String                  uri;

  protected String                  httpVersion;

  protected HashMap<String, String> header;

  static String                     EOL = "\r\n";

  private TimeStamp                 t   = TimeStamp.create();

  public RequestHandler(Socket clientSocket) {

    header = new HashMap<String, String>();
    this.clientSocket = clientSocket;
  }

  public void onExecute() {

    try {

      clientInputStream = new DataInputStream(clientSocket.getInputStream());
      clientOutputStream = clientSocket.getOutputStream();

      clientToProxy();

      proxyToRemote();

      remoteToClient();

      close(remoteOutputStream, remoteInputStream, clientInputStream, clientOutputStream, clientSocket, remoteSocket);

    } catch (IOException e) {
      log.error(url, e);
    }
  }

  private void clientToProxy() {

    String line, key, value;
    StringTokenizer tokens;

    try {

      if ((line = clientInputStream.readLine()) != null) {

        tokens = new StringTokenizer(line, " ");
        requestType = tokens.nextToken();
        url = tokens.nextToken();
        httpVersion = tokens.nextToken();
      }

      while ((line = clientInputStream.readLine()) != null) {
        if (line.trim().length() == 0)
          break;

        tokens = new StringTokenizer(line);
        key = tokens.nextToken(":");
        value = line.replaceAll(key, "").replace(": ", "");
        header.put(key.toLowerCase(), value);
      }

    } catch (Exception e) {
      log.error("past " + t.past() + "ms, url=" + url, e);
      return;
    }
  }

  private void proxyToRemote() {

    try {
      if (!X.isSame("GET", requestType) && !X.isSame("POST", requestType))
        return;

      int i = url.indexOf("//");
      int j = url.indexOf("/", i + 2);
      String host = null;
      if (j > 0) {
        host = url.substring(i + 2, j);
        uri = url.substring(j);
      } else {
        host = url.substring(i + 2);
        uri = "/";
      }
      i = host.indexOf(":");
      int port = 80;
      if (i > 0) {
        host = host.substring(0, i);
        port = X.toInt(host.substring(i + 1), 80);
      }
      remoteSocket = new Socket(host, port);
      remoteOutputStream = remoteSocket.getOutputStream();

      checkRemoteStreams();
      checkClientStreams();

      String request = requestType + " " + uri + " " + httpVersion;
      remoteOutputStream.write(request.getBytes());
      remoteOutputStream.write(EOL.getBytes());
      // OpLog.info(proxy.class, "request", request, null, null);

      String command = "host: " + host;
      remoteOutputStream.write(command.getBytes());
      remoteOutputStream.write(EOL.getBytes());
      StringBuilder sb = new StringBuilder(url).append("<hr>");
      sb.append(command).append("<br/>");

      for (String key : header.keySet()) {
        if (!key.equals("host")) {
          command = key + ": " + header.get(key);
          remoteOutputStream.write(command.getBytes());
          remoteOutputStream.write(EOL.getBytes());
          sb.append(command).append("<br/>");

        }
      }
      OpLog.info(proxy.class, "request", sb.toString(), null, null);

      remoteOutputStream.write(EOL.getBytes());
      remoteOutputStream.flush();

      int contentLength = X.toInt(header.get("content-length"));
      for (int ii = 0; ii < contentLength; ii++) {
        remoteOutputStream.write(clientInputStream.read());
      }

      remoteOutputStream.write(EOL.getBytes());
      remoteOutputStream.flush();
    } catch (Exception e) {
      log.error("past " + t.past() + "ms, url=" + url, e);
      OpLog.warn(proxy.class, "request", "error=" + e.getMessage() + "<br/>url=" + url, null, null);
      return;
    }
  }

  private void remoteToClient() {

    try {

      if (remoteSocket == null)
        return;

      String line;
      DataInputStream in = new DataInputStream(remoteSocket.getInputStream());

      StringBuilder sb = new StringBuilder(url).append("<hr/>");
      while ((line = in.readLine()) != null) {

        if (line.trim().length() == 0)
          break;

        if (line.toLowerCase().startsWith("proxy"))
          continue;
        if (line.contains("keep-alive"))
          continue;

        sb.append(line).append("<br/>");

        clientOutputStream.write(line.getBytes());
        clientOutputStream.write(EOL.getBytes());
      }

      clientOutputStream.write(EOL.getBytes());
      clientOutputStream.flush();

      byte[] buffer = new byte[1024];

      for (int i; (i = in.read(buffer)) != -1;) {
        clientOutputStream.write(buffer, 0, i);
        clientOutputStream.flush();
      }

      OpLog.info(proxy.class, "response", sb.toString(), null, null);

    } catch (Exception e) {
      log.error("past " + t.past() + "ms, url=" + url, e);
      OpLog.warn(proxy.class, "response", "error=" + e.getMessage() + "<br/>url=" + url, null, null);
      return;
    }
  }

  private void close(Object... m) {
    for (Object o : m) {
      try {
        if (o instanceof InputStream) {
          ((InputStream) o).close();
        } else if (o instanceof Reader) {
          ((Reader) o).close();
        } else if (o instanceof OutputStream) {
          ((OutputStream) o).close();
        } else if (o instanceof Socket) {
          ((Socket) o).close();
        }
      } catch (IOException e) {
        log.error("past " + t.past() + "ms, url=" + url, e);
      }
    }
  }

  /**
   * Helper function to strip out unwanted request header from client
   * 
   */
  private void stripUnwantedHeaders() {

    if (header.containsKey("user-agent"))
      header.remove("user-agent");
    if (header.containsKey("referer"))
      header.remove("referer");
    if (header.containsKey("proxy-connection"))
      header.remove("proxy-connection");
    if (header.containsKey("connection") && header.get("connection").equalsIgnoreCase("keep-alive")) {
      header.remove("connection");
    }
  }

  /**
   * Helper function to check for client input and output stream, reconnect if
   * closed
   * 
   */
  private void checkClientStreams() {

    try {
      if (clientSocket.isOutputShutdown())
        clientOutputStream = clientSocket.getOutputStream();
      if (clientSocket.isInputShutdown())
        clientInputStream = new DataInputStream(clientSocket.getInputStream());
    } catch (Exception e) {
      log.error("past " + t.past() + "ms, url=" + url, e);
      return;
    }
  }

  /**
   * Helper function to check for remote input and output stream, reconnect if
   * closed
   * 
   */
  private void checkRemoteStreams() {

    try {
      if (remoteSocket.isOutputShutdown())
        remoteOutputStream = remoteSocket.getOutputStream();
      if (remoteSocket.isInputShutdown())
        remoteInputStream = new DataInputStream(remoteSocket.getInputStream());
    } catch (Exception e) {
      log.error("past " + t.past() + "ms, url=" + url, e);
      return;
    }
  }

}
