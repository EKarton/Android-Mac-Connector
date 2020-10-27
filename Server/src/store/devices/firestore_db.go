package devices

import (
	"context"
	"errors"

	firestore "cloud.google.com/go/firestore"
	"google.golang.org/api/iterator"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
)

type FirestoreDevicesStore struct {
	client        *firestore.Client
	clientContext context.Context
}

func CreateFirestoreDevicesStore(client *firestore.Client) *FirestoreDevicesStore {
	return &FirestoreDevicesStore{
		client:        client,
		clientContext: context.Background(),
	}
}

// Checks if a device already exists under a user given the user id, the device type, and the hardware id
//
// Returns two things:
// 1. a boolean: true if it exists, else false
// 2. an error, if it was not able to determine if it exists or not
//
func (store *FirestoreDevicesStore) DoesDeviceExist(userId string, deviceType string, hardwareId string) (bool, error) {
	devicesCollection := store.client.Collection("devices")
	query := devicesCollection.Where("user_id", "==", userId)
	query = query.Where("device_type", "==", deviceType)
	query = query.Where("hardware_id", "==", hardwareId)

	_, err := query.Limit(1).Documents(store.clientContext).Next()

	if err == iterator.Done {
		return false, nil
	}

	return true, err
}

// Registers a device
//
// It returns two things:
// 1. The device id: it is a random ID assigned to this device. Use this in further API calls
// 2. error:
//    a) Returns a DeviceAlreadyRegisteredError if the device already exists, or
//    b) Generic error for unknown errors
//    c) nil, if there are no errors
//
func (store *FirestoreDevicesStore) RegisterDevice(userId string, deviceType string, hardwareId string, capabilities []string) (string, error) {
	if isExist, err := store.DoesDeviceExist(userId, deviceType, hardwareId); isExist {
		return "", CreateDeviceAlreadyRegisteredError(userId, deviceType, hardwareId)

	} else if err != nil {
		return "", err
	}

	devicesCollection := store.client.Collection("devices")
	doc, _, err := devicesCollection.Add(context.Background(), map[string]interface{}{
		"user_id":                 userId,
		"device_type":             deviceType,
		"hardware_id":             hardwareId,
		"push_notification_token": "",
		"capabilities":            capabilities,
	})

	if err != nil {
		return "", err
	}
	return doc.ID, nil
}

// Updates the device with new capabilities
//
// Returns:
// a) a CreateDeviceNotFoundError if the device could not be found, or
// b) a generic error, or
// c) nil, if no error is thrown
//
func (store *FirestoreDevicesStore) UpdateDeviceCapabilities(deviceId string, capabilities []string) error {
	if isExist, err := store.DoesDeviceIdExist(deviceId); isExist {
		return CreateDeviceNotFoundError(deviceId)
	} else if err != nil {
		return err
	}

	updatedData := map[string]interface{}{
		"capabilities": capabilities,
	}

	devicesCollection := store.client.Collection("devices")
	_, err := devicesCollection.Doc(deviceId).Set(context.Background(), updatedData, firestore.MergeAll)

	return err
}

// Returns the capabilities of a device
// The `deviceId` should be the value when you call store.RegisterDevice()
//
// Returns two things:
// 1. The capabilities, in an array of strings, and
// 2. An error:
//    a) a CreateDeviceNotFoundError if it could not find an error, or
//    b) a generic error, or
//    c) nil if no error occured
//
func (store *FirestoreDevicesStore) GetDeviceCapabilities(deviceId string) ([]string, error) {
	doc, err := store.client.Collection("devices").Doc(deviceId).Get(context.Background())

	if err != nil {
		if grpc.Code(err) == codes.NotFound {
			return nil, CreateDeviceNotFoundError(deviceId)
		}
		return nil, err
	}

	capabilitiesInterface, isArray := doc.Data()["capabilities"].([]interface{})

	if !isArray {
		return nil, errors.New("'capabilities' field for device " + deviceId + " is not an array")
	}

	capabilities := make([]string, 0, 0)

	for _, v := range capabilitiesInterface {
		capability, isString := v.(string)
		if !isString {
			return nil, errors.New("capability item " + capability + " is not a string")
		}

		capabilities = append(capabilities, capability)
	}

	return capabilities, nil
}

// Updates the push notification token for a particular device
// The `deviceId` should be the value when you call store.RegisterDevice()
//
// Returns:
// a) a CreateDeviceNotFoundError when it could not find the device, or
// b) a generic error, or
// c) nil if no error occured
//
func (store *FirestoreDevicesStore) UpdatePushNotificationToken(deviceId string, newToken string) error {
	if isExist, err := store.DoesDeviceIdExist(deviceId); !isExist {
		return CreateDeviceNotFoundError(deviceId)
	} else if err != nil {
		return err
	}

	updatedData := map[string]interface{}{
		"push_notification_token": newToken,
	}

	devicesCollection := store.client.Collection("devices")
	_, err := devicesCollection.Doc(deviceId).Set(context.Background(), updatedData, firestore.MergeAll)

	return err
}

// Gets the push notification token of a device
// The `deviceId` should be the value when you call store.RegisterDevice()
//
// Returns:
// 1. the push notification token of this device, and
// 2. an error:
//    a) A CreateDeviceNotFoundError if it could not find the device, or
//    b) a generic error, or
//    c) nil if no error occured
//
func (store *FirestoreDevicesStore) GetPushNotificationToken(deviceId string) (string, error) {
	doc, err := store.client.Collection("devices").Doc(deviceId).Get(context.Background())

	if err != nil {
		if grpc.Code(err) == codes.NotFound {
			return "", CreateDeviceNotFoundError(deviceId)
		}
		return "", err
	}

	token, isString := doc.Data()["push_notification_token"].(string)

	if !isString {
		return "", errors.New("'push_notification_token' field for device " + deviceId + " is not a string")
	}

	return token, nil
}

// Determines if a device exists
//
// Returns two things:
// 1. a boolean: true if the device exists, else false, and
// 2. an error: an error else nil if no error occured
//
func (store *FirestoreDevicesStore) DoesDeviceIdExist(deviceId string) (bool, error) {
	devicesCollection := store.client.Collection("devices")
	doc, err := devicesCollection.Doc(deviceId).Get(context.Background())

	if err != nil {
		if grpc.Code(err) == codes.NotFound {
			return false, nil
		}
		return false, err
	}

	return doc.Exists(), err
}
