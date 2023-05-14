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

### Consumer details
#### At-Least-Once processing
- default behavior of consumer is At-Least-Once!  (offsets are commited automatically after a certain time, if there are no errors processing the received set of records;   but if there is any intermediate error, no offset is commited, and the consumer will re-read & re-process the same set of records)
- We can also disable auto commit (enable.auto.commit=false), and then we have to commit the offset (+1) manually 
- best performance but can lead to duplicates

#### At Most Once Processing
- disable auto-commit
- collect records in  some data structure, and then commit the highest offset before processing the messages
- makes sense in use cases where it may be ok missing a few messages, but it is critical not to process the same message twice!

#### Exactly Once Processing
- disable auto-commit
- commit offset after processing each message
- requires idempotent producer (to avoid duplicates)
- highest robustness, but also the slowest
- generally this is the recommended approach based on instructor's experience
- transactions??   (only read commited messages?) ;  TODO:  read more about this

#### Consumer Lag
- metric to understand how caught up the consumer is wrt the producer
- it's possible to reset the offset to a partition; useful for re-processing or skipping messages


## Confluent Schema Registry

- provides a REST API for storing and retrieving JSON/Protobuf/Avro schemas -> Avro is the most common in industry
- versioned history of key and value schemas

- systems are unevitably cpuled to data
- data exchange contracts + controlled data evolution

- producer stores schema ID assigned by registry, and consumer uses it to retrieve the schema from the registry and deserialize the message
- schemas are cached by the producer and consumer, so they don't have to go to the registry every time

### Resources

- Schema Compatibility enforcement summary: https://docs.confluent.io/platform/current/schema-registry/fundamentals/avro.html

- On-premises deployment docker config: https://docs.confluent.io/platform/current/installation/docker/config-reference.html#sr-long-configuration

- Schema Registry Configuration Options: https://docs.confluent.io/platform/current/schema-registry/installation/config.html

- Schema Registry REST API Example Requests: https://docs.confluent.io/platform/current/schema-registry/develop/api.html#example-requests-format-and-valid-json

### Console avro consumer
Inside schema-registry container:

```
kafka-avro-console-consumer --bootstrap-server broker0:29092 --topic people.avro.java --from-beginning --property "schema.registry.url=http://localhost:8081"
```