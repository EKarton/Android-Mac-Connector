package jobs

import (
	"errors"

	uuid "github.com/google/uuid"
)

type InMemoryJobsStore struct {
	jobIdToJobStatus map[string]string
	jobIdToData      map[string](interface{})
}

func CreateInMemoryStore() *InMemoryJobsStore {
	store := InMemoryJobsStore{
		jobIdToJobStatus: make(map[string]string),
		jobIdToData:      make(map[string](interface{})),
	}

	return &store
}

func (store *InMemoryJobsStore) AddJob(status string, data interface{}) (string, error) {
	jobId := ""

	if uuid, err := uuid.NewRandom(); err != nil {
		return "", err

	} else {
		jobId = uuid.String()
	}

	store.jobIdToJobStatus[jobId] = status
	store.jobIdToData[jobId] = status
	return jobId, nil
}

func (store *InMemoryJobsStore) DeleteJob(jobId string) error {
	if _, ok := store.jobIdToJobStatus[jobId]; ok {
		delete(store.jobIdToJobStatus, jobId)
		return nil
	}

	return errors.New("Job ID " + jobId + " does not exist in InMemoryJobsStore")
}

func (store *InMemoryJobsStore) UpdateJobStatus(jobId string, newStatus string) error {
	if _, ok := store.jobIdToJobStatus[jobId]; ok {
		store.jobIdToJobStatus[jobId] = newStatus
		return nil
	}
	return errors.New("Job ID " + jobId + " does not exist in InMemoryJobsStore")
}

func (store *InMemoryJobsStore) UpdateJobData(jobId string, newData interface{}) error {
	if _, ok := store.jobIdToData[jobId]; ok {
		store.jobIdToData[jobId] = newData
		return nil
	}
	return errors.New("Job ID " + jobId + " does not exist in InMemoryJobsStore")
}

func (store *InMemoryJobsStore) GetJobStatus(jobId string) (string, error) {
	if _, ok := store.jobIdToJobStatus[jobId]; ok {
		jobStatus := store.jobIdToJobStatus[jobId]
		return jobStatus, nil
	}

	return "", errors.New("Job ID " + jobId + " does not exist in InMemoryJobsStore")
}

func (store *InMemoryJobsStore) GetJobData(jobId string) (interface{}, error) {
	if _, ok := store.jobIdToData[jobId]; ok {
		return store.jobIdToData[jobId], nil
	}

	return "", errors.New("Job ID " + jobId + " does not exist in InMemoryJobsStore")
}
