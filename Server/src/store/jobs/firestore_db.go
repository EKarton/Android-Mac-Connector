package jobs

import (
	"context"
	"errors"
	"fmt"

	"cloud.google.com/go/firestore"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
)

type FirestoreJobsStore struct {
	client *firestore.Client
}

// Creates a FirestoreJobsStore instance
// It needs a valid firestore client (refer to https://firebase.google.com/docs/firestore/quickstart)
//
// Returns a pointer to a FirestoreJobsStore instance
//
func CreateFirestoreJobsStore(client *firestore.Client) *FirestoreJobsStore {
	return &FirestoreJobsStore{
		client: client,
	}
}

// Adds a new job to the jobs store
//
// It returns two things:
// 1. the job id (string), and
// 2. an error, which is nil if no errors occured; else an error object is returned
//
func (store *FirestoreJobsStore) AddJob(status string, data interface{}) (string, error) {
	devicesCollection := store.client.Collection("jobs")
	doc, _, err := devicesCollection.Add(context.Background(), map[string]interface{}{
		"status": status,
		"data":   data,
	})

	if err != nil {
		return "", err
	}
	return doc.ID, nil
}

// Deletes a job from the job queue
// If the `jobId` does not exist in the job queue, it will not do anything
//
// It returns an error if an error occured, else nil
//
func (store *FirestoreJobsStore) DeleteJob(jobId string) error {
	devicesCollection := store.client.Collection("jobs")
	_, err := devicesCollection.Doc(jobId).Delete(context.Background())

	return err
}

// Updates the status of a job
// The `jobId` is the jobId returned from calling `store.AddJob()`
//
// It returns an error if an error occured; else nil
//
func (store *FirestoreJobsStore) UpdateJobStatus(jobId string, newStatus string) error {
	devicesCollection := store.client.Collection("jobs")
	_, err := devicesCollection.Doc(jobId).Get(context.Background())

	if err != nil {
		if grpc.Code(err) == codes.NotFound {
			return CreateJobNotFoundError(jobId)
		}
		return err
	}

	updatedData := map[string]interface{}{
		"status": newStatus,
	}

	_, err = devicesCollection.Doc(jobId).Set(context.Background(), updatedData, firestore.MergeAll)

	return err
}

func (store *FirestoreJobsStore) UpdateJobData(jobId string, newData interface{}) error {
	devicesCollection := store.client.Collection("jobs")
	_, err := devicesCollection.Doc(jobId).Get(context.Background())

	if err != nil {
		if grpc.Code(err) == codes.NotFound {
			return CreateJobNotFoundError(jobId)
		}
		return err
	}

	updatedData := map[string]interface{}{
		"data": newData,
	}

	_, err = devicesCollection.Doc(jobId).Set(context.Background(), updatedData, firestore.MergeAll)

	return err
}

// Gets the current job status
// The `jobId` is the jobId returned from calling `store.AddJob()`
//
// It returns two things:
// 1. The current job status (string), and
// 2. An error, which is either nil (if no error occured), or an error object
//
func (store *FirestoreJobsStore) GetJobStatus(jobId string) (string, error) {
	devicesCollection := store.client.Collection("jobs")
	doc, err := devicesCollection.Doc(jobId).Get(context.Background())

	if err != nil {
		if grpc.Code(err) == codes.NotFound {
			return "", CreateJobNotFoundError(jobId)
		}
		return "", err
	}

	result, err := doc.DataAt("status")
	if err != nil {
		return "", nil
	}

	jobStatus, isString := result.(string)
	if !isString {
		return "", errors.New(fmt.Sprintf("The field 'status' for job %s is not a string!", jobId))
	}

	return jobStatus, nil
}

func (store *FirestoreJobsStore) GetJobData(jobId string) (interface{}, error) {
	devicesCollection := store.client.Collection("jobs")
	doc, err := devicesCollection.Doc(jobId).Get(context.Background())

	if err != nil {
		if grpc.Code(err) == codes.NotFound {
			return "", CreateJobNotFoundError(jobId)
		}
		return "", err
	}

	data, err := doc.DataAt("data")
	if err != nil {
		return "", err
	}
	return data, nil
}
