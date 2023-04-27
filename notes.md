# Notes
## Admin API
- instructor recommends to define configurations as code, as opposed to the CLI where changes are not tracked

## Producer API
- at most 1 consumer from a consumer-group can read from a partition at a time
- kafka guarantees ordering at the partition level (not at the topic level)
- messages with the same key will always go to the same partition
- producers influence the throughput scalability
    - based on key, msgs are assigned to partitions via hashing + modulo (default algo), or round-robin if no key is specified
    - example; if the key is a category with only 2 options, you will only ever realize the value of 2 partitions!

### CLI Producer/Consumer
- To produce message with key:
    ```
    kafka-console-producer --bootstrap-server broker0:29092 --topic people --property "parse.key=true" --property "key.separator=|"
    ``` 
    example value:
    ```
    chief-scientist|{"name":"Martin Fowler", "title":"Chief Scientist"}
    ```
- To consume message and print key:
    ```
    kafka-console-consumer --bootstrap-server broker0:29092 --topic people --from-beginning --property "print.key=true"
    ``` 

### Producer Details

- retry defaults depend on implementation!
- batching is configurable, and recommended for high throughput apps (that write a lot) - this helps to avoid overhead of sending many small messages 
    - batch size is the number of bytes to wait for before sending (is there no way to specify nr. of messages?)
    - linger.ms is the time to wait for more messages before sending
- configs for sending records:
    - acks=0 -> producer will not wait for any acknowledgement from the broker (not recommended)
    - acks=1 -> producer will wait for the leader to acknowledge the message
    - acks=all -> producer will wait for the leader and all replicas to acknowledge the message (most reliable)
    - for really sensitive odering, set `max.in.flight.requests.per.connection=1` to ensure that only 1 message is sent at a time.
- retries & timeout are very high by default, better left as is
- beware of ordering that can be lost when adding partitions!

## Consumer API
- scalability of throughput in kafka is achieved via partitions;  more consumers can be added to the group if one consumer starts lagging behind 
- each partition can be read by only 1 consumer from a givne consumer group
- consumer rebalancing:  re-distributing the available partitions across teh available consumers
- consumer offsets topic stores the offsets of the next message to be read by each consumer group from a specific partition of a specific topic.  In other words, the messages stored follow the following format:
    - key: consumer group + topic + partition
    - value: offset of the next message to be read

