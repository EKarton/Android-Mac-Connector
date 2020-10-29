package store

import (
	"context"
	"errors"
	"fmt"

	"cloud.google.com/go/firestore"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
)

type FirebaseJobQueue struct {
	client   *FirebaseJobQueueService
	deviceId string
}

type FirebaseJobQueueService struct {
	client *firestore.Client
}

func CreateFirebaseJobQueueService(client *firestore.Client) *FirebaseJobQueueService {
	return &FirebaseJobQueueService{
		client: client,
	}
}

func (client *FirebaseJobQueueService) GetQueue(deviceId string) (JobQueue, error) {
	queue := FirebaseJobQueue{
		client:   client,
		deviceId: deviceId,
	}
	return &queue, nil
}

func (queue *FirebaseJobQueue) AddJob() (string, error) {
	devicesCollection := queue.getCollection()
	doc, _, err := devicesCollection.Add(context.Background(), map[string]interface{}{
		"status":  "pending",
		"results": nil,
	})

	if err != nil {
		return "", err
	}
	return doc.ID, nil
}

func (queue *FirebaseJobQueue) DeleteJob(jobId string) error {
	devicesCollection := queue.getCollection()
	_, err := devicesCollection.Doc(jobId).Delete(context.Background())

	return err
}

func (queue *FirebaseJobQueue) UpdateJob(jobId string, newStatus string, results interface{}) error {
	devicesCollection := queue.getCollection()
	_, err := devicesCollection.Doc(jobId).Get(context.Background())

	if err != nil {
		if grpc.Code(err) == codes.NotFound {
			return CreateJobNotFoundError(jobId)
		}
		return err
	}

	updatedData := map[string]interface{}{
		"status":  newStatus,
		"results": results,
	}

	_, err = devicesCollection.Doc(jobId).Set(context.Background(), updatedData, firestore.MergeAll)

	return err
}

func (queue *FirebaseJobQueue) GetJobStatus(jobId string) (string, error) {
	devicesCollection := queue.getCollection()
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

func (queue *FirebaseJobQueue) GetJobResults(jobId string) (interface{}, error) {
	devicesCollection := queue.getCollection()
	doc, err := devicesCollection.Doc(jobId).Get(context.Background())

	if err != nil {
		if grpc.Code(err) == codes.NotFound {
			return "", CreateJobNotFoundError(jobId)
		}
		return "", err
	}

	return doc.DataAt("results")
}

func (queue *FirebaseJobQueue) getCollection() *firestore.CollectionRef {
	collectionName := queue.getCollectionName()
	return queue.client.client.Collection(collectionName)
}

func (queue *FirebaseJobQueue) getCollectionName() string {
	return fmt.Sprintf("devices/%s/job-queue", queue.deviceId)
}
