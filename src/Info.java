import java.io.PrintWriter;

import uk.co.caprica.vlcj.component.AudioMediaPlayerComponent;

/**
 * Hebra encargada de enviar información del tiempo de reproducción al cliente.
 * @author iye19
 *
 */
public class Info implements Runnable{
	private static AudioMediaPlayerComponent mediaPlayerComponent;
	private PrintWriter sock_out;
	private boolean connected;
	
	/**
	 * 
	 * @param ampc Componente encargado de la reproducción.
	 * @param pw Salida del socket.
	 */
	Info(AudioMediaPlayerComponent ampc, PrintWriter pw){
		mediaPlayerComponent = ampc;
		sock_out = pw;
		connected = true;
	}
	
	/**
	 * Termina la ejecución de la hebra.
	 */
	synchronized void close(){
		connected = false;
	}

	@Override
	public void run() {
		while(connected){
			try{
				Thread.sleep(1000);
				while(!mediaPlayerComponent.getMediaPlayer().isPlaying())
					synchronized (this) {
						wait();
					}
				synchronized (sock_out) {
					sock_out.println("time");
					sock_out.println(mediaPlayerComponent.getMediaPlayer().getTime());
					sock_out.println("");	
				}
			}
			catch(InterruptedException ex){};
		}		
	}
}
