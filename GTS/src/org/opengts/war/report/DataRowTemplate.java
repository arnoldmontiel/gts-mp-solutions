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
//  2007/03/11  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.report;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.db.*;
import org.opengts.dbtools.*;
import org.opengts.util.*;

import org.opengts.war.report.*;

public abstract class DataRowTemplate
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private HashMap<String,DataColumnTemplate> lookupTable = null;
    
    public DataRowTemplate() 
    {
        this.lookupTable = new HashMap<String,DataColumnTemplate>();
    }

    // ------------------------------------------------------------------------

    /* normalize key (remove arg, make lower-case) */
    protected String _normalizeKey(String key)
    {
        // 'key' is not null
        int ka = key.indexOf(':'); // obsolete: no longer exists when using ReportColumn
        if (ka >= 0) { key = key.substring(0,ka); } // remove arg
        return key.trim().toLowerCase();
    }

    /* add column to table */
    protected void _addColumnTemplate(String key, DataColumnTemplate dc)
    {
        if (!StringTools.isBlank(key) && (dc != null)) {
            String k = this._normalizeKey(key);
            if (this.lookupTable.containsKey(k)) {
                Print.logStackTrace("Report column key already defined: " + k);
            }
            this.lookupTable.put(k, dc);
        }
    }

    /* add column to table */
    public void addColumnTemplate(DataColumnTemplate dc, String... aliasKeys)
    {
        if (dc != null) {
            this._addColumnTemplate(dc.getKeyName(), dc);
            if (!ListTools.isEmpty(aliasKeys)) {
                for (String key : aliasKeys) {
                    this._addColumnTemplate(key, dc);
                }
            }
        }
    }

    /* return requested column */
    public DataColumnTemplate getColumnTemplate(String key)
    {
        if (key != null) {
            return this.lookupTable.get(this._normalizeKey(key));
        } else {
            return null;
        }
    }

    /* return true if column exists */
    public boolean hasColumn(String key)
    {
        if (key != null) {
            return this.lookupTable.containsKey(this._normalizeKey(key));
        } else {
            return false;
        }
    }
    
    /* return a list of available columns names */
    public Set<String> getColumnNames()
    {
        return new HashSet<String>(this.lookupTable.keySet());
    }

    /* add column alias (specified column must exist) */
    /*
    private boolean addColumnAlias(String newAliasKey, String existingKey)
    {
        DataColumnTemplate dc = this.getColumnTemplate(existingKey);
        if (dc == null) {
            Print.logWarn("Column key does not exist: " + existingKey);
        } else
        if (StringTools.isBlank(newAliasKey)) {
            Print.logWarn("Column alias not specified");
        } else
        if (this.hasColumn(newAliasKey)) {
            Print.logWarn("Column alias already exists: " + newAliasKey);
        } else {
            this._addColumnTemplate(newAliasKey, dc);
            return true;
        }
        return false;
    }
    */

    // ------------------------------------------------------------------------

    public DBDataRow.RowType getRowType(Object obj)
    {
        return DBDataRow.RowType.DETAIL;
    }
    
    // ------------------------------------------------------------------------

    /* return the value for the specified column */
    public Object getFieldValue(String key, int rowNdx, ReportData rd, ReportColumn rc, Object obj)
    {
        DataColumnTemplate cdv = this.getColumnTemplate(key);
        if (cdv != null) {
            return cdv.getColumnValue(rowNdx, rd, rc, obj);
        } else
        if (obj instanceof DBRecord) {
            try {
                DBRecord  dbr = (DBRecord)obj;
                DBField dbFld = dbr.getFactory(true).getField(key);
                if (dbFld != null) {
                    Object val = dbr.getFieldValue(key);
                    return StringTools.trim(dbFld.formatValue(val));
                } else {
                    return "";
                }
            } catch (DBException dbe) {
                return "";
            }
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------

}
