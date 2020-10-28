package notifications

import (
	"context"

	"cloud.google.com/go/firestore"
	"google.golang.org/api/iterator"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
)

type FirestoreNotificationsStore struct {
	client         *firestore.Client
	maxQueueLength int
}

func CreateFirestoreNotificationsStore(client *firestore.Client, maxQueueLength int) *FirestoreNotificationsStore {
	return &FirestoreNotificationsStore{
		client:         client,
		maxQueueLength: maxQueueLength,
	}
}

type smsNotificationQueue struct {
	queueId             string
	deviceId            string
	firstNotificationId string
	lastNotificationId  string
	curLength           int64
	maxLength           int64
}

type smsNotification struct {
	notificationId string
	next           string
	previous       string
	contactInfo    string
	data           string
	timestamp      int64
}

// Adds a SMS notification to the queue
// It will create a queue if no queue with the matching device id is found
// It will also remove the oldest messages if the queue length exceeds the max length
//
// It returns an error if an error occured; else nil
//
func (store *FirestoreNotificationsStore) AddSmsNotification(deviceId string, notification SmsNotification) error {
	// Get or create an SMS notification queue
	queue, err := store.getOrCreateSmsNotificationsQueue(deviceId)
	if err != nil {
		return err
	}

	// Create the SMS notification
	newSmsNotification := map[string]interface{}{
		"next":     nil,
		"previous": nil,
		"notification": map[string]interface{}{
			"contact_info": notification.ContactInfo,
			"data":         notification.Data,
			"timestamp":    int64(notification.Timestamp),
		},
	}
	if queue.curLength > 0 {
		newSmsNotification["previous"] = queue.firstNotificationId
	}

	// Add the notification to the collection
	newSmsNotificationId := ""
	notificationsCollection := store.client.Collection("sms-notifications")
	if doc, _, err := notificationsCollection.Add(context.Background(), newSmsNotification); err != nil {
		return err
	} else {
		newSmsNotificationId = doc.ID
	}

	// Update the first notification's 'next' field to point to our current one
	if queue.curLength > 0 {
		updatedData := map[string]interface{}{
			"next": newSmsNotificationId,
		}
		_, err = notificationsCollection.Doc(queue.firstNotificationId).Set(context.Background(), updatedData, firestore.MergeAll)
	}

	// Update the queue's 'first_notification' and 'cur_length' field
	updatedLastNotificationId := queue.lastNotificationId
	if queue.curLength == 0 {
		updatedLastNotificationId = newSmsNotificationId
	}
	updatedData := map[string]interface{}{
		"first_notification_id": newSmsNotificationId,
		"last_notification_id":  updatedLastNotificationId,
		"cur_length":            queue.curLength + 1,
	}
	queuesCollection := store.client.Collection("sms-notification-queues")
	_, err = queuesCollection.Doc(queue.queueId).Set(context.Background(), updatedData, firestore.MergeAll)

	return err
}

// Gets the Sms notification queue
// If it cannot find it, it will make one
//
// It returns:
// 1. The sms queue
// 2. An error, if an error occured; else nil
//
func (store *FirestoreNotificationsStore) getOrCreateSmsNotificationsQueue(deviceId string) (*smsNotificationQueue, error) {
	queue, err := store.getSmsNotificationQueue(deviceId)
	if err != nil {
		return nil, err
	}

	if queue == nil {
		return store.createSmsNotificationQueue(deviceId)
	}

	return queue, nil
}

// Gets the SMS Notification Queue from a device id
// Note: if the queue could not be found, it does not return an error
//
// Returns two things:
// 1. The queue, if it finds one, else nil
// 2. The error, if an error occured; else nil
//
func (store *FirestoreNotificationsStore) getSmsNotificationQueue(deviceId string) (*smsNotificationQueue, error) {
	queuesCollection := store.client.Collection("sms-notification-queues")
	query := queuesCollection.Where("device_id", "==", deviceId)

	doc, err := query.Limit(1).Documents(context.Background()).Next()
	if err == iterator.Done {
		return nil, nil

	} else if err != nil {
		return nil, err
	}

	rawData := doc.Data()
	queue := smsNotificationQueue{
		queueId:             doc.Ref.ID,
		deviceId:            rawData["device_id"].(string),
		firstNotificationId: rawData["first_notification_id"].(string),
		lastNotificationId:  rawData["last_notification_id"].(string),
		curLength:           rawData["cur_length"].(int64),
		maxLength:           rawData["max_length"].(int64),
	}
	queue.queueId = doc.Ref.ID

	return &queue, nil
}

// Creates a new Sms notification queue given the device id
// NOTE: It does not check if a queue with the same device id exists!!
//
// Returns two things:
// 1. A typed SMS notification queue
// 2. The error object, if an error occured, else nil
//
func (store *FirestoreNotificationsStore) createSmsNotificationQueue(deviceId string) (*smsNotificationQueue, error) {
	newQueue := map[string]interface{}{
		"first_notification_id": "",
		"last_notification_id":  "",
		"device_id":             deviceId,
		"cur_length":            0,
		"max_length":            store.maxQueueLength,
	}

	queuesCollection := store.client.Collection("sms-notification-queues")
	queueId := ""
	if doc, _, err := queuesCollection.Add(context.Background(), newQueue); err != nil {
		return nil, err
	} else {
		queueId = doc.ID
	}

	queue := smsNotificationQueue{
		queueId:             queueId,
		firstNotificationId: "",
		lastNotificationId:  "",
		deviceId:            deviceId,
		curLength:           0,
		maxLength:           int64(store.maxQueueLength),
	}

	return &queue, nil
}

// Returns a list of at most X newest SMS notifications starting from (but not including) a starting notification id
// It will return an empty list if the starting notification is the newest notification
//
// Returns:
// 1. A list of SMS notifications
// 2. An error object, if an error occured; else nil
//
func (store *FirestoreNotificationsStore) GetNewSmsNotificationsFromUuid(deviceId string, numNotifications int, startingUuid string) ([]SmsNotification, error) {
	notification, err := store.getSmsNotification(startingUuid)
	if err != nil {
		return nil, err
	}

	lst := make([]SmsNotification, 0, numNotifications)

	for len(lst) < numNotifications && notification != nil {
		nextNotification, err := store.getSmsNotification(notification.next)
		if err != nil {
			return nil, err
		}
		if nextNotification == nil {
			break
		}

		lst = append(lst, SmsNotification{
			Uuid:        nextNotification.notificationId,
			ContactInfo: nextNotification.contactInfo,
			Data:        nextNotification.data,
			Timestamp:   int(nextNotification.timestamp),
		})

		notification = nextNotification
	}

	return lst, nil
}

// Returns a list of at most X oldest SMS notifications starting from (but not including) a starting notification id
// It will return an empty list if the starting notification is the oldest notification
//
// Returns:
// 1. A list of SMS notifications
// 2. An error object, if an error occured; else nil
//
func (store *FirestoreNotificationsStore) GetPreviousSmsNotificationsFromUuid(deviceId string, numNotifications int, startingUuid string) ([]SmsNotification, error) {
	notification, err := store.getSmsNotification(startingUuid)
	if err != nil {
		return nil, err
	}

	lst := make([]SmsNotification, 0, numNotifications)

	for len(lst) < numNotifications && notification != nil {
		prevNotification, err := store.getSmsNotification(notification.previous)
		if err != nil {
			return nil, err
		}
		if prevNotification == nil {
			break
		}

		lst = append(lst, SmsNotification{
			Uuid:        prevNotification.notificationId,
			ContactInfo: prevNotification.contactInfo,
			Data:        prevNotification.data,
			Timestamp:   int(prevNotification.timestamp),
		})

		notification = prevNotification
	}

	return lst, nil
}

func (store *FirestoreNotificationsStore) GetOldestSmsNotification(deviceId string) (SmsNotification, error) {
	return SmsNotification{}, nil
}

func (store *FirestoreNotificationsStore) GetLatestSmsNotification(deviceId string) (SmsNotification, error) {
	return SmsNotification{}, nil
}

func (store *FirestoreNotificationsStore) RemoveSmsNotification(deviceId string, nodeUuid string) error {
	return nil
}

func (store *FirestoreNotificationsStore) getSmsNotification(uuid string) (*smsNotification, error) {
	if uuid == "" {
		return nil, nil
	}

	notificationsCollection := store.client.Collection("sms-notifications")
	notification, err := notificationsCollection.Doc(uuid).Get(context.Background())
	if err != nil {

		// If it could not find the notification
		if grpc.Code(err) == codes.NotFound {
			return nil, nil
		}

		return nil, err
	}

	notificationData := notification.Data()
	typedNotification := smsNotification{
		notificationId: notification.Ref.ID,
		contactInfo:    notificationData["notification"].(map[string]interface{})["contact_info"].(string),
		data:           notificationData["notification"].(map[string]interface{})["data"].(string),
		timestamp:      notificationData["notification"].(map[string]interface{})["timestamp"].(int64),
	}

	if next, isString := notificationData["next"].(string); isString {
		typedNotification.next = next
	}

	if previous, isString := notificationData["previous"].(string); isString {
		typedNotification.previous = previous
	}

	return &typedNotification, nil
}
