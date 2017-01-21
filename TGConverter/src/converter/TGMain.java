package converter;

public class TGMain {

	private static String SOURCE_FILE = "/Users/matti/Desktop/guitar-tab-demo.gpx";
	private static String TARGET_FILE = "/Users/matti/Desktop/guitar-tab-demo.gp5";
	
	public static void main(String[] args) {
		if (args == null || args.length < 2) {
			System.out.println("Usage example: java -jar converter full_path_to_source.gpx full_path_to_target.gp5");
			System.exit(-1);
		}
		
		SOURCE_FILE = args[0];
		TARGET_FILE = args[1];

		TGFileConverter converter = new TGFileConverter();
		converter.initialize();
		converter.convert(SOURCE_FILE, TARGET_FILE);

		System.exit(0);
	}
}
