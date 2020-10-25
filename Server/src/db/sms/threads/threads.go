package threads

import (
	"errors"
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

func GetSmsMessagesForThread(threadId string) ([]SmsMessage, error) {
	if messages, ok := threadIdToSmsMessages[threadId]; ok {
		return messages, nil
	}

	return nil, errors.New("CannotFindThreadIdError")
}

func GetLastTimeSmsMessagesForThreadUpdated(threadId string) (int, error) {
	if lastTime, ok := threadIdToLastTimeUpdated[threadId]; ok {
		return lastTime, nil
	}
	return 0, errors.New("CannotFindThreadIdError")
}

func UpdateSmsMessagesForThread(threadId string, smsMessages []SmsMessage) {
	threadIdToSmsMessages[threadId] = smsMessages
	threadIdToLastTimeUpdated[threadId] = int(time.Now().Unix())
}
