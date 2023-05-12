from pydantic import BaseModel


class Person(BaseModel):
    id: str
    name: str
    title: str
