package middlewares

import (
	"net/http"
)

func VerifyAuthorization(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// TODO: Check if the user has access to it

		// Go to the next middleware
		next.ServeHTTP(w, r)
	})
}
