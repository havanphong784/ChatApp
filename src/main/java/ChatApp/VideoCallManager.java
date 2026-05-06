package ChatApp;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the video call lifecycle:
 * - Captures webcam frames and sends them as JPEG over UDP to the remote peer.
 * - Receives JPEG frames from the remote peer over UDP.
 * - Provides callbacks for updating the UI with local and remote video frames.
 *
 * Protocol (UDP):
 *   [4 bytes: frame length][N bytes: JPEG data]
 */
public class VideoCallManager {

    public interface VideoFrameListener {
        void onLocalFrame(Image frame);
        void onRemoteFrame(Image frame);
        void onCallError(String error);
        void onCallEnded();
    }

    private static final int MAX_PACKET_SIZE = 65000; // UDP max practical payload
    private static final int FRAME_INTERVAL_MS = 66;  // ~15 FPS (good balance of quality/bandwidth)

    private Webcam webcam;
    private DatagramSocket sendSocket;
    private DatagramSocket receiveSocket;
    private int localPort;
    private InetAddress remoteAddress;
    private int remotePort;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread captureThread;
    private Thread receiveThread;
    private VideoFrameListener listener;

    public VideoCallManager() {
    }

    public void setListener(VideoFrameListener listener) {
        this.listener = listener;
    }

    /**
     * Start the video call. Opens webcam, binds a UDP receive socket, and begins
     * capturing + sending frames to the remote peer.
     *
     * @param remoteHost IP address of the remote peer
     * @param remotePort UDP port of the remote peer
     * @param localPort  local UDP port to listen on
     */
    public void start(String remoteHost, int remotePort, int localPort) throws Exception {
        if (running.get()) return;

        this.localPort = localPort;
        this.remotePort = remotePort;
        this.remoteAddress = InetAddress.getByName(remoteHost);

        // Open webcam
        webcam = Webcam.getDefault();
        if (webcam == null) {
            throw new RuntimeException("Không tìm thấy webcam!");
        }
        webcam.setViewSize(new Dimension(320, 240));
        webcam.open();

        // Create sockets
        sendSocket = new DatagramSocket();
        receiveSocket = new DatagramSocket(localPort);
        receiveSocket.setSoTimeout(5000);

        running.set(true);

        // Start capture thread (local webcam → send to remote)
        captureThread = new Thread(this::captureLoop, "VideoCall-Capture");
        captureThread.setDaemon(true);
        captureThread.start();

        // Start receive thread (remote frames → display)
        receiveThread = new Thread(this::receiveLoop, "VideoCall-Receive");
        receiveThread.setDaemon(true);
        receiveThread.start();

        System.out.println("[VideoCall] Started: local=" + localPort + " remote=" + remoteHost + ":" + remotePort);
    }

    /**
     * Stop the video call and release all resources.
     */
    public void stop() {
        if (!running.getAndSet(false)) return;

        System.out.println("[VideoCall] Stopping...");

        // Close webcam
        try {
            if (webcam != null && webcam.isOpen()) {
                webcam.close();
            }
        } catch (Exception e) {
            System.err.println("[VideoCall] Error closing webcam: " + e.getMessage());
        }

        // Close sockets
        if (sendSocket != null && !sendSocket.isClosed()) {
            sendSocket.close();
        }
        if (receiveSocket != null && !receiveSocket.isClosed()) {
            receiveSocket.close();
        }

        // Interrupt threads
        if (captureThread != null) captureThread.interrupt();
        if (receiveThread != null) receiveThread.interrupt();

        System.out.println("[VideoCall] Stopped.");
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * Find an available UDP port for receiving video.
     */
    public static int findAvailablePort() {
        try (DatagramSocket s = new DatagramSocket(0)) {
            return s.getLocalPort();
        } catch (SocketException e) {
            return 9000 + (int) (Math.random() * 1000);
        }
    }

    /**
     * Get the local IP address (non-loopback).
     */
    public static String getLocalIp() {
        try {
            // Try to find a non-loopback address
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                var addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            // Fallback
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    // ======================== CAPTURE LOOP ========================

    private void captureLoop() {
        while (running.get()) {
            try {
                BufferedImage frame = webcam.getImage();
                if (frame == null) continue;

                // Convert to JPEG bytes
                byte[] jpegData = toJpeg(frame);

                // Send local frame to UI
                if (listener != null) {
                    Image fxImage = toFxImage(jpegData);
                    if (fxImage != null) {
                        Platform.runLater(() -> listener.onLocalFrame(fxImage));
                    }
                }

                // Send to remote peer (if frame fits in one UDP packet)
                if (jpegData.length <= MAX_PACKET_SIZE) {
                    // Prepend 4-byte length header
                    byte[] packet = new byte[4 + jpegData.length];
                    ByteBuffer.wrap(packet).putInt(jpegData.length);
                    System.arraycopy(jpegData, 0, packet, 4, jpegData.length);

                    DatagramPacket dp = new DatagramPacket(packet, packet.length, remoteAddress, remotePort);
                    sendSocket.send(dp);
                }

                Thread.sleep(FRAME_INTERVAL_MS);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("[VideoCall] Capture error: " + e.getMessage());
                }
            }
        }
    }

    // ======================== RECEIVE LOOP ========================

    private void receiveLoop() {
        byte[] buffer = new byte[MAX_PACKET_SIZE + 4];
        while (running.get()) {
            try {
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                receiveSocket.receive(dp);

                if (dp.getLength() < 4) continue;

                // Extract length header
                int frameLen = ByteBuffer.wrap(dp.getData(), 0, 4).getInt();
                if (frameLen <= 0 || frameLen > dp.getLength() - 4) continue;

                byte[] jpegData = new byte[frameLen];
                System.arraycopy(dp.getData(), 4, jpegData, 0, frameLen);

                // Convert to JavaFX Image and send to UI
                if (listener != null) {
                    Image fxImage = toFxImage(jpegData);
                    if (fxImage != null) {
                        Platform.runLater(() -> listener.onRemoteFrame(fxImage));
                    }
                }
            } catch (SocketTimeoutException e) {
                // Normal timeout, continue waiting
            } catch (SocketException e) {
                if (running.get()) {
                    System.err.println("[VideoCall] Receive socket closed: " + e.getMessage());
                }
                break;
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("[VideoCall] Receive error: " + e.getMessage());
                }
            }
        }
    }

    // ======================== UTILITIES ========================

    private byte[] toJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    private Image toFxImage(byte[] jpegData) {
        try {
            return new Image(new ByteArrayInputStream(jpegData));
        } catch (Exception e) {
            return null;
        }
    }
}
