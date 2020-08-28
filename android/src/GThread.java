package com.ermote.ArduUiPush;


import com.ermote.ArduUiPush.R;

/**
 * Created by marius on 16/01/16.
 */
public class GThread extends Thread{

    private TCall   _cb;
    private boolean _run;
    private  boolean _runing;

    GThread(TCall t){
        _cb=t;
        _run=false;
    }

    public  void create(){
        this.tdestroy();
        _run=true;
        start();
    }

    public void tdestroy(){
        _run=false;
        try {

            join();
        }catch(InterruptedException e){}
    }

    public synchronized  boolean alive(){
        return _run;
    }

    public void run(){
        _runing=true;
        _cb.t_call(this);
        _runing=false;
    }
}

