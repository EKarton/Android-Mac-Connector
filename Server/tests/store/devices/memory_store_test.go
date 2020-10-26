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

func TestAddDeviceAndGetDevice(t *testing.T) {
	store := devicesStore.CreateInMemoryStore()

	deviceId1, err1 := store.AddDevice(devicesStore.Device{
		Capabilities: []string{
			"read_sms",
			"send_sms",
		},
	})

	_, err2 := store.AddDevice(devicesStore.Device{
		Capabilities: []string{
			"read_sms2",
			"send_sms2",
		},
	})

	device, err3 := store.GetDevice(deviceId1)

	if err1 != nil {
		t.Errorf(err1.Error())
	} else if err2 != nil {
		t.Errorf(err2.Error())
	} else if err3 != nil {
		t.Errorf(err3.Error())
	}

	expectedDevice := devicesStore.Device{
		Capabilities: []string{
			"read_sms",
			"send_sms",
		},
	}

	if !reflect.DeepEqual(device, expectedDevice) {
		t.Error("Actual device ", device, "do not match", expectedDevice)
	}
}

func TestDeleteDevice(t *testing.T) {
	store := devicesStore.CreateInMemoryStore()
	deviceId, addErr := store.AddDevice(devicesStore.Device{
		Capabilities: []string{
			"read_sms2",
			"send_sms2",
		},
	})

	if addErr != nil {
		t.Error("AddDevice() should not return error")
	}

	deleteErr := store.DeleteDevice(deviceId)

	if deleteErr != nil {
		t.Error("DeleteDevice() should not return error")
	}

	_, getErr := store.GetDevice(deviceId)

	if getErr != nil {
		t.Error("GetDevice() should return error")
	}
}
