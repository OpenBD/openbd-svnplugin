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
public class DiffLines extends java.util.LinkedList<DiffLine> {

	private static final long serialVersionUID = 1L;


	/**
	 * Add a new diffLine to a given revision
	 * 
	 * @param _nRev
	 * @param _sSource
	 * @param _nLn
	 */
	public void addLineRev( int _nRev, String _sSource, int _nLn ) {
		// add the diffLine
		this.add( new DiffLine( _nRev, _sSource, _nLn ) );
	}


	/**
	 * Add a pristine diffLine across revisions
	 * 
	 * @param _nRev1
	 * @param _nLn1
	 * @param _nRev2
	 * @param _nLn2
	 */
	public void addLineRevPristine( int _nRev1, int _nLn1, int _nRev2, int _nLn2 ) {
		// get the diffLine index so we can update it
		int idx = getRevLineIndex( _nRev1, _nLn1 );
		// get the idffLine
		DiffLine currentLine = get( idx );
		DiffRevision newRevision = null;

		// if the lines is pristin
		if ( currentLine.isPristine() ) {
			// create a revisions for a pristine line
			newRevision = new DiffRevision( _nLn2 );
		} else {
			// create a revision for an exploeded line
			// constains a status, line, and source (copied from previouse revions)
			newRevision = new DiffRevision( _nLn2, DiffRevision.PRISTINE, currentLine.getRevision( _nRev1 ).getRevSource() );
		}

		// add the revision
		currentLine.setRevision( _nRev2, newRevision );
	}


	/**
	 * Add a new diffLine for a given revision
	 * 
	 * @param _nRev1
	 * @param _nLnAfter
	 * @param _nRev2
	 * @param _nLnNew
	 * @param _sSource
	 */
	public void addLineRevAdd( int _nRev1, int _nLnAfter, int _nRev2, int _nLnNew, String _sSource ) {
		// get the diffLine index at the previous revision for a starting point
		int idx = getRevLineIndex( _nRev1, _nLnAfter );

		// increment till we get to where the new line should be
		while ( idx + 1 < size() && !get( idx + 1 ).hasRevision( _nRev1 ) ) {
			// still not there
			idx++;
		}

		// add in the new line here
		this.add( idx + 1, new DiffLine( _nRev2, _sSource, _nLnNew, DiffRevision.ADD, _nLnAfter ) );
	}


	/**
	 * Add a removed diffLine for a given revision
	 * 
	 * @param _nRev
	 * @param _nLn
	 */
	public void addLineRevRemove( int _nRev, int _nLn ) {
		// get the diffLine index at the revisions so we can modify it
		int idx = getRevLineIndex( _nRev, _nLn );
		// get the line
		DiffLine currLine = get( idx );
		// explode the line
		currLine.explodeLine();
		// set the revision status as removed
		currLine.getRevision( _nRev ).put( DiffLine.STATUS, DiffRevision.REMOVE );
		// set the line status as removed
		currLine.setLineStatus( DiffRevision.REMOVE );
	}


	/**
	 * Add an updated diffLine for the given revisions
	 * 
	 * @param _nRev1
	 * @param _nLn1
	 * @param _nRev2
	 * @param _nLn2
	 * @param _sSource
	 */
	public void addLineRevUpdate( int _nRev1, int _nLn1, int _nRev2, int _nLn2, String _sSource ) {
		// get the diffLine index at the revisions so we can modify it
		int idx = getRevLineIndex( _nRev1, _nLn1 );
		// get the line
		DiffLine currLine = get( idx );
		// explode it
		currLine.explodeLine();
		// set the line status to updated
		currLine.setLineStatus( DiffRevision.UPDATE );
		// set the revision status to updated
		currLine.setRevision( _nRev2, new DiffRevision( _nLn2, DiffRevision.UPDATE, _sSource ) );
	}


	/**
	 * Get the index of a diffline for a line at a given revision
	 * 
	 * @param _nRev
	 * @param _idx
	 * @return
	 */
	public int getRevLineIndex( int _nRev, int _idx ) {
		// loop over all diffLine
		for ( int i = 0; i < size(); i++ ) {
			// get the diffLine
			DiffLine curr = get( i );
			// if we have a revision there, and the revision matches the line we are looking for
			if ( curr.hasRevision( _nRev ) && curr.getRevision( _nRev ).getRevLine() == _idx ) {
				// found it!
				return i;
			}
		}
		// line not found for this revision
		return -1;
	}


	/**
	 * Explode a diffLine at a given index
	 * 
	 * @param _idx
	 */
	public void explodeLine( int _idx ) {
		// explode the diffLine
		get( _idx ).explodeLine();
	}


	/**
	 * Fill missing revision information in all diffLine
	 * 
	 * @param _nRev
	 * @param _nRevOld
	 */
	public void fillLineRevisions( int _nRev, int _nRevOld ) {
		int nLine = 1;
		// loop over all lines
		for ( int i = 0; i < size(); i++ ) {
			// get the line
			DiffLine currLine = get( i );


			if ( currLine.isPristine() && !currLine.hasRevision( _nRev ) ) {
				// the lines is pristine and doesn't have the revision, add the pristine revision
				currLine.setRevision( _nRev, new DiffRevision( nLine ) );
				nLine++;
			} else if ( !currLine.getStatus().equals( DiffRevision.REMOVE ) ) {
				// the lines has been removed in previous revision,
				if ( !currLine.hasRevision( _nRev ) ) {
					// don't have this revision for this line
					// fill in a revision for this line
					DiffRevision rev = new DiffRevision( nLine, DiffRevision.PRISTINE, currLine.getRevision( _nRevOld ).getRevSource() );
					// set a filled flag
					rev.setFilled();
					currLine.setRevision( _nRev, rev );
				}
				nLine++;
			}
		}
	}


	/**
	 * insert a DiffLine for a given revision
	 * 
	 * @param _newLine
	 * @param _nRev1
	 * @param _nRev2
	 */
	public void lineInsert( DiffLine _newLine, int _nRev1, int _nRev2 ) {
		int nInsertIndex = 1;
		boolean bReplace = false;


		if ( size() == 0 ) {
			// this is the first line
			this.add( _newLine );
		} else {
			// find the correct place to insert it, loop over all lines
			for ( int i = 0; i < size(); i++ ) {
				// see if we have the prev revision, and our new line is yet to come
				if ( get( i ).hasRevision( _nRev1 ) && get( i ).getRevision( _nRev1 ).getRevLine() > _newLine.getRevision( _nRev1 ).getRevLine() ) {
					// see if the new rev is CREATE(0) or this line has the new revision and our new line is yet to come
					if ( _nRev2 == 0 || get( i ).hasRevision( _nRev2 ) && get( i ).getRevision( _nRev2 ).getRevLine() > _newLine.getRevision( _nRev2 ).getRevLine() ) {
						// see if the previous revision lines match, and the current revision lines match
						if ( get( i ).getRevision( _nRev1 ).getRevLine() == _newLine.getRevision( _nRev1 ).getRevLine() && get( i ).getRevision( _nRev2 ).getRevLine() == _newLine.getRevision( _nRev2 ).getRevLine() ) {
							// replace the line
							bReplace = true;
						}
						// go to next line
						nInsertIndex = i;
						// at insert point
						break;
					}
				}
				nInsertIndex = i + 1;
			}

			if ( bReplace ) {
				// replace the line
				set( nInsertIndex, _newLine );
			} else {
				// found the insert point
				if ( nInsertIndex > size() ) {
					// insert the new line at the end
					this.add( _newLine );
				} else {
					// insert the new line at the position
					this.add( nInsertIndex, _newLine );
				}
			}

		}
	}


}
