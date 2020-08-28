package com.ermote.ArduUiPush;



import android.util.Log;
import java.util.UUID;



/**
 * Created by marius on 16/01/16.
 */

public class Konst {

    static public boolean   BTLE_ENABLED=true;
    public static final int STRING_MAX = 40;

    public static final int STATE_OFF=0;
    public static final int STATE_CONNECTING=1;
    public static final int STATE_OFFFLINE=2;
    public static final int STATE_ONLINE=3;
    public static final int STATE_CREATING=4;
    public static final int NOTY_RECEIVED=5;
    public static final int MESSAGE_PING=10;
    public static final int MESSAGE_DELAY_PING=11;
    public static final int SET_DELAY=12;
    public static final int SET_TEXT_MODE=13;
    public static final int SET_RMOTE_MODE=14;
    public static final int MESSAGE_TICK=15;
    public static final int MESSAGE_SCANNING=16;
    public static final int MESSAGE_SCANNED=17;


    public final static UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString ("00002902-0000-1000-8000-00805f9b34fb");
    public final static UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID1 = UUID.fromString("00002902-0000-1000-8000-00805f9b34fc");


    public final static int ACTION_GATT_CONNECTED =51;
    public final static int ACTION_GATT_DISCONNECTED = 52;
    public final static int ACTION_GATT_SERVICES_DISCOVERED = 54;
    public final static int ACTION_DATA_AVAILABLE = 55;

    public final static int TYPE_BOOL = 0;
    public final static int TYPE_CHAR = 1;
    public final static int TYPE_INT16 = 2;
    public final static int TYPE_LONG32 = 3;
    public final static int TYPE_STRING = 4;
    public final static int TYPE_FLOAT = 5;
    public final static int TYPE_BINARY = 6;

    public static void   _sleep(int ms){
        try {
            if(ms>5000)
                Thread.sleep(1000,0);
            else
                Thread.sleep(ms,0);
        } catch (InterruptedException e) {
            Log.e("", "sleep: " + ms + " Exception");
        }
    }

    public static final UUID MY_UUID_INSECURE =  UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    public static final UUID CUIDDD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID NULL_UUID = UUID.fromString("00000000-0000-0000-0000-008000000000");
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString( "00002902-0000-1000-8000-00805f9b34fb");


    static public byte CRC8(byte [] data, int len) {
        byte crc = 0x00;
        int  idx = 0;
        while (len-->0) {
            byte extract = (byte)(data[idx]&0xFF);
            idx++;
            for (byte tempI = 8; tempI>0; tempI--) {
                byte sum = (byte)((crc ^ extract) & 0x01);
                crc >>= 1;
                if (sum!=0) {
                    crc ^= (byte)0x8C;
                }
                extract >>= 1;
            }
        }
        if(crc==0) crc=1;
        return (byte)crc;
    }


}
