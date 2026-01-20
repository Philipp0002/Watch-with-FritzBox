package de.hahnphilipp.watchwithfritzbox.hbbtv;

public class HbbTVChannel {
    /// 0 = TV; 1 = RADIO
    public int channelType;
    /**
     * 10 = ID_DVB_C (‘onid’, ‘tsid’, ‘sid’)
     * 4 = ID_DVB_C2 1 (‘onid’, ‘tsid’, ‘sid’)
     */
    public int idType = 4;
    public String longName;
    public String name;
    public int nid;
    public int onid;
    public int sid;
    public int tsid;
}
