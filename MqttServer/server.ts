import cluster from 'cluster'
import os from 'os'
import process from 'process'
import { MqttServerApp } from './lib/app';
import { FirebaseAuthenticator } from './lib/authenticator';
import { FirebaseAuthorizer } from './lib/authorizer';

let numRetries = 10

if (cluster.isMaster) {
  // Make N copies of the same app with N being the number of CPUs
  for (let i = 0; i < 1; i++) { //os.cpus().length; i++) {
      cluster.fork();
  }

  // Fork the server again if it dies
  cluster.on("exit", (worker: Worker, code: number, signal: string) => {
      console.log("A worker has died!");
      numRetries--;

      if (numRetries > 0) {
          console.log("Relaunching worker again");
          cluster.fork();
      }
  });
} else {
  console.log("Child process #", process.pid, " has spawned");
  new MqttServerApp().startServer()
}