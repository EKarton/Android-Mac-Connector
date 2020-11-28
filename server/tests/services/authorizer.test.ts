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

  describe("given a valid topic and client id that belong to the same user", () => {
    const authorizer = new FirebaseUserBasedAuthorizer(new firestore())
    const getPropertyFn = jest.fn().mockImplementation(() => {
      return "User1234"
    })

    beforeAll(() => {
      getFn.mockImplementation(() => {
        return {
          exists: true,
          get: getPropertyFn
        }
      })
    })

    it("isPublishAuthorized() should return true", async () => {
      const result = await authorizer.isPublishAuthorized("1234/sms-messages", "1234")

      expect(collectionFn).toBeCalledWith("devices")
      expect(getPropertyFn).toBeCalledWith("user_id")
      expect(result).toBeTruthy()
    })

    it("isSubscriptionAuthorized() should return true", async () => {
      const result = await authorizer.isSubscriptionAuthorized("1234/sms-messages", "1234")

      expect(collectionFn).toBeCalledWith("devices")
      expect(getPropertyFn).toBeCalledWith("user_id")
      expect(result).toBeTruthy()
    })
  })

  describe("given a valid topic and client id that belong to a different user", () => {
    let authorizer = new FirebaseUserBasedAuthorizer(new firestore());

    beforeEach(() => {
      docFn.mockImplementation((deviceId) => ({
        get: jest.fn().mockImplementation(() => ({
          exists: true,
          get: jest.fn().mockImplementation(() => {
            return deviceId
          })
        }))
      }))
    })

    it("isPublishAuthorized() should return false", async () => {
      const result = await authorizer.isPublishAuthorized("1234/sms-messages", "6789")
      expect(result).toBeFalsy()
    })

    it("isSubscriptionAuthorized() should return false", async () => {
      const result = await authorizer.isSubscriptionAuthorized("1234/sms-messages", "6789")
      expect(result).toBeFalsy()
    })
  })

  describe("given an invalid topic", () => {
    const authorizer = new FirebaseUserBasedAuthorizer(new firestore())

    it("isPublishAuthorized() should throw an error", async () => {
      await expect(authorizer.isPublishAuthorized("1234", "6789")).rejects.toThrow("Invalid topic 1234")
    })

    it("isPublishAuthorized() should throw an error", async () => {
      await expect(authorizer.isSubscriptionAuthorized("1234", "6789")).rejects.toThrow("Invalid topic 1234")
    })
  })

  describe("given a topic with an unknown device id", () => {
    const authorizer = new FirebaseUserBasedAuthorizer(new firestore())

    beforeAll(() => {
      docFn.mockImplementation((deviceId) => ({
        get: jest.fn().mockImplementation(() => ({
          exists: deviceId == "6789",
          get: jest.fn().mockImplementation(() => {
            return deviceId
          })
        }))
      }))
    })

    it("isPublishAuthorized() should throw an error", async () => {
      await expect(authorizer.isPublishAuthorized("1234/messages", "6789")).rejects.toThrow("Device id 1234 does not exist")
    })

    it("isSubscriptionAuthorized() should throw an error", async () => {
      await expect(authorizer.isSubscriptionAuthorized("1234/messages", "6789")).rejects.toThrow("Device id 1234 does not exist")
    })
  })

  describe("given a valid topic and an unknown client id", () => {
    const authorizer = new FirebaseUserBasedAuthorizer(new firestore())

    beforeAll(() => {
      docFn.mockImplementation((deviceId) => ({
        get: jest.fn().mockImplementation(() => ({
          exists: deviceId == "1234",
          get: jest.fn().mockImplementation(() => {
            return deviceId
          })
        }))
      }))
    })
    
    it("isPublishAuthorized() should throw an error", async () => {
      await expect(authorizer.isPublishAuthorized("1234/messages", "6789")).rejects.toThrow("Device id 6789 does not exist")
    })

    it("isSubscriptionAuthorized() should throw an error", async () => {
      await expect(authorizer.isSubscriptionAuthorized("1234/messages", "6789")).rejects.toThrow("Device id 6789 does not exist")
    })
  })

  describe("given firebase throws an error", () => {
    const authorizer = new FirebaseUserBasedAuthorizer(new firestore())

    beforeAll(() => {
      docFn.mockImplementation((deviceId) => ({
        get: jest.fn().mockImplementation(() => {
          return Promise.reject(new Error("Unknown error encountered"))
        })
      }))
    })

    it("isPublishAuthorized() should propagate that error", async () => {
      await expect(authorizer.isPublishAuthorized("1234/messages", "6789")).rejects.toThrow("Unknown error encountered")
    })

    it("isSubscriptionAuthorized() should propagate that error", async () => {
      await expect(authorizer.isSubscriptionAuthorized("1234/messages", "6789")).rejects.toThrow("Unknown error encountered")
    })
  })
})