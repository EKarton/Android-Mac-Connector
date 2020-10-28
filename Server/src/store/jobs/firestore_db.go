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

func CreateFirestoreJobsStore(client *firestore.Client) *FirestoreJobsStore {
	return &FirestoreJobsStore{
		client: client,
	}
}

func (store *FirestoreJobsStore) AddJob(status string) (string, error) {
	devicesCollection := store.client.Collection("jobs")
	doc, _, err := devicesCollection.Add(context.Background(), map[string]interface{}{
		"status": status,
	})

	if err != nil {
		return "", err
	}
	return doc.ID, nil
}

func (store *FirestoreJobsStore) DeleteJob(jobId string) error {
	devicesCollection := store.client.Collection("jobs")
	_, err := devicesCollection.Doc(jobId).Delete(context.Background())

	return err
}

func (store *FirestoreJobsStore) UpdateJobStatus(jobId string, newStatus string) error {
	devicesCollection := store.client.Collection("jobs")
	_, err := devicesCollection.Doc(jobId).Get(context.Background())

	if err != nil {
		if grpc.Code(err) == codes.NotFound {
			return errors.New(fmt.Sprintf("Job with id %s does not exist", jobId))
		}
		return err
	}

	updatedData := map[string]interface{}{
		"status": newStatus,
	}

	_, err = devicesCollection.Doc(jobId).Set(context.Background(), updatedData, firestore.MergeAll)

	return err
}

func (store *FirestoreJobsStore) GetJobStatus(jobId string) (string, error) {
	devicesCollection := store.client.Collection("jobs")
	doc, err := devicesCollection.Doc(jobId).Get(context.Background())

	if err != nil {
		if grpc.Code(err) == codes.NotFound {
			return "", nil
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
