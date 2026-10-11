package com.lms.system.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrivateFileService {

    private static final long STORAGE_LIMIT_BYTES = 100L * 1024 * 1024;
    private static final List<PrivateFile> SAMPLE_FILES = List.of(
            new PrivateFile(
                    "perthlemon.jpg",
                    "JPEG image",
                    105_676,
                    "image",
                    "🍋"
            )
    );

    public PrivateFilesOverview overview() {
        long usedBytes = SAMPLE_FILES.stream().mapToLong(PrivateFile::sizeBytes).sum();
        return new PrivateFilesOverview(SAMPLE_FILES, usedBytes, STORAGE_LIMIT_BYTES);
    }

    public record PrivateFilesOverview(List<PrivateFile> files, long usedBytes, long limitBytes) {
        public PrivateFilesOverview {
            files = List.copyOf(files);
        }

        public double usedMegabytes() {
            return usedBytes / (1024.0 * 1024.0);
        }

        public double limitMegabytes() {
            return limitBytes / (1024.0 * 1024.0);
        }

        public int usedPercentage() {
            return (int) Math.min(100, usedBytes * 100 / limitBytes);
        }
    }

    public record PrivateFile(String name, String type, long sizeBytes, String kind, String previewSymbol) {
        public String displayName() {
            if (name.length() <= 15) {
                return name;
            }
            return name.substring(0, 12) + "...";
        }

        public String formattedSize() {
            if (sizeBytes < 1024) {
                return sizeBytes + " B";
            }
            return String.format("%.1f KB", sizeBytes / 1024.0);
        }
    }
}
