package messages

import (
	"time"
)

type SmsThread struct {
	threadId            string
	contactName         string
	lastMessageBodySent string
	lastTimeMessageSent int
	numUnreadMessages   int
}

type SmsMessage struct {
	messageId string
	address   string
	person    string
	body      string
	readState bool
	time      int
	msgType   string
}

var smsThreads = make([]SmsThread, 0, 0)
var lastTimeSmsThreadsUpdated = 0

var threadIdToSmsMessages = make(map[string][]SmsMessage)
var threadIdToLastTimeUpdated = make(map[string]int)

func GetSmsThreads() []SmsThread {
	return smsThreads
}

func UpdateSmsThreads(threads []SmsThread) {
	smsThreads = threads
	lastTimeSmsThreadsUpdated = int(time.Now().Unix())
	threadIdToSmsMessages = make(map[string][]SmsMessage)
	threadIdToLastTimeUpdated = make(map[string]int)

	for _, v := range threads {
		threadIdToSmsMessages[v.threadId] = make([]SmsMessage, 0, 0)
	}
}

func GetLastTimeSmsThreadsUpdated() int {
	return lastTimeSmsThreadsUpdated
}

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
