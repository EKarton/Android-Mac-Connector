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
	JobStatus string `json:"job_status"`
}

type UpdateSendSmsJobStatus2xxResponse struct {
	Status string `json:"status"`
}

/**
 * Handler for when the device wants to notify all subscribers that a new SMS message has been received
 */
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

/**
 * Handler for when a device wants to send an SMS message from another device
 * It submits a job to the jobs queue, where its status can be found by polling the job queue
 */
func addSendSmsJob(responseWriter http.ResponseWriter, request *http.Request) {
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

/**
 * Returns the status of the SMS job
 */
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

/**
 * Updates the status of a SMS job
 */
func updateSendSmsJobStatus(responseWriter http.ResponseWriter, request *http.Request) {
	// Get request path variables
	var variables = mux.Vars(request)
	var deviceId = variables["deviceId"]
	var jobUuid = variables["uuid"]
	var newSendSmsJobStatus SendSmsJob
	json.NewDecoder(request.Body).Decode(&newSendSmsJobStatus)

	log.Println("Update send sms job", jobUuid, "from", deviceId, "with payload", newSendSmsJobStatus)

	jobsdb.UpdateJobStatus(jobUuid, newSendSmsJobStatus)

	// Set response headers
	responseWriter.Header().Set("Content-Type", "application/json")

	// Write response body
	var responseBody = UpdateSendSmsJobStatus2xxResponse{
		"success",
	}
	json.NewEncoder(responseWriter).Encode(responseBody)
}

func InitializeRouter(router *mux.Router) {
	// Add paths for when new SMS message is received
	router.HandleFunc("/new", notifyNewSmsMessageReceived).Methods("POST")

	// Add paths for when to send SMS message
	router.HandleFunc("", addSendSmsJob).Methods("POST")
	router.HandleFunc("/{uuid}/status", getSendSmsJobStatus).Methods("GET")
	router.HandleFunc("/{uuid}/status", updateSendSmsJobStatus).Methods("POST")
}
