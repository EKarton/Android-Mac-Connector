package store

import (
	"Android-Mac-Connector-Server/src/data/fcm"
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
	var smsNotificationsStore notifications.SmsNotificationsStore = notifications.CreateFirestoreNotificationsStore(fcm.GetFirestoreClient(), 10)
	var smsNotificationSubscribersStore = notifications.CreateNotificationSubscribersStore(smsNotificationsStore)

	return &Datastore{
		DevicesStores:              devices.CreateFirestoreDevicesStore(fcm.GetFirestoreClient()),
		JobStatusStore:             jobs.CreateFirestoreJobsStore(fcm.GetFirestoreClient()),
		ResourcePoliciesStore:      resourcepolicies.CreateInMemoryStore(),
		SmsMessagesStore:           messages.CreateInMemoryStore(),
		SmsNotifications:           smsNotificationSubscribersStore,
		SmsNotificationSubscribers: smsNotificationSubscribersStore,
	}
}
