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
//  2009/01/28  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.util.*;

import org.opengts.util.*;

/**
*** <code>DBRecordListener</code> callback listener for DBRecord update/insert
**/

public interface DBRecordListener<gDBR extends DBRecord>
{

    /**
    *** Callback when record is about to be inserted into the table
    *** @param rcd  The record about to be inserted
    **/
    public void recordWillInsert(gDBR rcd);

    /**
    *** Callback after record has been be inserted into the table
    *** @param rcd  The record that was just inserted
    **/
    public void recordDidInsert(gDBR rcd);

    /**
    *** Callback when record is about to be updated in the table
    *** @param rcd  The record about to be updated
    **/
    public void recordWillUpdate(gDBR rcd);
    
    /**
    *** Callback after record has been be updated in the table
    *** @param rcd  The record that was just updated
    **/
    public void recordDidUpdate(gDBR rcd);

}
