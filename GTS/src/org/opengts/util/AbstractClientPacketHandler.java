// ----------------------------------------------------------------------------
// Copyright 2006-2011, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Description:
//  Partial implementation of a ClientPacketHandler
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2009/04/02  Martin D. Flynn
//     -Added 'getMinimumPacketLength' and 'getMaximumPacketLength'
//  2011/05/13  Martin D. Flynn
//     -Added several convenience functions.
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.net.*;
import javax.net.*;

//import javax.net.ssl.*;

/**
*** An abstract implementation of the <code>ClientPacketHandler</code> interface
**/

public abstract class AbstractClientPacketHandler
    implements ClientPacketHandler
{

    // ------------------------------------------------------------------------

    public static final int     PACKET_LEN_ASCII_LINE_TERMINATOR = ServerSocketThread.PACKET_LEN_ASCII_LINE_TERMINATOR;
    public static final int     PACKET_LEN_END_OF_STREAM         = ServerSocketThread.PACKET_LEN_END_OF_STREAM;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static boolean DebugMode    = false;

    /**
    *** Sets the global debug mode 
    **/
    public static void SetDebugMode(boolean debug)
    {
        AbstractClientPacketHandler.DebugMode = debug;
    }
    
    /**
    *** Gets the global debug mode 
    **/
    public static boolean GetDebugMode()
    {
        return AbstractClientPacketHandler.DebugMode;
    }
    
    /**
    *** Gets the global debug mode 
    **/
    public static boolean IsDebugMode()
    {
        return AbstractClientPacketHandler.DebugMode;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private long                            sessStartTime   = 0L;
    private InetAddress                     inetAddr        = null;
    private boolean                         isTCP           = true;
    private boolean                         isTextPackets   = false;

    private boolean                         terminateSess   = true;

    private ServerSocketThread.SessionInfo  sessionInfo     = null;

    private int                             eventCount      = 0; // DCS use only

    public AbstractClientPacketHandler()
    {
        super();
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Sets the session info handler
    *** @param sessionInfo An implementation of the ServerSocketThread.SessionInfo interface
    **/
    public void setSessionInfo(ServerSocketThread.SessionInfo sessionInfo)
    {
        this.sessionInfo = sessionInfo;
    }
    
    /**
    *** Gets a reference to the ClientPacketHandler's session info implementation
    *** @return Reference to the session info object
    **/
    public ServerSocketThread.SessionInfo getSessionInfo()
    {
        return this.sessionInfo;
    }
    
    /**
    *** Gets the local port to which this socket is bound
    *** @return The local port to which this socket is bound
    **/
    public int getLocalPort()
    {
        return (this.sessionInfo != null)? this.sessionInfo.getLocalPort() : -1;
    }

    /**
    *** Gets the remote/client port used by the client to send the received packet
    *** @return The client remote port
    **/
    public int getRemotePort()
    {
        return (this.sessionInfo != null)? this.sessionInfo.getRemotePort() : -1;
    }

    // ------------------------------------------------------------------------

    /**
    *** Called when the session has started
    **/
    public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText) 
    {
        this.sessStartTime  = DateTime.getCurrentTimeSec();
        this.inetAddr       = inetAddr;
        this.isTCP          = isTCP;
        this.isTextPackets  = isText;
        this.clearEventCount();
        this.printSessionStart();
    }

    /**
    *** Displays the sesion startup message.
    *** (override to disable)
    **/
    protected void printSessionStart()
    {
        if (this.isDuplex()) {
            Print.logInfo("Begin TCP communication: " + this.getHostAddress());
        } else {
            Print.logInfo("Begin UDP communication: " + this.getHostAddress());
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the session start time
    **/
    public long getSessionStartTime()
    {
        return this.sessStartTime;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the packets are text
    *** @return True if the packets are text
    **/
    protected boolean isTextPackets() 
    {
        return this.isTextPackets;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this session is duplex (ie TCP)
    **/
    public boolean isDuplex()
    {
        return this.isTCP;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the IP adress of the host
    *** @return The IP adress of the host
    **/
    public InetAddress getInetAddress()
    {
        return this.inetAddr;
    }

    /**
    *** Gets the IP adress of the host
    *** @return The IP adress of the host
    **/
    public String getHostAddress()
    {
        String ipAddr = (this.inetAddr != null)? this.inetAddr.getHostAddress() : null;
        return ipAddr;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the event count state to the specified value
    **/
    public void setEventCount(int count)
    {
        this.eventCount = count;
    }

    /**
    *** Clears the event count state
    **/
    public void clearEventCount()
    {
        this.setEventCount(0);
    }

    /**
    *** Increments the event count state
    **/
    public void incrementEventCount()
    {
        this.eventCount++;
    }

    /**
    *** Gets the current value of the event count state
    **/
    public int getEventCount()
    {
        return this.eventCount;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns the client response port#
    **/
    public int getResponsePort()
    {
        return 0;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the minimum packet length
    **/
    public int getMinimumPacketLength()
    {
        // '-1' indicates that 'ServerSocketThread' should be used
        return -1;
    }

    /**
    *** Returns the maximum packet length
    **/
    public int getMaximumPacketLength()
    {
        // '-1' indicates that 'ServerSocketThread' should be used
        return -1;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns the initial packet that should be sent to the device upon openning 
    *** the socket connection .
    **/
    public byte[] getInitialPacket() 
        throws Exception
    {
        return null;
    }

    /** 
    *** Returns the final packet that should be sent to the device before closing 
    *** the socket connection.
    **/
    public byte[] getFinalPacket(boolean hasError) 
        throws Exception
    {
        return null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Callback to obtain the length of the next packet, based on the provided partial
    *** packet data.
    **/
    public int getActualPacketLength(byte packet[], int packetLen) 
    {
        return this.isTextPackets? PACKET_LEN_ASCII_LINE_TERMINATOR : packetLen;
    }

    /**
    *** Parse the provided packet information, and return any response that should
    *** be sent back to the remote device
    **/
    public abstract byte[] getHandlePacket(byte cmd[]) 
        throws Exception;

    // ------------------------------------------------------------------------

    /**
    *** Sets the terminate-session state to the specified value
    **/
    public void setTerminateSession(boolean term)
    {
        this.terminateSess = term;
    }
    
    /**
    *** Sets the terminate-session state to true
    **/
    public void setTerminateSession()
    {
        this.setTerminateSession(true);
    }
    
    /**
    *** Callback to determine if the current session should be terminated
    **/
    public boolean terminateSession() 
    {
        return this.terminateSess; // always terminate by default
    }

    /**
    *** Callback just before the session is terminated
    **/
    public void sessionTerminated(Throwable err, long readCount, long writeCount)
    {
        this.printSessionTerminated();
    }

    /**
    *** Displays the sesion startup message.
    *** (override to disable)
    **/
    protected void printSessionTerminated()
    {
        if (this.isDuplex()) {
            Print.logInfo("End TCP communication: " + this.getHostAddress());
        } else {
            Print.logInfo("End UDP communication: " + this.getHostAddress());
        }
    }

    // ------------------------------------------------------------------------

}
