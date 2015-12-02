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


import com.bluedragon.plugin.Plugin;
import com.bluedragon.plugin.PluginManagerInterface;
import com.naryx.tagfusion.xmlConfig.xmlCFML;


/**
 * OpenBD Plugin registration class
 * 
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 1.0.0
 */
public class LVPlugin implements Plugin {


	@Override
	public String getPluginDescription() {
		return "SVN plugin";
	}


	@Override
	public String getPluginName() {
		return "SVNPlugin";
	}


	@Override
	public String getPluginVersion() {
		return "2.0.0";
	}


	/**
	 * Used to register the SVN functions
	 * 
	 * @author AW20 Ltd
	 * @author Trace Sinclair
	 * @version 2.0.0
	 * @since 1.0.0
	 * 
	 * 
	 */
	@Override
	public void pluginStart( PluginManagerInterface _manager, xmlCFML _systemParameters ) {

		// register connection functions
		_manager.registerFunction( "SVNRegisterRepository", "net.aw20.openbd.plugins.svn.SVNRegisterRepository" );
		_manager.registerFunction( "SVNRemoveRepository", "net.aw20.openbd.plugins.svn.SVNRemoveRepository" );
		_manager.registerFunction( "SVNIsValid", "net.aw20.openbd.plugins.svn.SVNIsValid" );

		// register get functions
		_manager.registerFunction( "SVNUpdate", "net.aw20.openbd.plugins.svn.functions.SVNUpdate" );
		_manager.registerFunction( "SVNGetFile", "net.aw20.openbd.plugins.svn.functions.SVNGetFile" );
		_manager.registerFunction( "SVNGetDir", "net.aw20.openbd.plugins.svn.functions.SVNGetDir" );

		// register information functions
		_manager.registerFunction( "SVNLatestRevision", "net.aw20.openbd.plugins.svn.functions.SVNLatestRevision" );
		_manager.registerFunction( "SVNGetRevision", "net.aw20.openbd.plugins.svn.functions.SVNGetRevision" );
		_manager.registerFunction( "SVNLogView", "net.aw20.openbd.plugins.svn.functions.SVNLogView" );
		_manager.registerFunction( "SVNGetStatus", "net.aw20.openbd.plugins.svn.functions.SVNGetStatus" );
		_manager.registerFunction( "SVNDirectoryList", "net.aw20.openbd.plugins.svn.functions.SVNDirectoryList" );
		_manager.registerFunction( "SVNDiff", "net.aw20.openbd.plugins.svn.functions.SVNDiff" );

		// register commit function
		_manager.registerFunction( "SVNCommit", "net.aw20.openbd.plugins.svn.functions.SVNCommit" );

	}


	@Override
	public void pluginStop( PluginManagerInterface arg0 ) {}


}
