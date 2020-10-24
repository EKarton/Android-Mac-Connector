package main

import (
	"encoding/json"
	"log"
	"net/http"

	"github.com/gorilla/mux"

	jobsdb "Android-Mac-Connector-Server/src/db/jobs"
	smsdb "Android-Mac-Connector-Server/src/db/sms"
)

type NewSmsMessageReceived struct {
	Address   string `json:"address"`
	Body      string `json:"body"`
	Timestamp int32  `json:"timestamp"`
}

type NewSmsMessageReceived2xxResponse struct {
	Status string `json:"status"`
}

type SendSmsMessageRequest struct {
	Address string `json:"address"`
	Body    string `json:"body"`
}

type SendSmsMessage2xxResponse struct {
	Status string `json:"status"`
	JobId  string `json:"jobId"`
}

type SendSmsJob struct {
	Status string `json:"status"`
}

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

// Logic for when a new message has been received
func notifyNewSmsMessageReceived(responseWriter http.ResponseWriter, request *http.Request) {

	// Get the contents from the request
	var variables = mux.Vars(request)
	var newSmsMessage NewSmsMessageReceived
	json.NewDecoder(request.Body).Decode(&newSmsMessage)

	log.Println("Received new sms message from", variables["deviceId"], "with payload", newSmsMessage)

	// TODO: Do something with the payload (store in database, etc)

	// Write response
	responseWriter.Header().Set("Content-Type", "application/json")
	json.NewEncoder(responseWriter).Encode(NewSmsMessageReceived2xxResponse{"success"})
}

// Logic for when sending SMS message
func sendSms(responseWriter http.ResponseWriter, request *http.Request) {
	var variables = mux.Vars(request)
	var sendSmsMessageRequest SendSmsMessageRequest
	json.NewDecoder(request.Body).Decode(&sendSmsMessageRequest)

	log.Println("Received send sms request for", variables["deviceId"], "with payload", sendSmsMessageRequest)

	// Set response headers
	responseWriter.Header().Set("Content-Type", "application/json")

	uuid := jobsdb.AddJob(SendSmsJob{"pending"})

	// Write response body
	var responseBody = SendSmsMessage2xxResponse{
		"success",
		uuid,
	}
	json.NewEncoder(responseWriter).Encode(responseBody)
}

func getSendSmsJobStatus(responseWriter http.ResponseWriter, request *http.Request) {
	var variables = mux.Vars(request)
	var deviceId = variables["deviceId"]
	var jobUuid = variables["uuid"]
	var sendSmsMessageRequest SendSmsMessageRequest
	json.NewDecoder(request.Body).Decode(&sendSmsMessageRequest)

	log.Println("Received get sms job status for", deviceId, "with payload", sendSmsMessageRequest)

	var job = jobsdb.GetJobStatus(jobUuid)

	// Write response
	responseWriter.Header().Set("Content-Type", "application/json")
	json.NewEncoder(responseWriter).Encode(job)
}

// Logic for getting SMS threads
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

// Logic for getting SMS messages for a particular thread
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

func main() {

	// Create our router
	var router = mux.NewRouter()

	// Add paths for the job queue

	// Add paths for when new SMS message is received
	router.HandleFunc("/api/v1/{deviceId}/sms/messages/new", notifyNewSmsMessageReceived).Methods("POST")

	// Add paths for when to send SMS message
	router.HandleFunc("/api/v1/{deviceId}/sms/messages", sendSms).Methods("POST")
	router.HandleFunc("/api/v1/{deviceId}/sms/messages/{uuid}/status", getSendSmsJobStatus).Methods("GET")

	// Add paths for when to get SMS threads
	router.HandleFunc("/api/v1/{deviceId}/sms/threads", getSmsThreads).Methods("POST")
	router.HandleFunc("/api/v1/{deviceId}/sms/threads", updateSmsThreads).Methods("PUT")

	// Add paths for when to get SMS messages of a particular thread
	router.HandleFunc("/api/v1/{deviceId}/sms/threads/{threadId}/messages", getSmsMessagesForThread).Methods("GET")
	router.HandleFunc("/api/v1/{deviceId}/sms/threads/{threadId}/messages", updateSmsMessagesForThread).Methods("PUT")

	// Start the server
	log.Fatal(http.ListenAndServe(":8080", router))
}
