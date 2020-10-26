package users

import (
	"errors"
)

type InMemoryUsersStore struct {
	userIdToDevices map[string](map[string]bool)
}

func CreateInMemoryStore() *InMemoryUsersStore {
	store := InMemoryUsersStore{
		userIdToDevices: make(map[string](map[string]bool)),
	}
	return &store
}

func (store *InMemoryUsersStore) AddUser(userId string) error {
	if isExist, _ := store.DoesUserExist(userId); isExist {
		return errors.New("User " + userId + " already exist in InMemoryUsersStore")
	}

	store.userIdToDevices[userId] = make(map[string]bool)

	return nil
}

func (store *InMemoryUsersStore) DeleteUser(userId string) error {
	if isExist, _ := store.DoesUserExist(userId); isExist {
		delete(store.userIdToDevices, userId)
		return nil
	}

	return errors.New("User " + userId + " does not exist in InMemoryUsersStore")
}

func (store *InMemoryUsersStore) DoesUserExist(userId string) (bool, error) {
	if _, ok := store.userIdToDevices[userId]; ok {
		return true, nil
	}
	return false, nil
}

func (store *InMemoryUsersStore) RegisterDeviceToUser(userId string, deviceId string) error {
	if isExist, _ := store.DoesUserExist(userId); isExist {
		if _, ok := store.userIdToDevices[userId][deviceId]; !ok {
			store.userIdToDevices[userId][deviceId] = true
			return nil
		}
		return errors.New("Device " + deviceId + " is already registered to " + userId)
	}

	return errors.New("User " + userId + " does not exist in InMemoryUsersStore")
}

func (store *InMemoryUsersStore) UnregisterDeviceFromUser(userId string, deviceId string) error {
	if isExist, _ := store.DoesUserExist(userId); isExist {
		if _, ok := store.userIdToDevices[userId][deviceId]; ok {
			delete(store.userIdToDevices[userId], deviceId)
			return nil
		}
		return errors.New("Device " + deviceId + " is not registered to " + userId)
	}

	return errors.New("User " + userId + " does not exist in InMemoryUsersStore")
}

func (store *InMemoryUsersStore) GetDevicesRegisteredToUser(userId string) ([]string, error) {
	if isExist, _ := store.DoesUserExist(userId); isExist {
		deviceIds := make([]string, 0, 0)
		for deviceId, _ := range store.userIdToDevices[userId] {
			deviceIds = append(deviceIds, deviceId)
		}
		return deviceIds, nil
	}

	return nil, errors.New("User " + userId + " does not exist in InMemoryUsersStore")
}
