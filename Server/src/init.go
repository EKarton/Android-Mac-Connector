package main

import (
	"log"
	"net/http"
	"time"

	devicesRoute "Android-Mac-Connector-Server/src/routes/devices"
	smsRoute "Android-Mac-Connector-Server/src/routes/sms"

	"github.com/gorilla/mux"
)

func main() {

	// Create our router
	var router = mux.NewRouter()

	// Add subrouters
	var smsRouter = router.PathPrefix("/api/v1/{deviceId}/sms").Subrouter()
	smsRoute.InitializeRouter(smsRouter)

	var devicesRouter = router.PathPrefix("/api/v1/devices").Subrouter()
	devicesRoute.InitializeRouter(devicesRouter)

	server := &http.Server{
		Addr:              ":8080",
		Handler:           router,
		ReadTimeout:       5 * time.Second,
		ReadHeaderTimeout: 5 * time.Second,
		WriteTimeout:      5 * time.Second,
		IdleTimeout:       90 * time.Second,
	}

	// Start the server
	log.Printf("Starting server at port %v", 8080)
	log.Fatal(server.ListenAndServe())
}
