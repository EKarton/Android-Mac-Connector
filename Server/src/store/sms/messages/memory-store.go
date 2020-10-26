package messages

type InMemoryMessagesStore struct{}

func CreateInMemoryStore() *InMemoryMessagesStore {
	store := InMemoryMessagesStore{}
	return &store
}

func (store *InMemoryMessagesStore) AddSmsThead(deviceId string, smsThread SmsThread) error {
	return nil
}

func (store *InMemoryMessagesStore) GetSmsThreads(deviceId string) ([]string, error) {
	return nil
}

func (store *InMemoryMessagesStore) GetSmsThread(threadId string) (SmsThread, error) {
	return nil
}

func (store *InMemoryMessagesStore) DeleteSmsThread(threadId string) error {
	return nil
}

func (store *InMemoryMessagesStore) AddSmsMessage(smsThread string, smsMessage SmsMessage) (string, error) {
	return "", nil
}

func (store *InMemoryMessagesStore) GetSmsMessage(smsMessageId string) (SmsMessage, error) {
	return nil, nil
}

func (store *InMemoryMessagesStore) GetNewestSmsMessagesFromUuid(numSmsMessages int, startingSmsMessageId string) ([]SmsMessage, error) {
	return make([]SmsMessage, 0), nil
}

func (store *InMemoryMessagesStore) GetOldestSmsMessagesFromUuid(numSmsMessages int, startingSmsMessageId string) ([]SmsMessage, error) {
	return make([]SmsMessage, 0), nil
}
