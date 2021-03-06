package org.vertx.java.core.sockjs;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.logging.Logger;

import java.util.Map;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
class HtmlFileTransport extends BaseTransport {

  private static final Logger log = Logger.getLogger(HtmlFileTransport.class);

  private static final String HTML_FILE_TEMPLATE;

  static {
    String str =
    "<!doctype html>\n" +
    "<html><head>\n" +
    "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\" />\n" +
    "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
    "</head><payload><h2>Don't panic!</h2>\n" +
    "  <script>\n" +
    "    document.domain = document.domain;\n" +
    "    var c = parent.{{ callback }};\n" +
    "    c.start();\n" +
    "    function p(d) {c.message(d);};\n" +
    "    window.onload = function() {c.stop();};\n" +
    "  </script>";

    String str2 = str.replace("{{ callback }}", "");
    StringBuilder sb = new StringBuilder(str);
    int extra = 1024 - str2.length();
    for (int i = 0; i < extra; i++) {
      sb.append(' ');
    }
    sb.append("\r\n");
    HTML_FILE_TEMPLATE = sb.toString();
  }

  HtmlFileTransport(RouteMatcher rm, String basePath, Map<String, Session> sessions, final AppConfig config,
            final Handler<SockJSSocket> sockHandler) {
    super(sessions, config);
    String htmlFileRE = basePath + COMMON_PATH_ELEMENT_RE + "htmlfile";

    rm.getWithRegEx(htmlFileRE, new Handler<HttpServerRequest>() {
      public void handle(final HttpServerRequest req) {

        String callback = req.getAllParams().get("callback");
        if (callback == null) {
          callback = req.getAllParams().get("c");
          if (callback == null) {
            req.response.statusCode = 500;
            req.response.end("\"callback\" parameter required\n");
            return;
          }
        }

        String sessionID = req.getAllParams().get("param0");
        Session session = getSession(config.getSessionTimeout(), config.getHeartbeatPeriod(), sessionID, sockHandler);
        session.register(new HtmlFileListener(req, callback));
      }
    });
  }

  private class HtmlFileListener implements TransportListener {

    final HttpServerRequest req;
    final String callback;
    boolean headersWritten;

    HtmlFileListener(HttpServerRequest req, String callback) {
      this.req = req;
      this.callback = callback;
    }

    public void sendFrame(String payload) {
      if (!headersWritten) {
        String htmlFile = HTML_FILE_TEMPLATE.replace("{{ callback }}", callback);
        req.response.putHeader("Content-Type", "text/html; charset=UTF-8");
        req.response.putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        req.response.setChunked(true);
        setJSESSIONID(config, req);
        req.response.write(htmlFile);
        headersWritten = true;
      }
      payload = escapeForJavaScript(payload);
      StringBuilder sb = new StringBuilder();
      sb.append("<script>\np(\"");
      sb.append(payload);
      sb.append("\");\n</script>\r\n");
      req.response.write(sb.toString());
    }
  }
}
