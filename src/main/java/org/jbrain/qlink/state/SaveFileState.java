/*
Copyright Jim Brain and Brain Innovations, 2005.

This file is part of QLinkServer.

QLinkServer is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

QLinkServer is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of_iToID=info.getAccountID(); the GNU General Public License
along with QLinkServer; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

@author Jim Brain
Created on Jul 23, 2005
SKERN
*/
package org.jbrain.qlink.state;

import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.jbrain.qlink.*;
import org.jbrain.qlink.cmd.action.*;
import org.jbrain.qlink.db.DBUtils;
import org.jbrain.qlink.user.AccountInfo;
import org.jbrain.qlink.user.QHandle;
import org.jbrain.qlink.user.UserManager;

public class SaveFileState extends AbstractState {
  private static Logger _log = Logger.getLogger(SaveFileState.class);

  /**
   * @uml.property name="_intState"
   * @uml.associationEnd multiplicity="(0 1)"
   */
  private QState _intState;
  public String Filename;
  public String Ftype;
  public int FileText;
  private int _iToID;
  private StringBuffer _sbData = new StringBuffer();
  private StringBuffer _sbDatas = new StringBuffer();
  //public int _IDf;
  public SaveFileState(QSession session,String _filename) {
    super(session);
    String _Type ="";
     char Type ;
     String _Name = "" ;
      int i = _filename.length() ;
      _log.debug("Lenghth " + i);
      Type = _filename.charAt(i-1) ;
     _log.debug("Type " + Type);
     if (Type == 's') {
      _Type = "seq";
      }
     else if (Type == 'p') {
      _Type = "prg";
      }
     else if (Type == 'u') {
      _Type = "usr"; 
      }   
      for (int j = 0; (_filename.charAt(j) != 0x90); j++) {
      
      _Name = _Name + _filename.charAt((j) & 0x0f);
      }
     _log.debug( "Filename " + _Name + " Type " + _Type);
  Filename = _Name;
  Ftype = _Type;
  FileText = 0;
}

  public void activate() throws IOException {
	
    _log.debug("activate upload" + Filename + ","+Ftype+",count"+FileText);
    _intState=_session.getState();
    super.activate();
   
    _session.send(new SendSYSOLM("press F5 than RETURN"));
    _session.send(new FO());//press Return
  }

 public boolean execute(Action a) throws IOException {
	
    boolean rc = false;
    _log.debug("Action a");
	   QState state;
	 if (a instanceof FileCanceled) {
      // Upload is cancelled
      _log.debug("Cancelled Upload File "+ Filename );
         
	 } else if (a instanceof FileNextBlock) {
      // save first/next Block of File
      
      String data = ((FileNextBlock) a).getData();

      _log.debug("Block: "+ asciiToHex(data));
      _sbData.append(data);
      return true;
     } else if (a instanceof FileLastBlock) {
	  rc = true;	 
      // save last Block of File ;
   
      String data = ((FileLastBlock) a).getData();
     
      _log.debug("last File Block: "+ asciiToHex(data));
      _sbData.append(data);
     String datas = _sbData.toString();
       datas = datas.replace("]\u0055","\u0000");
	   datas = datas.replace("]\u0058","\r");
	   datas = datas.replace("]\u005b","\u000e");
	   datas = datas.replace("]\u00aa","\u00ff");
	   datas = datas.replace("]\u00d8","\u008d");
	   datas = datas.replace("]\u00db","\u008e");
	   datas = datas.replace("]\u0008","\u005d");
	  _sbDatas.append(datas); 
      // now, save File...
      saveFile(PostMessage._IDf, _sbDatas);
      // get DB connection and save file
      _log.debug("File saved");
    _session.setState(_intState);
    return true;
    
    }else {
			return _intState.execute(a);
		}
    
     return false; 
  }

  public void terminate() {
    _intState.terminate();
  }
 private static String asciiToHex(String asciiValue){
    char[] chars = asciiValue.toCharArray();
    StringBuffer hex = new StringBuffer();
    for (int i = 0; i < chars.length; i++){
        hex.append(Integer.toHexString((int) chars[i])+" ");
    }
    return hex.toString();
  }





  private void saveFile(int id, StringBuffer Data) {
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    String sql;
   _log.debug("File content for SQL in Hex "+ asciiToHex(Data.toString()));
    try {
      conn = DBUtils.getConnection();
      _log.debug("Saving File to sql");
      
      sql =
          "INSERT INTO files (reference_id, name, filetype, downloads,"
              + "data ) "
              + "VALUES (?, ?, ?, ?, ?)";
     
		

  InputStream is = new  StringBufferInputStream(Data.toString());
 

    

      
      pstmt = conn.prepareStatement(sql);
      pstmt.setInt(1, id);
      pstmt.setString(2, Filename);
      pstmt.setString(3, Ftype);
      pstmt.setInt(4, FileText);
      pstmt.setBlob(5, is);
      _log.debug(pstmt.toString()); // show generated SQL
      pstmt.execute();
      if (pstmt.getUpdateCount() > 0) {
        // we added it.
        _log.debug("File successfully saved");
      } else {
        _log.debug("File not saved");
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
      // big time error, send back error string and close connection
    } finally {
      DBUtils.close(rs);
      DBUtils.close(pstmt);
      DBUtils.close(conn);
    }
  }
}
