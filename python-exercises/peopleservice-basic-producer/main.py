import logging
import os
from typing import List

from dotenv import load_dotenv
from faker import Faker
from fastapi import FastAPI
from kafka import KafkaAdminClient
from kafka.admin import NewTopic
from kafka.errors import TopicAlreadyExistsError

from commands import CreatePeopleCommand
from entities import Person
from handlers import SuccessHandler, ErrorHandler
from utils import make_producer, to_kafka_message_key

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

load_dotenv(verbose=True)

app = FastAPI()


@app.on_event("startup")
async def startup_event():
    client = KafkaAdminClient(
        bootstrap_servers=os.environ.get("BOOTSTRAP_SERVERS"))
    topic = NewTopic(name=os.environ.get("TOPICS_PEOPLE_BASIC_NAME"),
                     num_partitions=int(os.environ.get(
                         "TOPICS_PEOPLE_BASIC_PARTITIONS")),
                     replication_factor=int(os.environ.get("TOPICS_PEOPLE_BASIC_REPLICAS")))
    try:
        client.create_topics([topic])
    except TopicAlreadyExistsError:
        logger.warning(f"Topic {topic} already exists")
    finally:
        client.close()


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

        producer.send(
            topic=os.environ.get("TOPICS_PEOPLE_ADVANCED_NAME"),
            key=to_kafka_message_key(person.title),
            value=person.json().encode("utf-8")
        ).add_callback(SuccessHandler(person)).add_errback(ErrorHandler(person))

    # always important to flush the producer, because it blocks until all messages are sent, including retries
    producer.flush()

    return people
