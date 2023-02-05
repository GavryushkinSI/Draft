package ru.app.draft.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
public class LastPrice {

    Long price;
    Date updateTime;
}
