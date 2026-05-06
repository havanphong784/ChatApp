package ChatApp;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Utility class to record and play simple WAV audio using the system microphone.
 * Audio recorded at 44.1kHz for higher quality and louder playback.
 */
public class VoiceHandler {
    private TargetDataLine line;
    private ByteArrayOutputStream rawOut;
    private volatile boolean recording = false;
    private final AudioFormat format;
    private Thread captureThread;

    public VoiceHandler() {
        // 44.1 kHz, 16 bits, mono, signed, little-endian — higher quality, louder
        this.format = new AudioFormat(44100f, 16, 1, true, false);
    }

    public void startRecording() throws LineUnavailableException {
        if (recording) return;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        rawOut = new ByteArrayOutputStream();
        recording = true;

        captureThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            try {
                while (recording) {
                    int read = line.read(buffer, 0, buffer.length);
                    if (read > 0) rawOut.write(buffer, 0, read);
                }
            } catch (Exception ignored) {
            } finally {
                if (line != null) {
                    try {
                        line.stop();
                    } catch (Exception ignored) {
                    }
                    try {
                        line.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }, "Voice-Capture-Thread");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    /**
     * Stop recording and return WAV-formatted bytes (with proper WAV header).
     * Applies volume boost (2x amplification) to recorded audio.
     */
    public byte[] stopRecording() throws IOException {
        if (!recording) return new byte[0];
        recording = false;
        // Unblock read() quickly so capture thread can finish cleanly.
        if (line != null) {
            line.stop();
            line.close();
        }
        if (captureThread != null) {
            try {
                captureThread.join(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        byte[] pcm = rawOut.toByteArray();

        // ═══════ AMPLIFY VOLUME (2x boost) ═══════
        pcm = amplify(pcm, 2.0);

        ByteArrayInputStream bais = new ByteArrayInputStream(pcm);
        long frameCount = pcm.length / format.getFrameSize();
        AudioInputStream ais = new AudioInputStream(bais, format, frameCount);

        ByteArrayOutputStream wavOut = new ByteArrayOutputStream();
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavOut);
        ais.close();
        return wavOut.toByteArray();
    }

    /**
     * Play WAV bytes with maximum volume. This method blocks while playback occurs.
     */
    public void play(byte[] wavBytes) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(wavBytes);
             AudioInputStream ais = AudioSystem.getAudioInputStream(bais)) {
            AudioFormat playbackFormat = ais.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, playbackFormat);
            Clip clip = (Clip) AudioSystem.getLine(info);
            CountDownLatch done = new CountDownLatch(1);
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP || event.getType() == LineEvent.Type.CLOSE) {
                    done.countDown();
                }
            });
            try {
                clip.open(ais);

                // ═══════ SET VOLUME TO MAXIMUM ═══════
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl vol = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    vol.setValue(vol.getMaximum()); // Max gain (typically +6dB)
                }

                clip.start();
                done.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                clip.close();
            }
        }
    }

    /**
     * Amplify PCM 16-bit audio data by a given factor.
     * Clamps values to prevent clipping distortion.
     */
    private byte[] amplify(byte[] pcm, double factor) {
        byte[] result = new byte[pcm.length];
        for (int i = 0; i < pcm.length - 1; i += 2) {
            // Read 16-bit sample (little-endian)
            int sample = (pcm[i] & 0xFF) | (pcm[i + 1] << 8);
            // Amplify
            sample = (int) (sample * factor);
            // Clamp to 16-bit range
            if (sample > Short.MAX_VALUE) sample = Short.MAX_VALUE;
            if (sample < Short.MIN_VALUE) sample = Short.MIN_VALUE;
            // Write back (little-endian)
            result[i] = (byte) (sample & 0xFF);
            result[i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return result;
    }
}
