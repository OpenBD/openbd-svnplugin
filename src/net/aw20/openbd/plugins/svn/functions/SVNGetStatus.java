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


import java.util.Map;

import net.aw20.openbd.plugins.svn.SVNRepo;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import com.naryx.tagfusion.cfm.engine.cfArgStructData;
import com.naryx.tagfusion.cfm.engine.cfData;
import com.naryx.tagfusion.cfm.engine.cfSession;
import com.naryx.tagfusion.cfm.engine.cfStringData;
import com.naryx.tagfusion.cfm.engine.cfmRunTimeException;


/**
 * OpenBD class for function: SVNGetStatus()
 * 
 * Called from OpenBD <code>type = SVNGetStatus("mySVN","temp\",-1)</code>
 * 
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 2.0.0
 * @see http://svnkit.com
 */
public class SVNGetStatus extends SVNLogView {

	private static final long serialVersionUID = 1L;


	public SVNGetStatus() {
		min = 2;
		max = 3;
		setNamedParams( new String[] {
				"name",
				"svnPath",
				"revision" } );
	}


	@Override
	public String[] getParamInfo() {
		return new String[] {
				"Name of the SVN Repository to use",
				"SVN Path to get info on",
				"The revision to use, default HEAD" };
	}


	@Override
	public Map<String, String> getInfo() {
		return makeInfo(
				"svn",
				"Returns the file at the given path and revision in the repository",
				ReturnType.NUMERIC );
	}


	/**
	 * Used to get the SVN element type
	 * 
	 * Called from OpenBD <code>type = SVNGetStatus("mySVN","temp\",-1)</code>
	 * 
	 * @param _session
	 * @param _argStruct
	 *          name: String repository name to use
	 *          svnPath: String path in SVN to get information on
	 *          revision: Int revision to get
	 * @return String what type of element exists at the _path (dir/file/none/unknown/ERROR)
	 * @throws cfmRunTimeException
	 *           SVN Repository name not supplied
	 *           Unable to connect to repository
	 * @since 2.0.0
	 */
	@Override
	public cfData execute( cfSession _session, cfArgStructData _argStruct ) throws cfmRunTimeException {

		String name = getNamedStringParam( _argStruct, "name", "" ).trim();
		String svnPath = getNamedStringParam( _argStruct, "svnPath", "" ).trim();
		int revision = getNamedIntParam( _argStruct, "revision", SVNRepo.HEAD );

		SVNRepository svn = null;

		// Validate arguments
		if ( name.isEmpty() ) {
			throwException( _session, "Please provide a SVN Repository" );
		}

		try ( SVNRepo repo = new SVNRepo( name ) ) {
			svn = repo.getRepository();

			try {
				return new cfStringData( svn.checkPath( svnPath, revision ).toString() );
			} catch ( SVNException e ) {
				throwException( _session, e.toString() );
			}

		} catch ( SVNException e ) {
			throwException( _session, "Unable to connect to " + name + ". Please provide a Valid SVN Repository" );
		}


		return new cfStringData( "ERROR" );

	}


}
