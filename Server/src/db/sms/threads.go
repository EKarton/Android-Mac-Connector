package sms

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

var smsThreads = make([]SmsThread, 0, 0)
var lastTimeSmsThreadsUpdated = 0

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
