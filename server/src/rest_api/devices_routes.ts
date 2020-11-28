import { Router } from "express";
import asyncHandler from "express-async-handler"
import { DeviceService } from "../services/device_service";

export const createDeviceRouter = (deviceService: DeviceService) => {
  const router = Router();  

  router.get("/registered", asyncHandler(async (req, res) => {
    const userId = req.header("user_id")
    const deviceType = req.query.device_type.toString()
    const hardwareId = req.query.hardware_id.toString()

    const result = await deviceService.doesDeviceExist(userId, deviceType, hardwareId)

    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({
      "is_registered": result.length > 0,
      "device_id": result
    })
  }));

  router.post("/register", asyncHandler(async (req, res) => {
    const userId = req.header("user_id")
    const deviceType = req.body["device_type"]
    const hardwareId = req.body["hardware_id"]
    const name = req.body["name"]
    const capabilities = <string[]>req.body["capabilities"]

    // Register the device
    const deviceId = await deviceService.registerDevice(userId, deviceType, hardwareId, name, capabilities)

    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({
      "device_id": deviceId
    })
  }));

  router.get("/", asyncHandler(async (req, res) => {
    const userId = req.header("user_id")

    const devices = await deviceService.getDevices(userId)

    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({
      "devices": devices
    })
  }))

  router.get("/:deviceId", asyncHandler(async (req, res) => {
    const deviceId = req.params.deviceId
    const device = await deviceService.getDevice(deviceId)

    res.setHeader('Content-Type', 'application/json');
    res.status(200).json(device)
  }));

  router.put("/:deviceId", asyncHandler(async (req, res) => {
    const deviceId = req.params.deviceId
    const updatedProperties = req.body

    await deviceService.updateDevice(deviceId, updatedProperties)

    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({
      "status": "success"
    })
  }));

  router.delete("/:deviceId", asyncHandler(async (req, res) => {
    const deviceId = req.params.deviceId

    await deviceService.removeDevice(deviceId)

    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({
      "status": "success"
    })
  }));

  // deprecated
  router.get("/:deviceId/capabilities", asyncHandler(async (req, res) => {
    const deviceId = req.params.deviceId
    const capabilities = await deviceService.getDeviceCapabilities(deviceId)
    
    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({
      "capabilities": capabilities
    })
  }));

  // deprecated
  router.put("/:deviceId/capabilities", asyncHandler(async (req, res) => {
    const deviceId = req.params.deviceId
    const newCapabilities = req.body["new_capabilities"]

    await deviceService.updateDeviceCapabilities(deviceId, newCapabilities)

    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({
      "status": "success"
    })
  }));

  router.put("/:deviceId/token", asyncHandler(async (req, res) => {
    const deviceId = req.params.deviceId
    const newToken = req.body["new_token"]

    await deviceService.updatePushNotificationToken(deviceId, newToken)

    res.setHeader('Content-Type', 'application/json');
    res.status(200).json({
      "status": "success"
    })
  }));

  return router
}