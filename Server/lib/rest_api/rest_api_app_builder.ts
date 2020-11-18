import express, { json } from "express";
import { DeviceService } from "../services/device_service";
import { Authenticator } from "../services/authenticator";
import { createDeviceRouter } from "./devices_routes";
import { ResourcePolicyService } from "../services/resource_policy_service";
import { handleErrorsMiddleware, logRequestsMiddleware } from "./middlewares";

export class RestApiAppBuilder {
  private authenticator: Authenticator;
  private deviceService: DeviceService;
  private resourcePolicyService: ResourcePolicyService;

  constructor() {}

  public withAuthenticator(authenticator: Authenticator): RestApiAppBuilder {
    this.authenticator = authenticator
    return this
  }

  public withDeviceService(deviceService: DeviceService): RestApiAppBuilder {
    this.deviceService = deviceService
    return this
  }

  public withResourcePolicyService(resourcePolicyService: ResourcePolicyService): RestApiAppBuilder {
    this.resourcePolicyService = resourcePolicyService
    return this
  }

  public build(): express.Express {
    const app = express();

    // Middleware to parse json body
    app.use(json());

    // Middleware to log requests
    app.use(logRequestsMiddleware)

    app.use("/api/v1/devices", createDeviceRouter(this.authenticator, this.deviceService, this.resourcePolicyService))

    // Middleware to handle errors
    app.use((err, req, res, next) => {
      handleErrorsMiddleware(err, res)
      next()
    });

    return app
  }
}