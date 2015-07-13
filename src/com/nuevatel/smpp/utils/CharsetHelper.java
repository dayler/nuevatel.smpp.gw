/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.utils;

import java.util.BitSet;

/**
 *
 * @author luis
 */
public class CharsetHelper {


    private static final byte[] gsmCharMap = new byte[]{
    //	GSM 03.38 to ISO 8859-1
    //	When character can't be mapped (byte)63 '?' is used.
    //      x0		x1		x2		x3		x4		x5		x6		x7		x8		x9		xA		xB		xC		xD		xE		xF
    /*0x*/  (byte)64,	(byte)163,	(byte)36,	(byte)165,	(byte)232,	(byte)233,	(byte)249,	(byte)236,	(byte)242,	(byte)199,	(byte)10,	(byte)216,	(byte)248,	(byte)13,	(byte)197,	(byte)229,
    /*1x*/  (byte)63,	(byte)95,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)198,	(byte)230,	(byte)223,	(byte)201,
    /*2x*/  (byte)32,	(byte)33,	(byte)34,	(byte)35,	(byte)164,	(byte)37,	(byte)38,	(byte)39,	(byte)40,	(byte)41,	(byte)42,	(byte)43,	(byte)44,	(byte)45,	(byte)46,	(byte)47,
    /*3x*/  (byte)48,	(byte)49,	(byte)50,	(byte)51,	(byte)52,	(byte)53,	(byte)54,	(byte)55,	(byte)56,	(byte)57,	(byte)58,	(byte)59,	(byte)60,	(byte)61,	(byte)62,	(byte)63,
    /*4x*/  (byte)161,	(byte)65,	(byte)66,	(byte)67,	(byte)68,	(byte)69,	(byte)70,	(byte)71,	(byte)72,	(byte)73,	(byte)74,	(byte)75,	(byte)76,	(byte)77,	(byte)78,	(byte)79,
    /*5x*/  (byte)80,	(byte)81,	(byte)82,	(byte)83,	(byte)84,	(byte)85,	(byte)86,	(byte)87,	(byte)88,	(byte)89,	(byte)90,	(byte)196,	(byte)214,	(byte)209,	(byte)220,	(byte)167,
    /*6x*/  (byte)191,	(byte)97,	(byte)98,	(byte)99,	(byte)100,	(byte)101,	(byte)102,	(byte)103,	(byte)104,	(byte)105,	(byte)106,	(byte)107,	(byte)108,	(byte)109,	(byte)110,	(byte)111,
    /*7x*/  (byte)112,	(byte)113,	(byte)114,	(byte)115,	(byte)116,	(byte)117,	(byte)118,	(byte)119,	(byte)120,	(byte)121,	(byte)122,	(byte)228,	(byte)246,	(byte)241,	(byte)252,	(byte)224
    };


    public static final byte[] gsmExtendedCharmap = new byte[]{
    //	GSM 03.38 extended to ISO 8859-1
    //	When character can't be mapped (byte)63 '?' is used.
    //      x0		x1      	x2		x3		x4		x5		x6		x7		x8		x9		xA		xB		xC		xD		xE		xF
    /*1B0x*/(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)12,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*1B1x*/(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)94,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*1B2x*/(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)123,	(byte)125,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)92,
    /*1B3x*/(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)91,	(byte)126,	(byte)93,	(byte)63,
    /*1B4x*/(byte)124,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*1B5x*/(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*1B6x*/(byte)63,   (byte)63,       (byte)63,       (byte)63,       (byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*1B7x*/(byte)63,   (byte)63,       (byte)63,       (byte)63,       (byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63
    };

    private static final byte[] gsmAsciiCharMap = new byte[]{
    //	GSM 03.38 to ISO 8859-1
    //	When character can't be mapped (byte)63 '?' is used.
    //      x0		x1		x2		x3		x4		x5		x6		x7		x8		x9		xA		xB		xC		xD		xE		xF
    /*0x*/  (byte)64,	(byte)163,	(byte)36,	(byte)165,	(byte)232,	(byte)233,	(byte)249,	(byte)236,	(byte)242,	(byte)199,	(byte)10,	(byte)216,	(byte)248,	(byte)13,	(byte)197,	(byte)229,
    /*1x*/  (byte)63,	(byte)95,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)198,	(byte)230,	(byte)223,	(byte)201,
    /*2x*/  (byte)32,	(byte)33,	(byte)34,	(byte)35,	(byte)164,	(byte)37,	(byte)38,	(byte)39,	(byte)40,	(byte)41,	(byte)42,	(byte)43,	(byte)44,	(byte)45,	(byte)46,	(byte)47,
    /*3x*/  (byte)48,	(byte)49,	(byte)50,	(byte)51,	(byte)52,	(byte)53,	(byte)54,	(byte)55,	(byte)56,	(byte)57,	(byte)58,	(byte)59,	(byte)60,	(byte)61,	(byte)62,	(byte)63,
    /*4x*/  (byte)161,	(byte)65,	(byte)66,	(byte)67,	(byte)68,	(byte)69,	(byte)70,	(byte)71,	(byte)72,	(byte)73,	(byte)74,	(byte)75,	(byte)76,	(byte)77,	(byte)78,	(byte)79,
    /*5x*/  (byte)80,	(byte)81,	(byte)82,	(byte)83,	(byte)84,	(byte)85,	(byte)86,	(byte)87,	(byte)88,	(byte)89,	(byte)90,	(byte)196,	(byte)214,	(byte)209,	(byte)220,	(byte)167,
    /*6x*/  (byte)191,	(byte)97,	(byte)98,	(byte)99,	(byte)100,	(byte)101,	(byte)102,	(byte)103,	(byte)104,	(byte)105,	(byte)106,	(byte)107,	(byte)108,	(byte)109,	(byte)110,	(byte)111,
    /*7x*/  (byte)112,	(byte)113,	(byte)114,	(byte)115,	(byte)116,	(byte)117,	(byte)118,	(byte)119,	(byte)120,	(byte)121,	(byte)122,	(byte)228,	(byte)246,	(byte)241,	(byte)252,	(byte)224
    };


    public static final byte[] gsmAsciiExtendedCharmap = new byte[]{
    //	GSM 03.38 extended to ISO 8859-1
    //	When character can't be mapped (byte)63 '?' is used.
    //      x0		x1      	x2		x3		x4		x5		x6		x7		x8		x9		xA		xB		xC		xD		xE		xF
    /*1B0x*/(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)12,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*1B1x*/(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)94,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*1B2x*/(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)123,	(byte)125,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)92,
    /*1B3x*/(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)91,	(byte)126,	(byte)93,	(byte)63,
    /*1B4x*/(byte)124,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*1B5x*/(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*1B6x*/(byte)63,   (byte)63,       (byte)63,       (byte)63,       (byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*1B7x*/(byte)63,   (byte)63,       (byte)63,       (byte)63,       (byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63
    };


    private static final byte[] isoLatinCharMap = new byte[]{
    //    ISO 8859-1 to GSM 03.38
    //    When character can't be mapped (byte)63 '?' is used.
    //    If character maps to a extended GSM character (byte)27 (0x1B) is used. Use isoLatinExtendedCharMap
    //    to find out what character to append to obtain the full GSM escaped character.
    //    In addition, this map also contains the following special mappings:
    //
    //    (byte)0xe1 mapped to (byte)0x61	á -> a
    //    (byte)0xe2 mapped to (byte)0x61	â -> a
    //    (byte)0xe3 mapped to (byte)0x61	ã -> a
    //    (byte)0xea mapped to (byte)0x65	ê -> e
    //    (byte)0xeb mapped to (byte)0x65	ë -> e
    //    (byte)0xed mapped to (byte)0x69	í -> i
    //    (byte)0xee mapped to (byte)0x69	î -> i
    //    (byte)0xef mapped to (byte)0x69	ï -> i
    //    (byte)0xf3 mapped to (byte)0x6f	ó -> o
    //    (byte)0xf4 mapped to (byte)0x6f	ô -> o
    //    (byte)0xf5 mapped to (byte)0x6f	õ -> o
    //    (byte)0xfa mapped to (byte)0x75	ú -> u
    //    (byte)0xfb mapped to (byte)0x75	û -> u
    //    (byte)0xc0 mapped to (byte)0x41	À -> A
    //    (byte)0xc1 mapped to (byte)0x41	Á -> A
    //    (byte)0xc2 mapped to (byte)0x41	Â -> A
    //    (byte)0xc3 mapped to (byte)0x41	Ã -> A
    //    (byte)0xc8 mapped to (byte)0x45	È -> E
    //    (byte)0xca mapped to (byte)0x45	Ê -> E
    //    (byte)0xcb mapped to (byte)0x45	Ë -> E
    //    (byte)0xcc mapped to (byte)0x49	Ì -> I
    //    (byte)0xcd mapped to (byte)0x49	Í -> I
    //    (byte)0xce mapped to (byte)0x49	Î -> I
    //    (byte)0xcf mapped to (byte)0x49	Ï -> I
    //    (byte)0xd2 mapped to (byte)0x4F	Ò -> O
    //    (byte)0xd3 mapped to (byte)0x4F	Ó -> O
    //    (byte)0xd4 mapped to (byte)0x4F	Ô -> O
    //    (byte)0xd5 mapped to (byte)0x4F	Õ -> O
    //    (byte)0xd9 mapped to (byte)0x55	Ù -> U
    //    (byte)0xda mapped to (byte)0x55	Ú -> U
    //    (byte)0xdb mapped to (byte)0x55	Û -> U

    //      x0		x1		x2		x3		x4		x5		x6		x7		x8		x9		xA		xB		xC		xD		xE		xF
    /*0x*/  (byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)10,	(byte)63,	(byte)27,	(byte)13,	(byte)63,	(byte)63,
    /*1x*/  (byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*2x*/  (byte)32,	(byte)33,	(byte)34,	(byte)35,	(byte)2,	(byte)37,	(byte)38,	(byte)39,	(byte)40,	(byte)41,	(byte)42,	(byte)43,	(byte)44,	(byte)45,	(byte)46,	(byte)47,
    /*3x*/  (byte)48,	(byte)49,	(byte)50,	(byte)51,	(byte)52,	(byte)53,	(byte)54,	(byte)55,	(byte)56,	(byte)57,	(byte)58,	(byte)59,	(byte)60,	(byte)61,	(byte)62,	(byte)63,
    /*4x*/  (byte)0,	(byte)65,	(byte)66,	(byte)67,	(byte)68,	(byte)69,	(byte)70,	(byte)71,	(byte)72,	(byte)73,	(byte)74,	(byte)75,	(byte)76,	(byte)77,	(byte)78,	(byte)79,
    /*5x*/  (byte)80,	(byte)81,	(byte)82,	(byte)83,	(byte)84,	(byte)85,	(byte)86,	(byte)87,	(byte)88,	(byte)89,	(byte)90,	(byte)27,	(byte)27,	(byte)27,	(byte)27,	(byte)17,
    /*6x*/  (byte)63,	(byte)97,	(byte)98,	(byte)99,	(byte)100,	(byte)101,	(byte)102,	(byte)103,	(byte)104,	(byte)105,	(byte)106,	(byte)107,	(byte)108,	(byte)109,	(byte)110,	(byte)111,
    /*7x*/  (byte)112,	(byte)113,	(byte)114,	(byte)115,	(byte)116,	(byte)117,	(byte)118,	(byte)119,	(byte)120,	(byte)121,	(byte)122,	(byte)27,	(byte)27,	(byte)27,	(byte)27,	(byte)63,
    /*8x*/  (byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*9x*/  (byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*Ax*/  (byte)63,	(byte)64,	(byte)63,	(byte)1,	(byte)36,	(byte)3,	(byte)63,	(byte)95,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*Bx*/  (byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)96,
    /*Cx*/  (byte)65,	(byte)65,	(byte)65,	(byte)65,	(byte)91,	(byte)14,	(byte)28,	(byte)9,	(byte)69,	(byte)31,	(byte)69,	(byte)69,	(byte)73,	(byte)73,	(byte)73,	(byte)63,
    /*Dx*/  (byte)63,	(byte)93,	(byte)79,	(byte)79,	(byte)79,	(byte)79,	(byte)92,	(byte)63,	(byte)11,	(byte)85,	(byte)85,	(byte)85,	(byte)94,	(byte)63,	(byte)63,	(byte)30,
    /*Ex*/  (byte)127,	(byte)97,	(byte)97,	(byte)97,	(byte)123,	(byte)15,	(byte)29,	(byte)63,	(byte)4,	(byte)5,	(byte)101,	(byte)101,	(byte)7,	(byte)105,	(byte)105,	(byte)105,
    /*Fx*/  (byte)63,	(byte)125,	(byte)8,	(byte)111,	(byte)111,	(byte)111,	(byte)124,	(byte)63,	(byte)12,	(byte)6,	(byte)117,	(byte)117,	(byte)126,	(byte)16,	(byte)18,	(byte)19
    };

    private static final byte[] isoLatinExtendedCharMap = new byte[]{
    //      ISO 8859-1 to GSM 03.38
    //      when character can't be mapped (byte)63 '?' is used
    //      Use where isoLatinCharMap returns (byte)27 (0x1B)
    //      x0		x1		x2		x3		x4		x5		x6		x7		x8		x9		xA		xB		xC		xD		xE		xF
    /*0x*/  (byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)10,	(byte)63,	(byte)10,	(byte)13,	(byte)63,	(byte)63,
    /*1x*/  (byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*2x*/  (byte)32,	(byte)33,	(byte)34,	(byte)35,	(byte)2,	(byte)37,	(byte)38,	(byte)39,	(byte)40,	(byte)41,	(byte)42,	(byte)43,	(byte)44,	(byte)45,	(byte)46,	(byte)47,
    /*3x*/  (byte)48,	(byte)49,	(byte)50,	(byte)51,	(byte)52,	(byte)53,	(byte)54,	(byte)55,	(byte)56,	(byte)57,	(byte)58,	(byte)59,	(byte)60,	(byte)61,	(byte)62,	(byte)63,
    /*4x*/  (byte)0,	(byte)65,	(byte)66,	(byte)67,	(byte)68,	(byte)69,	(byte)70,	(byte)71,	(byte)72,	(byte)73,	(byte)74,	(byte)75,	(byte)76,	(byte)77,	(byte)78,	(byte)79,
    /*5x*/  (byte)80,	(byte)81,	(byte)82,	(byte)83,	(byte)84,	(byte)85,	(byte)86,	(byte)87,	(byte)88,	(byte)89,	(byte)90,	(byte)60,	(byte)47,	(byte)62,	(byte)20,	(byte)17,
    /*6x*/  (byte)63,	(byte)97,	(byte)98,	(byte)99,	(byte)100,	(byte)101,	(byte)102,	(byte)103,	(byte)104,	(byte)105,	(byte)106,	(byte)107,	(byte)108,	(byte)109,	(byte)110,	(byte)111,
    /*7x*/  (byte)112,	(byte)113,	(byte)114,	(byte)115,	(byte)116,	(byte)117,	(byte)118,	(byte)119,	(byte)120,	(byte)121,	(byte)122,	(byte)40,	(byte)64,	(byte)41,	(byte)61,	(byte)63,
    /*8x*/  (byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*9x*/  (byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*Ax*/  (byte)63,	(byte)64,	(byte)63,	(byte)1,	(byte)36,	(byte)3,	(byte)63,	(byte)95,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,
    /*Bx*/  (byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)63,	(byte)96,
    /*Cx*/  (byte)65,	(byte)65,	(byte)65,	(byte)65,	(byte)91,	(byte)14,	(byte)28,	(byte)9,	(byte)69,	(byte)31,	(byte)69,	(byte)69,	(byte)73,	(byte)73,	(byte)73,	(byte)63,
    /*Dx*/  (byte)63,	(byte)93,	(byte)79,	(byte)79,	(byte)79,	(byte)79,	(byte)92,	(byte)63,	(byte)11,	(byte)85,	(byte)85,	(byte)85,	(byte)94,	(byte)63,	(byte)63,	(byte)30,
    /*Ex*/  (byte)127,	(byte)97,	(byte)97,	(byte)97,	(byte)123,	(byte)15,	(byte)29,	(byte)63,	(byte)4,	(byte)5,	(byte)101,	(byte)101,	(byte)7,	(byte)105,	(byte)105,	(byte)105,
    /*Fx*/  (byte)63,	(byte)125,	(byte)8,	(byte)111,	(byte)111,	(byte)111,	(byte)124,	(byte)63,	(byte)12,	(byte)6,	(byte)117,	(byte)117,	(byte)126,	(byte)16,	(byte)18,	(byte)19
    };


    public static byte[] GSMToISOLatin(byte[] gsmBytes){
        byte[] result = new byte[gsmBytes.length];
        for (int i=0; i<gsmBytes.length; i++){
            if ((gsmBytes[i] == 0x1B) && (i!=gsmBytes.length-1)){
                result[i] = gsmExtendedCharmap[(gsmBytes[i+1] & 255)];
                i++;
            }
            else {
                result[i] = gsmCharMap[(gsmBytes[i] & 255)];
            }
        }
        return result;
    }


    public static byte[] ISOLatinToGSM(byte[] isoLatinBytes, TPUDLOverhead tpUDLOverhead){
        int tmpIndex=0;
        byte[] result = new byte[isoLatinBytes.length];
        for (int i=0; i<isoLatinBytes.length; i++){
            result[i+tmpIndex] = isoLatinCharMap[isoLatinBytes[i] & 255];
            if (result[i+tmpIndex] == 0x1B){
                byte[] tmpResult = new byte[result.length+1];
                System.arraycopy(result, 0, tmpResult, 0, i+tmpIndex+1);
                tmpResult[i+tmpIndex+1]= isoLatinExtendedCharMap[isoLatinBytes[i] & 255];
                result=new byte[tmpResult.length];
                System.arraycopy(tmpResult, 0, result, 0, tmpResult.length);
                tmpIndex++;
            }
        }
        tpUDLOverhead.setOverhead(tmpIndex);
        return result;
    }

    public static byte[] encode7bitUserData(byte[] udhOctets, byte[] textSeptets){
        // UDH octets and text have to be encoded together in a single pass
        // UDH octets will need to be converted to unencoded septets in order
        // to properly pad the data
        if (udhOctets == null){
            // convert string to uncompressed septets
            return unencodedSeptetsToEncodedSeptets(textSeptets);
        }
        else{
            // convert UDH octets as if they were encoded septets
            // NOTE: DO NOT DISCARD THE LAST SEPTET IF IT IS ZERO
            byte[] udhSeptets = encodedSeptetsToUnencodedSeptets(udhOctets, false);
            // combine the two arrays and encode them as a whole
            byte[] combined = new byte[udhSeptets.length + textSeptets.length];
            System.arraycopy(udhSeptets, 0, combined, 0, udhSeptets.length);
            System.arraycopy(textSeptets, 0, combined, udhSeptets.length, textSeptets.length);
            // convert encoded byte[] to a PDU string
            return unencodedSeptetsToEncodedSeptets(combined);
        }
    }

    public static byte[] decode7bitEncoding(byte[] udhData, byte[] encodedPduData){
        int udhLength = ((udhData == null) ? 0 : udhData.length);
        if (udhLength == 0){
            // just process the whole pdu as one thing
            return encodedSeptetsToUnencodedSeptets(encodedPduData);
        }
        else{
            byte[] unencodedUDH = encodedSeptetsToUnencodedSeptets(udhData, false);
            byte[] decoded = encodedSeptetsToUnencodedSeptets(encodedPduData);
            byte[] result = new byte[udhLength+(decoded.length-unencodedUDH.length)];
            System.arraycopy(udhData, 0, result, 0, udhLength);
            System.arraycopy(decoded, unencodedUDH.length, result, udhLength, result.length-udhLength);
            return result;
        }
    }

    public static byte[] unencodedSeptetsToEncodedSeptets(byte[] septetBytes){
        byte[] txtBytes;
        byte[] txtSeptets;
        int txtBytesLen;
        BitSet bits;
        int i, j;
        txtBytes = septetBytes;
        txtBytesLen = txtBytes.length;
        bits = new BitSet();
        for (i = 0; i < txtBytesLen; i++){
            for (j = 0; j < 7; j++){
                if ((txtBytes[i] & (1 << j)) != 0){
                        bits.set((i * 7) + j);
                    }
            }
        }
        // big diff here
        int encodedSeptetByteArrayLength = txtBytesLen * 7 / 8 + ((txtBytesLen * 7 % 8 != 0) ? 1 : 0);
        txtSeptets = new byte[encodedSeptetByteArrayLength];
        for (i = 0; i < encodedSeptetByteArrayLength; i++){
            for (j = 0; j < 8; j++){
                txtSeptets[i] |= (byte) ((bits.get((i * 8) + j) ? 1 : 0) << j);
            }
        }
        return txtSeptets;
    }
    
    public static byte[] encodedSeptetsToUnencodedSeptets(byte[] octetBytes) {
        return encodedSeptetsToUnencodedSeptets(octetBytes, true);
    }

    public static byte[] encodedSeptetsToUnencodedSeptets(byte[] octetBytes, boolean discardLast){
        byte newBytes[];
        BitSet bitSet;
        int i, j, value1, value2;
        bitSet = new BitSet(octetBytes.length * 8);
        value1 = 0;
        for (i = 0; i < octetBytes.length; i++)
            for (j = 0; j < 8; j++){
                value1 = (i * 8) + j;
                if ((octetBytes[i] & (1 << j)) != 0) bitSet.set(value1);
            }
        value1++;
        // this is a bit count NOT a byte count
        value2 = value1 / 7 + ((value1 % 7 != 0) ? 1 : 0); // big diff here
        //System.out.println(octetBytes.length);
        //System.out.println(value1+" --> "+value2);
        if (value2 == 0) value2++;
        newBytes = new byte[value2];
        for (i = 0; i < value2; i++){
            for (j = 0; j < 7; j++){
                if ((value1 + 1) > (i * 7 + j)){
                    if (bitSet.get(i * 7 + j)){
                        newBytes[i] |= (byte) (1 << j);
                    }
                }
            }
        }
        if (discardLast){
            // when decoding a 7bit encoded string
            // the last septet may become 0, this should be discarded
            // since this is an artifact of the encoding not part of the
            // original string
            // this is only done for decoding 7bit encoded text NOT for
            // reversing octets to septets (e.g. for the encoding the UDH)
            if (newBytes[newBytes.length - 1] == 0){
                byte[] retVal = new byte[newBytes.length - 1];
                System.arraycopy(newBytes, 0, retVal, 0, retVal.length);
                return retVal;
            }
        }
        return newBytes;
    }
}
