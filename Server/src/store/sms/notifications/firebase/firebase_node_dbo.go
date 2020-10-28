package firebase

import (
	"context"
	"errors"

	"cloud.google.com/go/firestore"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
)

type FirebaseNodeClient struct {
	firebaseClient         *firestore.Client
	firebaseCollectionName string
}

type FirebaseNode struct {
	client            *FirebaseNodeClient
	id                string
	next              string
	previous          string
	data              interface{}
	changedFieldNames []string
}

func CreateFirebaseNodeClient(client *firestore.Client, collectionName string) *FirebaseNodeClient {
	return &FirebaseNodeClient{
		firebaseClient:         client,
		firebaseCollectionName: collectionName,
	}
}

func (client *FirebaseNodeClient) GetNode(nodeId string) (*FirebaseNode, error) {
	if nodeId == "" {
		return nil, nil
	}

	nodesCollection := client.firebaseClient.Collection(client.firebaseCollectionName)
	nodeDoc, err := nodesCollection.Doc(nodeId).Get(context.Background())
	if err != nil {

		// If it could not find the notification
		if grpc.Code(err) == codes.NotFound {
			return nil, nil
		}

		return nil, err
	}

	nodeRawData := nodeDoc.Data()
	node := FirebaseNode{
		client:   client,
		id:       nodeDoc.Ref.ID,
		next:     "",
		previous: "",
		data:     nodeRawData["data"],
	}

	if next, isString := nodeRawData["next"].(string); isString {
		node.next = next
	}
	if previous, isString := nodeRawData["previous"].(string); isString {
		node.previous = previous
	}

	return &node, nil
}

func (client *FirebaseNodeClient) CreateNewNode(next string, previous string, data interface{}) (*FirebaseNode, error) {
	newNode := map[string]interface{}{
		"next":     next,
		"previous": previous,
		"data":     data,
	}

	nodesCollection := client.firebaseClient.Collection(client.firebaseCollectionName)
	nodeId := ""
	if doc, _, err := nodesCollection.Add(context.Background(), newNode); err != nil {
		return nil, err
	} else {
		nodeId = doc.ID
	}

	typedNode := FirebaseNode{
		client:            client,
		id:                nodeId,
		next:              next,
		previous:          previous,
		data:              data,
		changedFieldNames: make([]string, 0),
	}
	return &typedNode, nil
}

func (node *FirebaseNode) GetId() string {
	return node.id
}

func (node *FirebaseNode) GetNextNode() string {
	return node.next
}

func (node *FirebaseNode) SetNextNode(nodeId string) {
	node.next = nodeId
	node.changedFieldNames = append(node.changedFieldNames, "next")
}

func (node *FirebaseNode) GetPreviousNode() string {
	return node.previous
}

func (node *FirebaseNode) SetPreviousNode(nodeId string) {
	node.previous = nodeId
	node.changedFieldNames = append(node.changedFieldNames, "previous")
}

func (node *FirebaseNode) GetData() interface{} {
	return node.data
}

func (node *FirebaseNode) SetData(newData interface{}) {
	node.data = newData
	node.changedFieldNames = append(node.changedFieldNames, "data")
}

func (node *FirebaseNode) Commit() error {
	updatedData := make(map[string]interface{})

	for _, changedFieldName := range node.changedFieldNames {
		switch changedFieldName {
		case "next":
			updatedData["next"] = node.next
		case "previous":
			updatedData["previous"] = node.previous
		case "data":
			updatedData["data"] = node.data
		default:
			return errors.New("Cannot find field " + changedFieldName)
		}
	}

	nodesCollection := node.client.firebaseClient.Collection(node.client.firebaseCollectionName)
	_, err := nodesCollection.Doc(node.id).Set(context.Background(), updatedData, firestore.MergeAll)
	return err
}
