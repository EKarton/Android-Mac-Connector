package devices

import (
	"errors"
	"fmt"

	uuid "github.com/google/uuid"
)

type InMemoryDevicesStore struct {
	userIdToDeviceIds map[string][]string
	deviceIdToDevice  map[string]*Device
}

func CreateInMemoryStore() *InMemoryDevicesStore {
	store := InMemoryDevicesStore{
		userIdToDeviceIds: make(map[string][]string),
		deviceIdToDevice:  make(map[string]*Device),
	}

	return &store
}

func (store *InMemoryDevicesStore) DoesDeviceExist(userId string, deviceType string, hardwareId string) (bool, error) {
	if _, ok := store.userIdToDeviceIds[userId]; !ok {
		return false, nil
	}

	for _, deviceId := range store.userIdToDeviceIds[userId] {
		device := store.deviceIdToDevice[deviceId]
		if device.DeviceType == deviceType && device.HardwareId == hardwareId {
			return true, nil
		}
	}
	return false, nil
}

func (store *InMemoryDevicesStore) RegisterDevice(userId string, deviceType string, hardwareId string, capabilities []string) (string, error) {
	isExist, err := store.DoesDeviceExist(userId, deviceType, hardwareId)

	if isExist {
		return "", errors.New("Device already exists")
	}

	if err != nil {
		return "", errors.New("Unknown error " + err.Error())
	}

	deviceId := ""

	if uuid, err := uuid.NewRandom(); err != nil {
		return "", err
	} else {
		deviceId = uuid.String()
	}

	newDevice := Device{
		UserId:                userId,
		DeviceType:            deviceType,
		HardwareId:            hardwareId,
		Capabilities:          capabilities,
		PushNotificationToken: "",
	}

	store.deviceIdToDevice[deviceId] = &newDevice
	store.userIdToDeviceIds[userId] = append(store.userIdToDeviceIds[userId], deviceId)

	return deviceId, nil
}

func (store *InMemoryDevicesStore) UpdateDeviceCapabilities(deviceId string, capabilities []string) error {
	device, isExist := store.deviceIdToDevice[deviceId]

	if !isExist {
		return errors.New("Device id " + deviceId + " does not exist")
	}

	device.Capabilities = capabilities
	return nil
}

func (store *InMemoryDevicesStore) GetDeviceCapabilities(deviceId string) ([]string, error) {
	device, isExist := store.deviceIdToDevice[deviceId]

	if !isExist {
		return nil, errors.New("Device id " + deviceId + " does not exist")
	}

	return device.Capabilities, nil
}

func (store *InMemoryDevicesStore) UpdatePushNotificationToken(deviceId string, newToken string) error {
	device, isExist := store.deviceIdToDevice[deviceId]

	if !isExist {
		return errors.New("Device id " + deviceId + " does not exist")
	}

	device.PushNotificationToken = newToken

	fmt.Println(device, newToken, device.PushNotificationToken)

	return nil
}

func (store *InMemoryDevicesStore) GetPushNotificationToken(deviceId string) (string, error) {
	device, isExist := store.deviceIdToDevice[deviceId]

	if !isExist {
		return "", errors.New("Device id " + deviceId + " does not exist")
	}

	fmt.Println(device, device.PushNotificationToken)

	return device.PushNotificationToken, nil
}
