# Notes
## Admin API
- instructor recommends to define configurations as code, as opposed to the CLI where changes are not tracked

## Producer API
- at most 1 consumer from a consumer-group can read from a partition at a time
- kafka guarantees ordering at the partition level (not at the topic level)
- messages with the same key will always go to the same partition
- producers influence the throughput scalability