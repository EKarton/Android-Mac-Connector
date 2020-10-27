package devices

import (
	"encoding/json"
	"net/http"

	"github.com/gorilla/mux"

	middlewares "Android-Mac-Connector-Server/src/middlewares"
	deviceStore "Android-Mac-Connector-Server/src/store/devices"
)

var devicesStore deviceStore.DevicesStore = deviceStore.CreateInMemoryStore()

type IsDeviceRegistered2xxResponse struct {
	IsRegistered bool `json:"is_registered"`
}

type RegisterDeviceRequest struct {
	DeviceType       string `json:"device_type"`
	HardwareDeviceId string `json:"hardware_id"`
	Capabilities     string `json:"capabilities"`
}

type RegisterDevice2xxResponse struct {
	DeviceId string `json:"device_id"`
}

type GetDeviceCapabilities2xxResponse struct {
	Capabilities []string `json:"capabilities"`
}

type UpdateDeviceCapabilitiesRequest struct {
	Capabilities []string `json:"capabilities"`
}

type UpdateAndroidPushNotificationTokenRequest struct {
	Token string `json:"fcm_token"`
}

/*
 * Checks if a device is registered or not, based on the user id, the device type, and the hardware number
 */
func isDeviceRegistered(responseWriter http.ResponseWriter, request *http.Request) {
	userId := request.Header.Get("user_id")
	deviceType := request.URL.Query().Get("device_type")
	hardwareId := request.URL.Query().Get("hardware_id")

	isExist, err := devicesStore.DoesDeviceExist(userId, deviceType, hardwareId)

	if err != nil {
		responseWriter.WriteHeader(http.StatusInternalServerError)
		return
	}

	json.NewEncoder(responseWriter).Encode(IsDeviceRegistered2xxResponse{
		IsRegistered: isExist,
	})
}

/**
 * Registers a device given the user id, device type, and the hardware id
 */
func registerDevice(responseWriter http.ResponseWriter, request *http.Request) {
	userId := request.Header.Get("user_id")

	var jsonBody RegisterDeviceRequest
	json.NewDecoder(request.Body).Decode(&jsonBody)

	deviceId, err := devicesStore.RegisterDevice(userId, jsonBody.DeviceType, jsonBody.HardwareDeviceId, jsonBody.Capabilities)

	if err != nil {
		responseWriter.WriteHeader(http.StatusInternalServerError)
		return
	}

	json.NewEncoder(responseWriter).Encode(RegisterDevice2xxResponse{
		DeviceId: deviceId,
	})
}

func updateDeviceCapabilities(responseWriter http.ResponseWriter, request *http.Request) {
	variables := mux.Vars(request)
	deviceId := variables["deviceId"]

	var jsonBody UpdateDeviceCapabilitiesRequest
	json.NewDecoder(request.Body).Decode(&jsonBody)

	if err := devicesStore.UpdateDeviceCapabilities(deviceId, jsonBody.Capabilities); err != nil {
		responseWriter.WriteHeader(http.StatusInternalServerError)
		return
	}
}

func getDeviceCapabilities(responseWriter http.ResponseWriter, request *http.Request) {
	variables := mux.Vars(request)
	deviceId := variables["deviceId"]

	capabilities, err := devicesStore.GetDeviceCapabilities(deviceId)

	if err != nil {
		responseWriter.WriteHeader(http.StatusInternalServerError)
		return
	}

	responseWriter.Header().Set("Content-Type", "application/json")

	responseBody := GetDeviceCapabilities2xxResponse{Capabilities: capabilities}
	json.NewEncoder(responseWriter).Encode(responseBody)
}

func updatePushNotificationToken(responseWriter http.ResponseWriter, request *http.Request) {
	variables := mux.Vars(request)
	deviceId := variables["deviceId"]

	var jsonBody UpdateAndroidPushNotificationTokenRequest
	json.NewDecoder(request.Body).Decode(&jsonBody)

	if err := devicesStore.UpdatePushNotificationToken(deviceId, jsonBody.Token); err != nil {
		responseWriter.WriteHeader(http.StatusInternalServerError)
		return
	}
}

func InitializeRouter(router *mux.Router) {
	router.Handle("/registered", middlewares.VerifyCredentials(http.HandlerFunc(isDeviceRegistered))).Methods("GET")
	router.Handle("/register", middlewares.VerifyCredentials(http.HandlerFunc(registerDevice))).Methods("POST")

	router.Handle("/{deviceId}/capabilities", middlewares.VerifyCredentials(http.HandlerFunc(updateDeviceCapabilities))).Methods("PUT")
	router.Handle("/{deviceId}/capabilities", middlewares.VerifyCredentials(http.HandlerFunc(getDeviceCapabilities))).Methods("GET")

	router.Handle("/{deviceId}/token", middlewares.VerifyCredentials(http.HandlerFunc(updatePushNotificationToken))).Methods("PUT")
}
