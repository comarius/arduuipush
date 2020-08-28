package com.ermote.ArduUiPush;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import com.ermote.ArduUiPush.*;
import com.ermote.ArduUiPush.Konst;

/**
 * Created by marius on 16/01/16.
 */

public class BtSrv extends TCall {

    public final static int           _HDRLEN = 4;
    public final static byte           _HDR   = (byte)0xFF;
    public final static byte           _CMD   = (byte)0xFE;
    public final static byte           _DATA  = (byte)0xFC;

    public final static byte           C_PING = (byte)0x1;
    public final static byte           C_STOP = (byte)0x2;
    public final static byte           C_ASK  = (byte)0x4; //version
    public final static byte           C_VER  = (byte)0x10;

    private static final String NAME_SECURE    = "eRmote14";
    private static final UUID MY_UUID_INSECURE =  UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID_INSECURE2 =  UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static UUID MY_UUID_ACTIVE =MY_UUID_INSECURE;


    private final Context _context;
    private final BluetoothAdapter _adapter;
    private final Handler _handler;
    private GThread                 _thread;
    private int                     _state = Konst.STATE_OFF;
    private boolean                 _secured=true;
    //==================================================================================
    private BluetoothSocket _socket;
    private InputStream _istream;
    private OutputStream _ostream;
    private final   String _stype = "Insecure";
    private BluetoothDevice _device;
    private  byte[] _buffer = new byte[512];


    public BtSrv(Context context, BluetoothAdapter ba, Handler handler) {
        _context = context;
        _handler = handler;
        _adapter = ba;
    }

    public void startt(BluetoothDevice device, boolean secure){
        stopp();
        _device = device;
        if(_thread!=null) {
            _thread.tdestroy();
            _thread = null;
        }
        _thread=new GThread(this);
        _thread.setName("btsrv" + _stype);
        _thread.create();
    }

    public void stopp(){
        if(_socket!=null){
            try {
                if(_socket!=null)
                    _socket.close();
            } catch (IOException e) {
            }
            _socket=null;
        }
        if(_thread!=null) {
            _thread.tdestroy();
            _thread=null;
        }
    }


    private void _notify_ui(String w, int state){
        Message m = _handler.obtainMessage(state);
        Bundle bundle = new Bundle();
        bundle.putString("E",w);
        m.setData(bundle);
        _handler.sendMessage(m);
    }


    public boolean write(byte[] out) {
        synchronized (this) {
            return _write(out);
        }
    }

    public int send_data(byte[] b, int l, byte cook) {

        byte[] by = new byte[BtSrv._HDRLEN + l];
        by[0] = BtSrv._HDR;
        by[1] = BtSrv._DATA;
        by[2] = (byte)(l & 0xFF);
        by[3] = cook; // Konst.CRC8(b, l);

        for (int i = 0; i < l; i++)
            by[i + BtSrv._HDRLEN] = b[i];

        this.write(by);
        return 1;
    }

    public void send_command(byte command)
    {
        byte[] by = new byte[BtSrv._HDRLEN];

        by[0] = BtSrv._HDR;
        by[1] = BtSrv._CMD;     // command
        by[2] = command;
        by[3] = (byte)((byte)0xFF -command);
        write(by);
    }

    //called from thread
    public int t_call(GThread t){
        _notify_ui("starting blue tooth", Konst.STATE_CREATING);
        if(!_create()){
            _notify_ui("Connecting", Konst.STATE_OFFFLINE);
            return 0;
        }
        Konst._sleep(100);
        _notify_ui("Connecting", Konst.STATE_CONNECTING);
        if(!_connect()){
            _notify_ui("Connection failed", Konst.STATE_OFFFLINE);
            return 0;
        }

        _notify_ui("CONNECTED", Konst.STATE_ONLINE);
        Konst._sleep(100);
        _receive_pool(t);
        _notify_ui("Connection terminated", Konst.STATE_OFFFLINE);
        return 0;
    }

    private boolean _create(){
        if(_istream!=null) {
            try {
                _istream.close();
            }catch (IOException e) {
            }
            _istream = null;
        }
        if(_ostream!=null) {
            try {
                _ostream.close();
            }catch (IOException e) {
            }
            _ostream = null;
        }

        if(_socket!=null){
            try {
                if(_socket!=null) {
                    Konst._sleep(1000);
                    _socket.close();
                }
            } catch (IOException e) {
            }
            _socket=null;
        }
        try {
            ParcelUuid puuids[] = _device.getUuids();// [0].getUuid();
            if(puuids != null && puuids.length!=0 )
            {
                MY_UUID_ACTIVE = puuids[0].getUuid();
                Log.i("UID ----> ", "Using UUID" + MY_UUID_ACTIVE.toString());
            }
            BluetoothSocket tmp = null;
            if(_secured) {
                Log.i("C","secured");

                Method m = null;
                try {
                    m = _device.getClass().getMethod("createRfcommSocket",new Class[] { int.class });
                } catch (NoSuchMethodException e1) {
                    e1.printStackTrace();
                }
                try {
                    tmp = (BluetoothSocket) m.invoke(_device, 1);
                    _socket = tmp;
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            else {
                _socket = _device.createInsecureRfcommSocketToServiceRecord(MY_UUID_ACTIVE);
                Log.i("C","non secured");
            }

        } catch (IOException e) {
            _socket=null;
        }

        if(_socket==null) {
            _secured = !_secured;
            return false;
        }

        try {
            _istream = _socket.getInputStream();
            _ostream = _socket.getOutputStream();
        } catch (IOException e) {
            _ostream=null;
            _istream=null;
            try {
                if(_socket!=null)
                    Konst._sleep(1000);
                    _socket.close();
            } catch (IOException es) {
            }
            _socket=null;
        }
        return _ostream!=null && _istream!=null;
    }

    private boolean _connect(){

        synchronized (this) {
            if (_socket != null) {
                try {
                    if (_socket.isConnected()) {
                        Konst._sleep(1000);
                        _socket.close();
                    }
                    Konst._sleep(100);
                    _socket.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        if (_socket != null && _socket.isConnected())
                            Konst._sleep(100);
                        _socket.close();
                        Konst._sleep(100);
                    } catch (IOException e2) {

                    }
                    _secured = !_secured;

                    return false;
                }
                return true;
            }
        }
        return false;
    }


    synchronized private boolean _write(byte[] buffer) {

        try {
            _ostream.write(buffer);
            Konst._sleep(8);
            _ostream.write(buffer);
            Konst._sleep(24);
            _ostream.write(buffer);

            return true;
        } catch (IOException e) {
        }
        Konst._sleep(100);
        try {
            Konst._sleep(1000);
            _socket.close();
        } catch (IOException e2)
        {
            ;
        }
        return false;
    }

    private int _receive(ByteArrayOutputStream iost) {
        int     ishot = 0;
        try {
            ishot = _istream.read(_buffer);
            if(ishot > 0) {
                iost.write(_buffer, 0, ishot);
            }
        }catch (IOException e) {
            ishot = -1;
        }
        return ishot;
    }

    private int _is_header(byte[] b, int OFFS)
    {
        // [len] [crc]
        if(b[OFFS] == BtSrv._HDR && b[OFFS + 1] == BtSrv._DATA)
            return (int)BtSrv._DATA;

        // [len] [len]
        if(b[OFFS] == BtSrv._HDR && b[OFFS + 1] == BtSrv._CMD)
            return (int)BtSrv._CMD;

        return 0;
    }

    private void _receive_pool(GThread t){
        int     textmode=-1;
        int     length = 0;
        int     icrc   = 0;
        int     curcrc = 0;
        int     curidx = 0;
        int     errs = 0;
        int     OFFS = 0;
        int     shot,bytes;
        byte [] sent;
        byte    b [];

        ByteArrayOutputStream sending = new ByteArrayOutputStream();
        ByteArrayOutputStream iost =  new ByteArrayOutputStream();

        for(int i=0;i<4;i++) {
            send_command(BtSrv.C_STOP);
            Konst._sleep(128);
        }
        //Log.i("","start bt thread");
        while(t.alive() && t.isAlive())
        {
            if (errs > 32){
                iost.reset();
                curidx = 0;
                length = 0;
                errs   = 0;
            }
            // Read from the OFFSnputStream
            shot = _receive(iost);
            if(shot==0){
                Konst._sleep(16);
                continue;
            }else if(shot==-1){
                Log.e("ermotee", "error reading socket");
                break;
            }
            b  = iost.toByteArray();
            bytes = iost.size();
            // Log.i("","got:" + bytes + " bytes");
            if(textmode==-1) {
                if (bytes > 0 && b[0] == '\r' || b[0] == '\n') {
                    textmode = 1;
                    _notify_ui("T", Konst.SET_TEXT_MODE);
                    //Log.i("","going in text-mode");
                }
                else
                {
                    _notify_ui("T", Konst.SET_RMOTE_MODE);
                    //Log.i("","going in binary-mode");
                    textmode=0; //binary
                }
            }

            if(textmode==1){
                Message msg = _handler.obtainMessage(Konst.NOTY_RECEIVED,length, -1, b);
                _handler.sendMessage(msg);
                Konst._sleep(8);
            }

            if(bytes < BtSrv._HDRLEN) {
                Konst._sleep(8);
                continue;
            }
            if(bytes>1500){
                OFFS       = 0;
                length  = 0;
                errs    = 0;
                iost.reset();
                continue;
            }

            while(bytes>0) {

                if (length == 0) {
                    while (OFFS < bytes - BtSrv._HDRLEN) {
                        int ish = _is_header(b, OFFS);
                        if(ish == BtSrv._DATA){
                            length = (int)b[OFFS + 2] & 0xFF;
                            icrc   = (int)b[OFFS + 3] & 0xFF;
                            if(icrc == 0) icrc = 1; // fix this
                            break;
                        }
                        if(ish == BtSrv._CMD){ //is only screen
                            length = (int) ((b[OFFS + 2] & 0xFF) | ((b[OFFS + 3] & 0xFF) << 8)) & 0xFFFF;
                            icrc = 0;
                            break;
                        }
                        OFFS++;
                    }
                    if(length<=0) {
                        length=0;
                        break;
                    }
                }
                if (bytes < (OFFS+BtSrv._HDRLEN+length)) {  //not enough
                    Konst._sleep(8);
                    break;
                }

                sending.reset();
                sending.write(b, OFFS + BtSrv._HDRLEN, length);

                byte bs [] = sending.toByteArray();
                Message msg = _handler.obtainMessage(Konst.NOTY_RECEIVED,length, icrc, bs);
                _handler.sendMessage(msg);
                iost.reset();
                if(bytes - (OFFS+BtSrv._HDRLEN+length)>0) {
                    int offset   = OFFS + length + BtSrv._HDRLEN;
                    int leftover = bytes-(length+BtSrv._HDRLEN+OFFS);
                    iost.write(b, offset, leftover);
                }
                bytes   = iost.size();
                b       = iost.toByteArray();
                OFFS    = 0;
                length  = 0;
                errs    = 0;
            }
        }
    }
}
