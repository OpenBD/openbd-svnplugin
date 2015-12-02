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

import java.util.HashMap;
import java.util.Map;


/**
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 2.0.0
 */
public class DiffLine extends java.util.HashMap<String, Object> {

	private static final long serialVersionUID = 1L;

	private Map<Integer, DiffRevision> revData;
	public static final String SOURCE = "source";
	public static final String REVISION = "revision";
	public static final String STATUS = "status";
	public static final String LINEEXPLODE = "explode";
	public static final String EXPLODEVAL = "Exploded";


	/**
	 * Create a blank diffLine
	 */
	DiffLine() {

	}


	/**
	 * initialize a diffLine with a revision, source, and line number
	 * 
	 * @param _nRev
	 * @param _source
	 * @param _idx
	 */
	DiffLine( int _nRev, String _source, int _idx ) {
		// set the source
		put( SOURCE, _source );
		// set the line status as pristine
		setLineStatus( DiffRevision.PRISTINE );
		// set up the revision
		setRevision( _nRev, new DiffRevision( _idx ) );
	}


	/**
	 * initialize a diffLine with revision, source, line number, and status
	 * 
	 * @param _nRev
	 * @param _source
	 * @param _idx
	 * @param _status
	 */
	DiffLine( int _nRev, String _source, int _idx, String _status ) {
		// set the line status
		setLineStatus( _status );
		// set up the revision
		setRevision( _nRev, new DiffRevision( _idx, _status, _source ) );
	}


	/**
	 * initialize a diffLine with revision, source, line number, status, added after line number
	 * 
	 * @param _nRev
	 * @param _source
	 * @param _idx
	 * @param _status
	 * @param _idxAfter
	 */
	DiffLine( int _nRev, String _source, int _idx, String _status, int _idxAfter ) {
		// set the line status
		setLineStatus( _status );
		// set up the revision
		setRevision( _nRev, new DiffRevision( _idx, _status, _source, _idxAfter ) );
	}


	/**
	 * set a revsions map, map will contain key of the revsion, and data is passed in
	 * 
	 * @param _revision
	 * @param _revData
	 */
	public void setRevision( int _revision, DiffRevision _revData ) {
		if ( revData == null ) {
			// we have revision data to set up first
			revData = new HashMap<Integer, DiffRevision>();
			put( REVISION, revData );
		}

		// set up the revisions
		revData.put( _revision, _revData );
	}


	/**
	 * test to see if we have a given revision for this line
	 * 
	 * @param _revision
	 * @return
	 */
	public boolean hasRevision( int _revision ) {
		// test if have a mapkey
		return revData.containsKey( _revision );
	}


	/**
	 * get the revision data for this line
	 * 
	 * @param _revision
	 * @return
	 */
	public DiffRevision getRevision( int _revision ) {
		DiffRevision data = null;

		// make sure we have data for the revision
		if ( hasRevision( _revision ) ) {
			data = revData.get( _revision );
		}

		// return the data
		return data;
	}


	/**
	 * get the line status (A,D,U)
	 * 
	 * @return
	 */
	public String getStatus() {
		return (String) get( STATUS );
	}


	/**
	 * have a line with 1+ revisions that has a change on it now
	 * an unexploded line will look like, the source is common to all revisions
	 * {source:"",status:"",revisions:{1:{line:1}}}
	 * an exploded line will look like, the source becomes unique to all revisions
	 * {status:"",revisions:{1:{status:"",source:"",line:1}}}
	 */
	public void explodeLine() {
		// make the line is not already exploeded
		if ( containsKey( SOURCE ) ) {

			// set the exploded flag
			put( LINEEXPLODE, EXPLODEVAL );

			// loop over all revisions
			for ( Map.Entry<Integer, DiffRevision> currRevData : revData.entrySet() ) {
				// add the source to the revision
				currRevData.getValue().setRevSource( (String) get( SOURCE ) );
			}

			// remove the common source
			remove( SOURCE );
		}
	}


	/**
	 * check if the line is pristine
	 * 
	 * @return
	 */
	public boolean isPristine() {
		return containsKey( STATUS ) && get( STATUS ) == DiffRevision.PRISTINE;
	}


	/**
	 * Set the line status
	 * 
	 * @param _status
	 */
	public void setLineStatus( String _status ) {
		put( STATUS, _status );
	}


	/**
	 * Set the status for a given revision
	 * 
	 * @param _revision
	 * @param _Status
	 */
	public void setLineRevStatus( int _revision, String _status ) {
		// make sure we have that revision
		if ( hasRevision( _revision ) ) {
			// set the revision status
			getRevision( _revision ).setRevStatus( _status );
		}
	}


}
