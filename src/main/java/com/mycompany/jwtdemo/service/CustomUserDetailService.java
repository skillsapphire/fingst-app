package com.mycompany.jwtdemo.service;

import com.mycompany.jwtdemo.entity.GstAccountEntity;
import com.mycompany.jwtdemo.entity.RoleEntity;
import com.mycompany.jwtdemo.entity.UserEntity;
import com.mycompany.jwtdemo.model.RoleModel;
import com.mycompany.jwtdemo.model.UserModel;
import com.mycompany.jwtdemo.repository.GstAccountRepository;
import com.mycompany.jwtdemo.repository.RoleRepository;
import com.mycompany.jwtdemo.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CustomUserDetailService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GstAccountRepository gstAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public void updateLastRefreshMasterData(List<Long> accounts, String fy){

        LocalDate ld = LocalDate.now();
        Integer currentYear = ld.getYear();

            // take the instant
            Instant instant = Instant.now();
            //https://stackoverflow.com/questions/59048196/to-display-local-date-time-for-different-countries-in-java
            // then in Asia/Calcutta
            ZonedDateTime currentISTime = instant.atZone(ZoneId.of("Asia/Calcutta"));
            List<GstAccountEntity> gstAccountEntityList = gstAccountRepository.findAllByIdIn(accounts);
            for(GstAccountEntity gae: gstAccountEntityList){
                if(currentYear.toString().equalsIgnoreCase(fy)){
                    gae.setLastRefreshedCurrFy(currentISTime);
                }else{
                    gae.setLastRefreshedPrevFy(currentISTime);
                }
            }
            gstAccountRepository.saveAll(gstAccountEntityList);
    }

    public UserModel register(UserModel userModel){

        UserEntity userEntity = new UserEntity();
        BeanUtils.copyProperties(userModel, userEntity);//it does not do a deep copy
        userEntity.setActive("N");
        userEntity.setUnmaskedpassword(userModel.getPassword());
        Set<RoleEntity> roleEntities = new HashSet<>();
        //fetch every role from DB based on role id and than set this role to user entity roles
        for(RoleModel rm :userModel.getRoles()){
            Optional<RoleEntity> optRole = roleRepository.findById(rm.getId());
            if(optRole.isPresent()){
                roleEntities.add(optRole.get());
            }
        }
        userEntity.setRoles(roleEntities);
        userEntity.setPassword(this.passwordEncoder.encode(userModel.getPassword()));
        userEntity = userRepository.save(userEntity);

        BeanUtils.copyProperties(userEntity, userModel);

        //convert RoleEntities to RoleModels
        Set<RoleModel> roleModels = new HashSet<>();
        RoleModel rm = null;
        for(RoleEntity re :userEntity.getRoles()){
            rm = new RoleModel();
            rm.setRoleName(re.getRoleName());
            rm.setId(re.getId());
            roleModels.add(rm);
        }
        userModel.setRoles(roleModels);
        return userModel;
    }

    public void updatePassword(Long caId, UserModel user){
        Optional<UserEntity> ue = userRepository.findById(caId);
        UserEntity entity = ue.get();
        entity.setUnmaskedpassword(user.getPassword());
        entity.setPassword(this.passwordEncoder.encode(user.getPassword()));
        userRepository.save(entity);
    }

    //this method actually does the validation for user existence
    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {

         UserEntity userEntity = userRepository.findByUsernameAndActiveContains(userName, "Y");

        if(userEntity == null){//here you can make a DB call with the help of repository and do the validation
            throw new UsernameNotFoundException("User does not exist!");
        }

        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(userEntity, userModel);
        ZonedDateTime istTime = userEntity.getLastRefreshed().withZoneSameInstant(ZoneId.of("Asia/Calcutta"));
        userModel.setLastRefreshed(istTime);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy@HH:mm:ss");
        String formattedString = istTime.format(formatter);
        userModel.setLastRefreshedFormatted(formattedString);

        istTime = userEntity.getLastRefreshedNotFiled().withZoneSameInstant(ZoneId.of("Asia/Calcutta"));
        formattedString = istTime.format(formatter);
        userModel.setLastRefreshedNotFiled(istTime);
        userModel.setLastRefreshedNotFiledFormatted(formattedString);

        //convert RoleEntities to RoleModels
        Set<RoleModel> roleModels = new HashSet<>();
        RoleModel rm = null;
        for(RoleEntity re :userEntity.getRoles()){
            rm = new RoleModel();
            rm.setRoleName(re.getRoleName());
            rm.setId(re.getId());
            roleModels.add(rm);
        }

        userModel.setRoles(roleModels);
        return userModel;
    }
}
