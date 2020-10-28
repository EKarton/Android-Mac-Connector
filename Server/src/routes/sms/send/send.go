package send

import (
	"encoding/json"
	"log"
	"net/http"

	"github.com/gorilla/mux"

	fcm "Android-Mac-Connector-Server/src/data/fcm"
	"Android-Mac-Connector-Server/src/store"
)

type SendSmsMessageRequest struct {
	PhoneNumber string `json:"phone_number"`
	Body        string `json:"body"`
}

type SendSmsMessage2xxResponse struct {
	JobId string `json:"job_id"`
}

type SendSmsJob struct {
	JobStatus string `json:"job_status"`
}

type GetSendSmsJobStatus struct {
	JobStatus string `json:"job_status"`
}

type UpdateSendSmsJobStatus2xxResponse struct {
	Status string `json:"status"`
}

// Handles when a device wants to send an SMS message from another device
// It submits a job to the jobs queue, where its status can be found by polling the job queue
//
// The request must contain a JSON body in this form:
// {
// 	"phone_number": "<a phone number>",
// 	"body": "<a text body>"
// }
//
// The response will return a JSON body in this form:
// {
// 	"job_id": "<a job id>"
// }
//
func addSendSmsJob(dataStore *store.Datastore) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		variables := mux.Vars(r)
		deviceId := variables["deviceId"]
		jsonBody := SendSmsMessageRequest{}
		json.NewDecoder(r.Body).Decode(&jsonBody)

		log.Println("Received send sms request for", deviceId, "with payload", jsonBody)

		// Set response headers
		w.Header().Set("Content-Type", "application/json")

		// Keep track of the job
		uuid, err := dataStore.JobStatusStore.AddJob("pending")

		if err != nil {
			panic(err)
		}

		// Get the Push notification token
		token, err := dataStore.DevicesStores.GetPushNotificationToken(deviceId)

		if err != nil {
			panic(err)
		}

		// Send the sms
		data := map[string]string{
			"action":       "send_sms",
			"uuid":         uuid,
			"phone_number": jsonBody.PhoneNumber,
			"body":         jsonBody.Body,
		}
		if err := fcm.SendFcmMessage(token, data, nil); err != nil {
			panic(err)
		}

		// Write response body
		responseBody := SendSmsMessage2xxResponse{
			JobId: uuid,
		}
		json.NewEncoder(w).Encode(responseBody)
	}
}

// Returns the status of an SMS job
//
// If successful, it outputs the data in json form:
// {
// 	"job_status": "<job status>"
// }
//
func getSendSmsJobStatus(dataStore *store.Datastore) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		variables := mux.Vars(r)
		deviceId := variables["deviceId"]
		jobUuid := variables["uuid"]
		sendSmsMessageRequest := SendSmsMessageRequest{}
		json.NewDecoder(r.Body).Decode(&sendSmsMessageRequest)

		log.Println("Received get sms job status for", deviceId, "with payload", sendSmsMessageRequest)

		jobStatus, err := dataStore.JobStatusStore.GetJobStatus(jobUuid)

		if err != nil {
			panic(err)
		}

		// Write response
		w.Header().Set("Content-Type", "application/json")
		responseBody := GetSendSmsJobStatus{
			JobStatus: jobStatus,
		}
		json.NewEncoder(w).Encode(responseBody)
	}
}

// Updates the status of an SMS job
//
// The request must contain a json body in this form:
// {
// 	"job_status": "<job status>"
// }
//
func updateSendSmsJobStatus(dataStore *store.Datastore) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		// Get request path variables
		variables := mux.Vars(r)
		deviceId := variables["deviceId"]
		jobUuid := variables["uuid"]

		var jsonBody SendSmsJob
		json.NewDecoder(r.Body).Decode(&jsonBody)

		log.Println("Update send sms job", jobUuid, "from", deviceId, "with payload", jsonBody.JobStatus)

		err := dataStore.JobStatusStore.UpdateJobStatus(jobUuid, jsonBody.JobStatus)
		if err != nil {
			panic(err)
		}
	}
}

// Initializes the router for when the device wants to send SMS message
func InitializeRouter(dataStore *store.Datastore, router *mux.Router) {
	router.HandleFunc("", addSendSmsJob(dataStore)).Methods("POST")
	router.HandleFunc("/{uuid}/status", getSendSmsJobStatus(dataStore)).Methods("GET")
	router.HandleFunc("/{uuid}/status", updateSendSmsJobStatus(dataStore)).Methods("PUT")
}
