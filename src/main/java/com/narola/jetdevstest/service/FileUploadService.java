package com.narola.jetdevstest.service;

import com.narola.jetdevstest.exception.CustomException;
import com.narola.jetdevstest.model.Files;
import com.narola.jetdevstest.model.Records;
import com.narola.jetdevstest.repository.FileUploadRepository;
import com.narola.jetdevstest.repository.RecordsRepository;
import com.narola.jetdevstest.service.component.ExcelToCSVConverter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileUploadService {

    private final FileUploadRepository fileUploadRepository;
    private final RecordsRepository recordsRepository;
    private final FileUploadProgressService fileUploadProgressService;

    public FileUploadService(FileUploadRepository fileUploadRepository,
                             RecordsRepository recordsRepository,
                             FileUploadProgressService fileUploadProgressService) {
        this.fileUploadRepository = fileUploadRepository;
        this.recordsRepository = recordsRepository;
        this.fileUploadProgressService = fileUploadProgressService;
    }

    public void processFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new CustomException("File is empty");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new CustomException("Invalid file: Filename is missing");
        }

        if (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx")) {
            throw new CustomException("Invalid file type. Please upload a valid Excel file.");
        }

        // Initialize and save file metadata
        Files files = initializeAndSaveFileMetadata(file);

        // Convert Excel to CSV InputStream
        try (InputStream csvInputStream = ExcelToCSVConverter.convertExcelToCSVInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(csvInputStream));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            List<CSVRecord> records = csvParser.getRecords();
            int totalRows = records.size();
            if (totalRows == 0) {
                throw new CustomException("The uploaded file is empty or has no valid records.");
            }

            // Process records in batches
            processCSVRecordsInBatches(files, records, totalRows);

            // Mark upload completion
            fileUploadProgressService.updateProgress(String.valueOf(files.getId()), 100); // Set progress to 100%
        } catch (Exception e) {
            // Log error for debugging and throw to notify caller
            e.printStackTrace();
            throw new CustomException("Error processing file: " + e.getMessage());
        }
    }

    private Files initializeAndSaveFileMetadata(MultipartFile file) {
        Files files = new Files();
        files.setFileName(file.getOriginalFilename());
        files.setUploadTime(LocalDateTime.now());
        files.setAccessTime(LocalDateTime.now());
        fileUploadRepository.save(files);
        fileUploadProgressService.updateProgress(String.valueOf(files.getId()), 0); // Initialize progress as 0
        return files;
    }

    private void processCSVRecordsInBatches(Files files, List<CSVRecord> records, int totalRows) {
        int processedRows = 0;

        for (CSVRecord csvRecord : records) {
            // Convert CSV record to map
            Map<String, Object> recordMap = convertRecordToMap(csvRecord);

            // Save record
            Records recordEntity = new Records();
            recordEntity.setFile(files);
            recordEntity.setRecord(recordMap);
            recordsRepository.save(recordEntity);

            // Update progress in batches
            processedRows++;
            if (processedRows % 10 == 0 || processedRows == totalRows) {
                int progress = (processedRows * 100) / totalRows; // Calculate percentage
                fileUploadProgressService.updateProgress(String.valueOf(files.getId()), progress);
            }
        }
    }

    private Map<String, Object> convertRecordToMap(CSVRecord csvRecord) {
        Map<String, Object> recordMap = new HashMap<>();
        for (String header : csvRecord.toMap().keySet()) {
            recordMap.put(header, csvRecord.get(header));
        }
        return recordMap;
    }

    @Transactional
    public void deleteFile(Long fileId) {
        recordsRepository.deleteByFile_Id(fileId);  // Delete all records associated with the file
        if (fileUploadRepository.existsById(fileId))
            fileUploadRepository.deleteById(fileId);
        fileUploadProgressService.clearProgress(String.valueOf(fileId)); // Clear progress for the file
    }
}

