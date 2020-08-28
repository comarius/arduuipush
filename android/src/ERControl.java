/**
 * Created by marius on 16/01/16.
 */
package com.ermote.ArduUiPush;


import android.content.Context;
import android.graphics.*;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by marius on 28/05/15.
 */
class ERControl {
    static final protected int CON = 0;
    static final protected int COF = 1;
    static final protected int BON = 2;
    static final protected int BOF = 3;

    protected int       _tid = 0;
    protected boolean   _iscontrol=false;
    protected Rect _rect = new Rect();
    protected int       _radius = 32;
    protected boolean   _shape = false;
    protected boolean   _orient = false;
    protected boolean   _fill = false;
    protected int       _rpos = 0;
    protected int       _margin = 1;
    protected int       _dtype = 0;
    protected int       _dsize = 0;
    protected String    _ctrllid = "";
    protected String    _caption = "";
    protected int       _font = 24;
    protected int[]     _color = new int[4];
    protected String[]  _lmM = new String[8];
    protected String[]  _svals = new String[8];
    protected Typeface _tfont;
    public int          _touchidx = -1;
    public boolean      _on = false;
    protected Point _xydown = new Point();
    protected Point     _xyup = new Point();
    protected Point     _xymoving = new Point();
    protected VCanvas   _parent;
    protected int       _iid = -1;
    protected int       _sstart = 0;
    protected int       _ssize = 0;
    protected boolean   _dragable = false;
    protected boolean   _autoscale = false;
    protected int       _roundrect;
    protected long      _lastevent = 0;
    protected byte      _cooksend=0;
    public    boolean   _dummygat = false;
    protected float     _val;
    protected float     _min;
    protected float     _max;
    protected float     _scale;
    protected UUID      _gatguid;
    protected float     _prescale = 1.0f;
    protected int       _leprop = 0;

    public ERControl(VCanvas parent, byte[] by, int idx, ArrayList<String> strl)
    {
        _dummygat = false;
        _parent = parent;
        _tid = by[idx++];
        _iscontrol = (_tid < 50);
        _roundrect = 0;
        _touchidx=-1;

        int left = (int) ( (double)((long)by[idx++]) * 16.0 * VCanvas._snap);
        int top =  (int) ( (double)((long)by[idx++]) * 16.0 * VCanvas._snap);
        int widd = (int) ( (double)((long)by[idx++]) * 16.0 * VCanvas._snap);
        int hegg = (int) ( (double)((long)by[idx++]) * 16.0 * VCanvas._snap);

        _rect.set(left, top, left + widd, top + hegg);
        _radius = Math.max(_rect.width() / 2, _rect.height() / 2);

        _shape = by[idx] == 1;
        idx++;
        _orient = by[idx] == 1;
        idx++;
        _fill = by[idx] == 1;
        idx++;
        _margin = by[idx];
        //_margin *= VCanvas._;
        idx++;
        _rpos = by[idx];
        idx++;
        _dtype = by[idx];
        idx++;
        _dsize = by[idx];
        if(_dtype == 4) //tweak string length
            _dsize=16;
        idx++;
        if (by[idx] > 0)
            _ctrllid = strl.get(by[idx]);
        idx++;
        if (by[idx] > 0)
            _caption = strl.get(by[idx]);
        idx++;
        _lmM[0] = strl.get(by[idx]);
        idx++;
        _lmM[1] = strl.get(by[idx]);
        idx++;
        _lmM[2] = strl.get(by[idx]);
        idx++;
        _lmM[3] = strl.get(by[idx]);
        idx++;
        _lmM[4] = strl.get(by[idx]);
        idx++;
        _lmM[5] = strl.get(by[idx]);
        idx++;

        int fi = by[idx];
        float fscale =parent._h / 600.0f;
        if (fi < VCanvas._fonts.length)
            _font = (int)((float)VCanvas._fonts[fi] * fscale);
        else
            _font = (int)((float)20 * fscale);
        idx++;

        //_tfont = new Typeface();


        _color[CON] = VCanvas._colors[by[idx]];
        idx++;
        _color[COF] = VCanvas._colors[by[idx]];
        idx++;
        _color[BON] = VCanvas._colors[by[idx]];
        idx++;
        _color[BOF] = VCanvas._colors[by[idx]];
        idx++;

        _svals[0] = _lmM[0];
        _svals[1] = _lmM[0]; // on
        _svals[2] = _lmM[0]; // of
        _svals[3] = _lmM[0];
        _svals[4] = _lmM[0];
        _svals[5] = _lmM[0]; //ffault value

        if(_lmM[2].compareTo("0")!=0)
            _prescale = 1.0f/Float.parseFloat(_lmM[2]);

        byte check = by[idx];
        idx++;
        if (check == 123)
            return;

        Log.e("ermotee", "INVALID CONTORL END");
    }

    static  int _leftDUmmy=0;
    static  int _topDUmmy=0;
    public ERControl(VCanvas parent,
                     boolean iscontrol,
                     String ctrlid,
                     int dtype)
    {
        _dummygat = true;
        _parent     = parent;
        _tid        = 0;
        _iscontrol  = iscontrol;
        _roundrect  = 0;
        _touchidx=-1;


        int left    = 10;
        int top     =  _topDUmmy;
        int widd    =  _parent._w/2;
        int hegg    =  _parent._h/32;

        _topDUmmy   += _parent._h/32;
        _rect.set(left, top, left+widd, top+hegg);
        _radius = Math.max(_rect.width() / 2, _rect.height() / 2);
        _shape = false;
        _orient = false;
        _fill   = false;
        _margin = 1;
        _rpos   = 0;
        _dtype  = Konst.TYPE_BINARY;
        _dsize  = 64;
        _ctrllid = ctrlid;
        _caption = ctrlid;
        _font = (int)((float) _parent._h/12);
        if(ctrlid.contains("N")) {
            _color[CON] = VCanvas._colors[3];
            _color[COF] = VCanvas._colors[2];
            _color[BON] = VCanvas._colors[1];
            _color[BOF] = VCanvas._colors[0];
        }else
        {
            _color[CON] = VCanvas._colors[7];
            _color[COF] = VCanvas._colors[6];
            _color[BON] = VCanvas._colors[5];
            _color[BOF] = VCanvas._colors[4];
        }
    }



    public void set_as_control()
    {
        _iscontrol=true;
    }
    public void pause(){

    }

    public int set_iid(int id) {
        //calc offsets in the structure
        // private int        _sstart = 0;
        // private int        _ssize = 0;
        ArrayList<Integer> ci;
        int strstart = 0;
        _iid = id;
        if (_tid >= 80) {
            ci = _parent._inds;
        } else {
            ci = _parent._ctrls;
        }
        if (ci.size() > 0) {
            ERControl c = _parent._controls.get(ci.get(ci.size() - 1));
            _sstart = c._sstart + c._ssize;
        }
        _ssize = VCanvas._dtlengths[_dtype] * _dsize;
        return _sstart + _ssize;
    }

    protected boolean _onoff_state() {
        return _on;
    }

    public void set_value(String[] values, int dtype, int dlen) {
        boolean inv = false;

        for (int i = 0; i < values.length; i++) {
            if(_svals[i].equals(values[i])==false) {
                _svals[i] = values[i];
                inv=true;
            }
        }
        if(_svals[0].equals(_lmM[1])==true)
            _on = true;
        else
            _on = false;
        _parse_value(values, dtype, dlen);
        //if(inv)
        //    _parent.invalidate(_rect.left,_rect.top,_rect.right, _rect.bottom);

        //Log.i("","on " + _on + " sval " + _svals[0] + " lim " + _lmM[0]);
    }

    protected void _parse_value(String[] values, int dtype, int dlen)
    {
        if(values[0].length()>=0) {
            String sf = values[0];
            switch (dtype) {
                case Konst.TYPE_STRING:
                    _val = 0;
                    _caption = values[0];
                    break;
                case Konst.TYPE_FLOAT:
                    _val = Float.parseFloat(sf) * _prescale;
                    _caption = String.format("%.2f", _val);
                    break;
                default:
                    _val = (float)Float.parseFloat(values[0]) * _prescale;
                    _caption = sf;
                    break;
            }
        }
        else
        {
            _val=0;
            _caption="";
        }
    }

    public boolean in_rect(int x, int y) {
        return _rect.contains(x, y);
    }

    public boolean touch_down(int x, int y, int sel) {
        if(_touchidx!=-1)
            return false;
        _touchidx = (sel);
        _xydown.set(x, y);
        return true;
    }

    public boolean touch_up(int x, int y, int sel) {
        if ((sel) != _touchidx)
            return false;
        //Log.i("","sel u" + sel);
        _xyup.set(x, y);
        _xydown.set(x, y);
        _touchidx = -1;
        if (in_rect(x, y)) {
            _clicked();
        }
        return true;
    }

    public void touch_move(int x, int y, int sel) {
        if (sel != _touchidx) return;
        //Log.i("","sel m" + sel);
        if (_dragable) {
            _move(x, y);
        }
    }

    protected void _move(int x, int y) {
    }

    protected void _clicked() {
        return;
    }

    public void draw_it(Canvas ctx, Paint p) {
        ctx.save();

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(_margin);
        if (_onoff_state()) {
            p.setColor(_color[BON]);
        } else {
            p.setColor(_color[BOF]);
        }

        if (_shape == false) {
            if(_roundrect!=0) {
                RectF rf = new RectF((float) _rect.left, (float) _rect.top, (float) _rect.right, (float) _rect.bottom);
                ctx.drawRoundRect(rf,(float)_roundrect,(float)_roundrect,p);
            }else {
                ctx.drawRect(_rect.left, _rect.top, _rect.right, _rect.bottom, p);
            }
        }
        else {
            ctx.drawCircle(_rect.centerX(), _rect.centerY(), _radius, p);
        }
        if (_fill == true) {
            p.setStyle(Paint.Style.FILL_AND_STROKE);

            if (_onoff_state()) {
                p.setColor(_color[CON]);
            } else {
                p.setColor(_color[COF]);
            }

            if (_shape == false) {
                if(_roundrect>0) {
                    RectF rf = new RectF((float) (_rect.left+_margin),
                            (float) (_rect.top-_margin),
                            (float) (_rect.right-_margin),
                            (float) (_rect.bottom+_margin));
                    ctx.drawRoundRect(rf, (float) _roundrect, (float) _roundrect, p);
                }else {
                    ctx.drawRect(_rect.left + _margin,
                            _rect.top + _margin,
                            _rect.right - _margin,
                            _rect.bottom - _margin, p);
                }
            }
            else {
                ctx.drawCircle(_rect.centerX(), _rect.centerY(), _radius - _margin, p);
            }

        }
        ctx.restore();


    }

    protected void _draw_limit(float around, float rad, boolean inc, Paint p,Canvas ctx) {
        ctx.drawLine(_rect.centerX()+(float)Math.cos(around) * rad,
                _rect.centerY()+(float)Math.sin(around) * rad,
                _rect.centerX()+(float)Math.cos(around) * (rad*1.2f),
                _rect.centerY()+(float)Math.sin(around) * (rad*1.2f),
                p);

    }

    protected void _draw_arrow(float around, float rad, boolean inc, Paint p,Canvas ctx){

        ctx.drawLine(_rect.centerX(),
                _rect.centerY(),
                _rect.centerX()+(float)Math.cos(around)*rad,
                _rect.centerY()+(float)Math.sin(around)*rad,
                p);

        ctx.drawLine(_rect.centerX()+(float)Math.cos(around)*rad,
                _rect.centerY()+(float)Math.sin(around)*rad,
                _rect.centerX()+(float)Math.cos(around+0.1f)*rad*0.8f,
                _rect.centerY()+(float)Math.sin(around+0.1f)*rad*0.8f,
                p);

        ctx.drawLine(_rect.centerX()+(float)Math.cos(around)*rad,
                _rect.centerY()+(float)Math.sin(around)*rad,
                _rect.centerX()+(float)Math.cos(around-0.1f)*rad*0.8f,
                _rect.centerY()+(float)Math.sin(around-0.1f)*rad*0.8f,
                p);
    }


    protected  void draw_txt(Canvas ctx, Paint p, boolean vc, Point off) {
        draw_txt( ctx,  p,  vc,  off, _color[CON]);
    }
    protected  void draw_txt(Canvas ctx, Paint p, boolean vc, Point off, int tcolor)
    {
        Rect    bounds = new Rect();
        ctx.save();
        p.setStrokeWidth(1);

        p.setTextSize(_font);

        p.setStyle(Paint.Style.STROKE);
        p.setColor(tcolor);
        p.setTextSize(_font);
        p.setTextAlign(Paint.Align.CENTER);
        p.getTextBounds(_caption, 0, _caption.length(), bounds);
        if(!vc)
            ctx.drawText(_caption,
                    (float) (_rect.left + _rect.width() / 2)+off.x,
                    (float) (_rect.top + _margin + bounds.height()+1) + off.y, p);
        else
            ctx.drawText(_caption,
                    (float) (_rect.left + _rect.width() / 2)+off.x,
                    (float) (_rect.top + _rect.height() / 2 + bounds.height()/2)+off.y, p);
        ctx.restore();
    }
    /*
    Byte.parseByte(String)
    Long.parseLong(String)
    Float.parseFloat(String)
    Double.parseDouble(String)
    "boolean",
    "char",
    "int",
    "long",
    "string"};
     */
    protected void _notify_action(boolean delay){
        // the data of this controls starts at _sstart
        long now = SystemClock.uptimeMillis();
        if(now-_lastevent < _parent._cmddelay) {
            if(delay){
                Konst._sleep((int)(1+_parent._cmddelay-(now-_lastevent)));
            }else{
                return;
            }
        }


        byte [] b = _parent._outbuffer;
        int  ioff = _sstart;
        int  bytes= 0; //check to fill _ssize;

        switch(_dtype) {
            case Konst.TYPE_BOOL:
                for(int i=0;i<_dsize;i++) {
                    b[ioff] = (byte)((int)Float.parseFloat(_svals[i]) & 0xFF);
                    ioff+=VCanvas._dtlengths[0];
                }
                break;
            case Konst.TYPE_CHAR:
                for(int i=0;i<_dsize;i++) {
                    b[ioff] = (byte)((int)Float.parseFloat(_svals[i]) & 0xFF);
                    ioff+=VCanvas._dtlengths[1];
                }
                break;
            case Konst.TYPE_INT16:

                for (int i = 0; i < _dsize; i++) {

                    int svalue = (int)Float.parseFloat(_svals[i]);
                    b[ioff] = (byte) ( (int) (svalue & 0x00FF));
                    b[ioff + 1] = (byte) (((int) (svalue & 0xFF00)) >> 8);
                    ioff += VCanvas._dtlengths[2];
                }

                break;
            case Konst.TYPE_LONG32:
                for(int i=0;i<_dsize;i++) {

                    b[ioff]   = (byte)((int)Float.parseFloat(_svals[i]) & 0x000000FF);
                    b[ioff+1] = (byte)(((int)Float.parseFloat(_svals[i])>>8) & 0xFF);
                    b[ioff+2] = (byte)(((int)Float.parseFloat(_svals[i])>>16) & 0xFF);
                    b[ioff+3] = (byte)(((int)Float.parseFloat(_svals[i])>>24) & 0xFF);
                    ioff+=VCanvas._dtlengths[3];
                }
                break;
            case Konst.TYPE_STRING: {
                byte[] by = _svals[0].getBytes();
                for (int i = 0; i < _dsize; i++) {
                    if (i < by.length) {
                        b[ioff] = (byte) (by[i] & 0xFF);
                    } else {
                        b[ioff] = 0;
                    }
                    ioff++;
                }
                b[ioff] = 0;
            }
            break;
            case Konst.TYPE_FLOAT:
            {
            }
            break;
        }
        _cooksend++;
        _parent.send_outbuff(_cooksend, _gatguid, _sstart);
        _lastevent = SystemClock.uptimeMillis();
        if(_cooksend>255)
            _cooksend=0;
    }

    public void notify_inits(){
    }
};

class shape extends ERControl
{
    public shape(VCanvas  parent, byte [] by, int idx, ArrayList<String> strl){
        super(parent, by,idx,strl);
    }

    @Override
    public void draw_it(Canvas ctx, Paint p)
    {

        super.draw_it(ctx, p);
    }

}

class gauge extends ERControl
{
    private int   _dx;
    private int   _dy;

    public gauge(VCanvas  parent, byte [] by, int idx, ArrayList<String> strl){

        super(parent, by,idx,strl);
        int lc = _rect.left;
        int tc = _rect.bottom;
        float fm = Float.parseFloat(_lmM[0]);
        float fM = Float.parseFloat(_lmM[1]);
        _min   = fm * _prescale;
        _max   = fM * _prescale;
        _dx    = 0; // Integer.parseInt(_lmM[2]);
        _dy    = 0; //Integer.parseInt(_lmM[3]);
        if(_dx<0)
        {
            if(_dx==-1)
                _dx = -(_rect.centerX()-_rect.left);
            else if(_dx==-2)
                _dx = _rect.centerX()-_rect.left;
        }
        if(_dy<0)
        {
            _dy = (_rect.centerY()-_rect.bottom) + _font * (-_dy);
        }

        if(fm==888 && fM==889) {
            _autoscale = true;
            _min = Float.MAX_VALUE;
            _max = Float.MIN_VALUE;
        }
        if (_shape == false) {
            if (_orient == false)
                _scale = (float) ((float) (_rect.height() - _font) / (float) (_max - _min));
            else
                _scale = (float) (float) (_rect.width() / (float) (_max - _min));
        } else {
            _scale = (240.0f / (float) (_max - _min));
        }
        _val   = _min;
    }

    @Override
    public  void set_value(String [] values, int dtype, int dlen) {
        super.set_value(values,dtype,dlen);

        if(_autoscale)
        { //auto adjust between values
            if (_min == Integer.MAX_VALUE)
            {
                _min = _val - _val/10.0f;
                _max = _val + _val/10.0f;
            }
            else {
                _min = Math.min(_val, _min);
                _max = Math.max(_val, _max);
            }
            if(_shape==false)
            {
                if (_orient == false)
                    _scale = (float) ((float) (_rect.height() - _font) / (float) (_max - _min));
                else
                    _scale = (float) ((float) (_rect.width() / (float) (_max - _min)));
            }
            else
            {
                _scale = 240.f/ (float)(_max - _min);
            }
        }
    }

    @Override
    public void draw_it(Canvas ctx, Paint p) {
        String sts = new String(String.valueOf(_val));
        int m = _margin;
        _margin=1;
        super.draw_it(ctx, p);
        _margin = m;
        if (_shape == false) {
            ctx.drawLine(_rect.left, _rect.top + _font + _margin, _rect.right, _rect.top + _font + _margin, p);

            p.setColor(_color[CON]);
            p.setStyle(Paint.Style.FILL);
            ctx.drawRect(_rect.left + _margin,
                    _rect.bottom - _margin - ((_val - _min) * _scale),
                    _rect.right - _margin,
                    _rect.bottom - _margin, p);
            p.setStyle(Paint.Style.STROKE);
            p.setColor(_color[CON]);
            draw_txt(ctx, p, false, new Point(0,0));
        } else {

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(_margin);
            float enda = ((_val -_min) * _scale);
            p.setColor(_color[CON]);
            RectF rf = new RectF((float) _rect.centerX()-_radius  + _margin,
                    (float) _rect.centerY()-_radius  + _margin,
                    (float) _rect.centerX()+_radius  - _margin,
                    (float) _rect.centerY()+_radius  - _margin);

            p.setStrokeWidth(_margin);

            ctx.drawArc(rf, 150.0f, enda, _fill, p);
            p.setStrokeWidth(1);
            _draw_arrow((float)Math.toRadians(enda+150.0f), _radius-(_margin*2)-5, true, p, ctx);

            _draw_limit((float)Math.toRadians(150.0f), _radius - (_margin * 2) - 5, true, p, ctx);
            _draw_limit((float)Math.toRadians(150.0f + 240.0f), _radius-(_margin*2)-5, true, p, ctx);

            super.draw_txt(ctx, p, true, new Point(_dx + (_shape ? _radius : 0),_dy));
        }
    }
}

class display extends ERControl
{
    public display(VCanvas  parent, byte [] by, int idx, ArrayList<String> strl){
        super(parent, by,idx,strl);
    }

    public display(VCanvas parent,
                   boolean iscontrol,
                   String ctrlid,
                   int dtype)
    {
        super(parent, iscontrol,ctrlid, dtype);
    }

    @Override
    public  void set_value(String [] values, int dtype, int dlen)
    {
        if(values[0].length()>0)
            _caption = values[0];
        else
            _caption="";
    }

    @Override
    public void draw_it(Canvas ctx, Paint p) {
        //super.draw_it(ctx,p);
        p.setTextSize(_font);
        if (_caption.contains("=")) {
            Rect bounds = new Rect();
            String[] separated = _caption.split("=");

            p.setColor( Color.rgb(0xFF, 0x00, 0x00));
            ctx.drawText(separated[0]+"= ", _rect.left + _margin, _rect.bottom - _margin -1 , p);
            p.getTextBounds(separated[0], 0, separated[0].length(), bounds);

            p.setColor( Color.rgb(0x00, 0xFF, 0xFF));
            ctx.drawText(separated[1], _rect.left + _margin + bounds.width() + 20 , _rect.bottom - _margin - 1 , p);
        }
        else
            ctx.drawText(_caption, _rect.left + _margin+1 , _rect.bottom - _margin -1 , p);
        //super.draw_txt(ctx, p, true, new Point(0,0));
    }
}


class dir extends ERControl
{
    private float _angle;

    public dir(VCanvas  parent, byte [] by, int idx, ArrayList<String> strl){
        super(parent, by,idx,strl);
        _angle=0.0f;
        _shape=true;
    }

    public  void set_value(String [] values, int dtype, int dlen)
    {
        _angle = Float.parseFloat(values[0]);
        _caption = String.format("%.2f",_angle);
    }

    @Override
    public void draw_it(Canvas ctx, Paint p)
    {
        super.draw_it(ctx, p);

        float around = _angle/360.0f * (float) (2.0f * (float)-Math.PI)+(float)Math.PI/2.0f;
        p.setStyle(Paint.Style.STROKE);
        p.setColor(_color[CON]);
        _draw_arrow(around-(float)Math.PI/2, _radius-(_margin*2), true, p, ctx);
        draw_txt(ctx,p,true, new Point(0,0));
    }
}

class graph extends ERControl
{
    ArrayList<Float>  _samples  = new ArrayList<Float>();

    private int        _count=0;
    private int        _capacity=0;
    private int        _step=1;
    private float      _scale=1.0f;

    public graph(VCanvas  parent, byte [] by, int idx, ArrayList<String> strl){

        super(parent, by,idx,strl);
        _step = (int)Float.parseFloat(_lmM[5]);
        if(_step<=0)_step=1;
        _capacity = _rect.width()/_step;
        _count = 0;
        float fm = Float.parseFloat(_lmM[0]);
        float fM = Float.parseFloat(_lmM[1]);
        _min   = fm * _prescale;
        _max   = fM * _prescale;
        _val   = 0;
        _scale = ((float)(_rect.height())/(float)(_max-_min));
        _on = false;
        if(fm==888 && fM==889) {
            _autoscale = true;
            _min=Float.MAX_VALUE;
            _max=Float.MIN_VALUE;
            _val=_min+(_max-_min)/2;
        }
    }

    @Override
    public  void set_value(String [] values, int dtype, int dlen) {
        super.set_value(values,dtype,dlen);

        if (_samples.size() >= _capacity) {
            _samples.remove(0);
        }
        _samples.add(_val);
        if(_autoscale)
        { //auto adjust between values
            if (_min == Float.MAX_VALUE)
            {
                _min = _val - (_val / 10.0f);
                _max = _val + (_val / 10.0f);
            }
            else
            {
                _min = Math.min(_val, _min);
                _max = Math.max(_val, _max);
            }
            _scale =  ((float)(_rect.height())/(float)(_max-_min));
        }
    }

    @Override
    public void draw_it(Canvas ctx, Paint p)
    {
        String st;
        Rect bounds = new Rect();

        super.draw_it(ctx, p);

        p.setStyle(Paint.Style.STROKE);
        p.setColor(_color[CON]);
        p.setTextSize(_rect.height()/15.0f);
        p.setAlpha(0x80);
        if(_min<_max) {
            for (int l = 0; l <= _rect.height() - _rect.height() / 10; l += _rect.height() / 10) {
                ctx.drawLine(_rect.left + _margin, _rect.bottom - l, _rect.right - _margin, _rect.bottom - l, p);
                st = String.format("%.2f", ((float) l / (float) _rect.height()) * (float) (_max - _min) + (float) _min);

                p.getTextBounds(st, 0, st.length(), bounds);
                p.setTextAlign(Paint.Align.LEFT);
                if (l >= _rect.height())
                    ctx.drawText(st, _rect.left + _margin + 1, _rect.bottom - l - _margin - 1 + bounds.height(), p);
                else
                    ctx.drawText(st, _rect.left + _margin + 1, _rect.bottom - l - _margin - 1, p);
            }
        }

        p.setAlpha(0xFF);
        if(_samples.size()>1) {
            p.setStyle(Paint.Style.STROKE);
            Path dp = new Path();
            p.setColor(_color[BON]);
            float val = _samples.get(0);
            dp.moveTo(_rect.left, _rect.bottom - _scale * (val - _min));
            int j = 1;
            for (; j < _samples.size(); j++) {
                val = _samples.get(j) - _min;
                dp.lineTo(_rect.left + (_step * j), _rect.bottom - _scale * val);
                if(_step > 5)
                    ctx.drawCircle(_rect.left + (_step * j), _rect.bottom - _scale * val,2,p);
            }
            ctx.drawCircle(_rect.left + (_step * j), _rect.bottom - _scale * val,5,p);

            if(_samples.size()>=_capacity-2)
                dp.lineTo(_rect.right - _margin, _rect.bottom - _scale * val);
            ctx.drawPath(dp, p);

            p.setColor(_color[BOF]);
            st = String.format("%.2f",_val);
            p.setTextSize(_rect.height()/10);
            p.getTextBounds(st, 0, st.length(), bounds);
            p.setTextAlign(Paint.Align.LEFT);
            ctx.drawText(st, _rect.right - bounds.width()-_margin-1, _rect.bottom - _margin-1, p);
        }
    }
}


class rgb extends ERControl
{
    private int _privargb;

    public rgb(VCanvas  parent, byte [] by, int idx, ArrayList<String> strl){
        super(parent, by,idx,strl);
        _fill=true;
        _privargb = 0xFF000000;
    }

    @Override
    public  void set_value(String [] values, int dtype, int dlen) {
        if(values[0]!="") {
            _privargb = (int)Float.parseFloat(values[0]);
            _color[COF] = _privargb|0xFF000000;
            _color[CON] = _privargb|0xFF000000;
        }
    }

    @Override
    public void draw_it(Canvas ctx, Paint p)
    {
        super.draw_it(ctx, p);
    }
}

class gyro extends ERControl
{
    private double _azim;
    private double _elev;
    private double _roll;
    RectF         _rectf;

    public gyro(VCanvas  parent, byte [] by, int idx, ArrayList<String> strl){

        super(parent, by,idx,strl);
        _on   = true;
        _azim = 0;
        _roll = 0;
        _elev = 0;
        _shape = true;
        float  ex = (float)_radius * (float)Math.sqrt(2.0);
        _rectf = new RectF(_rect.centerX()-_radius+_margin,
                _rect.centerY()-_radius+_margin,
                _rect.centerX()+_radius-_margin,
                _rect.centerY()+_radius-_margin);
    }

    @Override
    public  void set_value(String [] values, int dtype, int dlen) {
        super.set_value(values,dtype,dlen);
        _azim = Math.toRadians((double)Integer.valueOf(_svals[0]));
        _elev = Math.toRadians((double)Integer.valueOf(_svals[1]));
        _roll = Math.toRadians((double)Integer.valueOf(_svals[2]));
    }

    @Override
    public void draw_it(Canvas ctx, Paint p)
    {
        _on=true;
        _fill=true;
        super.draw_it(ctx, p);
        double  around = _azim;// * (double)(2.0 * -Math.PI) + (double)Math.PI/2;

        p.setColor(_color[BON]);
        p.setStyle(Paint.Style.STROKE);
        p.setTextSize(_font*.8f);
        String [] card ={"E","SE","S","SW","W","NW","N","NE",".","."};
        int       icard=0;
        for(float ang = 0; ang<Math.PI*2.0 && icard<8; ang+=Math.PI/4.0)
        {
            ctx.save();
            ctx.translate( _rectf.centerX()+ (float)Math.cos(around+ang)*(_rect.height()/2+_font),
                    _rectf.centerY()+ (float)Math.sin(around+ang)*(_rect.height()/2+_font));

            ctx.rotate((float)Math.toDegrees((float)around + (float)ang + (float)Math.PI/2));
            ctx.drawText(card[icard++], 0, 0, p);
            ctx.restore();
        }

        p.setColor(_color[COF]);
        p.setStyle(Paint.Style.FILL_AND_STROKE);
        float sa =(float)Math.toDegrees(-_elev + _roll);
        float ea =(float)Math.toDegrees(Math.PI  + (_elev * 2) + (_roll ));
        ctx.drawArc(_rectf, sa+1, ea-1 , false, p);

        p.setColor(_color[BON]);
        _draw_arrow(-(float)Math.PI/2, _radius-(_margin*2), true, p, ctx);
        Rect    bounds = new Rect();
        _caption = _svals[1]+ " / " + _svals[2];
        p.setTextSize(_font);
        p.getTextBounds(_caption, 0, _caption.length(), bounds);
        p.setTextAlign(Paint.Align.RIGHT);
        draw_txt(ctx,p,true, new Point(0,_rect.height()/3),_color[BON]);
    }
}

/*
    static final protected int CON = 0;
    static final protected int COF = 1;
    static final protected int BON = 2;
    static final protected int BOF = 3;
 */

class touch extends ERControl
{
    public touch(VCanvas  parent, byte [] by, int idx, ArrayList<String> strl){

        super(parent, by,idx,strl);
        _dragable = true;
        _xydown.set(_rect.centerX(), _rect.centerY());
        _xymoving = _xydown;
        _xyup = _xydown;
    }

    @Override
    protected void _move(int x, int y) {
        int dx = x -_xydown.x;
        int dy = y -_xydown.y;
        _xymoving.set(x,y);
        _svals[0]=String.valueOf(dx);
        _svals[1]=String.valueOf(dy);
        _xydown.x = x;
        _xydown.y = y;
        _notify_action(false);
    }

    @Override
    public boolean  touch_up(int x, int y, int sel) {
        if(super.touch_up(x,y,sel)){
            int dx = x -_xydown.x;
            int dy = y -_xydown.y;

            _svals[0]=String.valueOf(dx);
            _svals[1]=String.valueOf(dy);
            _notify_action(true);
            _xymoving.x=_rect.centerX();
            _xymoving.y=_rect.centerY();
            return true;
        }
        return false;
    }

    @Override
    public void draw_it(Canvas ctx, Paint p)
    {
        super.draw_it(ctx, p);

        p.setColor(_color[BON]);
        ctx.drawLine(_xydown.x,_xydown.y, _xymoving.x, _xymoving.y,p);
        ctx.drawCircle(_xymoving.x, _xymoving.y,16,p);
    }

}

class button extends ERControl
{
    public button(VCanvas  parent, byte [] by, int idx, ArrayList<String> strl){
        super(parent, by,idx,strl);
    }

    @Override
    public boolean touch_down(int x, int y, int sel){
        if(super.touch_down(x,y,sel)) {
            _on = true;
            _svals[0] = _lmM[1];
            _notify_action(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean touch_up(int x, int y, int sel){
        if(super.touch_up(x, y, sel)) {
            _on = false;
            _svals[0] = _lmM[0];
            _notify_action(true);
            return true;
        }
        return false;
    }

    @Override
    public void draw_it(Canvas ctx, Paint p)
    {
        super.draw_it(ctx, p);
        super.draw_txt(ctx, p,true, new Point(0,0));
    }
}

class check extends ERControl
{
    public check(VCanvas  parent, byte [] by, int idx, ArrayList<String> strl){

        super(parent, by,idx,strl);
        _radius = _rect.width()/2-_margin*2-4;
    }

    @Override
    public boolean touch_up(int x, int y, int sel){
        if(super.touch_up(x, y, sel)) {
            _on = !_on;
            if(_on)
                _svals[0] = _lmM[0];
            else
                _svals[0] = _lmM[1];
            _notify_action(true);
            return true;
        }
        return false;
    }


    @Override
    public void draw_it(Canvas ctx, Paint p)
    {
        //_fill=false;
        super.draw_it(ctx, p);


        if(_onoff_state()) {
            _caption="ON";
            p.setColor(_color[BON]);

            p.setStyle(Paint.Style.FILL_AND_STROKE);
            if(_orient==false)
                ctx.drawCircle(_rect.left+_rect.width()/2,_rect.top+_margin+_radius+1,_radius,p);
            else
                ctx.drawCircle(_rect.left+_margin+_radius+1,_rect.top+_rect.height()/2,_radius,p);


        }else {
            _caption="OFF";
            p.setColor(_color[BOF]);
            p.setStyle(Paint.Style.FILL_AND_STROKE);
            if(_orient==false)
                ctx.drawCircle(_rect.left+_rect.width()/2,_rect.bottom-_margin-_radius-1,_radius,p);
            else
                ctx.drawCircle(_rect.right-_margin-_radius-1,_rect.top+_rect.height()/2,_radius,p);
        }
        draw_txt(ctx, p, false, new Point(0,0));
    }
}


class slider extends ERControl
{
    private int     _snap;
    private float   _tick;
    private float   _scale;


    public slider(VCanvas  parent, byte [] by, int idx, ArrayList<String> strl){
        super(parent, by,idx,strl);
        _min      = Float.parseFloat(_lmM[0]);
        _max      = Float.parseFloat(_lmM[1]);
        _val      = Float.parseFloat(_lmM[3]);

        _snap      = (int)Float.parseFloat(_lmM[2]);
        _caption   = _lmM[3];
        _svals[0]  = _lmM[2];
        _tick      =_rect.height()/20;
        _on        = false;
        if(_val<_min)
            _val=_min;
        else if(_val>_max)
            _val=_max;
        _dragable = true;

        if(_orient==false)
            _scale = (float)((float)(_rect.height()-_font-_tick*2)/(float)(_max-_min));
        else
            _scale = (float)(float)((_rect.width()-_tick*2)/(float)(_max-_min));

    }

    @Override
    public boolean in_rect(int x, int y) {
        int ypos = _rect.bottom - _margin - (int)((_val - _min) * _scale) - (int)_tick/2;
        return x>_rect.left && x<_rect.right && y<ypos+_tick+16 && y>ypos-_tick-16;
    }

    @Override
    protected void _move(int x, int y) {
        int dx = x -_xydown.x;
        int dy = y -_xydown.y;
        _xydown.x = x;
        _xydown.y = y;
        if (_orient == false) //vertical //dy only
        {
            _val -= dy/_scale;
        }
        else
        {
            _val -= dx/_scale;
        }
        if(_val<_min)_val=_min;
        else if(_val>_max)_val=_max;
        _svals[0]=String.valueOf(_val);
        _caption =_svals[0];
        _notify_action(false);
    }

    @Override
    public boolean  touch_down(int x, int y, int sel) {
        if (super.touch_down(x, y, sel)) {
            _on = true;
            //_parent.invalidate();
            return true;
        }
        return false;
    }
    @Override
    public boolean  touch_up(int x, int y, int sel) {
        if(super.touch_up(x,y,sel)){
            _on=false;
            _svals[0]=String.valueOf(_val);
            _caption =_svals[0];
            _notify_action(true);
            if(_snap>=_min && _snap<=_max) {
                Konst._sleep(64);
                _svals[0]=_lmM[2];
                _caption =_lmM[2];
                _notify_action(true);
                _val =_snap;
            }
            return true;
        }
        return false;
    }

    @Override
    public void draw_it(Canvas ctx, Paint p)
    {

        super.draw_it(ctx, p);

        ctx.drawLine(_rect.left,
                _rect.top + _font + _margin,
                _rect.right, _rect.top + _font + _margin, p);

        draw_txt(ctx,p,false, new Point(0,0));

        p.setStyle(Paint.Style.FILL);

        if(_on)
            p.setColor(_color[BON]);
        else
            p.setColor(_color[BOF]);
        ctx.drawRect(_rect.left + _margin,
                (_rect.bottom - _margin - ((_val - _min) * _scale))-_tick-_tick/2-_margin*2,
                _rect.right   - _margin,
                (_rect.bottom - _margin - ((_val - _min) * _scale))+_tick-_tick/2-_margin*2, p);

        p.setColor(_color[CON]);
        ctx.drawLine(_rect.left+_rect.width()/2,
                _rect.top + _font + _margin,
                _rect.left+_rect.width()/2,
                _rect.bottom, p);


/*
        if(_snap>=_min && _snap<=_max) {
            p.setColor(_color[BOF]);
            ctx.save();
            p.setStyle(Paint.Style.STROKE);
            p.setColor(_color[BOF]);
            ctx.drawLine(_rect.left+_margin + 3,
                        _rect.bottom - _margin - ((_snap - _min) * _scale),
                        _rect.right   -_margin -3,
                        _rect.bottom - _margin - ((_snap - _min) * _scale), p);
            ctx.restore();
        }
*/

    }
}

class joy extends ERControl
{
    private int _xpos;
    private int _ypos;
    private int _miny;
    private int _maxy;
    private int _snapy;
    private int _minx;
    private int _maxx;
    private int _snapx;
    private float _scalex;
    private float _scaley;
    private int _valx;
    private int _valy;

    public joy(VCanvas  parent, byte [] by, int idx, ArrayList<String> strl){

        super(parent,by, idx,strl);

        _miny           = (int)Float.parseFloat(_lmM[0]);
        _maxy           = (int)Float.parseFloat(_lmM[1]);
        _valy=_snapy    = (int)Float.parseFloat(_lmM[2]);

        _minx           = (int)Float.parseFloat(_lmM[3]);
        _maxx           = (int)Float.parseFloat(_lmM[4]);
        _valx=_snapx    = (int)Float.parseFloat(_lmM[5]);

        _scalex = (float)(_radius*2.0f)/(float)(_maxx-_minx);
        _scaley = (float)(_radius*2.0f)/(float)(_maxy-_miny);

        _svals[0] = _lmM[2];
        _svals[1] = _lmM[5];

        _dragable = true;
        int left    =  _rect.centerX()-_radius;
        int bottom  =  _rect.centerY()+_radius;
        int right   =  _rect.centerX()+_radius;
        int top     =  _rect.centerY()-_radius;
        _rect.left = left;
        _rect.bottom = bottom;
        _rect.right = right;
        _rect.top = top;
    }


    @Override
    public boolean in_rect(int x, int y) {
        int py = _rect.bottom - (int)((_valy-_miny) * _scaley);
        int px = _rect.left +   (int)((_valx-_minx) * _scalex);
        return Math.abs(x-px) < _radius/3 && Math.abs(y-py) < _radius/3;
    }

    @Override
    protected void _move(int x, int y) {
        int dx = x -_xydown.x;
        int dy = y -_xydown.y;

        _xydown.x = x;
        _xydown.y = y;

        _valx += ((float)dx/_scalex);
        _valy -= ((float)dy/_scaley);

        if(_valx<_minx)_valx=_minx;
        else if(_valx>_maxx)_valx=_maxx;

        if(_valy<_miny)_valy=_miny;
        else if(_valy>_maxy)_valy=_maxy;

        _svals[1]=String.valueOf(_valx);
        _svals[0]=String.valueOf(_valy);
        _caption = _svals[0]+","+_svals[1];
        _notify_action(false);
    }

    @Override
    public boolean  touch_up(int x, int y, int sel) {
        if(super.touch_up(x,y,sel)){
            boolean snap = false;

            int dx = x -_xydown.x;
            int dy = y -_xydown.y;
            _valx += ((float)dx/_scalex);
            _valy -= ((float)dy/_scaley);

            if(_valx<_minx)_valx=_minx;
            else if(_valx>_maxx)_valx=_maxx;

            if(_valy<_miny)_valy=_miny;
            else if(_valy>_maxy)_valy=_maxy;

            _svals[0]=String.valueOf(_valy);
            _svals[1]=String.valueOf(_valx);
            _notify_action(true);

            if(_snapy>=_miny && _snapy<=_maxy) {
                _svals[0]=_lmM[2];
                _valy =_snapy;
                snap = true;
            }
            if(_snapx >= _minx && _snapx<=_maxx) {
                _svals[1]=_lmM[5];
                _valx =_snapx;
                snap = true;
            }
            if(snap) {
                _notify_action(true);
                _caption = _svals[0]+","+_svals[1];
            }
            return true;
        }
        return false;
    }

    @Override
    public void draw_it(Canvas ctx, Paint p)
    {
        _shape=true;
        super.draw_it(ctx, p);
        ctx.save();
        p.setColor(_color[CON]);
        ctx.drawLine(_rect.centerX(),_rect.centerY(),

                _rect.left + ((_valx-_minx) * _scalex),
                _rect.bottom -(( _valy-_miny) * _scaley),p);


        if(_fill)
            p.setStyle(Paint.Style.FILL_AND_STROKE);
        else
            p.setStyle(Paint.Style.STROKE);

        p.setColor(_color[CON]);
        ctx.drawCircle(_rect.left +   ((_valx-_minx) * _scalex),
                _rect.bottom - ((_valy-_miny) * _scaley),
                _radius/4, p);

        if(_snapy>=_miny &&
                _snapy<=_maxy &&
                _snapx>=_minx &&
                _snapx<=_maxx) {
            p.setColor(_color[BON]);
            ctx.drawCircle(_rect.left + ((_snapx - _minx) * _scaley),
                    _rect.bottom - ((_snapy - _miny) * _scaley),
                    _radius / 8, p);
        }
        draw_txt(ctx,p,true, new Point(0,0));
        ctx.restore();

    }
}


class accel extends ERControl implements SensorEventListener
{
    private Sensor _accelsens;
    private SensorManager _sensman = null;
    float                  _prevx;
    float                  _prevy;
    float                  _prevz;
    float                  _scale;
    int                    _svalsx;
    int                    _svalsy;
    ArrayList<Float>       _flatx = new ArrayList<Float>();
    ArrayList<Float>       _flaty = new ArrayList<Float>();
    ArrayList<Float>       _flatz = new ArrayList<Float>();

    public accel(VCanvas  parent, byte [] by, int idx, ArrayList<String> strl){
        super(parent, by,idx,strl);
        _caption = "N/A";
        _sensman = (SensorManager) _parent._mactivity.getSystemService(Context.SENSOR_SERVICE);
        if(_sensman !=null) {
            _accelsens = _sensman.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        _prevx=0;
        _prevy=0;
        _prevz=0;
        _svalsx = 0;
        _svalsy = 0;

        _on=false;
        _scale = (float)_radius/300.0f;
        _roundrect = 12;
    }

    public void onDestroy(){
        if(_sensman!=null)
            _sensman.unregisterListener(this);
    }

    @Override
    public void pause(){
        if(_sensman!=null)
            _sensman.unregisterListener(this);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent evt) {
        Sensor mySensor = evt.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            if(SystemClock.uptimeMillis()-_lastevent<_parent._cmddelay) {
                return;
            }

            Float [] r1 = new Float[9];
            Float [] r2 = new Float[3];
            Float [] r3 = new Float[3];

            float x = evt.values[0];
            float y = evt.values[1];
            float z = evt.values[2];

            _flatx.add(x);
            _flaty.add(y);
            _flatz.add(z);
            float avx=0.0f,avy=0.0f,avz=0.0f;
            for(int i=0;i<_flatx.size();i++) {
                avx+=_flatx.get(i);
                avy+=_flaty.get(i);
                avz+=_flatz.get(i);
            }
            x = avx/_flatx.size();
            y = avy/_flaty.size();
            z = avz/_flatz.size();

            if(_flatx.size() > 16){
                _flatx.remove(0);
                _flaty.remove(0);
                _flatz.remove(0);
            }

            //Log.i("", "x=" + x + " y=" + y + " z=" + z);

            if( Math.abs(_prevx-x) > 1.2 ||
                    Math.abs(_prevy-y) > 1.2||
                    Math.abs(_prevz-z) > 1.2){

                _prevx=x;
                _prevy=y;
                _prevz=z;

                _svals[0] = String.valueOf(_svalsx=(int)Math.floor(Math.toDegrees(x)));
                _svals[1] = String.valueOf(_svalsy=(int) Math.floor(Math.toDegrees(y)));
                _svals[2] = String.valueOf((int) Math.floor(Math.toDegrees(z)));

                _notify_action(false);
                _caption = _svals[0] + ", " + _svals[1] ;
                _parent.invalidate();
            }
        }
    }

    @Override
    public boolean touch_up(int x, int y, int sel){
        if(super.touch_up(x, y, sel)) {
            _on = !_on;
            if(_sensman!=null && _accelsens!=null) {
                if (_on) {
                    _sensman.registerListener(this, _accelsens, SensorManager.SENSOR_DELAY_UI);
                    _caption="ON";
                } else {
                    _sensman.unregisterListener(this);
                    _caption="OFF";
                }
            }
            return true;
        }
        return false;
    }
    @Override
    public void draw_it(Canvas ctx, Paint p) {
        super.draw_it(ctx, p);

        p.setTextSize(_font*.8f);
        draw_txt(ctx, p, false, new Point(0,0));
        p.setColor(_color[BON]);
        ctx.drawLine(_rect.centerX(), _rect.centerY(),
                _rect.centerX() + _svalsy * _scale, _rect.centerY() + _svalsx * _scale, p);
        ctx.drawCircle(_rect.centerX() + _svalsy * _scale, _rect.centerY() + _svalsx * _scale, 8, p);
        draw_txt(ctx, p, false, new Point(0,0));
    }
}

class cube3 extends ERControl {
    private double _rx;
    private double _ry;
    private double _rz;

    public cube3(VCanvas parent, byte[] by, int idx, ArrayList<String> strl) {
        super(parent, by, idx, strl);
        _rx = 0.0f;
        _ry = 0.0f;
        _rz = 0.0f;

    }

    @Override
    public void set_value(String[] values, int dtype, int dlen) {
        super.set_value(values, dtype, dlen);
        _rx = Math.toRadians((double) Integer.valueOf(_svals[0]));
        _ry = Math.toRadians((double) Integer.valueOf(_svals[1]));
        _rz = Math.toRadians((double) Integer.valueOf(_svals[2]));
    }

    @Override
    public void draw_it(Canvas ctx, Paint p) {
        super.draw_it(ctx, p);
        ctx.save();
        ctx.translate(_rect.centerX(), _rect.centerY());
        ctx.rotate(10, 10, 10);
        ctx.restore();
        //super.draw_txt(ctx, p, true, new Point(0,0));
    }
}
/*
    var ym=0;
    var yM=1;
    var yS=2;
    echo "t.lims[ym] = 100;";
    echo "t.lims[yM] = 100;";
    echo "t.lims[yS] = 100;";
 */
class acc3d extends ERControl
{
    private float _rx;
    private float _ry;
    private float _rz;
    private float _scalex;
    private float _scaley;
    private float _scalez;
    private float _min=0;
    private float _max=0;

    public acc3d(VCanvas  parent, byte [] by, int idx, ArrayList<String> strl){
        super(parent, by,idx,strl);
        _rx = 0.0f;
        _ry = 0.0f;
        _rz = 0.0f;
        float fm = Float.parseFloat(_lmM[0]);
        float fM = Float.parseFloat(_lmM[1]);
        _min   = fm * _prescale;
        _max   = fM * _prescale;

        if(fm==888 && fM==889) {
            _autoscale = true;
            _min = (float)Float.MAX_VALUE;
            _max = (float)Float.MIN_VALUE;
            _scalex = 1.0f;
        }
        else
            _scalex = (_rect.height()/2-(_margin*2))/(_max-_min);
    }

    public  void set_value(String [] values, int dtype, int dlen) {
        _rx = _prescale * ((float)Float.parseFloat(values[0]));
        _ry = _prescale * ((float)Float.parseFloat(values[1]));
        _rz = _prescale * ((float)Float.parseFloat(values[2]));
        if(_autoscale) {
            if (_rx < _min) _min = _rx - _rx/32.0f;
            if (_ry < _min) _min = _ry - _rx/32.0f;
            if (_rz < _min) _min = _rz - _rx/32.0f;
            if (_rx > _max) _max = _rx + _rx/32.0f;
            if (_rx > _max) _max = _ry + _rx/32.0f;
            if (_rz > _max) _max = _rz + _rx/32.0f;
        }

        _scalex = (_rect.height()/2-(_margin*2))/(_max-_min);
    }

    @Override
    public void draw_it(Canvas ctx, Paint p)
    {
        super.draw_it(ctx, p);
        ctx.save();
        p.setStrokeWidth(_margin);
        p.setColor(_color[COF]);
/*
        ctx.drawLine(_rect.centerX(),_rect.centerY(),
                _rect.centerX()+_rx*_scalex,
                _rect.centerY(),p);
*/
        if(_rx>0)
            _draw_arrow(0, _rx*_scalex, true, p, ctx);
        else
            _draw_arrow((float) Math.toRadians(180.0), _rx * _scalex, true, p, ctx);

        p.setColor(_color[BOF]);
        /*
        ctx.drawLine(_rect.centerX(),_rect.centerY(),
                _rect.centerX(),
                _rect.centerY()+_ry*_scalex,p);
*/
        if(_ry>0)
            _draw_arrow((float) Math.toRadians(270.0), _ry*_scalex, true, p, ctx);
        else
            _draw_arrow((float) Math.toRadians(90.0), _ry * _scalex, true, p, ctx);

        p.setColor(_color[CON]);
        float sqr2 = _scalex*0.707f;
/*
        ctx.drawLine(_rect.centerX(),_rect.centerY(),
                _rect.centerX()-_rz*sqr2,
                _rect.centerY()+_rz*sqr2,p);
*/

        if(_rz>0)
            _draw_arrow((float) Math.toRadians(135.0), _rz*_scalex, true, p, ctx);
        else
            _draw_arrow((float) Math.toRadians(225.0), _rz * _scalex, true, p, ctx);

        ctx.restore();

        p.setColor(VCanvas._colors[COF]);
        _caption = String.format("X:%.2f Y:%.2f Z:%.2f", _rx, _ry, _rz);
        draw_txt(ctx, p, true, new Point(0, _rect.height()/2+_margin - 32));
    }
}
