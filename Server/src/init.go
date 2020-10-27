package main

import (
	"log"
	"net/http"
	"time"

	devices "Android-Mac-Connector-Server/src/routes/devices"
	sms "Android-Mac-Connector-Server/src/routes/sms"
	"Android-Mac-Connector-Server/src/store"

	"github.com/gorilla/mux"
)

func main() {

	// Create our data store
	dataStore := store.CreateInMemoryDatastore()

	// Create our router
	router := mux.NewRouter()

	// Add subrouters
	smsRouter := router.PathPrefix("/api/v1/{deviceId}/sms").Subrouter()
	devicesRouter := router.PathPrefix("/api/v1/devices").Subrouter()

	// Add paths to each subrouter
	sms.InitializeRouter(dataStore, smsRouter)
	devices.InitializeRouter(dataStore, devicesRouter)

	// Create and start our server
	server := &http.Server{
		Addr:              ":8080",
		Handler:           router,
		ReadTimeout:       5 * time.Second,
		ReadHeaderTimeout: 5 * time.Second,
		WriteTimeout:      5 * time.Second,
		IdleTimeout:       90 * time.Second,
	}
	log.Printf("Starting server at port %v", 8080)
	log.Fatal(server.ListenAndServe())
}
