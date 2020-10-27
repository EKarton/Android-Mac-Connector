package messages

import (
	"encoding/json"
	"net/http"

	"github.com/gorilla/mux"

	"Android-Mac-Connector-Server/src/data/fcm"
	"Android-Mac-Connector-Server/src/store"
	"Android-Mac-Connector-Server/src/store/sms/messages"
)

type GetSmsThreads2xxResponse struct {
	SmsThreads  []messages.SmsThread `json:"sms_threads"`
	LastUpdated int                  `json:"last_updated"`
}

type UpdateSmsThreads2xxResponse struct {
	Status string `json:"status"`
}

type GetSmsMessagesForThread2xxResponse struct {
	SmsMessages []messages.SmsMessage `json:"sms_messages"`
	LastUpdated int                   `json:"last_updated"`
}

type GetSmsMessagesForThread4xxResponse struct {
	Error string `json:"error"`
}

type UpdateSmsMessagesForThread2xxResponse struct {
	Status string `json:"status"`
}

/**
 * Returns the sms threads of a particular device
 */
func getSmsThreads(dataStore *store.Datastore) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		variables := mux.Vars(r)
		deviceId := variables["deviceId"]

		// Ask the Android device to update the sms thread
		token, err := dataStore.DevicesStores.GetPushNotificationToken(deviceId)
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			return
		}

		data := map[string]string{
			"action": "update_sms_threads",
		}
		fcm.SendFcmMessage(token, data, nil)

		// Get the existing sms threads
		smsThreads, err := dataStore.SmsMessagesStore.GetSmsThreads(deviceId)
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			return
		}

		// Write response
		w.Header().Set("Content-Type", "application/json")
		responseBody := GetSmsThreads2xxResponse{
			SmsThreads: smsThreads,
		}
		json.NewEncoder(w).Encode(responseBody)
	}
}

/**
 * Update a sms thread stored on a particular device
 */
func updateSmsThread(dataStore *store.Datastore) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		variables := mux.Vars(r)
		deviceId := variables["deviceId"]
		threadId := variables["threadId"]

		var smsThread messages.SmsThread
		json.NewDecoder(r.Body).Decode(&smsThread)

		if err := dataStore.SmsMessagesStore.UpdateSmsThread(deviceId, threadId, smsThread); err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			return
		}
	}
}

/**
 * Returns a list of messages of a sms thread of a particular device
 */
func getSmsMessagesForThread(dataStore *store.Datastore) http.HandlerFunc {
	return func(responseWriter http.ResponseWriter, request *http.Request) {
		var variables = mux.Vars(request)
		deviceId := variables["deviceId"]
		threadId := variables["threadId"]

		smsMessages, err := dataStore.SmsMessagesStore.GetSmsMessagesOfThread(deviceId, threadId)
		if err != nil {
			responseWriter.WriteHeader(http.StatusInternalServerError)
			return
		}

		if err != nil {
			responseWriter.Header().Set("Content-Type", "application/json")
			json.NewEncoder(responseWriter).Encode(GetSmsMessagesForThread4xxResponse{
				Error: err.Error(),
			})
			return
		}

		// Write response header
		responseWriter.Header().Set("Content-Type", "application/json")

		// Write response body
		responseBody := GetSmsMessagesForThread2xxResponse{
			SmsMessages: smsMessages,
			LastUpdated: 0,
		}
		json.NewEncoder(responseWriter).Encode(responseBody)
	}
}

/**
 * Updates the messages of a sms thread of a particular device
 */
func updateSmsMessagesForThread(dataStore *store.Datastore) http.HandlerFunc {
	return func(responseWriter http.ResponseWriter, request *http.Request) {
		variables := mux.Vars(request)
		deviceId := variables["deviceId"]
		threadId := variables["threadId"]

		var smsMessages []messages.SmsMessage
		json.NewDecoder(request.Body).Decode(&smsMessages)

		if err := dataStore.SmsMessagesStore.UpdateMessagesForSmsThread(deviceId, threadId, smsMessages); err != nil {
			responseWriter.WriteHeader(http.StatusInternalServerError)
		}
	}
}

/**
 * Initializes the router to include paths and path handlers
 */
func InitializeRouter(dataStore *store.Datastore, router *mux.Router) {
	// Add paths for when to get SMS threads
	router.HandleFunc("", getSmsThreads(dataStore)).Methods("POST")
	router.HandleFunc("/{threadId}", updateSmsThread(dataStore)).Methods("PUT")

	// Add paths for when to get SMS messages of a particular thread
	router.HandleFunc("/{threadId}/messages", getSmsMessagesForThread(dataStore)).Methods("GET")
	router.HandleFunc("/{threadId}/messages", updateSmsMessagesForThread(dataStore)).Methods("PUT")
}
