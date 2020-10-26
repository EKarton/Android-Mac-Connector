package users

type UsersStore interface {
	AddUser() error
	DeleteUser() error
	DoesUserExist() (bool, error)
}
