package server.db;

public class DeviceInfo {
    public Integer userId;
    public String deviceId;
    public String deviceName;
    public boolean active;
    public boolean deleted;
    public SqlServer.DEVICE_STATE deviceState;
    public Integer attempts;
}
