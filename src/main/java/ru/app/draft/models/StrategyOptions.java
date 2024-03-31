package ru.app.draft.models;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
public class StrategyOptions implements Serializable {

    private Boolean useGrid=Boolean.FALSE;

    private Integer countOfGrid=3;

    private BigDecimal offsetOfGrid= BigDecimal.valueOf(0.6);

    private BigDecimal lotOfOneGrid=BigDecimal.valueOf(0.001);
}
