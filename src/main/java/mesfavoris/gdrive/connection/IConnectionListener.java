package mesfavoris.gdrive.connection;


import java.util.EventListener;

public interface IConnectionListener extends EventListener {

	void connected();
	
	void disconnected();
	
}
