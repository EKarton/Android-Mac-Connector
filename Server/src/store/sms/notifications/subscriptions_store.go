package notifications

import "github.com/google/uuid"

type Subscriber struct {
	uuid    string
	Channel chan SmsNotification
}

type SmsNotificationSubscribersStore struct {
	actualStore                 SmsNotificationsStore
	deviceIdToSubscriberUuid    map[string](map[string]bool)
	subscriberUuidToSubscribers map[string](*Subscriber)
	subscriberUuidToDeviceId    map[string]string
}

func CreateNotificationSubscribersStore(store SmsNotificationsStore) *SmsNotificationSubscribersStore {
	return &SmsNotificationSubscribersStore{
		actualStore:                 store,
		deviceIdToSubscriberUuid:    make(map[string](map[string]bool)),
		subscriberUuidToSubscribers: make(map[string](*Subscriber)),
		subscriberUuidToDeviceId:    make(map[string]string),
	}
}

func (store *SmsNotificationSubscribersStore) CreateSubscriber(deviceId string) (Subscriber, error) {
	subscriberUuid := ""
	if uuid, err := uuid.NewRandom(); err != nil {
		return Subscriber{}, err
	} else {
		subscriberUuid = uuid.String()
	}

	subscriber := Subscriber{
		uuid:    subscriberUuid,
		Channel: make(chan SmsNotification),
	}

	if _, ok := store.deviceIdToSubscriberUuid[deviceId]; !ok {
		store.deviceIdToSubscriberUuid[deviceId] = make(map[string]bool)
	}
	store.deviceIdToSubscriberUuid[deviceId][subscriberUuid] = true

	store.subscriberUuidToSubscribers[subscriberUuid] = &subscriber

	store.subscriberUuidToDeviceId[subscriberUuid] = deviceId

	return subscriber, nil
}

func (store *SmsNotificationSubscribersStore) RemoveSubscriber(subscriber Subscriber) {
	// Get the device id the subscriber belongs to
	deviceId := store.subscriberUuidToDeviceId[subscriber.uuid]

	delete(store.deviceIdToSubscriberUuid[deviceId], subscriber.uuid)
	delete(store.subscriberUuidToSubscribers, subscriber.uuid)
	delete(store.subscriberUuidToDeviceId, subscriber.uuid)
}

func (store *SmsNotificationSubscribersStore) AddSmsNotification(deviceId string, notification SmsNotification) error {
	err := store.actualStore.AddSmsNotification(deviceId, notification)
	if err != nil {
		return err
	}

	// Notify the channels
	for subscriberUuid := range store.deviceIdToSubscriberUuid[deviceId] {
		subscriber := store.subscriberUuidToSubscribers[subscriberUuid]

		// We send notifications in parallel
		go func(channel chan SmsNotification) {
			channel <- notification
			close(channel)
		}(subscriber.Channel)
	}

	return nil
}
