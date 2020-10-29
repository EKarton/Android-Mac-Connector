package main

import (
	"Android-Mac-Connector-Server/src/jobs"
	"Android-Mac-Connector-Server/src/middlewares"
	"Android-Mac-Connector-Server/src/routes/devices"
	"Android-Mac-Connector-Server/src/routes/sms"
	"Android-Mac-Connector-Server/src/store"
	"log"
	"net/http"
	"time"

	"github.com/gorilla/mux"
)

func main() {

	// // Initialize default app
	// app, err := firebase.NewApp(context.Background(), nil)
	// if err != nil {
	// 	log.Fatalf("error initializing app: %v\n", err)
	// }

	// // Access auth service from the default app
	// client, err := app.Auth(context.Background())
	// if err != nil {
	// 	log.Fatalf("error getting Auth client: %v\n", err)
	// }

	// _, err = client.VerifyIDTokenAndCheckRevoked(context.Background(), "wasdf")
	// fmt.Println(err)

	// Create our data store
	dataStore := store.CreateInMemoryDatastore()

	// Create our router
	router := mux.NewRouter()

	router.Use(middlewares.HandleErrors)

	// Add subrouters
	smsRouter := router.PathPrefix("/api/v1/{deviceId}/sms").Subrouter()
	jobsRouter := router.PathPrefix("/api/v1/{deviceId}/jobs").Subrouter()
	devicesRouter := router.PathPrefix("/api/v1/devices").Subrouter()

	// Add paths to each subrouter
	sms.InitializeRouter(dataStore, smsRouter)
	jobs.InitializeRouter(dataStore, jobsRouter)
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
