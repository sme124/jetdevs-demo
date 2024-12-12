package com.narola.jetdevstest.service;

import com.narola.jetdevstest.model.Files;
import com.narola.jetdevstest.model.Records;
import com.narola.jetdevstest.repository.FileUploadRepository;
import com.narola.jetdevstest.repository.RecordsRepository;
import com.narola.jetdevstest.service.component.ExcelToCSVConverter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class FileUploadServiceTest {

    @Mock
    private FileUploadRepository fileUploadRepository;

    @Mock
    private RecordsRepository recordsRepository;

    @Mock
    private FileUploadProgressService fileUploadProgressService;

    @Mock
    private MultipartFile mockFile;

    @InjectMocks
    private FileUploadService fileUploadService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessFile_EmptyFile() throws Exception {
        when(mockFile.isEmpty()).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            fileUploadService.processFile(mockFile);
        });

        assertEquals("File is empty", exception.getMessage());
    }

    @Test
    void testProcessFile_InvalidFileType() throws Exception {
        // Simulate an invalid file type (e.g., .txt or .csv)
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.txt");

        // Assert that the correct exception is thrown
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> fileUploadService.processFile(mockFile));

        assertEquals("Invalid file type. Please upload a valid Excel file.", exception.getMessage());
    }

    @Test
    void testProcessFile_Success() throws Exception {
        // Set up mock file and dependencies
        Files file = new Files();
        file.setFileName("test.csv");
        file.setUploadTime(LocalDateTime.now());
        file.setAccessTime(LocalDateTime.now());

        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.xlsx");
        when(fileUploadRepository.save(any(Files.class))).thenReturn(file);
        when(recordsRepository.save(any(Records.class))).thenReturn(new Records());

        // Generate a valid .xlsx file in-memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("header1");
            headerRow.createCell(1).setCellValue("header2");
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("value1");
            dataRow.createCell(1).setCellValue("value2");

            workbook.write(baos);
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(baos.toByteArray());
        when(mockFile.getInputStream()).thenReturn(inputStream);

        fileUploadService.processFile(mockFile);

        // Verify interactions
        verify(fileUploadRepository, atLeast(1)).save(any(Files.class));
        verify(fileUploadProgressService).updateProgress(anyString(), eq(0));  // Initial progress
        verify(fileUploadProgressService, atLeast(1)).updateProgress(anyString(), eq(100));  // Final progress
        verify(recordsRepository, times(1)).save(any(Records.class));  // Verify that records were saved
    }

    @Test
    void testDeleteFile() {
        Long fileId = 1L;

        // Mocks
        doNothing().when(recordsRepository).deleteByFile_Id(fileId);
        when(fileUploadRepository.existsById(fileId)).thenReturn(true);
        doNothing().when(fileUploadRepository).deleteById(fileId);
        doNothing().when(fileUploadProgressService).clearProgress(anyString());

        // Call delete method
        fileUploadService.deleteFile(fileId);

        // Verify interactions
        verify(recordsRepository).deleteByFile_Id(fileId);
        verify(fileUploadRepository).deleteById(fileId);
        verify(fileUploadProgressService).clearProgress(anyString());
    }
}
