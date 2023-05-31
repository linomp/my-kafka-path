import logging
import os
from typing import List

from confluent_kafka import SerializingProducer
from confluent_kafka.admin import AdminClient
from confluent_kafka.cimpl import NewTopic
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer
from confluent_kafka.serialization import StringSerializer
from dotenv import load_dotenv
from faker import Faker
from fastapi import FastAPI

import schemas
from commands import CreatePeopleCommand
from models import Person
from utils import to_kafka_message_key

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

load_dotenv(verbose=True)

app = FastAPI()


@app.on_event("startup")
async def startup_event():
    client = AdminClient({"bootstrap.servers": os.environ.get("BOOTSTRAP_SERVERS")})
    topic = NewTopic(
        topic=os.environ.get("TOPICS_PEOPLE_AVRO_NAME"),
        num_partitions=int(os.environ.get("TOPICS_PEOPLE_AVRO_PARTITIONS")),
        replication_factor=int(os.environ.get("TOPICS_PEOPLE_AVRO_PARTITIONS"))
    )

    try:
        futures = client.create_topics([topic])
        for topic, future in futures.items():
            future.result()
            logger.info(f"Topic {topic} created")
    except Exception as e:
        logger.warning(e)


def make_producer() -> SerializingProducer:
    # make schema registry client
    schema_reg_client = SchemaRegistryClient({"url": os.environ.get("SCHEMA_REGISTRY_URL")})

    # create avro serializer
    # last parameter: callable that converts objects we wnt to send, into  dictionaries
    avro_serializer = AvroSerializer(
        schema_reg_client,
        schemas.person_value_v1,
        lambda person, ctx: person.dict()
    )

    # create and return serializing producer
    # Note: hashing algorithm for partitioner changes across implementations!
    #       java implementation okf concluent kafka lib uses murmur2_random,  instructor tip is to explicitly set it
    #       so that we can have the same hashing algorithm across producers written in different languages
    #       otherwise we could have messages with the same key ending up in different partitions!!

    return SerializingProducer(
        {
            "bootstrap.servers": os.environ.get("BOOTSTRAP_SERVERS"),
            "linger.ms": 300,  # to have some batching
            "enable.idempotence": True,
            "max.in.flight.requests.per.connection": 1,  # no out of order writing due to retries
            "key.serializer": StringSerializer("utf_8"),
            "value.serializer": avro_serializer,
            "partitioner": "murmur2_random"
        }
    )


@app.post("/api/people", status_code=201, response_model=List[Person])
async def create_people(command: CreatePeopleCommand):
    people: List[Person] = []
    faker = Faker()

    producer = make_producer()

    for _ in range(command.count):
        person = Person(
            id=faker.uuid4(),
            name=faker.name(),
            title=faker.job().title(),
        )
        people.append(person)
        producer.produce(
            topic=os.environ.get("TOPICS_PEOPLE_AVRO_NAME"),
            key=to_kafka_message_key(person.title),
            value=person
        )

    # always important to flush the producer, so that it blocks until all messages are sent, including retries
    producer.flush()

    return people
