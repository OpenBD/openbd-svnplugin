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
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;


/**
 * Storage class for working with SVN repository credentials
 * 
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 2.0.0
 */
public class SVNKeyFactory {

	private static final Map<String, SVNKey> SVNRepositories = new HashMap<>();


	/**
	 * Register a repository by storing the credentials associated to the name
	 * 
	 * <code>SVNKeyFactory.registerSVN("TestSVN", mySVNKey)</code>
	 * 
	 * @param _name
	 *          String value of the name of the repository (will be stored in lowercase)
	 * @param _srep
	 *          SVNKey with credentials
	 * @see net.aw20.openbd.plugins.svn.SVNKey
	 * @since 2.0.0
	 */
	public static void registerSVN( String _name, SVNKey _srep ) {
		synchronized ( SVNRepositories ) {
			if ( _name != null ) {
				String name = _name.trim().toLowerCase().trim();

				if ( !name.isEmpty() && _srep != null ) {
					SVNRepositories.put( name, _srep );
				}
			}
		}

	}


	/**
	 * Get a stored repository by its associted name
	 * 
	 * <code>mySVNKey = SVNKeyFactory.getRepo("TestSVN")</code>
	 * 
	 * @param _name
	 *          String value of the name of the repository (stored in lowercase)
	 * @return the SVNKey that was stored to the _name
	 * @see net.aw20.openbd.plugins.svn
	 * @since 2.0.0
	 */
	public static SVNKey getRepo( String _name ) {
		if ( _name != null ) {
			String name = _name.trim().toLowerCase().trim();
			SVNKey key = null;

			synchronized ( SVNRepositories ) {
				key = SVNRepositories.get( name );
			}

			if ( key != null ) {
				File keyFile = new File( key.getPath() );
				File keyPath = new File( keyFile.getParent() );

				if ( !keyPath.exists() ) {
					keyPath.mkdirs();
				}

				if ( key.isTemp() && !keyFile.exists() ) {
					synchronized ( key ) {
						if ( key.isTemp() && !keyFile.exists() ) {
							try ( PrintWriter writer = new PrintWriter( key.getPath() ) ) {
								writer.print( key.getKey() );
								keyFile.deleteOnExit();
							} catch ( FileNotFoundException e ) {
								return null;
							}
						}
					}
				}

			}
			return key;
		}
		return null;

	}


	/**
	 * Remove a register repository associated to a name
	 * 
	 * <code>SVNKeyFactory.removeRepo("TestSVN")</code>
	 * 
	 * @param _name
	 *          String value of the name of the repository (stored in lowercase)
	 * @see net.aw20.openbd.plugins.svn.SVNKey
	 * @since 2.0.0
	 */
	public static void removeRepo( String _name ) {
		if ( _name != null ) {
			String name = _name.toLowerCase().trim();

			synchronized ( SVNRepositories ) {
				SVNKey key = SVNRepositories.remove( name );
				if ( key != null && key.isTemp() ) {
					new File( key.getPath() ).delete();
				}
			}

		}
	}


}
