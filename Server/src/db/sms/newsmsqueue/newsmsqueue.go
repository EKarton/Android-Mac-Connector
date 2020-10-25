package newsmsqueue

import (
	"log"

	"github.com/google/uuid"
)

type SmsNotificationQueueNode struct {
	uuid       string
	smsMessage SmsMessageNotification
	previous   *SmsNotificationQueueNode
	next       *SmsNotificationQueueNode
}

type SmsNotificationQueue struct {
	firstNode   *SmsNotificationQueueNode
	lastNode    *SmsNotificationQueueNode
	uuidToNode  map[string]*SmsNotificationQueueNode
	curLength   int
	maxNumItems int
}

type SmsMessageNotification struct {
	ContactInfo string
	Data        string
	Timestamp   int
}

func generateRandomUuid() string {
	uuid, _ := uuid.NewRandom()
	return uuid.String()
}

/**
 * Creates a new queue
 */
func NewQueue(maxNumItems int) *SmsNotificationQueue {
	queue := SmsNotificationQueue{
		firstNode:   nil,
		lastNode:    nil,
		uuidToNode:  make(map[string]*SmsNotificationQueueNode),
		curLength:   0,
		maxNumItems: maxNumItems,
	}

	return &queue
}

/**
 * Adds a new entry to the top of the queue
 * It evicts entries at the bottom of the queue if the queue is empty
 */
func (queue *SmsNotificationQueue) Add(smsMessage SmsMessageNotification) string {
	uuid := generateRandomUuid()
	node := SmsNotificationQueueNode{
		uuid:       uuid,
		smsMessage: smsMessage,
		previous:   nil,
		next:       nil,
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
	queue.uuidToNode[uuid] = &node

	// Remove the last node if the num items in queue > max num items
	if queue.curLength > queue.maxNumItems {
		lastNodeUuid := queue.lastNode.uuid
		queue.Remove(lastNodeUuid)
	}

	return uuid
}

/**
 * Removes a message from the queue
 */
func (queue *SmsNotificationQueue) Remove(uuid string) {
	node := queue.uuidToNode[uuid]

	if node != nil {
		// Remove the node from the hashmap
		delete(queue.uuidToNode, uuid)

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
}

/**
 * Returns the length of the queue
 */
func (queue *SmsNotificationQueue) Len() int {
	return queue.curLength
}

/**
 * Retrieves the SMS message node for a given Uuid
 */
func (queue *SmsNotificationQueue) Get(uuid string) *SmsNotificationQueueNode {
	log.Println(queue.uuidToNode)
	return queue.uuidToNode[uuid]
}

/**
 * Returns the oldest element in the queue
 */
func (queue *SmsNotificationQueue) GetOldest() *SmsNotificationQueueNode {
	return queue.lastNode
}

/**
 * Returns the UUID of the Sms notification
 */
func (node *SmsNotificationQueueNode) Uuid() string {
	return node.uuid
}

/**
 * Get the next SMS notification
 * Returns nil if there is no next notification
 */
func (node *SmsNotificationQueueNode) Next() *SmsNotificationQueueNode {
	return node.next
}

/**
 * Get the previous SMS notification
 * Returns nil if there is no previous notification
 */
func (node *SmsNotificationQueueNode) Previous() *SmsNotificationQueueNode {
	return node.previous
}

/**
 * Get the message of the SMS notification
 */
func (node *SmsNotificationQueueNode) SmsMessage() SmsMessageNotification {
	return node.smsMessage
}
