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
    private CustomUserDetailService customUserDetailService;

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
            List<GstAccountEntity> gstAccountEntityList = null;
            if(accounts.isEmpty()){
                gstAccountEntityList = overviewService.getGstAccounts(caId);
            }else{
                gstAccountEntityList = gstAccountRepository.findAllByIdIn(accounts);
            }

            String gstApiRtype = "BOTH";
            if(type.equalsIgnoreCase("GSTR1")){
                gstApiRtype = "R1";
            }else if(type.equalsIgnoreCase("GSTR1")){
                gstApiRtype = "R3B";
            }
            System.out.println("Starting Batch Call Govt GST API");
            gstMasterDataService.performBatchWithFilter(gstAccountEntityList, fy, gstApiRtype);
            System.out.println("End Batch Call Govt GST API");
            System.out.println("Starting updateNotFiledOverview");
            overviewService.updateNotFiledOverview(gstAccountEntityList, type);
            System.out.println("Ending updateNotFiledOverview");
            customUserDetailService.updateLastRefreshMasterData(accounts, fy);
            System.out.println("Done updateLastRefreshMasterData");
            gstMasterDataService.sendEmail("success");
            System.out.println("Done sendEmail success");
        } catch (Exception ex) {
            System.out.println("Inside Refresh Data Exception");
            gstMasterDataService.sendEmail("error");
            System.out.println("Done sendEmail error");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

}
