import request from "supertest"
import express, { json } from "express";
import { createDeviceRouter } from "../../src/rest_api/devices_routes";

describe("createDeviceRouter()", () => {

  // Mocked device service
  const doesDeviceExistFn = jest.fn()
  const registerDeviceFn = jest.fn()
  const getDevicesFn = jest.fn()
  const getDeviceFn = jest.fn()
  const updateDeviceFn = jest.fn()
  const removeDeviceFn = jest.fn()
  const updatePushNotificationTokenFn = jest.fn()

  const mockedDeviceService = jest.fn().mockImplementation(() => ({
    doesDeviceExist: doesDeviceExistFn,
    registerDevice: registerDeviceFn,
    getDevices: getDevicesFn,
    getDevice: getDeviceFn,
    updateDevice: updateDeviceFn,
    removeDevice: removeDeviceFn,
    updatePushNotificationToken: updatePushNotificationTokenFn
  }))

  // The app
  function makeApp() {
    const app = express()
    app.use(json());
    app.use("/api/v1/devices", createDeviceRouter(mockedDeviceService()))

    return app
  }

  describe("GET /registered", () => {
    function createResponse() {
      return request(makeApp())
        .get('/api/v1/devices/registered?device_type=android&hardware_id=1234')
        .set("user_id", "User 1234")
        .send()
    }

    it("should return correct response given device service does not throw an error", async () => {
      doesDeviceExistFn.mockReturnValue(Promise.resolve("DeviceId1234"))

      const res = await createResponse()
      expect(res.statusCode).toEqual(200)
      expect(res.body).toEqual({
        "device_id": "DeviceId1234",
        "is_registered": true
      })
    })

    it("should return mocked middleware's response given device service throws an error", async () => {
      doesDeviceExistFn.mockImplementation(() => Promise.reject(new Error("Error")))

      const res = await createResponse()
      expect(res.statusCode).toEqual(500)
      expect(res.body).toEqual({})
    })
  })

  describe("GET /registered", () => {
    function sendAndReceiveResponse() {
      return request(makeApp())
        .get('/api/v1/devices/registered?device_type=android&hardware_id=1234')
        .set("user_id", "User 1234")
        .send()
    }

    it("should return correct response given DeviceService returns a proper response", async () => {
      doesDeviceExistFn.mockReturnValue(Promise.resolve("DeviceId1234"))

      const res = await sendAndReceiveResponse()
      expect(res.statusCode).toEqual(200)
      expect(res.body).toEqual({
        "device_id": "DeviceId1234",
        "is_registered": true
      })
    })

    it("should return correct response given DeviceService returns a proper response", async () => {
      doesDeviceExistFn.mockImplementation(() => Promise.reject(new Error("Error")))

      const res = await sendAndReceiveResponse()
      expect(res.statusCode).toEqual(500)
      expect(res.body).toEqual({})
    })
  })

  describe("POST /register", () => {
    function makeRequest() {
      return request(makeApp())
        .post('/api/v1/devices/register')
        .set("user_id", "User 1234")
        .send({
          device_type: "android_phone",
          hardware_id: "hardware_1234",
          name: "My Android Phone",
          capabilities: ["send_sms", "read_sms"]
        })
    }
    
    it("should return correct response given DeviceService returns an expected response", async () => {
      registerDeviceFn.mockResolvedValue(Promise.resolve("Device1234"))
  
      const res = await makeRequest()
      expect(res.statusCode).toEqual(200)
      expect(res.body).toEqual({ "device_id": "Device1234" })
    })

    it("should return correct response given DeviceService throws an error", async () => {
      registerDeviceFn.mockImplementation(() => Promise.reject(new Error("Error")))
  
      const res = await makeRequest()
      expect(res.statusCode).toEqual(500)
      expect(res.body).toEqual({})
    })
  })

  describe("GET /", () => {
    function makeResponse() {
      return request(makeApp())
        .get('/api/v1/devices/')
        .set("user_id", "User 1234")
        .send()
    }
    it("should return correct response given DeviceService returns expected response", async () => {
      getDevicesFn.mockReturnValue(Promise.resolve([
        {
          id: "1234",
          type: "android",
          name: "My Android Phone",
          capabilities: ["send_sms"]
        }
      ]))

      const res = await makeResponse()
      expect(res.statusCode).toEqual(200)
      expect(res.body).toEqual({
        "devices": [
          {
            id: "1234",
            type: "android",
            name: "My Android Phone",
            capabilities: ["send_sms"]
          }
        ]
      })
    })

    it("should return correct response given DeviceService throws an error", async () => {
      getDevicesFn.mockImplementation(() => Promise.reject(new Error("Error")))

      const res = await makeResponse()
      expect(res.statusCode).toEqual(500)
      expect(res.body).toEqual({})
    })
  })

  describe("GET /", () => {
    function makeRequest() {
      return request(makeApp())
        .get('/api/v1/devices/')
        .set("user_id", "User 1234")
        .send()
    }

    it("should return correct response given DeviceService returns expected response", async () => {
      getDevicesFn.mockReturnValue(Promise.resolve([
        {
          id: "1234",
          type: "android",
          name: "My Android Phone",
          capabilities: ["send_sms"]
        }
      ]))
  
      const res = await makeRequest()
      expect(res.statusCode).toEqual(200)
      expect(res.body).toEqual({
        "devices": [
          {
            id: "1234",
            type: "android",
            name: "My Android Phone",
            capabilities: ["send_sms"]
          }
        ]
      })
    })

    it("should return correct response given DeviceService throws an error", async () => {
      getDevicesFn.mockImplementation(() => Promise.reject(new Error("Error")))
  
      const res = await makeRequest()
      expect(res.statusCode).toEqual(500)
      expect(res.body).toEqual({})
    })
  })

  describe("GET /:deviceId", () => {
    function makeRequest() {
      return request(makeApp())
        .get('/api/v1/devices/1234')
        .set("user_id", "User 1234")
        .send()
    }

    it("should return correct response given DeviceService doesn't throw an error", async () => {
      getDeviceFn.mockReturnValue(Promise.resolve({
        id: "1234",
        type: "android",
        name: "My Android Phone",
        capabilities: ["send_sms"]
      }))
  
      const res = await makeRequest()
      expect(res.statusCode).toEqual(200)
      expect(res.body).toEqual({
        id: "1234",
        type: "android",
        name: "My Android Phone",
        capabilities: ["send_sms"]
      })
    })
  
    it("should return correct response given DeviceService throws an error", async () => {
      getDeviceFn.mockImplementation(() => Promise.reject(new Error("Error")))
  
      const res = await makeRequest()
      expect(res.statusCode).toEqual(500)
      expect(res.body).toEqual({})
    })
  })

  describe("PUT /:deviceId", () => {
    function makeRequest() {
      return request(makeApp())
        .put('/api/v1/devices/1234')
        .set("user_id", "User 1234")
        .send({
          new_type: "android",
          new_name: "new name",
          new_capabilities: []
        })
    }

    it("should return the correct response given DeviceService doesn't throw an error", async () => {
      updateDeviceFn.mockReturnValue(Promise.resolve())
  
      const res = await makeRequest()
      expect(res.statusCode).toEqual(200)
      expect(res.body).toEqual({
        status: "success"
      })
    })
  
    it("should return the correct response given DeviceService throws an error", async () => {
      updateDeviceFn.mockImplementation(() => Promise.reject(new Error("Error")))
        
      const res = await makeRequest()
      expect(res.statusCode).toEqual(500)
      expect(res.body).toEqual({})
    })
  })

  describe("DELETE /:deviceId", () => {
    function makeRequest() {
      return request(makeApp())
        .delete('/api/v1/devices/1234')
        .set("user_id", "User 1234")
        .send()
    }

    it("should return the correct response given DeviceService doesn't throw an error", async () => {
      removeDeviceFn.mockReturnValue(Promise.resolve())

      const res = await makeRequest()
      expect(res.statusCode).toEqual(200)
      expect(res.body).toEqual({
        status: "success"
      })
    })

    it("should return the correct response given DeviceService throws an error", async () => {
      removeDeviceFn.mockImplementation(() => Promise.reject(new Error("Error")))

      const res = await makeRequest()
      expect(res.statusCode).toEqual(500)
      expect(res.body).toEqual({})
    })
  })

  describe("PUT /:deviceId/token", () => {
    function makeRequest() {
      return request(makeApp())
        .put('/api/v1/devices/1234/token')
        .set("user_id", "User 1234")
        .send({
          new_token: "new-token-1234"
        })
    }
    it("should return correct response given DeviceService returns expected response", async () => {
      updatePushNotificationTokenFn.mockReturnValue(Promise.resolve())
      
      const res = await makeRequest()
      expect(res.statusCode).toEqual(200)
      expect(res.body).toEqual({ status: "success" })
    })

    it("should return correct response given DeviceService throws an error", async () => {
      updatePushNotificationTokenFn.mockImplementation(() => Promise.reject(new Error("Error")))
      
      const res = await makeRequest()
      expect(res.statusCode).toEqual(500)
      expect(res.body).toEqual({})
    })
  })
})