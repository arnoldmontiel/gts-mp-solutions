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
// Change History:
//  2008/02/11  Martin D. Flynn
//     -Initial release
//  2008/02/17  Martin D. Flynn
//     -Added 'getMapIconIndex'
//  2008/09/12  Martin D. Flynn
//     -Added 'getSatelliteCount', 'getBatteryLevel'
//  2008/10/16  Martin D. Flynn
//     -Added 'setIsLastEvent', 'getIsLastEvent'
//  2009/07/01  Martin D. Flynn
//     -Renamed "getMapIconIndex(...)" to "getPushpinIconIndex(...)"
//  2011/05/13  Martin D. Flynn
//     -Added "setEventIndex", "getEventIndex", "getIsFirstEvent"
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;

public interface EventDataProvider
{

    public String getAccountID();
    public String getDeviceID();
    public String getDeviceDescription();
    public String getDeviceVIN();

    public long   getTimestamp();
    public int    getStatusCode();
    public String getStatusCodeDescription(BasicPrivateLabel bpl);
    
    public int    getPushpinIconIndex(String iconSelector, OrderedSet<String> iconKeys,
        boolean isFleet, BasicPrivateLabel bpl);

    public double getLatitude();
    public double getLongitude();
    public GeoPoint getGeoPoint();

    public int    getSatelliteCount();

    public double getBatteryLevel();

    public double getSpeedKPH();
    public double getHeading();

    public double getAltitude(); // meters

    public String getGeozoneID();
    public String getAddress();

    public long   getInputMask();

    public double getOdometerKM();

    /* icon selector properties */
    
    public void setEventIndex(int ndx);
    public int getEventIndex();
    public boolean getIsFirstEvent();

    public void setIsLastEvent(boolean isLast);
    public boolean getIsLastEvent();

}
