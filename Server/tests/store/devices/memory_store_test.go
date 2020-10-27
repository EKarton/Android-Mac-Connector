package devices

import (
	"reflect"
	"testing"

	devicesStore "Android-Mac-Connector-Server/src/store/devices"
)

func TestCreateInMemoryStore(t *testing.T) {
	store := devicesStore.CreateInMemoryStore()
	if store == nil {
		t.Error("Store is nil")
	}

	var interfaceStore devicesStore.DevicesStore = store
	if interfaceStore == nil {
		t.Error("interfaceStore is nil")
	}
}

func TestDoesDeviceExist_ShouldReturnTrue_GivenDeviceIsRegistered(t *testing.T) {
	store := devicesStore.CreateInMemoryStore()
	store.RegisterDevice("userId", "android", "1234", make([]string, 0))
	isExist, err := store.DoesDeviceExist("userId", "android", "1234")

	if err != nil {
		t.Errorf("Error %s should not be returned", err.Error())
	}

	if !isExist {
		t.Error("isExist should be true")
	}
}

func TestDoesDeviceExist_ShouldReturnFalse_GivenHardwareIdDoesNotMatch(t *testing.T) {
	store := devicesStore.CreateInMemoryStore()
	store.RegisterDevice("userId", "android", "1234", make([]string, 0))
	isExist, err := store.DoesDeviceExist("userId", "android", "12345")

	if err != nil {
		t.Errorf("Error %s should not be returned", err.Error())
	}

	if isExist {
		t.Error("isExist should be false")
	}
}

func TestDoesDeviceExist_ShouldReturnFalse_GivenUserIdDoesNotMatch(t *testing.T) {
	store := devicesStore.CreateInMemoryStore()
	store.RegisterDevice("userId", "android", "1234", make([]string, 0))
	isExist, err := store.DoesDeviceExist("userId2", "android", "1234")

	if err != nil {
		t.Errorf("Error %s should not be returned", err.Error())
	}

	if isExist {
		t.Error("isExist should be false")
	}
}

func TestDoesDeviceExist_ShouldReturnFalse_GivenDeviceTypeDoesNotMatch(t *testing.T) {
	store := devicesStore.CreateInMemoryStore()
	store.RegisterDevice("userId", "android", "1234", make([]string, 0))
	isExist, err := store.DoesDeviceExist("userId", "mac", "1234")

	if err != nil {
		t.Errorf("Error %s should not be returned", err.Error())
	}

	if isExist {
		t.Error("isExist should be false")
	}
}

func TestRegisterDevice_ShouldReturnNewDeviceId_GivenDeviceIsNotRegisteredYet(t *testing.T) {
	store := devicesStore.CreateInMemoryStore()
	deviceId1, err1 := store.RegisterDevice("userId1", "android", "1234", make([]string, 0))
	deviceId2, err2 := store.RegisterDevice("userId2", "android", "1234", make([]string, 0))

	if err1 != nil {
		t.Errorf("Error %s should not be returned", err1.Error())
	}

	if err2 != nil {
		t.Errorf("Error %s should not be returned", err2.Error())
	}

	if deviceId1 == deviceId2 {
		t.Errorf("Device IDs %s %s should be different", deviceId1, deviceId2)
	}
}

func TestRegisterDevice_ShouldReturnError_GivenDeviceIsAlreadyRegistered(t *testing.T) {
	store := devicesStore.CreateInMemoryStore()
	store.RegisterDevice("userId1", "android", "1234", make([]string, 0))
	_, err := store.RegisterDevice("userId1", "android", "1234", make([]string, 0))

	if err == nil {
		t.Error("Error should be returned")
	}
}

func TestUpdateDeviceCapabilities_ShouldUpdateCapabilities_GivenValidDevice(t *testing.T) {
	store := devicesStore.CreateInMemoryStore()
	deviceId, _ := store.RegisterDevice("userId1", "android", "1234", make([]string, 0))
	err := store.UpdateDeviceCapabilities(deviceId, []string{"read_sms"})

	if err != nil {
		t.Errorf("Error %s should not be returned", err.Error())
	}

	capabilities, _ := store.GetDeviceCapabilities(deviceId)

	if reflect.DeepEqual(capabilities, []string{"read_sms"}) {
		t.Errorf("Capabilities %s should be read_sms", capabilities)
	}
}

func TestUpdateDeviceCapabilities_ShouldReturnError_GivenInvalidDeviceId(t *testing.T) {
	store := devicesStore.CreateInMemoryStore()
	err := store.UpdateDeviceCapabilities("1234", []string{"read_sms"})

	if err == nil {
		t.Error("Error should not be returned")
	}
}

func TestGetDeviceCapabilities_ShouldReturnEmptyCapabilities_GivenValidDeviceId(t *testing.T) {
	store := devicesStore.CreateInMemoryStore()
	deviceId, _ := store.RegisterDevice("user1", "android", "1234", make([]string, 0))
	capabilities, err := store.GetDeviceCapabilities(deviceId)

	if err != nil {
		t.Errorf("Error %s should not be thrown", err.Error())
	}

	if len(capabilities) > 0 {
		t.Errorf("Capabilities should be [], not %s", capabilities)
	}
}

func TestGetDeviceCapabilities_ShouldReturnError_GivenInvalidDeviceId(t *testing.T) {
	store := devicesStore.CreateInMemoryStore()
	_, err := store.GetDeviceCapabilities("1234")

	if err == nil {
		t.Error("Error should not be returned")
	}
}

func TestUpdatePushNotificationToken_ShouldReturnNoError_GivenValidDeviceId(t *testing.T) {
	store := devicesStore.CreateInMemoryStore()
	deviceId, _ := store.RegisterDevice("user1", "android", "1234", make([]string, 0))
	err := store.UpdatePushNotificationToken(deviceId, "token")

	if err != nil {
		t.Errorf("Error %s should not be thrown", err.Error())
	}
}

func TestUpdatePushNotificationToken_ShouldReturnError_GivenInvalidDeviceId(t *testing.T) {
	store := devicesStore.CreateInMemoryStore()
	err := store.UpdatePushNotificationToken("1234", "ha")

	if err == nil {
		t.Error("Error should not be returned")
	}
}
