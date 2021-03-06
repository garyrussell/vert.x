load('test_utils.js')
load('vertx.js')

var tu = new TestUtils();

var eb = vertx.EventBus;

var user = 'tim@localhost';

function testMailer() {

  log.println("in testmailer");

  var msg = {
    from: user,
    to: user,
    subject: 'this is the subject',
    body: 'this is the payload'
  }

  eb.send("test.mailer", msg, function(msg) {
    tu.azzert(msg.status == 'ok');
    log.println("Got reply")
    tu.testComplete();
  });

  log.println("Sent mail");
}

function testMailerError() {
  var msg = {
    from: "wdok wdqwd qd",
    to: user,
    subject: 'this is the subject',
    body: 'this is the payload'
  }

  eb.send("test.mailer", msg, function(msg) {
    tu.azzert(msg.status == 'error');
    tu.testComplete();
  });
}

tu.registerTests(this);

var mailerConfig = {address: 'test.mailer'}
var mailerID = vertx.deployWorkerVerticle('busmods/mailer.js', mailerConfig, 1, function() {
  tu.appReady();
});

function vertxStop() {
  tu.unregisterAll();
  vertx.undeployVerticle(mailerID, function() {
    tu.appStopped();
  });
}