import java.util.UUID;

public class Test {
	public static void main(String[] args) {
		String s = UUID.randomUUID().toString();
		System.out.println(s);
		System.out.println(s.length());
		System.out.println(s.substring(0, 8));
	}
}
