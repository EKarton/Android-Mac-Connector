package messages

type SmsThread struct {
	ThreadId            string
	ContactName         string
	LastMessageId       string
	FirstMessageId      string
	LastMessageBodySent string
	LastTimeMessageSent int
	NumUnreadMessages   int
	LastUpdated         int
}

type SmsMessage struct {
	ThreadId    string
	MessageId   string
	PhoneNumber string
	ContactInfo string
	Body        string
	IsRead      bool
	LastUpdated int
}

type SmsMessagesStore interface {
	GetSmsThreads(deviceId string) ([]SmsThread, error)
	UpdateSmsThread(deviceId string, threadId string, smsThread SmsThread) error
	GetSmsMessagesOfThread(deviceId string, threadId string) ([]SmsMessage, error)
	UpdateMessagesForSmsThread(deviceId string, threadId string, smsMessages []SmsMessage) error
}
