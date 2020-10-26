package users

type InMemoryUsersStore struct {
}

func CreateInMemoryStore() *InMemoryUsersStore {
	store := InMemoryUsersStore{}
	return &store
}

func (store *InMemoryUsersStore) AddUser() error {
	return nil
}

func (store *InMemoryUsersStore) DeleteUser() error {
	return nil
}

func (store *InMemoryUsersStore) DoesUserExist() (bool, error) {
	return false, nil
}
