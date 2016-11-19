package net.masaya3.gunsound.data;

/**
 * Created by masaya3 on 2016/07/30.
 */
public class GunInfo {

    public enum Type {
        ON,
        OFF
    };

    public boolean auto = false;
    public boolean one_shoot = true;
    public boolean two_shoot = false;
    public boolean three_shoot = false;
    public boolean four_shoot = false;
    public Type triggerType = Type.OFF;
    public Type reloadType = Type.OFF;
}
