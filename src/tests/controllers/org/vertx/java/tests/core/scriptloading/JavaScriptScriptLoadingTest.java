package org.vertx.java.tests.core.scriptloading;

import org.junit.Test;
import org.vertx.java.newtests.TestBase;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class JavaScriptScriptLoadingTest extends TestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  @Test
  public void testScriptLoading() throws Exception {
    startApp("core/scriptloading/test_client.js");
    startTest(getMethodName());
  }


}
