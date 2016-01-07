/* 
 *  Copyright (C) 2000 - 2015 aw2.0Ltd
 *
 *  This file is part of Open BlueDragon (OpenBD) CFML Server Engine.
 *  
 *  OpenBD is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  Free Software Foundation,version 3.
 *  
 *  OpenBD is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with OpenBD.  If not, see http://www.gnu.org/licenses/
 *  
 *  Additional permission under GNU GPL version 3 section 7
 *  
 *  If you modify this Program, or any covered work, by linking or combining 
 *  it with any of the JARS listed in the README.txt (or a modified version of 
 *  (that library), containing parts covered by the terms of that JAR, the 
 *  licensors of this Program grant you additional permission to convey the 
 *  resulting work. 
 *  README.txt @ http://www.openbluedragon.org/license/README.txt
 *  
 *  http://openbd.org/
 */
package net.aw20.openbd.plugins.svn;

/**
 * Storage class for SVN repository credentials
 * 
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 2.0.0
 */
public class SVNKey {

	private final String svnURL;
	private final String svnUser;
	private final String svnPass;
	private final String svnPath;
	private final String svnKey;
	private final Boolean isTmpKey;


	/**
	 * Used to store information for connecting to a SVN repository
	 * 
	 * <code>key = new SVNKey( String, String, String, String, String, boolean )</code>
	 * 
	 * @param _svnURL
	 *          the URL (svn://, file://, http://, etc...) of the repository
	 * @param _svnUser
	 *          the User that will be connecting
	 * @param _svnPass
	 *          the Password that will be authenticating the user
	 * @param _svnPath
	 *          the path to the Key file that will be used to authenticate the user
	 * @param _svnKey
	 *          The content of the Key file
	 * @param _tmpKey
	 *          flag for if this is a temporary file that needs to be cleaned up
	 * @see net.aw20.openbd.plugins.svn.SVNKeyFactory
	 * @since 2.0.0
	 */
	public SVNKey( String _svnURL, String _svnUser, String _svnPass, String _svnPath, String _svnKey, Boolean _tmpKey ) {
		svnURL = _svnURL;
		svnUser = _svnUser;
		svnPass = _svnPass;
		svnPath = _svnPath;
		svnKey = _svnKey;
		isTmpKey = _tmpKey;
	}


	/**
	 * get the URL of the repository
	 * 
	 * <code>url = key.getURL()</code>
	 * 
	 * @return String value of the URL
	 * @since 2.0.0
	 */
	public String getURL() {
		return svnURL;
	}


	/**
	 * get the user of the repository
	 * 
	 * <code>userName = key.getUser()</code>
	 * 
	 * @return String value of the username
	 * @since 2.0.0
	 */
	public String getUser() {
		return svnUser;
	}


	/**
	 * get the password of the repository
	 * 
	 * <code>passWord = key.getPass()</code>
	 * 
	 * @return String value of the password
	 * @since 2.0.0
	 */
	public String getPass() {
		return svnPass;
	}


	/**
	 * get the path of the Key file for the repository
	 * 
	 * <code>keyPath = key.getPath()</code>
	 * 
	 * @return String value of the key file path
	 * @since 2.0.0
	 */
	public String getPath() {
		return svnPath;
	}


	/**
	 * get the key for the repository
	 * 
	 * <code>svnKey = key.getKey()</code>
	 * 
	 * @return String value of the key
	 * @since 2.0.0
	 */
	public String getKey() {
		return svnKey;
	}


	/**
	 * get the user of the repository
	 * 
	 * <code>isTempFile = key.isTemp()</code>
	 * 
	 * @return boolean flag for a tempory file
	 *         true: it is a temporary file
	 *         false: this not a temporary file
	 * @since 2.0.0
	 */
	public boolean isTemp() {
		return isTmpKey;
	}


}
