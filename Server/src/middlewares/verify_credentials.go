package middlewares

import (
	"Android-Mac-Connector-Server/src/application"
	"net/http"
	"os"
)

/**
 * Creates middleware that verifies if the auth tokens are valid
 * If they are valid, it will attach the user ID to the request headers
 */
func VerifyCredentials(appContext *application.ApplicationContext) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {

			// If the environment variable has specified to skip validating credentials
			if os.Getenv("VERIFY_CREDENTIALS") == "false" {
				r.Header.Add("user_id", "1234")
				next.ServeHTTP(w, r)
				return
			}

			authHeaderValue := r.Header.Get("Authorization")

			if authHeaderValue == "" {
				http.Error(w, "Missing Authorization header", http.StatusBadRequest)
				return
			}

			if len(authHeaderValue) < 7 {
				http.Error(w, "Malformed Authorization header", http.StatusBadRequest)
				return
			}

			if authHeaderValue[0:6] != "Bearer" {
				http.Error(w, "Invalid Authorization header prefix", http.StatusBadRequest)
				return
			}

			accessToken := authHeaderValue[7:]
			isValid, uid, err := appContext.Services.AuthService.VerifyAccessToken(accessToken)

			if err != nil {
				http.Error(w, "Internal Server Error", http.StatusBadGateway)
				return
			}

			if !isValid {
				http.Error(w, "Invalid authorization token", http.StatusUnauthorized)
				return
			}

			r.Header.Add("user_id", uid)

			// Go to the next middleware
			next.ServeHTTP(w, r)
		})
	}
}
