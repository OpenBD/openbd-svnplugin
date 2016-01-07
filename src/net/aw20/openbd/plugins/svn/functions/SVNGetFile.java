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

package net.aw20.openbd.plugins.svn.functions;


import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.aw20.openbd.plugins.svn.SVNRepo;

import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.io.SVNRepository;

import com.naryx.tagfusion.cfm.engine.cfArgStructData;
import com.naryx.tagfusion.cfm.engine.cfBooleanData;
import com.naryx.tagfusion.cfm.engine.cfData;
import com.naryx.tagfusion.cfm.engine.cfEngine;
import com.naryx.tagfusion.cfm.engine.cfSession;
import com.naryx.tagfusion.cfm.engine.cfStructData;
import com.naryx.tagfusion.cfm.engine.cfmRunTimeException;
import com.naryx.tagfusion.expression.function.functionBase;


/**
 * OpenBD class for function: SVNGetDir()
 * 
 * called from OpenBD <code>props = SVNGetDir(_session,_repo,"temp/","C:\temp\",-1,true)</code>
 * 
 * @author AW20 Ltd
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 1.0.0
 * @see http://svnkit.com
 */
public class SVNGetFile extends functionBase {

	private static final long serialVersionUID = 1L;


	public SVNGetFile() {
		min = 3;
		max = 5;
		setNamedParams( new String[] {
				"name",
				"svnPath",
				"localPath",
				"revision",
				"properties"
		} );
	}


	@Override
	public String[] getParamInfo() {
		return new String[] {
				"Name of the SVN Repository to use",
				"SVN Path to get",
				"Full local path where to get to",
				"The revision to get, default HEAD",
				"Flag to indicate if the properties are to be returned, default to false"
		};
	}


	@Override
	public Map<String, String> getInfo() {
		return makeInfo(
				"svn",
				"Returns the file at the given path and revision in the repository",
				ReturnType.NUMERIC );
	}


	/**
	 * Used to get a single file from SVN
	 * 
	 * called from OpenBD <code>props = SVNGetDir(_session,_repo,"temp/","C:\temp\",-1,true)</code>
	 * 
	 * @param _session
	 * @param _argStruct
	 *          name: String repository name to use
	 *          svnPath: String path in SVN to get
	 *          localPath: String path on the local file system to get files to
	 *          revision: Int revision to get
	 *          properties: boolean flat to indicate if we expect a struct of properties are returned (true: yes / false: no), default to false
	 * @return if (properties) a struct modeled after the file structure retrieve, file elements will have property values, directories will have structs
	 * @return if (!properties) true
	 * @throws cfmRunTimeException
	 *           SVN Repository name not supplied
	 *           Unable to connect to repository
	 *           localPath not supplied
	 * @since 1.0.0
	 */
	@Override
	public cfData execute( cfSession _session, cfArgStructData _argStruct ) throws cfmRunTimeException {
		// Get all arguments
		String name = getNamedStringParam( _argStruct, "name", "" ).trim();
		String svnPath = getNamedStringParam( _argStruct, "svnPath", "" ).trim();
		String localPath = getNamedStringParam( _argStruct, "localPath", "" ).trim();
		int revision = getNamedIntParam( _argStruct, "revision", SVNRepo.HEAD );
		boolean properties = getNamedParam( _argStruct, "properties", cfBooleanData.FALSE ).getBoolean();


		// Validate arguments
		if ( name.isEmpty() ) {
			throwException( _session, "Please provide a SVN Repository" );
		}
		try ( SVNRepo repo = new SVNRepo( name ) ) {

			// svnPath can be empty, it will grab all the contents of the repository
			if ( localPath.isEmpty() ) {
				throwException( _session, "Please provide a valid local direcot" );
			}

			// Call to do all the work
			return this.execute( _session, repo, svnPath, localPath, revision, properties );
		} catch ( SVNException e ) {
			throwException( _session, "Unable to connect to " + name + ". Please provide a Valid SVN Repository" );
		}

		return cfBooleanData.FALSE;
	}


	/**
	 * Overload of execute( cfSession, cfArgStructData ) for calling from Java
	 * 
	 * <code>md5s = execute(_session,repo,"temp/","C:\temp\",-1,true)</code>
	 * 
	 * @param _session
	 *          OpenBD Session object for error throwing
	 * @param _repo
	 *          SVNRepo used to get the file
	 * @param _svnPath
	 *          String path in SVN to get
	 * @param _localPath
	 *          String path on the local file system to get files to
	 * @param _revision
	 *          Int revision to get
	 * @param _properties
	 *          boolean flat to indicate if we expect a struct of properties are returned (true: yes / false: no), default to false
	 * @return if (properties) a struct modeled after the file structure retrieve, file elements will have properties values, directories will have structs
	 * @return if (!properties) true
	 * @since 2.0.0
	 */
	public cfData execute( cfSession _session, SVNRepo _repo, String _svnPath, String _localPath, int _revision, boolean _properties ) throws cfmRunTimeException {

		SVNRepository svn = _repo.getRepository();
		File localFile = new File( _localPath );

		if ( !localFile.getParentFile().exists() ) {
			localFile.getParentFile().mkdirs();
		}

		return this.execute( _session, svn, _svnPath, localFile, _revision, _properties );

	}


	/**
	 * overload of execute( cfSession, cfArgStructData ) for use in calling from Java
	 * overload of execute( cfSession, SVNRepo, String, String, int, boolean )
	 * 
	 * <code>md5s = execute(_session,svn,"temp/","C:\temp\",-1,true)</code>
	 * 
	 * @param _session
	 *          OpenBD Session object for error throwing
	 * @param _svn
	 *          SVNRepository used to get the file
	 * @param _svnPath
	 *          String path in SVN to get
	 * @param _localFile
	 *          String path on the local file system to get files to
	 * @param _revision
	 *          Int revision to get
	 * @param _properties
	 *          boolean flat to indicate if we expect a struct of properties are returned (true: yes / false: no), default to false
	 * @return if (properties) a struct modeled after the file structure retrieve, file elements will have SVN property values, directories will have structs
	 * @return if (!properties) true
	 * @since 1.0.0
	 */
	public cfData execute( cfSession _session, SVNRepository _svn, String _svnPath, File _localFile, int _revision, boolean _properties ) throws cfmRunTimeException {

		if ( _localFile.exists() ) {
			_localFile.delete();
		}
		SVNProperties prop = new SVNProperties();

		try ( OutputStream out = cfEngine.thisPlatform.getFileIO().getFileOutputStream( _localFile ) ) {
			_svn.getFile( _svnPath, _revision, prop, out );
			out.flush();
		} catch ( SVNAuthenticationException e ) {
			throwException( _session, "Failed to authenticate user with svn. Check user permissions. " + e.getMessage() );
		} catch ( SVNException e ) {
			if ( _revision >= 0 ) {
				throwException( _session, "Failed to retrieve file " + _svnPath + " from SVN at Revision #" + _revision +
						". Check connection to remote repository, and verify remote file exists at this revision. " + e.getMessage() );
			} else {
				throwException( _session, "Failed to retrieve file " + _svnPath + " at HEAD from SVN. " +
						"Check connection to remote repository, and verify remote file exists at HEAD. " + e.getMessage() );
			}
		} catch ( IOException e ) {
			throwException( _session, "Failed to write files to " + _localFile.getPath() +
					". Check directory exists and it has the appropriate permissions." );
		}

		if ( _properties ) {

			cfStructData ret = new cfStructData();

			for ( Entry entry : prop.asMap().entrySet() ) {
				ret.setData( entry.getKey().toString(), entry.getValue().toString() );
			}

			return ret;
		} else {
			return cfBooleanData.TRUE;
		}

	}


}
