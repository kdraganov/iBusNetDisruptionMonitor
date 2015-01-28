package lbsl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Konstantin on 28/01/2015.
 */
public class ObservationList<E> extends ArrayList<E> {

    private Date updateTime = Calendar.getInstance().getTime();

    public boolean add(E e) {
        updateTime = Calendar.getInstance().getTime();
        return super.add(e);
    }

    public void add(int index, E e) {
        updateTime = Calendar.getInstance().getTime();
        super.add(index, e);
    }

    public Date getLastUpdateTime() {
        return updateTime;
    }

}
