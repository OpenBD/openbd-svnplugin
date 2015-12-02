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

import java.util.ArrayList;


/**
 * @author Simon White
 * @author Trace Sinclair
 * @version 2.0.0
 * @since 2.0.0
 * @see http://www.catalysoft.com/articles/StrikeAMatch.html
 */

public class LetterPairSimiarity {


	/**
	 * @author Simon White
	 * @param str
	 *          the string to break down
	 * @return an array of adjacent letter pairs contained in the input string
	 */
	private static String[] letterPairs( String str ) {
		int numPairs = str.length() - 1;
		String[] pairs = new String[numPairs];
		for ( int i = 0; i < numPairs; i++ ) {
			pairs[i] = str.substring( i, i + 2 );
		}
		return pairs;
	}


	/**
	 * @author Simon White
	 * @param str
	 *          the string to break down
	 * @return an ArrayList of 2-character Strings
	 */
	private static ArrayList<String> wordLetterPairs( String str ) {
		ArrayList<String> allPairs = new ArrayList<String>();
		// Tokenize the string and put the tokens/words into an array
		String[] words = str.split( "\\s+" );
		// For each word
		for ( int w = 0; w < words.length; w++ ) {
			// Find the pairs of characters
			if ( words[w].length() > 0 ) {
				String[] pairsInWord = letterPairs( words[w] );
				for ( int p = 0; p < pairsInWord.length; p++ ) {
					allPairs.add( pairsInWord[p] );
				}
			}
		}
		return allPairs;
	}


	/**
	 * test the simiarity of 2 strings A:B on a normalized [0,1] scale
	 * 
	 * @author Simon White
	 * @param str
	 *          the string to break down
	 * @return lexical similarity value in the range [0,1]
	 */
	public static double compareStrings( String str1, String str2 ) {
		ArrayList<String> pairs1 = wordLetterPairs( str1.toUpperCase() );
		ArrayList<String> pairs2 = wordLetterPairs( str2.toUpperCase() );
		int intersection = 0;
		int union = pairs1.size() + pairs2.size();
		for ( int i = 0; i < pairs1.size(); i++ ) {
			Object pair1 = pairs1.get( i );
			for ( int j = 0; j < pairs2.size(); j++ ) {
				Object pair2 = pairs2.get( j );
				if ( pair1.equals( pair2 ) ) {
					intersection++;
					pairs2.remove( j );
					break;
				}
			}
		}
		return ( 2.0 * intersection ) / union;
	}


	/**
	 * test the similarity of 2 string (A:B and B:A) on a normalized [0,1] scale
	 * this is useful it it is unknown which string is the dominant string
	 * 
	 * @author Trace Sinclair
	 * @param str1
	 * @param str2
	 * @return lexical similarity value in the range [0,1]
	 */
	public static double doubleCompareStrings( String str1, String str2 ) {
		return ( ( compareStrings( str1, str2 ) + compareStrings( str2, str1 ) ) / 2 );
	}

}
