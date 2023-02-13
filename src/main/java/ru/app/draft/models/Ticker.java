package ru.app.draft.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
public class Ticker implements Serializable {
    private String value;
    private String label;
    private String figi;
    private String classCode;
    private Integer minLot;
}
