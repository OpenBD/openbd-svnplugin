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


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import net.aw20.openbd.plugins.svn.LogFilter;
import net.aw20.openbd.plugins.svn.SVNRepo;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.io.SVNRepository;

import com.naryx.tagfusion.cfm.engine.cfArgStructData;
import com.naryx.tagfusion.cfm.engine.cfArrayData;
import com.naryx.tagfusion.cfm.engine.cfData;
import com.naryx.tagfusion.cfm.engine.cfDateData;
import com.naryx.tagfusion.cfm.engine.cfSession;
import com.naryx.tagfusion.cfm.engine.cfStructData;
import com.naryx.tagfusion.cfm.engine.cfmRunTimeException;
import com.naryx.tagfusion.expression.function.functionBase;


/**
 * OpenBD class for function: SVNLogView()
 * 
 * <code>logs = SVNLogView("mySVN",100,200,"","","","","")</code>
 * 
 * @author AW20 Ltd
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 1.0.0
 * @see http://svnkit.com
 */
public class SVNLogView extends functionBase {

	private static final long serialVersionUID = 1L;


	public SVNLogView() {
		min = 1;
		max = 8;
		setNamedParams( new String[] {
				"name",
				"startRevision",
				"endRevision",
				"filter",
				"fileFilter",
				"startDateFilter",
				"endDateFilter",
				"patterndate" } );
	}


	@Override
	public String[] getParamInfo() {
		return new String[] {
				"Name of the SVN Repository to use",
				"revision to inclusively begin with, default to 0",
				"revision to inclusively end with, default to HEAD",
				"filter text to search for in the commit message",
				"filter to search for files",
				"filter to search for a begin date range",
				"filter to search for a end date range",
				"Date format pattern" };
	}


	@Override
	public Map<String, String> getInfo() {
		return makeInfo(
				"svn",
				"Returns the latest revision number in the repository",
				ReturnType.NUMERIC );
	}


	/**
	 * Used to get the log records bewteen 2 revision numbers
	 * 
	 * <code>logs = SVNLogView("mySVN",100,200,"","","","","")</code>
	 * 
	 * @param _session
	 * @param _argStruct
	 *          name: String repository name to use
	 *          startRevision: Int revision to begin log capture (inclusive)
	 *          endRevision: Int revision to end log capture (inclusive)
	 *          filter: String filter to wildcard search on
	 *          fileFilter: Path filter to use
	 *          startDateFilter: date start filter to use
	 *          endDateFilter: date end filter to use
	 *          patterndate: String date format to use
	 * @return array of structs that contain log information
	 * @throws cfmRunTimeException
	 *           SVN Repository name not supplied
	 *           Unable to connect to repository
	 *           Malformed start-end revisions
	 *           Error retrieving logs
	 * @since 1.0.0
	 */
	@Override
	public cfData execute( cfSession _session, cfArgStructData _argStruct ) throws cfmRunTimeException {

		// Get all arguments
		String name = getNamedStringParam( _argStruct, "name", "" ).trim();
		int startRevision = getNamedIntParam( _argStruct, "startRevision", SVNRepo.BEGIN );
		int endRevision = getNamedIntParam( _argStruct, "endRevision", SVNRepo.HEAD );
		String filter = getNamedStringParam( _argStruct, "filter", "" );
		String fileFilter = getNamedStringParam( _argStruct, "fileFilter", "" );
		String startDateFilter = getNamedStringParam( _argStruct, "startDateFilter", "" );
		String endDateFilter = getNamedStringParam( _argStruct, "endDateFilter", "" );


		SVNRepository svn = null;
		Collection<SVNLogEntry> logEntries = null;

		// Validate arguments

		// make sure startRevision < endRevision, unless endRevision is HEAD
		if ( endRevision != SVNRepo.HEAD && startRevision > endRevision ) {
			throwException( _session, "Malformed start-end revisions [" + startRevision + "," + endRevision + "]" );
		}

		if ( name.isEmpty() ) {
			throwException( _session, "Please provide a SVN Repository" );
		}

		try ( SVNRepo repo = new SVNRepo( name ) ) {
			svn = repo.getRepository();

			LogFilter logFilter = getLogFilter( filter, fileFilter, startDateFilter, endDateFilter );

			try {
				if ( logFilter == null ) {
					logEntries = getLogEntries( svn, startRevision, endRevision );
				} else {
					logEntries = getLogEntries( svn, logFilter, startRevision, endRevision );
				}
			} catch ( SVNException e ) {
				throwException( _session, "Error retrieving logs: " + e.getMessage() );
			}

			repo.close();

			return getLogsArray( logEntries );

		} catch ( SVNException e ) {
			throwException( _session, "Unable to connect to " + name + ". Please provide a Valid SVN Repository" );
		}

		return new cfStructData();

	}


	/**
	 * @param _svn
	 *          SVNRepository to use
	 * @param _startRevision
	 *          Int revision to begin log capture (inclusive)
	 * @param _endRevision
	 *          Int revision to end log capture (inclusive)
	 * @since 1.0.0
	 */
	public Collection<SVNLogEntry> getLogEntries( SVNRepository _svn, long _startRevision, long _endRevision ) throws SVNException {

		Collection<SVNLogEntry> logEntries = null;
		logEntries = _svn.log( new String[] { "" },
				null,
				_startRevision,
				_endRevision,
				true,
				true );

		return logEntries;

	}


	/**
	 * overlaod of getLogEntries( SVNRepository, long, long )
	 * 
	 * @param _svn
	 *          SVNRepository to use
	 * @param _logFilter
	 *          message filter to use
	 * @param _startRevision
	 *          Int revision to begin log capture (inclusive)
	 * @param _endRevision
	 *          Int revision to end log capture (inclusive)
	 * @since 1.0.0
	 */
	public Collection<SVNLogEntry> getLogEntries( SVNRepository _svn, LogFilter _logFilter, long _startRevision, long _endRevision ) throws SVNException {

		final Collection<SVNLogEntry> result = new LinkedList<SVNLogEntry>();
		final LogFilter flogFilter = _logFilter;
		_svn.log( new String[] { "" }, _startRevision, _endRevision, true, true, new ISVNLogEntryHandler() {

			@Override
			public void handleLogEntry( SVNLogEntry _logEntry ) {
				if ( !flogFilter.filter( _logEntry ) )
					result.add( _logEntry );
			}

		} );
		return result;

	}


	/**
	 * overlaod of getLogEntries( SVNRepository, long, long )
	 * overload of getLogEntries( SVNRepository, LogFilter, long, long )
	 * 
	 * @param _logFilter
	 *          message filter to use
	 * @param _file
	 *          Path filter to use
	 * @param _begin
	 *          date start filter to use
	 * @param _end
	 *          date end filter to use
	 * @since 1.0.0
	 */
	private LogFilter getLogFilter( final String _filter, final String _file, final String _begin, final String _end ) throws cfmRunTimeException {

		LogFilter logFilter = null;
		if ( isNotEmpty( _filter ) || isNotEmpty( _file ) || isNotEmpty( _begin ) || isNotEmpty( _end ) ) {
			logFilter = new LogFilter() {

				/**
				 * @description filters are based on exclusion (true to exclude)
				 */

				@Override
				public boolean filter( SVNLogEntry _logEntry ) {
					return filterMessage( _logEntry ) || filterFile( _logEntry ) || filterBeginDate( _logEntry ) || filterEndDate( _logEntry );
				}


				/**
				 * @description message filter
				 * @param _logEntry
				 * @return boolean exclusion flag (true: exclude, false: include)
				 */
				public boolean filterMessage( SVNLogEntry _logEntry ) {
					if ( _filter != null && !_filter.toString().isEmpty() ) {
						return _logEntry.getMessage() != null && _logEntry.getMessage().indexOf( _filter ) == -1;
					}
					return false;
				}


				/**
				 * @descrition File filter
				 * @param _logEntry
				 * @return boolean exclusion flag (true: exclude, false: include)
				 */
				public boolean filterFile( SVNLogEntry _logEntry ) {
					if ( _file != null && !_file.toString().isEmpty() ) {
						boolean ret = true;
						Map<String, SVNLogEntryPath> paths = _logEntry.getChangedPaths();
						Iterator<Entry<String, SVNLogEntryPath>> it = paths.entrySet().iterator();
						String filePattern = _file.replace( ".", "\\." ).replace( "*", ".*" );
						while ( ret && it.hasNext() ) {
							Map.Entry<String, SVNLogEntryPath> f = it.next();
							ret = ret && !f.getKey().matches( filePattern );
						}
						return ret;
					}
					return false;
				}


				/**
				 * @description Begin date filter
				 * @param _logEntry
				 * @return boolean exclusion flag (true: exclude, false: include)
				 */
				public boolean filterBeginDate( SVNLogEntry _logEntry ) {
					if ( _begin != null && !_begin.toString().isEmpty() ) {
						Date dt = null;
						try {
							dt = new SimpleDateFormat( "yyyy/M/d" ).parse( _begin );
						} catch ( ParseException e ) {
							return true;
						}
						return _logEntry.getDate().before( dt );
					}
					return false;
				}


				/**
				 * @description End date filter
				 * @param _logEntry
				 * @return boolean exclusion flag (true: exclude, false: include)
				 */
				public boolean filterEndDate( SVNLogEntry _logEntry ) {
					if ( _end != null && !_end.toString().isEmpty() ) {
						Date dt = null;
						try {
							dt = new SimpleDateFormat( "yyyy/M/d kk:mm:ss.S" ).parse( _end + " 23:59:59.9" );
						} catch ( ParseException e ) {
							return true;
						}
						return _logEntry.getDate().after( dt );
					}
					return false;
				}


			};

		}

		return logFilter;

	}


	/**
	 * Used to determine if a variable is null, or empty (string representation)
	 * 
	 * @param _value
	 *          variable to be checked
	 * @return boolean
	 * @since 1.0.0
	 */
	private boolean isNotEmpty( Object _value ) {
		return _value != null && !_value.toString().isEmpty();
	}


	/**
	 * @param _logEntries
	 *          Collection of log entries
	 * @return array of logs
	 * @since 1.0.0
	 */
	private cfArrayData getLogsArray( Collection<SVNLogEntry> _logEntries ) throws cfmRunTimeException {

		cfArrayData arr = cfArrayData.createArray( 1 );
		SVNLogEntry logEntry = null;
		cfStructData st = null;

		for ( Iterator<SVNLogEntry> entries = _logEntries.iterator(); entries.hasNext(); ) {
			logEntry = entries.next();

			st = new cfStructData();
			st.put( "revision", logEntry.getRevision() );
			st.put( "author", logEntry.getAuthor() );
			st.put( "date", new cfDateData( logEntry.getDate() ) );
			st.put( "logmessage", logEntry.getMessage() );

			Map<String, SVNLogEntryPath> changePaths = logEntry.getChangedPaths();
			if ( changePaths.size() > 0 ) {
				cfArrayData cp = cfArrayData.createArray( 1 );

				for ( Iterator<SVNLogEntryPath> file = changePaths.values().iterator(); file.hasNext(); ) {
					SVNLogEntryPath slep = file.next();
					cfStructData cpp = new cfStructData();
					cpp.put( "path", slep.getPath() );
					cpp.put( "type", slep.getType() );

					cp.addElement( cpp );
				}

				st.put( "changed", cp );
			}

			arr.addElement( st );
		}
		return arr;
	}


}
