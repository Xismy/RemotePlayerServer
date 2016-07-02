
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;


import uk.co.caprica.vlcj.component.AudioMediaPlayerComponent;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.filter.AudioFileFilter;

/**
 * Hebra que implementa un reproductor de m�sica. Recibe peticiones del cliente, ejecuta las acciones correspondientes y responde si es necesario.
 * @author iye19
 *
 */
public class Player implements Runnable{
	
	private Socket socket;
	private BufferedReader sock_in;
	private PrintWriter sock_out;
	private static AudioMediaPlayerComponent mediaPlayerComponent;
	private Info info;
	private static AudioFileFilter filter = new AudioFileFilter();
	
	static{
		new NativeDiscovery().discover();
		mediaPlayerComponent = new AudioMediaPlayerComponent();
	}
	
	/**
	 * Inicializa los buffers de entrada y salida.
	 * @param s Socket creado al aceptar la petici�n del cliente.
	 */
	Player(Socket s){
		try{
			socket = s;
			sock_in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			sock_out = new PrintWriter(s.getOutputStream(), true);
			info = new Info(mediaPlayerComponent, sock_out);
			new Thread(info).start();
		}
		catch(IOException ex){}
	}
	
	private String[] ls(File current){
		File[] ls = current.listFiles();
		ArrayList<String> names =  new ArrayList<String>();
		for(File f:ls){
			if(f.isDirectory())
				names.add("d"+f.getName());
			else if(filter.accept(f))		
				names.add("a"+f.getName());
		}
		String[] namesArr = new String[names.size()];
		names.toArray(namesArr);
		return namesArr;
	}
	
	@Override
	public void run() {
		String cmd = "";
		File current = new File("").getAbsoluteFile();
		int playing = 0;
		String[] ls = ls(current);
		boolean isPlaying = false;
		boolean end = false;
	
		while(!end){
			try{
				cmd = sock_in.readLine();
				System.out.println(cmd);
				
				println(cmd);
				
				switch (cmd) {
					case"open":
						int index = Integer.parseInt(sock_in.readLine());
						String file = "";
						
						if(index == -1){
							file = current.getParent();
							if(file==null)
								file = current.getPath();
						}
						else
							file = current.getPath()+"/"+ls[index].substring(1);
						
						File aux = new File(file);
						if(aux.isDirectory()){
							current = aux;
							ls = ls(current);
						}
						else {
							playing = index;
							play(aux);
							isPlaying = mediaPlayerComponent.getMediaPlayer().isPlaying();
							break;
						}
							
					case "ls":
						for(String str:ls)
							println(str);
						println("");
						break;
						
					case "play":
						isPlaying = mediaPlayerComponent.getMediaPlayer().isPlaying();
						mediaPlayerComponent.getMediaPlayer().setPause(isPlaying);
						synchronized (info) {
							 info.notifyAll();
						}
						println("");
						break;
						
					case "set_time":
						Long time =Long.parseLong(sock_in.readLine());
						mediaPlayerComponent.getMediaPlayer().setTime(time);
						synchronized (info) {
							 info.notifyAll();
						}
						println("");
						break;
						
					case "volume_up":
						int vol = mediaPlayerComponent.getMediaPlayer().getVolume()+10;
						if(vol>200)
							vol=200;
						mediaPlayerComponent.getMediaPlayer().setVolume(vol);
						println(""+vol/10);
						println("");
						break;
						
					case "next":
						if(!isPlaying)
							break;
						do{
							playing++;
							if(playing >= ls.length)
								playing = 0;
							
						}while(!ls[playing].startsWith("a"));
						play(new File(current.getPath()+"/"+ls[playing].substring(1)));
						break;
						
					case "prev":
						if(!isPlaying)
							break;
						do{
							playing--;
							if(playing < 0)
								playing = ls.length-1;
							
						}while(!ls[playing].startsWith("a"));
						play(new File(current.getPath()+"/"+ls[playing].substring(1)));
						break;
						
					case "volume_down":
						vol = mediaPlayerComponent.getMediaPlayer().getVolume()-10;
						if(vol<0)
							vol=0;
						mediaPlayerComponent.getMediaPlayer().setVolume(vol);
						println(""+vol/10);
						println("");
						break;
						
					case "disconnect":
						end = true;
						println("");
						info.close();
						Thread.sleep(1000);
						sock_out.close();
						try{
							sock_in.close();
						}
						catch(IOException ioex){}
						socket.close();
						break;
		
					default:
						break;
				}
			}
			catch(Exception ex){
				end = true;
				ex.printStackTrace(new PrintWriter(System.out, true));
			}
		}
	}
	
	
	void play(File path){
		mediaPlayerComponent.getMediaPlayer().playMedia(path.getPath());
		while(!mediaPlayerComponent.getMediaPlayer().isMediaParsed())						 
			;
		Long length = mediaPlayerComponent.getMediaPlayer().getLength();
		println("playing "+length);
		println(path.getName());
		println("");
		while(!mediaPlayerComponent.getMediaPlayer().isPlaying())						 
			;
		synchronized (info) {
			info.notifyAll();
		}
	}
	
	/**
	 * Env�a un mensaje por el socket, impidiendo que otra hebra tome el control del buffer durante el proceso.
	 * @param str Mensaje a enviar.
	 */
	void println(String str){
		synchronized (sock_out) {
			sock_out.println(str);
		}
	}
}