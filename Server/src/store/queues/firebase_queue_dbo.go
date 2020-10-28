package queues

import (
	"context"
	"errors"

	"cloud.google.com/go/firestore"
	"google.golang.org/api/iterator"
)

type FirebaseQueueClient struct {
	firebaseClient         *firestore.Client
	firebaseCollectionName string
}

type FirebaseQueue struct {
	firebaseClient         *firestore.Client
	firebaseCollectionName string
	queueId                string
	deviceId               string
	firstNotificationId    string
	lastNotificationId     string
	curLength              int
	maxLength              int
	changedFieldNames      []string
}

func NewFirebaseQueueClient(client *firestore.Client, collectionName string) *FirebaseQueueClient {
	return &FirebaseQueueClient{
		firebaseClient:         client,
		firebaseCollectionName: collectionName,
	}
}

func (client *FirebaseQueueClient) GetOrCreateQueue(deviceId string, maxQueueLength int) (*FirebaseQueue, error) {
	queue, err := client.GetQueue(deviceId)
	if err != nil {
		return nil, err
	}

	if queue == nil {
		return client.CreateQueue(deviceId, maxQueueLength)
	}

	return queue, nil
}

func (client *FirebaseQueueClient) GetQueue(deviceId string) (*FirebaseQueue, error) {
	queuesCollection := client.firebaseClient.Collection(client.firebaseCollectionName)
	query := queuesCollection.Where("device_id", "==", deviceId)

	doc, err := query.Limit(1).Documents(context.Background()).Next()
	if err == iterator.Done {
		return nil, nil

	} else if err != nil {
		return nil, err
	}

	rawData := doc.Data()
	queue := FirebaseQueue{
		firebaseClient:         client.firebaseClient,
		firebaseCollectionName: client.firebaseCollectionName,
		queueId:                doc.Ref.ID,
		deviceId:               rawData["device_id"].(string),
		firstNotificationId:    rawData["first_notification_id"].(string),
		lastNotificationId:     rawData["last_notification_id"].(string),
		curLength:              int(rawData["cur_length"].(int64)),
		maxLength:              int(rawData["max_length"].(int64)),
		changedFieldNames:      make([]string, 0),
	}
	queue.queueId = doc.Ref.ID

	return &queue, nil
}

func (client *FirebaseQueueClient) CreateQueue(deviceId string, maxLength int) (*FirebaseQueue, error) {
	newQueue := map[string]interface{}{
		"first_notification_id": "",
		"last_notification_id":  "",
		"device_id":             deviceId,
		"cur_length":            0,
		"max_length":            maxLength,
	}

	queuesCollection := client.firebaseClient.Collection(client.firebaseCollectionName)
	queueId := ""
	if doc, _, err := queuesCollection.Add(context.Background(), newQueue); err != nil {
		return nil, err
	} else {
		queueId = doc.ID
	}

	queue := FirebaseQueue{
		firebaseClient:         client.firebaseClient,
		firebaseCollectionName: client.firebaseCollectionName,
		queueId:                queueId,
		deviceId:               deviceId,
		firstNotificationId:    "",
		lastNotificationId:     "",
		curLength:              0,
		maxLength:              maxLength,
		changedFieldNames:      make([]string, 0),
	}

	return &queue, nil
}

func (queue *FirebaseQueue) GetFirstNotificationId() string {
	return queue.firstNotificationId
}

func (queue *FirebaseQueue) SetFirstNotificationId(id string) {
	queue.firstNotificationId = id
	queue.changedFieldNames = append(queue.changedFieldNames, "firstNotificationId")
}

func (queue *FirebaseQueue) GetLastNotificationId() string {
	return queue.lastNotificationId
}

func (queue *FirebaseQueue) SetLastNotificationId(id string) {
	queue.lastNotificationId = id
	queue.changedFieldNames = append(queue.changedFieldNames, "lastNotificationId")
}

func (queue *FirebaseQueue) GetCurLength() int {
	return queue.curLength
}

func (queue *FirebaseQueue) SetCurLength(newLength int) {
	queue.curLength = newLength
	queue.changedFieldNames = append(queue.changedFieldNames, "curLength")
}

func (queue *FirebaseQueue) GetMaxLength() int {
	return queue.maxLength
}

func (queue *FirebaseQueue) SetMaxLength(newLength int) {
	queue.maxLength = newLength
	queue.changedFieldNames = append(queue.changedFieldNames, "maxLength")
}

func (queue *FirebaseQueue) Commit() error {
	updatedData := make(map[string]interface{})

	for _, changedFieldName := range queue.changedFieldNames {
		switch changedFieldName {
		case "firstNotificationId":
			updatedData["first_notification_id"] = queue.firstNotificationId
		case "lastNotificationId":
			updatedData["last_notification_id"] = queue.lastNotificationId
		case "curLength":
			updatedData["cur_length"] = int64(queue.curLength)
		case "maxLength":
			updatedData["max_length"] = int64(queue.maxLength)
		default:
			return errors.New("Cannot find field " + changedFieldName)
		}
	}

	queuesCollection := queue.firebaseClient.Collection(queue.firebaseCollectionName)
	_, err := queuesCollection.Doc(queue.queueId).Set(context.Background(), updatedData, firestore.MergeAll)
	return err
}
