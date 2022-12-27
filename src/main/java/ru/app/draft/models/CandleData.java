package ru.app.draft.models;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class CandleData  implements Serializable, Comparable {

    private String x;

    private List<Long> y;

    public CandleData(String x, List y) {
        this.x = x;
        this.y = y;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public List getY() {
        return y;
    }

    public void setY(List y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CandleData that = (CandleData) o;
        return x.equals(that.x);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x);
    }

    @Override
    public int compareTo(Object o) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        Date date1=null;
        Date date2=null;
        try {
            date1=dateFormat.parse(this.getX());
            date2=dateFormat.parse(((CandleData)o).getX());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assert date1 != null;
        return date1.compareTo(date2);
    }
}
