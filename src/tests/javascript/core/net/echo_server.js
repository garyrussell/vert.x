load('vertx.js')
load('test_utils.js')

var tu = new TestUtils();

var server = new vertx.NetServer();

var h = function(sock) {
  tu.checkContext();
  sock.dataHandler(function(data) {
    tu.checkContext();
    sock.write(data);
  })
};

server.connectHandler(h);

server.listen(1234, 'localhost');

tu.appReady();

function vertxStop() {
  tu.checkContext();
  server.close(function() {
    tu.checkContext();
    tu.appStopped();
  });
}
