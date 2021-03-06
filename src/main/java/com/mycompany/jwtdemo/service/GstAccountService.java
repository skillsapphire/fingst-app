package com.mycompany.jwtdemo.service;

import com.mycompany.jwtdemo.entity.GstAccountEntity;
import com.mycompany.jwtdemo.entity.UserEntity;
import com.mycompany.jwtdemo.model.GstAccountDTO;
import com.mycompany.jwtdemo.repository.GstAccountRepository;
import com.mycompany.jwtdemo.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GstAccountService {

    @Autowired
    private GstAccountRepository gstAccountRepository;

    public GstAccountDTO saveGstAccount(GstAccountDTO gstAccountDTO){

        GstAccountEntity gstAccountEntity = new GstAccountEntity();
        BeanUtils.copyProperties(gstAccountDTO, gstAccountEntity);
        gstAccountEntity.setCreationDate(LocalDate.now());
        gstAccountEntity.setActive("Y");

        GstAccountEntity gse = gstAccountRepository.save(gstAccountEntity);
        BeanUtils.copyProperties(gse, gstAccountDTO);

        return gstAccountDTO;
    }

    public GstAccountDTO getAccountDetail(Long accountId){
        Optional<GstAccountEntity> optge = gstAccountRepository.findById(accountId);
        GstAccountDTO gstAccountDTO = null;
        if(optge.isPresent()){
            gstAccountDTO = new GstAccountDTO();
            BeanUtils.copyProperties(optge.get(), gstAccountDTO);
        }
        return gstAccountDTO;
    }

    public GstAccountDTO updateAccount(GstAccountDTO gstAccountDTO, Long accountId){
        Optional<GstAccountEntity> optge = gstAccountRepository.findById(accountId);
        if(optge.isPresent()){
            GstAccountEntity ge = optge.get();
            BeanUtils.copyProperties(gstAccountDTO, ge);
            ge.setCreationDate(LocalDate.now());
            ge = gstAccountRepository.save(ge);
            BeanUtils.copyProperties(ge, gstAccountDTO);
        }
        return gstAccountDTO;
    }

    public GstAccountDTO deleteUpdateAccount(Long accountId){
        Optional<GstAccountEntity> optge = gstAccountRepository.findById(accountId);
        GstAccountDTO gstAccountDTO = null;
        if(optge.isPresent()){
            GstAccountEntity ge = optge.get();
            ge.setActive("N");
            ge = gstAccountRepository.save(ge);
            gstAccountDTO = new GstAccountDTO();
            BeanUtils.copyProperties(ge, gstAccountDTO);
        }
        return gstAccountDTO;
    }

    public List<GstAccountDTO> getAllMyGstAccounts(Long caId, Pageable pageable){

        List<GstAccountEntity> gstAccountEntities = null;
        Page<GstAccountEntity> pagedAccounts = gstAccountRepository.findByCaIdAndActiveContainsOrderByFirmName(caId, "Y", pageable);

        gstAccountEntities = pagedAccounts.getContent();

        List<GstAccountDTO> gstAccountDTOList = new ArrayList<>();
        GstAccountDTO gstAccountDTO = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy@HH:mm:ss");
        ZonedDateTime istTime = null;
        String formattedString = null;

        for(GstAccountEntity gae: gstAccountEntities){
            gstAccountDTO = new GstAccountDTO();
            BeanUtils.copyProperties(gae, gstAccountDTO);
            if(gae.getLastRefreshed() != null) {
                istTime = gae.getLastRefreshed().withZoneSameInstant(ZoneId.of("Asia/Calcutta"));
                gstAccountDTO.setLastRefreshed(istTime);
                formattedString = istTime.format(formatter);
                gstAccountDTO.setLastRefreshedFormatted(formattedString);
            }
            if(gae.getLastRefreshedCurrFy() != null) {
                istTime = gae.getLastRefreshedCurrFy().withZoneSameInstant(ZoneId.of("Asia/Calcutta"));
                gstAccountDTO.setLastRefreshedCurrFy(istTime);
                formattedString = istTime.format(formatter);
                gstAccountDTO.setLastRefreshedCurrFyFormatted(formattedString);
            }
            if(gae.getLastRefreshedPrevFy() != null) {
                istTime = gae.getLastRefreshedPrevFy().withZoneSameInstant(ZoneId.of("Asia/Calcutta"));
                gstAccountDTO.setLastRefreshedPrevFy(istTime);
                formattedString = istTime.format(formatter);
                gstAccountDTO.setLastRefreshedPrevFyFormatted(formattedString);
            }
            gstAccountDTOList.add(gstAccountDTO);
        }

        return gstAccountDTOList;
    }
}
