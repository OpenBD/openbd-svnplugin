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


import java.util.Map;

import org.tmatesoft.svn.core.SVNException;

import com.naryx.tagfusion.cfm.engine.cfArgStructData;
import com.naryx.tagfusion.cfm.engine.cfBooleanData;
import com.naryx.tagfusion.cfm.engine.cfData;
import com.naryx.tagfusion.cfm.engine.cfSession;
import com.naryx.tagfusion.cfm.engine.cfmRunTimeException;
import com.naryx.tagfusion.expression.function.functionBase;
import net.aw20.openbd.plugins.svn.SVNRepo;


/**
 * OpenBD class for function: SVNIsValid()
 * 
 * Called from <i>OpenBD</i>
 * test = SVNIsValid("myRepository");
 * 
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 2.0.0
 * @see http://svnkit.com
 */
public class SVNIsValid extends functionBase {

	private static final long serialVersionUID = 1L;


	public SVNIsValid() {
		min = 1;
		max = 1;
		setNamedParams( new String[] { "name" } );
	}


	@Override
	public String[] getParamInfo() {
		return new String[] {
				"Name of the repository"
		};
	}


	@Override
	public Map<String, String> getInfo() {
		return makeInfo(
				"SVN",
				"SVN Repository: Test if a repository is valid",
				ReturnType.BOOLEAN );
	}


	/**
	 * Used to test if SVN connection is valid
	 * 
	 * Called from <i>OpenBD</i>
	 * test = SVNIsValid("myRepository");
	 * 
	 * @param _session
	 * @param _argStruct
	 *          name: the name of the repository to test
	 * @return true/false if a connection could be established with the named repository credentials
	 * @throws cfmRunTimeException
	 *           if a repository name is not supplied
	 * @see net.aw20.openbd.plugins.svn.SVNRegisterRepository
	 * @see net.aw20.openbd.plugins.svn.SVNKeyFactory
	 * @see net.aw20.openbd.plugins.svn.SVNKey
	 * @since 2.0.0
	 */
	@Override
	public cfData execute( cfSession _session, cfArgStructData _argStruct ) throws cfmRunTimeException {

		String repoName = getNamedStringParam( _argStruct, "name", null );

		if ( repoName == null || repoName.isEmpty() ) {
			throwException( _session, "Please provide a value for the Repository Name" );
		}


		try ( SVNRepo repo = new SVNRepo( repoName ) ) {
			repo.getRepository().testConnection();
		} catch ( SVNException e ) {
			return cfBooleanData.FALSE;
		}

		return cfBooleanData.TRUE;
	}


}
