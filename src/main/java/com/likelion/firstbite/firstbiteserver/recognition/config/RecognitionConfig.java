package com.likelion.firstbite.firstbiteserver.recognition.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableAsync
public class RecognitionConfig {
    @Bean
    S3Client s3Client(@Value("${app.aws.region:ap-northeast-2}") String region) {
        return S3Client.builder().region(Region.of(region)).build();
    }
}
