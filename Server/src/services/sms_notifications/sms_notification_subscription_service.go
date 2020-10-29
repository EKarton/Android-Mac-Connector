package sms_notifications

import (
	"github.com/google/uuid"
)

type Subscriber struct {
	uuid    string
	Channel chan SmsNotification
}

type SmsNotificationSubscriptionService struct {
	actualStore                 SmsNotificationService
	deviceIdToSubscriberUuid    map[string](map[string]bool)
	subscriberUuidToSubscribers map[string](*Subscriber)
	subscriberUuidToDeviceId    map[string]string
}

func CreateSmsNotificationSubscriptionService(store SmsNotificationService) *SmsNotificationSubscriptionService {
	return &SmsNotificationSubscriptionService{
		actualStore:                 store,
		deviceIdToSubscriberUuid:    make(map[string](map[string]bool)),
		subscriberUuidToSubscribers: make(map[string](*Subscriber)),
		subscriberUuidToDeviceId:    make(map[string]string),
	}
}

func (store *SmsNotificationSubscriptionService) CreateSubscriber(deviceId string) (Subscriber, error) {
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

func (store *SmsNotificationSubscriptionService) RemoveSubscriber(subscriber Subscriber) {
	// Get the device id the subscriber belongs to
	deviceId := store.subscriberUuidToDeviceId[subscriber.uuid]

	delete(store.deviceIdToSubscriberUuid[deviceId], subscriber.uuid)
	delete(store.subscriberUuidToSubscribers, subscriber.uuid)
	delete(store.subscriberUuidToDeviceId, subscriber.uuid)
}

func (store *SmsNotificationSubscriptionService) AddSmsNotification(deviceId string, notification SmsNotification) (string, error) {
	nodeId, err := store.actualStore.AddSmsNotification(deviceId, notification)
	if err != nil {
		return "", err
	}

	// Make a copy of the original notification object so that
	// the data is not gone when the sender's request is finished
	copiedNotification := SmsNotification{
		Uuid:        nodeId,
		ContactInfo: notification.ContactInfo,
		Data:        notification.Data,
		Timestamp:   notification.Timestamp,
	}

	// Notify the channels in parallel
	for subscriberUuid := range store.deviceIdToSubscriberUuid[deviceId] {
		subscriber := store.subscriberUuidToSubscribers[subscriberUuid]

		go func(channel chan SmsNotification) {
			channel <- copiedNotification
		}(subscriber.Channel)
	}

	return nodeId, nil
}

func (store *SmsNotificationSubscriptionService) GetNewSmsNotificationsFromUuid(deviceId string, numNotifications int, startingUuid string) ([]SmsNotification, error) {
	return store.actualStore.GetNewSmsNotificationsFromUuid(deviceId, numNotifications, startingUuid)
}

func (store *SmsNotificationSubscriptionService) GetPreviousSmsNotificationsFromUuid(deviceId string, numNotifications int, startingUuid string) ([]SmsNotification, error) {
	return store.actualStore.GetPreviousSmsNotificationsFromUuid(deviceId, numNotifications, startingUuid)
}

func (store *SmsNotificationSubscriptionService) GetOldestSmsNotification(deviceId string) (SmsNotification, error) {
	return store.actualStore.GetOldestSmsNotification(deviceId)
}

func (store *SmsNotificationSubscriptionService) GetLatestSmsNotification(deviceId string) (SmsNotification, error) {
	return store.actualStore.GetLatestSmsNotification(deviceId)
}
