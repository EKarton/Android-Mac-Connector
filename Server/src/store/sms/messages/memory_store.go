package messages

import (
	"errors"
	"time"
)

type InMemoryMessagesStore struct {
	deviceIdToThreadIdsToThread  map[string](map[string]*SmsThread)
	deviceIdToThreadIdToMessages map[string](map[string]([]*SmsMessage))
}

func CreateInMemoryStore() *InMemoryMessagesStore {
	return &InMemoryMessagesStore{
		deviceIdToThreadIdsToThread:  make(map[string](map[string]*SmsThread)),
		deviceIdToThreadIdToMessages: make(map[string](map[string]([]*SmsMessage))),
	}
}

func (store *InMemoryMessagesStore) GetSmsThreads(deviceId string) ([]SmsThread, error) {
	threadIdToThread, ok := store.deviceIdToThreadIdsToThread[deviceId]

	if !ok {
		return nil, errors.New("Cannot find threads for device " + deviceId)
	}

	lst := make([]SmsThread, 0, 0)
	for _, thread := range threadIdToThread {
		lst = append(lst, *thread)
	}

	return lst, nil
}

func (store *InMemoryMessagesStore) UpdateSmsThread(deviceId string, threadId string, smsThread SmsThread) error {
	threadIdToThread, canFindDeviceId := store.deviceIdToThreadIdsToThread[deviceId]
	if !canFindDeviceId {
		return errors.New("Cannot find threads for device " + deviceId)
	}

	thread, canFindThread := threadIdToThread[threadId]
	if !canFindThread {
		newThread := SmsThread{
			ThreadId:            threadId,
			ContactName:         smsThread.ContactName,
			NumUnreadMessages:   smsThread.NumUnreadMessages,
			LastMessageBodySent: smsThread.LastMessageBodySent,
			LastTimeMessageSent: smsThread.LastTimeMessageSent,
			LastUpdated:         int(time.Now().Unix()),
		}
		threadIdToThread[threadId] = &newThread

	} else {
		thread.ContactName = smsThread.ContactName
		thread.NumUnreadMessages = smsThread.NumUnreadMessages
		thread.LastMessageBodySent = smsThread.LastMessageBodySent
		thread.LastTimeMessageSent = smsThread.LastTimeMessageSent
		thread.LastUpdated = int(time.Now().Unix())
	}

	return nil
}

func (store *InMemoryMessagesStore) GetSmsMessagesOfThread(deviceId string, threadId string) ([]SmsMessage, error) {
	threadIdsToMessages, foundDeviceId := store.deviceIdToThreadIdToMessages[deviceId]
	if !foundDeviceId {
		return nil, errors.New("Cannot find device id " + deviceId)
	}

	messages, foundThreadId := threadIdsToMessages[threadId]
	if !foundThreadId {
		return nil, errors.New("Cannot find thread id " + threadId)
	}

	lst := make([]SmsMessage, 0, 0)
	for _, message := range messages {
		lst = append(lst, *message)
	}

	return lst, nil
}

func (store *InMemoryMessagesStore) UpdateMessagesForSmsThread(deviceId string, threadId string, smsMessages []SmsMessage) error {
	threadIdsToMessages, foundDeviceId := store.deviceIdToThreadIdToMessages[deviceId]
	if !foundDeviceId {
		return errors.New("Cannot find device id " + deviceId)
	}

	threadIdsToMessages[threadId] = make([]*SmsMessage, len(smsMessages))

	for _, smsMessage := range smsMessages {
		threadIdsToMessages[threadId] = append(threadIdsToMessages[threadId], &smsMessage)
	}
	return nil
}
