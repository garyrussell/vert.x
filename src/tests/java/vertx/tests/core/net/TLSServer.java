package vertx.tests.core.net;

import org.vertx.java.core.Handler;
import org.vertx.java.core.SimpleHandler;
import org.vertx.java.core.app.Verticle;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.shareddata.SharedData;
import org.vertx.java.newtests.TestUtils;
import vertx.tests.core.http.TLSTestParams;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class TLSServer implements Verticle {

  protected TestUtils tu = new TestUtils();

  private NetServer server;

  public void start() {
    server = new NetServer();

    TLSTestParams params = TLSTestParams.deserialize(SharedData.instance.<String, byte[]>getMap("TLSTest").get("params"));

    server.setSSL(true);

    if (params.serverTrust) {
      server.setTrustStorePath("./src/tests/keystores/server-truststore.jks").setTrustStorePassword
          ("wibble");
    }
    if (params.serverCert) {
      server.setKeyStorePath("./src/tests/keystores/server-keystore.jks").setKeyStorePassword("wibble");
    }
    if (params.requireClientAuth) {
      server.setClientAuthRequired(true);
    }

    server.connectHandler(getConnectHandler());
    server.listen(4043);
    tu.appReady();
  }

  public void stop() {
    server.close(new SimpleHandler() {
      public void handle() {
        tu.checkContext();
        tu.appStopped();
      }
    });
  }

  protected Handler<NetSocket> getConnectHandler() {
    return new Handler<NetSocket>() {
      public void handle(final NetSocket socket) {

        tu.checkContext();
        socket.dataHandler(new Handler<Buffer>() {
          public void handle(Buffer buffer) {
            tu.checkContext();
            socket.write(buffer);
          }
        });
      }
    };
  }
}
