import { HttpError } from "../../src/rest_api/middlewares";
import { FirebaseDeviceService, UpdatedDevice } from "../../src/services/device_service";

describe("FirebaseDeviceService", () => {
  describe("doesDeviceExist()", () => {
    it("given valid user id, device type, hardware id, it should return the document id", async () => {
      // TODO
    })

    it("given unknown user id / device type / hardware id, it should throw an error", async () => {
      // TODO
    })
  })

  describe("registerDevice()", () => {
    it("given valid userid, device type, hardware id, name, capabilities, it should create a new document and return the document id", async () => {
      // TODO
    })
  })
  
  describe("getDevices()", () => {
    it("given an existing user id, it should return the correct list of devices", async () => {
      // TODO
    })

    it("given an unknown user id, it should return an empty list", async () => {
      // TODO
    })
  })

  describe("getDevice()", () => {
    const updateFn = jest.fn()
    const firestore = jest.fn().mockImplementation(() => ({
      collection: jest.fn().mockImplementation(() => ({
        doc: jest.fn().mockImplementation((docId) => ({
          get: jest.fn().mockImplementation(() => ({
            exists: docId == "Device1234",
            id: docId == "Device1234" ? "Device1234" : "",
            data: jest.fn().mockImplementation(() => ({
              device_type: "Android",
              name: "My Android phone",
              capabilities: ["send_sms", "read_sms"],
              user_id: "User1234"
            }))
          })),
          update: updateFn
        }))
      }))
    }))
    const service = new FirebaseDeviceService(firestore())

    it("given an existing device id, it should return the correct device", async () => {
      const result = await service.getDevice("Device1234")
      expect(result).toEqual({
        id: "Device1234",
        type: "Android",
        name: "My Android phone",
        capabilities: ["send_sms", "read_sms"]
      })
    })

    it("given an unknown device id, it should throw an error", async () => {
      const expectedError = new HttpError(404, "DeviceNotExist", "Device does not exist")
      await expect(service.getDevice("Device9999")).rejects.toThrow(expectedError)
    })
  })

  describe("updateDevice()", () => {
    const updateFn = jest.fn()
    const firestore = jest.fn().mockImplementation(() => ({
      collection: jest.fn().mockImplementation(() => ({
        doc: jest.fn().mockImplementation((docId) => ({
          get: jest.fn().mockImplementation(() => ({
            exists: docId == "Device1234"
          })),
          update: updateFn
        }))
      }))
    }))
    const service = new FirebaseDeviceService(firestore())

    beforeEach(() => {
      updateFn.mockReset()
    })

    it("given existing device id and correct updated info, it should update the document", async () => {
      const updatedInfo: UpdatedDevice = {
        new_type: "iPhone",
        new_name: "My iPhone",
        new_capabilities: [],
      }

      await service.updateDevice("Device1234", updatedInfo)
      expect(updateFn).toBeCalledWith({
        device_type: "iPhone",
        name: "My iPhone",
        capabilities: []
      })
    })

    it("given existing device id but nothing to update, it should not do anything", async () => {
      const updatedInfo: UpdatedDevice = {}
      await service.updateDevice("Device1234", updatedInfo)
      expect(updateFn).toBeCalledTimes(0)
    })

    it("given unknown device id, it should throw an error", async () => {
      const updatedInfo: UpdatedDevice = {
        new_type: "iPhone",
        new_name: "My iPhone",
        new_capabilities: [],
      }

      const pendingResult = service.updateDevice("Device9999", updatedInfo)
      const expectedError = new HttpError(404, "DeviceNotExist", "Device does not exist")
      await expect(pendingResult).rejects.toThrowError(expectedError)
    })
  })

  describe("removeDevice()", () => {
    const deleteFn = jest.fn()
    const firestore = jest.fn().mockImplementation(() => ({
      collection: jest.fn().mockImplementation(() => ({
        doc: jest.fn().mockImplementation((docId) => ({
          get: jest.fn().mockImplementation(() => ({
            exists: docId == "Device1234"
          })),
          delete: deleteFn
        }))
      }))
    }))
    const service = new FirebaseDeviceService(firestore())

    it("given valid device id, it should call Firebase.delete()", async () => {
      await service.removeDevice("Device1234")

      expect(deleteFn).toBeCalled()
    })

    it("given unknown device id, it should throw an error", async () => {
      await expect(service.removeDevice("Device9999")).rejects.toThrowError(new HttpError(404, "DeviceNotExist", "Device does not exist"))
    })
  })

  describe("updatePushNotificationToken()", () => {
    let updateFn = null
    const firestore = jest.fn().mockImplementation(() => ({
      collection: jest.fn().mockImplementation(() => ({
        doc: jest.fn().mockImplementation((deviceId) => {
          updateFn = jest.fn().mockImplementation(() => {
            if (deviceId != "Device1234") {
              return Promise.reject(new Error("Cannot find document id"))
            }
            return Promise.resolve()
          })
          return {
            get: jest.fn().mockImplementation(() => ({
              exists: deviceId == "Device1234"
            })),
            update: updateFn
          }
        })
      }))
    }))

    it("given existing device id, it should update the document with the new token", async () => {
      const service = new FirebaseDeviceService(firestore())
      await service.updatePushNotificationToken("Device1234", "NewToken")

      expect(updateFn).toBeCalledWith("push_notification_token", "NewToken")
    })

    it("given an unknown device id, it should throw an error", async () => {
      const service = new FirebaseDeviceService(firestore())
      const pendingResult = service.updatePushNotificationToken("Device9999", "New Token")
      const expectedError = new HttpError(404, "DeviceNotFound", `Device with id Device9999 does not exist`)
      
      await expect(pendingResult).rejects.toThrowError(expectedError)
    })
  })

  describe("getPushNotificationToken()", () => {
    const firestore = jest.fn().mockImplementation(() => ({
      collection: jest.fn().mockImplementation(() => ({
        doc: jest.fn().mockImplementation((deviceId) => ({
          get: jest.fn().mockImplementation(() => ({
            exists: deviceId == "Device1234",
            data: jest.fn().mockImplementation(() => ({
              push_notification_token: "Token1234"
            }))
          }))
        }))
      }))
    }))

    it("given an existing device id, it should return the correct token", async () => {
      const service = new FirebaseDeviceService(firestore())
      const result = await service.getPushNotificationToken("Device1234")
      expect(result).toEqual("Token1234")
    })

    it("given an unknown device id, it should throw an error", async () => {
      const service = new FirebaseDeviceService(firestore())
      const pendingResult = service.getPushNotificationToken("Device9999")
      const expectedError = new HttpError(404, "DeviceNotFound", `Device with id Device9999 does not exist`)
      await expect(pendingResult).rejects.toThrowError(expectedError)
    })
  })
})