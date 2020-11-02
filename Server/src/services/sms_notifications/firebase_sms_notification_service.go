package sms_notifications

import (
	"Android-Mac-Connector-Server/src/store/queues"
	"errors"
	"fmt"

	"cloud.google.com/go/firestore"
)

type FirebaseSmsNotificationService struct {
	client               *firestore.Client
	firebaseQueueService *queues.FirebaseQueueClient
	firebaseNodeService  *queues.FirebaseNodeClient
	maxQueueLength       int
}

func CreateFirebaseSmsNotificationService(client *firestore.Client, maxQueueLength int) *FirebaseSmsNotificationService {
	return &FirebaseSmsNotificationService{
		client:               client,
		firebaseQueueService: queues.NewFirebaseQueueClient(client, "sms-notification-queues"),
		firebaseNodeService:  queues.CreateFirebaseNodeClient(client, "sms-notifications"),
		maxQueueLength:       maxQueueLength,
	}
}

// Adds a SMS notification to the queue
// It will create a queue if no queue with the matching device id is found
// It will also remove the oldest messages if the queue length exceeds the max length
//
// It returns an error if an error occured; else nil
//
func (store *FirebaseSmsNotificationService) AddSmsNotification(deviceId string, notification SmsNotification) (string, error) {
	// Get our queue
	queue, err := store.firebaseQueueService.GetOrCreateQueue(deviceId, 1000)
	if err != nil {
		return "", err
	}

	// Set the previous node to point to the cur queue's first node
	smsNotificationPrevious := ""
	if queue.GetCurLength() > 0 {
		smsNotificationPrevious = queue.GetFirstNotificationId()
	}

	// Create a SMS notification
	smsNotification, err := store.firebaseNodeService.CreateNewNode("", smsNotificationPrevious, map[string]interface{}{
		"contact_info": notification.ContactInfo,
		"data":         notification.Data,
		"timestamp":    int64(notification.Timestamp),
	})

	if err != nil {
		return "", err
	}

	// Update the first notification's 'next' field to point to our current one
	if queue.GetCurLength() > 0 {
		node, err := store.firebaseNodeService.GetNode(queue.GetFirstNotificationId())
		if err != nil {
			return "", err
		}

		node.SetNextNode(smsNotification.GetId())
		if err := node.Commit(); err != nil {
			return "", err
		}
	}

	// Update the queue's 'first_notification' and 'cur_length' field
	if queue.GetCurLength() == 0 {
		queue.SetLastNotificationId(smsNotification.GetId())
	}
	queue.SetFirstNotificationId(smsNotification.GetId())
	queue.SetCurLength(queue.GetCurLength() + 1)

	if err := queue.Commit(); err != nil {
		return "", err
	}
	return smsNotification.GetId(), nil
}

// Returns a list of at most X newest SMS notifications starting from (but not including) a starting notification id
// It will return an empty list if the starting notification is the newest notification
//
// Returns:
// 1. A list of SMS notifications
// 2. An error object, if an error occured; else nil
//
func (store *FirebaseSmsNotificationService) GetNewSmsNotificationsFromUuid(deviceId string, numNotifications int, startingUuid string) ([]SmsNotification, error) {
	// Create our iterator
	rule := func(node *queues.FirebaseNode) string {
		return node.GetNextNode()
	}
	iterator := queues.CreateNodeIterator(store.firebaseNodeService, rule)

	// Get the starting node
	startingNode, err := store.firebaseNodeService.GetNode(startingUuid)
	if err != nil {
		return nil, err
	}

	// Iterate through the nodes
	nodes, err := iterator(startingNode.GetNextNode(), numNotifications)
	if err != nil {
		return nil, err
	}

	return store.parseNodesData(nodes)
}

// Returns a list of at most X oldest SMS notifications starting from (but not including) a starting notification id
// It will return an empty list if the starting notification is the oldest notification
//
// Returns:
// 1. A list of SMS notifications
// 2. An error object, if an error occured; else nil
//
func (store *FirebaseSmsNotificationService) GetPreviousSmsNotificationsFromUuid(deviceId string, numNotifications int, startingUuid string) ([]SmsNotification, error) {
	// Create our iterator
	rule := func(node *queues.FirebaseNode) string {
		return node.GetPreviousNode()
	}
	iterator := queues.CreateNodeIterator(store.firebaseNodeService, rule)

	// Get the starting node
	startingNode, err := store.firebaseNodeService.GetNode(startingUuid)
	if err != nil {
		return nil, err
	}

	// Iterate through the nodes
	nodes, err := iterator(startingNode.GetNextNode(), numNotifications)
	if err != nil {
		return nil, err
	}

	return store.parseNodesData(nodes)
}

func (store *FirebaseSmsNotificationService) GetOldestSmsNotification(deviceId string) (SmsNotification, error) {
	queue, err := store.firebaseQueueService.GetQueue(deviceId)
	if err != nil {
		return SmsNotification{}, err
	}
	if queue == nil {
		return SmsNotification{}, nil
	}

	node, err := store.firebaseNodeService.GetNode(queue.GetLastNotificationId())
	if err != nil {
		return SmsNotification{}, err
	}
	if node == nil {
		return SmsNotification{}, nil
	}

	return store.parseNodeData(node)
}

func (store *FirebaseSmsNotificationService) GetLatestSmsNotification(deviceId string) (SmsNotification, error) {
	queue, err := store.firebaseQueueService.GetQueue(deviceId)
	if err != nil {
		return SmsNotification{}, err
	}
	if queue == nil {
		return SmsNotification{}, nil
	}

	node, err := store.firebaseNodeService.GetNode(queue.GetFirstNotificationId())
	if err != nil {
		return SmsNotification{}, err
	}
	if node == nil {
		return SmsNotification{}, nil
	}

	return store.parseNodeData(node)
}

func (store *FirebaseSmsNotificationService) parseNodesData(nodes [](*queues.FirebaseNode)) ([]SmsNotification, error) {
	smsNotifications := make([]SmsNotification, 0)
	for _, node := range nodes {
		smsNotification, err := store.parseNodeData(node)
		if err != nil {
			return nil, err
		}

		smsNotifications = append(smsNotifications, smsNotification)
	}

	return smsNotifications, nil
}

func (store *FirebaseSmsNotificationService) parseNodeData(node *queues.FirebaseNode) (SmsNotification, error) {
	nodeData, isParsable := node.GetData().(map[string]interface{})
	if !isParsable {
		return SmsNotification{}, errors.New(fmt.Sprintf("The data %s is not parsable with type (map[string]interface{})", node.GetData()))
	}
	smsNotification := SmsNotification{
		Uuid:        node.GetId(),
		ContactInfo: nodeData["contact_info"].(string),
		Data:        nodeData["data"].(string),
		Timestamp:   int(nodeData["timestamp"].(int64)),
	}

	return smsNotification, nil
}