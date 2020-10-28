package firebase

type NodeIteratorRule = func(*FirebaseNode) string
type NodeIterator = func(startingNodeId string, numNodesToFetch int) ([](*FirebaseNode), error)

// Creates a new iterator which can be used to iterate over the nodes in a queue in both directions
// It takes in a 'rule' which takes in the current node, and outputs the id of the next node
//
// Returns an iterator, which can be run by specifying the id of the starting node and the number of nodes to fetch
//
func CreateNodeIterator(service *FirebaseNodeClient, rule NodeIteratorRule) NodeIterator {
	return func(startingNodeId string, numNodesToFetch int) ([](*FirebaseNode), error) {
		curNode, err := service.GetNode(startingNodeId)
		if err != nil {
			return nil, err
		}

		lst := make([](*FirebaseNode), 0, numNodesToFetch)

		for len(lst) < numNodesToFetch && curNode != nil {
			lst = append(lst, curNode)
			nextNode, err := service.GetNode(rule(curNode))
			if err != nil {
				return nil, err
			}
			if nextNode == nil {
				break
			}

			curNode = nextNode
		}

		return lst, nil
	}
}
