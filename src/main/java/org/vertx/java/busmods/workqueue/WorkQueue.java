package org.vertx.java.busmods.workqueue;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.app.Verticle;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.LinkedList;
import java.util.Queue;

/**
 * TODO finish persistence
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class WorkQueue extends BusModBase implements Verticle {

  private static final Logger log = Logger.getLogger(WorkQueue.class);

  // LHS is typed as ArrayList to ensure high perf offset based index operations
  private final Queue<String> processors = new LinkedList<>();
  private final Queue<JsonObject> messages = new LinkedList<>();
  private Handler<Message<JsonObject>> registerHandler;
  private Handler<Message<JsonObject>> unregisterHandler;
  private Handler<Message<JsonObject>> sendHandler;

  private long processTimeout;
  private String persistorAddress;
  private String collection;

  public WorkQueue() {
    super(false);
  }

  public void start() {
    super.start();

    processTimeout = super.getOptionalLongConfig("process_timeout", 5 * 60 * 1000);
    persistorAddress = super.getOptionalStringConfig("persistor_address", null);
    collection = super.getOptionalStringConfig("collection", null);

    registerHandler = new Handler<Message<JsonObject>>() {
      public void handle(Message<JsonObject> message) {
        doRegister(message);
      }
    };
    eb.registerHandler(address + ".register", registerHandler);
    unregisterHandler = new Handler<Message<JsonObject>>() {
      public void handle(Message<JsonObject> message) {
        doUnregister(message);
      }
    };
    eb.registerHandler(address + ".unregister", unregisterHandler);
    sendHandler = new Handler<Message<JsonObject>>() {
      public void handle(Message<JsonObject> message) {
        doSend(message);
      }
    };
    eb.registerHandler(address, sendHandler);
  }

  public void stop() {
    eb.unregisterHandler(address + ".register", registerHandler);
    eb.unregisterHandler(address + ".unregister", unregisterHandler);
    eb.unregisterHandler(address, sendHandler);
  }

  private void checkWork() {
    if (!messages.isEmpty() && !processors.isEmpty()) {
      final JsonObject message = messages.poll();
      final String address = processors.poll();
      final long timeoutID = Vertx.instance.setTimer(processTimeout, new Handler<Long>() {
        public void handle(Long id) {
          // Processor timed out - put message back on queue
          log.warn("Processor timed out, message will be put back on queue");
          messages.add(message);
        }
      });
      eb.send(address, message, new Handler<Message<JsonObject>>() {
        public void handle(Message<JsonObject> reply) {
          Vertx.instance.cancelTimer(timeoutID);
          processors.add(address);
          checkWork();
        }
      });

    }
  }

  private void doRegister(Message<JsonObject> message) {
    String processor = getMandatoryString("processor", message);
    if (processor == null) {
      return;
    }
    processors.add(processor);
    checkWork();
    sendOK(message);
  }

  private void doUnregister(Message<JsonObject> message) {
    String processor = getMandatoryString("processor", message);
    if (processor == null) {
      return;
    }
    processors.remove(processor);
    sendOK(message);
  }

  private void doSend(final Message<JsonObject> message) {
    if (persistorAddress != null) {
      JsonObject msg = new JsonObject().putString("action", "save").putString("collection", collection)
                                       .putObject("document", message.body);
      eb.send(persistorAddress, msg, new Handler<Message<JsonObject>>() {
        public void handle(Message<JsonObject> reply) {
          if (reply.body.getString("status").equals("ok")) {
            actualSend(message, message.body);
          } else {
            sendError(message, reply.body.getString("message"));
          }
        }
      });
    } else {
      actualSend(message, message.body);
    }
  }

  private void actualSend(Message<JsonObject> message, JsonObject work) {
    messages.add(work);
    //Been added to the queue so reply
    sendOK(message);
    checkWork();
  }

}
