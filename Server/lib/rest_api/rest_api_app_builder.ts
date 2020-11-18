import express, { json } from "express";
import admin from "firebase-admin";
import { FirebaseDeviceService } from "../services/device_service";
import { FirebaseAuthenticator } from "../services/authenticator";
import { createDeviceRouter } from "./devices_routes";
import { FirebaseResourcePolicyService } from "../services/resource_policy_service";
import { handleErrorsMiddleware, logRequestsMiddleware } from "./middlewares";

export class RestApiAppBuilder { 
  private readonly firebaseApp: admin.app.App;

  constructor(firebaseApp: admin.app.App) {
    this.firebaseApp = firebaseApp
  }

  public build(): express.Express {
    const app = express();

    const firestore = this.firebaseApp.firestore();
    const firebaseAuth = this.firebaseApp.auth();
    const authService = new FirebaseAuthenticator(firebaseAuth, firestore)
    const service = new FirebaseDeviceService(firestore)
    const resourcePolicyService = new FirebaseResourcePolicyService(firestore)

    // Middleware to parse json body
    app.use(json());

    // Middleware to log requests
    app.use(logRequestsMiddleware)

    app.use("/api/v1/devices", createDeviceRouter(service, authService, resourcePolicyService))

    // Middleware to handle errors
    app.use((err, req, res, next) => {
      handleErrorsMiddleware(err, res)
      next()
    });

    return app
  }
}