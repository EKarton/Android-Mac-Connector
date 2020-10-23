package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"

	"github.com/gorilla/mux"
)

type SendSmsMessageRequest struct {
	Address string `json:"address"`
	Body    string `json:"body"`
}

// Logic for when a new message has been received
func notifyNewSmsMessageReceived(responseWriter http.ResponseWriter, request *http.Request) {
}

// Logic for when sending SMS message
func sendSms(responseWriter http.ResponseWriter, request *http.Request) {
	var sendSmsMessageRequest SendSmsMessageRequest
	json.NewDecoder(request.Body).Decode(&sendSmsMessageRequest)

	fmt.Println(sendSmsMessageRequest)
}

func getSendSmsJobStatus(responseWriter http.ResponseWriter, request *http.Request) {

}

// Logic for getting SMS threads
func getSmsThreads(responseWriter http.ResponseWriter, request *http.Request) {
}

func updateSmsThreads(responseWriter http.ResponseWriter, request *http.Request) {

}

// Logic for getting SMS messages for a particular thread
func getSmsMessagesForThread(responseWriter http.ResponseWriter, request *http.Request) {

}

func updateSmsMessagesForThread(responseWriter http.ResponseWriter, request *http.Request) {

}

func main() {

	// Create our router
	var router = mux.NewRouter()

	// Add paths
	router.HandleFunc("/api/v1/{deviceId}/sms/messages/new", notifyNewSmsMessageReceived).Methods("POST")
	router.HandleFunc("/api/v1/{deviceId}/sms/messages", sendSms).Methods("POST")
	router.HandleFunc("/api/v1/{deviceId}/sms/messages/{uuid}/status", getSendSmsJobStatus).Methods("GET")
	router.HandleFunc("/api/v1/{deviceId}/sms/threads", getSmsThreads).Methods("POST")
	router.HandleFunc("/api/v1/{deviceId}/sms/threads", updateSmsThreads).Methods("PUT")
	router.HandleFunc("/api/v1/{deviceId}/sms/threads/{threadId}/messages", getSmsMessagesForThread).Methods("GET")
	router.HandleFunc("/api/v1/{deviceId}/sms/threads/{threadId}/messages", updateSmsMessagesForThread).Methods("PUT")

	// Start the server
	log.Fatal(http.ListenAndServe(":8080", router))
}
