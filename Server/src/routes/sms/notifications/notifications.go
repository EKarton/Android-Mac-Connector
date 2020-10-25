package notifications

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"

	"github.com/gorilla/mux"

	sms_notifications "Android-Mac-Connector-Server/src/db/sms/notifications"
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
	var newSmsMsg = sms_notifications.SmsMessage{
		ContactInfo: newSmsMessage.Address,
		Data:        newSmsMessage.Body,
		Timestamp:   newSmsMessage.Timestamp,
	}
	sms_notifications.AddNewSmsMessageNotification(newSmsMsg)

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
	notifications := sms_notifications.GetNotificationsFromUuid(startingUuid, int(numNotifications))

	if isLongPolling && len(notifications) == 0 {
		log.Println("Subscribing to new sms notifications from", deviceId)

		subscriptionChannel := make(chan []sms_notifications.SmsMessageNotification)
		subscriptionUuid := sms_notifications.SubscribeToNewNotifications(subscriptionChannel)

		select {
		case notification := <-subscriptionChannel:
			log.Println("Received notification", notification)

			// Close the subscription
			close(subscriptionChannel)
			sms_notifications.UnsubscribeToNewNotifications(subscriptionUuid)

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
 * Initializes the router to include paths and path handlers
 */
func InitializeRouter(router *mux.Router) {
	router.HandleFunc("", notifyNewSmsMessageReceived).Methods("POST")
	router.HandleFunc("", getNewSmsMessagesReceived).Methods("GET")
}
