package com.likelion.firstbite.firstbiteserver.recognition.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class RecognitionConfig {

    /**
     * 사진 인식 비동기 처리 전용 스레드풀. 기본 executor(무제한 스레드) 대신 상한을 둬서
     * OpenAI 호출이 몰릴 때 스레드 폭증을 막고 초과분은 큐잉한다.
     */
    @Bean("recognitionExecutor")
    Executor recognitionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("recognition-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    /**
     * S3 호환 스토리지 클라이언트.
     * - app.aws.s3-endpoint 가 설정되면 해당 엔드포인트(예: 사내 MinIO)로 연결하고 path-style 접근을 사용한다.
     * - access-key/secret-key 가 있으면 정적 자격증명을 사용한다(없으면 기본 자격증명 체인).
     * 엔드포인트가 비어 있으면 실제 AWS S3로 동작한다(설정만 바꾸면 이전 가능).
     */
    @Bean
    S3Client s3Client(@Value("${app.aws.region:ap-northeast-2}") String region,
                      @Value("${app.aws.s3-endpoint:}") String endpoint,
                      @Value("${app.aws.access-key:}") String accessKey,
                      @Value("${app.aws.secret-key:}") String secretKey) {
        var builder = S3Client.builder().region(Region.of(region));
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
        }
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)));
        }
        return builder.build();
    }
}
