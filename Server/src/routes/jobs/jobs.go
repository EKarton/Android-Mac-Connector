package jobs

import (
	"Android-Mac-Connector-Server/src/application"
	"Android-Mac-Connector-Server/src/data/fcm"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"

	"github.com/gorilla/mux"
)

type PublishJob2xxResponse struct {
	JobId string `json:"job_id"`
}

type PublishJobResults2xxResponse struct {
	Status string `json:"status"`
}

type GetJobResults2xxResponse struct {
	Status  string      `json:"status"`
	Results interface{} `json:"results"`
}

func publishJob(appContext *application.ApplicationContext) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		variables := mux.Vars(r)
		deviceId := variables["deviceId"]

		var rawJsonBody map[string]interface{}
		json.NewDecoder(r.Body).Decode(&rawJsonBody)

		// Convert the json body to map[string][string]
		jsonBody := make(map[string]string)
		for key, value := range rawJsonBody {
			strValue := fmt.Sprintf("%v", value)
			jsonBody[key] = strValue
		}

		// Keep track of the job
		queue, err := appContext.DataStores.JobQueueService.GetQueue(deviceId)
		if err != nil {
			panic(err)
		}

		jobId, err := queue.AddJob()
		if err != nil {
			panic(err)
		}

		// Get the Push notification token
		token, err := appContext.DataStores.DevicesStores.GetPushNotificationToken(deviceId)
		if err != nil {
			panic(err)
		}

		// Inject the job id to the payload
		jsonBody["uuid"] = jobId
		if err := fcm.SendFcmMessage(token, jsonBody, nil); err != nil {
			panic(err)
		}

		// Write response body
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(PublishJob2xxResponse{
			JobId: jobId,
		})
	}
}

func publishJobResults(appContext *application.ApplicationContext) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {

		// Getting the request info
		variables := mux.Vars(r)
		deviceId := variables["deviceId"]
		jobId := variables["jobId"]

		// Parsing the request body
		var jsonBody map[string]interface{}
		json.NewDecoder(r.Body).Decode(&jsonBody)

		updatedStatus, isString := jsonBody["status"].(string)
		if !isString {
			panic(errors.New("Should be string!"))
		}
		updatedResults := jsonBody["results"]

		queue, err := appContext.DataStores.JobQueueService.GetQueue(deviceId)
		if err != nil {
			panic(err)
		}

		// Updating the job with new results
		if err := queue.UpdateJob(jobId, updatedStatus, updatedResults); err != nil {
			panic(err)
		}

		// Write response body
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(PublishJobResults2xxResponse{
			Status: "success",
		})
	}
}

func getJobResults(appContext *application.ApplicationContext) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {

		// Getting request info
		variables := mux.Vars(r)
		deviceId := variables["deviceId"]
		jobId := variables["jobId"]

		// Getting the job results and status
		queue, err := appContext.DataStores.JobQueueService.GetQueue(deviceId)
		if err != nil {
			panic(err)
		}

		results, err1 := queue.GetJobResults(jobId)
		status, err2 := queue.GetJobStatus(jobId)

		if err1 != nil {
			panic(err1)
		} else if err2 != nil {
			panic(err2)
		}

		// Write response body
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(GetJobResults2xxResponse{
			Status:  status,
			Results: results,
		})
	}
}

func InitializeRouter(appContext *application.ApplicationContext, router *mux.Router) {
	router.HandleFunc("", publishJob(appContext)).Methods("POST")
	router.HandleFunc("/{jobId}/results", publishJobResults(appContext)).Methods("POST")

	router.HandleFunc("/{jobId}/results", getJobResults(appContext)).Methods("GET")
}
