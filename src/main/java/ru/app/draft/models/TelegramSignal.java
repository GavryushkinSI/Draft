package ru.app.draft.models;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class TelegramSignal implements Serializable {

    private String entry1;
    private String entry2;

    private String take1;
    private String take2;
    private String take3;

    private String stop;

    private String symbol;

    private String direction;

    private Long time;

    private Integer mode;

    private Boolean useReverse=false;

    private Boolean useAddMode=false;
    //Точка безубыточности
    private Boolean useProfitLossPoint=false;

    private String increaseValue;

    private String takeValue1;

    private String takeValue2;
}
