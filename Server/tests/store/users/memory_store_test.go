package users

import (
	"reflect"
	"testing"

	usersStore "Android-Mac-Connector-Server/src/store/users"
)

func TestCreateInMemoryStore(t *testing.T) {
	store := usersStore.CreateInMemoryStore()
	if store == nil {
		t.Error("Store is nil")
	}
}

func TestAddUser_ShouldNotThrowError_WhenUniqueUserIdIsGiven(t *testing.T) {
	store := usersStore.CreateInMemoryStore()
	err1 := store.AddUser("user1")
	err2 := store.AddUser("user2")

	if err1 != nil || err2 != nil {
		t.Error("Threw error when it shouldn't be")
	}
}

func TestAddUser_ShouldThrowError_WhenSameUserIdIsGiven(t *testing.T) {
	store := usersStore.CreateInMemoryStore()
	err1 := store.AddUser("user1")
	err2 := store.AddUser("user1")

	if err1 != nil {
		t.Error("Should not return error the first time")
	}

	if err2 == nil {
		t.Error("Should return error the second time")
	}
}

func TestDeleteUser_ShouldNotThrowError_WhenUserIdIsValid(t *testing.T) {
	store := usersStore.CreateInMemoryStore()
	store.AddUser("Testing")
	err := store.DeleteUser("Testing")
	hasExist, _ := store.DoesUserExist("Testing")

	if err != nil {
		t.Error("Should not throw exception when deleting user")
	}

	if hasExist {
		t.Error("User should no longer exist")
	}
}

func TestDeleteUser_ShouldThrowError_WhenUserIdIsNotValid(t *testing.T) {
	store := usersStore.CreateInMemoryStore()
	err := store.DeleteUser("Testing")

	if err == nil {
		t.Error("Should throw exception")
	}
}

func TestDoesUserExist_ShouldReturnTrue_WhenUserIdIsValid(t *testing.T) {
	store := usersStore.CreateInMemoryStore()
	store.AddUser("user1")
	isExist, err := store.DoesUserExist("user1")

	if err != nil {
		t.Error("Should not return error")
	}

	if !isExist {
		t.Error("isExist should be true")
	}
}

func TestDoesUserExist_ShouldReturnTrue_WhenUserIdIsInvalid(t *testing.T) {
	store := usersStore.CreateInMemoryStore()
	isExist, err := store.DoesUserExist("user1")

	if err != nil {
		t.Error("Should not return error")
	}

	if isExist {
		t.Error("isExist should be false")
	}
}

func TestRegisterDeviceToUser_ShouldRegisterDevice_GivenValidUserIdAndNewDeviceId(t *testing.T) {
	store := usersStore.CreateInMemoryStore()
	store.AddUser("user1")
	err := store.RegisterDeviceToUser("user1", "device1")

	if err != nil {
		t.Error("Should not return error: " + err.Error())
	}
}

func TestRegisterDeviceToUser_ShouldRegisterDevice_GivenInvalidUserId(t *testing.T) {
	store := usersStore.CreateInMemoryStore()
	err := store.RegisterDeviceToUser("user1", "device1")

	if err == nil {
		t.Error("Should return user not found error")
	}
}

func TestRegisterDeviceToUser_ShouldRegisterDevice_GivenValidUserIdAndDuplicateDeviceId(t *testing.T) {
	store := usersStore.CreateInMemoryStore()
	store.AddUser("user1")
	store.RegisterDeviceToUser("user1", "device1")
	err := store.RegisterDeviceToUser("user1", "device1")

	if err == nil {
		t.Error("Should return device already added error")
	}
}

func TestUnregisterDeviceFromUser_ShouldReturnNoError_GivenValidUserIdAndValidDeviceId(t *testing.T) {
	store := usersStore.CreateInMemoryStore()
	store.AddUser("user1")
	store.RegisterDeviceToUser("user1", "device1")
	err := store.UnregisterDeviceFromUser("user1", "device1")

	if err != nil {
		t.Error("Should not return error")
	}
}

func TestUnregisterDeviceFromUser_ShouldReturnError_GivenInvalidUserId(t *testing.T) {
	store := usersStore.CreateInMemoryStore()
	err := store.UnregisterDeviceFromUser("user1", "device1")

	if err == nil {
		t.Error("Should return invalid user error")
	}
}

func TestUnregisterDeviceFromUser_ShouldReturnError_GivenValidUserIdAndInvalidDeviceId(t *testing.T) {
	store := usersStore.CreateInMemoryStore()
	store.AddUser("user1")
	store.RegisterDeviceToUser("user1", "device1")
	err := store.UnregisterDeviceFromUser("user1", "device2")

	if err == nil {
		t.Error("Should return invalid device error")
	}
}

func TestGetDevicesRegisteredToUser_ShouldReturnEmptyList_GivenValidUserIdAndNoDevicesRegisterd(t *testing.T) {
	store := usersStore.CreateInMemoryStore()
	store.AddUser("user1")
	devices, err := store.GetDevicesRegisteredToUser("user1")

	if err != nil {
		t.Error("Should not throw an error")
	}

	if len(devices) > 0 {
		t.Error("Should not have any devices registered")
	}
}

func TestGetDevicesRegisteredToUser_ShouldCorrectList_GivenValidUserIdAndManyDevicesRegisterd(t *testing.T) {
	store := usersStore.CreateInMemoryStore()
	store.AddUser("user1")
	store.RegisterDeviceToUser("user1", "device1")
	store.RegisterDeviceToUser("user1", "device2")
	devices, err := store.GetDevicesRegisteredToUser("user1")

	expectedDevices := []string{"device1", "device2"}

	if err != nil {
		t.Error("Should not throw an error")
	}

	if !reflect.DeepEqual(devices, expectedDevices) {
		t.Errorf("Actual device list %s does not match %s", devices, expectedDevices)
	}
}

func TestGetDevicesRegisteredToUser_ShouldThrowError_GivenInvalidUserId(t *testing.T) {
	store := usersStore.CreateInMemoryStore()
	_, err := store.GetDevicesRegisteredToUser("user1")

	if err == nil {
		t.Error("Should throw invalid user id error")
	}
}
