import logging

from entities import Person

logger = logging.getLogger(__name__)


class SuccessHandler:
    def __init__(self, person: Person):
        self.person = person

    def __call__(self, record_metadata):
        logger.info(
            f"Successfully sent person {self.person} to topic {record_metadata.topic} and partition {record_metadata.partition} at offset {record_metadata.offset}")


class ErrorHandler:
    def __init__(self, person: Person):
        self.person = person

    def __call__(self, exception):
        logger.error(
            f"Failed to send person {self.person}", exc_info=exception)
