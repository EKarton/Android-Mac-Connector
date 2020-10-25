package notifications

import (
	"log"
	"math"

	"github.com/google/uuid"
)

type Subscriber struct {
	channel chan []SmsMessageNotification
}

type SmsMessageNotification struct {
	Uuid string
	SmsMessage
}

var queue = NewQueue(1000)
var uuidToSubscribers = make(map[string]*Subscriber)

func AddNewSmsMessageNotification(smsMessage SmsMessage) {

	// Add it to the queue
	uuid := queue.Add(smsMessage)

	// Notify the channels
	for _, subscriber := range uuidToSubscribers {
		var notifications [1]SmsMessageNotification
		notifications[0] = SmsMessageNotification{
			uuid,
			smsMessage,
		}

		// We send notifications in parallel
		go func(channel chan []SmsMessageNotification) {
			channel <- notifications[:]
		}(subscriber.channel)
	}
}

func GetNotificationsFromUuid(startingUuid string, numNotificationsToFetch int) []SmsMessageNotification {
	var node *SmsNotificationQueueNode = nil

	if foundNode := queue.Get(startingUuid); foundNode != nil {
		node = foundNode.Next()
	} else if oldestNode := queue.GetOldest(); oldestNode != nil {
		node = oldestNode
	}

	numToFetch := int(math.Min(float64(numNotificationsToFetch), float64(queue.Len())))
	lst := make([]SmsMessageNotification, 0, numToFetch)

	for node != nil {
		lstItem := SmsMessageNotification{node.Uuid(), node.SmsMessage()}
		lst = append(lst, lstItem)
		node = node.Next()
	}

	return lst
}

func SubscribeToNewNotifications(channel chan []SmsMessageNotification) string {
	newUuid := generateRandomUuid()
	subscriber := Subscriber{
		channel,
	}
	uuidToSubscribers[newUuid] = &subscriber
	return newUuid
}

func UnsubscribeToNewNotifications(subscriberUuid string) {
	delete(uuidToSubscribers, subscriberUuid)
}

func generateRandomUuid() string {
	newUuid := ""
	if newUuidResult, err := uuid.NewRandom(); err != nil {
		log.Fatalln("Unable to get UUID for subscriber")
	} else {
		newUuid = newUuidResult.String()
	}
	return newUuid
}
