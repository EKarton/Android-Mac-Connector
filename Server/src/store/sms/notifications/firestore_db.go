package notifications

import (
	"Android-Mac-Connector-Server/src/store/sms/notifications/firebase"
	"errors"
	"fmt"
	"log"

	"cloud.google.com/go/firestore"
)

type SmsNotificationIteratorRule = func(*firebase.FirebaseNode) string
type SmsNotificationsIterator = func(startingUuid string, numNotifications int) ([]SmsNotification, error)

type FirestoreNotificationsStore struct {
	client               *firestore.Client
	firebaseQueueService *firebase.FirebaseQueueClient
	firebaseNodeService  *firebase.FirebaseNodeClient
	maxQueueLength       int
}

func CreateFirestoreNotificationsStore(client *firestore.Client, maxQueueLength int) *FirestoreNotificationsStore {
	return &FirestoreNotificationsStore{
		client:               client,
		firebaseQueueService: firebase.NewFirebaseQueueClient(client, "sms-notification-queues"),
		firebaseNodeService:  firebase.CreateFirebaseNodeClient(client, "sms-notifications"),
		maxQueueLength:       maxQueueLength,
	}
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
	smsNotification, err := store.firebaseNodeService.CreateNewNode("", "", map[string]interface{}{
		"contact_info": notification.ContactInfo,
		"data":         notification.Data,
		"timestamp":    int64(notification.Timestamp),
	})

	if err != nil {
		return err
	}

	// Update the first node's 'next' field to point to our new node
	if queue.GetCurLength() > 0 {
		smsNotification.SetPreviousNode(queue.GetFirstNotificationId())
		smsNotification.Commit()
	}

	// Update the first notification's 'next' field to point to our current one
	if queue.GetCurLength() > 0 {
		node, err := store.firebaseNodeService.GetNode(queue.GetFirstNotificationId())
		if err != nil {
			return err
		}

		node.SetNextNode(smsNotification.GetId())
		if err := node.Commit(); err != nil {
			return err
		}
	}

	// Update the queue's 'first_notification' and 'cur_length' field
	if queue.GetCurLength() == 0 {
		queue.SetLastNotificationId(smsNotification.GetId())
	}
	queue.SetFirstNotificationId(smsNotification.GetId())
	queue.SetCurLength(queue.GetCurLength() + 1)

	return queue.Commit()
}

// Returns a list of at most X newest SMS notifications starting from (but not including) a starting notification id
// It will return an empty list if the starting notification is the newest notification
//
// Returns:
// 1. A list of SMS notifications
// 2. An error object, if an error occured; else nil
//
func (store *FirestoreNotificationsStore) GetNewSmsNotificationsFromUuid(deviceId string, numNotifications int, startingUuid string) ([]SmsNotification, error) {
	rule := func(node *firebase.FirebaseNode) string {
		return node.GetNextNode()
	}
	return store.CreateNotificationsIterator(rule)(startingUuid, numNotifications)
}

// Returns a list of at most X oldest SMS notifications starting from (but not including) a starting notification id
// It will return an empty list if the starting notification is the oldest notification
//
// Returns:
// 1. A list of SMS notifications
// 2. An error object, if an error occured; else nil
//
func (store *FirestoreNotificationsStore) GetPreviousSmsNotificationsFromUuid(deviceId string, numNotifications int, startingUuid string) ([]SmsNotification, error) {
	rule := func(node *firebase.FirebaseNode) string {
		return node.GetPreviousNode()
	}
	return store.CreateNotificationsIterator(rule)(startingUuid, numNotifications)
}

func (store *FirestoreNotificationsStore) CreateNotificationsIterator(rule SmsNotificationIteratorRule) SmsNotificationsIterator {
	return func(startingUuid string, numNotifications int) ([]SmsNotification, error) {
		curNode, err := store.firebaseNodeService.GetNode(startingUuid)
		if err != nil {
			return nil, err
		}

		lst := make([]SmsNotification, 0, numNotifications)

		for len(lst) < numNotifications && curNode != nil {
			log.Println(curNode)
			nextNode, err := store.firebaseNodeService.GetNode(rule(curNode))
			if err != nil {
				return nil, err
			}
			if nextNode == nil {
				break
			}

			nodeData, isParsable := nextNode.GetData().(map[string]interface{})
			if !isParsable {
				return nil, errors.New(fmt.Sprintf("The data %s is not parsable with type (map[string]interface{})", nextNode.GetData()))
			}

			lst = append(lst, SmsNotification{
				Uuid:        nextNode.GetId(),
				ContactInfo: nodeData["contact_info"].(string),
				Data:        nodeData["data"].(string),
				Timestamp:   int(nodeData["timestamp"].(int64)),
			})

			curNode = nextNode
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
