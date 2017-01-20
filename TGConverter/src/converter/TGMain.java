package converter;

public class TGMain {

	private static String SOURCE_FILE = "/full_path_to_folder/source.tg";
	private static String TARGET_FILE = "/full_path_to_folder/target.mid";
	
	public static void main(String[] s) {
		TGFileConverter converter = new TGFileConverter();
		converter.initialize();
		converter.convert(SOURCE_FILE, TARGET_FILE);
	}
}
