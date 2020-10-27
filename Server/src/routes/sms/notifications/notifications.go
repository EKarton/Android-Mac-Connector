package notifications

import (
	"net/http"

	"github.com/gorilla/mux"

	"Android-Mac-Connector-Server/src/store/sms/notifications"
)

var smsNotificationsStore notifications.SmsNotificationsStore = notifications.CreateInMemoryStore()

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

	// // Get the contents from the request
	// var variables = mux.Vars(request)
	// var newSmsMessage NewSmsMessageReceived
	// json.NewDecoder(request.Body).Decode(&newSmsMessage)

	// log.Println("Received new sms message from", variables["deviceId"], "with payload", newSmsMessage)

	// // Store new msg in db
	// var newSmsMsg = notifications.SmsNotification{
	// 	ContactInfo: newSmsMessage.Address,
	// 	Data:        newSmsMessage.Body,
	// 	Timestamp:   newSmsMessage.Timestamp,
	// }

	// if err := smsNotificationsStore.AddSmsNotification(newSmsMsg); err != nil {
	// 	responseWriter.WriteHeader(http.StatusInternalServerError)
	// }

	// // Write response
	// responseWriter.Header().Set("Content-Type", "application/json")
	// json.NewEncoder(responseWriter).Encode(NewSmsMessageReceived2xxResponse{"success"})
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
func getNewSmsMessagesReceived(responseWriter http.ResponseWriter, request *http.Request) {
	// // Get the contents from the request
	// variables := mux.Vars(request)
	// deviceId := variables["deviceId"]
	// startingUuid := request.URL.Query().Get("starting_uuid")
	// numNotifications, numNotificationsErr := strconv.ParseInt(request.URL.Query().Get("fetch_count"), 10, 0)
	// isLongPolling, isLongPollingErr := strconv.ParseBool(request.URL.Query().Get("long_polling"))

	// if isLongPollingErr != nil {
	// 	responseWriter.Header().Set("Content-Type", "application/json")
	// 	responseWriter.WriteHeader(400)
	// 	json.NewEncoder(responseWriter).Encode(GetNewSmsMessagesReceivedErrorResponse{
	// 		Status: "failure",
	// 		Reason: "LongPollingQueryParamParseFailure",
	// 	})
	// 	return
	// }

	// if numNotificationsErr != nil {
	// 	responseWriter.Header().Set("Content-Type", "application/json")
	// 	responseWriter.WriteHeader(400)
	// 	json.NewEncoder(responseWriter).Encode(GetNewSmsMessagesReceivedErrorResponse{
	// 		Status: "failure",
	// 		Reason: "FetchCountQueryParamParseFailure",
	// 	})
	// 	return
	// }

	// log.Println("Getting at most", numNotifications, "sms notifications from", deviceId, "starting from", startingUuid)

	// Get the notifications starting from the Uuid
	// newNotifications, err := smsNotificationsStore.GetNewSmsNotificationsFromUuid(int(numNotifications), startingUuid)
	// if err != nil {
	// 	responseWriter.WriteHeader(http.StatusInternalServerError)
	// }

	// if isLongPolling && len(newNotifications) == 0 {
	// 	log.Println("Subscribing to new sms notifications from", deviceId)

	// 	subscriptionChannel := make(chan []notifications.SmsNotification)
	// 	subscriptionUuid := notifications.SubscribeToNewNotifications(subscriptionChannel)

	// 	select {
	// 	case notification := <-subscriptionChannel:
	// 		log.Println("Received notification", notification)

	// 		// Close the subscription
	// 		close(subscriptionChannel)
	// 		notifications.UnsubscribeToNewNotifications(subscriptionUuid)

	// 		// Send the output to the user
	// 		responseWriter.Header().Set("Content-Type", "application/json")
	// 		json.NewEncoder(responseWriter).Encode(notification)
	// 	}

	// } else {
	// 	// Write response
	// 	responseWriter.Header().Set("Content-Type", "application/json")
	// 	json.NewEncoder(responseWriter).Encode(notifications)
	// }
}

/**
 * Initializes the router to include paths and path handlers
 */
func InitializeRouter(router *mux.Router) {
	router.HandleFunc("", notifyNewSmsMessageReceived).Methods("POST")
	router.HandleFunc("", getNewSmsMessagesReceived).Methods("GET")
}
