package ru.app.draft.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@RequiredArgsConstructor
public class ConditionalOrder implements Serializable {

    private String size;

    private String direction;
}
