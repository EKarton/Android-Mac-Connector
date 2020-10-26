package threads

import (
	"encoding/json"
	"log"
	"net/http"

	"github.com/gorilla/mux"

	fcm "Android-Mac-Connector-Server/src/data/fcm"
	sms_threads "Android-Mac-Connector-Server/src/store/sms/threads"
)

type GetSmsThreads2xxResponse struct {
	SmsThreads  []sms_threads.SmsThread `json:"sms_threads"`
	LastUpdated int                     `json:"last_updated"`
}

type UpdateSmsThreads2xxResponse struct {
	Status string `json:"status"`
}

type GetSmsMessagesForThread2xxResponse struct {
	SmsMessages []sms_threads.SmsMessage `json:"sms_messages"`
	LastUpdated int                      `json:"last_updated"`
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
func getSmsThreads(responseWriter http.ResponseWriter, request *http.Request) {
	var variables = mux.Vars(request)
	var deviceId = variables["deviceId"]

	log.Println("Received get sms threads for", deviceId)

	// Ask the Android device to update the sms thread
	token := "cFAS88fZTgmw37RtNze_kq:APA91bHTfUd2X4CQa1_S0dwRmp9WeIfDlgTsW4GnIwR1Hr1OkQ_wWFnUi_CFn6GiAs2_2RIoUnD-8JMrOtrUggn7ktwqa2vTD7prS8IfJKIKXjeIpWBnup2NZ8M7EAP9J5rxxu4YLHQx"
	data := map[string]string{
		"action": "update_sms_threads",
	}
	fcm.SendFcmMessage(token, data, nil)

	// Get the existing sms threads
	smsThreads := sms_threads.GetSmsThreads()
	lastUpdated := sms_threads.GetLastTimeSmsThreadsUpdated()

	// Write response header
	responseWriter.Header().Set("Content-Type", "application/json")

	// Write response body
	responseBody := GetSmsThreads2xxResponse{smsThreads, lastUpdated}
	json.NewEncoder(responseWriter).Encode(responseBody)
}

/**
 * Updates the sms threads stored on a particular device
 */
func updateSmsThreads(responseWriter http.ResponseWriter, request *http.Request) {
	var variables = mux.Vars(request)
	var deviceId = variables["deviceId"]
	var smsThreads []sms_threads.SmsThread
	json.NewDecoder(request.Body).Decode(&smsThreads)

	log.Println("Received update sms threads for", deviceId, "with payload", smsThreads)

	sms_threads.UpdateSmsThreads(smsThreads)

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

	smsMessages, err := sms_threads.GetSmsMessagesForThread(threadId)
	if err != nil {
		responseWriter.Header().Set("Content-Type", "application/json")
		json.NewEncoder(responseWriter).Encode(GetSmsMessagesForThread4xxResponse{
			Error: err.Error(),
		})
		return
	}

	lastTimeUpdated, err := sms_threads.GetLastTimeSmsMessagesForThreadUpdated(threadId)

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
		LastUpdated: lastTimeUpdated,
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
	var smsMessages []sms_threads.SmsMessage
	json.NewDecoder(request.Body).Decode(&smsMessages)

	log.Println("Update sms msgs for thread", threadId, "from", deviceId, "with payload", smsMessages)

	sms_threads.UpdateSmsMessagesForThread(threadId, smsMessages)

	// Write response header
	responseWriter.Header().Set("Content-Type", "application/json")

	// Write response body
	responseBody := UpdateSmsMessagesForThread2xxResponse{"success"}
	json.NewEncoder(responseWriter).Encode(responseBody)
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
