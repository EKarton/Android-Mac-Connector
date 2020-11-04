import { json, Router } from "express";
import { Authenticator } from "../authenticator";
import { DeviceService } from "./device_service";
import { ResourcePolicyService } from "./resource_policy_service";

export const createDeviceRouter = (service: DeviceService, authService: Authenticator, resourcePolicyService: ResourcePolicyService) => {
  const router = Router();

  // Middleware to authenticate user
  router.use(async (req, res, next) => {
    const authHeaderValue = req.header("Authorization")

    if (!authHeaderValue) {
      throw new Error("Missing Authorization header")
    }

    if (authHeaderValue.length < 7) {
      throw new Error("Malformed Authorization header")
    }

    if (authHeaderValue.substring(0, 6) != "Bearer") {
      throw new Error("Invalid Authorization header prefix")
    }

    const accessToken = authHeaderValue.substring(7)
    const userId = await authService.getUserIdFromToken(accessToken)

    req.headers.user_id = userId

    next()
  })

  router.get("/registered", async (req, res) => {
    const userId = req.header("user_id")
    const deviceType = req.query.device_type.toString()
    const hardwareId = req.query.hardware_id.toString()

    console.log(userId, deviceType, hardwareId)

    const result = await service.doesDeviceExist(userId, deviceType, hardwareId)

    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({
      "is_registered": result
    })
  });

  router.post("/register", async (req, res) => {
    const userId = req.header("user_id")
    const deviceType = req.body["device_type"]
    const hardwareId = req.body["hardware_id"]
    const capabilities = <string[]>req.body["capabilities"]

    console.log(userId, deviceType, hardwareId, capabilities)

    // Register the device
    const deviceId = await service.registerDevice(userId, deviceType, hardwareId, capabilities)
    
    // Add default resource policies
    const pendingResults1 = capabilities.map(capability => {
      const resource1 = `${capability}:publish`
      return resourcePolicyService.addPermission(resource1, deviceId, deviceId)
    });

    const pendingResults2 = capabilities.map(capability => {
      const resource1 = `${capability}:subscribe`
      return resourcePolicyService.addPermission(resource1, deviceId, deviceId)
    });

    Promise.all([...pendingResults1, ...pendingResults2])

    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({
      "device_id": deviceId
    })
  });

  router.get("/:deviceId/capabilities", async (req, res) => {
    const deviceId = req.params.deviceId
    const capabilities = await service.getDeviceCapabilities(deviceId)
    
    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({
      "capabilities": capabilities
    })
  });

  router.put("/:deviceId/capabilities", async (req, res) => {
    const deviceId = req.params.deviceId
    const newCapabilities = req.body["new_capabilities"]

    await service.updateDeviceCapabilities(deviceId, newCapabilities)

    res.setHeader('Content-Type', 'application/json');
    res.status(200)
  });

  router.put("/:deviceId/token", async (req, res) => {
    const deviceId = req.params.deviceId
    const newToken = req.body["new_token"]

    await service.updatePushNotificationToken(deviceId, newToken)

    res.setHeader('Content-Type', 'application/json');
    res.status(200)
  });

  return router
}