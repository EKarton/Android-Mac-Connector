package devices

import (
	"encoding/json"
	"log"
	"net/http"

	"github.com/gorilla/mux"

	middlewares "Android-Mac-Connector-Server/src/middlewares"
)

type IsDeviceRegistered2xxResponse struct {
	IsRegistered bool `json:"is_registered"`
}

type RegisterDevice2xxResponse struct {
	DeviceId string `json:"device_id"`
}

func isDeviceRegistered(responseWriter http.ResponseWriter, request *http.Request) {
	// TODO: Implement api
	log.Println(request)
	json.NewEncoder(responseWriter).Encode(IsDeviceRegistered2xxResponse{
		IsRegistered: false,
	})
}

func registerDevice(responseWriter http.ResponseWriter, request *http.Request) {
	// TODO: Implement api
	log.Println(request)
	json.NewEncoder(responseWriter).Encode(RegisterDevice2xxResponse{
		DeviceId: "1234",
	})
}

func updateDeviceCapabilities(responseWriter http.ResponseWriter, request *http.Request) {
	// TODO: Implement api
	log.Println(request)
}

func getDeviceCapabilities(responseWriter http.ResponseWriter, request *http.Request) {
	// TODO: Implement api
	log.Println(request)
}

func updatePushNotificationToken(responseWriter http.ResponseWriter, request *http.Request) {
	// TODO: Implement api
	log.Println(request)
}

func InitializeRouter(router *mux.Router) {
	router.Handle("/registered", middlewares.VerifyCredentials(http.HandlerFunc(isDeviceRegistered))).Methods("GET")
	router.Handle("/register", middlewares.VerifyCredentials(http.HandlerFunc(registerDevice))).Methods("POST")

	router.Handle("/{deviceId}/capabilities", middlewares.VerifyCredentials(http.HandlerFunc(updateDeviceCapabilities))).Methods("PUT")
	router.Handle("/{deviceId}/capabilities", middlewares.VerifyCredentials(http.HandlerFunc(getDeviceCapabilities))).Methods("GET")

	router.Handle("/{deviceId}/token", middlewares.VerifyCredentials(http.HandlerFunc(updatePushNotificationToken))).Methods("PUT")
}
