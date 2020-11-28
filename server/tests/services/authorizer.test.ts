import { FirebaseUserBasedAuthorizer, GrantAllAuthorizer } from "../../src/services/authorizer"

describe("GrantAllAuthorizer", () => {
  describe("isPublishAuthorized()", () => {
    it("should return true", async () => {
      const authorizer = new GrantAllAuthorizer()
      const result = await authorizer.isPublishAuthorized("topic", "clientid")

      expect(result).toBeTruthy()
    })
  })

  describe("isSubscriptionAuthorized()", () => {
    it("should return true", async () => {
      const authorizer = new GrantAllAuthorizer()
      const result = await authorizer.isSubscriptionAuthorized("topic", "clientid")

      expect(result).toBeTruthy()
    })
  })
})

describe("FirebaseUserBasedAuthorizer", () => {

  // Mocks
  const getFn = jest.fn();
  const docFn = jest.fn((deviceId) => { 
    return { 
      get: getFn 
    }
  })
  const collectionFn = jest.fn((collectionName) => {
    return {
      doc: docFn
    }
  })
  const firestore = jest.fn().mockImplementation(() => {
    return {
      collection: collectionFn
    }
  })

  describe("isPublishAuthorized()", () => {
    it("given a valid topic and client id that belong to the same user, it should return true", async () => {
      const getPropertyFn = jest.fn().mockImplementation(() => {
        return "User1234"
      })
      getFn.mockImplementation(() => {
        return {
          exists: true,
          get: getPropertyFn
        }
      })

      const authorizer = new FirebaseUserBasedAuthorizer(new firestore())
      const result = await authorizer.isPublishAuthorized("1234/sms-messages", "1234")

      expect(collectionFn).toBeCalledWith("devices")
      expect(getPropertyFn).toBeCalledWith("user_id")
      expect(result).toBeTruthy()
    })

    it("given a valid topic and client id that belong to a different user, it should return false", async () => {
      docFn.mockImplementation((deviceId) => ({
        get: jest.fn().mockImplementation(() => ({
          exists: true,
          get: jest.fn().mockImplementation(() => {
            return deviceId
          })
        }))
      }))

      const authorizer = new FirebaseUserBasedAuthorizer(new firestore())
      const result = await authorizer.isPublishAuthorized("1234/sms-messages", "6789")
      expect(result).toBeFalsy()
    })

    it("given an invalid topic, it should throw an error", async () => {
      const authorizer = new FirebaseUserBasedAuthorizer(new firestore())
      await expect(authorizer.isPublishAuthorized("1234", "6789")).rejects.toThrow("Invalid topic 1234")
    })

    it("given a topic with an unknown device id, it should throw an error", async () => {
      docFn.mockImplementation((deviceId) => ({
        get: jest.fn().mockImplementation(() => ({
          exists: deviceId == "6789",
          get: jest.fn().mockImplementation(() => {
            return deviceId
          })
        }))
      }))

      const authorizer = new FirebaseUserBasedAuthorizer(new firestore())
      await expect(authorizer.isPublishAuthorized("1234/messages", "6789")).rejects.toThrow("Device id 1234 does not exist")
    })

    it("given a valid topic and an unknown client id, it should throw an error", async () => {
      docFn.mockImplementation((deviceId) => ({
        get: jest.fn().mockImplementation(() => ({
          exists: deviceId == "1234",
          get: jest.fn().mockImplementation(() => {
            return deviceId
          })
        }))
      }))

      const authorizer = new FirebaseUserBasedAuthorizer(new firestore())
      await expect(authorizer.isPublishAuthorized("1234/messages", "6789")).rejects.toThrow("Device id 6789 does not exist")
    })

    it("given firebase throws an error, it should propagate that error", async () => {
      docFn.mockImplementation((deviceId) => ({
        get: jest.fn().mockImplementation(() => {
          return Promise.reject(new Error("Unknown error encountered"))
        })
      }))

      const authorizer = new FirebaseUserBasedAuthorizer(new firestore())
      await expect(authorizer.isPublishAuthorized("1234/messages", "6789")).rejects.toThrow("Unknown error encountered")
    })
  })
})