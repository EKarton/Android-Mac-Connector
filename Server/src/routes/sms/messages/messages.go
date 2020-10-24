package messages

import (
	"encoding/json"
	"log"
	"net/http"

	"github.com/gorilla/mux"

	jobsdb "Android-Mac-Connector-Server/src/db/jobs"
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

func InitializeRouter(router *mux.Router) {
	// Add paths for when to send SMS message
	router.HandleFunc("", sendSms).Methods("POST")
	router.HandleFunc("/{uuid}/status", getSendSmsJobStatus).Methods("GET")

	// Add paths for when new SMS message is received
	router.HandleFunc("/new", notifyNewSmsMessageReceived).Methods("POST")
}
