import express, { json } from "express";
import { App } from "../app";
import { Server } from "net"
import admin from "firebase-admin";
import { FirebaseDeviceService } from "../services/device_service";
import { FirebaseAuthenticator } from "../services/authenticator";
import { createDeviceRouter } from "./devices_routes";
import { FirebaseResourcePolicyService } from "../services/resource_policy_service";
import { handleErrorsMiddleware, logRequestsMiddleware } from "./middlewares";

export class RestApiServerApp implements App {
  private readonly port = 8080
  private readonly app: express.Express;

  private server: Server;

  constructor(firebaseApp: admin.app.App) {
    this.app = express();

    const firestore = firebaseApp.firestore();
    const firebaseAuth = firebaseApp.auth();
    const authService = new FirebaseAuthenticator(firebaseAuth, firestore)
    const service = new FirebaseDeviceService(firestore)
    const resourcePolicyService = new FirebaseResourcePolicyService(firestore)

    // Middleware to parse json body
    this.app.use(json());

    // Middleware to log requests
    this.app.use(logRequestsMiddleware)

    this.app.use("/api/v1/devices", createDeviceRouter(service, authService, resourcePolicyService))

    // Middleware to handle errors
    this.app.use((err, req, res, next) => {
      handleErrorsMiddleware(err, res)
      next()
    });
  }

  startServer() {
    this.server = this.app.listen(this.port, () => {
      console.log(`Express server listening on port ${this.port}`)
    })
  }

  stopServer() {
    this.server.close((err: Error) => {
      if (err) {
        console.error("Error encounterd when closing express server", err)
        return
      }

      console.log("Successfully shut down express server")
    })
  }
}