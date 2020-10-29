package devices

import (
	"encoding/json"
	"net/http"

	"github.com/gorilla/mux"

	"Android-Mac-Connector-Server/src/application"
	"Android-Mac-Connector-Server/src/middlewares"
)

type IsDeviceRegistered2xxResponse struct {
	IsRegistered bool `json:"is_registered"`
}

type RegisterDeviceRequest struct {
	DeviceType       string   `json:"device_type"`
	HardwareDeviceId string   `json:"hardware_id"`
	Capabilities     []string `json:"capabilities"`
}

type RegisterDevice2xxResponse struct {
	DeviceId string `json:"device_id"`
}

type GetDeviceCapabilities2xxResponse struct {
	Capabilities []string `json:"capabilities"`
}

type UpdateDeviceCapabilitiesRequest struct {
	Capabilities []string `json:"new_capabilities"`
}

type UpdateAndroidPushNotificationTokenRequest struct {
	Token string `json:"new_token"`
}

/*
 * Checks if a device is registered or not, based on the user id, the device type, and the hardware number
 */
func isDeviceRegistered(appContext *application.ApplicationContext) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		userId := r.Header.Get("user_id")
		deviceType := r.URL.Query().Get("device_type")
		hardwareId := r.URL.Query().Get("hardware_id")

		isExist, err := appContext.DataStores.DevicesStores.DoesDeviceExist(userId, deviceType, hardwareId)

		if err != nil {
			panic(err)
		}

		json.NewEncoder(w).Encode(IsDeviceRegistered2xxResponse{
			IsRegistered: isExist,
		})
	}
}

/**
 * Registers a device given the user id, device type, and the hardware id
 */
func registerDevice(appContext *application.ApplicationContext) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		userId := r.Header.Get("user_id")

		var jsonBody RegisterDeviceRequest
		json.NewDecoder(r.Body).Decode(&jsonBody)

		deviceId, err := appContext.DataStores.DevicesStores.RegisterDevice(userId, jsonBody.DeviceType, jsonBody.HardwareDeviceId, jsonBody.Capabilities)

		if err != nil {
			panic(err)
		}

		json.NewEncoder(w).Encode(RegisterDevice2xxResponse{
			DeviceId: deviceId,
		})
	}
}

func updateDeviceCapabilities(appContext *application.ApplicationContext) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		variables := mux.Vars(r)
		deviceId := variables["deviceId"]

		var jsonBody UpdateDeviceCapabilitiesRequest
		json.NewDecoder(r.Body).Decode(&jsonBody)

		if err := appContext.DataStores.DevicesStores.UpdateDeviceCapabilities(deviceId, jsonBody.Capabilities); err != nil {
			panic(err)
		}
	}
}

func getDeviceCapabilities(appContext *application.ApplicationContext) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		variables := mux.Vars(r)
		deviceId := variables["deviceId"]

		capabilities, err := appContext.DataStores.DevicesStores.GetDeviceCapabilities(deviceId)

		if err != nil {
			panic(err)
		}

		w.Header().Set("Content-Type", "application/json")

		responseBody := GetDeviceCapabilities2xxResponse{Capabilities: capabilities}
		json.NewEncoder(w).Encode(responseBody)
	}
}

func updatePushNotificationToken(appContext *application.ApplicationContext) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		variables := mux.Vars(r)
		deviceId := variables["deviceId"]

		var jsonBody UpdateAndroidPushNotificationTokenRequest
		json.NewDecoder(r.Body).Decode(&jsonBody)

		if err := appContext.DataStores.DevicesStores.UpdatePushNotificationToken(deviceId, jsonBody.Token); err != nil {
			panic(err)
		}
	}
}

func InitializeRouter(appContext *application.ApplicationContext, router *mux.Router) {
	verifyCredentials := middlewares.VerifyCredentials(appContext)

	router.Handle("/registered",
		verifyCredentials(
			http.HandlerFunc(
				isDeviceRegistered(appContext)))).Methods("GET")

	router.Handle("/register",
		verifyCredentials(
			http.HandlerFunc(
				registerDevice(appContext)))).Methods("POST")

	router.Handle("/{deviceId}/capabilities",
		verifyCredentials(
			http.HandlerFunc(
				updateDeviceCapabilities(appContext)))).Methods("PUT")

	router.Handle("/{deviceId}/capabilities",
		verifyCredentials(
			http.HandlerFunc(
				getDeviceCapabilities(appContext)))).Methods("GET")

	router.Handle("/{deviceId}/token",
		verifyCredentials(
			http.HandlerFunc(
				updatePushNotificationToken(appContext)))).Methods("PUT")
}
