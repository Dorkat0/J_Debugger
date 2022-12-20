import java.io.*;

public class Redirection extends Thread{
    Reader in;
    Writer out;
    Redirection(InputStream is, OutputStream os) {
        super();
        in = new InputStreamReader(is);
        out = new OutputStreamWriter(os);
    }
    public void run() {
        char[] buf = new char[1024];
        try {
            int n;
            while ((n = in.read(buf, 0, 1024)) >= 0) out.write(buf, 0, n);
            out.flush();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}

