import os
import re

from dotenv import load_dotenv
from kafka import KafkaProducer

load_dotenv(verbose=True)


def make_producer():
    return KafkaProducer(
        bootstrap_servers=os.environ.get("BOOTSTRAP_SERVERS"),
        retries=int(os.environ.get("TOPICS_PEOPLE_ADVANCED_RETRIES")),
        max_in_flight_requests_per_connection=int(
            os.environ.get("TOPICS_PEOPLE_ADVANCED_MAX_IN_FLIGHT_REQS")),
        acks=int(os.environ.get("TOPICS_PEOPLE_ADVANCED_ACKS"))
    )


def to_kafka_message_key(value: str) -> bytes:
    # Regex source: https://www.autoregex.xyz/;  it matches all non-alphanumeric characters
    return re.sub(r'[^a-zA-Z0-9]', '-', value.lower()).encode("utf-8")
