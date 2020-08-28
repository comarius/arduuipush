// Copyright: ermote.com &  Marius Octavian Chincisan
// marrius9876@gmail.com
//
#include <Arduino.h>
#include <SoftwareSerial.h>

#ifndef ERMOTE_H_H
#define ERMOTE_H_H

//=============================================================================
static int      HDR_LEN    =   4;
// HDR  0xFF OPT CMD LEN CRC
static byte     HDR_BYTE  = (byte)0xFF;
static byte     CMD_OPT   = (byte)0xFE;
static byte     DATA_OPT  = (byte)0xFC;

static byte     PING_CMD =  (int)0x1;
static byte     STOP_CMD =  (int)0x2;
static byte     ASK_CMD  =  (int)0x4;
static unsigned long TTL_MS = 30000;

//=============================================================================
class Ermote
{
    enum HDR_TYPE{
        NO_HDR      = 0,
        HDR_DATA    = 1,
        HDR_CMD     = 2
    };

    void _send_hdr(byte len, byte crc)
    {
        int sent=0;
        sent+= _pb->write(HDR_BYTE);
        sent+= _pb->write(DATA_OPT);
        sent+= _pb->write(len);
        sent+= _pb->write(crc);
        //_pb->flush();
        if(sent != 4 && _initialized)
	{
            Serial.println("SEND-ERR");
            _initialized = false;
        }
    }

    void _send_scr_hdr(int len, byte crc)
    {
        int sent=0;
        sent+=_pb->write(HDR_BYTE);
        sent+=_pb->write(CMD_OPT);
        sent+=_pb->write(len);
        sent+=_pb->write((len>>8) & 0xFF);
        if(sent != HDR_LEN && _initialized){
            Serial.println("SEND-ERR");
            _initialized = false;
        }
        delay(256);
    }


    void PrintHex8(uint8_t *data, uint8_t length) // prints 8-bit data in hex with leading zeroes
    {

        for (int i=0; i<length; i++) {
          Serial.print(data[i],HEX);
          Serial.print(" ");
        }
	Serial.println(" ");
    } 

public:

    Ermote(SoftwareSerial* pb, int ibreak):_pb(pb),
                                        _initialized(false),
                                        _keepalive(TTL_MS),
                                        _crcout(0),
                                        _crc(0),
                                        _off(0),
                                        _len(0),
                                        _looptime(0),
                                        _ct(millis()),
                                        _ibreak(ibreak),
                                        _pdata((byte*)&_eRDATA),
                                        _ht(NO_HDR),
  				        _receives(0),
                                        _delaybps(8),
                                        _second(0),
                                        _bps(0),
                                        _accum(0)
    {
        _pdata +=  _ibreak;
    };
 
    int isAlive()const{return _keepalive;}
    void setttl(unsigned long ttl)
	{
		TTL_MS = ttl + 1000;
	}
    bool writeread(){
        // calc delay on BPS 9600
        unsigned int ct = millis();
		       
        _looptime = (int)(ct - _ct);
        _ct = ct;
        delay((sizeof(_eRDATA)/32) + 1);
        _second+=_looptime;
        if(_second>1000)
        {
            _bps=_accum;
            _second=0;
        }
        if(sizeof(_eRDATA) - _ibreak > 0) //are controls here, sucks
        {
            if(_receive())
            {
                    _receives=0;
                    return true;
            }
            if(_receives < 2)
            {
                    ++_receives;
                    return false;
            }		
        }
        else
        {
            if(_receive())
            {
                 return true;
            }
        }
        _send();
        ++_receives;
        return false;
    }

    void begin(int bps)
    {
        int      i,l = sizeof(_eRDATA);
        byte* pt = (byte*)&_eRDATA;
        for(i=0; i<l; ++i)
        {
            pt[i]=0;
        }
        Serial.println("ER begin()");
        _screendef();
    }

    bool _shift()
    {
        for(int i=1; i<_off; i++)
        {
            _hdr[i-1]=_hdr[i];
        }
        --_off;
        return true;
    }
    inline void _reset_()
    {
        _off=0;
        _len=0;
        _crc=0;
        _ht=NO_HDR;
    }
protected:
    bool _receive()
    {
        byte by;
        bool shift=false;
        
	if(_pb->available()<=0)
        {
            delay(1);
            return false;
        }
        _keepalive = TTL_MS;
        if(_len==0)
        {
            while(_off<HDR_LEN)
            {
                if(_rec_more(&by)||shift)
                {
                    shift=false;
                    if(_check_hdr_byte(by,_off))
                    {
                        _hdr[_off++]=by;
#ifdef _DEBUG
                        Serial.print("A:");
                        PrintHex8(_hdr,_off);
#endif
                        continue;
                    }
                    if (_off>1)
                    {
                        shift = _shift();
#ifdef _DEBUG
                        Serial.print("S:");
                        PrintHex8(_hdr,_off);

#endif
                        continue;
                    }
                    _reset_();
                    continue;
                }
#ifdef _DEBUG
                Serial.print("LESS HDR:");
                Serial.println(_off, DEC);
                 PrintHex8(_hdr,_off);
#endif
               return false; //not enough header
            }
            _ht = _is_header();
            if(NO_HDR == _ht)
            {
                 Serial.println("BAD HDR");
                _off=0;
                _pdata = ((byte*)&_eRDATA)+_ibreak;
                return false;
            }
            _off=0;
        }

        if(_ht == HDR_CMD)
        {
            return _internal_job();
        }
        if(_ht == HDR_DATA)
        {
#ifdef _DEBUG
            Serial.println(_len,DEC);
			Serial.println("DATA=");
            PrintHex8(_hdr,HDR_LEN);
#endif
            register int exl = sizeof(_eRDATA) - _ibreak;
Serial.println(exl,DEC);
                        
            if(_len != exl)
            {
#ifdef _DEBUG
                Serial.print("REC-ERR LEN=");
                Serial.println(_len, DEC);
#endif //DEBUG
                _reset_();
                return false;
            }
            while(_off<_len)
            {
                if(_rec_more(&by))
		{
                    _pdata[_off++]=by;
		}
                else
		{
                    return false; //more data
		}
            }
            _reset_();
             return true;
        }
        return false;
    }

    //-------------------------------------------------------------------
    // udate indicators
    int _send()
    {
		//Serial.println(_keepalive,DEC);
        if(_keepalive > 0)
        {
            register int      i;
            byte     l = (byte)_ibreak & 0xFF;
            byte*    pt = (byte*)&_eRDATA;
            byte     crc = CRC8(pt,l);

            _keepalive -= _looptime;

            if(_crcout != crc)
            {
                if(_len != 0){
                    return 0;
                }
                _crcout = crc;
                _send_hdr(l,crc);
                for(i=0; i<l; ++i)
                {
                    _pb->write(pt[i]);
                    ++_accum;
                }
                delay(4);
#ifdef _DEBUG
                Serial.print("LEN = ");
                Serial.println(((int)l),DEC);
#endif
                return l;
            }
        }else{
            ;//Serial.println("cannot send");
            // _keepalive = 0;
        }
        return 0;
    };


private:
    bool  _rec_more(byte* pb)
    {
        if(_pb->available()>0)
        {
            *pb = _pb->read();
            ++_accum;
            return true;
        }
        delay(1);
        return false;
    }

    inline bool _check_hdr_byte(byte by, int off)
    {
        if(off == 0)
            return by == HDR_BYTE;
        if(off == 1)
            return by == CMD_OPT || by == DATA_OPT;
        return true;
    }

    bool _internal_job()
    {
#ifdef _DEBUG
        Serial.print("INTERN-LEN ");
        Serial.println(_len,HEX);
#endif //DEBUG
        _keepalive=TTL_MS;
        if(_len==ASK_CMD)   //ask for screen
        {
            if(_initialized==false)
            {
                _screendef();
                _initialized=true;
                Serial.println("ASK SCR");
            }
        }
        else if(_len==STOP_CMD)   //keep alive
        {
            if(_initialized){
                _initialized = false;
                Serial.println("STOPPED");
            }
        }
        _off   = 0;
        _len   = 0;
        _crc   = 0;
        return false;
    }

    HDR_TYPE _is_header()
    {
        _len=0;
        _crc=0;
        if(_hdr[0]==HDR_BYTE)
        {
            if(_hdr[1]==CMD_OPT)
            {
                _len = _hdr[2];
                _crc = _hdr[3];
                return HDR_CMD;
            }
            if(_hdr[1]==DATA_OPT)
            {
                _len = (int)_hdr[2];
                _crc = (byte)_hdr[3];
                return HDR_DATA;
            }
        }

        return NO_HDR;
    }

    int _screendef()
    {
        char        crc = 0;
        int         i,tl=0; //_sdf char array
        const char* p;
        byte* pt;
        int   si = 0,ci=0;

		delay(256);
        do
        {
            p = _sdf[si];
            if(p)
            {
                ci = 0;
                do
                {
                    tl++;
                }
                while(p[ci++]!=0);
            }
            else
            {
                tl++;
            }
            ++si;
        }
        while(p!=0);

        int sz = (int)sizeof(_sdp);
        pt = (byte*)&_sdp;
        crc = CRC8(pt,sz);
#ifdef _DEBUG
        Serial.println("SCR");
#endif //DEBUG
        tl += sz;
        _send_scr_hdr(tl, 0);
        si=0;
        do
        {
            p = _sdf[si];
            if(p!=0)
            {
                ci = 0;
                do
                {
                    _pb->write(p[ci] & 0xFF);
                }
                while(p[ci++] != (byte)0);
            }
            else
            {
                _pb->write((byte)0);
            }
            ++si;
        }while(p!=0);

        pt = (byte*)&_sdp;
        for(i=0; i<sz; i++)
            _pb->write(pt[i]);
		delay(256);
        return tl;
    };

//CRC-8 - based on the CRC8 formulas by Dallas/Maxim
//code released under the therms of the GNU GPL 3.0 license
    byte CRC8(const byte *data, int len)
    {
        byte crc=0;
        for(int i=0;i<len;i++)
            crc += data[i];
        if(crc==0)
            crc=255;
        return crc;

/*      taken out for the moment due some errors
        byte crc = 0x00;
        while (len-->0)
        {
            byte extract = (*data&0xFF);
            data++;
            for (byte tempI = 8; tempI>0; tempI--)
            {
                byte sum = (crc ^ extract) & 0x01;
                crc >>= 1;
                if (sum!=0)
                {
                    crc ^= 0x8C;
                }
                extract >>= 1;
            }
        }
        return (byte)(crc & 0xFF);
*/
    }
public:
    unsigned long     _looptime;
    uint32_t _bps;

private:
    SoftwareSerial* _pb;
    bool     _initialized;
    long     _keepalive;
    byte    _crcout;
    byte    _crc;
    int     _off;
    int     _len;
    byte    _hdr[12];
    unsigned long _ct;
    int     _ibreak ;
    byte*   _pdata;
    int     _delaybps;
    int	    _receives;
    HDR_TYPE _ht;
    uint32_t _second;
    uint32_t _accum;

};

 #endif / ERMOTE_H_
 
