package vertx.tests.core.eventbus;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class ClusteredPeer extends LocalPeer {

  protected boolean isLocal() {
    return false;
  }
}
