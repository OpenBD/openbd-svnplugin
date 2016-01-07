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

import com.naryx.tagfusion.cfm.engine.cfArgStructData;
import com.naryx.tagfusion.cfm.engine.cfBooleanData;
import com.naryx.tagfusion.cfm.engine.cfData;
import com.naryx.tagfusion.cfm.engine.cfmRunTimeException;
import com.naryx.tagfusion.expression.function.functionBase;
import com.naryx.tagfusion.cfm.engine.cfSession;


/**
 * OpenBD class for function: SVNRemoveRepository()
 * 
 * Called from <i>OpenBD</i>
 * repoName = SVNRemoveRespository("myRepository");
 * 
 * @author tsinclair
 * @version 2.0.0
 * @since 2.0.0
 */
public class SVNRemoveRepository extends functionBase {

	private static final long serialVersionUID = 1L;


	public SVNRemoveRepository() {
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
				"SVN Repository: Removes a previously registered datasource",
				ReturnType.BOOLEAN );
	}


	/**
	 * Used to remove the registration for a SVN repository
	 * 
	 * @param _session
	 * 
	 * @param _argStruct
	 *          name: the name of the repository registration to delete (converted to lowercase)
	 * @return the name of SVN repository created, blank if the repository was not found
	 * @throws cfmRunTimeException
	 *           if a name is not supplied for the repository
	 * @since 2.0.0
	 */
	@Override
	public cfData execute( cfSession _session, cfArgStructData _argStruct ) throws cfmRunTimeException {

		String repoName = getNamedStringParam( _argStruct, "name", "" ).trim();

		if ( repoName.isEmpty() ) {
			throwException( _session, "Invalid name for SVN Repository" );
		}

		SVNKey repo = SVNKeyFactory.getRepo( repoName );

		if ( repo != null ) {
			if ( repo.isTemp() ) {
				new File( repo.getPath() ).delete();
			}
			SVNKeyFactory.removeRepo( repoName );
			return cfBooleanData.TRUE;
		}

		return cfBooleanData.FALSE;
	}

}
