package jobs

import (
	"errors"

	uuid "github.com/google/uuid"
)

type InMemoryJobsStore struct {
	jobIdToJobStatus map[string]string
}

func CreateInMemoryStore() *InMemoryJobsStore {
	store := InMemoryJobsStore{
		jobIdToJobStatus: make(map[string]string),
	}

	return &store
}

func (store *InMemoryJobsStore) AddJob(status string) (string, error) {
	jobId := ""

	if uuid, err := uuid.NewRandom(); err != nil {
		return "", err

	} else {
		jobId = uuid.String()
	}

	store.jobIdToJobStatus[jobId] = status
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

func (store *InMemoryJobsStore) GetJobStatus(jobId string) (string, error) {
	if _, ok := store.jobIdToJobStatus[jobId]; ok {
		jobStatus := store.jobIdToJobStatus[jobId]
		return jobStatus, nil
	}

	return "", errors.New("Job ID " + jobId + " does not exist in InMemoryJobsStore")
}
