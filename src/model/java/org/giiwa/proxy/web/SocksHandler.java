package org.giiwa.proxy.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SocksHandler {

  private static Log log      = LogFactory.getLog(SocksHandler.class);

  SocketChannel      client, remote;
  boolean            connected;
  long               lastData = 0;

  SocksHandler(SocketChannel c) throws IOException {
    client = c;
    client.configureBlocking(false);
    lastData = System.currentTimeMillis();
  }

  public void newRemoteData(Selector selector, SelectionKey sk) throws IOException {
    ByteBuffer buf = ByteBuffer.allocate(1024);
    if (remote.read(buf) == -1)
      throw new IOException("disconnected");
    lastData = System.currentTimeMillis();
    buf.flip();
    client.write(buf);
  }

  public void newClientData(Selector selector, SelectionKey sk) throws IOException {
    if (!connected) {
      ByteBuffer inbuf = ByteBuffer.allocate(512);
      if (client.read(inbuf) < 1)
        return;
      inbuf.flip();

      // read socks header
      int ver = inbuf.get();
      if (ver != 4) {
        throw new IOException("incorrect version" + ver);
      }
      int cmd = inbuf.get();

      // check supported command
      if (cmd != 1) {
        throw new IOException("incorrect version");
      }

      final int port = inbuf.getShort();

      final byte ip[] = new byte[4];
      // fetch IP
      inbuf.get(ip);

      InetAddress remoteAddr = InetAddress.getByAddress(ip);

      while ((inbuf.get()) != 0)
        ; // username

      // hostname provided, not IP
      if (ip[0] == 0 && ip[1] == 0 && ip[2] == 0 && ip[3] != 0) { // host
                                                                  // provided
        String host = "";
        byte b;
        while ((b = inbuf.get()) != 0) {
          host += b;
        }
        remoteAddr = InetAddress.getByName(host);
        System.out.println(host + remoteAddr);
      }

      remote = SocketChannel.open(new InetSocketAddress(remoteAddr, port));

      ByteBuffer out = ByteBuffer.allocate(20);
      out.put((byte) 0);
      out.put((byte) (remote.isConnected() ? 0x5a : 0x5b));
      out.putShort((short) port);
      out.put(remoteAddr.getAddress());
      out.flip();
      client.write(out);

      if (!remote.isConnected())
        throw new IOException("connect failed");

      remote.configureBlocking(false);
      remote.register(selector, SelectionKey.OP_READ);

      connected = true;
    } else {
      ByteBuffer buf = ByteBuffer.allocate(1024);
      if (client.read(buf) == -1)
        throw new IOException("disconnected");
      lastData = System.currentTimeMillis();
      buf.flip();
      remote.write(buf);
    }
  }

}
