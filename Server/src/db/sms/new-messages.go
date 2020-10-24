package sms

import (
	newSmsQueue "Android-Mac-Connector-Server/src/db/sms/newsmsqueue"
	"math"
)

type SmsMessageNotification struct {
	Uuid string
	newSmsQueue.SmsMessageNotification
}

var queue = newSmsQueue.NewQueue(1000)

func AddNewSmsMessageNotification(smsMsgNotification newSmsQueue.SmsMessageNotification) {
	queue.Add(smsMsgNotification)
}

func GetNotificationsFromUuid(startingUuid string, numNotificationsToFetch int) []SmsMessageNotification {
	node := queue.Get(startingUuid)

	if node == nil {
		node = queue.GetOldest()
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
