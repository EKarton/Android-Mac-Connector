package middlewares

import (
	"Android-Mac-Connector-Server/src/application"
	logger "log"
	"net/http"
)

func Log(appContext *application.ApplicationContext) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			logger.Println("Before")
			next.ServeHTTP(w, r) // call original
			logger.Println("After")
		})
	}
}
