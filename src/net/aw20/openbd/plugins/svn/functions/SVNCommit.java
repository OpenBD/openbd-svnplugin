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


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.aw20.openbd.plugins.svn.SVNRepo;

import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;

import com.naryx.tagfusion.cfm.engine.cfArgStructData;
import com.naryx.tagfusion.cfm.engine.cfArrayData;
import com.naryx.tagfusion.cfm.engine.cfBinaryData;
import com.naryx.tagfusion.cfm.engine.cfBooleanData;
import com.naryx.tagfusion.cfm.engine.cfData;
import com.naryx.tagfusion.cfm.engine.cfSession;
import com.naryx.tagfusion.cfm.engine.cfStructData;
import com.naryx.tagfusion.cfm.engine.cfmRunTimeException;
import com.naryx.tagfusion.cfm.engine.dataNotSupportedException;


/**
 * OpenBD class for function: SVNCommit()
 * 
 * Called from OpenBD <code>SVNCommit("mySVN",
 * 									[	{svnPath:"/index.cfm",localPath:"c:/index.cfm"} //Add this file to the repository
 * 									 	{svnPath:"/index.cfm",content:"<html><h1>Hello World!</h1></html>", charset=""} //Add a file to the repository with this content
 * 									 	{svnPath:"/index.cfm",localPath:"c:/index.cfm",checkSum:"{PRISTINE MD5 HASH}"} //Update this file to the repository
 * 										{svnPath:"/index.cfm",content:"<html><h1>Hello World!</h1></html>",checkSum:"{PRISTINE MD5 HASH}", charset=""} //Add the file on the repository with this content
 * 										{svnPath:"/index.cfm"} //delete this file from the repository
 * 										{property:"{SVN-PROPERTY NAME}",value:"{PROPERTY VALUE}",checkSum:"{PRISTINE MD5 HASH}"} //set the SVN property of this file
 * 									],
 * 									"My Commit Message",
 * 									-1);</code>
 * 
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 2.0.0
 * @see http://svnkit.com
 */
public class SVNCommit extends SVNLogView {

	private static final long serialVersionUID = 1L;
	private static final String UPSERT = "upsert";
	private static final String ADD = SVNCommit.UPSERT;
	private static final String MODIFY = SVNCommit.UPSERT;
	private static final String DELETE = "delete";
	private static final String PROPERTY = "property";


	public SVNCommit() {
		min = 3;
		max = 4;
		setNamedParams( new String[] {
				"name",
				"actionElems",
				"message",
				"revision" } );
	}


	@Override
	public String[] getParamInfo() {
		return new String[] {
				"Name of the SVN Repository to use",
				"array of elements (file/dir) to commit",
				"log message to commit",
				"The revision to use, default HEAD" };
	}


	@Override
	public Map<String, String> getInfo() {
		return makeInfo(
				"svn",
				"Commits the specified actions",
				ReturnType.BOOLEAN );
	}


	/**
	 * Used to commit elements (add/modify/delete/properties) actions to SVN
	 * 
	 * @param _session
	 * @param _argStruct
	 *          name: String repository name to use
	 *          actionElems: Array of Structs that contain parameters appropriate to an action desired
	 *          message: String commit message to use
	 *          revision: Integer revision to target these action elements at, default to HEAD (-1)
	 * @return boolean true: the commit operation succeeded
	 * @throws cfmRunTimeException
	 *           SVN Repository name not supplied
	 *           Commit message not supplied
	 *           Action Array not supplied
	 *           Invalid parameter combination
	 *           expecting content in the following pairs
	 *           {svnPath,localPath,checksum} //Modify
	 *           {svnPath,content,checksum,charset} //Modify
	 *           {svnPath,localPath} //Add
	 *           {svnPath,content,charset} //Add
	 *           {svnPath} //Delete
	 *           {svnPath,property,value,checksum} //Set property
	 *           Unable to connect to repository
	 *           Unable to begin editor
	 *           Invalid Commit action
	 *           Error aborting editor
	 *           Unable to update file, local file/content not available
	 *           Pristine md5 does not match
	 *           Checksums are the same
	 * @since 2.0.0
	 */
	@Override
	public cfData execute( cfSession _session, cfArgStructData _argStruct ) throws cfmRunTimeException {
		String name = getNamedStringParam( _argStruct, "name", "" ).trim();
		cfArrayData actionElems = (cfArrayData) getNamedParam( _argStruct, "actionElems", null );
		String commitMessage = getNamedStringParam( _argStruct, "message", "" ).trim();
		int revision = getNamedIntParam( _argStruct, "revision", SVNRepo.HEAD );

		String action = null;
		ISVNEditor editor = null;

		// Validate arguments
		if ( actionElems == null ) {
			throwException( _session, "Please provide an Action Array" );
		}

		if ( commitMessage.isEmpty() ) {
			throwException( _session, "Please provide a Commit Message" );
		}

		if ( name.isEmpty() ) {
			throwException( _session, "Please provide a SVN Repository" );
		}

		try ( SVNRepo repo = new SVNRepo( name ) ) {

			for ( int x = 1; x <= actionElems.size(); x++ ) {
				cfStructData actionData = (cfStructData) actionElems.getData( x );
				boolean fileTester = actionData.getData( "svnPath" ).toString().endsWith( "/" );
				String thisAction = null;
				String type = "";

				try {
					type = repo.getPathType( actionData.getData( "svnPath" ).toString(), revision );
				} catch ( SVNException e ) {
					throwException( _session, "Unable to connect to " + name + ". Please provide a Valid SVN Repository" );
				}

				if ( actionData.containsKey( "svnPath" ) && ( actionData.containsKey( "localPath" ) || actionData.containsKey( "content" ) ) && actionData.containsKey( "checksum" ) && !actionData.getData( "checksum" ).toString().isEmpty() ) {
					thisAction = SVNCommit.MODIFY;
				} else if ( actionData.containsKey( "svnPath" ) && ( actionData.containsKey( "localPath" ) || actionData.containsKey( "content" ) ) ) {
					thisAction = SVNCommit.ADD;
				} else if ( actionData.containsKey( "svnPath" ) && ( actionData.containsKey( "properties" ) || actionData.containsKey( "property" ) && actionData.containsKey( "value" ) ) ) {
					thisAction = SVNCommit.PROPERTY;
				} else if ( actionData.containsKey( "svnPath" ) ) {
					thisAction = SVNCommit.DELETE;
				} else {
					throwException( _session, "Invalid combination of parameters. Only accepts {svnPath,localPath,checksum},{svnPath,content,checksum},{svnPath,localPath},{svnPath,content},{svnPath},{property,value,checksum}.  If checksum is provided, it must not be empty" );
				}

				actionData.put( "file", !fileTester );
				actionData.put( "existing", !type.equals( "none" ) );
				actionData.put( "action", thisAction );

			}

			// get the editor
			// svn can NOT be used again until editor.closeEdit() or editor.abortEdit()
			try {
				editor = repo.getEditor( commitMessage );
			} catch ( SVNException e1 ) {
				throwException( _session, "Unable begin editor to " + name + "." );
			}

			try {
				// open the SVN root at the revision
				editor.openRoot( revision );

				// Process the action array
				for ( int x = 1; x <= actionElems.size(); x++ ) {
					cfStructData actionData = (cfStructData) actionElems.getData( x );
					// possible elements that will be in sd
					action = actionData.getData( "action" ).toString();

					switch ( action ) {
						case SVNCommit.UPSERT : // add an element (file/dir)
							svnElementUpsert( actionData, editor, revision );
							break;
						case SVNCommit.DELETE : // delete an element (file/dir)
							svnElementDelete( actionData, editor, revision );
							break;
						case SVNCommit.PROPERTY : // only setting properties
							svnElementSetProperty( actionData, editor, revision, true );
							break;
						default :
							throwException( _session, action + " is not a valid SVN commit actions" );
					}

				}// for

				// actually commit the actions
				editor.closeEdit();

			} catch ( SVNException | IOException e ) {
				// Oops, roll back actions
				try {
					editor.abortEdit();
				} catch ( SVNException e1 ) {
					throwException( _session, "There was an error aborting the editor." + e1.getMessage() );
				}

				throwException( _session, e.getMessage() );
			} // try-catch
		} catch ( SVNException e ) {
			throwException( _session, "Unable to connect to " + name + ". Please provide a Valid SVN Repository" );
		}

		return cfBooleanData.TRUE;
	}


	/**
	 * Modify/Add an element in SVN
	 * 
	 * <code>this.svnElementUpsert(actionData, editor, SVNReop.HEAD)</code>
	 * 
	 * @param _params
	 *          cfStructData of parameters needed to add/modify an element in SVN
	 * @param _editor
	 *          ISVNEditor used to commit the action and element
	 * @param _revision
	 *          int revision to commit against
	 * @throws SVNException
	 *           Unable to update file, local file/content not available
	 *           Pristine md5 does not match
	 *           Checksums are the same
	 * @throws IOException
	 *           Unable to read local file
	 * @throws dataNotSupportedException
	 *           boolean flag no translated
	 * @since 2.0.0
	 */
	private void svnElementUpsert( cfStructData _params, ISVNEditor _editor, int _revision ) throws SVNException, IOException, dataNotSupportedException {

		if ( !_params.getData( "file" ).getBoolean() ) {
			_editor.addDir( _params.getData( "svnPath" ).toString(), null, _revision );
			if ( _params.containsKey( "properties" ) || _params.containsKey( "property" ) && _params.containsKey( "value" ) ) {
				_editor.openDir( _params.getData( "svnPath" ).toString(), _revision );
				try {
					svnElementSetProperty( _params, _editor, _revision, false );
				} finally {
					_editor.closeDir();
				}
			}
		} else {

			String baseCheckSum = null;
			String newCheckSum = null;
			byte[] localBytes = null;

			if ( _params.containsKey( "localPath" ) ) {
				localBytes = Files.readAllBytes( new File( _params.getData( "localPath" ).toString() ).toPath() );
			} else if ( _params.containsKey( "content" ) ) {
				if ( _params.getData( "content" ).getDataType() == cfData.CFBINARYDATA ) {
					localBytes = ( (cfBinaryData) _params.getData( "content" ) ).getByteArray();
				} else if ( _params.containsKey( "charset" ) ) {
					localBytes = _params.getData( "content" ).toString().getBytes( _params.getData( "charset" ).toString() );
				} else {
					localBytes = _params.getData( "content" ).toString().getBytes();
				}

			} else {
				throw new SVNException( SVNErrorMessage.create( SVNErrorCode.BAD_CONFIG_VALUE, "Unable to update " + _params.getData( "svnPath" ).toString() + ", localPath/content were not provided." ) );
			}

			ByteArrayInputStream bais = new ByteArrayInputStream( localBytes );
			String svnPath = _params.getData( "svnPath" ).toString();

			if ( _params.getData( "existing" ).getBoolean() ) {
				if ( !_params.containsKey( "checkSum" ) || _params.getData( "checkSum" ).toString().isEmpty() ) {
					throw new SVNException( SVNErrorMessage.create( SVNErrorCode.CHECKSUM_MISMATCH, "Checksum must be supplied to updated file " + _params.getData( "localPath" ).toString() ) );
				}

				baseCheckSum = _params.getData( "checkSum" ).toString();
				_editor.openFile( svnPath, _revision );
			} else {
				_editor.addFile( svnPath, baseCheckSum, _revision );
			}

			try {
				if ( _params.containsKey( "properties" ) || _params.containsKey( "property" ) && _params.containsKey( "value" ) ) {
					svnElementSetProperty( _params, _editor, _revision, false );
				}

				_editor.applyTextDelta( svnPath, baseCheckSum );
				// get the Delta Generator
				SVNDeltaGenerator delta = new SVNDeltaGenerator();
				// Calculate the changes, and get the new checksum
				newCheckSum = delta.sendDelta( svnPath, bais, _editor, true );

				if ( baseCheckSum != null && baseCheckSum.equals( newCheckSum ) ) {
					throw new SVNException( SVNErrorMessage.create( SVNErrorCode.CHECKSUM_MISMATCH, "There are no changes in " + _params.getData( "svnPath" ).toString() ) );
				}
			} finally {
				_editor.closeFile( svnPath, newCheckSum );
			}
		}

	}


	/**
	 * Delete an element in SVN
	 * 
	 * <code>this.svnElementDelete(actionData, editor, SVNReop.HEAD)</code>
	 * 
	 * @param _params
	 *          cfStructData of parameters needed to delete an element in SVN
	 * @param _editor
	 *          ISVNEditor used to commit the action and element
	 * @param _revision
	 *          int revision to commit against
	 * @throws SVNException
	 *           Unable to delete the file from SVN
	 * @since 2.0.0
	 */
	private void svnElementDelete( cfStructData _params, ISVNEditor _editor, int _revision ) throws SVNException {
		_editor.deleteEntry( _params.getData( "svnPath" ).toString(), _revision );
	}


	/**
	 * Set a property of an element in SVN
	 * 
	 * <code>this.svnElementSetProperty(actionData, editor, SVNReop.HEAD)</code>
	 * 
	 * @param _params
	 *          cfStructData of parameters needed to set a property an element in SVN
	 * @param _editor
	 *          ISVNEditor used to commit the action and element
	 * @param _revision
	 *          int revision to commit against
	 * @param _openClose
	 *          boolean flag to indicate if the file/directory needs to be opened and closed
	 * @throws SVNException
	 *           Unable to delete the file from SVN
	 * @throws dataNotSupportedException
	 *           boolean flag no translated
	 * @since 2.0.0
	 */
	private void svnElementSetProperty( cfStructData _params, ISVNEditor _editor, int _revision, boolean _openClose ) throws SVNException, dataNotSupportedException {
		String svnPath = _params.getData( "svnPath" ).toString();
		boolean isFile = _params.getData( "file" ).getBoolean();

		try {

			if ( _openClose && isFile ) {
				_editor.openFile( svnPath, _revision );
			} else {
				_editor.openDir( svnPath, _revision );
			}

			if ( _params.containsKey( "properties" ) ) {

				cfData props = _params.getData( "properties" );
				if ( props != null ) {
					for ( Entry entry : ( (cfStructData) _params.getData( "properties" ) ).entrySet() ) {
						Object property = entry.getKey();
						Object value = entry.getValue();
						if ( property != null && value != null ) {
							setProperty( _editor, property.toString(), value.toString(), svnPath, isFile, _revision );
						}
					}
				}

			} else if ( _params.containsKey( "property" ) && _params.containsKey( "value" ) ) {

				cfData property = _params.getData( "property" );
				cfData value = _params.getData( "value" );
				if ( property != null && value != null ) {
					setProperty( _editor, property.toString(), value.toString(), svnPath, isFile, _revision );
				}

			}

		} finally {

			if ( _openClose && isFile ) {
				String checksum = ( _params.containsKey( "checksum" ) ? _params.getData( "checksum" ).toString() : null );
				_editor.closeFile( svnPath, checksum );
			} else {
				_editor.closeDir();
			}

		}

	}


	/**
	 * set a file/directory property
	 * 
	 * @param _editor
	 *          ISVNEditor to make the changes with
	 * @param _prop
	 *          String property name to set
	 * @param _val
	 *          String property value to set
	 * @param _path
	 *          String path of the element to set the property of
	 * @param _isFile
	 *          Boolean flag to indicate if we are working on a file or folder
	 * @param _revision
	 *          int revision number to apply the property at
	 * @throws SVNException
	 */
	private void setProperty( ISVNEditor _editor, String _prop, String _val, String _path, boolean _isFile, int _revision ) throws SVNException {

		if ( _prop != null && !_prop.isEmpty() ) {
			if ( _isFile ) {
				_editor.changeFileProperty( _path, _prop, SVNPropertyValue.create( _val ) );
			} else {
				_editor.changeDirProperty( _prop, SVNPropertyValue.create( _val ) );
			}
		}

	}


}
