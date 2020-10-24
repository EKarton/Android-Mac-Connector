package threads

import (
	"encoding/json"
	"log"
	"net/http"

	"github.com/gorilla/mux"

	smsdb "Android-Mac-Connector-Server/src/db/sms"
)

type UpdateSmsThreads2xxResponse struct {
	Status string `json:"status"`
}

type GetSmsMessagesForThread2xxResponse struct {
	SmsMessages []smsdb.SmsMessage `json:"sms_messages"`
	LastUpdated int                `json:"last_updated"`
}

type UpdateSmsMessagesForThread2xxResponse struct {
	Status string `json:"status"`
}

/**
 * Returns the sms threads of a particular device
 */
func getSmsThreads(responseWriter http.ResponseWriter, request *http.Request) {
	var variables = mux.Vars(request)
	var deviceId = variables["deviceId"]

	log.Println("Received get sms threads for", deviceId)

	// TODO: Make an FCM request for Android device to update the sms thread

	// Get the existing sms threads
	var smsThreads = smsdb.GetSmsThreads()

	// Write response
	responseWriter.Header().Set("Content-Type", "application/json")
	json.NewEncoder(responseWriter).Encode(smsThreads)
}

/**
 * Updates the sms threads stored on a particular device
 */
func updateSmsThreads(responseWriter http.ResponseWriter, request *http.Request) {
	var variables = mux.Vars(request)
	var deviceId = variables["deviceId"]
	var smsThreads []smsdb.SmsThread
	json.NewDecoder(request.Body).Decode(&smsThreads)

	log.Println("Received update sms threads for", deviceId, "with payload", smsThreads)

	smsdb.UpdateSmsThreads(smsThreads)

	// Write response header
	responseWriter.Header().Set("Content-Type", "application/json")

	// Write response body
	var responseBody = UpdateSmsThreads2xxResponse{"success"}
	json.NewEncoder(responseWriter).Encode(responseBody)
}

/**
 * Returns the messages of a sms thread of a particular device
 */
func getSmsMessagesForThread(responseWriter http.ResponseWriter, request *http.Request) {
	var variables = mux.Vars(request)
	var deviceId = variables["deviceId"]
	var threadId = variables["threadId"]

	log.Println("Get sms msgs for thread", threadId, "from", deviceId)

	var smsMessages = smsdb.GetSmsMessagesForThread(threadId)

	// Write response header
	responseWriter.Header().Set("Content-Type", "application/json")

	// Write response body
	var responseBody = GetSmsMessagesForThread2xxResponse{
		smsMessages,
		1,
	}
	json.NewEncoder(responseWriter).Encode(responseBody)
}

/**
 * Updates the messages of a sms thread of a particular device
 */
func updateSmsMessagesForThread(responseWriter http.ResponseWriter, request *http.Request) {
	var variables = mux.Vars(request)
	var deviceId = variables["deviceId"]
	var threadId = variables["threadId"]
	var smsMessages []smsdb.SmsMessage
	json.NewDecoder(request.Body).Decode(&smsMessages)

	log.Println("Update sms msgs for thread", threadId, "from", deviceId, "with payload", smsMessages)

	smsdb.UpdateSmsMessagesForThread(threadId, smsMessages)

	// Write response header
	responseWriter.Header().Set("Content-Type", "application/json")
	json.NewEncoder(responseWriter).Encode(UpdateSmsMessagesForThread2xxResponse{"success"})
}

/**
 * Initializes the router to include paths and path handlers
 */
func InitializeRouter(router *mux.Router) {
	// Add paths for when to get SMS threads
	router.HandleFunc("", getSmsThreads).Methods("POST")
	router.HandleFunc("", updateSmsThreads).Methods("PUT")

	// Add paths for when to get SMS messages of a particular thread
	router.HandleFunc("/{threadId}/messages", getSmsMessagesForThread).Methods("GET")
	router.HandleFunc("/{threadId}/messages", updateSmsMessagesForThread).Methods("PUT")
}
