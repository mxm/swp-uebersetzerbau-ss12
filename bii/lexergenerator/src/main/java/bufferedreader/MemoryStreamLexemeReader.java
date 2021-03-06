/*
 * 
 * Copyright 2012 lexergen.
 * This file is part of lexergen.
 * 
 * lexergen is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * lexergen is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with lexergen.  If not, see <http://www.gnu.org/licenses/>.
 *  
 * lexergen:
 * A tool to chunk source code into tokens for further processing in a compiler chain.
 * 
 * Projectgroup: bi, bii
 * 
 * Authors: Johannes Dahlke
 * 
 * Module:  Softwareprojekt Übersetzerbau 2012 
 * 
 * Created: Apr. 2012 
 * Version: 1.0
 *
 */

package bufferedreader;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.CharBuffer;

import utils.Notification;


public class MemoryStreamLexemeReader implements LexemeReader {
	

	
	private int lexemeBeginMarker;
	private int streamPointer;
	private char[] buffer;
	
	public MemoryStreamLexemeReader( String SourceFile) throws IOException {
		// we open the file read only
		File file = new File( SourceFile);
		loadFileIntoMemory( file);
		lexemeBeginMarker = 0;
		streamPointer = 0;
	}
	

	private void loadFileIntoMemory( File file) throws IOException {
		// attention: int to long cast. Assume, that source code file stay below 
		buffer = new char[(int) file.length()];
		
		FileReader fileReader = new FileReader( file);
		fileReader.read( buffer);
		
	}


	public char getNextChar() throws LexemeReaderException {
		// Wenn nicht bereits zuvor EOF zurückgegeben wurde, dann spätestens jetzt.
		if ( streamPointer >= buffer.length)
			return SpecialChars.CHAR_EOF;
		// anderenfalls gib das aktuelle Zeichen zurück.
		return buffer[streamPointer++];
	}

	public void reset() throws LexemeReaderException {
		streamPointer = lexemeBeginMarker;
	}

	public void accept() throws LexemeReaderException {
  	lexemeBeginMarker = streamPointer;
	}

	public void stepBackward( int steps) throws LexemeReaderException {
		streamPointer -= steps;
		if (streamPointer < 0)
			streamPointer = 0;
	}
	
	
	
}
