import { Router } from "express";
import request from "supertest"
import { RestApiAppBuilder } from "../../src/rest_api/rest_api_app_builder";
import * as middlewares from "../../src/rest_api/middlewares"
import * as router from "../../src/rest_api/devices_routes"
import { Authenticator } from "../../src/services/authenticator";

// Mock middlewares
const spiedLogRequestsMiddleware = jest.spyOn(middlewares, "logRequestsMiddleware")
spiedLogRequestsMiddleware.mockImplementation((req, res, next) => {
  next()
})

const spiedCreateAuthenticateMiddleware = jest.spyOn(middlewares, "createAuthenticateMiddleware")
spiedCreateAuthenticateMiddleware.mockImplementation((_authenticator: Authenticator) => {
  return (req, res, next) => {
    next()
  }
})

const spiedHandleErrorsMiddleware = jest.spyOn(middlewares, "handleErrorsMiddleware")
spiedHandleErrorsMiddleware.mockImplementation((err: Error, res) => {})

// Mocked authenticator
const MockedAuthenticator = jest.fn().mockImplementation(() => ({
  authenticate: jest.fn().mockReturnValue(Promise.resolve(true)),
  getUserIdFromToken: jest.fn().mockReturnValue(Promise.resolve("User1234"))
}))

// Mocked device service
const MockedDeviceService = jest.fn().mockImplementation(() => ({
  testFunction: jest.fn()
}))

// Mocked devices router
const spiedCreateDeviceRouterFn = jest.spyOn(router, "createDeviceRouter")
spiedCreateDeviceRouterFn.mockImplementation(() => {
  const router = Router();
  router.get("/deviceroute", (req, res) => {
    res.status(200).json({})
  })

  return router
})

describe("RestApiAppBuilder", () => {
  it("given authenticator, device service, and router, when a request is made, it should go through the authenticator", async () => {
    const deviceService = MockedDeviceService()
    const authenticator = MockedAuthenticator()

    const app = new RestApiAppBuilder()
      .withAuthenticator(authenticator)
      .withDeviceService(deviceService)
      .build()

    const req = await request(app)
      .get("/api/v1/devices/deviceroute")
      .set("Authorization", "Bearer 1234")
      .send()

    expect(req.statusCode).toEqual(200)
    expect(req.body).toEqual({})
    expect(spiedLogRequestsMiddleware).toBeCalled()
    expect(spiedCreateAuthenticateMiddleware).toBeCalledWith(authenticator)
    expect(spiedCreateDeviceRouterFn).toBeCalledWith(deviceService)
  })

  it("given authenticator, device service, and router, when a request is made and an error is thrown, it should go through the error middleware", async () => {
    const deviceService = MockedDeviceService()
    const authenticator = MockedAuthenticator()

    spiedCreateDeviceRouterFn.mockImplementation(() => {
      const router = Router();
      router.get("/deviceroute", (req, res) => {
        throw new Error("Error")
      })
    
      return router
    })

    const app = new RestApiAppBuilder()
      .withAuthenticator(authenticator)
      .withDeviceService(deviceService)
      .build()

    const req = await request(app)
      .get("/api/v1/devices/deviceroute")
      .set("Authorization", "Bearer 1234")
      .send()

    expect(req.statusCode).toEqual(404)
    expect(req.body).toEqual({})
    expect(spiedLogRequestsMiddleware).toBeCalled()
    expect(spiedCreateAuthenticateMiddleware).toBeCalledWith(authenticator)
    expect(spiedCreateDeviceRouterFn).toBeCalledWith(deviceService)
    expect(spiedHandleErrorsMiddleware).toBeCalledWith(new Error("Error"), expect.anything())
  })
})