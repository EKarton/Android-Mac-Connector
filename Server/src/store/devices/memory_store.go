package devices

type InMemoryDevicesStore struct {
	userIdToDevices map[string][]Device
}

func CreateInMemoryStore() *InMemoryDevicesStore {
	store := InMemoryDevicesStore {
		userIdToDevices: make(map[string][]Device)
	}

	return &store
}

func (store *InMemoryDevicesStore) GetDevices(userId string) ([]Device, error) {
	return store.userIdToDevices[userId], nil
}

func (store *InMemoryDevicesStore) AddDevice(userId string, device Device) (string, error) {
	return "", nil
}

func (store *InMemoryDevicesStore) DeleteDevice(deviceId string) error {
	return nil
}
