package com.ermote.ArduUiPush;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.Uri;
import android.os.*;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import com.ermote.*;
import com.ermote.ArduUiPush.R;
import com.ermote.ArduUiPush.*;


public class MyActivity extends Activity {

    public int                          _state = com.ermote.ArduUiPush.Konst.STATE_OFFFLINE;
    private com.ermote.ArduUiPush.BtSrv _blut = null;
    private com.ermote.ArduUiPush.BtGattSrv _gatt;
    private static final int            REQUEST_ENABLE_BT = 3;
    private BluetoothAdapter            _ba;
    private String[]                    _devices;
    private android.widget.ListView     _lv;
    private ArrayList<String> _gatItems;//=new ArrayList<String>();
    private EditText                    _term;
    private KeepAlive                   _pinger;
    private boolean                     _active = false;
    private int                         _packets=0;
    private boolean                     _scr_done = false;
    private com.ermote.ArduUiPush.VCanvas _canvas;
    private SurfaceHolder _holder;
    private long                        _lastcheck = SystemClock.uptimeMillis()-20000;
    private boolean                     _pingnow=false;
    private int                         _coneerros = 0;
    private int                         _askingscreen = 0;
    private String                      _toutmsg = new String("");
    private boolean                     _text_mode=false;
    private boolean                     _canpool=false;
    private String                      _curdevname = new String();
    private com.ermote.ArduUiPush.BtDevItem _cursel = null;
    ArrayList<com.ermote.ArduUiPush.BtDevItem> _lv_devices = new ArrayList<com.ermote.ArduUiPush.BtDevItem>();
    ArrayList<com.ermote.ArduUiPush.BtDevItem> _lv_le_devices = new ArrayList<com.ermote.ArduUiPush.BtDevItem>();
    com.ermote.ArduUiPush.CustomListAdapter _cad;// = new CustomListAdapter(this, _lv_devices);
    private int                         _errors = 0;
    private boolean                     _readonceindicators = false;
    static final                        String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    private boolean                     _displaydef = false;
    public  boolean                     _potrait = false;
    public Point _display;
    private boolean                     _scanningles;
    private boolean                     _querynow = false;
    private static final long           SCAN_PERIOD = 8000;
    static public MyActivity            _theapp;
    private boolean                     _waiting = false;

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyActivity._theapp = this;
        _potrait = false;
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        boolean b = isChangingConfigurations();

        Display display = getWindowManager().getDefaultDisplay();
        _display = new Point();
        display.getSize(_display);


        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        _lv = (android.widget.ListView) findViewById(R.id.lvdevs);

        _lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object o = _lv.getItemAtPosition(position);
                _cursel = (BtDevItem) o;
                _device_selected();
            }
        });

        _term = (EditText) findViewById(R.id.editText);
        _term.setVisibility(View.GONE);
        _term.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    String txt = _term.getText().toString();
                    int idx = txt.lastIndexOf('\n');

                    String tosend = txt.substring(idx);
                    _blut.write(tosend.getBytes());
                    return true;
                }
                return false;
            }
        });

        final Button button = (Button) findViewById(R.id.PB_SCAN);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                scanQR(getCurrentFocus());
            }
        });

        final Button buttondel = (Button) findViewById(R.id.PB_DEL);

        buttondel.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v) {

                if(_ba!=null) {
                    if (_ba.isEnabled()) {
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                        _ba.disable();
                        Konst._sleep(2000);
                    }
                    if (!_ba.isEnabled()) {
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                        _ba.enable();
                        Konst._sleep(1000);
                    }
                }


                String path = getBaseContext().getFilesDir().getAbsolutePath();
                File dir = new File(path);
                deleteDir(dir);
            }
        });


        final Button buttonres = (Button) findViewById(R.id._rescan);

        buttonres.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v) {


                if(_state== com.ermote.ArduUiPush.Konst.STATE_OFFFLINE) {
                    _scan_le(true);
                }
            }
        });

    }

    public static boolean deleteDir(File dir)
    {

        if (dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++)
            {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success)
                {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        //_set_canvas(false);
        //super.onConfigurationChanged(newConfig);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean isChangingConfigurations() {
        if(android.os.Build.VERSION.SDK_INT >= 11){
            //Log.i("DEBUG", "Orientation changed api >= 11 ");
            return super.isChangingConfigurations();
        }else {
            //Log.i("DEBUG", "Orientation changed api < 11 ");
            return true;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    //configChanges
    @Override
    public void onDestroy(){
        super.onDestroy();

        if (isFinishing()) {
            _set_canvas(false);
            _active=false;
            if(_blut!=null)
                _blut.stopp();
            if(Konst.BTLE_ENABLED==true) {
                if (_gatt != null)
                    _gatt.stopp();
            }
            if(_pinger!=null)
                _pinger.stopp();
            _blut=null;
            _gatt=null;
            _pinger=null;
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        _active=true;
        if(_canvas!=null) {
            _canvas = null;
        }
        if(_ba==null) {
            _ba = BluetoothAdapter.getDefaultAdapter();
            if (_ba != null) {

                if (!_ba.isEnabled())
                {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                    _ba.enable();
                    Konst._sleep(200);
                }

                boolean btles = _scan_le(true);

                if ( btles )
                {
                    _active = true;
                    if (_pinger == null)
                    {
                        _pinger = new KeepAlive(_blut, _handler);
                        _pinger.startt();
                    }
                } else {
                    _tout("No devices found!!!", false);
                }

            } else {
                _tout("cannot get Bluetooth adapter!!!", false);
            }
        }
        _set_canvas(false);
    }


    @Override
    public void onPause(){
        _active=false;
        _set_canvas(false);
        super.onPause();
    }

    private void _tout(String s, boolean waiting) {

        _waiting =  waiting;
        for (BtDevItem device : _lv_devices)
        {
            if(_cursel.mac.compareTo(device.mac)==0 /*&&
                    _cursel.ledev.compareTo(device.ledev)==0*/ )
            {
                device.waiting=waiting;
                device.istate = _state;
                if(_toutmsg.compareTo("")==0) {
                    device.state = s;
                }
                else {
                    device.state = _toutmsg;
                }
                //hack
                if(_toutmsg.startsWith("Off")||s.startsWith("Off")) {
                    device.state = "Off-Line";
                    device.istate=Konst.STATE_OFFFLINE;
                    device.waiting = false;
                }
            }
            else
            {
                device.state = "Off-Line";
                device.istate=Konst.STATE_OFFFLINE;
                device.waiting = false;
            }
        }
        // _lv.setAdapter(_cad);
        _cad.notifyDataSetChanged();
        _lv.invalidate();
    }

    public void _set_text_mode(){
        _lv.setVisibility(View.GONE);
        _term.setVisibility(View.VISIBLE);
        _text_mode=true;

    }

    public void config_control(String uid)
    {
        // _canvas.config_control(uid);
    }


    public void set_text(String b){

        String s = _term.getText().toString();//
        int lines = _term.getMaxLines();
        int tl = 0;
        int firsttl = 0;
        for(int i=0;i<s.length();i++) {
            if (s.charAt(i)=='\r') {
                if(firsttl==0)
                    firsttl=i;
                tl++;
            }
        }
        _term.setText(s.substring(tl) + "\r\n" + b);
    }

    void set_text_mode(boolean b){
        if(b) {
            _text_mode = true;
            _lv.setVisibility(View.GONE);
            _term.setVisibility(View.VISIBLE);


        }
        else {
            _lv.setVisibility(View.VISIBLE);
            _term.setVisibility(View.GONE);

        }
    }

    public void update_control(String uid,  byte[] data){
        if(_canvas != null)
            _canvas.update_control(uid,data);
    }


    private void _conection_state(int state, String txt){
        boolean wait = (state == Konst.STATE_CONNECTING || state==Konst.STATE_CREATING);
        if(_state!=state) {
            _state = state;
            _set_canvas(false);
        }
        if(state==Konst.STATE_ONLINE){
            _scr_done=false;
            _active=true;
            //Log.i("","ONLINE");
        }else {
            _scr_done=false;
            _active =false;
            //Log.i("","OFFLINE");
            if(state==Konst.STATE_OFFFLINE)
                _disconnect();
        }
        //Toast.makeText(this, txt, 1).show();
    }


    void _terminal_io(byte[] buff)
    {
        String smsg = new String(buff, 0, buff.length);
        _term.setText(smsg);
    }

    /*
    ArrayList<Integer>                  _chunks = new ArrayList<Integer>();
    ByteArrayOutputStream               _outstream = new ByteArrayOutputStream();
    long                                _lastsent  = SystemClock.uptimeMillis();
     */

    synchronized public boolean send_buffer(byte[] b,int l, byte cook, UUID uid, int offbyte) {
        if (_state != Konst.STATE_ONLINE || _scr_done == false || l == 0 || !_active) {
            Log.e("OFF","=== Device is Off Line ===");

            return false;
        }
        if (_blut != null) {
            _blut.send_data(b, l, cook);
        }
        else if (_gatt != null) {
            if(Konst.BTLE_ENABLED)
            {
                if(uid!=null) {
                    // get rid of offbyte for BTLE, and put the bytes at 0
                    ByteArrayOutputStream bs = new ByteArrayOutputStream();
                    bs.write(b, offbyte, l-offbyte);
                    _gatt.write_data(bs.toByteArray(), bs.size(), uid);
                }else {
                    Toast.makeText(this,"Not assigned!",1);
                }
            }
        }
        _lastcheck = SystemClock.uptimeMillis();
        // Log.i("", "buffer:" + l + "  bytes was sent to ardu");
        return true;
    }

    synchronized public void _ping_ardu(){
        if (_blut==null || _state != Konst.STATE_ONLINE || !_active) {
            return;
        }

        if(!_scr_done){
            ++_askingscreen;
            if(_askingscreen % 3 == 0) {
                _blut.send_command(BtSrv.C_STOP);
                return;
            }

            if(_askingscreen > 8) {
                _disconnect();
                _askingscreen=0;
                _toutmsg ="Error: Ardu-reset/Check diagram/Restart BT";
                return;
            }
            _toutmsg ="Querying display...";
            _blut.send_command(BtSrv.C_ASK);
        }else {  // ping
            if(SystemClock.uptimeMillis() - _lastcheck < 1000) {
                return;
            }
            _blut.send_command(BtSrv.C_PING);
        }
        _lastcheck = SystemClock.uptimeMillis();
    }

    private boolean _is_class(String className) {
        try  {
            Class.forName(className);
            return true;
        }  catch (final ClassNotFoundException e) {
            return false;
        }
    }

    String _is_le_device(BluetoothDevice d) {
        if(Konst.BTLE_ENABLED==false)
        {
            return "BT";
        }
        Method connectGattMethod;
        try {
            connectGattMethod = d.getClass().getMethod("connectGatt", Context.class,
                    boolean.class,
                    BluetoothGattCallback.class,
                    int.class);
            String n = d.getName();
            int dtype = d.getType();
            if( dtype == BluetoothDevice.DEVICE_TYPE_LE ||dtype==BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
                return "BTLE";
            }
        } catch (NoSuchMethodException e) {
        } catch (RuntimeException e) {
        }
        return "BT";
    }

    public BluetoothAdapter.LeScanCallback _scan_le_cb = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    BtDevItem b = new BtDevItem();
                    b.name = device.getName();
                    b.mac = device.getAddress();
                    b.ledev = "BTLE";
                    b.state = "OffLine/Not paired";
                    b.waiting = false;

                    if (b.name != null && !_alredy_in(b.name)) {
                        _lv_le_devices.add(b);
                    }
                }
            });
        }
    };

    public boolean _alredy_in(String name) {
        for (int pd = 0; pd < _lv_le_devices.size(); pd++) {
            if (_lv_le_devices.get(pd).name.compareTo(name) == 0)
                return true;
        }
        return false;
    }
    public void _post_message(int message, int value, String text)
    {
        Message msg = _handler.obtainMessage(message);
        Bundle  b = new Bundle();
        b.putString("S", text);
        b.putInt("I", value);
        msg.setData(b);
        _handler.sendMessage(msg);
    }

    private boolean _scan_le(final boolean enable)
    {
        _lv_devices.clear();
        if(_cad==null)
            _cad = new CustomListAdapter(this, _lv_devices);
        _lv.setAdapter(_cad);
        _cad.notifyDataSetChanged();
        _lv.invalidate();

        if(!com.ermote.ArduUiPush.Konst.BTLE_ENABLED)
        {
            ProgressBar bu =  (ProgressBar) findViewById(R.id._scaning);
            bu.setVisibility(View.GONE);
            _get_paired_bts();
            return true;
        }

        if (_scanningles) {
            return false;
        }
        if (enable) {

            _handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    _ba.stopLeScan(_scan_le_cb);
                    invalidateOptionsMenu();
                    _scanningles = false;
                    MyActivity._theapp._post_message(Konst.MESSAGE_SCANNED, 0,"");
                }
            }, SCAN_PERIOD);

            _scanningles = true;
            _lv_le_devices.clear();

            _ba.startLeScan(_scan_le_cb);
            MyActivity._theapp._post_message(Konst.MESSAGE_SCANNING, 1,"");

        } else {
            _scanningles = false;
            _ba.stopLeScan(_scan_le_cb);
            MyActivity._theapp._post_message(Konst.MESSAGE_SCANNED, 0,"");
        }
        invalidateOptionsMenu();
        return true;
    }


    private boolean _get_paired_bts(){
        _scanningles=false;
        String path = getBaseContext().getFilesDir().getPath();
        Set<BluetoothDevice> pairedDevices = _ba.getBondedDevices();
        if (pairedDevices.size() > 0)
        {
            int len = pairedDevices.size();
            for (BluetoothDevice device : pairedDevices)
            {
                BtDevItem b = new BtDevItem();
                b.name = device.getName();
                b.mac = device.getAddress();
                b.ledev=_is_le_device(device);
                b.state="OffLine";
                b.waiting=false;
                String curdevname = path + "scr_" +b.name;
                b.delbutton = new File(curdevname).isFile();
                _lv_devices.add(b);
            }
        }
        if(Konst.BTLE_ENABLED) {
            for (BtDevItem device : _lv_le_devices) {
                device.state = "LE";
                _lv_devices.add(device);
            }
        }

        if(_lv_devices.size()==0)
        {
            BtDevItem b = new BtDevItem();
            b.name = "No devices found !";
            b.mac = "Go to settings and pair some devices !";
            b.ledev="--";
            b.state=".";
            b.waiting=false;
            b.delbutton = false;
            _lv_devices.add(b);
        }

        _cad = null;
        _cad = new CustomListAdapter(this, _lv_devices);
        _lv.setAdapter(_cad);
        _cad.notifyDataSetChanged();
        return _lv_devices.size() > 0;
    }

    protected void _device_selected() {
        _toutmsg="";
        int st = _state;
        _disconnect();
        if(_cursel.name.contains("No dev")) {
            return;
        }

        if(_waiting)
        {
            _tout("Off Line. Cancelled!", false);
            _disconnect();
            return;
        }

        _tout("Connecting...", true);
        _connect(_ba.getRemoteDevice(_cursel.mac));
        Konst._sleep(10);
    }

    public void _disconnect(){
        _curdevname="";
        _errors = 0;
        _packets = 0;
        _toutmsg="";
        _tout("Off-Line", false);
        if(Konst.BTLE_ENABLED) {
            if (_gatt != null)
                _gatt.stopp();
            _gatt = null;
        }
        if(_blut!=null)
            _blut.stopp();
        _blut=null;
        _set_canvas(false);
        _state= com.ermote.ArduUiPush.Konst.STATE_OFFFLINE;
    }

    private boolean refreshDeviceCache(com.ermote.ArduUiPush.BtGattSrv gatt){
        try {
            com.ermote.ArduUiPush.BtGattSrv localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        }
        catch (Exception localException) {
            Log.e("EX", "An exception occured while refreshing device");
        }
        return false;
    }



    private void _connect(BluetoothDevice device) {

        if(_ba!=null) {
            _ba.cancelDiscovery();
        }

        if(!_ba.isEnabled()) {
            _ba.enable();
            _get_paired_bts();
            return;
        }


        _toutmsg="";
        _errors = 0;
        if(Konst.BTLE_ENABLED) {
            if (_gatt != null) {
                _gatt.stopp();
                _gatt = null;
            }
        }
        if (_is_le_device(device).contains("BTLE") || _cursel.ledev.compareTo("BTLE")==0) {

            Method connectGattMethod;
            try {
                connectGattMethod = device.getClass().getMethod("connectGatt", Context.class,
                        boolean.class,
                        BluetoothGattCallback.class,
                        int.class);

                _curdevname = device.getName();
                _gatt = new BtGattSrv(this, _handler);
                _blut = null;
            } catch (NoSuchMethodException e) {
                _gatt = null;
            }
            if (_gatt != null) {

                if(_ba!=null)
                    _ba.stopLeScan(_scan_le_cb);
                _tout("Connecting...", true);
                if (load_dispplay_def()) {
                    _set_canvas(false);
                    _gatt.connect(device, getApplicationContext());
                } else {
                    _gatt.connect(device, getApplicationContext());
                    Toast.makeText(this, "Cannot load " + device.getName() + " display. Using dataview. Did you scan it from ermote.com?", 3);
                }
                refreshDeviceCache(_gatt);
            }
        } else {
            _set_canvas(false);
            if (_blut == null) {
                _blut = new BtSrv(this, _ba, _handler);
            }
            _blut.startt(device, false);
        }
        //Toast.makeText(this, "Connecting", 3);
        ////  _lv.setVisibility(View.GONE);
    }


    public class KeepAlive extends Thread {
        private BtSrv   _blut;
        private Handler _handler;
        private boolean running = false;

        public KeepAlive(BtSrv s, Handler h) {
            _handler = h;
            _blut = s;
        }

        public void startt() {
            if(running)
                stopp();
            Konst._sleep(100);
            running = true;
            start();
        }

        public void stopp() {
            running = false;
            try {
                join();
                Konst._sleep(200);
            }
            catch(InterruptedException e){
            }
        }

        @Override
        public void run() {
            int counter = 0;
            while(running)
            {
                try {
                    Thread.sleep(1024, 0);               //limit at 1 second
                } catch (InterruptedException e) {
                }

                if(_pingnow || counter++ % 3==0) {
                    Message msg = _handler.obtainMessage(Konst.MESSAGE_PING);
                    _handler.sendMessage(msg);
                    _pingnow=false;
                }
                if(_querynow || counter++ % 3==0) { //every 3 seconds
                    Message msg = _handler.obtainMessage(Konst.MESSAGE_TICK);
                    _handler.sendMessage(msg);
                    _querynow=false;
                }
            }
        }
    }

    private byte[] _try_load_locally(String screen, byte[] bytes)
    {
        try {

            String path = getBaseContext().getFilesDir().getPath();
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path+"/scr_"+screen));
            if(ois!=null) {
                bytes = (byte[]) ois.readObject();
                ois.close();
                ois = null;
            }

            return bytes;
        }  catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean load_dispplay_def()
    {
        //if(_displaydef==true)
        //    return true;
        byte[] buff = null;

        _displaydef=false;
        buff = _try_load_locally(_curdevname, buff);
        if (buff == null) {
            Toast.makeText(this, "Cannot find screen " + _curdevname + ". Using default screen !", 3);
            init_canvas("4/3", "0");
            return false;
        }//TODO screen size from buffer

        _process_ermo(buff, 0);
        _displaydef=true;
        return true;
    }

    private boolean _save_display_def(String screen,  byte [] bytes) {
        try {
            String path = getBaseContext().getFilesDir().getPath();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path + "/scr_"+screen));
            oos.writeObject(bytes);
            oos.close();
            oos = null;
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    public void _process_ermo(byte [] buff, int isdata)
    {
        int     off = 0;
        byte    cur = 1;
        int     cmddelay = 16;
        boolean demov=false; //demo mode

        /* send the screen with 0 crc only . all other crc !=0 */
        if( isdata == 0 && buff[0]==49 && buff[1]==48)             //screen definition
        {
            _askingscreen = 0;
            //Log.i("ermotee","canvas creating");
            ByteArrayOutputStream recs = new ByteArrayOutputStream();
            ArrayList<String> scrdeff = new ArrayList<String>();
            boolean           shortsd = false;

            _set_canvas(false);

            if(buff[2]==49) //save display / load display 101  for 100 do not cache
            {
                demov=false;
                for (int i = 0; i < buff.length - 1; i++) {
                    if (buff[i] == 0) {
                        shortsd = buff[i + 1] == 0;
                        break;
                    }
                    recs.write(buff[i]);
                }


                String sdef = recs.toString();
                if (shortsd) // is a screen is only
                {
                    buff = null;
                    buff = _try_load_locally(sdef, buff);
                    if (buff == null) {
                        _conection_state(Konst.STATE_OFFFLINE, "No screen found:" + sdef);
                        _disconnect();
                        return;
                    }
                    _canvas.set_msg("screen " + sdef + " was loaded locally");
                } else {
                    if (_save_display_def(sdef, buff)) {
                        _canvas.set_msg(sdef + " Saved!.");
                    }
                }
            }

            for (int i = 0; i < buff.length; i++) {
                if (buff[i] == 0) {
                    if (scrdeff.size() == 2){ //got the delay{
                        cmddelay = Integer.parseInt(recs.toString());
                    }
                    scrdeff.add(recs.toString());
                    recs.reset();
                } else {
                    recs.write(buff[i]);
                }
                if (cur == 0 && buff[i] == 0) {
                    off = i + 1;
                    break;
                }
                cur = buff[i];
            }

            int sz  =  scrdeff.size();
            if(sz<3) {
                if(_errors++> 4) {
                    _disconnect();
                }
                _toutmsg = "check ardu code (#define S_LDEF) ?!?";
                _tout("check ardu code (#define S_LDEF) ?!?", false);
                return;
            }

            int controls = buff[off];
            String sr = scrdeff.get(1);
            String color = scrdeff.get(2);

            init_canvas(sr, color);
            if(_canvas==null)
            {
                _toutmsg = "Invalid screen definition ?!?";
                _tout("Invalid screen definition ?!?", false);
                return;
            }
            off++;
            int offset = off;
            while (off < buff.length) {
                byte by = (byte) (buff[off] & 0xFF);
                if (by == 123) {
                    _canvas.add_control(buff, offset, scrdeff);
                    off++;
                    offset = off;
                    controls--; //at end should be 0
                } else {
                    off++;
                }
            }
            if(_state== com.ermote.ArduUiPush.Konst.STATE_ONLINE) {
                _tout("Running...", false);
                _set_canvas(true);
                _pingnow = true;
            }
            _canvas.finalizee(cmddelay, demov);
            //Log.i("ermotee","canvas created");

            _scr_done = true;
            if(_state== com.ermote.ArduUiPush.Konst.STATE_ONLINE) {

                _canvas.invalidate();
                _toutmsg = "In Session";
            }
        }

        else if(_canvas != null &&
                _canvas._constructed==true) {
            if(_canvas.set_values(buff)) {
                _canvas.invalidate();
            }
        }
    }

    private void _set_canvas(boolean b) {

        if (b == true)
        {
            if(_canvas!=null) {
                _canvas.setVisibility(View.VISIBLE);
            }
            _tout("OnLine",false);
        }else {
            _text_mode=false;
            _term.setVisibility(View.GONE);
            _lv.setVisibility(View.VISIBLE);
            if(_canvas!=null)
            {
                _canvas.turoff_controls();
                _canvas.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onBackPressed() {
        _toutmsg ="";
        if(Konst.BTLE_ENABLED) {
            if (_gatt != null) {

                _disconnect();
                return;
            }
        }
        if(_canvas!=null && _canvas.getVisibility() == View.VISIBLE){
            _disconnect();
            return;
        }
        do {
            AlertDialog ad = new AlertDialog.Builder(this)
                    .setIcon(R.drawable._marius)
                    .setTitle("Ermote-14 / Marius-O Chincisan")
                    .setMessage(Html.fromHtml("<a href=\"http://www.ermote.com\">ermote.com</a>"))
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }

                    })
                    .setNegativeButton("No", null)
                    .show();

            ((TextView)ad.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
        }while(false);

        //super.onBackPressed(); // allows standard use of backbutton for page 1
    }

    public void create_def_canvas(String sr, String color)
    {
        _canvas = new com.ermote.ArduUiPush.VCanvas(this, _display.x, _display.y);
        _potrait = false;
        this.addContentView(_canvas,
                new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT));
        _canvas.init(sr, color, _display.x, _display.y);
        _set_canvas(true);

    }

    private void init_canvas(String sr, String color)
    {

        String s[] = sr.split("/");
        _canvas = null;


        int sbh = getStatusBarHeight();
        int i0 = Integer.parseInt(s[0]);
        int i1 =  Integer.parseInt(s[1]);
        CheckBox ck = (CheckBox)findViewById(R.id._pl);
        if(ck.isChecked())
        {
            int tmp = i0; i0=i1; i1=tmp;
        }
        if(i0>i1)
        {
            _potrait = true;
            _canvas = new com.ermote.ArduUiPush.VCanvas(this, 2000,2000);
            _canvas.setMinimumWidth(2000);
            _canvas.setMinimumHeight(2000);

            this.addContentView(_canvas,
                    new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.MATCH_PARENT));

            _canvas.getHolder().setFixedSize(_display.x,_display.y);
            _canvas.setRotation(-90.0f);

            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)_canvas.getLayoutParams();
            params.height = _display.x ;
            params.width = _display.y - (4);
            params.topMargin  = ((_display.y-_display.x)/2) - ((sbh*2) + 16) ; // vertical pos
            params.leftMargin = ((_display.x-_display.y)/2); // left right
            _canvas.setLayoutParams(params);
            _canvas.getHolder().setKeepScreenOn(true);
            _canvas.init(sr, color, _display.y, _display.x-(sbh+16));
        }
        else
        {
            create_def_canvas(sr,color);
        }
        Log.i("--","Canvas Created");
        _set_canvas(_state == com.ermote.ArduUiPush.Konst.STATE_ONLINE);
    }

    //product qr code mode
    public void scanQR(View v) {
        try {
            //start the scanning activity from the com.google.zxing.client.android.SCAN intent
            Intent intent = new Intent(ACTION_SCAN);
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(intent, 0);
        } catch (ActivityNotFoundException anfe) {
            //on catch, show the download dialog
            showDialog(this, "No Scanner Found", "Download a scanner code activity?", "Yes", "No").show();
        }
    }

    //alert dialog for downloadDialog
    private static AlertDialog showDialog(final Activity act, CharSequence title, CharSequence message,
                                          CharSequence buttonYes, CharSequence buttonNo) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
        downloadDialog.setTitle(title);
        downloadDialog.setMessage(message);
        downloadDialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Uri uri = Uri.parse("market://search?q=pname:" + "com.google.zxing.client.android");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    act.startActivity(intent);
                } catch (ActivityNotFoundException anfe) {

                }
            }
        });
        downloadDialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        return downloadDialog.show();
    }

    ERControl add_dummy_control(String uuid, String IN, int dtype){
        if(_displaydef)
        {
            return null;
        }
        synchronized (this) {
            return _canvas.add_dummy_control(uuid, IN, dtype);
        }
    }

    ERControl is_Notification(UUID u)
    {
        synchronized (this) {
            if(_canvas!=null)
                return _canvas.get_dia_prop(u.toString(), "Nn");
        }
        return null;
    }

    ERControl is_Indicator(UUID u)
    {
        synchronized (this) {
            if(_canvas!=null)
                return _canvas.get_dia_prop(u.toString(), "IiRr");
        }
        return null;
    }

    ERControl is_Writable(UUID u)
    {
        synchronized (this) {
            if(_canvas!=null)
                return _canvas.get_dia_prop(u.toString(), "Ww");
        }
        return null;
    }

    //on ActivityResult method
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                //get the extras that are returned from the intent

                String contents = intent.getStringExtra("SCAN_RESULT");
                String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
                // Toast toast = Toast.makeText(this, "Content:" + contents + " Format:" + format, Toast.LENGTH_LONG);
                // toast.show();
                contents = contents.trim();
                String[] slist = contents.split(",");
                String   sname = slist[0].split("_")[1];
                boolean  asbyte=false;

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] zero={0};
                for (int i = 0; i < slist.length; i++) {
                    try {
                        if(slist[i].contains(":"))
                        {
                            String[] parts =slist[i].split(":");
                            outputStream.write(zero);
                            asbyte=true;
                            outputStream.write(Byte.parseByte(parts[1]));
                        } else {
                            if(asbyte) {
                                byte by = Byte.parseByte(slist[i]);
                                outputStream.write(by);
                            }else {
                                byte[] bytes = slist[i].getBytes("UTF-8");
                                outputStream.write(bytes);
                                outputStream.write(zero);
                            }
                        }
                    }catch(UnsupportedEncodingException e){
                        Log.e("", "UnsupportedEncodingException");
                        return;
                    }catch (IOException io){
                        Log.e("","IOException");
                        return;
                    }
                }
                _save_display_def(sname, outputStream.toByteArray());
            }
        }
    }

    public boolean display_loaded(){
        synchronized (this){
            return _displaydef;
        }
    }


    private final Handler _handler = new Handler() {

        private long    _asec = SystemClock.uptimeMillis();
        @Override
        public void handleMessage(Message msg) {
            String   smsg;
            long     ct = SystemClock.uptimeMillis();
            boolean  asec = (ct - _asec)>1000;

            if(asec) {
                _asec = ct;
            }
            switch(msg.what){
                case Konst.MESSAGE_SCANNING: {
                    ProgressBar bu =  (ProgressBar) findViewById(R.id._scaning);
                    bu.setVisibility(View.VISIBLE);
                }
                break;
                case Konst.MESSAGE_SCANNED: {
                    ProgressBar bu =  (ProgressBar) findViewById(R.id._scaning);
                    bu.setVisibility(View.GONE);
                    _get_paired_bts();
                }
                break;
                case Konst.NOTY_RECEIVED: {
                    if (_active) {
                        byte[] buff = (byte[]) msg.obj;

                        _tout("Connected: " + (_packets*11) + " BPS", false);
                        if(asec)
                        {
                            _packets = 0;
                            _packets += buff.length;
                        }
                        if(_text_mode)
                            _terminal_io(buff);
                        else
                            _process_ermo(buff, ((int) msg.arg2));
                    }else {
                        if(_canvas!=null)
                            _canvas.invalidate();
                    }
                }
                break;
                case Konst.STATE_CONNECTING:
                case Konst.STATE_CREATING:
                    _canpool=false;
                    _conection_state(msg.what,msg.getData().getString("E"));
                    _toutmsg ="Connecting...";
                    break;
                case Konst.STATE_OFF:
                    _canpool=false;
                    _conection_state(msg.what,msg.getData().getString("E"));
                    _toutmsg ="Off-Line";
                    break;
                case Konst.STATE_OFFFLINE:
                    _conection_state(msg.what,msg.getData().getString("E"));
                    _coneerros++;
                    _canpool=false;
                    _toutmsg ="Disconnected";
                    _set_canvas(false);
                    break;
                case Konst.STATE_ONLINE:
                    _coneerros=0;
                    _canpool=true;
                    _conection_state(msg.what,msg.getData().getString("E"));

                    _set_canvas(true);
                    break;
                case Konst.MESSAGE_PING:
                    if(_canvas != null && _canvas.getVisibility() == View.VISIBLE) {
                        _canvas.invalidate();
                    }
                    if(!_text_mode && _canpool)
                        _ping_ardu();
                    break;
                case Konst.MESSAGE_TICK:
                    if(Konst.BTLE_ENABLED) {
                        if (_gatt != null && _state == Konst.STATE_ONLINE) {
                            if (_readonceindicators) {
                                _readonceindicators = _gatt.queryDevice(null);
                            }
                        }
                    }
                    break;
                case Konst.MESSAGE_DELAY_PING:
                    _lastcheck = SystemClock.uptimeMillis();
                    break;
                case Konst.SET_TEXT_MODE:
                    _toutmsg ="Text Mode";
                    _set_text_mode();
                    break;
                case Konst.SET_RMOTE_MODE:
                    _toutmsg ="Connected";
                    _canpool=true;
                    break;
                case Konst.ACTION_GATT_CONNECTED:
                    _readonceindicators=true;
                    _state=Konst.STATE_ONLINE;
                    _set_canvas(true);
                    break;
                case Konst.ACTION_GATT_DISCONNECTED:
                    _displaydef = false;
                    _disconnect();
                    _readonceindicators=false;
                    break;
                case Konst.ACTION_GATT_SERVICES_DISCOVERED:
                    _readonceindicators=true;
                    _querynow = true;
                    break;
                case Konst.ACTION_DATA_AVAILABLE:
                    if(Konst.BTLE_ENABLED) {
                        if (_gatt != null) {
                            byte[] bytes = msg.getData().getByteArray("B");
                            int index = msg.getData().getInt("I");
                            String uid = msg.getData().getString("U");
                            ERControl c = _gatt.get_ctrl(index);
                            if (c != null && _canvas != null) {
                                _canvas.update_gatt_value(bytes, c, uid);
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        };
    };
}
