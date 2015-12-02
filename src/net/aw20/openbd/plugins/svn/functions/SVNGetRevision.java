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


import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import net.aw20.openbd.plugins.svn.SVNRepo;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

import com.naryx.tagfusion.cfm.engine.cfArgStructData;
import com.naryx.tagfusion.cfm.engine.cfData;
import com.naryx.tagfusion.cfm.engine.cfNumberData;
import com.naryx.tagfusion.cfm.engine.cfSession;
import com.naryx.tagfusion.cfm.engine.cfStringData;
import com.naryx.tagfusion.cfm.engine.cfmRunTimeException;
import com.naryx.tagfusion.expression.function.functionBase;


/**
 * OpenBD class for function: SVNGetStatus()
 * 
 * Called from OpenBD <code>
 * 	HEADRev = SVNGetRevision("mySVN","temp/","HEAD");
 * 	PrevRev = SVGGetRevision("mySVN","tmp/","PREVIOUS");
 * 	PrevPrevRev = SVGGetRevision("mySVN","tmp/","1000");
 * 	CreateRev = SVNGetRevision("mySVN","tmp/","CREATE");
 * 	HistoryList = SVNGetRevision("mySVN","/tmp","HISTORY");
 * </code>
 * 
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 2.0.0
 * @see http://svnkit.com
 */
public class SVNGetRevision extends functionBase {

	private static final long serialVersionUID = 1L;


	public SVNGetRevision() {
		min = 1;
		max = 3;
		setNamedParams( new String[] {
				"name",
				"svnPath",
				"revision" } );
	}


	@Override
	public String[] getParamInfo() {
		return new String[] {
				"Name of the SVN Repository to use",
				"SVN Path to get info on",
				"What to get {HEAD|PREVIOUS|CREATE|HISTORY|Number}" };
	}


	@Override
	public Map<String, String> getInfo() {
		return makeInfo(
				"svn",
				"Return revisions associated with a resource",
				ReturnType.STRING );
	}


	/**
	 * Used to get the SVN element type
	 * 
	 * <code>
	 * 	//get the revision number running at HEAD
	 * 	HEADRev = SVNGetRevision("mySVN","temp/","HEAD");
	 * 	//get the revision number previous to HEAD
	 * 	PrevRev = SVGGetRevision("mySVN","tmp/","PREVIOUS");
	 * 	//get the revision number previous to 1000
	 * 	PrevPrevRev = SVGGetRevision("mySVN","tmp/",1000);
	 * 	//get the revision number the item was created at
	 * 	CreateRev = SVNGetRevision("mySVN","tmp/","CREATE");
	 * 	//get the revision history for the item
	 * 	HistoryList = SVNGetRevision("mySVN","/tmp","HISTORY");
	 * </code>
	 * 
	 * @param _session
	 * @param _argStruct
	 *          name: String repository name to use
	 *          svnPath: String path in SVN to get information on
	 *          revision: String revision to get
	 * @return String what type of element exists at the _path (dir/file/none/unknown/ERROR)
	 * @throws cfmRunTimeException
	 *           SVN Repository name not supplied
	 *           Unable to connect to repository
	 * @since 2.0.0
	 */
	@Override
	public cfData execute( cfSession _session, cfArgStructData _argStruct ) throws cfmRunTimeException {

		String name = getNamedStringParam( _argStruct, "name", "" ).trim();
		String svnPath = getNamedStringParam( _argStruct, "svnPath", "" ).trim();
		String sRevision = getNamedStringParam( _argStruct, "revision", "head" ).trim().toLowerCase();

		// Validate arguments
		if ( name.isEmpty() ) {
			throwException( _session, "Please provide a SVN Repository" );
		}

		return execute( _session, name, svnPath, sRevision );
	}


	/**
	 * do the work of getting the requested revision
	 * 
	 * @param _session
	 * @param name
	 * @param svnPath
	 * @param sRevision
	 * @return the requested return
	 * @throws cfmRunTimeException
	 */
	public cfData execute( cfSession _session, String name, String svnPath, String sRevision ) throws cfmRunTimeException {

		cfNumberData targetRev = new cfNumberData( -1 );
		cfStringData historyList = new cfStringData( "" );

		// Validate arguments
		if ( name.isEmpty() ) {
			throwException( _session, "Please provide a SVN Repository" );
		}

		// make sure we can get the repo
		try ( SVNRepo repo = new SVNRepo( name ) ) {
			// figure out what we are looking form
			if ( sRevision.equalsIgnoreCase( "head" ) || sRevision.equalsIgnoreCase( "previous" ) ) {
				// HEAD or PREVIOUS
				// get the HEAD
				targetRev.set( getRevisionHead( repo, svnPath ).getRevision() );

				// see if we were actually looking for the revision previous to HEAD
				if ( sRevision.equalsIgnoreCase( "previous" ) ) {
					// Actually looking for PREVIOUS, but needed the HEAD to get it
					targetRev = (cfNumberData) execute( _session, name, svnPath, targetRev.toString() );
				}

			} else if ( sRevision.equalsIgnoreCase( "create" ) ) {
				// need the revision the item was created in
				targetRev.set( getRevisionCreate( repo, svnPath ).getRevision() );

			} else if ( sRevision.equalsIgnoreCase( "history" ) ) {
				// need the complete revision history
				historyList.setString( getRevisionHistory( repo, svnPath ) );

			} else {
				// might have a number, and we need the revision prior to it
				// get the revision number
				Integer rev = Integer.parseInt( sRevision );

				// make sure the number looks the same the string we got
				if ( rev.toString().equals( sRevision ) ) {
					// get the revision before the requested revision
					targetRev.set( getRevisionPrevious( repo, svnPath, rev.intValue() ).getRevision() );
				}

				// if the revision previous to the request revision is the requested revsion
				if ( rev.intValue() == targetRev.getInt() ) {
					// there is no revision prior to the CREATE revision, return 0
					targetRev.set( 0 );
				}
			}

			// determine which variable to return
			if ( sRevision.equalsIgnoreCase( "history" ) ) {
				// this is a list of numbers
				return historyList;
			} else {
				// this is a number
				return targetRev;
			}


		} catch ( SVNException e ) {
			// Something went wrong in the SVN layer
			throwException( _session, e.getMessage() );
		} catch ( NumberFormatException e ) {
			// didn't have a valid number
			throwException( _session, "Invalid value for sRevision.  Exptected HEAD,PREVIOUS,CREATE,HISTORY, or a Number.  Received  '" + sRevision + "'" );
		} catch ( NullPointerException e ) {
			// didn't find a revision
			throwException( _session, "Unable to find the requested revision for item " + svnPath + ".  Check the path." );
		}


		return new cfStringData( "ERROR" );

	}


	/**
	 * get the revision HEAD
	 * 
	 * @param _repo
	 * @param _svnPath
	 * @return
	 * @throws SVNException
	 */
	private SVNDirEntry getRevisionHead( SVNRepo _repo, String _svnPath ) throws SVNException {
		return getRevision( _repo.getRepository(), _svnPath, SVNRepo.HEAD );
	}


	/**
	 * Get a revision previous to the requested revision
	 * 
	 * @param _repo
	 * @param _svnPath
	 * @param _rev
	 * @return
	 * @throws SVNException
	 */
	private SVNDirEntry getRevisionPrevious( SVNRepo _repo, String _svnPath, int _rev ) throws SVNException {
		SVNRepository svn = _repo.getRepository();
		int prevRevision = _rev;
		final int nRev = _rev;
		final Collection<SVNLogEntry> result = new LinkedList<SVNLogEntry>();
		final SVNLogClient logC = _repo.getLogClient();
		final String paths[] = { "" };
		final SVNURL SVNFile = svn.getLocation().appendPath( _svnPath, false );
		SVNRevision SVNrev = SVNRevision.create( nRev );

		// define a log entry handler
		final ISVNLogEntryHandler handler = new ISVNLogEntryHandler() {

			@Override
			public void handleLogEntry( SVNLogEntry logEntry ) throws SVNException {
				// only grab the log if the revision is less than our specified revision
				if ( logEntry.getRevision() < nRev ) {
					result.add( logEntry );
				}
			}
		};

		// get the logs
		logC.doLog( SVNFile, paths, SVNrev, SVNrev, SVNRevision.create( 0 ), true, true, 2, handler );

		// prepare to loop over the logs
		Iterator<SVNLogEntry> it = result.iterator();

		// loop over the logs
		while ( it.hasNext() ) {
			SVNLogEntry f = it.next();
			// grab the log revision
			prevRevision = (int) f.getRevision();
		}

		// get and return the revision
		return getRevision( svn, _svnPath, prevRevision );
	}


	/**
	 * Get the revision of creation for the item
	 * 
	 * @param _repo
	 * @param _svnPath
	 * @return
	 * @throws SVNException
	 */
	private SVNDirEntry getRevisionCreate( SVNRepo _repo, String _svnPath ) throws SVNException {
		SVNRepository _svn = _repo.getRepository();
		// get the history for the file
		Collection<SVNLogEntry> history = getHistory( _repo, _svnPath );
		// prepare to loop of the logs
		Iterator<SVNLogEntry> revs = history.iterator();
		long createRev = 0;
		String checkPath = _svnPath;

		// loop over the logs
		while ( revs.hasNext() ) {
			SVNLogEntry f = revs.next();
			// get the logs paths
			if ( createRev == 0 || createRev > f.getRevision() ) {
				createRev = f.getRevision();
			}
		}

		// get and return the revision
		return getRevision( _svn, checkPath, createRev );
	}


	/**
	 * Get the revision history for an item
	 * 
	 * @param _repo
	 * @param _svnPath
	 * @return a comma separated list of revisions
	 * @throws SVNException
	 */
	private String getRevisionHistory( SVNRepo _repo, String _svnPath ) throws SVNException {
		// get the history for the item
		Collection<SVNLogEntry> history = getHistory( _repo, _svnPath );

		// prepare to iterate
		Iterator<SVNLogEntry> revs = history.iterator();
		StringBuilder sHistory = new StringBuilder();

		// loop over all revisions
		while ( revs.hasNext() ) {
			SVNLogEntry f = revs.next();

			// append the revisions, and delimiter
			sHistory.append( f.getRevision() ).append( "," );
		}

		// remove trailing delimiter
		if ( sHistory.length() > 0 ) {
			sHistory.deleteCharAt( sHistory.length() - 1 );
		}

		// return a comma separated list of revisions
		return sHistory.toString();
	}


	/**
	 * get the SVN history for an item
	 * 
	 * @param _repo
	 * @param _svnPath
	 * @return the SVN history
	 * @throws SVNException
	 */
	private Collection<SVNLogEntry> getHistory( SVNRepo _repo, String _svnPath ) throws SVNException {
		SVNRepository svn = _repo.getRepository();
		final Collection<SVNLogEntry> result = new LinkedList<SVNLogEntry>();
		final SVNLogClient logC = _repo.getLogClient();
		final String paths[] = { "" };
		final SVNURL SVNFile = svn.getLocation().appendPath( _svnPath, false );
		int cnt = 0;

		// set up the log handler
		final ISVNLogEntryHandler handler = new ISVNLogEntryHandler() {

			@Override
			public void handleLogEntry( SVNLogEntry logEntry ) throws SVNException {
				// grab all logs
				result.add( logEntry );
			}
		};

		// do not follow item copies
		// do not return paths for the log
		logC.doLog( SVNFile, paths, SVNRevision.HEAD, SVNRevision.HEAD, SVNRevision.create( 0 ), true, false, 0, handler );

		// return our logs
		return result;
	}


	/**
	 * Get the revision
	 * 
	 * @param _svn
	 * @param _svnPath
	 * @param createRev
	 * @return
	 * @throws SVNException
	 */
	private SVNDirEntry getRevision( SVNRepository _svn, String _svnPath, long createRev ) throws SVNException {
		// return the revision information
		return _svn.info( _svnPath, createRev );
	}


}
