# set up logger to INFO level
import json
import logging
import os

from dotenv import load_dotenv
from kafka import KafkaConsumer

logging.basicConfig(level=logging.INFO)

logger = logging.getLogger(__name__)

load_dotenv(verbose=True)


def main():
    # log that python consumer started for topic TOPICS_PEOPLE_BASIC_NAME
    logger.info(f"Python consumer started for topic {os.environ.get('TOPICS_PEOPLE_BASIC_NAME')}")

    consumer = KafkaConsumer(
        bootstrap_servers=os.environ.get('BOOTSTRAP_SERVERS'),
        group_id=os.environ.get('CONSUMER_GROUP'),
        key_deserializer=lambda m: m.decode('utf-8'),
        value_deserializer=lambda m: json.loads(m.decode('utf-8'))
    )

    consumer.subscribe([os.environ.get('TOPICS_PEOPLE_BASIC_NAME')])

    for record in consumer:
        logger.info(
            f"""
            Received message {record.value} 
            with key {record.key} 
            from partition {record.partition} 
            at offset {record.offset}
            """)


if __name__ == '__main__':
    main()
