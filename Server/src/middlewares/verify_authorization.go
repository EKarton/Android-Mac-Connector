package middlewares

import (
	"Android-Mac-Connector-Server/src/application"
	"net/http"
)

func VerifyAuthorization(appContext *application.ApplicationContext) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			// TODO: Check if the user has access to it

			// Go to the next middleware
			next.ServeHTTP(w, r)
		})
	}
}
