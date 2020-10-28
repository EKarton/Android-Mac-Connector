package notifications

import (
	"errors"
	"math"

	"github.com/google/uuid"
)

type SmsNotificationQueueNode struct {
	uuid         string
	notification SmsNotification
	previous     *SmsNotificationQueueNode
	next         *SmsNotificationQueueNode
}

type SmsNotificationQueue struct {
	firstNode   *SmsNotificationQueueNode
	lastNode    *SmsNotificationQueueNode
	uuidToNode  map[string]*SmsNotificationQueueNode
	curLength   int
	maxNumItems int
}

type InMemoryNotificationsStore struct {
	deviceIdToQueue map[string]*SmsNotificationQueue
	maxNumItems     int
}

func CreateInMemoryStore(maxNumItems int) *InMemoryNotificationsStore {
	return &InMemoryNotificationsStore{
		deviceIdToQueue: make(map[string]*SmsNotificationQueue),
		maxNumItems:     maxNumItems,
	}
}

func (store *InMemoryNotificationsStore) createSmsNotificationQueue(deviceId string) {
	store.deviceIdToQueue[deviceId] = &SmsNotificationQueue{
		firstNode:   nil,
		lastNode:    nil,
		uuidToNode:  make(map[string]*SmsNotificationQueueNode),
		curLength:   0,
		maxNumItems: store.maxNumItems,
	}
}

func (store *InMemoryNotificationsStore) doesSmsNotificationQueueExist(deviceId string) bool {
	_, ok := store.deviceIdToQueue[deviceId]
	return ok
}

func (store *InMemoryNotificationsStore) getOrCreateSmsNotificationQueue(deviceId string) *SmsNotificationQueue {
	if !store.doesSmsNotificationQueueExist(deviceId) {
		store.createSmsNotificationQueue(deviceId)
	}
	return store.deviceIdToQueue[deviceId]
}

func (store *InMemoryNotificationsStore) AddSmsNotification(deviceId string, notification SmsNotification) error {
	queue := store.getOrCreateSmsNotificationQueue(deviceId)

	nodeUuid := ""
	if uuid, err := uuid.NewRandom(); err != nil {
		return err
	} else {
		nodeUuid = uuid.String()
	}

	node := SmsNotificationQueueNode{
		uuid:         nodeUuid,
		notification: notification,
		previous:     nil,
		next:         nil,
	}

	// Add the node to the doubly linked list
	if queue.firstNode == nil {
		queue.firstNode = &node
		queue.lastNode = &node
		queue.curLength = 1

	} else {
		queue.firstNode.next = &node
		node.previous = queue.firstNode
		queue.firstNode = &node
		queue.curLength += 1
	}

	// Add the node to the hashmap
	queue.uuidToNode[nodeUuid] = &node

	// Remove the last node if the num items in queue > max num items
	if queue.curLength > queue.maxNumItems {
		lastNodeUuid := queue.lastNode.uuid

		if err := store.RemoveSmsNotification(deviceId, lastNodeUuid); err != nil {
			return err
		}
	}

	return nil
}

func (store *InMemoryNotificationsStore) GetNewSmsNotificationsFromUuid(deviceId string, numNotifications int, startingUuid string) ([]SmsNotification, error) {
	queue, isExist := store.deviceIdToQueue[deviceId]
	if !isExist {
		return nil, errors.New("Queue for device id " + deviceId + " does not exist")
	}

	var node *SmsNotificationQueueNode = nil
	if foundNode, isFound := queue.uuidToNode[startingUuid]; isFound {
		node = foundNode.next
	} else if oldestNode := queue.lastNode; oldestNode != nil {
		node = oldestNode
	}

	numToFetch := int(math.Min(float64(numNotifications), float64(queue.curLength)))
	lst := make([]SmsNotification, 0, numToFetch)

	for node != nil {
		lst = append(lst, node.notification)
		node = node.next
	}

	return lst, nil
}

func (store *InMemoryNotificationsStore) GetPreviousSmsNotificationsFromUuid(deviceId string, numNotifications int, startingUuid string) ([]SmsNotification, error) {
	queue, isExist := store.deviceIdToQueue[deviceId]
	if !isExist {
		return nil, errors.New("Queue for device id " + deviceId + " does not exist")
	}

	var node *SmsNotificationQueueNode = nil
	if foundNode, isFound := queue.uuidToNode[startingUuid]; isFound {
		node = foundNode.previous
	} else if newestNode := queue.firstNode; newestNode != nil {
		node = newestNode
	}

	numToFetch := int(math.Min(float64(numNotifications), float64(queue.curLength)))
	lst := make([]SmsNotification, 0, numToFetch)

	for node != nil {
		lst = append(lst, node.notification)
		node = node.previous
	}

	return lst, nil
}

func (store *InMemoryNotificationsStore) GetOldestSmsNotification(deviceId string) (SmsNotification, error) {
	queue, isExist := store.deviceIdToQueue[deviceId]
	if !isExist {
		return SmsNotification{}, errors.New("Queue for device id " + deviceId + " does not exist")
	}

	if queue.lastNode != nil {
		return queue.lastNode.notification, nil
	}
	return SmsNotification{}, errors.New("There are no notifications in this queue!")
}

func (store *InMemoryNotificationsStore) GetLatestSmsNotification(deviceId string) (SmsNotification, error) {
	queue, isExist := store.deviceIdToQueue[deviceId]
	if !isExist {
		return SmsNotification{}, errors.New("Queue for device id " + deviceId + " does not exist")
	}

	if queue.firstNode != nil {
		return queue.firstNode.notification, nil
	}
	return SmsNotification{}, errors.New("There are no notifications in this queue!")
}

func (store *InMemoryNotificationsStore) RemoveSmsNotification(deviceId string, nodeUuid string) error {
	if !store.doesSmsNotificationQueueExist(deviceId) {
		return errors.New("Queue for device id " + deviceId + " does not exist")
	}

	queue := store.deviceIdToQueue[deviceId]
	node := queue.uuidToNode[nodeUuid]

	if node != nil {
		// Remove the node from the hashmap
		delete(queue.uuidToNode, nodeUuid)

		// Remove the node from the linked list
		if node.next == nil && node.previous == nil {
			queue.firstNode = nil
			queue.lastNode = nil

		} else if node.next == nil {
			node.previous.next = nil
			queue.firstNode = node.previous

		} else if node.previous == nil {
			node.next.previous = nil
			queue.lastNode = node.next

		} else {
			node.next.previous = node.previous
			node.previous.next = node.next
		}
	}
	return nil
}
