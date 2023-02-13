package ru.app.draft.models;

import com.google.protobuf.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LastPrice {
    Long price;
    Timestamp updateTime;
}
