import { AllowAllAuthenticator, FirebaseAuthenticator } from "../../src/services/authenticator"

describe("AllowAllAuthenticator", () => {
  const authenticator = new AllowAllAuthenticator()

  it("given any device id, username, and password; authenticate() should return true", async () => {
    const result = await authenticator.authenticate("device-id", "username", "password")
    expect(result).toBeTruthy()
  })

  it("given any auth token, getUserIdFromToken() should return the default user id", async () => {
    const result = await authenticator.getUserIdFromToken("token")
    expect(result).toEqual("User-1234")
  })
})

describe("FirebaseAuthenticator", () => {
  // Mocks for Firestore
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

  // Mocks for Firebase Auth
  const verifyIdTokenFn = jest.fn()
  const firebaseAuth = jest.fn().mockImplementation(() => {
    return {
      verifyIdToken: verifyIdTokenFn
    }
  })

  describe("given valid device id, username, and password", () => {
    const authenticator = new FirebaseAuthenticator(firebaseAuth(), firestore())

    beforeAll(() => {
      verifyIdTokenFn.mockImplementation((token) => ({
        uid: token == "1234" ? "User1234" : "User9999"
      }))
      docFn.mockImplementation((deviceId) => ({
        get: getFn.mockImplementation(() => ({
          exists: true,
          get: jest.fn().mockReturnValue(deviceId == "Device1234" ? "User1234" : "User0000")
        }))
      }))
    })

    it("and device id belongs to the same user as the password; authenticate() should return true", async () => {
      const result = await authenticator.authenticate("Device1234", "username", "1234")
      expect(result).toBeTruthy()
    })

    it("but device id belong to a different user from the password; authenticate() should return false", async () => {  
      const result = await authenticator.authenticate("Device1234", "username", "9999")
      expect(result).toBeFalsy()
    })

    it("getUserIdFromToken() should return true", async () => {  
      const result = await authenticator.getUserIdFromToken("9999")
      expect(result).toBeTruthy()
    })
  })

  describe("given a valid device id but invalid username and password", () => {
    it("authenticate() should throw an error", async () => {
      verifyIdTokenFn.mockImplementation((token) => {
        return Promise.reject(new Error("Invalid token"))
      })
  
      const authenticator = new FirebaseAuthenticator(firebaseAuth(), firestore())
      await expect(authenticator.authenticate("Device1234", "username", "9999")).rejects.toThrow("Invalid token")
    })
  })

  describe("given an invalid device id but valid username and password", () => {
    it("authenticate() should throw an error", async () => {
      verifyIdTokenFn.mockImplementation((token) => ({
        uid: token == "1234" ? "User1234" : "User9999"
      }))
  
      docFn.mockImplementation((deviceId) => ({
        get: getFn.mockImplementation(() => ({
          exists: false
        }))
      }))
  
      const authenticator = new FirebaseAuthenticator(firebaseAuth(), firestore())
      await expect(authenticator.authenticate("Device1234", "username", "1234")).rejects.toThrow("No such document exists")
    })
  })

  describe("given FirebaseAuth throws an error", () => {
    const authenticator = new FirebaseAuthenticator(firebaseAuth(), firestore())

    beforeAll(() => {
      verifyIdTokenFn.mockImplementation((token) => {
        return Promise.reject(new Error("Unknown auth error"))
      })
    })

    it("authenticate() should throw that error", async () => {
      await expect(authenticator.authenticate("Device1234", "username", "9999")).rejects.toThrow("Unknown auth error")
    })

    it("getUserIdFromToken() should throw that error", async () => {
      await expect(authenticator.getUserIdFromToken("9999")).rejects.toThrow("Unknown auth error")
    })
  })

  describe("given Firebase throws an error", () => {
    it("authenticate() should throw that error", async () => {
      verifyIdTokenFn.mockImplementation((token) => ({
        uid: token == "1234" ? "User1234" : "User9999"
      }))
      docFn.mockImplementation((deviceId) => ({
        get: getFn.mockImplementation(() => {
          return Promise.reject(new Error("Unknown firestore error"))
        })
      }))
  
      const authenticator = new FirebaseAuthenticator(firebaseAuth(), firestore())
      await expect(authenticator.authenticate("Device1234", "username", "9999")).rejects.toThrow("Unknown firestore error")
    })
  })
})