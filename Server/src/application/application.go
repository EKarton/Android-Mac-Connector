package application

import (
	"Android-Mac-Connector-Server/src/data/fcm"
	"Android-Mac-Connector-Server/src/store/devices"
	"Android-Mac-Connector-Server/src/store/jobs"
	"Android-Mac-Connector-Server/src/store/resourcepolicies"
	"Android-Mac-Connector-Server/src/store/sms_notifications"
)

type ApplicationContext struct {
	DataStores *DataStores
}

type DataStores struct {
	DevicesStores              devices.DevicesStore
	JobQueueService            jobs.JobQueueService
	ResourcePoliciesStore      resourcepolicies.ResourcePoliciesStore
	SmsNotifications           sms_notifications.SmsNotificationsStore
	SmsNotificationSubscribers *sms_notifications.SmsNotificationSubscribersStore
}

func CreateApplicationContext() *ApplicationContext {
	return &ApplicationContext{
		DataStores: createDatastore(),
	}
}

func createDatastore() *DataStores {
	smsNotificationsStore := sms_notifications.CreateFirestoreNotificationsStore(fcm.GetFirestoreClient(), 10)
	smsNotificationSubscribersStore := sms_notifications.CreateNotificationSubscribersStore(smsNotificationsStore)

	return &DataStores{
		DevicesStores:              devices.CreateFirestoreDevicesStore(fcm.GetFirestoreClient()),
		JobQueueService:            jobs.CreateFirebaseJobQueueService(fcm.GetFirestoreClient()),
		ResourcePoliciesStore:      resourcepolicies.CreateInMemoryStore(),
		SmsNotifications:           smsNotificationSubscribersStore,
		SmsNotificationSubscribers: smsNotificationSubscribersStore,
	}
}
