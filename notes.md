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

