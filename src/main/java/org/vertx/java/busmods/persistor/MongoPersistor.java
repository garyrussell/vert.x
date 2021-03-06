package org.vertx.java.busmods.persistor;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;
import org.bson.types.ObjectId;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.app.Verticle;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.net.UnknownHostException;
import java.util.UUID;

/**
 * TODO max batch sizes
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class MongoPersistor extends BusModBase implements Verticle, Handler<Message<JsonObject>> {

  private static final Logger log = Logger.getLogger(MongoPersistor.class);

  private String host;
  private int port;
  private String dbName;

  private Mongo mongo;
  private DB db;

  public MongoPersistor() {
    super(true); // Persistor must be run as a worker
  }

  public void start() {
    super.start();

    host = super.getOptionalStringConfig("host", "localhost");
    port = super.getOptionalIntConfig("port", 27017);
    dbName = super.getMandatoryStringConfig("db_name");

    try {
      mongo = new Mongo(host, port);
      db = mongo.getDB(dbName);
      eb.registerHandler(address, this);
    } catch (UnknownHostException e) {
      log.error("Failed to connect to mongo server", e);
    }
  }

  public void stop() {
    eb.unregisterHandler(address, this);
    mongo.close();
  }

  public void handle(Message<JsonObject> message) {

    String action = message.body.getString("action");

    if (action == null) {
      sendError(message, "action must be specified");
      return;
    }

    switch (action) {
      case "save":
        doSave(message);
        break;
      case "find":
        doFind(message);
        break;
      case "findone":
        doFindOne(message);
        break;
      case "delete":
        doDelete(message);
        break;
      default:
        sendError(message, "Invalid action: " + action);
        return;
    }
  }

  private void doSave(Message<JsonObject> message) {
    String collection = getMandatoryString("collection", message);
    if (collection == null) {
      return;
    }
    JsonObject doc = getMandatoryObject("document", message);
    if (doc == null) {
      return;
    }
    String genID;
    if (doc.getField("_id") == null) {
      genID = UUID.randomUUID().toString();
      doc.putString("_id", genID);
    } else {
      genID = null;
    }
    DBCollection coll = db.getCollection(collection);
    DBObject obj = jsonToDBObject(doc);
    coll.save(obj);
    if (genID != null) {
      JsonObject reply = new JsonObject();
      reply.putString("_id", genID);
      sendOK(message, reply);
    } else {
      sendOK(message);
    }
  }

  private void doFind(Message<JsonObject> message) {
    String collection = getMandatoryString("collection", message);
    if (collection == null) {
      return;
    }
    Integer limit = (Integer)message.body.getNumber("limit");
    if (limit == null) {
      limit = -1;
    }
    JsonObject matcher = getMandatoryObject("matcher", message);
    if (matcher == null) {
      return;
    }
    JsonObject sort = message.body.getObject("sort");
    DBCollection coll = db.getCollection(collection);
    DBCursor cursor = coll.find(jsonToDBObject(matcher));
    if (limit != -1) {
      cursor.limit(limit);
    }
    if (sort != null) {
      cursor.sort(jsonToDBObject(sort));
    }
    JsonObject reply = new JsonObject();
    JsonArray results = new JsonArray();
    while (cursor.hasNext()) {
      DBObject obj = cursor.next();
      String s = obj.toString();
      JsonObject m = new JsonObject(s);
      results.add(m);
    }
    cursor.close();
    reply.putArray("results", results);
    sendOK(message, reply);
  }

  private void doFindOne(Message<JsonObject> message) {
    String collection = getMandatoryString("collection", message);
    if (collection == null) {
      return;
    }
    JsonObject matcher = message.body.getObject("matcher");
    DBCollection coll = db.getCollection(collection);
    DBObject res;
    if (matcher == null) {
      res = coll.findOne();
    } else {
      res = coll.findOne(jsonToDBObject(matcher));
    }
    JsonObject reply = new JsonObject();
    if (res != null) {
      String s = res.toString();
      JsonObject m = new JsonObject(s);
      reply.putObject("result", m);
    }
    sendOK(message, reply);
  }

  private void doDelete(Message<JsonObject> message) {
    String collection = getMandatoryString("collection", message);
    if (collection == null) {
      return;
    }
    JsonObject matcher = getMandatoryObject("matcher", message);
    if (matcher == null) {
      return;
    }
    DBCollection coll = db.getCollection(collection);
    DBObject obj = jsonToDBObject(matcher);
    WriteResult res = coll.remove(obj);
    int deleted = res.getN();
    JsonObject reply = new JsonObject().putNumber("number", deleted);
    sendOK(message, reply);
  }

  private DBObject jsonToDBObject(JsonObject object) {
    String str = object.encode();
    return (DBObject)JSON.parse(str);
  }

}
