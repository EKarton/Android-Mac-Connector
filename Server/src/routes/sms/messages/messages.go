package messages

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"

	"github.com/gorilla/mux"

	fcm "Android-Mac-Connector-Server/src/data/fcm"
	jobsdb "Android-Mac-Connector-Server/src/db/jobs"
	smsdb "Android-Mac-Connector-Server/src/db/sms"
	newsmsqueue "Android-Mac-Connector-Server/src/db/sms/newsmsqueue"
)

type NewSmsMessageReceived struct {
	Address   string `json:"address"`
	Body      string `json:"body"`
	Timestamp int    `json:"timestamp"`
}

type NewSmsMessageReceived2xxResponse struct {
	Status string `json:"status"`
}

type GetNewSmsMessagesReceivedErrorResponse struct {
	Status string `json:"status"`
	Reason string `json:"reason"`
}

type SendSmsMessageRequest struct {
	PhoneNumber string `json:"phone_number"`
	Body        string `json:"body"`
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

	// Store new msg in db
	var newSmsMsg = newsmsqueue.SmsMessageNotification{
		ContactInfo: newSmsMessage.Address,
		Data:        newSmsMessage.Body,
		Timestamp:   newSmsMessage.Timestamp,
	}
	smsdb.AddNewSmsMessageNotification(newSmsMsg)

	// Write response
	responseWriter.Header().Set("Content-Type", "application/json")
	json.NewEncoder(responseWriter).Encode(NewSmsMessageReceived2xxResponse{"success"})
}

func getNewSmsMessagesReceived(responseWriter http.ResponseWriter, request *http.Request) {
	// Get the contents from the request
	variables := mux.Vars(request)
	deviceId := variables["deviceId"]
	startingUuid := request.URL.Query().Get("starting_uuid")
	numNotifications, numNotificationsErr := strconv.ParseInt(request.URL.Query().Get("fetch_count"), 10, 0)
	isLongPolling, isLongPollingErr := strconv.ParseBool(request.URL.Query().Get("long_polling"))

	if isLongPollingErr != nil {
		responseWriter.Header().Set("Content-Type", "application/json")
		responseWriter.WriteHeader(400)
		json.NewEncoder(responseWriter).Encode(GetNewSmsMessagesReceivedErrorResponse{
			Status: "failure",
			Reason: "LongPollingQueryParamParseFailure",
		})
		return
	}

	if numNotificationsErr != nil {
		responseWriter.Header().Set("Content-Type", "application/json")
		responseWriter.WriteHeader(400)
		json.NewEncoder(responseWriter).Encode(GetNewSmsMessagesReceivedErrorResponse{
			Status: "failure",
			Reason: "FetchCountQueryParamParseFailure",
		})
		return
	}

	// Get the notifications starting from the Uuid
	notifications := smsdb.GetNotificationsFromUuid(startingUuid, int(numNotifications))

	if isLongPolling && len(notifications) == 0 {
		log.Println("Subscribing to new sms notifications from", deviceId)

		subscriptionChannel := make(chan []smsdb.SmsMessageNotification)
		subscriptionUuid := smsdb.SubscribeToNewNotifications(subscriptionChannel)

		select {
		case notification := <-subscriptionChannel:
			log.Println("Received notification", notification)

			// Close the subscription
			close(subscriptionChannel)
			smsdb.UnsubscribeToNewNotifications(subscriptionUuid)

			// Send the output to the user
			responseWriter.Header().Set("Content-Type", "application/json")
			json.NewEncoder(responseWriter).Encode(notification)
		}

	} else {

		log.Println("Getting", numNotifications, "sms notifications from", deviceId, "starting from", startingUuid)

		// Write response
		responseWriter.Header().Set("Content-Type", "application/json")
		json.NewEncoder(responseWriter).Encode(notifications)
	}
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

	// Keep track of the job
	uuid := jobsdb.AddJob(SendSmsJob{"pending"})

	// Perform SMS
	token := "cFAS88fZTgmw37RtNze_kq:APA91bHTfUd2X4CQa1_S0dwRmp9WeIfDlgTsW4GnIwR1Hr1OkQ_wWFnUi_CFn6GiAs2_2RIoUnD-8JMrOtrUggn7ktwqa2vTD7prS8IfJKIKXjeIpWBnup2NZ8M7EAP9J5rxxu4YLHQx"
	data := map[string]string{
		"action":       "send_sms",
		"uuid":         uuid,
		"phone_number": sendSmsMessageRequest.PhoneNumber,
		"body":         sendSmsMessageRequest.Body,
	}
	fcm.SendFcmMessage(token, data, nil)

	// Write response body
	var responseBody = SendSmsMessage2xxResponse{
		Status: "success",
		JobId:  uuid,
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
		Status: "success",
	}
	json.NewEncoder(responseWriter).Encode(responseBody)
}

/**
 * Initializes the router to include paths and path handlers
 */
func InitializeRouter(router *mux.Router) {
	// Add paths for when new SMS message is received
	router.HandleFunc("/new", notifyNewSmsMessageReceived).Methods("POST")
	router.HandleFunc("/new", getNewSmsMessagesReceived).Methods("GET")

	// Add paths for when to send SMS message
	router.HandleFunc("", addSendSmsJob).Methods("POST")
	router.HandleFunc("/{uuid}/status", getSendSmsJobStatus).Methods("GET")
	router.HandleFunc("/{uuid}/status", updateSendSmsJobStatus).Methods("PUT")
}
