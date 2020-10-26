package notifications

type InMemoryNotificationsStore struct {
}

func CreateInMemoryStore() *InMemoryNotificationsStore {
	store := InMemoryNotificationsStore{}
	return &store
}

func (store *InMemoryNotificationsStore) AddSmsNotification() error {
	return nil
}

func (store *InMemoryNotificationsStore) GetNewSmsNotificationsFromUuid(numNotifications int, startingUuid string) ([]SmsNotification, error) {
	return make([]SmsNotification, 0), nil
}

func (store *InMemoryNotificationsStore) GetPreviousSmsNotificationsFromUuid(numNotifications int, startingUuid string) ([]SmsNotification, error) {
	return make([]SmsNotification, 0), nil
}

func (store *InMemoryNotificationsStore) GetOldestSmsNotification() (SmsNotification, error) {
	return nil, nil
}

func (store *InMemoryNotificationsStore) GetLatestSmsNotification() (SmsNotification, error) {
	return nil, nil
}
