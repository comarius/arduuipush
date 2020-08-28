package com.ermote.ArduUiPush;


import android.app.Service;
import android.bluetooth.*;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import com.ermote.ArduUiPush.ERControl;
import com.ermote.ArduUiPush.Konst;

import java.util.*;

/**
 * Created by marius on 16/01/16.
 */


public class BtGattSrv extends Service {

    private static final UUID UUID_SRV_CHANGED = UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_DEVICE_NAME = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_DEVICE_APEARANCE = UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb");

    private final static String TAG = "FlexDemoSrv";

    private Queue<CharItem> _indic_read_q = new LinkedList<CharItem>();
    private Queue<CharItem> _noty_q = new LinkedList<CharItem>();
    private ArrayList<CharItem> _indicators = new ArrayList<CharItem>();
    private ArrayList<CharItem> _commands = new ArrayList<CharItem>();
    private ArrayList<CharItem> _all_chrs = new ArrayList<CharItem>();
    private ArrayList<ERControl> _controls = new ArrayList<ERControl>();


    private String _sDeviceMac;
    public BluetoothGatt _gatt;
    private MyActivity _main;
    private Handler _handler;
    private static final int CONN_INTERVAL = 500;
    private static final int SUPERVISION_TIMEOUT = 10;
    private static int _indchecked = 0;
    private boolean _gatt_can_send = false;
    private int _registerred = 0;

    // CTOR
    BtGattSrv(MyActivity activity, Handler h) {
        _handler = h;
        _main = activity;
        _indchecked = 0;
        _registerred = 0;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;

            if (gatt == null) {
                _update_ui(Konst.ACTION_GATT_DISCONNECTED, null, null);
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {

                _update_ui(Konst.ACTION_GATT_CONNECTED, null, null);
                _gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                _update_ui(Konst.ACTION_GATT_DISCONNECTED, null, null);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (gatt == null)
                return;
            Log.d(TAG, "onServicesDiscovered received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                registerChars();
                _update_ui(Konst.ACTION_GATT_SERVICES_DISCOVERED, null, null);
                _gatt_can_send = true;
            } else {
                Log.w(TAG, "ERR onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic ccrr, int status) {
            synchronized (this) {
                _indic_read_q.remove();
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                _update_ui(Konst.ACTION_DATA_AVAILABLE, ccrr, null);
            } else {
                Log.e(TAG, "onCharacteristicRead: " + status + (ccrr.getUuid()));
            }

            // enque next one because this is done
            synchronized (this) { // is comming into list from UI
                if (!_indic_read_q.isEmpty()) {
                    //enqueue for read next one from queue
                    if (!_gatt.readCharacteristic(_indic_read_q.element()._ccrr)) {
                        Log.e(TAG, "readCharacteristic failed onReadChar" + ccrr.getUuid());
                    }
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            //Log.d(TAG, "ReadRemoteRssi");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _update_ui(Konst.ACTION_DATA_AVAILABLE, null, "5;" + rssi);
            } else {
                Log.e(TAG, "onReadRemoteRssi error: " + status);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            } else {
                Log.e(TAG, "Callback: Error writing GATT Descriptor: " + descriptor.getUuid());
            }
            _noty_q.remove();
            if (!_noty_q.isEmpty()) {
                CharItem item = _noty_q.element();
                Log.d("", "QUEUING next char" + item._ccrr.getUuid().toString());
                setCharacteristicNotification(item);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic ccrr) {
            _update_ui(Konst.ACTION_DATA_AVAILABLE, ccrr, null);
        }

        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic ccrr, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("-->>???", ccrr.getUuid().toString());
            }else
            {
                Log.d(" CHAR WROTE: ", ccrr.getUuid().toString());
            }
            synchronized (this) {
                _gatt_can_send = true;
            }
        }
    };


    public class LocalBinder extends Binder {
        BtGattSrv getService() {
            return BtGattSrv.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        if (_gatt != null) _gatt.close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean connect(BluetoothDevice device, Context c) {
        if (_gatt != null) {
            _gatt = null;
        }
       // _gatt.close();
        _gatt = device.connectGatt(_main, false, mGattCallback);
        _sDeviceMac = device.getAddress();
        return true;
    }

    public void stopp() {
        if (_gatt != null) {
            _unsubscribeChars();
            _gatt.disconnect();
            close();
        }
    }

    public void close() {
        if (_gatt == null) {
            return;
        }
        _gatt.close();
        _gatt = null;
    }

    public boolean readRemoteRssi() {
        return true;
    }

    public List<BluetoothGattService> getGarSrvs() {
        if (_gatt == null) return null;
        return _gatt.getServices();
    }

    public boolean queryDevice(UUID sensor) {
        if (_registerred == 0) {
            return true;
        }

        if (_indic_read_q.isEmpty() && _noty_q.isEmpty()) {
            int readchars = _indicators.size();

            synchronized (this) {
                int index = 0;
                for (CharItem ccrr : _indicators) {
                    if (sensor == null) {
                        if (index == _indchecked) {
                            _indic_read_q.add(ccrr);
                            break;
                        }
                    } else if (sensor.compareTo(ccrr._ccrr.getUuid()) == 0) {
                        _indic_read_q.add(ccrr);
                        break;
                    }
                    ++index;
                }

                if (_indic_read_q.size() > 0) {
                    CharItem cx = _indic_read_q.element();
                    if (!_gatt.readCharacteristic(cx._ccrr)) {
                        Log.e(TAG, "readCharacteristic failed");
                        _indic_read_q.clear();
                    }
                }
                _indchecked++;
                if (_indchecked == _indicators.size())
                    _indchecked = 0;
            }
            return true;
        }
        return true;
    }

    private boolean setCharacteristicNotification(CharItem itm) {

        Log.d("SET", "setCharacteristicNotification : " + (itm._ccrr.getUuid()) + " noty: " + itm._enabled);

        BluetoothGattDescriptor descriptor = itm._ccrr.getDescriptor(Konst.CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);

        if (descriptor != null) {
            if (!_gatt.setCharacteristicNotification(itm._ccrr, itm._enabled)) {
                Log.e(TAG, "setCharacteristicNotification failed " + itm._ccrr.getUuid());
                return false;
            }
            descriptor.setValue(itm._enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[]{0x00, 0x00});
            _gatt.writeDescriptor(descriptor);
            Log.d("", "writeDescriptor OK : " + (itm._ccrr.getUuid()) + " noty: " + itm._enabled);
            return true;
        }
        Log.e(TAG, "No descriptor: " + itm._ccrr.getUuid());

        return false;
    }


    private void setConnectionInterval(BluetoothGattCharacteristic connCharacteristic) {

        byte[] value = {(byte) (CONN_INTERVAL & 0x00FF), // gets LSB of 2 byte value
                (byte) ((CONN_INTERVAL & 0xFF00) >> 8), // gets MSB of 2 byte value
                (byte) (CONN_INTERVAL & 0x00FF),
                (byte) ((CONN_INTERVAL & 0xFF00) >> 8),
                0, 0,
                (byte) (SUPERVISION_TIMEOUT & 0x00FF),
                (byte) ((SUPERVISION_TIMEOUT & 0xFF00) >> 8)
        };
        connCharacteristic.setValue(value);
        boolean status = _gatt.writeCharacteristic(connCharacteristic);
        Log.d(TAG, "setConnectionInterval. Change connection interval result: " + status);
    }

    private void _add_charitem(CharItem cit)
    {
        for (CharItem ci : _all_chrs)
        {
            if(ci._ccrr.getUuid().compareTo(cit._ccrr.getUuid())==0)
            {
                ci._enabled |= cit._enabled;
                ci._leprop |= cit._leprop;
                Log.w("++---------|", "MODIFIED " + cit._ccrr.getUuid().toString().substring(4,9) + cit._enabled);

                return;
            }
        }
        _all_chrs.add(cit);
        Log.w("++--------->", "ADD " + cit._ccrr.getUuid().toString().substring(4,9) + cit._enabled);
    }


    public void registerChars() {
        ArrayList<UUID> local = new ArrayList<UUID>();
        ArrayList<UUID> localNotis = new ArrayList<UUID>();
        List<BluetoothGattService> gattServices = _gatt.getServices();
        if (gattServices == null) return;
        int index = 0;
        for (BluetoothGattService gattService : gattServices) {

            UUID srvid = gattService.getUuid();
            Log.d("SRV:", gattService.getUuid() + " " + (gattService.getUuid()));
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            if (gattCharacteristics == null)
                continue;

            for (BluetoothGattCharacteristic ccrr : gattCharacteristics) {

                UUID uid = ccrr.getUuid();
                if (local.indexOf(uid) != -1)
                    continue;
                local.add(uid);

                final int cp = ccrr.getProperties();

                ERControl ctrl = _main.is_Notification(uid);
                if (ctrl != null) {
                    CharItem si = new CharItem(ccrr, ctrl, srvid, uid, true, CharItem.LE_NOTY, cp);
                    localNotis.add(uid);
                    si._index = _all_chrs.size();
                    _add_charitem(si);

                    // continue;
                }

                ctrl = _main.is_Indicator(uid);
                if (ctrl != null) {
                    CharItem si = new CharItem(ccrr, ctrl, srvid, uid, false, CharItem.LE_READ, cp);
                    si._index = _all_chrs.size();
                    _add_charitem(si);

                    // continue;
                }

                ctrl = _main.is_Writable(uid);
                if (ctrl != null) {

                    CharItem ci = new CharItem(ccrr, ctrl, srvid, uid, false, CharItem.LE_WRITE, cp);
                    ci._index = _all_chrs.size();
                    _add_charitem(ci);

                    // continue;
                }

                if (ctrl == null) {
                    int dt = _type_it(cp);


                    if ((cp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ||
                        (cp & BluetoothGattCharacteristic.PROPERTY_BROADCAST) != 0) {

                        localNotis.add(uid);

                        ctrl = _main.add_dummy_control(ccrr.getUuid().toString(), "N" + "-" + dt, dt);
                        CharItem si = new CharItem(ccrr, ctrl, srvid, uid, true, CharItem.LE_NOTY, cp);
                        si._index = _all_chrs.size();
                        _add_charitem(si);

                        continue;
                    }

                    if ((cp & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 ||
                        (cp & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {

                        ctrl = _main.add_dummy_control(ccrr.getUuid().toString(), "IR" + "-" + dt, dt);
                        CharItem si = new CharItem(ccrr, ctrl, srvid, uid, false, CharItem.LE_READ, cp);
                        si._index = _all_chrs.size();
                        _all_chrs.add(si);

                        continue;
                    }

                    if ( (cp & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                        (cp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0 ||
                         (cp & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0 )
                    {
                        ctrl = _main.add_dummy_control(ccrr.getUuid().toString(), "W" + "-" + dt, dt);
                        CharItem ci = new CharItem(ccrr, ctrl, srvid, uid, false, CharItem.LE_WRITE, cp);
                        ci._index = _all_chrs.size();
                        _add_charitem(ci);

                    }
                }
            }
        }
        _subscribeChars(localNotis);
    }


    private int _type_it(int characteristicProperties) {
        int dt=0;

        if((characteristicProperties&BluetoothGattCharacteristic.FORMAT_UINT8)==BluetoothGattCharacteristic.FORMAT_UINT8)
        dt=Konst.TYPE_CHAR;
        else if((characteristicProperties&BluetoothGattCharacteristic.FORMAT_UINT16)==BluetoothGattCharacteristic.FORMAT_UINT16)
        dt=Konst.TYPE_INT16;
        else if((characteristicProperties&BluetoothGattCharacteristic.FORMAT_UINT32)==BluetoothGattCharacteristic.FORMAT_UINT32)
        dt=Konst.TYPE_LONG32;
        if((characteristicProperties&BluetoothGattCharacteristic.FORMAT_SINT8)==BluetoothGattCharacteristic.FORMAT_SINT8)
        dt=Konst.TYPE_CHAR;
        if((characteristicProperties&BluetoothGattCharacteristic.FORMAT_SINT16)==BluetoothGattCharacteristic.FORMAT_SINT16)
        dt=Konst.TYPE_INT16;
        if((characteristicProperties&BluetoothGattCharacteristic.FORMAT_SINT32)==BluetoothGattCharacteristic.FORMAT_SINT32)
        dt=Konst.TYPE_LONG32;
        if((characteristicProperties&BluetoothGattCharacteristic.FORMAT_SFLOAT)==BluetoothGattCharacteristic.FORMAT_SFLOAT)
        dt=Konst.TYPE_FLOAT;
        if((characteristicProperties&BluetoothGattCharacteristic.FORMAT_FLOAT)==BluetoothGattCharacteristic.FORMAT_FLOAT)
        dt=Konst.TYPE_FLOAT;
        return dt;
    }

    /**
    private Queue<CharItem> _indic_read_q = new LinkedList<CharItem>();
    private Queue<CharItem> _noty_q = new LinkedList<CharItem>();
    private ArrayList<CharItem> _indicators = new ArrayList<CharItem>();
    private ArrayList<CharItem> _all_chrs = new ArrayList<CharItem>();
    */
    private void _subscribeChars(ArrayList<UUID> notis) {
        boolean enqued = false;

        for (UUID uuid : notis)
        {
            for (CharItem ci : _all_chrs)
            {
                if(ci._ccrr.getUuid().compareTo(uuid)==0) {
                    ci._enabled = true; //make it notification
                    ci._leprop = CharItem.LE_NOTY;
                }
            }
        }


        //Queue<CharItem>
        for (CharItem ci : _all_chrs) {

            if(ci._ccrr.getUuid().compareTo(UUID_SRV_CHANGED)==0)
            {
                Log.w("UUID", "ignoring charact: " + UUID_SRV_CHANGED.toString());
                continue;
            }

            if((ci._leprop & CharItem.LE_NOTY) != 0)
            {
                _noty_q.add(ci);
            }
            else if((ci._leprop & CharItem.LE_READ) != 0)
            {
                _indicators.add(ci);
            }
            else if ((ci._leprop & CharItem.LE_WRITE) != 0)
            {
                _commands.add(ci);
            }
            else
                _noty_q.add(ci);
        }

        Queue<CharItem> loco = _noty_q;
        while(loco.size()>0)
        {
            CharItem cx = loco.element();
            if(setCharacteristicNotification(cx))
            {
                break;
            }
            loco.remove();
        }
        _registerred = 1;
    }


    private void _unsubscribeChars()
    {

    }

/*

    Log.e("", uid.toString() + "  char not configured");
*/

    class CharItem {
        static final public int LE_NOTY=0x1;
        static final public int LE_READ=0x2;
        static final public int LE_WRITE=0x4;

        public BluetoothGattCharacteristic _ccrr;
        public boolean      _enabled = false;
        public ERControl    _control;
        public int          _index = 0;
        public UUID         _srvuid;
        public UUID         _chriid;
        public int          _leprop = 0;
        public int          _chrprop = 0;

        public CharItem(BluetoothGattCharacteristic ccrr, ERControl type,
                        UUID srvid, UUID chriid ,boolean enable, int prop, int cp)
        {
            _ccrr    = ccrr;
            _control = type;
            _enabled = enable;
            _srvuid  = srvid;
            _chriid   = chriid;
            _chrprop   = cp;
            if((cp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0)
                _enabled = true;
            Log.d("CHR ITEM", chriid.toString() + " " + _enabled);
        }
    }

    int _get_char_index(UUID u) {
        for (CharItem chr : _all_chrs) {
            if (chr._ccrr.getUuid().compareTo(u)==0)
                return chr._index;
        }
        return 0;
    }

    ERControl get_ctrl(int index){
        synchronized (this) {
            CharItem c = _all_chrs.get(index);
            return c._control;
        }
    }

    private void _update_ui(int who, BluetoothGattCharacteristic ccrr, String whatever)
    {
        Message m  = _handler.obtainMessage(who);
        UUID id    = ccrr != null ? ccrr.getUuid() : null;
        Bundle bun = new Bundle();

        if(id != null) {
            if (who == Konst.ACTION_DATA_AVAILABLE) {
                final   byte[] data = ccrr.getValue();

                bun.putByteArray("B",data);
                bun.putString("U",ccrr.getUuid().toString());
                bun.putInt("I",_get_char_index(id));
            }
        }
        m.setData(bun);
        _handler.sendMessage(m);
    }

    public boolean write_data(byte[] b, int l, UUID uid)
    {
        if(_gatt_can_send==false) {
            return false;
        }
        for (CharItem chr : _all_chrs)
        {
            if (chr._chriid.compareTo(uid)==0) {
                BluetoothGattService service = _gatt.getService(chr._srvuid);
                if(service!=null) {
                    BluetoothGattCharacteristic chrw = service.getCharacteristic(uid);
                    if (chrw != null) {

                        if ((chrw.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0)
                        {
                            chrw.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                        }
                        else if ((chrw.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0)
                        {
                            chrw.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                        }

                        if (chrw.setValue(b)) {
                            if (_gatt.writeCharacteristic(chrw)) {
                                return true;
                            } else {
                                _gatt_can_send = true;
                                Log.e("ERR", "writeCharacteristic ");
                            }
                        }
                        else
                        {
                            Log.e("ERR", "setWriteType ");
                            _gatt_can_send=true;
                        }
                    }
                }
                break;
            }
        }
        return false;
    }
};



