import { Authenticator } from "../services/authenticator";

export class HttpError extends Error {
  public readonly statusCode: number;
  public readonly errorCode: string;

  constructor(statusCode: number, errorCode: string, reason: string) {
    super(reason);

    this.statusCode = statusCode;
    this.errorCode = errorCode;
  }
}

export function logRequestsMiddleware(req, res, next) {
  console.log(`${req.method} ${req.url}: ${res.statusCode}`);
  next();
}

export function handleErrorsMiddleware(err: Error, res) {
  if (err instanceof HttpError) {
    const httpError = <HttpError> err;
    res.status(httpError.statusCode).json({
      "error_code": httpError.errorCode,
      "reason": httpError.message
    });

  } else {
    res.status(500).json({
      "error_code": "UnknownError",
      "reason": err.message
    });
  }
}

export function createAuthenticateMiddleware(authService: Authenticator) {
  return async (req, res, next) => {
    try {
      const authHeaderValue = req.header("Authorization")

      if (!authHeaderValue) {
        throw new HttpError(401, "InvalidAuthorization", "Missing Authorization header")
      }

      if (authHeaderValue.length < 7) {
        throw new HttpError(401, "InvalidAuthorization", "Malformed Authorization header")
      }

      if (authHeaderValue.substring(0, 6) != "Bearer") {
        throw new HttpError(401, "InvalidAuthorization", "Invalid Authorization header prefix")
      }

      const accessToken = authHeaderValue.substring(7)
      const userId = await authService.getUserIdFromToken(accessToken)
      req.headers.user_id = userId
      next()

    } catch (error) {
      next(new HttpError(401, "InvalidAuthorization", error.message))
    }
  }
}