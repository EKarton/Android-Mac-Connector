package devices

import (
	"encoding/json"
	"net/http"

	"github.com/gorilla/mux"

	"Android-Mac-Connector-Server/src/middlewares"
	"Android-Mac-Connector-Server/src/store"
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
func isDeviceRegistered(datastore *store.Datastore) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		userId := r.Header.Get("user_id")
		deviceType := r.URL.Query().Get("device_type")
		hardwareId := r.URL.Query().Get("hardware_id")

		isExist, err := datastore.DevicesStores.DoesDeviceExist(userId, deviceType, hardwareId)

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
func registerDevice(datastore *store.Datastore) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		userId := r.Header.Get("user_id")

		var jsonBody RegisterDeviceRequest
		json.NewDecoder(r.Body).Decode(&jsonBody)

		deviceId, err := datastore.DevicesStores.RegisterDevice(userId, jsonBody.DeviceType, jsonBody.HardwareDeviceId, jsonBody.Capabilities)

		if err != nil {
			panic(err)
		}

		json.NewEncoder(w).Encode(RegisterDevice2xxResponse{
			DeviceId: deviceId,
		})
	}
}

func updateDeviceCapabilities(datastore *store.Datastore) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		variables := mux.Vars(r)
		deviceId := variables["deviceId"]

		var jsonBody UpdateDeviceCapabilitiesRequest
		json.NewDecoder(r.Body).Decode(&jsonBody)

		if err := datastore.DevicesStores.UpdateDeviceCapabilities(deviceId, jsonBody.Capabilities); err != nil {
			panic(err)
		}
	}
}

func getDeviceCapabilities(datastore *store.Datastore) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		variables := mux.Vars(r)
		deviceId := variables["deviceId"]

		capabilities, err := datastore.DevicesStores.GetDeviceCapabilities(deviceId)

		if err != nil {
			panic(err)
		}

		w.Header().Set("Content-Type", "application/json")

		responseBody := GetDeviceCapabilities2xxResponse{Capabilities: capabilities}
		json.NewEncoder(w).Encode(responseBody)
	}
}

func updatePushNotificationToken(datastore *store.Datastore) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		variables := mux.Vars(r)
		deviceId := variables["deviceId"]

		var jsonBody UpdateAndroidPushNotificationTokenRequest
		json.NewDecoder(r.Body).Decode(&jsonBody)

		if err := datastore.DevicesStores.UpdatePushNotificationToken(deviceId, jsonBody.Token); err != nil {
			panic(err)
		}
	}
}

func InitializeRouter(datastore *store.Datastore, router *mux.Router) {
	router.Handle("/registered", middlewares.VerifyCredentials(http.HandlerFunc(isDeviceRegistered(datastore)))).Methods("GET")
	router.Handle("/register", middlewares.VerifyCredentials(http.HandlerFunc(registerDevice(datastore)))).Methods("POST")

	router.Handle("/{deviceId}/capabilities", middlewares.VerifyCredentials(http.HandlerFunc(updateDeviceCapabilities(datastore)))).Methods("PUT")
	router.Handle("/{deviceId}/capabilities", middlewares.VerifyCredentials(http.HandlerFunc(getDeviceCapabilities(datastore)))).Methods("GET")

	router.Handle("/{deviceId}/token", middlewares.VerifyCredentials(http.HandlerFunc(updatePushNotificationToken(datastore)))).Methods("PUT")
}
