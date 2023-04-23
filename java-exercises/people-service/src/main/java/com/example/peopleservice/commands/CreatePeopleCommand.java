package com.example.peopleservice.commands;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class CreatePeopleCommand {
    private int count;
}
