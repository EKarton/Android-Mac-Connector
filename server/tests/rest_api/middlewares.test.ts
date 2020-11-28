import { createAuthenticateMiddleware, handleErrorsMiddleware, HttpError, logRequestsMiddleware } from "../../src/rest_api/middlewares"

describe("logRequestsMiddleware()", () => {
  const spy = jest.spyOn(global.console, 'log').mockImplementation()

  afterEach(() => {
    spy.mockRestore()
  })

  it("given request, response, and next, it should log the request and call next()", async done => {
    const request = {
      method: "GET",
      url: "http://localhost:8080"
    }
    const response = {
      statusCode: 200
    }
    const next = () => {
      expect(console.log).toBeCalledWith("GET http://localhost:8080: 200")
      done() 
    }

    logRequestsMiddleware(request, response, next)
  })
})

describe("handleErrorsMiddleware()", () => {
  const jsonFn = jest.fn()
  const statusFn = jest.fn().mockImplementation((statusCode) => {
    return {
      json: jsonFn
    }
  })
  const mockedResponse = {
    status: statusFn
  }
  
  it("given HttpError, it should set the correct http status code and the proper json response", async () => {
    const error = new HttpError(400, "CustomErrorCode", "Custom Reason")
    handleErrorsMiddleware(error, mockedResponse)

    expect(statusFn).toBeCalledWith(400)
    expect(jsonFn).toBeCalledWith({"error_code": "CustomErrorCode", "reason": "Custom Reason"})
  })

  it("given any error, it should return a 500 with the correct error message", () => {
    const error = new Error("Unknown error message")
    handleErrorsMiddleware(error, mockedResponse)

    expect(statusFn).toBeCalledWith(500)
    expect(jsonFn).toBeCalledWith({"error_code": "UnknownError", "reason": "Unknown error message"})
  })
})

describe("createAuthenticateMiddleware()", () => {

  // Mock authenticator
  const getUserIdFromTokenFn = jest.fn().mockImplementation(token => {
    if (token == "123456"){
      return Promise.resolve("User1234")
    }
    return Promise.reject(new Error("Error"))
  })
  const authenticator = jest.fn().mockImplementation(() => {
    return {
      getUserIdFromToken: getUserIdFromTokenFn
    }
  })

  it("given a proper authorization header with a user id, it should set the request header to the user id and call next()", done => {
    const mockedRequest = jest.fn().mockImplementation(() => ({
      header: (key) => key == "Authorization" ? "Bearer 123456" : "",
      headers: {}
    }))

    const request = mockedRequest()

    const next = (err) => {
      expect(err).toBeUndefined()
      expect(request.headers.user_id).toEqual("User1234")
      done()
    }

    const middleware = createAuthenticateMiddleware(authenticator())
    middleware(request, null, next)
  })

  it("given the authenticator throws an error, it should pass the error object to next()", done => {
    const mockedRequest = jest.fn().mockImplementation(() => ({
      header: (key) => key == "Authorization" ? "Bearer 123456" : "",
      headers: {}
    }))

    getUserIdFromTokenFn.mockImplementation(() => {
      return Promise.reject(new Error("Unknown auth error"))
    })

    const next = (err) => {
      expect(err).toEqual(new Error("Unknown auth error"))
      done()
    }

    const middleware = createAuthenticateMiddleware(authenticator())
    middleware(mockedRequest(), null, next)
  })

  it("given no authorization header, it should pass an error object to next()", done => {
    const mockedRequest = jest.fn().mockImplementation(() => ({
      header: () => {},
      headers: {}
    }))

    const next = (err) => {
      expect(err).toEqual(new HttpError(401, "InvalidAuthorization", "Missing Authorization header"))
      done()
    }

    const middleware = createAuthenticateMiddleware(authenticator())
    middleware(mockedRequest(), null, next)
  })

  it("given an auth header with Bearer prefix but no auth token, it should return an error and call next()", done => {
    const mockedRequest = jest.fn().mockImplementation(() => ({
      header: (key) => key == "Authorization" ? "Bearer " : "",
      headers: {}
    }))

    const next = (err) => {
      expect(err).toEqual(new HttpError(401, "InvalidAuthorization", "Malformed Authorization header"))
      done()
    }

    const middleware = createAuthenticateMiddleware(authenticator())
    middleware(mockedRequest(), null, next)
  })

  it("given an auth header without Bearer prefix, it should return an error and call next()", done => {
    const mockedRequest = jest.fn().mockImplementation(() => ({
      header: (key) => key == "Authorization" ? "Auth 123453243" : "",
      headers: {}
    }))

    const next = (err) => {
      expect(err).toEqual(new HttpError(401, "InvalidAuthorization", "Invalid Authorization header prefix"))
      done()
    }

    const middleware = createAuthenticateMiddleware(authenticator())
    middleware(mockedRequest(), null, next)
  })
})