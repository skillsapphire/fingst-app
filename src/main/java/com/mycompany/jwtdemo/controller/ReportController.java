package com.mycompany.jwtdemo.controller;

import com.mycompany.jwtdemo.model.NotFiledDTO;
import com.mycompany.jwtdemo.repository.UserRepository;
import com.mycompany.jwtdemo.service.CustomUserDetailService;
import com.mycompany.jwtdemo.service.EmailService;
import com.mycompany.jwtdemo.service.GstAccountService;
import com.mycompany.jwtdemo.service.ReportService;
import com.mycompany.jwtdemo.util.ExcelHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/gst")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CustomUserDetailService customUserDetailService;

    @GetMapping(value = "/get-reports/{caId}")
    public ResponseEntity<HttpStatus> getReports(@RequestParam Integer fiscalYear, @RequestParam String month,
                                                 @RequestParam String returnType, @PathVariable Long caId){
        ByteArrayInputStream in = ExcelHelper.tutorialsToExcel(reportService.getReports(month, fiscalYear, returnType, caId));
        String filename = "filing_report_"+month+"_"+fiscalYear+".xlsx";
        String subject = "Filing Report " + month + "_" + fiscalYear;
        String body = "Hi, Your report is ready";
        emailService.sendMailWithAttachment(customUserDetailService.getEmail(caId), subject, body, filename, in);
        //emailService.sendMailWithAttachment("alishapatel2006@gmail.com", subject, body, filename, in);
        return ResponseEntity.ok().build();
        /*return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(file);*/
    }
}
