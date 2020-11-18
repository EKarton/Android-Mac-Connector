import { Router } from "express";
import { Authenticator } from "../services/authenticator";
import { createAuthenticateMiddleware } from "./middlewares";
import { DeviceService } from "../services/device_service";
import { ResourcePolicyService } from "../services/resource_policy_service";

export const createDeviceRouter = (authenticator: Authenticator, deviceService: DeviceService, resourcePolicyService: ResourcePolicyService) => {
  const router = Router();

  // Middleware to authenticate user
  router.use(createAuthenticateMiddleware(authenticator))

  router.get("/registered", async (req, res) => {
    const userId = req.header("user_id")
    const deviceType = req.query.device_type.toString()
    const hardwareId = req.query.hardware_id.toString()

    const result = await deviceService.doesDeviceExist(userId, deviceType, hardwareId)

    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({
      "is_registered": result.length > 0,
      "device_id": result
    })
  });

  router.post("/register", async (req, res) => {
    const userId = req.header("user_id")
    const deviceType = req.body["device_type"]
    const hardwareId = req.body["hardware_id"]
    const capabilities = <string[]>req.body["capabilities"]

    // Register the device
    const deviceId = await deviceService.registerDevice(userId, deviceType, hardwareId, capabilities)
    
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

  router.get("/", async (req, res) => {
    const userId = req.header("user_id")

    const devices = await deviceService.getDevices(userId)

    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({
      "devices": devices
    })
  })

  router.delete("/:deviceId", async (req, res) => {
    const deviceId = req.params.deviceId

    await deviceService.removeDevice(deviceId)
    await resourcePolicyService.deletePermission(null, null, deviceId)

    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({
      "status": "success"
    })
  });

  router.get("/:deviceId/capabilities", async (req, res) => {
    const deviceId = req.params.deviceId
    const capabilities = await deviceService.getDeviceCapabilities(deviceId)
    
    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({
      "capabilities": capabilities
    })
  });

  router.put("/:deviceId/capabilities", async (req, res) => {
    const deviceId = req.params.deviceId
    const newCapabilities = req.body["new_capabilities"]

    await deviceService.updateDeviceCapabilities(deviceId, newCapabilities)

    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({
      "status": "success"
    })
  });

  router.put("/:deviceId/token", async (req, res) => {
    const deviceId = req.params.deviceId
    const newToken = req.body["new_token"]

    await deviceService.updatePushNotificationToken(deviceId, newToken)

    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({
      "status": "success"
    })
  });

  return router
}