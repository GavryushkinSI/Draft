package ru.app.draft.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class ShortLastPrice {
    private String figi;
    private Long price;
    private String updateTime;
}
