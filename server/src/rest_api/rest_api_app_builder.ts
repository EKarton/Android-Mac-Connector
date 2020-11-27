import express, { json } from "express";
import { DeviceService } from "../services/device_service";
import { AllowAllAuthenticator, Authenticator } from "../services/authenticator";
import { createDeviceRouter } from "./devices_routes";
import { createAuthenticateMiddleware, handleErrorsMiddleware, logRequestsMiddleware } from "./middlewares";

export interface RestApiAppBuilderOpts {
  verifyAuthentication: boolean
}

export class RestApiAppBuilder {
  private opts?: RestApiAppBuilderOpts;
  private authenticator: Authenticator;
  private deviceService: DeviceService;

  constructor() {}

  public withOpts(opts: RestApiAppBuilderOpts): RestApiAppBuilder {
    this.opts = opts
    return this
  }

  public withAuthenticator(authenticator: Authenticator): RestApiAppBuilder {
    this.authenticator = authenticator
    return this
  }

  public withDeviceService(deviceService: DeviceService): RestApiAppBuilder {
    this.deviceService = deviceService
    return this
  }

  public build(): express.Express {
    if (!(this.opts?.verifyAuthentication)) {
      this.authenticator = new AllowAllAuthenticator()
    }

    if (this.authenticator == null || this.deviceService == null) {
      throw new Error("Authenticator and device service must be specified")
    }

    const app = express();

    // Middleware to parse json body
    app.use(json());

    // Middleware to log requests
    app.use(logRequestsMiddleware)

    // Middleware to authenticate requests
    app.use(createAuthenticateMiddleware(this.authenticator))

    app.use("/api/v1/devices", createDeviceRouter(this.deviceService))

    // Middleware to handle errors
    app.use((err, req, res, next) => {
      handleErrorsMiddleware(err, res)
      next()
    });

    return app
  }
}