package com.englishapp.video;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FfmpegService {

    private static final int TIMEOUT_MINUTES = 5;

    public int getDurationSec(File videoFile) {
        String output = run(List.of(
                "ffprobe", "-v", "error",
                "-show_entries", "format=duration",
                "-of", "csv=p=0",
                videoFile.getAbsolutePath()));
        return (int) Double.parseDouble(output.trim());
    }

    public File extractAudio(File videoFile) throws IOException {
        File output = Files.createTempFile("audio-", ".mp3").toFile();
        run(List.of(
                "ffmpeg", "-y", "-i", videoFile.getAbsolutePath(),
                "-vn", "-ar", "16000", "-ac", "1", "-b:a", "64k",
                output.getAbsolutePath()));
        return output;
    }

    /** Convert any audio format (webm, ogg, mp4…) to 16kHz mono WAV suitable for OpenAI audio models. */
    public byte[] convertToWav(byte[] inputBytes, String inputFormat) throws IOException {
        File inputFile  = Files.createTempFile("spk-in-",  "." + inputFormat).toFile();
        File outputFile = Files.createTempFile("spk-out-", ".wav").toFile();
        try {
            Files.write(inputFile.toPath(), inputBytes);
            run(List.of(
                    "ffmpeg", "-y",
                    "-i", inputFile.getAbsolutePath(),
                    "-ar", "16000", "-ac", "1",
                    "-f", "wav",
                    outputFile.getAbsolutePath()));
            return Files.readAllBytes(outputFile.toPath());
        } finally {
            inputFile.delete();
            outputFile.delete();
        }
    }

    public File extractThumbnail(File videoFile, double atSec) throws IOException {
        File output = Files.createTempFile("thumb-", ".jpg").toFile();
        run(List.of(
                "ffmpeg", "-y",
                "-ss", String.valueOf(atSec),
                "-i", videoFile.getAbsolutePath(),
                "-vframes", "1",
                "-q:v", "2",
                output.getAbsolutePath()));
        return output;
    }

    private String run(List<String> command) {
        log.debug("FFmpeg: {}", String.join(" ", command));
        long start = System.currentTimeMillis();

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread outReader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) stdout.append(line).append("\n");
                } catch (IOException ignored) {}
            });
            Thread errReader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) stderr.append(line).append("\n");
                } catch (IOException ignored) {}
            });

            outReader.start();
            errReader.start();

            boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            outReader.join(5000);
            errReader.join(5000);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("FFmpeg timed out after " + TIMEOUT_MINUTES + " minutes");
            }

            long elapsed = System.currentTimeMillis() - start;
            log.debug("FFmpeg done in {}ms, exit={}", elapsed, process.exitValue());

            if (process.exitValue() != 0) {
                throw new RuntimeException("FFmpeg exit " + process.exitValue() + ": " + stderr.toString().trim());
            }

            return stdout.toString().trim();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("FFmpeg execution failed: " + e.getMessage(), e);
        }
    }
}
