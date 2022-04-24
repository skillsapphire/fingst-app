package com.mycompany.jwtdemo.service;

import com.mycompany.jwtdemo.entity.GstAccountEntity;
import com.mycompany.jwtdemo.entity.UserEntity;
import com.mycompany.jwtdemo.repository.GstAccountRepository;
import com.mycompany.jwtdemo.repository.GstFiledRepository;
import com.mycompany.jwtdemo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class GstMasterDataService {

    @Autowired
    private GstFiledRepository filedRepository;
    @Autowired
    private GstApiCallService gstApiCallService;
    @Autowired
    private GstAccountRepository gstAccountRepository;
    @Autowired
    private FilingOverviewService overviewService;
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CustomUserDetailService customUserDetailService;

    public String getFinancialYear(String fyType){
        LocalDate ld = LocalDate.now();
        Integer currentYear = ld.getYear();
        Integer prevYear = currentYear - 1;
        if(fyType.equalsIgnoreCase("previous")){
            prevYear = prevYear-1;
            currentYear = currentYear-1;
        }
        return prevYear+"-"+currentYear.toString().substring(2);
    }

    //https://stackoverflow.com/questions/7979165/spring-cron-expression-for-every-after-30-minutes
    //@Scheduled(cron = "0 0/5 * * * ?")//every 5 min
    public void scheduleGetFilings(){
        System.out.println("*******Scheduler Started***********");
        //Delete all rows first than insert
        filedRepository.deleteAll();
        //Read all GST number from GstAccount table
        List<GstAccountEntity> allGstAccEntities = gstAccountRepository.findAll();
        //performBatch(allGstAccEntities);
        overviewService.updateNotFiledOverview(allGstAccEntities, "BOTH");
        System.out.println("*******Scheduler Iteration Ended***********");
    }

    @Transactional
    public void performBatchWithFilter(List<GstAccountEntity> allGstAccEntities, String fy, String gstApiRtype, String type, List<Long> accounts){
        try {
            if(allGstAccEntities != null && !allGstAccEntities.isEmpty()) {
                for (GstAccountEntity gae : allGstAccEntities) {//fy=2022
                    gstApiCallService.getAllFilingsWithFeign(gae.getGstNo(), fy, gstApiRtype, type, "obify.consulting@gmail.com");
                }
                customUserDetailService.updateLastRefreshMasterData(accounts, fy);
                System.out.println("Done updateLastRefreshMasterData");
                //this.sendEmail("success");
                System.out.println("Done sendEmail success");
            }
        }catch (Exception ex){
            throw ex;
        }
    }


    public ResponseEntity<String> sendEmail(String type){
        if(type.equalsIgnoreCase("success")){
            return restTemplate.getForEntity("http://obify.in/apis/email.php/filing-gst-success", String.class);
        }else{
            return restTemplate.getForEntity("http://obify.in/apis/email.php/filing-gst-error", String.class);
        }
    }
}
