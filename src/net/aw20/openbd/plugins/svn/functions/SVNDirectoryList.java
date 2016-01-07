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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.aw20.openbd.plugins.svn.SVNRepo;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;

import com.nary.util.FastMap;
import com.nary.util.string;
import com.naryx.tagfusion.cfm.engine.cfArgStructData;
import com.naryx.tagfusion.cfm.engine.cfArrayData;
import com.naryx.tagfusion.cfm.engine.cfBooleanData;
import com.naryx.tagfusion.cfm.engine.cfData;
import com.naryx.tagfusion.cfm.engine.cfQueryResultData;
import com.naryx.tagfusion.cfm.engine.cfSession;
import com.naryx.tagfusion.cfm.engine.cfStringData;
import com.naryx.tagfusion.cfm.engine.cfmRunTimeException;
import com.naryx.tagfusion.cfm.engine.dataNotSupportedException;
import com.naryx.tagfusion.expression.function.file.dirComparator;


/**
 * OpenBD class for function: SVNDirectoryList()
 * 
 * Called from OpenBD <code>type = SVNDirectoryList("mySVN","temp\",-1)</code>
 * 
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 2.0.0
 * @see http://svnkit.com
 */
public class SVNDirectoryList extends SVNLogView {

	private static final long serialVersionUID = 1L;


	public SVNDirectoryList() {
		min = 2;
		max = 7;
		setNamedParams( new String[] {
				"name",
				"svnPath",
				"recursive",
				"listInfo",
				"filter",
				"sort",
				"revision" } );

	}


	@Override
	public String[] getParamInfo() {
		return new String[] {
				"Name of the SVN Repository to use",
				"SVN Path to get info on",
				"whether it should list all the sub-directories [optional]",
				"Values: name, path, query. If 'name' returns a array of file names. 'path' full path of each one in an array, 'query' returns a query object [optional]",
				"File extension to filter on [optional]",
				"A comma separated list the query columns to sort on and in which direction e.g. 'name asc, size desc' [optional]",
				"The revision to use, default HEAD [optional]" };
	}


	@Override
	public Map<String, String> getInfo() {
		return makeInfo(
				"svn",
				"For the given repository directory get the file contents",
				ReturnType.ARRAY );
	}


	/**
	 * Used to get the SVN element type
	 * 
	 * Called from OpenBD <code>type = SVNGetStatus("mySVN","temp\",true,"query","*.txt","name asc", -1)</code>
	 * 
	 * @param _session
	 * @param _argStruct
	 *          name: String repository name to use
	 *          svnPath: String path in SVN to get information on
	 *          recursive: boolean flag to indicate recursive list
	 *          listinfo: String to indicate what is returned
	 *          filter: String file name filter
	 *          sort: String sort column name and directions
	 *          revision: Int revision to get
	 * @return Array/Query containing directory listing details
	 * @throws cfmRunTimeException
	 *           SVN Repository name not supplied
	 *           Unable to connect to repository
	 *           Malformed filter
	 * @since 2.0.0
	 */
	@Override
	public cfData execute( cfSession _session, cfArgStructData _argStruct ) throws cfmRunTimeException {

		String name = getNamedStringParam( _argStruct, "name", "" ).trim();
		String svnPath = getNamedStringParam( _argStruct, "svnPath", "" ).trim();
		boolean recursive = getNamedParam( _argStruct, "recursive", cfBooleanData.FALSE ).getBoolean();
		String listInfo = getNamedStringParam( _argStruct, "listInfo", "path" ).trim().toLowerCase();
		String filter = getNamedStringParam( _argStruct, "filter", "" ).trim();
		String sort = getNamedStringParam( _argStruct, "sort", "name asc" ).trim();
		int revision = getNamedIntParam( _argStruct, "revision", SVNRepo.HEAD );

		SVNRepository svn = null;

		// Validate arguments
		if ( name.isEmpty() ) {
			throwException( _session, "Please provide a SVN Repository" );
		}

		if ( svnPath.isEmpty() ) {
			throwException( _session, "Please provide a valid svnPath" );
		} else if ( !svnPath.endsWith( "/" ) ) {
			svnPath += "/";
		}


		try ( SVNRepo repo = new SVNRepo( name ) ) {
			svn = repo.getRepository();

			List<Map<String, cfData>> resultVector = new ArrayList<Map<String, cfData>>();
			Pattern pattern = null;

			// set up the pattern
			pattern = getFilterPattern( filter );

			// Call to do the work
			list( resultVector, svn, svnPath, "", recursive, pattern, revision );

			// sort the results
			if ( sort != null && sort.trim().length() != 0 ) {
				sortResults( resultVector, sort );
			}

			// process the results for the return type
			return processReturn( resultVector, listInfo );

		} catch ( MalformedPatternException e ) {
			throwException( _session, "Malformed filter (" + filter + ")" );
		} catch ( SVNException e ) {
			throwException( _session, "Unable to connect to " + name + ". Please provide a Valid SVN Repository" );
		}

		// shouldn't be here
		return cfArrayData.createArray( 1 );

	}


	/**
	 * Function to actually do the work of iterating over the SVN directory structure to collection files/folders
	 * 
	 * @param resultVector
	 *          file/folder collection
	 * @param _svn
	 *          SVN repository we are working with
	 * @param _svnBasePath
	 *          base SVN path that we are querying
	 * @param _svnPath
	 *          additions to the base SVN path that we are querying for recursive use
	 * @param _recursive
	 *          flag to control recursive listings
	 * @param _pattern
	 *          file name pattern to match
	 * @param _revision
	 *          revision number to get a directory listing of
	 * @throws SVNException
	 */
	private void list( List<Map<String, cfData>> resultVector, SVNRepository _svn, String _svnBasePath, String _svnPath, boolean _recursive, Pattern _pattern, int _revision ) throws SVNException {

		Collection<SVNDirEntry> entries = _svn.getDir( _svnBasePath + _svnPath, _revision, null, (Collection) null );
		Perl5Matcher matcher = ( _pattern == null ? null : new Perl5Matcher() );

		for ( SVNDirEntry entry : entries ) {

			if ( _pattern == null || matcher.matches( entry.getName(), _pattern ) ) {
				Map<String, cfData> entity = new FastMap<>();
				entity.put( "name", new cfStringData( _svnPath + entry.getName() ) );
				entity.put( "size", new cfStringData( ( new Long( entry.getSize() ).toString() ) ) );
				entity.put( "directory", new cfStringData( ( _svnBasePath.equals( "" ) ? "/" : _svnBasePath ) ) );
				entity.put( "type", new cfStringData( entry.getKind().toString() ) );
				entity.put( "datelastmodified", new cfStringData( entry.getDate().toString() ) );
				entity.put( "revision", new cfStringData( ( new Long( entry.getRevision() ) ).toString() ) );
				entity.put( "author", new cfStringData( entry.getAuthor() ) );
				resultVector.add( entity );
			}

			if ( _recursive && entry.getKind() == SVNNodeKind.DIR ) {
				list( resultVector, _svn, _svnBasePath, _svnPath + entry.getName() + "/", _recursive, _pattern, _revision );
			}

		}

	}


	/**
	 * Process the return into the appropriate CF return type (array/query)
	 * 
	 * @param resultVector
	 *          the directory listing content
	 * @param listInfo
	 *          the result type controller
	 * @return cfData type appropriate for what is requested
	 * @throws dataNotSupportedException
	 * @throws cfmRunTimeException
	 */
	private cfData processReturn( List<Map<String, cfData>> resultVector, String listInfo ) throws dataNotSupportedException, cfmRunTimeException {
		if ( listInfo.equals( "query" ) ) {
			cfQueryResultData queryResult = new cfQueryResultData( new String[] { "name", "datelastmodified", "directory", "size", "type", "revision", "author" }, "SVNDIRECTORY" );
			queryResult.populateQuery( resultVector );
			return queryResult;
		} else {
			cfArrayData arr = cfArrayData.createArray( 1 );

			Iterator<Map<String, cfData>> it = resultVector.iterator();
			while ( it.hasNext() ) {
				Map<String, cfData> m = it.next();

				if ( listInfo.equals( "path" ) || listInfo.equals( "all" ) ) {
					arr.addElement( new cfStringData( m.get( "directory" ).getString() + m.get( "name" ).getString() ) );
				} else {
					arr.addElement( m.get( "name" ) );
				}
			}
			return arr;
		}
	}


	/**
	 * Escape specific characters on the filter
	 * 
	 * @param _filter
	 *          filter to be escaped
	 * @return the escaped filter
	 */
	private static String escapeFilter( String _filter ) {
		String filter = _filter;
		return filter.replace( "?", "\\?" )
				.replace( "+", "+\\" )
				.replace( ".", ".\\" )
				.replace( "$", "$\\" )
				.replace( "^", "^\\" )
				.replace( "\\?", "." )
				.replace( "(", "\\(" )
				.replace( ")", "\\)" )
				.replace( "[", "\\[" )
				.replace( "]", "\\]" )
				.replace( "{", "\\{" )
				.replace( "}", "\\}" )
				.replace( "*", ".*" );
	}


	/**
	 * Sort the results according to a field name, and directions
	 * 
	 * @param results
	 *          the directory listing
	 * @param sortString
	 *          field name, and direction to sort
	 */
	private static void sortResults( List<Map<String, cfData>> results, String sortString ) {

		// List<String> tokenList = string.split( sortString.toLowerCase(), "," );
		String[] tokens = sortString.toLowerCase().split( "," );


		Comparator<Map<String, cfData>> comparator = null;
		for ( int i = tokens.length - 1; i >= 0; i-- ) {
			String subSort = tokens[i];
			boolean bAscending = true;
			String sortCol;

			// --[ Find out the order
			int c1 = subSort.indexOf( " " );
			if ( c1 == -1 ) {
				sortCol = subSort;
			} else {
				sortCol = subSort.substring( 0, c1 ).trim();
				String orderby = subSort.substring( c1 + 1 );
				if ( !orderby.equals( "asc" ) ) {
					bAscending = false;
				}
			}

			comparator = new dirComparator( sortCol, bAscending, comparator );
		}

		// -- Perform the sort
		Collections.sort( results, comparator );
	}


	/**
	 * Set up the file filter, if needed
	 * 
	 * @param filter
	 *          filter to run on file names
	 * @return Pattern
	 * @throws MalformedPatternException
	 */
	private Pattern getFilterPattern( String filter ) throws MalformedPatternException {
		if ( !filter.isEmpty() ) {
			Perl5Compiler perl = new Perl5Compiler();
			return perl.compile( escapeFilter( filter ), Perl5Compiler.CASE_INSENSITIVE_MASK );
		}
		return null;
	}


}
