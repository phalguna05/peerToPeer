/*
 *                     CEN5501C Project2
 * This is the program starting remote processes.
 * This program was only tested on CISE SunOS environment.
 * If you use another environment, for example, linux environment in CISE 
 * or other environments not in CISE, it is not guaranteed to work properly.
 * It is your responsibility to adapt this program to your running environment.
 */

/**
 * The RemotePeerInfo class represents information about a remote peer.
 * It contains the peer's ID, address, and port number.
 */
public class RemotePeerInfo {
	// peerId stores the unique identifier for the peer
	public String peerId;

	// peerAddress stores the Host Address of the peer
	public String peerAddress;

	// peerPort stores the network port number of the peer
	public String peerPort;
	

	//Constructor to initialize RemotePeerInfo with peer ID, address, and port.
	public RemotePeerInfo(String pId, String pAddress, String pPort) {
		peerId = pId;
		peerAddress = pAddress;
		peerPort = pPort;
	}
}
