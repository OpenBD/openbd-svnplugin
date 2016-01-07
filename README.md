# OpenBD SVN Plugin

This is an Open Blue Dragon plugin that allow for integration with an SVN repository

The underlying SVN functionality is based on [SVNKit](http://svnkit.com/,"SVNKit")

## Functionality
This plugin contains the following functions

### Repository Connectivity
* SVNRegisterRepository( name, url, user, pass, path, key )
* SVNRemoveRepository( name )
* SVNIsValid( name )

### Repository Content Retrieval 
* SVNUpdate( name, svnPath, localPath, revision, recursive, properties )
* SVNGetFile( name, svnPath, localPath, revision, properties )
* SVNGetDir( name, svnPath, localPath, revision, recursive, properties )

### Repository Content Update
* SVNCommit( name, actionElems, message, revision )

### Repository Information
* SVNLatestRevision( name )
* SVNGetRevision( name, svnPath, revision)
* SVNLogView( name, startRevision, endRevision, filter, fileFilter, startDateFilter, endDateFilter, patterndate)
* SVNGetStatus( name, svnPath, revision )
* SVNDirectoryList( name, svnPath, recursive, listInfo, filter, sort, revision )
* SVNDiff( name, svnPath, listInfo, revisionNewest, revisionOldest, splitRev, splitStartRevision, charset )

## Using this Plugin
This plugin requires OpenBD to be running.
See [ReadMe](https://github.com/OpenBD/openbd-core/blob/master/README.md, "Running OpenBD") for OpenBD.
The build directory contains openbdplugin-svn.jar and a number of supporting JARs that need to be placed inside the lib directory of OpenBD.
The Jetty instance will need to be restarted.