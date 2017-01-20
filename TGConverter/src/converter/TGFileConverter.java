package converter;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.herac.tuxguitar.io.base.TGFileFormat;
import org.herac.tuxguitar.io.base.TGFileFormatException;
import org.herac.tuxguitar.io.base.TGFileFormatManager;
import org.herac.tuxguitar.io.base.TGFileFormatUtils;
import org.herac.tuxguitar.io.base.TGSongReaderHandle;
import org.herac.tuxguitar.io.base.TGSongReaderHelper;
import org.herac.tuxguitar.io.base.TGSongStreamContext;
import org.herac.tuxguitar.io.base.TGSongWriterHandle;
import org.herac.tuxguitar.resource.TGResourceManager;
import org.herac.tuxguitar.song.factory.TGFactory;
import org.herac.tuxguitar.song.models.TGSong;
import org.herac.tuxguitar.util.TGContext;
import org.herac.tuxguitar.util.plugin.TGPluginManager;

public class TGFileConverter {
	
	private TGContext context;
	
	public TGFileConverter() {
		this.context = new TGContext();
	}
	
	public void initialize() {
		// We are looking for plugged song readers/writers
		TGResourceManager.getInstance(this.context).setResourceLoader(new TGResourceLoaderImpl());
		TGPluginManager.getInstance(this.context).connectEnabled();
	}
	
	public void convert(String sourceFileName, String targetFileName) {
		TGSong song = readFile(sourceFileName);
		if( song == null ) {
			throw new TGFileFormatException("Invalid song");
		}
		this.writeFile(song, targetFileName);
	}
	
	public TGSong readFile(String fileName) {
		try {
			// fomat code helps when the file format cannot not be determined from the input stream.
			String formatCode = TGFileFormatUtils.getFileFormatCode(fileName);
			
			TGSongReaderHandle handle = new TGSongReaderHandle();
			handle.setFactory(new TGFactory());
			handle.setInputStream(new FileInputStream(fileName));
			handle.setContext(new TGSongStreamContext());
			handle.getContext().setAttribute(TGSongReaderHelper.ATTRIBUTE_FORMAT_CODE, formatCode);
			TGFileFormatManager.getInstance(this.context).read(handle);
			
			return handle.getSong();
		} catch (IOException e) {
			throw new TGFileFormatException(e);
		}
	}
	
	public void writeFile(TGSong song, String fileName) {
		try {
			// fomat code helps when the file format cannot not be determined from the input stream.
			String formatCode = TGFileFormatUtils.getFileFormatCode(fileName);
			
			TGFileFormatManager fileFormatManager = TGFileFormatManager.getInstance(this.context);
			TGFileFormat fileFormat = fileFormatManager.findWriterFileFormatByCode(formatCode);
			
			TGSongWriterHandle handle = new TGSongWriterHandle();
			handle.setSong(song);
			handle.setFactory(new TGFactory());
			handle.setFormat(fileFormat);
			handle.setOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
			handle.setContext(new TGSongStreamContext());
			
			fileFormatManager.write(handle);
		} catch (IOException e) {
			throw new TGFileFormatException(e);
		}
	}
}
