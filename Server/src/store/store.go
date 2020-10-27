package store

import (
	"Android-Mac-Connector-Server/src/store/devices"
	"Android-Mac-Connector-Server/src/store/jobs"
	"Android-Mac-Connector-Server/src/store/resourcepolicies"
	"Android-Mac-Connector-Server/src/store/sms/messages"
	"Android-Mac-Connector-Server/src/store/sms/notifications"
)

type Datastore struct {
	DevicesStores              devices.DevicesStore
	JobStatusStore             jobs.JobStatusStore
	ResourcePoliciesStore      resourcepolicies.ResourcePoliciesStore
	SmsMessagesStore           messages.SmsMessagesStore
	SmsNotifications           notifications.SmsNotificationsStore
	SmsNotificationSubscribers *notifications.SmsNotificationSubscribersStore
}

func CreateInMemoryDatastore() *Datastore {
	smsNotificationsStore := notifications.CreateInMemoryStore()

	return &Datastore{
		DevicesStores:              devices.CreateInMemoryStore(),
		JobStatusStore:             jobs.CreateInMemoryStore(),
		ResourcePoliciesStore:      resourcepolicies.CreateInMemoryStore(),
		SmsMessagesStore:           messages.CreateInMemoryStore(),
		SmsNotifications:           smsNotificationsStore,
		SmsNotificationSubscribers: notifications.CreateNotificationSubscribersStore(smsNotificationsStore),
	}
}
