import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Hashtable;

public class ProxyServer {
	private static final int BUFFER_SIZE = 32768;
	public static PrintStream myconsole;

	public static void main(String[] args) {
		int port = 8080;
		/*For saving messages to log file starts*/
		try{
			myconsole = new PrintStream(new File("Log.txt"));
			System.setOut(myconsole);
			myconsole.println("ProxyServer Logs");
			
			
		}
		catch(FileNotFoundException ex)
		{
			System.out.println(ex);
			
		}
		/*For saving messages to log file ends*/
		if (args.length != 0) {
			port = Integer.parseInt(args[0]);
		}
		startServer(port);
	}

	private static void startServer(int port) {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);
			// wait for connection request
			 myconsole.println("Listening...");
			while (true) {
				Socket clientSocket = serverSocket.accept();
				// for multithreading
				Thread thread = new Thread(new SocketThread(clientSocket));
				thread.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != serverSocket) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static class SocketThread implements Runnable {
		private String BASE_FOLDER;
		private Socket client;

		public SocketThread(Socket s) {
			this.client = s;

			File cachedir = new File("Cache");
			cachedir.mkdirs();
			BASE_FOLDER = cachedir.getAbsolutePath();
		}

		@Override
		public void run() {
			DataOutputStream clientOutputStream = null;
			BufferedReader clientInputStream = null;
			FileOutputStream fos = null;
			String requestedUrlString =null;
			try {
				clientOutputStream = new DataOutputStream(client.getOutputStream());
				clientInputStream = new BufferedReader(new InputStreamReader(client.getInputStream()));
				String[] requestStrings = clientInputStream.readLine().split(" ");
				String requestedHttpMethod = requestStrings[0];
				requestedUrlString = requestStrings[1];
				if (requestedUrlString.startsWith("/www")) {
					requestedUrlString = "http:/" + requestedUrlString;
				} else if (requestedUrlString.startsWith("/http")) {
					requestedUrlString = requestedUrlString.replaceFirst("/", "");
				}
				// Cache cache = new Cache();
				URL requestedUrl = new URL(requestedUrlString);
				 //myconsole.println("requestedUrl "+requestedUrl);
				 myconsole.println("requestedUrlString  "+ requestedUrlString);
				if (requestedHttpMethod.equalsIgnoreCase("GET")) {
					if (IsCached(requestedUrlString)) {
						 myconsole.println("File Exists..Reading from Cache");
						fetchFromCache(requestedUrl, clientOutputStream, getFileInputStream(requestedUrlString));
					} else {
						//System.out.println("Reading from Server");
						myconsole.println("Reading from Server");
						fetchFromServer(requestedUrl, clientOutputStream, getFileOutputStream(requestedUrlString)); // h
					}
				} else {

				}
			} catch (IOException e) {
				try {
					if(null!=requestedUrlString){
						File f = new File(getFileName(requestedUrlString));
						f.delete();
					}
					clientOutputStream.writeBytes("HTTP/1.0 404 \n");
					clientOutputStream.writeBytes("Content-Type: text/html\n");
					clientOutputStream.writeBytes("\n");
					clientOutputStream.writeBytes("HTTP error. URL Destination not found!");
					clientOutputStream.writeBytes("\n");
					myconsole.println("HTTP/1.0 404 Not Found:URL Destination not found!");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} finally {
				try {
					if (null != clientInputStream) {
						clientInputStream.close();
					}
					if (null != clientOutputStream) {
						clientOutputStream.close();
					}
					if (null != client) {
						client.close();
					}
					if (null != fos) {
						fos.close();
					}

				} catch (IOException e) {
					e.printStackTrace();
				}

			}

		}

		private void fetchFromCache(URL requestedUrl, DataOutputStream outputResponse, FileInputStream fis)
				throws IOException {
			outputResponse.writeBytes("HTTP/1.0 200 \n");
			outputResponse.writeBytes("Content-Type: text/html\n");
			outputResponse.writeBytes("\n");
			byte by[] = new byte[BUFFER_SIZE];
			int index = fis.read(by, 0, BUFFER_SIZE);
			while (index != -1) {
				outputResponse.write(by, 0, index);
				index = fis.read(by, 0, BUFFER_SIZE);
			}
			fis.close();
		}

		private void fetchFromServer(URL requestedUrl, DataOutputStream outputResponse, FileOutputStream fos)
				throws IOException {
			URLConnection urlConnection = requestedUrl.openConnection();
			InputStream is = null;
			try {
				is = urlConnection.getInputStream();
				byte by[] = new byte[BUFFER_SIZE];
				int index = is.read(by, 0, BUFFER_SIZE);
				while (index != -1) {
					outputResponse.write(by, 0, index);
					fos.write(by, 0, index);//
					index = is.read(by, 0, BUFFER_SIZE);
				}
				outputResponse.flush();
				if (null != fos)
					fos.flush();

			} catch (IOException e) {
				throw e;
			} finally {
				if (null != is)
					is.close();
				if (null != fos)
					fos.close();
			}
		}

		public FileInputStream getFileInputStream(String rawurl) {
			FileInputStream in = null;
			String filename = "";
			try {
				filename = getFileName(rawurl);

				in = new FileInputStream(filename);
			} catch (FileNotFoundException fnf) {
				System.out.println("File Not Found:" + filename + " " + fnf);
				myconsole.println("File Not Found:" + filename + " " + fnf);
			} finally {
				return in;
			}
		}

		public boolean IsCached(String rawurl) {
			String filename = getFileName(rawurl);
			File cacheFile = new File(filename);
			if (cacheFile.exists())
				return true;

			return false;
		}

		public FileOutputStream getFileOutputStream(String rawurl) {
			FileOutputStream out = null;
			String filename;
			try {
				filename = getFileName(rawurl);
				out = new FileOutputStream(new File(filename));
			} catch (IOException e) {
			} finally {
				return out;
			}
		}

	    private String getFileName(String rawurl) {
			String filename = rawurl.substring(7).replace('/', '@');
			filename = filename.replaceAll("[:*?<.>|]", "@");
			String filePath = BASE_FOLDER + File.separatorChar + filename;
			return filePath;
		}
	}
}

