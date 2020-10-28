package devices

import (
	"fmt"
	"net/http"
)

type DeviceNotFoundError struct {
	deviceId string
}

func CreateDeviceNotFoundError(deviceId string) *DeviceNotFoundError {
	return &DeviceNotFoundError{deviceId}
}

func (err *DeviceNotFoundError) HttpCode() int {
	return http.StatusNotFound
}

func (err *DeviceNotFoundError) ErrorCode() string {
	return "DeviceNotFound"
}

func (err *DeviceNotFoundError) Error() string {
	return fmt.Sprintf("Device %s is not found", err.deviceId)
}

type DeviceAlreadyRegisteredError struct {
	userId     string
	deviceType string
	hardwareId string
}

func CreateDeviceAlreadyRegisteredError(userId string, deviceType string, hardwareId string) *DeviceAlreadyRegisteredError {
	return &DeviceAlreadyRegisteredError{
		userId:     userId,
		deviceType: deviceType,
		hardwareId: hardwareId,
	}
}

func (err *DeviceAlreadyRegisteredError) HttpCode() int {
	return http.StatusConflict
}

func (err *DeviceAlreadyRegisteredError) ErrorCode() string {
	return "DeviceAlreadyRegisteredError"
}

func (err *DeviceAlreadyRegisteredError) Error() string {
	return fmt.Sprintf("Device with user id %s, device type %s, and hardware id %s already exist", err.userId, err.deviceType, err.hardwareId)
}
