import logging
import os

from dotenv import load_dotenv
from fastapi import FastAPI
from kafka import KafkaAdminClient
from kafka.admin import NewTopic
from kafka.errors import TopicAlreadyExistsError

logger = logging.getLogger(__name__)

load_dotenv(verbose=True)

app = FastAPI()


@app.on_event("startup")
async def startup_event():
    client = KafkaAdminClient(bootstrap_servers=os.environ.get("BOOTSTRAP_SERVERS"))
    topic = NewTopic(name=os.environ.get("TOPICS_PEOPLE_BASIC_NAME"),
                     num_partitions=int(os.environ.get("TOPICS_PEOPLE_BASIC_PARTITIONS")),
                     replication_factor=int(os.environ.get("TOPICS_PEOPLE_BASIC_REPLICAS")))
    try:
        client.create_topics([topic])
    except TopicAlreadyExistsError:
        logger.warning(f"Topic {topic} already exists")
    finally:
        client.close()


@app.get("/")
async def root():
    return {"message": "Hello World"}
