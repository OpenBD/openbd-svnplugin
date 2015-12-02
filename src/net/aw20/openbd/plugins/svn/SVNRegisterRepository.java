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

import java.io.File;
import java.util.Map;
import java.util.UUID;

import com.naryx.tagfusion.cfm.engine.cfArgStructData;
import com.naryx.tagfusion.cfm.engine.cfData;
import com.naryx.tagfusion.cfm.engine.cfEngine;
import com.naryx.tagfusion.cfm.engine.cfSession;
import com.naryx.tagfusion.cfm.engine.cfStringData;
import com.naryx.tagfusion.cfm.engine.cfmRunTimeException;
import com.naryx.tagfusion.expression.function.functionBase;


/**
 * OpenBD class for function: SVNRegisterRepository()
 * 
 * Called from <i>OpenBD</i>
 * <p>
 * can be called using username/password authentication <code>repoName = SVNRegisterRepository("myRepository","svn+ssh://my.svnserver.com","myUser","myPass");</code>
 * </p>
 * 
 * <p>
 * can be called using username/authentication key where the authentication file exists <code>repoName = SVNRegisterRepository("myRepository","svn+ssh://my.svnserver.com","myUser","","C:\svnKey.txt");</code>
 * </p>
 * 
 * <p>
 * can be called using username/authentication key where the authentication file does not exist but we want it a specific location, and we are passing the key as a string <code>repoName = SVNRegisterRepository("myRepository","svn+ssh://my.svnserver.com","myUser","","C:\svnKey.txt","...");</code>
 * </p>
 * 
 * <p>
 * can be called using username/authentication key where the authentication file does not exist and we don't care where it is written to, and we are passing the key as a string <code>repoName = SVNRegisterRepository("myRepository","svn+ssh://my.svnserver.com","myUser","","","...");</code>
 * </p>
 * 
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 2.0.0
 */
public class SVNRegisterRepository extends functionBase {

	private static final long serialVersionUID = 1L;


	public SVNRegisterRepository() {
		min = 4;
		max = 6;
		setNamedParams( new String[] {
				"name",
				"url",
				"user",
				"pass",
				"path",
				"key" } );
	}


	@Override
	public String[] getParamInfo() {
		return new String[] {
				"Name of the repository",
				"URL of the repository",
				"User to access the repository",
				"Password to access the repository",
				"Path of the key file",
				"Content of the key file"
		};
	}


	@Override
	public Map<String, String> getInfo() {
		return makeInfo(
				"SVN",
				"SVN Repository: Register a repository for use with SVN functions",
				ReturnType.STRING );
	}


	/**
	 * Used to register a SVN repository
	 * 
	 * @param _session
	 * @param _argStruct
	 *          name: the name of the repository to create (stored in lowercase)
	 *          url: the URL of the SVN repository
	 *          user: the username used for connecting
	 *          pass: the password used if password authentication is used
	 *          path: the path to they authentication key file
	 *          key: the authentication key if passed as a string
	 * @return the name of SVN repository created
	 * @throws cfmRunTimeException
	 *           if a name is not supplied for the repository
	 *           if a url is not supplied for the repository
	 *           if a username is not supplied for the repository
	 *           if a path to the repository key, or repository key is not provided
	 *           if the repository key file does not exist, and the repository key was not supplied
	 *           if we are unable to write the repository key to the specified/generated path
	 * @since 2.0.0
	 */
	@Override
	public cfData execute( cfSession _session, cfArgStructData _argStruct ) throws cfmRunTimeException {

		String repoName = getNamedStringParam( _argStruct, "name", null );
		String repoURL = getNamedStringParam( _argStruct, "url", null );
		String repoUser = getNamedStringParam( _argStruct, "user", null );
		String repoPass = getNamedStringParam( _argStruct, "pass", "" );
		String repoPath = getNamedStringParam( _argStruct, "path", "" );
		String repoKey = getNamedStringParam( _argStruct, "key", "" );
		boolean tmpKey = !repoKey.isEmpty();

		if ( repoName == null || repoName.trim().isEmpty() ) {
			throwException( _session, "Please provide a value for the Repository Name" );
		}

		if ( repoURL == null || repoURL.trim().isEmpty() ) {
			throwException( _session, "Please provide a value for the Repository URL" );
		}

		if ( repoUser == null || repoUser.trim().isEmpty() ) {
			throwException( _session, "Please provide a value for the Repository User" );
		}

		if ( repoPath.isEmpty() && repoKey.isEmpty() ) {
			throwException( _session, "Please provide a value for the Key Path or the Key Content" );
		}

		if ( repoPath.isEmpty() && !repoKey.isEmpty() ) {
			repoPath = cfEngine.thisPlatform.getFileIO().getTempDirectory() + File.separator + UUID.randomUUID().toString().replace( "-", "" ) + ".key";
		}

		SVNKeyFactory.registerSVN( repoName, new SVNKey( repoURL, repoUser, repoPass, repoPath, repoKey, tmpKey ) );

		return new cfStringData( repoName );
	}


}
