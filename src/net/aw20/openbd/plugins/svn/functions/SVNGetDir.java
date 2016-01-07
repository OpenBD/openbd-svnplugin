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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import net.aw20.openbd.plugins.svn.SVNRepo;
import net.aw20.openbd.plugins.svn.functions.SVNGetFile;

import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.io.SVNRepository;

import com.naryx.tagfusion.cfm.engine.cfArgStructData;
import com.naryx.tagfusion.cfm.engine.cfBooleanData;
import com.naryx.tagfusion.cfm.engine.cfData;
import com.naryx.tagfusion.cfm.engine.cfSession;
import com.naryx.tagfusion.cfm.engine.cfStructData;
import com.naryx.tagfusion.cfm.engine.cfmRunTimeException;
import com.naryx.tagfusion.expression.function.functionBase;


/**
 * OpenBD class for function: SVNGetDir()
 * 
 * Called from OpenBD <code>properties = SVNGetDir("mySVN","temp/","C:\temp\",-1,true,true)</code>
 * 
 * @author AW20 Ltd
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 1.0.0
 * @see http://svnkit.com
 */
public class SVNGetDir extends functionBase {

	private static final long serialVersionUID = 1L;


	public SVNGetDir() {
		min = 3;
		max = 6;
		setNamedParams( new String[] {
				"name",
				"svnPath",
				"localPath",
				"revision",
				"recursive",
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
				"Flag to indicate if subdirectories are to be retrieved as well, default to false",
				"Flag to indicate if the properties is to be returned, default to false"
		};
	}


	@Override
	public Map<String, String> getInfo() {
		return makeInfo(
				"svn",
				"Fetches the files under given path and revision in the repository",
				ReturnType.NUMERIC );
	}


	/**
	 * Used to get a directory and its contents from SVN to the local file system
	 * 
	 * Called from OpenBD <code>properties = SVNGetDir("mySVN","temp/","C:\temp\",-1,true,true)</code>
	 * 
	 * @param _session
	 * @param _argStruct
	 *          name: String repository name to use
	 *          svnPath: String path in SVN to get
	 *          localPath: String path on the local file system to get files to
	 *          revision: Int revision to get
	 *          recursive: boolean flag to indicate if we get all subdirectories (true: yes / false: no), default to false
	 *          properties: boolean flat to indicate if we expect a struct of properties are returned (true: yes / false: no), default to false
	 * @return if (properties) a struct modeled after the file structure retrieve, file elements will have SVN property values, directories will have structs
	 * @return if (!properties) true
	 * @throws cfmRunTimeException
	 *           SVN Repository name not supplied
	 *           Unable to connect to repository
	 *           localPath not supplied
	 * 
	 * @since 1.0.0
	 */
	@Override
	public cfData execute( cfSession _session, cfArgStructData _argStruct ) throws cfmRunTimeException {

		// Get all arguments
		String name = getNamedStringParam( _argStruct, "name", "" ).trim();
		String svnPath = getNamedStringParam( _argStruct, "svnPath", "" ).trim();
		String localPath = getNamedStringParam( _argStruct, "localPath", "" ).trim();
		int revision = getNamedIntParam( _argStruct, "revision", SVNRepo.HEAD );
		boolean recursive = getNamedParam( _argStruct, "recursive", cfBooleanData.FALSE ).getBoolean();
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
			return this.execute( _session, repo, svnPath, localPath, revision, recursive, properties );

		} catch ( SVNException e ) {
			throwException( _session, "Unable to connect to " + name + ". Please provide a Valid SVN Repository" );
		}

		return cfBooleanData.FALSE;
	}


	/**
	 * overload of execute( cfSession, cfArgStructData ) for use in calling from Java
	 * 
	 * <code>properties = execute(_session,_repo,"temp/","C:\temp\",-1,true,true)</code>
	 * 
	 * @param _session
	 *          OpenBD Session object for error throwing
	 * @param _repo
	 *          SVNRepo connection already established
	 * @param _svnPath
	 *          String path in SVN to get
	 * @param _localPath
	 *          String path on the local file system to get files to
	 * @param _revision
	 *          Int revision to get
	 * @param _recur
	 *          boolean flag to indicate if we get all subdirectories (true: yes / false: no), default to false
	 * @param _properties
	 *          boolean flat to indicate if we expect a struct of properties are returned (true: yes / false: no), default to false
	 * @return if (properties) a struct modeled after the file structure retrieve, file elements will have properties values, directories will have structs
	 * @return if (!properties) true
	 * @since 2.0.0
	 */
	public cfData execute( cfSession _session, SVNRepo _repo, String _svnPath, String _localPath, int _revision, boolean _recur, boolean _properties ) throws cfmRunTimeException {

		File outDir = new File( _localPath );

		SVNRepository svn = _repo.getRepository();

		return fetchFiles( _session, svn, _repo, _svnPath, _revision, outDir, _recur, _properties );

	}


	/**
	 * Use to retieve the files in a directory, and sub-directories if specified
	 * 
	 * <code>fetchFiles( _session, svn, _repo, _svnPath, _revision, outDir, _recur, _properties )</code>
	 * 
	 * @param _session
	 *          OpenBD Session object for error throwing
	 * @param _repo
	 *          SVNRepo connection already established
	 * @param _svn
	 *          SVNRepository to work with
	 * @param _svnPath
	 *          String path in SVN to get
	 * @param _localPath
	 *          String path on the local file system to get files to
	 * @param _revision
	 *          Int revision to get
	 * @param _recursive
	 *          boolean flag to indicate if we get all subdirectories (true: yes / false: no), default to false
	 * @param _properties
	 *          boolean flat to indicate if we expect a struct of properties returned (true: yes / false: no), default to false
	 * @return if (properties) a struct modeled after the file structure retrieve, file elements will have properties values, directories will have structs
	 * @return if (!properties) true
	 * @see net.aw20.openbd.plugins.svn.SVNKey
	 * @since 1.0.0
	 */
	private cfData fetchFiles( cfSession _session, SVNRepository _svn, SVNRepo _repo, String _svnPath, int _revision, File _localFile, boolean _recur, boolean _properties ) throws cfmRunTimeException {

		cfStructData checkSums = new cfStructData();
		cfData ret = null;

		if ( !_localFile.exists() ) {
			_localFile.mkdirs();
		}
		_svnPath = _svnPath.replace( '\\', '/' );

		SVNProperties prop = new SVNProperties();
		Collection<SVNDirEntry> list = new ArrayList<>();
		SVNGetFile getFile = new SVNGetFile();


		try {
			_svn.getDir( _svnPath, _revision, prop, list );
		} catch ( SVNAuthenticationException e ) {
			throwException( _session, "Failed to authenticate user with svn. Check user permissions. " + e.getMessage() );
		} catch ( SVNException e ) {
			if ( _revision >= 0 ) {
				throwException( _session, "Failed to retrieve directory " + _svnPath + " from SVN at Revision #" + _revision +
						". Check connection to repository, and that remote directory exists at this revision. " + e.getMessage() );
			} else {
				throwException( _session, "Failed to retrieve directory " + _svnPath + " from SVN at HEAD. " +
						"Check connection to repository, and that remote directory exists at HEAD. " + e.getMessage() );
			}
		}

		Iterator<SVNDirEntry> cIT = list.iterator();

		while ( cIT.hasNext() ) {
			SVNDirEntry entry = cIT.next();

			String name = entry.getName();

			if ( name != null && !name.isEmpty() ) {
				String type = "";
				String path = ( !"".equals( _svnPath ) && !_svnPath.endsWith( "/" ) ? _svnPath + "/" + name : _svnPath + name );

				try {
					type = _repo.getPathType( path, _revision );
				} catch ( SVNException e ) {
					throwException( _session, "Failed to determine file structure element type of " + path + ". " + e.getMessage() );
				}

				File nextLocalFile = new File( _localFile, name );

				if ( type.equals( "dir" ) ) {
					if ( _recur ) {
						ret = fetchFiles( _session, _svn, _repo, path, _revision, nextLocalFile, _recur, _properties );
						if ( _properties ) {
							checkSums.put( name, ret );
						}
					} else {
						nextLocalFile.mkdirs();
					}
				} else {
					ret = getFile.execute( _session, _svn, path, nextLocalFile, _revision, _properties );
					if ( _properties ) {
						checkSums.put( name, getFile.execute( _session, _svn, path, nextLocalFile, _revision, _properties ) );
					}
				}

			}// if
		}// while

		if ( _properties ) {
			return checkSums;
		} else {
			return cfBooleanData.TRUE;
		}

	}


}
