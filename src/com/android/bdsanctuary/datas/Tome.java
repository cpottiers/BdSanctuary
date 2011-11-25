package com.android.bdsanctuary.datas;

import com.cyrilpottiers.androlib.arrays.ArraysUtils;

public class Tome {
    // TABLE VARIABLES
    public boolean isMissingTome = false;
    private int    msSerieId;
    private int    msEditionId;
    private int    msId;
    private int    msNumber;
    private byte[] msIcon;
    private String msIconUrl;
    private String msTomePageUrl;

    // READ VARIABLES

    public Tome() {
    }

    public int getSerieId() {
        return msSerieId;
    }

    public void setSerieId(int sid) {
        this.msSerieId = sid;
    }

    public int getId() {
        return msId;
    }

    public void setId(int id) {
        this.msId = id;
    }

    public int getNumber() {
        return msNumber;
    }

    public String getTomePageUrl() {
        return msTomePageUrl;
    }

    public void setNumber(int number) {
        this.msNumber = number;
    }

    public byte[] getIcon() {
        return msIcon;
    }

    public void setIcon(byte[] icon) {
        this.msIcon = icon;
    }

    public String getIconUrl() {
        return msIconUrl;
    }

    public void setIconUrl(String url) {
        this.msIconUrl = url;
    }

    public int getEditionId() {
        return msEditionId;
    }

    public void setEditionId(int eid) {
        this.msEditionId = eid;
    }

    public void setTomePageUrl(String msTomePageUrl) {
        this.msTomePageUrl = msTomePageUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tome)) return false;

        if (this.msId != ((Tome) o).msId) return false;
        if (this.msSerieId != ((Tome) o).msSerieId) return false;
        if (this.msEditionId != ((Tome) o).msEditionId) return false;
        if (this.msNumber != ((Tome) o).msNumber) return false;
        if ((this.msIconUrl == null && ((Tome) o).msIconUrl != null)
            || (this.msIconUrl != null && !this.msIconUrl.equals(((Tome) o).msIconUrl)))
            return false;
        if ((this.msTomePageUrl == null && ((Tome) o).msTomePageUrl != null)
            || (this.msTomePageUrl != null && !this.msTomePageUrl.equals(((Tome) o).msTomePageUrl)))
            return false;
        if (!ArraysUtils.equals(this.msIcon, ((Tome) o).msIcon)) return false;

        return true;
    }

}
