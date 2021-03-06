package org.vertx.java.core.app.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import org.mozilla.javascript.JavaScriptException;
import org.vertx.java.core.app.Verticle;
import org.vertx.java.core.app.VerticleFactory;
import org.vertx.java.core.app.VerticleManager;
import org.vertx.java.core.logging.Logger;

import java.lang.reflect.Method;
import java.net.URL;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class GroovyVerticleFactory implements VerticleFactory {

  public Verticle createVerticle(String main, ClassLoader cl) throws Exception {

    URL url = cl.getResource(main);
    GroovyCodeSource gcs = new GroovyCodeSource(url);
    GroovyClassLoader gcl = new GroovyClassLoader(cl);
    Class clazz = gcl.parseClass(gcs);

    Method stop;
    try {
      stop = clazz.getMethod("vertxStop", (Class<?>[])null);
    } catch (NoSuchMethodException e) {
      stop = null;
    }
    final Method mstop = stop;

    Method run;
    try {
      run = clazz.getMethod("run", (Class<?>[])null);
    } catch (NoSuchMethodException e) {
      run = null;
    }
    final Method mrun = run;

    if (run == null) {
      throw new IllegalStateException("Groovy script must have run() method [whether implicit or not]");
    }

    final Object verticle = clazz.newInstance();

    return new Verticle() {
      public void start() {
        try {
          mrun.invoke(verticle, (Object[])null);
        } catch (Throwable t) {
          reportException(t);
        }
      }

      public void stop() {
        if (mstop != null) {
          try {
            mstop.invoke(verticle, (Object[])null);
          } catch (Throwable t) {
            reportException(t);
          }
        }
      }
    };
  }

  public void reportException(Throwable t) {
    VerticleManager.instance.getLogger().error("Exception in Groovy verticle", t);
  }
}

