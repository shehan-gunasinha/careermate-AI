package com.careermate.resume;

import com.careermate.common.exception.ResourceNotFoundException;
import com.careermate.config.AppProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Stores resume binaries on disk (a Docker volume in production). Paths are always resolved inside the root. */
@Service
public class FileStorageService {
    private final Path root;

    public FileStorageService(AppProperties props) {
        this.root = Path.of(props.getStorage().getDir()).toAbsolutePath().normalize();
    }

    public String store(Long userId, byte[] bytes, String extension) {
        try {
            Path dir = root.resolve(String.valueOf(userId)).normalize();
            Files.createDirectories(dir);
            String name = UUID.randomUUID() + "." + extension;
            Files.write(dir.resolve(name), bytes);
            return userId + "/" + name;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store file", e);
        }
    }

    public byte[] load(String relativePath) {
        Path p = root.resolve(relativePath).normalize();
        if (!p.startsWith(root)) {
            throw new ResourceNotFoundException("File not found");
        }
        try {
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw new ResourceNotFoundException("File not found");
        }
    }

    public void delete(String relativePath) {
        Path p = root.resolve(relativePath).normalize();
        if (p.startsWith(root)) {
            try {
                Files.deleteIfExists(p);
            } catch (IOException ignored) {
                // best effort
            }
        }
    }
}
