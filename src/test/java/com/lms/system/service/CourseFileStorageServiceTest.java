package com.lms.system.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourseFileStorageServiceTest {

    @TempDir
    Path storageRoot;

    @Test
    void storesFileUsingOpaqueKeyAndReadsOriginalBytes() throws IOException {
        CourseFileStorageService storage = new CourseFileStorageService(storageRoot.toString());
        byte[] content = "Course notes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        MockMultipartFile upload = new MockMultipartFile(
                "attachments", "week-1.txt", "text/plain", content);

        List<CourseFileStorageService.StoredCourseFile> stored = storage.storeAll(List.of(upload));

        assertEquals(1, stored.size());
        assertEquals("week-1.txt", stored.get(0).originalName());
        assertEquals(content.length, stored.get(0).sizeBytes());
        assertEquals(stored.get(0).storageKey(), UUID.fromString(stored.get(0).storageKey()).toString());
        assertArrayEquals(content, storage.read(stored.get(0).storageKey()));
    }

    @Test
    void removesEarlierFilesWhenALaterUploadHasInvalidContent() throws IOException {
        CourseFileStorageService storage = new CourseFileStorageService(storageRoot.toString());
        MockMultipartFile validText = new MockMultipartFile(
                "attachments", "notes.txt", "text/plain", "notes".getBytes());
        MockMultipartFile invalidPdf = new MockMultipartFile(
                "attachments", "fake.pdf", "application/pdf", "not a PDF".getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> storage.storeAll(List.of(validText, invalidPdf)));

        try (var files = Files.list(storageRoot)) {
            assertEquals(0, files.count());
        }
    }
}
