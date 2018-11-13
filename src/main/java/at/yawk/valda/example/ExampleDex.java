package at.yawk.valda.example;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * This is the code that is being transformed by Main.
 *
 * @author yawkat
 */
public class ExampleDex {
    public static void main(String[] args) throws IOException {
        URL url = new URL(args[0]);
        InputStream inputStream = url.openStream();

        // copy the data to stdout
        byte[] buf = new byte[1024];
        int len;
        while ((len = inputStream.read(buf)) != -1) {
            System.out.write(buf, 0, len);
        }
        System.out.flush();
    }
}
