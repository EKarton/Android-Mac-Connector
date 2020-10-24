package main

import (
	"log"
	"net/http"

	smsRoute "Android-Mac-Connector-Server/src/routes/sms"

	"github.com/gorilla/mux"
)

func main() {

	// Create our router
	var router = mux.NewRouter()

	// Add subrouters
	var smsRouter = router.PathPrefix("/api/v1/{deviceId}/sms").Subrouter()
	smsRoute.InitializeRouter(smsRouter)

	// Start the server
	log.Printf("Server started at port %v", 8080)
	log.Fatal(http.ListenAndServe(":8080", router))
}
