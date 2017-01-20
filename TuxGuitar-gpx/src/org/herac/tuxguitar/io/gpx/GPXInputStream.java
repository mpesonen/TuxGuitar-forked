package org.herac.tuxguitar.io.gpx;

import java.io.InputStream;

import org.herac.tuxguitar.io.base.TGFileFormat;
import org.herac.tuxguitar.io.base.TGFileFormatException;
import org.herac.tuxguitar.io.base.TGSongReader;
import org.herac.tuxguitar.io.base.TGSongReaderHandle;
import org.herac.tuxguitar.song.factory.TGFactory;

public class GPXInputStream implements TGSongReader{

	public static final TGFileFormat FILE_FORMAT = new TGFileFormat("Guitar Pro 6", "audio/x-gtp", new String[]{"gpx"});

	// TODO: check if necessary
	private int gpxHeader;
	private InputStream gpxStream;
	private GPXFileSystem gpxFileSystem;
	private TGFactory factory;
	
	public TGFileFormat getFileFormat() {
		return FILE_FORMAT;
	}

	// TODO: check if necessary
	public void init(TGFactory factory, InputStream stream) {
		this.factory = factory;
		this.gpxStream = stream;
		this.gpxHeader = 0;
		this.gpxFileSystem = new GPXFileSystem();
	}

	// TODO: check if necessary
	public boolean isSupportedVersion() {
		try {
			this.gpxHeader = this.gpxFileSystem.getHeader( this.gpxStream );
			
			return this.gpxFileSystem.isSupportedHeader(this.gpxHeader);
		} catch (Throwable throwable) {
			return false;
		}
	}

	public void read(TGSongReaderHandle handle) throws TGFileFormatException {
		try {
			GPXFileSystem gpxFileSystem = new GPXFileSystem();
			gpxFileSystem.load(handle.getInputStream());

			GPXDocumentReader gpxReader = new GPXDocumentReader( gpxFileSystem.getFileContentsAsStream("score.gpif"));
			GPXDocumentParser gpxParser = new GPXDocumentParser( handle.getFactory() , gpxReader.read() );

			handle.setSong(gpxParser.parse());
		} catch (Throwable throwable) {
			throw new TGFileFormatException( throwable );
		}
	}
}
