package store

import (
	"Android-Mac-Connector-Server/src/store/devices"
	"Android-Mac-Connector-Server/src/store/jobs"
	"Android-Mac-Connector-Server/src/store/resourcepolicies"
	"Android-Mac-Connector-Server/src/store/sms/messages"
	"Android-Mac-Connector-Server/src/store/sms/notifications"
)

type Datastore struct {
	DevicesStores         devices.DevicesStore
	JobStatusStore        jobs.JobStatusStore
	ResourcePoliciesStore resourcepolicies.ResourcePoliciesStore
	SmsMessagesStore      messages.SmsMessagesStore
	SmsNotificationsStore notifications.SmsNotificationsStore
}

func CreateInMemoryDatastore() *Datastore {
	return &Datastore{
		DevicesStores:         devices.CreateInMemoryStore(),
		JobStatusStore:        jobs.CreateInMemoryStore(),
		ResourcePoliciesStore: resourcepolicies.CreateInMemoryStore(),
		SmsMessagesStore:      messages.CreateInMemoryStore(),
		SmsNotificationsStore: notifications.CreateInMemoryStore(),
	}
}
