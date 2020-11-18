import cluster from 'cluster'
import admin from 'firebase-admin';
import { createServer } from 'http';
import process from 'process'
import { MqttAppBuilder } from './lib/mqtt/mqtt_app_builder';
import { RestApiAppBuilder } from './lib/rest_api/rest_api_app_builder';

let numRetries = 10

if (cluster.isMaster) {
  // Make N copies of the same app with N being the number of CPUs
  for (let i = 0; i < 1; i++) { //os.cpus().length; i++) {
      cluster.fork();
  }

  // Fork the server again if it dies
  cluster.on("exit", () => {
      console.log("A worker has died!");
      numRetries--;

      if (numRetries > 0) {
          console.log("Relaunching worker again");
          cluster.fork();
      }
  });
} else {
  console.log("Child process #", process.pid, " has spawned");

  const firebaseApp = admin.initializeApp();

  const httpApp = new RestApiAppBuilder(firebaseApp).build()
  const mqttApp = new MqttAppBuilder(firebaseApp)
    .withOpts({
      verifyAuthorization: !(process.env.VERIFY_AUTHORIZATION == "false"),
      verifyAuthentication: !(process.env.VERIFY_AUTHENTICATION == "false"),
    })
    .build()

  const server = createServer(httpApp);
  require('websocket-stream').createServer({ server: server }, mqttApp.handle)

  const port = process.env.PORT || 3000
  server.listen(port, () => {
    console.log(`Listening to port ${port}`)
  });
}