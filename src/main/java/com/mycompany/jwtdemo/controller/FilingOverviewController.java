package com.mycompany.jwtdemo.controller;

import com.mycompany.jwtdemo.entity.GstAccountEntity;
import com.mycompany.jwtdemo.model.FilingOverviewDTO;
import com.mycompany.jwtdemo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gst")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class FilingOverviewController {

    @Autowired
    private FilingOverviewService overviewService;

    @Autowired
    private GstAccountService gstAccountService;

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

    @GetMapping("/refresh-master-data/{caId}")
    public void refreshMasterData(@PathVariable Long caId){
        try {
            List<GstAccountEntity> gstAccountEntityList = overviewService.getGstAccounts(caId);
            System.out.println("Starting Batch Call Govt GST API - ms - " + System.currentTimeMillis());
            gstMasterDataService.performBatch(gstAccountEntityList);
            System.out.println("Done with Batch Call Govt GST API - ms - " + System.currentTimeMillis());
            System.out.println("Starting updateNotFiledOverview - ms - " + System.currentTimeMillis());
            overviewService.updateNotFiledOverview(gstAccountEntityList);
            System.out.println("Done updateNotFiledOverview - ms - " + System.currentTimeMillis());
            customUserDetailService.updateLastRefreshMasterData(caId);
        }catch (Exception ex){
            System.out.println("Inside Refresh Data Exception");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
}
