package middlewares

import (
	"Android-Mac-Connector-Server/src/app_context"
	"log"
	"net/http"
)

func LogRequests(appContext *app_context.ApplicationContext) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			log.Println("Incoming request:", r.URL, r.Body, r.Header)
			next.ServeHTTP(w, r)
		})
	}
}
