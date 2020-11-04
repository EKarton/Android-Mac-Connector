import cluster from 'cluster'
import admin from 'firebase-admin';
import process from 'process'
import { MqttServerApp } from './lib/mqtt_server_app';
import { RestApiServerApp } from './lib/rest_api_server_app';

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
  
  new MqttServerApp(firebaseApp).startServer()
  new RestApiServerApp(firebaseApp).startServer()
}