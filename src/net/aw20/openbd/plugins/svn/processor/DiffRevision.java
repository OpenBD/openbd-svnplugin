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
package net.aw20.openbd.plugins.svn.processor;


/**
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 2.0.0
 */
public class DiffRevision extends java.util.HashMap<String, Object> {

	private static final long serialVersionUID = 1L;
	public static final String PRISTINE = "P";
	public static final String ADD = "A";
	public static final String REMOVE = "D";
	public static final String UPDATE = "U";
	public static final String LINE = "line";
	public static final String STATUS = "status";
	public static final String AFTER = "after";
	public static final String SOURCE = "source";
	public static final String FILL = "fill";
	public static final String FILLVAL = "Filled";


	/**
	 * create a revision with a line number
	 * 
	 * @param _idx
	 */
	DiffRevision( int _idx ) {
		put( LINE, _idx );
		put( STATUS, PRISTINE );
	}


	/**
	 * Create a revisions with a line number, status, and source
	 * 
	 * @param _idx
	 * @param _status
	 * @param _source
	 */
	DiffRevision( int _idx, String _status, String _source ) {
		this( _idx );
		put( STATUS, _status );
		setRevSource( _source );
	}


	/**
	 * Create a revision with a line number, status, source, and line after number
	 * 
	 * @param _idx
	 * @param _status
	 * @param _source
	 * @param _idxAfter
	 */
	DiffRevision( int _idx, String _status, String _source, int _idxAfter ) {
		this( _idx, _status, _source );
		put( AFTER, _idxAfter );
	}


	/**
	 * set the source for this revision
	 * 
	 * @param _source
	 */
	public void setRevSource( String _source ) {
		put( SOURCE, _source );
	}


	/**
	 * Get the source for this revision
	 * 
	 * @return
	 */
	public String getRevSource() {
		String source = "";

		// make sure we have source here first
		if ( containsKey( SOURCE ) ) {
			// get the source as a String
			source = (String) get( SOURCE );
		}
		return source;
	}


	/**
	 * get the line number for this revision
	 * 
	 * @return
	 */
	public int getRevLine() {
		return (int) get( LINE );
	}


	/**
	 * get the status for this revision
	 * 
	 * @return
	 */
	public String getRevStatus() {
		return (String) get( STATUS );
	}


	/**
	 * test if the revision is pristine
	 * 
	 * @return
	 */
	public boolean isRevPristine() {
		return ( getRevStatus() == PRISTINE );
	}


	/**
	 * set the status for this revision
	 * 
	 * @param _status
	 */
	public void setRevStatus( String _status ) {
		// if we already have a status, and it is changing
		if ( containsKey( STATUS ) && !get( STATUS ).equals( "" ) ) {
			// append the status
			put( STATUS, get( STATUS ) + _status );
		} else {
			// set the status
			put( STATUS, _status );
		}
	}


	/**
	 * Get the line after number for this revision
	 * 
	 * @return
	 */
	public int getAfter() {
		return (int) get( AFTER );
	}


	/**
	 * set the revision as being filled automatically
	 */
	public void setFilled() {
		put( FILL, FILLVAL );
	}

}
