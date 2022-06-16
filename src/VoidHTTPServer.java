import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

public class VoidHTTPServer implements Runnable {

    private final static File WEB_ROOT = new File(".");
    private final static File DEFAULT_FILE = new File("index.html");
    private final static File ERROR_404 = new File("404.html");
    private final static File METHOD_NOT_SUPPORTED = new File("not_supported.html");

    public static final boolean verbose = true;
    public static final int PORT = 8080;

    private Socket connect;

    public VoidHTTPServer(Socket s) {
        this.connect = s;
    }

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server started! The Magic happens on port " + PORT);

            while (!serverSocket.isClosed()) {
                Socket connectedSocket = serverSocket.accept();
                VoidHTTPServer httpServer = new VoidHTTPServer(connectedSocket);

                if (verbose) {
                    System.out.println("Connection opened (" + new Date() + ")");
                }

                Thread connectionThread = new Thread(httpServer);
                connectionThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        String fileRequested = null;
        try {
            in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            out = new PrintWriter(connect.getOutputStream());
            dataOut = new BufferedOutputStream(connect.getOutputStream());

            String input = in.readLine();
            StringTokenizer parser = new StringTokenizer(input);
            String method = parser.nextToken().toUpperCase();
            fileRequested = parser.nextToken().toLowerCase();
            if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("TEST")) {
                if (verbose) {
                    System.out.println("501 Not implemented : " + method + " method");
                }
                File file = new File(WEB_ROOT, String.valueOf(METHOD_NOT_SUPPORTED));
                int length = (int) file.length();
                String contentMimeType = "text/html";
                byte[] fileData = readFileData(file, length);
                out.println("HTTP/1.1 501 Not Implemented");
                out.println("Server: VoidHTTPServer by RedDev implemented in VoidClient : 1.0");
                out.println("Date: " + new Date());
                out.println("Content-Type" + contentMimeType);
                out.println("Content-length" + file.length());
                out.println();
                out.flush();

                dataOut.write(fileData, 0, length);
                dataOut.flush();
                return;
            } else {
                //GET or HEAD Method
                if (fileRequested.endsWith("/")) {
                    fileRequested += DEFAULT_FILE;
                }
                File file = new File(WEB_ROOT, fileRequested);
                int length = (int) file.length();
                String content = getContentType(fileRequested);

                if (method.equals("GET")) {
                    byte[] fileData = readFileData(file, length);
                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: VoidHTTPServer by RedDev implemented in VoidClient : 1.0");
                    out.println("Date: " + new Date());
                    out.println("Content-Type" + content);
                    out.println("Content-length" + file.length());
                    out.println();
                    out.flush();
                    dataOut.write(fileData, 0, length);
                    dataOut.flush();
                }

                if (verbose) {
                    System.out.println("File " + fileRequested + " of type " + content + " returned");
                }
            }
        }
        catch (FileNotFoundException e) {
            try {
                fileNotFound(out, dataOut, fileRequested);
            } catch (IOException ioException) {
                if (verbose) {
                    System.err.println("Error with file not found exception: " + ioException.getMessage());
                }
            }
        }
        catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
        finally {
            try {
                in.close();
                out.close();
                dataOut.close();
                connect.close();
            } catch (IOException e) {
                System.err.println("Error closing stream: " + e.getMessage());
            }

            if (verbose) {
                System.out.println("Connection closed\n");
            }
        }
    }

    private byte[] readFileData(File file, int length) throws IOException {
        byte[] fileData = new byte[length];
        FileInputStream fileIn = null;
        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null) {
                fileIn.close();
            }
        }
        return fileData;
    }

    private String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".htm") || fileRequested.endsWith(".html"))
            return "text/html";
         else
             return "text/plain";
    }

    private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
        File file  = new File(WEB_ROOT, String.valueOf(ERROR_404));
        int length = (int) file.length();
        String content = "text/html";
        byte[] fileData = readFileData(file, length);

        out.println("HTTP/1.1 404 File Not found");
        out.println("Server: VoidHTTPServer by RedDev implemented in VoidClient : 1.0");
        out.println("Date: " + new Date());
        out.println("Content-Type" + content);
        out.println("Content-length" + file.length());
        out.println();
        out.flush();
        dataOut.write(fileData, 0, length);
        dataOut.flush();

        if (verbose) {
            System.out.println("File " + fileRequested + " not found");
        }
    }
}