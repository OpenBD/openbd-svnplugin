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


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.aw20.openbd.plugins.svn.DiffHandler;
import net.aw20.openbd.plugins.svn.SVNRepo;
import net.aw20.openbd.plugins.svn.processor.Diff;
import net.aw20.openbd.plugins.svn.processor.DiffLine;
import net.aw20.openbd.plugins.svn.processor.DiffLines;
import net.aw20.openbd.plugins.svn.processor.DiffRevision;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

import com.nary.io.StreamUtils;
import com.naryx.tagfusion.cfm.engine.cfArgStructData;
import com.naryx.tagfusion.cfm.engine.cfArrayData;
import com.naryx.tagfusion.cfm.engine.cfBooleanData;
import com.naryx.tagfusion.cfm.engine.cfData;
import com.naryx.tagfusion.cfm.engine.cfEngine;
import com.naryx.tagfusion.cfm.engine.cfNumberData;
import com.naryx.tagfusion.cfm.engine.cfSession;
import com.naryx.tagfusion.cfm.engine.cfStringData;
import com.naryx.tagfusion.cfm.engine.cfStructData;
import com.naryx.tagfusion.cfm.engine.cfmRunTimeException;
import com.naryx.tagfusion.cfm.engine.dataNotSupportedException;
import com.naryx.tagfusion.expression.function.functionBase;


/**
 * OpenBD class for function: SVNDirectoryList()
 * 
 * Called from OpenBD <code>type = SVNDiff("mySVN","temp\",-1)</code>
 * 
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 2.0.0
 * @see http://svnkit.com
 */
public class SVNDiff extends functionBase {

	private static final long serialVersionUID = 1L;
	public static final String PREFIXADD = "+";
	public static final String PREFIXREMOVE = "-";
	// FS - File Separator
	public static final String DELIMITER = Character.toString( (char) 28 );
	// LF - Line Feed
	public static final String LF = Character.toString( (char) 10 );
	public static final String INFOSTATUS = "status";
	public static final String INFOSTRING = "string";
	public static final String INFOHTML = "html";
	public static final String INFODATA = "data";
	public static final String INFOOVERLAY = "overlay";
	public static final String RETDIFF = "diff";
	public static final String RETREVS = "revisions";

	private static final String htmlStyle = "<style type='text/css'><!--.patch pre{-moz-tab-size: 4;-o-tab-size: 4;tab-size: 4}.patch{width: 100%}.patch h4{font-family: verdana,arial,helvetica,sans-serif;font-size: 10pt;padding: 8px;background: #369;color: #fff;margin: 0}.patch .propset h4, #patch .binary h4{margin: 0}.patch pre{padding: 0;line-height: 1.2em;margin: 0}.patch .diff{width: 100%;background: #eee;padding: 0 0 10px 0;overflow: auto}.patch .propset .diff, #patch .binary .diff{padding: 10px 0}.patch span{display: block;padding: 0 10px}.patch .moadfile, #patch .addfile, #patch .delfile, #patch .propset, .patch .binary, #patch .copfile{border: 1px solid #ccc;margin: 10px 0}.patch ins{background: #afa;text-decoration: none;display: block;padding: 0 10px}.patch del{background: #faa;text-decoration: line-through;display: block;padding: 0 10px}.patch .lines, .info{color: #888;background: #fff}--></style>";

	private cfSession session;


	/**
	 * define the parameters for this CFML function
	 */
	public SVNDiff() {
		min = 2;
		max = 7;
		setNamedParams( new String[] {
				"name",
				"svnPath",
				"listInfo",
				"revisionNewest",
				"revisionOldest",
				"splitRev",
				"splitStartRevision",
				"charset" } );

	}


	/**
	 * define the helper text for the parameters
	 */
	@Override
	public String[] getParamInfo() {
		return new String[] {
				"Name of the SVN Repository to use",
				"SVN Path to get info on",
				"What information to return [optional]",
				"The starting revision to use, default CURRENT [optional]",
				"The ending revision to use, default CURRENT [optional]",
				"Split out each revision between revisionNewest and revisionOldest",
				"If Split, what revision to start the split from",
				"Character set of the file [optional]" };
	}


	/**
	 * Define helper test about the function
	 */
	@Override
	public Map<String, String> getInfo() {
		return makeInfo(
				"svn",
				"Get the diff of a file between revisions",
				ReturnType.STRUCTURE );
	}


	/**
	 * Used to get the SVN element type
	 * 
	 * Called from OpenBD <code>type = SVNDiff("mySVN","temp/index.cfc",-1)</code>
	 * 
	 * @param _session
	 * @param _argStruct
	 *          name: String repository name to use
	 *          svnPath: String path in SVN to get information on
	 *          listInfo: What to return (string:status|string|html|data|overlay)
	 *          revisionNewest: Int revision to start at (CURRENT)
	 *          revisionOldest: Int revision to end with (CURRENT-1)
	 *          splitRev: split out revisions (false)
	 *          charset: character encoding
	 * @return Diff string
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
		String listInfo = getNamedStringParam( _argStruct, "listInfo", "string" ).trim();
		int revisionNewest = getNamedIntParam( _argStruct, "revisionNewest", SVNRepo.HEAD );
		int revisionOldest = getNamedIntParam( _argStruct, "revisionOldest", revisionNewest );
		boolean splitRev = getNamedBooleanParam( _argStruct, "splitRev", false );
		int revisionSplitStart = getNamedIntParam( _argStruct, "splitStartRevision", SVNRepo.HEAD );
		String charSet = getNamedStringParam( _argStruct, "charset", "" );

		List<Integer> aRevs = new ArrayList<Integer>();

		session = _session;

		// Validate arguments
		if ( name.isEmpty() ) {
			throwException( session, "Please provide a SVN Repository" );
		}

		if ( svnPath.isEmpty() ) {
			throwException( session, "Please provide a valid svnPath" );
		}

		if ( revisionNewest < revisionOldest && revisionNewest != -1 ) {
			throwException( session, "Revision Newest should be a higher revision than revisionOldest" );
		}

		revisionNewest = processRevNewest( name, svnPath, revisionNewest );
		revisionOldest = processRevOldest( name, svnPath, revisionNewest, revisionOldest );
		revisionSplitStart = processRevSplit( name, svnPath, splitRev, revisionSplitStart );

		aRevs = GetRevisions( name, svnPath, revisionNewest, revisionOldest, splitRev, revisionSplitStart );

		return execute( name, svnPath, listInfo, aRevs, charSet );

	}


	/**
	 * get the newest revision number of the item if needed
	 * 
	 * @param _name
	 * @param _svnPath
	 * @param _revisionNewest
	 * @return if the -1 HEAD flag was passed in, get the head revisions for the path
	 * @throws cfmRunTimeException
	 */
	private int processRevNewest( String _name, String _svnPath, int _revisionNewest ) throws cfmRunTimeException {
		SVNGetRevision svnGR = new SVNGetRevision();
		if ( _revisionNewest == SVNRepo.HEAD ) {
			return ( (cfNumberData) svnGR.execute( session, _name, _svnPath, "head" ) ).getInt();
		}

		return _revisionNewest;
	}


	/**
	 * get the previous revision number if neded
	 * 
	 * @param _name
	 * @param _svnPath
	 * @param _revisionNewest
	 * @param _revisionOldest
	 * @return if the revision equals to the newest revision (after HEAD translation), get the revision previous
	 * @throws cfmRunTimeException
	 */
	private int processRevOldest( String _name, String _svnPath, int _revisionNewest, int _revisionOldest ) throws cfmRunTimeException {
		int revision = _revisionOldest;
		SVNGetRevision svnGR = new SVNGetRevision();

		if ( _revisionOldest == SVNRepo.HEAD ) {
			revision = ( (cfNumberData) svnGR.execute( session, _name, _svnPath, "head" ) ).getInt();
		}

		if ( _revisionNewest == revision ) {
			revision = ( (cfNumberData) svnGR.execute( session, _name, _svnPath, Integer.toString( _revisionOldest ) ) ).getInt();
		}

		return revision;
	}


	/**
	 * get the revision to split until
	 * 
	 * @param _name
	 * @param _svnPath
	 * @param _splitRev
	 * @param _revisionSplitStart
	 * @return if the -1 HEAD flag was passed in, get the head revisions for the path
	 * @throws cfmRunTimeException
	 */
	private int processRevSplit( String _name, String _svnPath, boolean _splitRev, int _revisionSplitStart ) throws cfmRunTimeException {
		SVNGetRevision svnGR = new SVNGetRevision();
		if ( _splitRev && _revisionSplitStart == SVNRepo.HEAD ) {
			return ( (cfNumberData) svnGR.execute( session, _name, _svnPath, "head" ) ).getInt();
		}

		return _revisionSplitStart;
	}


	/**
	 * Do all the diff work
	 * 
	 * @param _name
	 * @param _svnPath
	 * @param _listInfo
	 * @param _revisionNewest
	 * @param _revisionOldest
	 * @param _splitRev
	 * @param _revisionSplitStart
	 * @return an appropriate cfData element
	 * @throws cfmRunTimeException
	 * @throws FileNotFoundException
	 */
	public cfData execute( String _name, String _svnPath, String _listInfo, List<Integer> _aRevs, String _charSet ) throws cfmRunTimeException {
		SVNRepository svn = null;
		cfData ret = null;
		Integer revisionNewest = _aRevs.get( 0 );
		Integer revisionOldest = _aRevs.get( _aRevs.size() - 1 );

		// Get the SVN repo object
		try ( SVNRepo repo = new SVNRepo( _name ) ) {
			// make sure we are working with a file
			if ( repo.getPathType( _svnPath, revisionNewest ).equals( SVNNodeKind.FILE.toString() ) ) {
				// get the repository
				svn = repo.getRepository();

				final SVNDiffClient diffClient = new SVNDiffClient( repo.getClientManager(), null );
				final SVNURL svnFile = svn.getLocation().appendPath( _svnPath, false );

				// get just the status string from SVN
				if ( _listInfo.equalsIgnoreCase( INFOSTATUS ) ) {
					ret = diffStatus( diffClient, svnFile, _aRevs );

				} else {
					// all other returns are based on doing something with the diff output from SVN

					// get the diff string from SVN
					String diff = diffString( diffClient, svnFile, _aRevs, _charSet );

					if ( _listInfo.equalsIgnoreCase( INFOSTRING ) ) {
						// this is a plain text string
						ret = new cfStringData( diff );

					} else if ( _listInfo.equalsIgnoreCase( INFOHTML ) ) {
						// this is a HTML formatted text string
						ret = new cfStringData( htmlParse( diff ) );

					} else if ( _listInfo.equalsIgnoreCase( INFOOVERLAY ) || _listInfo.equalsIgnoreCase( INFODATA ) ) {
						// data is the parsed view of the diff string
						// overlay uses the diff string to overlay the diff onto the file contents
						String content = "";

						if ( revisionOldest > 0 ) {
							content = readSVNFile( repo, _svnPath, _aRevs, _charSet );
						}

						// set up to parse the diff
						Diff diffP = new Diff( content, revisionOldest );

						// parse the diff
						DiffLines tmp = diffP.parseDiff( diff, _listInfo.equalsIgnoreCase( INFODATA ) );

						// set up the return
						ret = new cfStructData();

						// translate the java objects into cfData elements
						cfArrayData cfRevs = cfArrayData.createArray( _aRevs.size() );
						for ( int i = 0; i < _aRevs.size(); i++ ) {
							cfRevs.addElement( new cfNumberData( _aRevs.get( i ) ) );
						}

						// return the revisions
						ret.setData( RETREVS, cfRevs );
						// return the diff struct
						ret.setData( RETDIFF, diffLinesToCF( tmp ) );
					} else {
						throwException( session, "Expected STATUS,STRING,HTML,DATA,OVERLAY for listinfo. Received: '" + _listInfo + "'" );

					}

				}
			} else if ( repo.getPathType( _svnPath, revisionNewest ).equals( SVNNodeKind.DIR.toString() ) ) {
				throwException( session, "Expected a file. " + _svnPath + " is a directory." );
			} else if ( repo.getPathType( _svnPath, revisionNewest ).equals( SVNNodeKind.NONE.toString() ) ) {
				throwException( session, "Expected file to exist at revision newest.  " + _svnPath + " at " + revisionNewest + " does not exist." );
			}

		} catch ( SVNException e ) {
			throwException( session, e.getMessage() );
		} catch ( IllegalStateException e ) {
			throwException( session, e.getMessage() );
		} catch ( UnsupportedEncodingException e ) {
			throwException( session, "There was an error loading the DIFF.  " + e.getMessage() );
		}

		// return what ever result type we have
		return ret;

	}


	/**
	 * Interrogate the revisions, and create a list of all diffs to be calculated
	 * 
	 * @param _name
	 * @param _svnPath
	 * @param _revisionNewest
	 * @param _revisionOldest
	 * @param _splitRev
	 * @param _revisionSplitStart
	 * @return a list of all relevant revisions
	 * @throws dataNotSupportedException
	 * @throws cfmRunTimeException
	 */
	private List<Integer> GetRevisions( String _name, String _svnPath, int _revisionNewest, int _revisionOldest, boolean _splitRev, int _revisionSplitStart ) throws dataNotSupportedException, cfmRunTimeException {
		List<Integer> aRevs = new ArrayList<Integer>();

		// see if we a getting the revision-by-revision history of a file
		if ( _splitRev ) {
			SVNGetRevision svnGR = new SVNGetRevision();
			int revIdx = _revisionNewest;

			if ( _revisionNewest != _revisionSplitStart ) {
				aRevs.add( _revisionSplitStart );
			}

			// loop over revisions between our boundaries
			while ( revIdx >= _revisionOldest && revIdx > 0 ) {
				aRevs.add( revIdx );
				String sRev = Integer.toString( revIdx ).toString();
				revIdx = svnGR.execute( session, _name, _svnPath, sRev ).getInt();
			}

			// check if we are going all the way back to file creation
			if ( _revisionOldest == 0 ) {
				// be sure to append the 0 revision if we need to go back to the creation of the file
				aRevs.add( 0 );
			}

		} else {
			// just add the revisions we are looking at
			aRevs.add( _revisionNewest );
			aRevs.add( _revisionOldest );
		}

		return aRevs;

	}


	/**
	 * Download a file from SVN, read it, delete it, return its content
	 * 
	 * @param _repo
	 * @param _svnPath
	 * @param _aRevs
	 * @param _charSet
	 * @return the contents of the file
	 * @throws cfmRunTimeException
	 */
	private String readSVNFile( SVNRepo _repo, String _svnPath, List<Integer> _aRevs, String _charSet ) throws cfmRunTimeException {
		String fileContent = "";
		// set up a path
		String tmpPath = cfEngine.thisPlatform.getFileIO().getTempDirectory() + File.separator + UUID.randomUUID().toString().replace( "-", "" ) + ".tmp";

		// get the file
		SVNGetFile svnGetFile = new SVNGetFile();
		svnGetFile.execute( session, _repo, _svnPath, tmpPath, _aRevs.get( _aRevs.size() - 1 ), false );

		try {
			// prepare to read the file
			File tmpFile = new File( tmpPath );
			FileInputStream fis = new FileInputStream( tmpFile );

			if ( _charSet.isEmpty() ) {
				fileContent = StreamUtils.readToString( fis );
			} else {
				fileContent = StreamUtils.readToString( fis, _charSet );
			}

		} catch ( UnsupportedEncodingException e ) {
			throwException( session, "There was an encoding issue with the file, and was unable to read it." );
		} catch ( IOException e ) {
			throwException( session, "There was an error reading the file" );
		} finally {
			try {
				// clean up the temporary file
				Files.deleteIfExists( Paths.get( tmpPath ) );
			} catch ( IOException e ) {
				// oops, something went wrong, we should have been able to delete the file
				throwException( session, "unable to delete temporary file: " + tmpPath );
			}
		}

		return fileContent;
	}


	/**
	 * get the diff file status string
	 * 
	 * @param _diffClient
	 * @param _target
	 * @param _aRevs
	 * @return the diff status {kind,file,modification,url}
	 * @throws SVNException
	 */
	private static cfStructData diffStatus( SVNDiffClient _diffClient, SVNURL _target, List<Integer> _aRevs ) throws SVNException {
		cfStructData retStatus = new cfStructData();
		DiffHandler hDiff = new DiffHandler();
		for ( int i = 1; i < _aRevs.size(); i++ ) {
			String revKey = _aRevs.get( i - 1 ) + "-" + _aRevs.get( i );
			SVNRevision rev1 = SVNRevision.create( _aRevs.get( i - 1 ) );
			SVNRevision rev2 = SVNRevision.create( _aRevs.get( i ) );
			_diffClient.doDiffStatus( _target, rev1, _target, rev2, SVNDepth.EMPTY, false, hDiff );

			cfStructData tmp = new cfStructData();
			tmp.setData( "kind", hDiff.getDiffStatus().getKind().toString() );
			tmp.setData( "file", hDiff.getDiffStatus().getFile().toString() );
			tmp.setData( "modification", hDiff.getDiffStatus().getModificationType().toString() );
			tmp.setData( "URL", hDiff.getDiffStatus().getURL().toString() );
			retStatus.setData( revKey, tmp );
		}

		return retStatus;
	}


	/**
	 * get the diff string for the file
	 * 
	 * @param _diffClient
	 * @param _target
	 * @param _aRevs
	 * @return get the diff for the file over the selected revisions, if multiple revisions, each revision will be delimited by ASCII-28
	 *         ASCII-28: File Separator
	 * @throws SVNException
	 * @throws UnsupportedEncodingException
	 */
	private String diffString( SVNDiffClient _diffClient, SVNURL _target, List<Integer> _aRevs, String _charSet ) throws SVNException, UnsupportedEncodingException {
		ByteArrayOutputStream osDiff = null;
		String diff = new String();
		SVNRevision revNewer = null;
		SVNRevision revOlder = null;

		// loop over all the revisions we are getting the diff for
		for ( int i = 1; i < _aRevs.size(); i++ ) {
			osDiff = new ByteArrayOutputStream();
			revNewer = SVNRevision.create( _aRevs.get( i - 1 ) );
			revOlder = SVNRevision.create( _aRevs.get( i ) );
			// get the diff string
			_diffClient.doDiff( _target, revOlder, _target, revNewer, SVNDepth.EMPTY, false, osDiff );
			if ( diff.length() > 0 ) {
				// append delimiter if needed
				diff += SVNDiff.DELIMITER;
			}
			// append the diff string
			if ( _charSet.isEmpty() ) {
				diff += osDiff.toString();
			} else {
				diff += osDiff.toString( _charSet );
			}

		}

		// return the list of diff strings
		return diff;
	}


	/**
	 * parse the diff string and send back HTML
	 * 
	 * @param _diff
	 * @return HTML formatted diff
	 */
	private static String htmlParse( String _diff ) {
		// split out all the diffs by revisions, and parse each one
		String[] aDiff = _diff.split( SVNDiff.DELIMITER );
		String htmlDiff = "";

		// loop over each reivison diff
		for ( int i = 0; i < aDiff.length; i++ ) {
			// format the diff into HTML
			htmlDiff += htmlParseRev( aDiff[i] );
		}

		// return back the HTML formatted diff
		return htmlStyle + htmlDiff;
	}


	/**
	 * format the diff with HTML
	 * 
	 * @param _diff
	 * @return HTML formated diff
	 */
	private static String htmlParseRev( String _diff ) {

		String[] aDiff = _diff.split( SVNDiff.LF );
		String htmlDiff = "";

		// the first 4 lines are informational, after than is actual conent
		if ( aDiff.length >= Diff.DIFFLINESTART ) {
			int i = Diff.DIFFLINESTART;

			// first line is a the file name
			// second line is a delimiter
			// third line is the path, and revision of the previous file-revision
			// fourth line is the path and revions of the current file-revision
			htmlDiff += "<div class='patch'><revision class='modfile'><h4>" + aDiff[0] + "</h4>";
			htmlDiff += "<pre class='diff'><span>";
			htmlDiff += "<span class='info'>" + aDiff[2] + " " + aDiff[3] + "</span>";

			// loop over all the diff lines and format


			for ( i = Diff.DIFFLINESTART; i < aDiff.length; i++ ) {
				if ( aDiff[i].startsWith( Diff.SVNDIFFSECTIONHEADER ) ) {
					/*
					 * inside a file wil be sections of diffs, it will start with a line that gives
					 * the information that depicts where in the file the changes are
					 * EX: @@ -1,2 +1,4
					 * starting at line 1 in the previous revisions, 2 lines were affected
					 * starting at line 1 in the newest revision, 4 lines were affected
					 */
					htmlDiff += "<span class='lines'>" + aDiff[i] + "</span>";
				} else {
					// lines prefixed with + are lines added
					// lines prefixed with - are lines removed
					aDiff[i] = cleanDiffLine( aDiff[i] );

					if ( aDiff[i].startsWith( SVNDiff.PREFIXADD ) ) {
						// added line
						htmlDiff += "<ins>" + aDiff[i] + "</ins>";
					} else if ( aDiff[i].startsWith( SVNDiff.PREFIXREMOVE ) ) {
						// removed line
						htmlDiff += "<del>" + aDiff[i] + "</del>";
					} else {
						// pristine line
						htmlDiff += "<span class='cx'>" + aDiff[i] + "</span>";
					}
				}
			}

			htmlDiff += "</span></pre></div></div>";
		}

		return htmlDiff;

	}


	/**
	 * replace character that would affect HTML rendering
	 * 
	 * @param _line
	 * @return html formatted string
	 */
	private static String cleanDiffLine( String _line ) {
		return _line.replace( "<", "&lt;" ).replace( ">", "&gt;" );
	}


	/**
	 * Translate diffLines to an cfArray
	 * 
	 * @param _lines
	 * @return the difflines converted to CFML array data type
	 * @throws cfmRunTimeException
	 */
	private static cfArrayData diffLinesToCF( DiffLines _lines ) throws cfmRunTimeException {
		cfArrayData aDiff = cfArrayData.createArray( 1 );

		// loop over ever item in the diffLines
		for ( int i = 0; i < _lines.size(); i++ ) {
			// add the translated diffLine as a struct to the array
			aDiff.addElement( diffLinesToCF( _lines.get( i ) ) );
		}

		// return the array
		return aDiff;
	}


	/**
	 * Translate a diffLine to a cfStruct
	 * 
	 * @param _line
	 * @return the diffLine converted to a CFML struct data type
	 */
	private static cfStructData diffLinesToCF( DiffLine _line ) {
		cfStructData uLine = new cfStructData();
		cfStructData uRevs = new cfStructData();
		Map<Integer, DiffRevision> revData = (Map<Integer, DiffRevision>) _line.get( "revision" );

		// set the status key
		uLine.setData( "status", _line.getStatus() );
		// set the reivision key
		uLine.setData( "revision", uRevs );
		// see if we need to set the explode key
		if ( uLine.containsKey( "explode" ) ) {
			uLine.setData( "explode", cfBooleanData.TRUE );
		}

		// see if we have a source key
		if ( _line.containsKey( "source" ) ) {
			uLine.setData( "source", (String) _line.get( "source" ) );
		}

		// loop over the revision and conver them to CFML structs
		for ( Map.Entry<Integer, DiffRevision> rev : revData.entrySet() )
		{
			// the key is the revision, and the value is a struc that contains extra informaiton
			uRevs.setData( rev.getKey().toString(), diffRevToCF( rev.getValue() ) );
		}

		return uLine;
	}


	/**
	 * convert the diffRevision to a cfStruct
	 * 
	 * @param _rev
	 * @return the diffRevision converted to a CFML struct data type
	 */
	private static cfStructData diffRevToCF( DiffRevision _rev ) {
		cfStructData uRev = new cfStructData();
		// loop over every item in here and set a key in the struct
		for ( Map.Entry<String, Object> rev : _rev.entrySet() ) {
			if ( rev.getKey() == "line" || rev.getKey() == "after" ) {
				uRev.setData( rev.getKey(), (Integer) rev.getValue() );
			} else {
				uRev.setData( rev.getKey(), (String) rev.getValue() );
			}
		}
		return uRev;
	}

}
