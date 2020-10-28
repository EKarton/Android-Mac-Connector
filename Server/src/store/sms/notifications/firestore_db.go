package notifications

import (
	"Android-Mac-Connector-Server/src/store/sms/notifications/firebase"
	"context"

	"cloud.google.com/go/firestore"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
)

type FirestoreNotificationsStore struct {
	client               *firestore.Client
	firebaseQueueService *firebase.FirebaseQueueClient
	maxQueueLength       int
}

func CreateFirestoreNotificationsStore(client *firestore.Client, maxQueueLength int) *FirestoreNotificationsStore {
	return &FirestoreNotificationsStore{
		client:               client,
		firebaseQueueService: firebase.NewFirebaseQueueClient(client, "sms-notification-queues"),
		maxQueueLength:       maxQueueLength,
	}
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
	// Get our queue
	queue, err := store.firebaseQueueService.GetOrCreateQueue(deviceId, 1000)
	if err != nil {
		return err
	}

	// Create a SMS notification
	newSmsNotification := smsNotification{
		next:        "",
		previous:    "",
		contactInfo: notification.ContactInfo,
		data:        notification.Data,
		timestamp:   int64(notification.Timestamp),
	}
	if queue.GetCurLength() > 0 {
		newSmsNotification.previous = queue.GetFirstNotificationId()
	}

	// Add the SMS notification to the queue
	newSmsNotificationId, err := store.addSmsNotification(&newSmsNotification)
	if err != nil {
		return err
	}

	// Update the first notification's 'next' field to point to our current one
	if queue.GetCurLength() > 0 {
		updatedData := map[string]interface{}{
			"next": newSmsNotificationId,
		}
		notificationsCollection := store.client.Collection("sms-notifications")
		_, err = notificationsCollection.Doc(queue.GetFirstNotificationId()).Set(context.Background(), updatedData, firestore.MergeAll)
	}

	// Update the queue's 'first_notification' and 'cur_length' field
	if queue.GetCurLength() == 0 {
		queue.SetLastNotificationId(newSmsNotificationId)
	}
	queue.SetFirstNotificationId(newSmsNotificationId)
	queue.SetCurLength(queue.GetCurLength() + 1)

	return queue.Commit()
}

func (store *FirestoreNotificationsStore) addSmsNotification(notification *smsNotification) (string, error) {
	newSmsNotification := map[string]interface{}{
		"next":     notification.next,
		"previous": notification.previous,
		"notification": map[string]interface{}{
			"contact_info": notification.contactInfo,
			"data":         notification.data,
			"timestamp":    notification.timestamp,
		},
	}

	// Add the notification to the collection
	notificationsCollection := store.client.Collection("sms-notifications")
	if doc, _, err := notificationsCollection.Add(context.Background(), newSmsNotification); err != nil {
		return "", err
	} else {
		return doc.ID, nil
	}
}

// Returns a list of at most X newest SMS notifications starting from (but not including) a starting notification id
// It will return an empty list if the starting notification is the newest notification
//
// Returns:
// 1. A list of SMS notifications
// 2. An error object, if an error occured; else nil
//
func (store *FirestoreNotificationsStore) GetNewSmsNotificationsFromUuid(deviceId string, numNotifications int, startingUuid string) ([]SmsNotification, error) {
	rule := func(sn *smsNotification) string {
		return sn.next
	}
	return store.createNotificationsIterator(rule)(startingUuid, numNotifications)
}

// Returns a list of at most X oldest SMS notifications starting from (but not including) a starting notification id
// It will return an empty list if the starting notification is the oldest notification
//
// Returns:
// 1. A list of SMS notifications
// 2. An error object, if an error occured; else nil
//
func (store *FirestoreNotificationsStore) GetPreviousSmsNotificationsFromUuid(deviceId string, numNotifications int, startingUuid string) ([]SmsNotification, error) {
	rule := func(sn *smsNotification) string {
		return sn.previous
	}
	return store.createNotificationsIterator(rule)(startingUuid, numNotifications)
}

type smsNotificationIteratorRule = func(*smsNotification) string
type smsNotificationsIterator = func(startingUuid string, numNotifications int) ([]SmsNotification, error)

func (store *FirestoreNotificationsStore) createNotificationsIterator(rule smsNotificationIteratorRule) smsNotificationsIterator {
	return func(startingUuid string, numNotifications int) ([]SmsNotification, error) {
		notification, err := store.getSmsNotification(startingUuid)
		if err != nil {
			return nil, err
		}

		lst := make([]SmsNotification, 0, numNotifications)

		for len(lst) < numNotifications && notification != nil {
			prevNotification, err := store.getSmsNotification(rule(notification))
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

// Get a SMS notification from an SMS notification id
//
// It returns two things:
// 1. *smsNotification:
//    a) nil, if it could not find the SMS notification, or
//    b) the SMS notification itself
// 2. error:
//    a) nil, if no error occured, or
//    b) the error object, if an error occured
//
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
