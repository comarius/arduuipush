package com.ermote.ArduUiPush;

/**
 * Created by marius on 16/01/16.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;
import com.ermote.ArduUiPush.*;

/**
 * Created by marius on 27/05/15.
 */
public class VCanvas extends SurfaceView {
    private static final int SNAP = 32;

    public static final int[] _colors = {Color.rgb(0x00, 0x00, 0x00),
            Color.rgb(0xC0, 0xC0, 0xC0),
            Color.rgb(0xFF, 0xFF, 0xFF),
            Color.rgb(0x80, 0x00, 0x00),
            Color.rgb(0xFF, 0x00, 0x00),
            Color.rgb(0x80, 0x80, 0x00),
            Color.rgb(0xFF, 0xFF, 0x00),
            Color.rgb(0x00, 0x80, 0x00),
            Color.rgb(0x00, 0xFF, 0x00),
            Color.rgb(0x00, 0x80, 0x80),
            Color.rgb(0x00, 0x00, 0xFF),
            Color.rgb(0xFF, 0x00, 0xFF)};
/*

    public static final int [] _colors = {
                            Color.rgb(0x00,0x00,0x00),
                            Color.rgb(0x79,0xBF,0xF2),
                            Color.rgb(0x88,0x99,0xAA),
                            Color.rgb(0x55,0x66,0x77),
                            Color.rgb(0x3F,0x64,0x7F),
                            Color.rgb(0x22,0x66,0x99),
                            Color.rgb(0x00,0x61,0xA8),
                            Color.rgb(0x11,0x55,0x88),
                            Color.rgb(0x3D,0x46,0x4C),
                            Color.rgb(0x00,0x44,0x66),
                            Color.rgb(0x2B,0x2B,0x2B),
                            Color.rgb(0x2B,0x2B,0x2B)};//
*/


    private static final String[] _types = {
            "boolean",
            "char",
            "int",
            "long",
            "string",
            "float",
            "bytes"};
    public static final int[] _dtlengths = {
            1, //bool arduino
            1, // char arduino
            2, // int arduinp
            4, // long arduino
            16, // string
            4, //float 4 bytes float
            16,
            -1};


    public static final int[] _fonts = {8, 10, 12, 14, 16, 18, 20, 22, 32, 34, 42, 50, 32, 34, 40};


    public ArrayList<ERControl> _controls = new ArrayList<ERControl>();
    public ArrayList<Integer> _ctrls = new ArrayList<Integer>();
    public ArrayList<Integer> _inds = new ArrayList<Integer>();

    public boolean _constructed = false;
    public int _bgcolor;
    public static double _snap = 1.0; //for 640 display
    public int _w = 0;
    public int _h = 0;

    private SurfaceHolder _surfaceHolder;
    public byte[] _outbuffer = null;
    private int _outbuflen = 0;
    private int _inbuflen = 0;
    private MyActivity _main_activ;
    private int _frames = 0;
    private String _message = new String();
    public Context _mactivity;
    private boolean _aspect = false; //landscape
    public long _cmddelay = 8;
    private byte _betar = 0;
    private boolean _wmark = false;
    GLSurfaceView _glsv = null;


    public VCanvas(Context context, int w, int h) {
        super(context);
        _mactivity = context;
        setWillNotDraw(false);
        _main_activ = (MyActivity) context;
        _w = w;
        _h = h;
        _snap = ((double) _w) / 640.0;

        com.ermote.ArduUiPush.ERControl._leftDUmmy=0;
        com.ermote.ArduUiPush.ERControl._topDUmmy=0;


    }

    void send_outbuff(byte cook, UUID uid, int offbyte) {
        _main_activ.send_buffer(_outbuffer, _outbuflen, cook, uid, offbyte);
    }

    public void set_aspect(boolean aspect) {
        if (_w > _h) //landscape
        {
            _aspect = aspect;
            this.setRotation(0.0f);
        }
        else
        {
            _aspect = !aspect;
            this.setRotation(90.0f);
        }

        //_main_activ.setPortrait(_h/_w);
    }

    public void turoff_controls() {
        for (int i = 0; i < _controls.size(); i++) {
            ERControl c = _controls.get(i);
            c.pause();
        }
    }

    void set_msg(String s) {
        _message = s;
        _frames = 255;
    }



    @Override
    synchronized protected void onDraw(Canvas cvx) {
        cvx.save();

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Typeface tf = Typeface.create("Arial", Typeface.NORMAL);
        p.setTypeface(tf);
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(Color.WHITE);

        cvx.drawColor(_bgcolor);//, PorterDuff.Mode.CLEAR);

        if (_controls.size() > 0) {
            for (int i = 0; i < _controls.size(); i++) {
                p.setStrokeWidth(1);
                ERControl c = _controls.get(i);
                try {
                    Method m = c.getClass().getMethod("draw_" + Integer.toString(c._tid));
                    m.invoke(cvx, p);
                    continue;
                } catch (NoSuchMethodException e) {
                } catch (IllegalAccessException k) {
                } catch (InvocationTargetException g) {
                }
                c.draw_it(cvx, p);
            }
        } else {

            cvx.save();
            p.setStyle(Paint.Style.STROKE);
            p.setColor(Color.rgb(255, 255, 200));
            p.setTextSize(50);
            p.setTextAlign(Paint.Align.CENTER);

            cvx.drawText("Loading...", _w / 2, _h / 2, p);
            cvx.restore();
        }
        if (_message.length() > 0 && _frames > 0) {
            _frames -= 10;
            p.setColor(Color.rgb(255, _frames, _frames));
            cvx.drawText(_message, _w / 2, _h / 2, p);
        }
        cvx.restore();
        if (_wmark) {
            cvx.save();
            p.setAlpha(1);
            p.setAntiAlias(true);
            p.setTextSize(48);
            p.setColor(Color.argb(_betar, _betar * 4, _betar * 6, _betar + 56));
            _betar++;
            //cvx.drawText("Free Version", _w/2 + (float)Math.random()*_w/2, _h / 2 + (float)Math.random()*_h/2, p);
            cvx.drawText("Version 1.0.0", _w / 2, _h - 64, p);
            cvx.restore();
        }

    }

    public void stop_thread() {

        _constructed = false;
    }

    public void finalizee(int cmddelay, boolean wmark) {
        com.ermote.ArduUiPush.ERControl._leftDUmmy=0;
        com.ermote.ArduUiPush.ERControl._topDUmmy=0;

        _surfaceHolder = getHolder();
        _constructed = true;
        _outbuffer = null;
        if (_outbuflen > 0)
            _outbuffer = new byte[_outbuflen + 1];

        _cmddelay = cmddelay + 96;
        _wmark = wmark;
        for (int i = 0; i < _controls.size(); i++) {
            ERControl c = _controls.get(i);
            if (c._tid < 80)
                c.notify_inits();
        }
    }

    public void init(String sr, String color, int w, int h) {
        _bgcolor = _colors[Integer.parseInt(color)];
        _controls.clear();
        _ctrls.clear();
        _inds.clear();
        _constructed = false;
        _outbuffer = null;
        _w = w;
        _h = h;
        if(_h>w)
            _snap = ((double) _h) / 640.0;
        else
            _snap = ((double) _w) / 640.0; // 640 is the size in web designer
        invalidate();
    }

    static public void _sleep() {
        try {
            Thread.sleep(64, 0);
        } catch (InterruptedException e) {
        }
    }

    public void add_control(byte[] by, int idx, ArrayList<String> strl) {
        int tid = by[idx];

        ERControl c;        // = new ERControl(by, idx, strl);
        switch (tid) {
            case 80:
                c = new shape(this, by, idx, strl);
                break;
            case 82:
                c = new gauge(this, by, idx, strl);
                break;
            case 83:
                c = new display(this, by, idx, strl);
                break;
            case 85:
                c = new dir(this, by, idx, strl);
                break;
            case 86:
                c = new graph(this, by, idx, strl);
                break;
            case 90:
                c = new acc3d(this, by, idx, strl);
                break;
            case 88:
                c = new rgb(this, by, idx, strl);
                break;
            case 89:
                c = new gyro(this, by, idx, strl);
                break;
            case 10:
                c = new touch(this, by, idx, strl);
                break;
            case 3:
                c = new button(this, by, idx, strl);
                break;
            case 4:
                c = new check(this, by, idx, strl);
                break;
            case 6:
                c = new slider(this, by, idx, strl);
                break;
            case 7:
                c = new joy(this, by, idx, strl);
                break;
            case 8:
                c = new accel(this, by, idx, strl);
                break;
            default:
                c = new ERControl(this, by, idx, strl);
                break;
        }

        if (c._tid >= 80) {
            _inbuflen = c.set_iid(_inds.size());
            _inds.add(_controls.size());
        } else {
            _outbuflen = c.set_iid(_ctrls.size());
            _ctrls.add(_controls.size());
        }
        _controls.add(c);
    }

    void _check_control_down(int idx, int x, int y) {
        for (int i = 0; i < _controls.size(); i++) {
            ERControl c = _controls.get(i);
            if (!c._iscontrol) continue; // an indicator

            if (c.in_rect(x, y)) {
                c.touch_down(x, y, idx);
            }
        }
    }

    void _check_control_up(int idx, int x, int y) {
        for (int i = 0; i < _controls.size(); i++) {
            ERControl c = _controls.get(i);
            if (!c._iscontrol) continue; // an indicator
            c.touch_up(x, y, idx);
        }
    }

    void _check_control_move(int idx, int x, int y) {
        for (int i = 0; i < _controls.size(); i++) {
            ERControl c = _controls.get(i);
            if (!c._iscontrol) continue; // an indicator
            c.touch_move(x, y, idx);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        int pid = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        int finger = event.getPointerId(pid);

        int x = (int) event.getX(pid);
        int y = (int) event.getY(pid);

        switch (actionCode) {

            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                _frames = 0;
                _check_control_down(finger, x, y);
                break;
            }
            case MotionEvent.ACTION_MOVE: { // a pointer was moved
                _check_control_move(finger, x, y);
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                _check_control_up(finger, x, y);
                break;
            }
        }
        invalidate();
        return true; //processed
    }

    public void update_gatt_value(byte[] data, ERControl c, String uid) {
        _update_one_ctrl(c, data, uid);
    }



    private String _ba2hex(byte[] a) {
        StringBuilder sb = new StringBuilder(100);
        byte    prev=-1;
        boolean aschr=true;
        boolean once=false;
        for (byte b : a) {
            sb.append(String.format("%02x:", b & 0xff));
        }
        return sb.toString();
    }

    private void _update_one_ctrl(ERControl c, byte[] struct, String uid)
    {
        int         I = 0;
        int         bytes = struct.length;
        String[]    vals = new String[6];

        if(c==null){
            Log.e("", "invalid control. NULL");
            return;
        }

        vals[0]=vals[1]=vals[2]=vals[3]=vals[4]=vals[5]="";
        int ardubhtes = _dtlengths[c._dtype] * c._dsize;

        //hack-hack
        switch(c._dtype) {
            case 2:
                if(bytes==1)c._dtype=1;
                break;//
            case 3:
                if(bytes==1)c._dtype=1;
                if(bytes==2)c._dtype=2;
                break;
            case 4:
                if(bytes==1)c._dtype=1;
                else
                if(bytes==2)c._dtype=2;
                else
                if(bytes==4)c._dtype=3;
                else
                    c._dtype=6; //string
                break;
        }

        switch(c._dtype)
        {
            case 0:     // bool 1 byte
                for (int i = 0; i < c._dsize; i++) {
                    vals[i] = Byte.toString(struct[I]);
                    I += _dtlengths[c._dtype];
                }
                break;
            case 1:     // char 1 byte
                for (int i = 0; i < c._dsize; i++) {
                    vals[i] = Byte.toString(struct[I]);
                    I += _dtlengths[c._dtype];
                }
                break;
            case 2:     // ardu int 2 byte
                for (int i = 0; i < c._dsize; i++) {
                    short temp = (short) ((struct[I] & 0xFF) | (struct[I + 1] & 0xFF) << 8);
                    vals[i] = Short.toString(temp);
                    I += _dtlengths[c._dtype];
                }
                break;
            case 3:     // ardu long 4 byte
                for (int i = 0; i < c._dsize; i++) {
                    long temp = (struct[I] & 0xFF) | (struct[I + 1] & 0xFF) << 8 | (struct[I + 2] & 0xFF) << 16 | (struct[I + 3] & 0xFF) << 24;
                    vals[i] = Long.toString(temp);
                    I += _dtlengths[c._dtype];
                }
                break;   // ardu char[_dsize]
            case 4: {//string
                StringBuilder sb = new StringBuilder(65);

                for (int i = 0; i < c._dsize; i++)
                {
                    if (struct[I] != 0)
                    {
                        sb.append((char) struct[I]);
                    }
                    I += 1;
                }
                vals[0] = sb.toString();
            }
            break;
            case 5: {
                for (int i = 0; i < c._dsize; i++)
                {
                    Float f = Float.intBitsToFloat( struct[I] ^ struct[I+1]<<8 ^ struct[I+2]<<16 ^ struct[I+3]<<24 );
                    vals[i] = Float.toString(f);
                    I += _dtlengths[c._dtype];
                }
            }
            break;
            case 6: { // as many bytes there are
                /*
                if(c._dummygat) {
                    vals[0] = uid + "=";
                    vals[0] += _ba2hex(struct);
                }else{
                    vals[0] = _ba2hex(struct);
                }
                */
                StringBuilder sb = new StringBuilder(65);
                int lmax = Math.min(16, c._dsize);
                lmax = Math.min(lmax, struct.length);
                for (int i = 0; i < lmax; i++)
                {
                    if (struct[I] != 0)
                    {
                        sb.append((char) struct[I]);
                    }else
                        break;
                    I += 1;
                }
                vals[0] = sb.toString();
            }
            break;
            default:
                break;
        }
        c.set_value(vals,c._dtype,c._dsize);
        invalidate();
    }

    // these are indicators
    public boolean set_values(byte[] struct) {
        int     dsz = _inds.size();
        int     I = 0;
        int     bytes = struct.length;
        String[] vals = new String[6];

        //Log.i("","got:" + struct.length);
        for (int d = 0; d < _inds.size(); d++) {
            vals[0] = vals[1] = vals[2] = vals[3] = vals[4] = vals[5] = "";

            ERControl c = _controls.get(_inds.get(d));
            if (c._ctrllid == "")
                continue;
            int ardubhtes = _dtlengths[c._dtype] * c._dsize;

            switch (c._dtype) {
                case Konst.TYPE_BOOL:     // bool 1 byte
                    for (int i = 0; i < c._dsize; i++) {
                        vals[i] = Byte.toString(struct[I]);
                        //if(I+_dtlengths[c._dtype]>bytes)return false;
                        I += _dtlengths[c._dtype];
                    }
                    break;
                case Konst.TYPE_CHAR:     // char 1 byte
                    for (int i = 0; i < c._dsize; i++) {
                        vals[i] = Byte.toString(struct[I]);
                        //if(I+_dtlengths[c._dtype]>bytes)return false;
                        I += _dtlengths[c._dtype];
                    }
                    break;
                case Konst.TYPE_INT16:     // ardu int 2 byte
                    for (int i = 0; i < c._dsize; i++) {
                        short temp = (short) ((struct[I] & 0xFF) | (struct[I + 1] & 0xFF) << 8);
                        vals[i] = Short.toString(temp);
                        //if(I+_dtlengths[c._dtype]>bytes)return false;
                        I += _dtlengths[c._dtype];
                    }
                    break;
                case Konst.TYPE_LONG32:     // ardu long 4 byte
                    for (int i = 0; i < c._dsize; i++) {
                        long temp = (struct[I] & 0xFF) | (struct[I + 1] & 0xFF) << 8 | (struct[I + 2] & 0xFF) << 16 | (struct[I + 3] & 0xFF) << 24;
                        vals[i] = Long.toString(temp);
                        //if(I+_dtlengths[c._dtype]>bytes)return false;
                        I += _dtlengths[c._dtype];
                    }
                    break;   // ardu char[_dsize]
                case Konst.TYPE_STRING: {//string
                    StringBuilder sb = new StringBuilder(16);
                    boolean  apending=true;
                    for (int i = 0; i < c._dsize; i++)
                    {
                        if (apending&&struct[I] != 0)
                        {
                            sb.append((char) struct[I]);
                        }
                        else
                            apending=false;
                        //if(I+_dtlengths[c._dtype]>bytes)return false;
                        I += _dtlengths[c._dtype];
                    }
                    vals[0] = sb.toString();
                }
                break;
                case Konst.TYPE_FLOAT: {//float
                    for (int i = 0; i < c._dsize; i++)
                    {
                        byte loc[]={struct[I+3],struct[I+2],struct[I+1],struct[I]};
                        float f =  ByteBuffer.wrap(loc).getFloat();
                        vals[i] = Float.toString(f);
                        //if(I+_dtlengths[c._dtype]>bytes)return false;
                        I += _dtlengths[c._dtype];
                    }
                }
                break;
            }
            c.set_value(vals, c._dtype, c._dsize);
        }
        return true;
    }

    ERControl add_dummy_control(String uuid, String IN, int dtype){

        ERControl c = new display(this, true, uuid.substring(0,7)+"-"+IN, dtype);
        c._font = Math.min(_w,_h)/40;
        _controls.add(c);
        return c;
    }


    public void config_control(String uid, boolean control)
    {
        for(int i=0;i<_controls.size();i++) {
            ERControl c = _controls.get(i);

            if(uid.contains(c._ctrllid))
            {
                c.set_as_control();
            }
        }
    }

    ERControl get_dia_prop(String guidchar, String niw) {

        ERControl cr = null;
        for(int i=0;i<_controls.size();i++)
        {
            ERControl c = _controls.get(i);
            String    cid = c._ctrllid;
            boolean   tok = false;
            char      ct = cid.charAt(cid.length()-1);

            for(int j=0;j<niw.length();j++)
            {
                if(ct==niw.charAt(j)){
                    tok=true;
                    break;
                }
            }

            if(tok==true)
            {
                String cuuid = cid.substring(0, cid.length()-1);
                String cguid = guidchar.substring(0,cid.length()-1);

                if(guidchar.contains(cuuid)==true)
                {
                    cr=c;
                    c._gatguid = UUID.fromString(guidchar);
                    break;
                }
            }
        }
        return cr;
    }

    public void update_control(String guid, byte[] struct)
    {
        ERControl cr = null;
        for(int i=0;i<_controls.size();i++)
        {
            ERControl   c = _controls.get(i);
            String      cid = c._ctrllid;
            String      cuuid = cid.substring(0, cid.length()-1);
            String      cguid = guid.substring(0,cid.length()-1);

            if(cuuid.compareTo(cguid)==0)
            {
                _update_one_ctrl(c, struct,"");
                invalidate();
                break;
            }
        }
    }
}

class CanvasRenderer implements GLSurfaceView.Renderer
{
    VCanvas _canvas;

    public CanvasRenderer(VCanvas canvas)
    {
    }

    public void onDrawFrame(GL10 gl) {
        //_canvas.onDraw();
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glDisable(GL10.GL_DITHER);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glEnable(GL10.GL_DEPTH_TEST);
    }
}
