package com.semicolon.spring.service.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import com.semicolon.spring.dto.HeadDTO;
import com.semicolon.spring.entity.user.User;
import com.semicolon.spring.entity.user.UserRepository;
import com.semicolon.spring.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcmService {

    private final UserRepository userRepository;
    private static final String FIREBASE_CONFIG_PATH = "ddyzd-firebase-adminsdk.json";

    @PostConstruct
    public void initialize(){
        try{
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new ClassPathResource(FIREBASE_CONFIG_PATH).getInputStream())).build();
            if(FirebaseApp.getApps().isEmpty()){
                FirebaseApp.initializeApp(options);
                log.info("Firebase application has been initialized");
            }
        }catch (IOException e){
            log.error(e.getMessage());
        }
    }

    public void send(HeadDTO.FcmRequest request) {
        try{
            if(request.getToken()!=null){
                Message message = Message.builder()
                        .setToken(request.getToken())
                        .putData("club_id", request.getClub().toString())
                        .setNotification(Notification.builder() // setImage추가하기.
                                .setTitle(request.getTitle())
                                .setBody(request.getMessage())
                                .build()
                        )
                        .setApnsConfig(ApnsConfig.builder()
                                .setAps(Aps.builder()
                                        .setSound("default")
                                        .build()
                                ).build()
                        )
                        .build();


                String response = FirebaseMessaging.getInstance().sendAsync(message).get();
                log.info("Sent Message" + response);
            }

        }catch (Exception e){
            log.error(e.getMessage());
            throw new NotFoundException();
        }

    }

    @Scheduled(cron = "0 45 8 * * *", zone = "Asia/Seoul")
    public void sendSelfDiagnosis(){
        for(User user : userRepository.findAll()){
            if(user.getDevice_token() != null){
                this.send(HeadDTO.FcmRequest.builder()
                        .title(user.getName() + "님 자가진단 해주세요.")
                        .club(19)
                        .token(user.getDevice_token())
                        .message("자가진단 실시해주세요.")
                        .build()
                );
            }
        }
    }

}
