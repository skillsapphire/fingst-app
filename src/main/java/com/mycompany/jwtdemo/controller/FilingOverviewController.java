package com.mycompany.jwtdemo.controller;

import com.mycompany.jwtdemo.entity.GstAccountEntity;
import com.mycompany.jwtdemo.entity.UserEntity;
import com.mycompany.jwtdemo.model.FilingOverviewDTO;
import com.mycompany.jwtdemo.repository.GstAccountRepository;
import com.mycompany.jwtdemo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/gst")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class FilingOverviewController {

    @Autowired
    private FilingOverviewService overviewService;

    @Autowired
    private GstAccountService gstAccountService;

    @Autowired
    private GstAccountRepository gstAccountRepository;

    @Autowired
    private GstMasterDataService gstMasterDataService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CustomUserDetailService userDetailService;

    @GetMapping(value = "/get-filing-det/{caId}")
    public ResponseEntity<List<FilingOverviewDTO>> getFilingOverview(@PathVariable Long caId, @RequestParam String fy, @RequestParam String nfGstr1){

        List<FilingOverviewDTO> filingOverviewDTOS = null;

        if(nfGstr1.equalsIgnoreCase("BOTH")){
            filingOverviewDTOS = overviewService.getFilingOverview(caId, fy);
        }else if(nfGstr1.equalsIgnoreCase("YES")){//get only not filed(if 1 month is also not filed then also get the account) gstr1 accounts
            filingOverviewDTOS = overviewService.getFilingOverviewWithGST1Filter(caId, fy, "YES");
        }else if(nfGstr1.equalsIgnoreCase("NO")){//Only get those accounts whose gst is filed for all months till now
            filingOverviewDTOS = overviewService.getFilingOverviewWithGST1Filter(caId, fy, "NO");
        }
        return ResponseEntity.ok(filingOverviewDTOS);
    }

    @GetMapping("/refresh-master-data-with-filter/{caId}")
    public void refreshMasterDataWithFilter(@PathVariable Long caId, @RequestParam List<Long> accounts, @RequestParam("fy") String fy, @RequestParam("type") String type) {
        try {
            List<GstAccountEntity> gstAccountEntityList;
            gstAccountEntityList = gstAccountRepository.findAllByIdIn(accounts);

            String gstApiRtype = "";
            if(type.equalsIgnoreCase("GSTR1")){
                gstApiRtype = "R1";
            }else if(type.equalsIgnoreCase("GSTR3B")){
                gstApiRtype = "R3B";
            }
            System.out.println("Starting Batch Call Govt GST API");
            gstMasterDataService.performBatchWithFilter(gstAccountEntityList, fy, gstApiRtype, type, accounts);
            emailService.sendMail(userDetailService.getEmail(caId),
                    "Successfully refreshed Master Data",
                    "Successfully refreshed Master Data");
            System.out.println("Done sendEmail success");
            System.out.println("End Batch Call Govt GST API");

        } catch (Exception ex) {
            System.out.println("Inside Refresh Data Exception");
            emailService.sendMail(userDetailService.getEmail(caId),
                    "Error refreshing Master Data",
                    "Some error occurred while refreshing Master Data");
            System.out.println("Done sendEmail error");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

}
