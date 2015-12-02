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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.aw20.openbd.plugins.svn.LetterPairSimiarity;
import net.aw20.openbd.plugins.svn.functions.SVNDiff;


/**
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 2.0.0
 */
public class Diff {

	public static final String SVNDIFFSECTIONHEADER = "@@";
	public static final int DIFFLINESTART = 4;
	private final DiffLines auDiff;
	private final List<Integer> anRevs;
	private static final double SIMTOLERANCE = .6;
	private static final String SVNNONEWLINE = "\\ No newline at end of file";
	private static final String HEADERRMSTART = "removeStart";
	private static final String HEADERRMCOUNT = "removeCount";
	private static final String HEADERADSTART = "addStart";
	private static final String HEADERADCOUNT = "addCount";


	/**
	 * set up to parse a diff
	 * 
	 * @param _fileContent
	 *          the content of a file
	 * @param _revision
	 *          the revision of the file
	 */
	public Diff( String _fileContent, int _revision ) {
		String[] aDiff;
		if ( !_fileContent.isEmpty() ) {
			// split the file on the Line Feed
			aDiff = _fileContent.split( SVNDiff.LF );
		} else {
			// empty array if no file passed
			aDiff = new String[0];
		}

		auDiff = new DiffLines();
		anRevs = new ArrayList<Integer>();

		// add each item for the source into the diffLines
		for ( int i = 0; i < aDiff.length; i++ ) {
			auDiff.addLineRev( _revision, aDiff[i], i + 1 );
		}
	}


	/**
	 * parse the diff string
	 * 
	 * @param _diff
	 * @param _compact
	 * @return diffLines
	 */
	public DiffLines parseDiff( String _diff, boolean _compact ) {
		// split the diffstring on the ASCII-28 for multiple revision diffs
		String[] aDiffs = _diff.split( SVNDiff.DELIMITER );

		for ( int i = aDiffs.length; i > 0; i-- ) {
			// parse each revision diff
			parseDiffSelection( aDiffs[i - 1] );
		}

		// see if we need to compact this to only modified lines
		if ( _compact ) {
			// remove all pristine lines
			return compact();
		} else {
			// return it all
			return auDiff;
		}
	}


	/**
	 * compact the diffLines, and remove all pristine lines
	 * 
	 * @return
	 */
	private DiffLines compact() {
		DiffLines auCompact = new DiffLines();

		for ( int i = 0; i < auDiff.size(); i++ ) {
			Map<String, Object> tmp = auDiff.get( i );
			if ( !tmp.get( "status" ).equals( DiffRevision.PRISTINE ) ) {
				auCompact.add( (DiffLine) tmp );
			}
		}

		return auCompact;
	}


	/**
	 * parse a diff string for a revision
	 * 
	 * @param _diff
	 */
	private void parseDiffSelection( String _diff ) {
		String[] diffLines = _diff.split( "\\r?\\n" );
		Map<String, Integer> uSegment;
		DiffLines aAdd = new DiffLines(), aRemove = new DiffLines();
		int idxA = 0, idxR = 0, lnA = 0, lnR = 0, nAfter = 0, nRevOld = 0, nRevNew = 0;

		if ( diffLines.length > 4 ) {
			nRevOld = getRevision( diffLines[2] ); // the old revision
			nRevNew = getRevision( diffLines[3] ); // the new revision
			// save the reivisions parsed
			if ( anRevs.indexOf( nRevOld ) < 0 ) {
				anRevs.add( nRevOld );
			}
			anRevs.add( nRevNew );

			// loop over the lines in the diff
			for ( int i = 4; i < diffLines.length; i++ ) {
				if ( diffLines[i].startsWith( SVNNONEWLINE ) ) {
					// special lines content for SVN
					// do nothing with this
				} else if ( idxA == 0 && idxR == 0 ) {
					// no line count that we are working with
					if ( diffLines[i].startsWith( SVNDIFFSECTIONHEADER ) ) {
						// parse the revisions lines identification
						// tells us what line to start on in which revision, and how many lines are affected
						// for each section, lines removed will be listed before lines added
						// a line that was updated will show up a line removed, and a line deleted
						// modified lines will be followed by at least 1 pristine line, unless at the end of a file
						uSegment = parseHeader( diffLines[i] );
						idxA = uSegment.get( HEADERADCOUNT );
						idxR = uSegment.get( HEADERRMCOUNT );
						// need to set lnA as #-1 to allow for adding at the beginning of a section
						lnA = uSegment.get( HEADERADSTART ) - 1;
						lnR = uSegment.get( HEADERRMSTART );
						nAfter = lnR - 1;
					}
				} else {
					// each line will start with an indicator as to what happened with it
					if ( diffLines[i].startsWith( SVNDiff.PREFIXADD ) ) {
						// added line
						// update counters
						idxA--;
						lnA++;
						// set up a line with the added content
						DiffLine tmp = new DiffLine( nRevNew, diffLines[i].substring( 1 ), lnA, DiffRevision.ADD, nAfter );
						// add the new line for batch processing
						aAdd.add( tmp );
					} else if ( diffLines[i].startsWith( SVNDiff.PREFIXREMOVE ) ) {
						// removed line
						// set up a line with the deleted content
						DiffLine tmp = new DiffLine( nRevOld, diffLines[i].substring( 1 ), lnR, DiffRevision.REMOVE, nAfter );
						// add the removed line for batch processing
						aRemove.add( tmp );
						// i[date counters
						idxR--;
						lnR++;
					} else {
						// we have a pristine line

						// if we have any new and removed lines to batch together, process them
						if ( !aAdd.isEmpty() || !aRemove.isEmpty() ) {
							// this allows us to check for lines that are updated lines, and order the lines together
							batchSourceLines( collapseSourceLines( aRemove, aAdd, nRevOld, nRevNew ), nRevOld, nRevNew );
						}

						// update the add counters
						nAfter = lnR;
						idxA--;
						lnA++;
						// add the pristine line
						auDiff.addLineRevPristine( nRevOld, lnR, nRevNew, lnA );
						// update the remove counters
						idxR--;
						lnR++;

						// clear the batch processing holders
						aAdd.clear();
						aRemove.clear();
					}
				}

			}

			// we are done with the diff string, see if there are still lines to be batched at the end
			if ( aAdd.size() > 0 || aRemove.size() > 0 ) {
				// this allows us to check for lines that are updated lines, and order the lines together
				batchSourceLines( collapseSourceLines( aRemove, aAdd, nRevOld, nRevNew ), nRevOld, nRevNew );
			}
		}

		// fill in any missing revision data so we have a complete data modal across all lines for the revisions
		auDiff.fillLineRevisions( nRevNew, nRevOld );

	}


	/**
	 * process the added, and removed lines together
	 * 
	 * @param _lines
	 * @param _nRevOld
	 * @param _nRevNew
	 */
	private void batchSourceLines( DiffLines _lines, int _nRevOld, int _nRevNew ) {
		// loop over all the lines
		for ( int i = 0; i < _lines.size(); i++ ) {
			switch ( _lines.get( i ).getStatus() ) {
				case DiffRevision.UPDATE :
					// process an updated line
					auDiff.addLineRevUpdate( _nRevOld,
							_lines.get( i ).getRevision( _nRevOld ).getRevLine(),
							_nRevNew,
							_lines.get( i ).getRevision( _nRevNew ).getRevLine(),
							_lines.get( i ).getRevision( _nRevNew ).getRevSource() );
					break;
				case DiffRevision.REMOVE :
					// process a removed line
					auDiff.addLineRevRemove( _nRevOld, _lines.get( i ).getRevision( _nRevOld ).getRevLine() );
					break;
				case DiffRevision.ADD :
					// process an added line
					auDiff.addLineRevAdd( _nRevOld,
							_lines.get( i ).getRevision( _nRevNew ).getAfter(),
							_nRevNew,
							_lines.get( i ).getRevision( _nRevNew ).getRevLine(),
							_lines.get( i ).getRevision( _nRevNew ).getRevSource() );
					break;
				case DiffRevision.PRISTINE :
					// process a pristine line
					auDiff.addLineRevPristine( _nRevOld,
							_lines.get( i ).getRevision( _nRevOld ).getRevLine(),
							_nRevNew,
							_lines.get( i ).getRevision( _nRevNew ).getRevLine() );
					break;
			}
		}

	}


	/**
	 * test if a removed line and an added line look an updated line, and collapse the 2 lines
	 * 
	 * @param _aRemove
	 * @param _aAdd
	 * @param _nRevOld
	 * @param _nRevNew
	 * @return
	 */
	private DiffLines collapseSourceLines( DiffLines _aRemove, DiffLines _aAdd, int _nRevOld, int _nRevNew ) {
		DiffLines aLines = new DiffLines();
		boolean bMerged = false;

		// make sure we have removed and added lines to look at
		if ( !_aRemove.isEmpty() && !_aAdd.isEmpty() ) {
			// loop over all the added lines
			for ( int i = 0; i < _aAdd.size(); i++ ) {
				bMerged = false;
				// loop over all the removed lines
				for ( int k = 0; k < _aRemove.size(); k++ ) {

					// calculate the double (forward and backwards) string comparison
					double nSimilar = LetterPairSimiarity.doubleCompareStrings(
							_aAdd.get( i ).getRevision( _nRevNew ).getRevSource(),
							_aRemove.get( k ).getRevision( _nRevOld ).getRevSource() );

					// if the 2 lines are above the similarity threshold
					if ( nSimilar > SIMTOLERANCE ) {
						// the lines are similar and should be considered an update
						DiffLine tmp = new DiffLine();
						tmp.setLineStatus( DiffRevision.UPDATE );

						tmp.setRevision( _nRevNew, _aAdd.get( i ).getRevision( _nRevNew ) );
						tmp.setRevision( _nRevOld, _aRemove.get( k ).getRevision( _nRevOld ) );

						// loop over the rest of the lines to add
						for ( int l = i + 1; l < _aAdd.size(); l++ ) {
							// check to see if the lines should be inserted in the same area
							if ( _aAdd.get( l ).getRevision( _nRevNew ).getAfter() == tmp.getRevision( _nRevNew ).getAfter() ) {
								// update where they should be inserted to preserve line order
								_aAdd.get( l ).getRevision( _nRevNew ).put( "after", tmp.getRevision( _nRevOld ).get( "line" ) );
							} else {
								// gone past where this insert should happen, we are done
								break;
							}
						}

						// remove previous removed lines from the pool to be matched against, we have mathced beyond them
						for ( int m = 0; m < k; m++ ) {
							aLines.lineInsert( _aRemove.get( 0 ), _nRevOld, 0 );
							_aRemove.remove( 0 );
						}

						// remove the item we just matched
						_aRemove.remove( 0 );

						// add the merged line
						aLines.lineInsert( tmp, _nRevNew, _nRevOld );
						bMerged = true;
						break;
					}
				}
				if ( !bMerged ) {
					// didn't merge anything, add the added line
					aLines.lineInsert( _aAdd.get( i ), _nRevNew, 0 );
				}
			}

			for ( int m = 0; m < _aRemove.size(); m++ ) {
				// merge in all unmerged removed lines
				aLines.lineInsert( _aRemove.get( m ), _nRevOld, 0 );
			}
		} else if ( !_aRemove.isEmpty() ) {
			// we only have removed lines
			aLines = _aRemove;
		} else {
			// we only had added lines
			aLines = _aAdd;
		}

		return aLines;
	}


	/**
	 * parse the revision line
	 * 
	 * @param _revLine
	 * @return the revisions
	 */
	private static int getRevision( String _revLine ) {
		// regex parse the line
		Pattern revLine = Pattern.compile( "[\\+\\-]{3}.+\\(revision (\\d*)\\)" );
		Matcher revMatcher = revLine.matcher( _revLine );
		revMatcher.matches();
		String tmp = revMatcher.group( 1 );
		// return the revision
		return Integer.parseInt( tmp );
	}


	/**
	 * parse the diff section header
	 * 
	 * @param _header
	 * @return map with the section header broke apart
	 */
	private static Map<String, Integer> parseHeader( String _header ) {
		// set up a map
		Map<String, Integer> uSegment = new HashMap<String, Integer>();

		// regex parse the line
		Pattern headerLine = Pattern.compile( "@@ -(\\d+)(,(\\d+))? \\+(\\d+)(,(\\d+))? @@" );
		Matcher headerMatcher = headerLine.matcher( _header );
		headerMatcher.matches();

		// collect all the parts of the section header
		uSegment.put( HEADERRMSTART, Integer.parseInt( headerMatcher.group( 1 ) ) );
		if ( headerMatcher.group( 2 ) != null ) {
			uSegment.put( HEADERRMCOUNT, Integer.parseInt( headerMatcher.group( 3 ) ) );
		} else {
			uSegment.put( HEADERRMCOUNT, 1 );
		}

		uSegment.put( HEADERADSTART, Integer.parseInt( headerMatcher.group( 4 ) ) );
		if ( headerMatcher.group( 5 ) != null ) {
			uSegment.put( HEADERADCOUNT, Integer.parseInt( headerMatcher.group( 6 ) ) );
		} else {
			uSegment.put( HEADERADCOUNT, 1 );
		}

		// return the map with all the sections in it
		return uSegment;
	}


}
