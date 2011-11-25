package com.android.bdsanctuary.datas;

import java.util.Hashtable;

public class Serie {

    public enum Status {
        SUIVIE,
        COMPLETE,
        NON_SUIVIE,
        INTERROMPUE;

        public static final int SUIVIE_VALUE      = 0;
        public static final int COMPLETE_VALUE    = 1;
        public static final int NON_SUIVIE_VALUE  = 2;
        public static final int INTERROMPUE_VALUE = 3;

        private Status() {
        }

        public int getValue() {
            switch (this) {
                case SUIVIE:
                    return SUIVIE_VALUE;
                case COMPLETE:
                    return COMPLETE_VALUE;
                case NON_SUIVIE:
                    return NON_SUIVIE_VALUE;
                case INTERROMPUE:
                    return INTERROMPUE_VALUE;
            }
            return SUIVIE_VALUE;
        }

        public static Status getValue(int value) {
            switch (value) {
                case SUIVIE_VALUE:
                    return SUIVIE;
                case COMPLETE_VALUE:
                    return COMPLETE;
                case NON_SUIVIE_VALUE:
                    return NON_SUIVIE;
                case INTERROMPUE_VALUE:
                    return INTERROMPUE;
            }
            return SUIVIE;
        }
    }

    // TABLE VARIABLES
    int    msId;
    String msName;
    Status msStatus;

    public Serie() {
    }

    public void setId(int id) {
        msId = id;
    }

    public void setName(String name) {
        msName = name;
    }

    public int getId() {
        return msId;
    }

    public String getName() {
        return msName;
    }

    public Status getStatus() {
        return msStatus;
    }

    public void setStatus(Status status) {
        this.msStatus = status;
    }

    // READ VARIABLES
    int                       msTomeCount;
    Hashtable<String, String> msEditions = new Hashtable<String, String>();

    public void setTomeCount(int count) {
        msTomeCount = count;
    }

    public int getTomeCount() {
        return msTomeCount;
    }

    public void addEdition(int eid, int count) {
        msEditions.put(Integer.toString(eid), Integer.toString(count));
    }

    public Hashtable<String, String> getEditions() {
        return msEditions;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Serie)) return false;

        if (this.msId != ((Serie) o).msId) return false;
        if ((this.msName == null && ((Serie) o).msName != null)
                || (this.msName != null && !this.msName.equals(((Serie) o).msName)))
            return false;
        if ((this.msStatus == null && ((Serie) o).msStatus != null)
            || (this.msStatus != null && !this.msStatus.equals(((Serie) o).msStatus)))
            return false;

        return true;
    }
    
    @Override
    public String toString() {
        if(this == null)
            return "[null]";
        else
            return String.format("[id=%1$d/name=%2$s/status=%3$s/count=%4$d]", this.msId, this.msName, this.msStatus, this.msTomeCount);
    }
}
