# set up logger to INFO level
import json
import logging
import os

from dotenv import load_dotenv
from kafka import KafkaConsumer, TopicPartition, OffsetAndMetadata

logging.basicConfig(level=logging.INFO)

logger = logging.getLogger(__name__)

load_dotenv(verbose=True)


def main():
    logger.info(f"Python consumer started for topic {os.environ.get('TOPICS_PEOPLE_NAME')}")

    consumer = KafkaConsumer(
        bootstrap_servers=os.environ.get('BOOTSTRAP_SERVERS'),
        group_id=os.environ.get('CONSUMER_GROUP'),
        key_deserializer=lambda m: m.decode('utf-8'),
        value_deserializer=lambda m: json.loads(m.decode('utf-8')),
        enable_auto_commit=False
    )

    consumer.subscribe([os.environ.get('TOPICS_PEOPLE_NAME')])

    for record in consumer:
        logger.info(
            f"""
            Received message {record.value} 
            with key {record.key} 
            from partition {record.partition} 
            at offset {record.offset}
            """)

        topic_partition = TopicPartition(record.topic, record.partition)

        # we will commit current offset + 1 because we have already processed the current message
        offset = OffsetAndMetadata(record.offset + 1, record.timestamp)

        consumer.commit({topic_partition: offset})


if __name__ == '__main__':
    main()
