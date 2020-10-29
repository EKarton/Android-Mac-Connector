package sms_notifications

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"

	"github.com/gorilla/mux"

	"Android-Mac-Connector-Server/src/app_context"
	"Android-Mac-Connector-Server/src/store/sms_notifications"
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
	Reason string `json:"reason"`
}

/**
 * Handler for when the device wants to notify all subscribers that a new SMS message has been received
 */
func notifyNewSmsMessageReceived(appContext *app_context.ApplicationContext) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		variables := mux.Vars(r)
		deviceId := variables["deviceId"]

		var newSmsMessage NewSmsMessageReceived
		json.NewDecoder(r.Body).Decode(&newSmsMessage)

		// Store new msg in db
		var newSmsMsg = sms_notifications.SmsNotification{
			ContactInfo: newSmsMessage.Address,
			Data:        newSmsMessage.Body,
			Timestamp:   newSmsMessage.Timestamp,
		}

		if _, err := appContext.DataStores.SmsNotifications.AddSmsNotification(deviceId, newSmsMsg); err != nil {
			panic(err)
		}
	}
}

/**
 * Handles when a request wants to see which new messages were received by the device
 * It requires these query params present:
 *  1. starting_uuid (int): the starting UUID of a message. If not found, it will return the oldest SMS message
 *  2. fetch_count (int): the max number of notifications to fetch in one request
 *  3. long_polling (bool):
 *     - If true, and there are no new notifications, then it will hold the request until a new notification arrives from the device
 *     - Else, it returns an empty array
 */
func getNewSmsMessagesReceived(appContext *app_context.ApplicationContext) http.HandlerFunc {
	return func(responseWriter http.ResponseWriter, request *http.Request) {
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
				Reason: "LongPollingQueryParamParseFailure",
			})
			return
		}

		if numNotificationsErr != nil {
			responseWriter.Header().Set("Content-Type", "application/json")
			responseWriter.WriteHeader(400)
			json.NewEncoder(responseWriter).Encode(GetNewSmsMessagesReceivedErrorResponse{
				Reason: "FetchCountQueryParamParseFailure",
			})
			return
		}

		// Get the notifications starting from the Uuid
		pastUnreadNotifications, err := appContext.DataStores.SmsNotifications.GetNewSmsNotificationsFromUuid(deviceId, int(numNotifications), startingUuid)
		if err != nil {
			panic(err)
		}

		if isLongPolling && len(pastUnreadNotifications) == 0 {
			log.Println("Subscribing to new sms notifications from", deviceId)

			subscriber, err := appContext.DataStores.SmsNotificationSubscribers.CreateSubscriber(deviceId)

			if err != nil {
				panic(err)
			}

			// log.Println("Reading data")
			newNotification := <-subscriber.Channel
			appContext.DataStores.SmsNotificationSubscribers.RemoveSubscriber(subscriber)

			// Send the output to the user
			responseWriter.Header().Set("Content-Type", "application/json")
			json.NewEncoder(responseWriter).Encode(newNotification)

		} else {
			// Write response
			responseWriter.Header().Set("Content-Type", "application/json")
			json.NewEncoder(responseWriter).Encode(pastUnreadNotifications)
		}
	}
}

/**
 * Initializes the router to include paths and path handlers
 */
func InitializeRouter(appContext *app_context.ApplicationContext, router *mux.Router) {
	router.HandleFunc("", notifyNewSmsMessageReceived(appContext)).Methods("POST")
	router.HandleFunc("", getNewSmsMessagesReceived(appContext)).Methods("GET")
}
