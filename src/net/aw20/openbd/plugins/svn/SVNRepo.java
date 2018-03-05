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
import java.lang.AutoCloseable;
import java.util.TimeZone;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;


/**
 * Storage class for basic repository functionality
 *
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 2.0.0
 * @see http://svnkit.com
 */
public class SVNRepo implements AutoCloseable {

	public static final int HEAD = -1;
	public static final int BEGIN = 0;


	private final SVNRepository repository;


	/**
	 * Used for basic Repository interaction that is not specific to an action
	 * Set up the connection to the repository
	 *
	 * <code>repo = new SVNRepo("mySVN")</code>
	 *
	 * @param _name
	 *          name of a registered repository
	 * @throws SVNException
	 *           Bad repository name
	 * @since 2.0.0
	 */
	public SVNRepo( String _name ) throws SVNException {

		SVNKey repoKey = SVNKeyFactory.getRepo( _name );

		if ( repoKey == null ) {
			throw new SVNException( SVNErrorMessage.create( SVNErrorCode.AUTHZ_INVALID_CONFIG, "Repository " + _name + " could not be found" ) );
		}

		SVNRepositoryFactoryImpl.setup();

		repository = SVNRepositoryFactory.create( SVNURL.parseURIEncoded( repoKey.getURL() ) );

		ISVNAuthenticationManager authManager = new BasicAuthenticationManager( repoKey.getUser(), new File( repoKey.getPath() ), repoKey.getPass(), -1 );

		repository.setAuthenticationManager( authManager );

	}


	/**
	 * Get the SVNRepository to work with
	 *
	 * <code>svn = repo.getRepository()</code>
	 *
	 * @return SVNRepostiry the repository object to work with
	 * @since 2.0.0
	 */
	public SVNRepository getRepository() {
		return repository;
	}


	/**
	 * Get the editor object to work with
	 *
	 * <code>editor = repo.getEditor()</code>
	 *
	 * @param commitMessage
	 *          String message to commit with
	 * @return ISVNEditor the repository editor object to work with
	 * @throws SVNException
	 *           Unable to retrieve editor
	 * @since 2.0.0
	 *
	 */
	public ISVNEditor getEditor( String commitMessage ) throws SVNException {
		return getRepository().getCommitEditor( commitMessage, null );
	}


	/**
	 * Used to close a repository connection. Should be called when doen with the repository
	 *
	 * <code>repo.close()</code>
	 *
	 * implementing AutoCloseable makes this an autocall function when used with a try-with-resource <code>try (repo = new SVNRepo("mySVN")){...} catch(...){...}</code>
	 *
	 * @since 2.0.0
	 */
	@Override
	public void close() {
		if ( repository != null ) {
			repository.closeSession();
		}
	}


	/**
	 * Get the client Manager
	 *
	 * <code>client = repo.getClientManager()</code>
	 *
	 * @return SVNClientManager the client manager object to work with
	 * @since 2.0.0
	 */
	public SVNClientManager getClientManager() {
		ISVNOptions options = SVNWCUtil.createDefaultOptions( true );
		return SVNClientManager.newInstance( options, getRepository().getAuthenticationManager() );
	}


	/**
	 * Get the SVN node type at a given path
	 *
	 * <code>type = repo.getPathType("a/b.txt",SVNRepo.HEAD)</code>
	 *
	 * @param _path
	 *          String path of an element we are testing
	 * @param _revision
	 *          Int revision to check the type at
	 * @return String what type of element exists at the _path (dir/file/none/unknown)
	 * @since 2.0.0
	 */
	public String getPathType( String _path, int _revision ) throws SVNException {
		return repository.checkPath( _path, _revision ).toString();
	}


	/**
	 * Get the Diff Client for the repository
	 *
	 * <code>client = repo.getDiffClient()</code>
	 *
	 * @return SVNDiffClient for working with Diff
	 * @since 2.0.0
	 */
	public SVNDiffClient getDiffClient() {
		return new SVNDiffClient( getClientManager(), null );
	}


	/**
	 * Get the Log Client for the repository
	 *
	 * <code>client = repo.getLogClient()</code>
	 *
	 * @return SVNLogClient for working with logs
	 * @since 2.0.0
	 */
	public SVNLogClient getLogClient() {
		return getClientManager().getLogClient();
	}


}
