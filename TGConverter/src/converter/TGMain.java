package converter;

public class TGMain {

	private static String SOURCE_FILE = "/Users/matti/Desktop/guitar-tab-demo.gpx";
	private static String TARGET_FILE = "/Users/matti/Desktop/guitar-tab-demo.gp5";
	
	public static void main(String[] s) {
		TGFileConverter converter = new TGFileConverter();
		converter.initialize();
		converter.convert(SOURCE_FILE, TARGET_FILE);
	}
}
