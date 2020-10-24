package sms

import (
	"time"
)

type SmsMessage struct {
	messageId string
	address   string
	person    string
	body      string
	readState bool
	time      int
	msgType   string
}

var threadIdToSmsMessages = make(map[string][]SmsMessage)
var threadIdToLastTimeUpdated = make(map[string]int)

func GetSmsMessagesForThread(threadId string) []SmsMessage {
	return threadIdToSmsMessages[threadId]
}

func GetLastTimeSmsMessagesForThreadUpdated(threadId string) int {
	return threadIdToLastTimeUpdated[threadId]
}

func UpdateSmsMessagesForThread(threadId string, smsMessages []SmsMessage) {
	threadIdToSmsMessages[threadId] = smsMessages
	threadIdToLastTimeUpdated[threadId] = int(time.Now().Unix())
}
