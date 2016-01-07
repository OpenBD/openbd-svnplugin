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


/**
 * OpenBD class for function: SVNLatestRevision()
 * 
 * Called from OpenBD <code>rev = SVNLatestRevision("mySVN")</code>
 * 
 * @author AW20 Ltd
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 1.0.0
 * @see http://svnkit.com
 */
import java.util.Map;

import com.naryx.tagfusion.cfm.engine.cfArgStructData;
import com.naryx.tagfusion.cfm.engine.cfData;
import com.naryx.tagfusion.cfm.engine.cfSession;
import com.naryx.tagfusion.cfm.engine.cfmRunTimeException;


public class SVNLatestRevision extends SVNLogView {

	private static final long serialVersionUID = 1L;


	public SVNLatestRevision() {
		min = 1;
		max = 1;
		setNamedParams( new String[] { "name" } );
	}


	@Override
	public String[] getParamInfo() {
		return new String[] { "Name of the SVN Repository to use" };
	}


	@Override
	public Map<String, String> getInfo() {
		return makeInfo(
				"svn",
				"Returns the latest revision number in the repository",
				ReturnType.NUMERIC );
	}


	/**
	 * Used to get the latest revision number of SVN
	 * 
	 * Called from OpenBD <code>rev = SVNLatestRevision("mySVN")</code>
	 * 
	 * @param _session
	 * @param _argStruct
	 *          name: String repository name to use
	 * @return Int value of the SVN current revision
	 * @throws cfmRunTimeException
	 *           SVN Repository name not supplied
	 *           Unable to connect to repository
	 * @since 1.0.0
	 */
	@Override
	public cfData execute( cfSession _session, cfArgStructData _argStruct ) throws cfmRunTimeException {
		// Get all arguments
		String name = getNamedStringParam( _argStruct, "name", "" ).trim();

		// Validate arguments
		if ( name.isEmpty() ) {
			throwException( _session, "Please provide a SVN Repository" );
		}

		SVNGetRevision SVNfn = new SVNGetRevision();
		return SVNfn.execute( _session, _argStruct );

	}


}
