package devices

import (
	"errors"

	uuid "github.com/google/uuid"
)

type InMemoryDevicesStore struct {
	deviceIdToDevice map[string]Device
}

func CreateInMemoryStore() *InMemoryDevicesStore {
	store := InMemoryDevicesStore{
		deviceIdToDevice: make(map[string]Device),
	}

	return &store
}

func (store *InMemoryDevicesStore) GetDevice(deviceId string) (Device, error) {
	return store.deviceIdToDevice[deviceId], nil
}

func (store *InMemoryDevicesStore) AddDevice(device Device) (string, error) {
	deviceId := ""

	if uuid, err := uuid.NewRandom(); err != nil {
		return "", err
	} else {
		deviceId = uuid.String()
	}

	store.deviceIdToDevice[deviceId] = device
	return deviceId, nil
}

func (store *InMemoryDevicesStore) DeleteDevice(deviceId string) error {
	if _, ok := store.deviceIdToDevice[deviceId]; ok {
		delete(store.deviceIdToDevice, deviceId)
		return nil
	}

	return errors.New("Device id" + deviceId + " does not exist in InMemoryDevicesStore")
}
