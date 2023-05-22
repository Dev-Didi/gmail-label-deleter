package gmailcleaner;

import com.google.api.services.gmail.model.Label;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
public class CleanerQuery {

    private Date cutOff;
    private boolean removeRead;
    private List<Label> labels;
    CleanerQuery() {
        this.cutOff = new Date(System.currentTimeMillis());
        this.removeRead = false;
        this.labels = new ArrayList<>();
    }

    public Date getCutOff() {
        return cutOff;
    }

    public void setCutOff(Date cutOff) {
        this.cutOff = cutOff;
    }

    public boolean isRemoveRead() {
        return removeRead;
    }

    public void setRemoveRead(boolean removeRead) {
        this.removeRead = removeRead;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }
}
