import logging
import os

from confluent_kafka import DeserializingConsumer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroDeserializer
from confluent_kafka.serialization import StringDeserializer
from dotenv import load_dotenv

import schemas
from models import Person

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

load_dotenv(verbose=True)


def make_consumer() -> DeserializingConsumer:
    # create schema registry client
    schema_reg_client = SchemaRegistryClient({"url": os.environ.get("SCHEMA_REGISTRY_URL")})

    # create avro deserializer
    avro_deserializer = AvroDeserializer(
        schema_reg_client,
        schemas.person_value_v1,
        lambda person, ctx: Person(**person)
    )

    # create and return deserializing consumer
    consumer = DeserializingConsumer(
        {
            "bootstrap.servers": os.getenv("BOOTSTRAP_SERVERS"),
            "key.deserializer": StringDeserializer("utf_8"),
            "value.deserializer": avro_deserializer,
            "group.id": os.getenv("CONSUMER_GROUP"),
            "enable.auto.commit": False,  # we will commit manually to get as close to once-only as possible
            "auto.offset.reset": "earliest"
        }
    )

    return consumer


def main():
    logger.info(f"Python Avro consumer started for topic {os.getenv('TOPICS_PEOPLE_AVRO_NAME')}")

    consumer = make_consumer()
    consumer.subscribe([os.getenv("TOPICS_PEOPLE_AVRO_NAME")])

    while True:
        message = consumer.poll(1.0)
        if message is None:
            continue
        if message.error():
            logger.error(f"Consumer error: {message.error()}")
            continue

        logger.info(f"Consumed message: {message.value()}")
        consumer.commit(message=message)


if __name__ == "__main__":
    main()
