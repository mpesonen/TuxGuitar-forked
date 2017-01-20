package converter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.herac.tuxguitar.resource.TGResourceException;
import org.herac.tuxguitar.resource.TGResourceLoader;

public class TGResourceLoaderImpl implements TGResourceLoader {
	
	@SuppressWarnings("unchecked")
	public <T> Class<T> loadClass(String name) throws TGResourceException {
		try {
			return (Class<T>) this.getClass().getClassLoader().loadClass(name);
		} catch (ClassNotFoundException e) {
			throw new TGResourceException(e);
		}
	}

	public InputStream getResourceAsStream(String name) throws TGResourceException {
		return this.getClass().getClassLoader().getResourceAsStream(name);
	}

	public URL getResource(String name) throws TGResourceException {
		return this.getClass().getClassLoader().getResource(name);
	}

	public Enumeration<URL> getResources(String name) throws TGResourceException {
		try {
			return this.getClass().getClassLoader().getResources(name);
		} catch (IOException e) {
			throw new TGResourceException(e);
		}
	}
}
