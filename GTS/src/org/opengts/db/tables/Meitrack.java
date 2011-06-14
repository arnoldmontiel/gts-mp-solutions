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
// Notes:
//  This 'Device' table currently assumes a 1-1 relationship between the device hardware
//  used to perform the tracking and communication, and the Vehicle being tracked.
//  However, it is possible to have more than one device on a given vehicle, or a single
//  hardware device may be moved between vehicles.  Ideally, this table should be split
//  into 2 separate tables: The Device table, and the MobileAsset table.
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/09  Martin D. Flynn
//     -Integrate DBException
//  2006/05/23  Martin D. Flynn
//     -Changed column 'uniqueID' to a 'VARCHAR(40)'
//  2007/01/25  Martin D. Flynn
//     -Moved to "OpenGTS"
//     -Various new fields added
//  2007/03/25  Martin D. Flynn
//     -Added 'equipmentType', 'groupID'
//     -Moved to 'org.opengts.db.tables'
//  2007/04/15  Martin D. Flynn
//     -Added 'borderCrossing' column.
//  2007/06/30  Martin D. Flynn
//     -Added 'getFirstEvent', 'getLastEvent'
//  2007/07/14  Martin D. Flynn
//     -Added '-uniqueid' command-line option.
//  2007/07/27  Martin D. Flynn
//     -Added 'notifyAction' column
//  2007/08/09  Martin D. Flynn
//     -Renamed command-line option "uniqid" to "uniqueid"
//     -Set 'deviceExists' to true when creating a new device.
//  2007/09/16  Martin D. Flynn
//     -Integrated DBSelect
//     -Added handlers for client device errors, diagnostics, and properties.
//     -Added device lookup for the specified unique-id.
//  2007/11/28  Martin D. Flynn
//     -Added columns 'lastBorderCrossTime', 'simPhoneNumber', 'lastInputState'.
//     -Added additional 'Entity' methods
//     -Added OpenDMTP 'CommandErrors' definition section.
//     -Added '-editall' command-line option to display all fields.
//  2007/12/13  Martin D. Flynn
//     -Added an EventData filter to check for invalid odometer values.
//  2007/01/10  Martin D. Flynn
//     -Added column 'notes', 'imeiNumber'
//     -Removed handlers for client device errors, diagnostics, and properties
//      (these handlers have been implemented in 'DeviceDBImpl.java')
//  2008/02/11  Martin D. Flynn
//     -Added columns 'FLD_deviceCode', 'FLD_vehicleID'
//  2008/03/12  Martin D. Flynn
//     -Added column 'FLD_notifyPriority'
//  2008/05/14  Martin D. Flynn
//     -Integrated Device DataTransport interface
//  2008/05/20  Martin D. Flynn
//     -Fixed 'UniqueID" to again make it visible to the CLI record editor.
//  2008/06/20  Martin D. Flynn
//     -Added column 'FLD_notifyDescription'
//  2008/07/21  Martin D. Flynn
//     -Added column 'FLD_linkURL'
//  2008/08/24  Martin D. Flynn
//     -Added 'validGPS' argument to 'getRangeEvents' and 'getLatestEvents'
//  2008/09/01  Martin D. Flynn
//     -Added optional field list "FixedLocationFieldInfo"
//     -Added field/column "FLD_smsEmail"
//  2008/10/16  Martin D. Flynn
//     -Added FLD_lastPingTime, FLD_totalPingCount
//  2008/12/01  Martin D. Flynn
//     -Added FLD_linkDescription, FLD_pushpinID
//     -Added optional field list 'GeoCorridorFieldInfo'
//  2009/05/24  Martin D. Flynn
//     -Added FLD_pendingPingCommand, FLD_remotePortCurrent
//     -Added FLD_lastValidLatitude/Longitude to optimize Geozone calculations.
//     -Added FLS_lastOdometerKM to optimize GPS odometer calculations.
//  2009/06/01  Martin D. Flynn
//     -Increased background thread pool size/limit to 25.
//  2009/09/23  Martin D. Flynn
//     -Added support for ignoring/truncating events with future timestamps
//     -Added FLD_maxPingCount
//  2009/10/02  Martin D. Flynn
//     -Changed "getGeozoneTransition" to return an array of Geozone transitions,
//      fixing the case where 2 adjacent events occur in 2 different geozones.
//  2009/11/01  Martin D. Flynn
//     -Added FLD_expectAck, FLD_lastAckCommand, FLD_lastAckTime
//  2009/12/16  Martin D. Flynn
//     -Added command-line check for "Periodic Maintenance/Service Due" (-maintkm=email)
//  2010/01/29  Martin D. Flynn
//     -Added FLD_listenPortCurrent
//  2010/04/11  Martin D. Flynn
//     -Added FLD_dataKey, FLD_displayColor, FLD_licensePlate
//     -Added 'deleteEventDataPriorTo' to delete old historical EventData records.
//  2010/07/04  Martin D. Flynn
//     -Added FLD_expirationTime, FLD_maintIntervalKM1, FLD_maintOdometerKM1
//  2010/07/18  Martin D. Flynn
//     -Added FLD_lastBatteryLevel, FLD_fuelCapacity
//  2010/09/09  Martin D. Flynn
//     -Added "deleteOldEvents" option
//  2010/11/29  Martin D. Flynn
//     -Added FLD_lastFuelLevel
//     -Added configurable "maximum odometer km"
//  2011/01/28  Martin D. Flynn
//     -Added FLD_lastOilLevel
//  2011/03/08  Martin D. Flynn
//     -Added "getFieldValueString"
//     -Added alternate key "simphone" to field FLD_simPhoneNumber.
//     -Added "loadDeviceBySimPhoneNumber(...)"
//     -Added column FLD_speedLimitKPH
//  2011/04/01  Martin D. Flynn
//     -Added FuelManager module support (requires installed FuelManager)
//     -If "ALLOW_USE_EMAIL_WRAPPER" is false, "getNotifyUseWrapper()" returns false.
//  2011/05/13  Martin D. Flynn
//     -Change to invalid speed maximum checking.
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.util.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.RuleFactory.NotifyAction;
import org.opengts.db.tables.Transport.Encodings;


/**
*** This class represents a tracked asset (ie. something that is being tracked).
*** Currently, this DBRecord also represents the tracking hardware device as well.
**/

public class Meitrack // Asset
    extends DeviceRecord<Meitrack>
    implements DataTransport
{

    // ------------------------------------------------------------------------

    /* optimization for caching status code descriptions */
    public static       boolean CACHE_STATUS_CODE_DESCRIPTIONS      = true;

    /* ReverseGeocodeProvider required on command-line "-insertGP" */
    public static       boolean INSERT_REVERSEGEOCODE_REQUIRED      = false;

    /* allow Device record specified "notifyUseWrapper" value */
    public static       boolean ALLOW_USE_EMAIL_WRAPPER             = false;

    // ------------------------------------------------------------------------

    /* "Device" title (ie. "Taxi", "Tractor", "Vehicle", etc) */
    //TODO cambiar el titulo x otro!!
    public static String[] GetTitles(Locale loc)
    {
        I18N i18n = I18N.getI18N(Meitrack.class, loc);
        return new String[] {
            i18n.getString("Device.title.singular", "Vehicle"),
            i18n.getString("Device.title.plural"  , "Vehicles"),
        };
    }


    // ------------------------------------------------------------------------
    // border crossing flags (see 'borderCrossing' column)

    public enum BorderCrossingState implements EnumTools.StringLocale, EnumTools.IntValue {
        OFF         ( 0, I18N.getString(Meitrack.class,"Device.boarderCrossing.off","off")),
        ON          ( 1, I18N.getString(Meitrack.class,"Device.boarderCrossing.on" ,"on" ));
        // ---
        private int         vv = 0;
        private I18N.Text   aa = null;
        BorderCrossingState(int v, I18N.Text a)     { vv = v; aa = a; }
        public int     getIntValue()                { return vv; }
        public String  toString()                   { return aa.toString(); }
        public String  toString(Locale loc)         { return aa.toString(loc); }
    };

    // ------------------------------------------------------------------------
    // maximum reasonable odometer value for a vehicle

    // ------------------------------------------------------------------------
    // new asset defaults

    private static final String NEW_DEVICE_NAME_                        = "New Device";

    // ------------------------------------------------------------------------
    // (Vehicle) Rule factory

    private static RuleFactory ruleFactory = null;

    /* set the RuleFactory for event notification */
    public static void setRuleFactory(RuleFactory rf)
    {
        if (rf != null) {
            Meitrack.ruleFactory = rf;
            Print.logDebug("Device RuleFactory installed: " + StringTools.className(Meitrack.ruleFactory));
        } else
        if (Meitrack.ruleFactory != null) {
            Meitrack.ruleFactory = null;
            Print.logDebug("Device RuleFactory removed.");
        }
    }

    /* return ture if a RuleFactory has been defined */
    public static boolean hasRuleFactory()
    {
        return (Meitrack.ruleFactory != null);
    }

    /* get the event notification RuleFactory */
    public static RuleFactory getRuleFactory()
    {
        return Meitrack.ruleFactory;
    }

    // ------------------------------------------------------------------------
    // (Device) Session statistics

    private static SessionStatsFactory statsFactory = null;

    /* set the SessionStatsFactory */
    public static void setSessionStatsFactory(SessionStatsFactory rf)
    {
        if (rf != null) {
            Meitrack.statsFactory = rf;
            Print.logDebug("Device SessionStatsFactory installed: " + StringTools.className(Meitrack.statsFactory));
        } else
        if (Meitrack.statsFactory != null) {
            Meitrack.statsFactory = null;
            Print.logDebug("Device SessionStatsFactory removed.");
        }
    }

    /* return ture if a SessionStatsFactory has been defined */
    public static boolean hasSessionStatsFactory()
    {
        return (Meitrack.statsFactory != null);
    }

    /* get the event notification SessionStatsFactory */
    public static SessionStatsFactory getSessionStatsFactory()
    {
        return Meitrack.statsFactory;
    }

    // ------------------------------------------------------------------------
    // (Vehicle) Entity manager

    private static EntityManager entityManager = null;

    /* set the connect/disconnect EntityManager */
    public static void setEntityManager(EntityManager ef)
    {
        if (ef != null) {
            Meitrack.entityManager = ef;
            //Print.logDebug("Device EntityManager installed: " + StringTools.className(Device.entityManager));
        } else
        if (Meitrack.entityManager != null) {
            Meitrack.entityManager = null;
            //Print.logDebug("Device EntityManager removed.");
        }
    }

    /* return true if an EntityManager has been defined */
    public static boolean hasEntityManager()
    {
        return (Meitrack.entityManager != null);
    }

    /* return the EntityManager (or null if not defined) */
    public static EntityManager getEntityManager()
    {
        return Meitrack.entityManager;
    }

    public static String getEntityDescription(String accountID, String entityID, int entityType)
    {
        String eid = StringTools.trim(entityID);
        if (!eid.equals("") && Meitrack.hasEntityManager()) {
            eid = Meitrack.getEntityManager().getEntityDescription(accountID, eid, entityType);
        }
        return eid;
    }

    // ------------------------------------------------------------------------
    // (Vehicle) Fuel manager

    private static FuelManager fuelManager = null;

    /* set the FuelManager */
    public static void setFuelManager(FuelManager fm)
    {
        if (fm != null) {
            Meitrack.fuelManager = fm;
            //Print.logDebug("Device FuelManager installed: " + StringTools.className(Device.fuelManager));
        } else
        if (Meitrack.fuelManager != null) {
            Meitrack.fuelManager = null;
            //Print.logDebug("Device FuelManager removed.");
        }
    }

    /* return true if an FuelManager has been defined */
    public static boolean hasFuelManager()
    {
        return (Meitrack.fuelManager != null);
    }

    /* return the FuelManager (or null if not defined) */
    public static FuelManager getFuelManager()
    {
        return Meitrack.fuelManager;
    }

    // ------------------------------------------------------------------------
    // (Vehicle) "Ping" dispatcher

    private static PingDispatcher pingDispatcher = null;

    /* set the PingDispatcher */
    public static void setPingDispatcher(PingDispatcher pd)
    {
        if (pd != null) {
            Meitrack.pingDispatcher = pd;
            Print.logDebug("Device PingDispatcher installed: " + StringTools.className(Meitrack.pingDispatcher));
        } else
        if (Meitrack.pingDispatcher != null) {
            Meitrack.pingDispatcher = null;
            Print.logDebug("Device PingDispatcher removed.");
        }
    }

    /* return true if an PingDispatcher has been defined */
    public static boolean hasPingDispatcher()
    {
        return (Meitrack.pingDispatcher != null);
    }

    /* return the PingDispatcher (or null if not defined) */
    public static PingDispatcher getPingDispatcher()
    {
        return Meitrack.pingDispatcher;
    }

    // ------------------------------------------------------------------------
    // Future EventDate timestamp check

    public static final int FUTURE_DATE_UNDEFINED   = -999;
    public static final int FUTURE_DATE_IGNORE      = -1;
    public static final int FUTURE_DATE_DISABLED    = 0;
    public static final int FUTURE_DATE_TRUNCATE    = 1;

    private static int  FutureEventDateAction = FUTURE_DATE_UNDEFINED;
    public static int futureEventDateAction()
    {
        // TODO: synchronize?
        if (FutureEventDateAction == FUTURE_DATE_UNDEFINED) {
            // "Device.futureDate.action="
            String act = RTConfig.getString(DBConfig.PROP_Device_futureDate_action,"");
            if (act.equalsIgnoreCase("ignore")   ||
                act.equalsIgnoreCase("skip")     ||
                act.equalsIgnoreCase("-1")         ) {
                FutureEventDateAction = FUTURE_DATE_IGNORE;
            } else
            if (act.equalsIgnoreCase("truncate") ||
                act.equalsIgnoreCase("1")          ) {
                FutureEventDateAction = FUTURE_DATE_TRUNCATE;
            } else
            if (StringTools.isBlank(act)         ||
                act.equalsIgnoreCase("disabled") ||
                act.equalsIgnoreCase("disable")  ||
                act.equalsIgnoreCase("0")          ) {
                FutureEventDateAction = FUTURE_DATE_DISABLED;
            } else {
                Print.logError("Invalid property value %s => %s", DBConfig.PROP_Device_futureDate_action, act);
                FutureEventDateAction = FUTURE_DATE_DISABLED;
            }
        }
        return FutureEventDateAction;
    }

    private static long FutureEventDateMaxSec = -999L;
    public static long futureEventDateMaximumSec()
    {
        // TODO: synchronize?
        if (FutureEventDateMaxSec == -999L) {
            FutureEventDateMaxSec = RTConfig.getLong(DBConfig.PROP_Device_futureDate_maximumSec,0L);
        }
        return FutureEventDateMaxSec;
    }


    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below
    // Note: The following fields should be updated upon each connection from the client device:
    //  - FLD_lastInputState
    //  - FLD_ipAddressCurrent
    //  - FLD_remotePortCurrent
    //  - FLD_lastTotalConnectTime
    //  - FLD_lastDuplexConnectTime (OpenDMTP clients, otherwise optional)
    //  - FLD_totalProfileMask (OpenDMTP clients)
    //  - FLD_duplexProfileMask (OpenDMTP clients)

    /* table name */
    public static final String _TABLE_NAME               = "Meitrack"; // "Asset"
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    // Meitrack specific information:
    public static final String FLD_timestamp            = "timestamp";              // Unix Epoch time        
    public static final String FLD_input1                = "input1";               // vehicle group (user informational only)
    public static final String FLD_input2                = "input2";               // vehicle group (user informational only)
    public static final String FLD_input3                = "input3";               // vehicle group (user informational only)
    public static final String FLD_input4                = "input4";               // vehicle group (user informational only)
    public static final String FLD_input5                = "input5";               // vehicle group (user informational only)
    public static final String FLD_output1               = "output1";               // vehicle group (user informational only)
    public static final String FLD_output2               = "output2";               // vehicle group (user informational only)
    public static final String FLD_output3               = "output3";               // vehicle group (user informational only)
    public static final String FLD_output4               = "output4";               // vehicle group (user informational only)
    public static final String FLD_output5               = "output5";               // vehicle group (user informational only)
    public static final String FLD_currentOutput1        = "currentoutput1";               // vehicle group (user informational only)
    public static final String FLD_currentOutput2        = "currentoutput2";               // vehicle group (user informational only)
    public static final String FLD_currentOutput3        = "currentoutput3";               // vehicle group (user informational only)
    public static final String FLD_currentOutput4        = "currentoutput4";               // vehicle group (user informational only)
    public static final String FLD_currentOutput5        = "currentoutput5";               // vehicle group (user informational only)
    
    //
    private static DBField FieldInfo[] = {
        // Asset/Vehicle specific fields
        newField_accountID(true),
        newField_deviceID(true),
        
        
        new DBField(FLD_timestamp      		, Long.TYPE     , DBField.TYPE_UINT32      , "Timestamp"                  , "key=true"),
        new DBField(FLD_input1		        , Integer.TYPE  , DBField.TYPE_UINT32      , "Input 1"		                , ""),
        new DBField(FLD_input2		        , Integer.TYPE  , DBField.TYPE_UINT32      , "Input 2"		                , ""),
        new DBField(FLD_input3		        , Integer.TYPE  , DBField.TYPE_UINT32      , "Input 3"		                , ""),
        new DBField(FLD_input4		        , Integer.TYPE  , DBField.TYPE_UINT32      , "Input 4"		                , ""),
        new DBField(FLD_input5		        , Integer.TYPE  , DBField.TYPE_UINT32      , "Input 5"		                , ""),
        new DBField(FLD_output1		        , Integer.TYPE  , DBField.TYPE_UINT32      , "Output 1"		                , ""),
        new DBField(FLD_output2		        , Integer.TYPE  , DBField.TYPE_UINT32      , "Output 2"		                , ""),
        new DBField(FLD_output3		        , Integer.TYPE  , DBField.TYPE_UINT32      , "Output 3"		                , ""),
        new DBField(FLD_output4		        , Integer.TYPE  , DBField.TYPE_UINT32      , "Output 4"		                , ""),
        new DBField(FLD_output5		        , Integer.TYPE  , DBField.TYPE_UINT32      , "Output 5"		                , ""),
        new DBField(FLD_currentOutput1      , Integer.TYPE  , DBField.TYPE_UINT32      , "Current Output 1"		        , ""),
        new DBField(FLD_currentOutput2		, Integer.TYPE  , DBField.TYPE_UINT32      , "Current Output 2"		        , ""),
        new DBField(FLD_currentOutput3		, Integer.TYPE  , DBField.TYPE_UINT32      , "Current Output 3"		        , ""),
        new DBField(FLD_currentOutput4		, Integer.TYPE  , DBField.TYPE_UINT32      , "Current Output 4"		        , ""),
        new DBField(FLD_currentOutput5		, Integer.TYPE  , DBField.TYPE_UINT32      , "Current Output 5"		        , ""),
        
    };
    
    /* key class */
    public static class Key
        extends DeviceKey<Meitrack>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String devId, long timestamp) {
            super.setFieldValue(FLD_accountID, ((acctId != null)? acctId.toLowerCase() : ""));
            super.setFieldValue(FLD_deviceID , ((devId  != null)? devId.toLowerCase()  : ""));
            super.setFieldValue(FLD_deviceID , ((devId  != null)? devId.toLowerCase()  : ""));
            super.setFieldValue(FLD_timestamp , timestamp);            
        }
        public DBFactory<Meitrack> getFactory() {
            return Meitrack.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<Meitrack> factory = null;
    public static DBFactory<Meitrack> getFactory()
    {
        if (factory == null) {
            EnumTools.registerEnumClass(NotifyAction.class);
            factory = DBFactory.createDBFactory(
                Meitrack.TABLE_NAME(),
                Meitrack.FieldInfo,
                DBFactory.KeyType.PRIMARY,
                Meitrack.class,
                Meitrack.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public Meitrack()
    {
        super();
    }

    /* database record */
    public Meitrack(Meitrack.Key key)
    {
        super(key);
    }

    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(Meitrack.class, loc);
        return i18n.getString("Meitrack.description",
            "This table defines " +
            "Input/Output from an specific device. " +
            "Input represent messages sent from device to the system, Output is the other way."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below
    // ------------------------------------------------------------------------
    /**
     *** Gets the timestamp of this event in Unix/Epoch time
     *** @return The timestamp of this event
     **/
     public long getTimestamp()
     {
         return this.getFieldValue(FLD_timestamp, 0L);
     }

     /**
     *** Sets the timestamp of this event in Unix/Epoch time
     *** @param v The timestamp of this event
     **/
     public void setTimestamp(long v)
     {
         this.setFieldValue(FLD_timestamp, v);
     }
   
     public void setInput1(int v)
     {
         this.setFieldValue(FLD_input1, v);
     }

     public int getInput1()
     {
         return this.getFieldValue(FLD_input1, 0);
     }

     // ------------------------------------------------------------------------

     public void setInput2(int v)
     {
         this.setFieldValue(FLD_input2, v);
     }

     public int getInput2()
     {
         return this.getFieldValue(FLD_input2, 0);
     }

     // ------------------------------------------------------------------------
     
     public void setInput3(int v)
     {
         this.setFieldValue(FLD_input3, v);
     }

     public int getInput3()
     {
         return this.getFieldValue(FLD_input3, 0);
     }

     // ------------------------------------------------------------------------
     
     public void setInput4(int v)
     {
         this.setFieldValue(FLD_input4, v);
     }

     public int getInput4()
     {
         return this.getFieldValue(FLD_input4, 0);
     }

     // ------------------------------------------------------------------------
     
     public void setInput5(int v)
     {
         this.setFieldValue(FLD_input5, v);
     }

     public int getInput5()
     {
         return this.getFieldValue(FLD_input5, 0);
     }

     // ------------------------------------------------------------------------
     
     public void setOutput1(int v)
     {
         this.setFieldValue(FLD_output1, v);
     }

     public int getOutput1()
     {
         return this.getFieldValue(FLD_output1, 0);
     }

     // ------------------------------------------------------------------------
     
     public void setOutput2(int v)
     {
         this.setFieldValue(FLD_output2, v);
     }

     public int getOutput2()
     {
         return this.getFieldValue(FLD_output2, 0);
     }

     // ------------------------------------------------------------------------
     
     public void setOutput3(int v)
     {
         this.setFieldValue(FLD_output3, v);
     }

     public int getOutput3()
     {
         return this.getFieldValue(FLD_output3, 0);
     }

     // ------------------------------------------------------------------------
     
     public void setOutput4(int v)
     {
         this.setFieldValue(FLD_output4, v);
     }

     public int getOutput4()
     {
         return this.getFieldValue(FLD_output4, 0);
     }

     // ------------------------------------------------------------------------
     
     public void setOutput5(int v)
     {
         this.setFieldValue(FLD_output5, v);
     }

     public int getOutput5()
     {
         return this.getFieldValue(FLD_output5, 0);
     }
    
// ------------------------------------------------------------------------
     
     public void setCurrentOutput1(int v)
     {
         this.setFieldValue(FLD_currentOutput1, v);
     }

     public int getCurrentOutput1()
     {
         return this.getFieldValue(FLD_currentOutput1, 0);
     }

     // ------------------------------------------------------------------------
     
     public void setCurrentOutput2(int v)
     {
         this.setFieldValue(FLD_currentOutput2, v);
     }

     public int getCurrentOutput2()
     {
         return this.getFieldValue(FLD_currentOutput2, 0);
     }

     // ------------------------------------------------------------------------
     
     public void setCurrentOutput3(int v)
     {
         this.setFieldValue(FLD_currentOutput3, v);
     }

     public int getCurrentOutput3()
     {
         return this.getFieldValue(FLD_currentOutput3, 0);
     }

     // ------------------------------------------------------------------------
     
     public void setCurrentOutput4(int v)
     {
         this.setFieldValue(FLD_currentOutput4, v);
     }

     public int getCurrentOutput4()
     {
         return this.getFieldValue(FLD_currentOutput4, 0);
     }

     // ------------------------------------------------------------------------
     
     public void setCurrentOutput5(int v)
     {
         this.setFieldValue(FLD_currentOutput5, v);
     }

     public int getCurrentOutput5()
     {
         return this.getFieldValue(FLD_currentOutput5, 0);
     }
     // ------------------------------------------------------------------------
     
    // Device/Asset specific data above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // DataTransport specific data below
    
    private String modemID = "";
    
    public String getModemID()
    {
        return this.modemID;
    }

    public void setModemID(String mid)
    {
        // NOT stored in the Device table.  Only used by the caller
        this.modemID = StringTools.trim(mid);
    }

    // --------  

    // ------------------------------------------------------------------------
    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        this.setIsActive(true);
        this.setDescription(NEW_DEVICE_NAME_ + " [" + this.getDeviceID() + "]");
        this.setIgnitionIndex(-1);
        
        // DataTransport attributes below
        this.setSupportedEncodings(Transport.DEFAULT_ENCODING);
        
        // other defaults
        super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* DataTransport interface */
    public String getAssocAccountID()
    {
        return this.getAccountID();
    }

    /* DataTransport interface */
    public String getAssocDeviceID()
    {
        return this.getDeviceID();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return a list of supported commands */
    public DCServerConfig getDCServerConfig()
    {
        return DCServerFactory.getServerConfig(this.getDeviceCode());
    }

    /**
    *** Return a list of supported commands
    *** @param privLabel  The current PrivateLabel instance
    *** @param user       The current user instance
    *** @param type       The command location type (ie. "map", "admin", ...)
    *** @return A map of the specified commands
    **/
    public Map<String,String> getSupportedCommands(BasicPrivateLabel privLabel, User user, 
        String type)
    {
        DCServerConfig dcs = this.getDCServerConfig();
        return (dcs != null)? dcs.getCommandDescriptionMap(privLabel,user,type) : null;
    }
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static boolean  allowSlowReverseGeocode = true;

    /**
    *** Enabled/Disabled slow reverse-geocoding (default is to allow)
    *** @param allow  True to allow, false to dis-allow
    **/
    public static void SetAllowSlowReverseGeocoding(boolean allow)
    {
        Meitrack.allowSlowReverseGeocode = allow;
    }

    /**
    *** Returns true is slow reverse-geocoding is allowed
    *** @return  True if allowed, false otherwise
    **/
    public static boolean GetAllowSlowReverseGeocoding()
    {
        return Meitrack.allowSlowReverseGeocode;
    }

    



    // ------------------------------------------------------------------------

    public interface EventDataHandler
    {
        public void handleEventDataRecord(EventData ev);
    }
    
    public void reprocessEventDataRecords(long timeStart, long timeEnd, final EventDataHandler edh)
        throws DBException
    {
        EventData.getRangeEvents(
            this.getAccountID(), this.getDeviceID(),
            timeStart, timeEnd,
            null/*statusCodes*/,
            false/*validGPS*/,
            EventData.LimitType.LAST, -1L/*limit*/, true/*ascending*/,
            null/*additionalSelect*/,
            new DBRecordHandler<EventData>() {
                public int handleDBRecord(EventData rcd) throws DBException {
                    edh.handleEventDataRecord(rcd);
                    return DBRecordHandler.DBRH_SKIP;
                }
            });
    }

    // ------------------------------------------------------------------------

    /**
    *** Save this Device to db storage
    **/
    public void save()
        throws DBException
    {

        /* save */
        super.save();
        if (this.transport != null) { this.transport.save(); }

    }
    public void save(PrintWriter out)
    throws DBException
{

    /* save */
    super.save();
    if (this.transport != null) { this.transport.save(); }

}
    
    // ------------------------------------------------------------------------

    /**
    *** Return a String representation of this Device
    *** @return The String representation
    **/
    public String toString()
    {
        return this.getAccountID() + "/" + this.getDeviceID();
    }

    // ------------------------------------------------------------------------

    private Transport transport = null;

    /**
    *** Sets the Transport for this Device
    *** @param xport  The Transport instance
    **/
    public void setTransport(Transport xport)
    {
        this.transport = xport;
    }

    /**
    *** Gets the Transport-ID for this Device (if any)
    *** @return The Transport-ID for this Device, or an empty string is not defined
    **/
    public String getTransportID()
    {
        return (this.transport != null)? this.transport.getTransportID() : "";
    }

    /**
    *** Gets the DataTransport for this Device
    *** @return The DataTransport for this Device
    **/
    public DataTransport getDataTransport()
    {
        return (this.transport != null)? (DataTransport)this.transport : (DataTransport)this;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static boolean exists(String acctID, String devID, long timestamp)
        throws DBException // if error occurs while testing existence
    {
        if ((acctID != null) && (devID != null)) {
            Meitrack.Key meiKey = new Meitrack.Key(acctID, devID, timestamp);
            return meiKey.exists();
        }
        return false;
    }

    // ------------------------------------------------------------------------


    /**
    *** This method is called by "Transport.loadDeviceByTransportID(...)" to load a Device
    *** within a Device Communication Server, based on the Account and Device IDs.
    *** @param account  The Account instance represetning the owning account
    *** @param devID    The Device-ID
    *** @return The loaded Device instance, or null if the Device was not found
    *** @throws DBException if a database error occurs
    **/
    public static Meitrack loadDeviceByName(Account account, String devID, int timestamp)
        throws DBException
    {
        Meitrack mei = Meitrack.getMeitrack(account, devID, timestamp);
        return mei;
    }
    
    public static Meitrack loadDeviceByName(Account account, String devID)
    throws DBException
	{
        if ((account != null) && (devID != null)) {
            String acctID = account.getAccountID();
            return Meitrack.getCurrentEvent(acctID, devID); // just say it doesn't exist
        }
        return null;
	}


    // ------------------------------------------------------------------------
    public static Meitrack getMeitrack(Account account, String devID)
    throws DBException
	{
	    if ((account != null) && (devID != null)) {
            String acctID = account.getAccountID();
	        return Meitrack.getCurrentEvent(acctID, devID); // just say it doesn't exist
	    }
	    return null;
	}

    /* get device (may return null) */
    public static Meitrack getMeitrack(Account account, String devID, int timestamp)
        throws DBException
    {
        if ((account != null) && (devID != null)) {
            String acctID = account.getAccountID();
            Meitrack.Key key = new Meitrack.Key(acctID, devID, timestamp);
            if (key.exists()) {
                Meitrack mei = key.getDBRecord(true);
                mei.setAccount(account);
                return mei;
            } else {
                // device does not exist
                return null;
            }
        } else {
            return null; // just say it doesn't exist
        }
    }
    /* get/create device */
    // Note: does NOT return null (throws exception if not found)
    public static Meitrack getDevice(Account account, String devID,int timestamp, boolean create)
        throws DBException
    {

        /* account-id specified? */
        if (account == null) {
            throw new DBNotFoundException("Account not specified.");
        }
        String acctID = account.getAccountID();

        /* device-id specified? */
        if (StringTools.isBlank(devID)) {
            throw new DBNotFoundException("Device-ID not specified for account: " + acctID);
        }

        /* get/create */
        Meitrack mei = null;
        Meitrack.Key meiKey = new Meitrack.Key(acctID, devID, timestamp);
        if (!meiKey.exists()) {
            if (create) {
            	mei = meiKey.getDBRecord();
            	mei.setAccount(account);
            	mei.setCreationDefaultValues();
                return mei; // not yet saved!
            } else {
                throw new DBNotFoundException("Device-ID does not exists: " + meiKey);
            }
        } else
        if (create) {
            // we've been asked to create the device, and it already exists
            throw new DBAlreadyExistsException("Device-ID already exists '" + meiKey + "'");
        } else {
        	mei = Meitrack.getMeitrack(account, devID, timestamp);
            if (mei == null) {
                throw new DBException("Unable to read existing Device-ID: " + meiKey);
            }
            return mei;
        }

    }

    // ------------------------------------------------------------------------

    public static Meitrack createNewDevice(Account account, String devID, int timestamp,String uniqueID)
        throws DBException
    {
        if ((account != null) && !StringTools.isBlank(devID)) {
            Meitrack dev = Meitrack.getDevice(account, devID, timestamp,true); // does not return null
            dev.save();
            return dev;
        } else {
            throw new DBException("Invalid Account/DeviceID specified");
        }
    }
    
    /* create a virtual device record (used for testing purposes) */
    public static Meitrack createVirtualDevice(String acctID, String devID, int timestamp)
    {

        /* get/create */
        Meitrack.Key devKey = new Meitrack.Key(acctID, devID, timestamp);
        Meitrack dev = devKey.getDBRecord();
        dev.setCreationDefaultValues();
        dev.setVirtual(true);
        return dev;

    }
    /* get current meitrak event */
    public static Meitrack getCurrentEvent(String acctID, String devID) throws DBException
    {

        DBFactory<Meitrack> dbFact = Meitrack.getFactory();

        /* has FLD_autoIndex? */
        if (!dbFact.hasField(EventData.FLD_autoIndex)) {
            return null;
        }
        
        /* create key */
        //DBFactory dbFact = EventData.getFactory();
        //DBRecordKey<EventData> evKey = dbFact.createKey();
        //evKey.setFieldValue(EventData.FLD_autoIndex, autoIndex);

        /* create selector */
        int limit = 1;
        DBSelect<Meitrack> dsel = Meitrack._createCurrentEventSelector(
                acctID, devID, limit);

        /* get events */
        Meitrack ed[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            ed = DBRecord.select(dsel, null); // select:DBSelect
        } finally {
            DBProvider.unlockTables();
        }

        /* return result */
        return !ListTools.isEmpty(ed)? ed[0] : null;

    }

    // ------------------------------------------------------------------------

    private static DBSelect<Meitrack> _createCurrentEventSelector(
			String acctID, String devID, int limit) {
        /* invalid account/device */
        if (StringTools.isBlank(acctID)) {
            //Print.logWarn("No AccountID specified ...");
            return null;
        } else
        if (StringTools.isBlank(devID)) {
            //Print.logWarn("No DeviceID specified ...");
            return null;
        }

        /* create/return DBSelect */
        // DBSelect: [SELECT * FROM Meitrack] <Where> <FLD_timestamp> [DESC] LIMIT <Limit>
        DBSelect<Meitrack> dsel = new DBSelect<Meitrack>(Meitrack.getFactory());
        dsel.setWhere(Meitrack.getWhereClause(
            acctID, devID));
        dsel.setOrderByFields(FLD_timestamp);
        dsel.setOrderAscending(true);
        dsel.setLimit(limit);
        return dsel;
	}

	private static String getWhereClause(String acctID, String devID) {
        DBWhere dwh = new DBWhere(Meitrack.getFactory());

        /* Account/Device */
        // ( (accountID='acct') AND (deviceID='dev') )
        if (!StringTools.isBlank(acctID)) {
            dwh.append(dwh.EQ(EventData.FLD_accountID, acctID));
            if (!StringTools.isBlank(devID) && !devID.equals("*")) {
                dwh.append(dwh.AND_(dwh.EQ(EventData.FLD_deviceID , devID)));
            }
        }
        
        /* end of where */
        return dwh.WHERE(dwh.toString());
	}

	/* return list of all Devices owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static OrderedSet<String> getDeviceIDsForAccount(String acctId, User userAuth, boolean inclInactv)
        throws DBException
    {
        return Meitrack.getDeviceIDsForAccount(acctId, userAuth, inclInactv, -1L);
    }

    /* return list of all Devices owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static OrderedSet<String> getDeviceIDsForAccount(String acctId, User userAuth, boolean inclInactv, long limit)
        throws DBException
    {

        /* no account specified? */
        if (StringTools.isBlank(acctId)) {
            if (userAuth != null) {
                acctId = userAuth.getAccountID();
            } else {
                Print.logError("Account not specified!");
                return new OrderedSet<String>();
            }
        }

        /* read devices for account */
        OrderedSet<String> devList = new OrderedSet<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* select */
            // DBSelect: SELECT * FROM Device WHERE (accountID='acct') ORDER BY deviceID
            DBSelect<Meitrack> dsel = new DBSelect<Meitrack>(Meitrack.getFactory());
            dsel.setSelectedFields(Meitrack.FLD_deviceID);
            DBWhere dwh = dsel.createDBWhere();
            if (inclInactv) {
                dsel.setWhere(dwh.WHERE(
                    dwh.EQ(Meitrack.FLD_accountID,acctId)
                ));
            } else {
                dsel.setWhere(dwh.WHERE_(
                    dwh.AND(
                        dwh.EQ(Meitrack.FLD_accountID,acctId),
                        dwh.NE(Meitrack.FLD_isActive,0)
                    )
                ));
            }
            dsel.setOrderByFields(Meitrack.FLD_deviceID);
            dsel.setLimit(limit);

            /* get records */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs = stmt.getResultSet();
            while (rs.next()) {
                String devId = rs.getString(Meitrack.FLD_deviceID);
                if ((userAuth == null) || userAuth.isAuthorizedDevice(devId)) {
                    devList.add(devId);
                }
            }

        } catch (SQLException sqe) {
            throw new DBException("Getting Account Device List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return devList;

    }
    
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below

    private static final String ARG_ACCOUNT[]       = new String[] { "account"   , "acct"  , "a" };
    private static final String ARG_DEVICE[]        = new String[] { "device"    , "dev"   , "d" };
    private static final String ARG_UNIQID[]        = new String[] { "uniqueid"  , "unique", "uniq", "uid", "u" };
    private static final String ARG_TIMESTAMP[]       = new String[] { "timestamp"   , "time"  , "t" };
    private static final String ARG_CREATE[]        = new String[] { "create"               };
    private static final String ARG_EDIT[]          = new String[] { "edit"      , "ed"     };
    private static final String ARG_EDITALL[]       = new String[] { "editall"   , "eda"    }; 
    private static final String ARG_DELETE[]        = new String[] { "delete"               };
    
    private static String _fmtDevID(String acctID, String devID)
    {
        return acctID + "/" + devID;
    }

    private static void usage()
    {
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  java ... " + Meitrack.class.getName() + " {options}");
        Print.sysPrintln("Common Options:");
        Print.sysPrintln("  -account=<id>               Acount ID which owns Device");
        Print.sysPrintln("  -device=<id>                Device ID to create/edit");
        Print.sysPrintln("  -uniqueid=<id>              Unique ID to create/edit");
        Print.sysPrintln("");
        Print.sysPrintln("  -create                     Create a new Device");
        Print.sysPrintln("  -edit                       Edit an existing (or newly created) Device");
        Print.sysPrintln("  -delete                     Delete specified Device");
        Print.sysPrintln("");
        Print.sysPrintln("  -events=<limit>             Retrieve the last <limit> events");
        Print.sysPrintln("  -ckRules=<lat>/<lon>,<sc>   Check rule (may change db!)");
        Print.sysPrintln("");
        Print.sysPrintln("  -countFutureEvents=<sec>    Count events beyond (now + sec) into the future");
        Print.sysPrintln("  -deleteFutureEvents=<sec>   Delete events beyond (now + sec) into the future");
        Print.sysPrintln("");
        Print.sysPrintln("  -countOldEvents=<time>      Count events before specified time");
        Print.sysPrintln("  -deleteOldEvents=<time>     Delete events ibefore specified time");
        Print.sysPrintln("");
        Print.sysPrintln("  -zoneCheck=<GP1>/<GP2>      Geozone transition check");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        String acctID  = RTConfig.getString(ARG_ACCOUNT, "");
        String devID   = RTConfig.getString(ARG_DEVICE , "");
        String uniqID  = RTConfig.getString(ARG_UNIQID , "");
        int timestamp  = RTConfig.getInt(ARG_TIMESTAMP , 0);

        /* account-id specified? */
        if (StringTools.isBlank(acctID)) {
            Print.logError("Account-ID not specified.");
            usage();
        }

        /* get account */
        Account acct = null;
        try {
            acct = Account.getAccount(acctID); // may throw DBException
            if (acct == null) {
                Print.logError("Account-ID does not exist: " + acctID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + acctID, dbe);
            //dbe.printException();
            System.exit(99);
        }

        /* device-id specified? */
        if (StringTools.isBlank(devID)) {
            Print.logError("Device-ID not specified.");
            usage();
        }

        /* device exists? */
        boolean deviceExists = false;
        try {
            deviceExists = Meitrack.exists(acctID, devID, timestamp);
        } catch (DBException dbe) {
            Print.logError("Error determining if Device exists: " + _fmtDevID(acctID,devID));
            System.exit(99);
        }
        
        /* get device if it exists */
        Meitrack deviceRcd = null;
        if (deviceExists) {
            try {
                deviceRcd = Meitrack.getDevice(acct, devID, timestamp,false); // may throw DBException
            } catch (DBException dbe) {
                Print.logError("Error getting Device: " + _fmtDevID(acctID,devID));
                dbe.printException();
                System.exit(99);
            }
        }

        /* option count */
        int opts = 0;

        /* delete */
        if (RTConfig.getBoolean(ARG_DELETE, false) && !StringTools.isBlank(acctID) && !StringTools.isBlank(devID)) {
            opts++;
            if (!deviceExists) {
                Print.logWarn("Device does not exist: " + _fmtDevID(acctID,devID));
                Print.logWarn("Continuing with delete process ...");
            }
            try {
                Meitrack.Key devKey = new Meitrack.Key(acctID, devID, timestamp);
                devKey.delete(true); // also delete dependencies
                Print.logInfo("Device deleted: " + _fmtDevID(acctID,devID));
                deviceExists = false;
            } catch (DBException dbe) {
                Print.logError("Error deleting Device: " + _fmtDevID(acctID,devID));
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* create */
        if (RTConfig.getBoolean(ARG_CREATE, false)) {
            opts++;
            if (deviceExists) {
                Print.logWarn("Device already exists: " + _fmtDevID(acctID,devID));
            } else {
                try {
                    Meitrack.createNewDevice(acct, devID, timestamp, uniqID);
                    Print.logInfo("Created Device: " + _fmtDevID(acctID,devID));
                    deviceExists = true;
                } catch (DBException dbe) {
                    Print.logError("Error creating Device: " + _fmtDevID(acctID,devID));
                    dbe.printException();
                    System.exit(99);
                }
            }
        }

        /* edit */
        if (RTConfig.getBoolean(ARG_EDIT,false) || RTConfig.getBoolean(ARG_EDITALL,false)) {
            opts++;
            if (!deviceExists) {
                Print.logError("Device does not exist: " + _fmtDevID(acctID,devID));
            } else {
                try {
                    boolean allFlds = RTConfig.getBoolean(ARG_EDITALL,false);
                    DBEdit editor = new DBEdit(deviceRcd);
                    editor.edit(allFlds); // may throw IOException
                } catch (IOException ioe) {
                    if (ioe instanceof EOFException) {
                        Print.logError("End of input");
                    } else {
                        Print.logError("IO Error");
                    }
                }
            }
            System.exit(0);
        }



        /* no options specified */
        if (opts == 0) {
            Print.logWarn("Missing options ...");
            usage();
        }

    }



	@Override
	public String getDeviceCode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDeviceCode(String v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getDeviceType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDeviceType(String v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getSerialNumber() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSerialNumber(String v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getSimPhoneNumber() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSimPhoneNumber(String v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getSmsEmail() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSmsEmail(String v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getImeiNumber() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setImeiNumber(String v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public DTIPAddrList getIpAddressValid() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setIpAddressValid(DTIPAddrList v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isValidIPAddress(String ipAddr) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DTIPAddress getIpAddressCurrent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setIpAddressCurrent(String v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getRemotePortCurrent() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setRemotePortCurrent(int v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getListenPortCurrent() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setListenPortCurrent(int v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getLastInputState() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setLastInputState(long v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getIgnitionIndex() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setIgnitionIndex(int v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int[] getIgnitionStatusCodes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCodeVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCodeVersion(String v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getFeatureSet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFeatureSet(String v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getSupportsDMTP() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setSupportsDMTP(boolean v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getSupportedEncodings() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setSupportedEncodings(int encodingMask) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getUnitLimitInterval() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxAllowedEvents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public DTProfileMask getTotalProfileMask() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setTotalProfileMask(DTProfileMask v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getTotalMaxConn() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getTotalMaxConnPerMin() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public DTProfileMask getDuplexProfileMask() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDuplexProfileMask(DTProfileMask v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getDuplexMaxConn() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getDuplexMaxConnPerMin() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getLastPingTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getTotalPingCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxPingCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getLastTotalConnectTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setLastTotalConnectTime(long v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getLastDuplexConnectTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setLastDuplexConnectTime(long v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getUniqueID() {
		// TODO Auto-generated method stub
		return null;
	}

}
