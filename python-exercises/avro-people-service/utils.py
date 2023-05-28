import logging
import re

from models import Person

logger = logging.getLogger(__name__)


def to_kafka_message_key(value: str) -> str:
    # Regex source: https://www.autoregex.xyz/;  it matches all non-alphanumeric characters
    return re.sub(r'[^a-zA-Z0-9]', '-', value.lower())


class ProducerCallback:
    def __init__(self, person: Person):
        self.person = person

    def __call__(self, err, msg):
        if err:
            logger.error(f"Failed to send person {self.person} to Kafka: {err}")
        else:
            logger.info(f"""
                Successfully produced {self.person} 
                to partition: {msg.partition()}
                at offset: {msg.offset()}
            """)
