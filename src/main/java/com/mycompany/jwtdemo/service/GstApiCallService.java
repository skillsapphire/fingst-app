package com.mycompany.jwtdemo.service;

import com.mycompany.jwtdemo.client.feign.GstFeignClient;
import com.mycompany.jwtdemo.client.feign.model.GstDataModel;
import com.mycompany.jwtdemo.client.feign.model.GstWrapperModel;
import com.mycompany.jwtdemo.entity.GstFiledEntity;
import com.mycompany.jwtdemo.entity.NotFiledOverviewEntity;
import com.mycompany.jwtdemo.repository.GstFiledRepository;
import com.mycompany.jwtdemo.repository.GstNotFiledRepository;
import com.mycompany.jwtdemo.util.GstCommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class GstApiCallService {

    @Value("${gst.api.baseurl}")
    private String baseUrl;

    @Value("${gst.client.id}")
    private String clientId;

    @Value("${gst.client.sec}")
    private String clientSec;

    @Autowired
    private GstCommonUtil gstCommonUtil;

    @Autowired
    private GstFiledRepository filedRepository;

    @Autowired
    private GstNotFiledRepository gstNotFiledRepository;

    @Autowired
    private GstFeignClient gstFeignClient;

    @Autowired
    private RestTemplate restTemplate;

    public GstWrapperModel getAllFilings(String gstNo, String fy, String email){

        GstWrapperModel gstWrapperModel = null;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("client_id", clientId);
        headers.set("client_secret", clientSec);

        HttpEntity<GstWrapperModel> httpEntity = new HttpEntity<>(gstWrapperModel, headers);
        ResponseEntity<GstWrapperModel> gstEntity = restTemplate.exchange(baseUrl+"/rettrack-success.json?"+"gstin="+gstNo+"&fy="+fy+"&email=obify.consulting@gmail.com", HttpMethod.GET, httpEntity, GstWrapperModel.class);
        //ResponseEntity<GstWrapperModel> gstEntity = restTemplate.exchange(baseUrl+"/rettrack-success.json?"+"gstin="+gstNo+"&fy="+fy+"&email=obify.consulting@gmail.com", HttpMethod.GET, httpEntity, GstWrapperModel.class);

        if(gstEntity.getStatusCode() == HttpStatus.OK){
            gstWrapperModel = gstEntity.getBody();
        }

        return gstWrapperModel;
    }

    @Transactional//fy=2022
    public GstWrapperModel getAllFilingsWithFeign(String gstNo, String fy, String gstApiRtype, String type, String email) {
        System.out.println("======Starting getAllFilingsWithFeign Call Govt GST API - for FY - "+fy);
        GstWrapperModel gfwm = null;
        String apiUrl = "";
        try {
            //making actual Govt. GST API call
            /*if(type.equalsIgnoreCase("BOTH")) {
                gfwm = gstFeignClient.getAllFilings(gstNo, fy, email);
            }*/
            gfwm = gstFeignClient.getAllFilingsWithRtypeFilter(gstNo, gstCommonUtil.getFinancialYearValue(fy), gstApiRtype, email);

            //////Rest Template
            /*HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("client_id", clientId);
            headers.set("client_secret", clientSec);*/
            // "2021-22" => getFinancialYearValue("2022")
            // gstApiRtype -> R1 or R3B
            // type -> GSTR1 or GSTR3B
            /*apiUrl = "gstin="+gstNo+"&fy="+gstCommonUtil.getFinancialYearValue(fy)+"&type="+gstApiRtype+"&email=obify.consulting@gmail.com";
            HttpEntity<GstWrapperModel> httpEntity = new HttpEntity<>(gfwm, headers);
            ResponseEntity<GstWrapperModel> gstEntity = restTemplate.exchange(baseUrl+"/public/rettrack?"+apiUrl, HttpMethod.GET, httpEntity, GstWrapperModel.class);
            gfwm = gstEntity.getBody();*/
            ////End
            System.out.println("gstFeignClient call went well for fy "+fy);
        } catch (Exception ex) {
            System.out.println("Inside Refresh getAllFilingsWithFeign Exception");
            System.out.println(ex.getMessage());
            throw ex;
        }

        if(gfwm != null){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            //Converting every GST Filing entry for 1 GST Number for 1 FY into GstEntity and saving in Database
            if(gfwm.getData() != null) {
                //Create List of Filed Entities
                List<GstFiledEntity> filedEntities = new ArrayList<>();
                GstFiledEntity gstFiledEntity = null;

                List<NotFiledOverviewEntity> notFiledEntities = new ArrayList<>();
                NotFiledOverviewEntity notFiledOverviewEntity = null;
                List<String> filedFyList = new ArrayList<>();
                /***********
                 * Dummy
                 * SELECT * FROM `gst_filed_tracker` WHERE gst_no LIKE '%21BJHPP8268E1Z3%' AND return_type LIKE '%GSTR1%';
                 */
                //filedFyList.addAll(Arrays.asList("012022", "092021", "052021", "082021", "072021", "042021", "062021", "032021"));
                for (GstDataModel gdm : gfwm.getData().getEFiledlist()) {
                    filedFyList.add(gdm.getRet_prd());
                    //create filed entity
                    gstFiledEntity = new GstFiledEntity();
                    gstFiledEntity.setReturnType(gdm.getRtntype());
                    gstFiledEntity.setGstNo(gfwm.getHeader().getGstin());
                    LocalDate localDate = LocalDate.parse(gdm.getDof(), formatter);
                    gstFiledEntity.setDateOfFiling(localDate);
                    gstFiledEntity.setReturnPeriod(gstCommonUtil.convertReturnPeriodDate(gdm.getRet_prd()));
                    gstFiledEntity.setStatus(gdm.getStatus());
                    gstFiledEntity.setMode(gdm.getMof());
                    filedEntities.add(gstFiledEntity);
                }
                //delete all filed entries for this gst and return type
                filedRepository.deleteAllByGstNoAndReturnType(gstNo, type);//GSTR1
                //saveAll(filedEntityList)
                filedRepository.saveAll(filedEntities);
                //Subtract the two list to get list of not filed periods
                List<String> staticListRetPeriod = gstCommonUtil.listOfStaticFy(fy);
                staticListRetPeriod.removeAll(filedFyList);
                //Create List of Not Filed Entities
                for(String retPeriod: staticListRetPeriod){//now it has only not file ret periods
                    notFiledOverviewEntity = new NotFiledOverviewEntity();
                    notFiledOverviewEntity.setGstNo(gstNo);
                    notFiledOverviewEntity.setIsGstFiled(false);
                    notFiledOverviewEntity.setReturnType(type);
                    notFiledOverviewEntity.setDateOfGstFiling(gstCommonUtil.convertReturnPeriodDate(retPeriod));
                    notFiledEntities.add(notFiledOverviewEntity);
                }
                //delete all filed entries for this gst and return type
                gstNotFiledRepository.deleteAllByGstNoAndReturnType(gstNo, type);//GSTR1
                //saveAll(notFiledEntityList)
                gstNotFiledRepository.saveAll(notFiledEntities);

                System.out.println("Deletion and Updation Done for gstNo: "+gstNo);
            }
            System.out.println("======Done getAllFilingsWithFeign Call Govt GST API - for FY - "+fy);
        }
        //log after successful insertion of all records for 1 GST number
        return gfwm;
    }

}
