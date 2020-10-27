package middlewares

import (
	logger "log"
	"net/http"
)

func log(h http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		logger.Println("Before")
		h.ServeHTTP(w, r) // call original
		logger.Println("After")
	})
}
