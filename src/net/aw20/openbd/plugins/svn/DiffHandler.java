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

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.ISVNDiffStatusHandler;
import org.tmatesoft.svn.core.wc.SVNDiffStatus;


/**
 * @author Dmitry Pavlenko
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 2.0.0
 * @see http://svnkit.com
 * @see org.tmatesoft.svn.core.wc.ISVNDiffStatusHandler#handleDiffStatus(org.tmatesoft.svn.core.wc.SVNDiffStatus)
 */

public class DiffHandler implements ISVNDiffStatusHandler {

	private SVNDiffStatus _status = null;


	@Override
	public void handleDiffStatus( SVNDiffStatus diffStatus ) throws SVNException {
		_status = diffStatus;
	}


	public SVNDiffStatus getDiffStatus() {
		return _status;
	}

}
