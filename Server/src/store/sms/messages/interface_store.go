package messages

type SmsThread struct {
	ContactName         string
	LastMessageId       string
	FirstMessageId      string
	LastMessageBodySent string
	LastTimeMessageSent int
	NumUnreadMessages   int
}

type SmsMessage struct {
	ThreadId    string
	MessageId   string
	PhoneNumber string
	ContactInfo string
	Body        string
	IsRead      bool
}

type SmsMessagesStore interface {
	AddSmsThead(deviceId string, smsThread SmsThread) error
	GetSmsThreads(deviceId string) ([]string, error)
	GetSmsThread(threadId string) (SmsThread, error)
	DeleteSmsThread(threadId string) error
	AddSmsMessage(smsThread string, smsMessage SmsMessage) (string, error)
	GetSmsMessage(smsMessageId string) (SmsMessage, error)
	GetNewestSmsMessagesFromUuid(numSmsMessages int, startingSmsMessageId string) ([]SmsMessage, error)
	GetOldestSmsMessagesFromUuid(numSmsMessages int, startingSmsMessageId string) ([]SmsMessage, error)
}
