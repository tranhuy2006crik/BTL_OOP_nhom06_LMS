package com.lms.system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CourseFileStorageService {

    private static final System.Logger LOGGER = System.getLogger(CourseFileStorageService.class.getName());

    public static final int MAX_FILES_PER_UPLOAD = 5;
    public static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;
    public static final long MAX_TOTAL_SIZE_BYTES = 50L * 1024 * 1024;

    private final Path storageRoot;

    public CourseFileStorageService(
            @Value("${lms.course-files.storage-directory:./data/course-files}") String storageDirectory
    ) {
        storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public List<StoredCourseFile> storeAll(List<MultipartFile> uploads) throws IOException {
        List<MultipartFile> selected = uploads == null ? List.of()
                : uploads.stream().filter(file -> file != null && !file.isEmpty()).toList();
        if (selected.size() > MAX_FILES_PER_UPLOAD) {
            throw new IllegalArgumentException("Tối đa 5 tệp trong một lần tải lên");
        }

        long totalBytes = selected.stream().mapToLong(MultipartFile::getSize).sum();
        if (totalBytes > MAX_TOTAL_SIZE_BYTES) {
            throw new IllegalArgumentException("Tổng dung lượng tệp không được vượt quá 50 MB");
        }

        List<StoredCourseFile> storedFiles = new ArrayList<>();
        try {
            for (MultipartFile upload : selected) {
                storedFiles.add(store(upload));
            }
            return List.copyOf(storedFiles);
        } catch (IOException | RuntimeException exception) {
            deleteAll(storedFiles, exception);
            throw exception;
        }
    }

    public byte[] read(String storageKey) throws IOException {
        return Files.readAllBytes(resolve(storageKey));
    }

    public void deleteAll(List<StoredCourseFile> storedFiles, Throwable originalFailure) {
        for (StoredCourseFile storedFile : storedFiles) {
            try {
                Files.deleteIfExists(resolve(storedFile.storageKey()));
            } catch (IOException cleanupFailure) {
                originalFailure.addSuppressed(cleanupFailure);
                LOGGER.log(System.Logger.Level.ERROR, "Could not clean up stored course file", cleanupFailure);
            }
        }
    }

    private StoredCourseFile store(MultipartFile upload) throws IOException {
        if (upload.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Mỗi tệp không được vượt quá 20 MB");
        }
        String originalName = safeFileName(upload.getOriginalFilename());
        String extension = extensionOf(originalName);
        String mimeType = mimeTypeFor(extension);
        byte[] content = upload.getBytes();
        if (content.length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Mỗi tệp không được vượt quá 20 MB");
        }
        validateContent(extension, content);

        String storageKey = UUID.randomUUID().toString();
        Path target = resolve(storageKey);
        Files.createDirectories(storageRoot);
        Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return new StoredCourseFile(storageKey, originalName, mimeType, content.length);
    }

    private Path resolve(String storageKey) {
        Path resolved = storageRoot.resolve(storageKey).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return resolved;
    }

    private static String safeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("Tên tệp không hợp lệ");
        }
        String normalized = originalName.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (fileName.isEmpty() || fileName.length() > 255
                || fileName.chars().anyMatch(Character::isISOControl)
                || fileName.equals(".") || fileName.equals("..")) {
            throw new IllegalArgumentException("Tên tệp không hợp lệ");
        }
        return fileName;
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new IllegalArgumentException("Chỉ hỗ trợ PDF, PowerPoint, Word, ODP, ODT và TXT");
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String mimeTypeFor(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "odp" -> "application/vnd.oasis.opendocument.presentation";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "odt" -> "application/vnd.oasis.opendocument.text";
            case "txt" -> "text/plain";
            default -> throw new IllegalArgumentException(
                    "Chỉ hỗ trợ PDF, PowerPoint, Word, ODP, ODT và TXT");
        };
    }

    private static void validateContent(String extension, byte[] content) {
        if (content.length == 0) {
            throw new IllegalArgumentException("Tệp tải lên đang trống");
        }
        boolean valid = switch (extension) {
            case "pdf" -> startsWith(content, new byte[]{'%', 'P', 'D', 'F', '-'});
            case "ppt", "doc" -> startsWith(content,
                    new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1});
            case "pptx", "odp", "docx", "odt" -> startsWith(content, new byte[]{'P', 'K', 0x03, 0x04});
            case "txt" -> !containsNullByte(content);
            default -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException("Định dạng nội dung không khớp với phần mở rộng tệp");
        }
    }

    private static boolean startsWith(byte[] content, byte[] prefix) {
        if (content.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (content[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsNullByte(byte[] content) {
        for (byte value : content) {
            if (value == 0) {
                return true;
            }
        }
        return false;
    }

    public record StoredCourseFile(String storageKey, String originalName, String mimeType, long sizeBytes) {
    }
}
