package ChatApp;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the video call lifecycle with BOTH video and audio:
 * - Captures webcam frames (JPEG) and microphone audio, sends over UDP.
 * - Receives video frames and audio from the remote peer.
 *
 * UDP Protocol:
 *   [1 byte: type][4 bytes: data length][N bytes: data]
 *   type = 0x01 for video frame (JPEG)
 *   type = 0x02 for audio chunk (PCM)
 */
public class VideoCallManager {

    public interface VideoFrameListener {
        void onLocalFrame(Image frame);
        void onRemoteFrame(Image frame);
        void onCallError(String error);
        void onCallEnded();
    }

    // Packet type markers
    private static final byte TYPE_VIDEO = 0x01;
    private static final byte TYPE_AUDIO = 0x02;

    private static final int MAX_PACKET_SIZE = 65000;
    private static final int FRAME_INTERVAL_MS = 66;  // ~15 FPS

    // Audio format: 16kHz, 16-bit, mono — matches VoiceHandler
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(16000f, 16, 1, true, false);
    private static final int AUDIO_BUFFER_SIZE = 3200; // 100ms of audio at 16kHz * 2 bytes

    // Video
    private Webcam webcam;

    // Network
    private DatagramSocket sendSocket;
    private DatagramSocket receiveSocket;
    private int localPort;
    private InetAddress remoteAddress;
    private int remotePort;

    // Audio capture/playback
    private TargetDataLine micLine;
    private SourceDataLine speakerLine;

    // Threads
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread videoCaptureThread;
    private Thread audioCaptureThread;
    private Thread receiveThread;
    private VideoFrameListener listener;

    public VideoCallManager() {
    }

    public void setListener(VideoFrameListener listener) {
        this.listener = listener;
    }

    /**
     * Start the video call with video + audio.
     */
    public void start(String remoteHost, int remotePort, int localPort) throws Exception {
        if (running.get()) return;

        this.localPort = localPort;
        this.remotePort = remotePort;
        this.remoteAddress = InetAddress.getByName(remoteHost);

        // ═══════ OPEN WEBCAM ═══════
        webcam = Webcam.getDefault();
        if (webcam == null) {
            throw new RuntimeException("Không tìm thấy webcam!");
        }
        webcam.setViewSize(new Dimension(320, 240));
        webcam.open();

        // ═══════ OPEN MICROPHONE ═══════
        try {
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
            micLine = (TargetDataLine) AudioSystem.getLine(micInfo);
            micLine.open(AUDIO_FORMAT);
            micLine.start();
            System.out.println("[VideoCall] Microphone opened.");
        } catch (LineUnavailableException e) {
            System.err.println("[VideoCall] Microphone unavailable: " + e.getMessage());
            micLine = null;
        }

        // ═══════ OPEN SPEAKER ═══════
        try {
            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);
            speakerLine = (SourceDataLine) AudioSystem.getLine(speakerInfo);
            speakerLine.open(AUDIO_FORMAT);
            speakerLine.start();

            // Boost volume to maximum
            if (speakerLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl vol = (FloatControl) speakerLine.getControl(FloatControl.Type.MASTER_GAIN);
                vol.setValue(vol.getMaximum());
            }

            System.out.println("[VideoCall] Speaker opened.");
        } catch (LineUnavailableException e) {
            System.err.println("[VideoCall] Speaker unavailable: " + e.getMessage());
            speakerLine = null;
        }

        // ═══════ NETWORK ═══════
        sendSocket = new DatagramSocket();
        receiveSocket = new DatagramSocket(localPort);
        receiveSocket.setSoTimeout(5000);

        running.set(true);

        // Start video capture thread
        videoCaptureThread = new Thread(this::videoCaptureLoop, "VideoCall-VideoCap");
        videoCaptureThread.setDaemon(true);
        videoCaptureThread.start();

        // Start audio capture thread
        if (micLine != null) {
            audioCaptureThread = new Thread(this::audioCaptureLoop, "VideoCall-AudioCap");
            audioCaptureThread.setDaemon(true);
            audioCaptureThread.start();
        }

        // Start receive thread (handles both video and audio)
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
            if (webcam != null && webcam.isOpen()) webcam.close();
        } catch (Exception e) {
            System.err.println("[VideoCall] Error closing webcam: " + e.getMessage());
        }

        // Close microphone
        try {
            if (micLine != null && micLine.isOpen()) {
                micLine.stop();
                micLine.close();
            }
        } catch (Exception e) {
            System.err.println("[VideoCall] Error closing mic: " + e.getMessage());
        }

        // Close speaker
        try {
            if (speakerLine != null && speakerLine.isOpen()) {
                speakerLine.drain();
                speakerLine.stop();
                speakerLine.close();
            }
        } catch (Exception e) {
            System.err.println("[VideoCall] Error closing speaker: " + e.getMessage());
        }

        // Close sockets
        if (sendSocket != null && !sendSocket.isClosed()) sendSocket.close();
        if (receiveSocket != null && !receiveSocket.isClosed()) receiveSocket.close();

        // Interrupt threads
        if (videoCaptureThread != null) videoCaptureThread.interrupt();
        if (audioCaptureThread != null) audioCaptureThread.interrupt();
        if (receiveThread != null) receiveThread.interrupt();

        System.out.println("[VideoCall] Stopped.");
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * Find an available UDP port.
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
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    // ======================== VIDEO CAPTURE ========================

    private void videoCaptureLoop() {
        while (running.get()) {
            try {
                BufferedImage frame = webcam.getImage();
                if (frame == null) continue;

                byte[] jpegData = toJpeg(frame);

                // Update local preview
                if (listener != null) {
                    Image fxImage = toFxImage(jpegData);
                    if (fxImage != null) {
                        Platform.runLater(() -> listener.onLocalFrame(fxImage));
                    }
                }

                // Send to remote: [TYPE_VIDEO][4-byte length][JPEG data]
                if (jpegData.length <= MAX_PACKET_SIZE - 5) {
                    byte[] packet = new byte[1 + 4 + jpegData.length];
                    packet[0] = TYPE_VIDEO;
                    ByteBuffer.wrap(packet, 1, 4).putInt(jpegData.length);
                    System.arraycopy(jpegData, 0, packet, 5, jpegData.length);

                    DatagramPacket dp = new DatagramPacket(packet, packet.length, remoteAddress, remotePort);
                    sendSocket.send(dp);
                }

                Thread.sleep(FRAME_INTERVAL_MS);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("[VideoCall] Video capture error: " + e.getMessage());
                }
            }
        }
    }

    // ======================== AUDIO CAPTURE ========================

    private void audioCaptureLoop() {
        byte[] buffer = new byte[AUDIO_BUFFER_SIZE];
        while (running.get()) {
            try {
                int read = micLine.read(buffer, 0, buffer.length);
                if (read > 0) {
                    // Send to remote: [TYPE_AUDIO][4-byte length][PCM data]
                    byte[] packet = new byte[1 + 4 + read];
                    packet[0] = TYPE_AUDIO;
                    ByteBuffer.wrap(packet, 1, 4).putInt(read);
                    System.arraycopy(buffer, 0, packet, 5, read);

                    DatagramPacket dp = new DatagramPacket(packet, packet.length, remoteAddress, remotePort);
                    sendSocket.send(dp);
                }
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("[VideoCall] Audio capture error: " + e.getMessage());
                }
                break;
            }
        }
    }

    // ======================== RECEIVE LOOP ========================

    private void receiveLoop() {
        byte[] buffer = new byte[MAX_PACKET_SIZE];
        while (running.get()) {
            try {
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                receiveSocket.receive(dp);

                if (dp.getLength() < 5) continue;

                byte type = dp.getData()[0];
                int dataLen = ByteBuffer.wrap(dp.getData(), 1, 4).getInt();
                if (dataLen <= 0 || dataLen > dp.getLength() - 5) continue;

                byte[] data = new byte[dataLen];
                System.arraycopy(dp.getData(), 5, data, 0, dataLen);

                if (type == TYPE_VIDEO) {
                    // Video frame
                    if (listener != null) {
                        Image fxImage = toFxImage(data);
                        if (fxImage != null) {
                            Platform.runLater(() -> listener.onRemoteFrame(fxImage));
                        }
                    }
                } else if (type == TYPE_AUDIO) {
                    // Audio chunk → play through speaker
                    if (speakerLine != null && speakerLine.isOpen()) {
                        speakerLine.write(data, 0, data.length);
                    }
                }

            } catch (SocketTimeoutException e) {
                // Normal timeout
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
