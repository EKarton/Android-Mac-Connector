package main

import (
	"Android-Mac-Connector-Server/src/app_context"
	"Android-Mac-Connector-Server/src/middlewares"
	"Android-Mac-Connector-Server/src/routes/devices"
	"Android-Mac-Connector-Server/src/routes/jobs"
	"Android-Mac-Connector-Server/src/routes/sms_notifications"
	"log"
	"net/http"
	"time"

	"github.com/gorilla/mux"
)

func main() {
	// Create our application context
	appContext := app_context.CreateApplicationContext()

	// Create our router
	router := mux.NewRouter()

	router.Use(middlewares.HandleErrors(appContext))

	// Add subrouters
	smsRouter := router.PathPrefix("/api/v1/{deviceId}/sms").Subrouter()
	jobsRouter := router.PathPrefix("/api/v1/{deviceId}/jobs").Subrouter()
	devicesRouter := router.PathPrefix("/api/v1/devices").Subrouter()

	// Add paths to each subrouter
	sms_notifications.InitializeRouter(appContext, smsRouter)
	jobs.InitializeRouter(appContext, jobsRouter)
	devices.InitializeRouter(appContext, devicesRouter)

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
