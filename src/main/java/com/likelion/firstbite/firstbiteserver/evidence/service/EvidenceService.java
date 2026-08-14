package com.likelion.firstbite.firstbiteserver.evidence.service;

import com.likelion.firstbite.firstbiteserver.analysis.repository.MealAnalysisRepository;
import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.evidence.dto.EvidenceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EvidenceService {
    private static final Set<String> TYPES = Set.of("NUTRITION", "GI", "ORDER_EFFECT", "CALCULATION");
    private static final List<EvidenceResponse.Source> SOURCES = List.of(
            new EvidenceResponse.Source("ev-nutrition-1", "NUTRITION", "식품영양성분 데이터베이스",
                    List.of("식품의약품안전처"), 2026, "https://various.foodsafetykorea.go.kr/nutrient/",
                    "1인분 영양성분 매핑"),
            new EvidenceResponse.Source("ev-gi-1", "GI", "한국인 다소비 탄수화물 식품 GI 실측 연구",
                    List.of("농촌진흥청"), 2019, "https://www.rda.go.kr/", "실측 GI 값 우선 적용"),
            new EvidenceResponse.Source("ev-order-1", "ORDER_EFFECT", "영양소 섭취 순서와 식후 혈당 반응 연구",
                    List.of("연속혈당측정 기반 연구진"), 2019, "https://pubmed.ncbi.nlm.nih.gov/",
                    "단백질 우선 코칭과 단계 간격 근거"),
            new EvidenceResponse.Source("ev-calculation-1", "CALCULATION", "국제 표준 혈당부하 계산식",
                    List.of("FirstBite 산식 문서"), 2026, null, "GL 및 상대 부담지수 계산"));

    private final MealAnalysisRepository analysisRepository;

    @Transactional(readOnly = true)
    public EvidenceResponse get(UUID memberId, UUID analysisId, String type) {
        if (type != null && !TYPES.contains(type)) throw new BusinessException(HttpStatus.BAD_REQUEST,
                "EVIDENCE_FILTER_INVALID", "지원하지 않는 근거 유형입니다.");
        if (analysisId != null && analysisRepository.findByIdAndMemberId(analysisId, memberId).isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "ANALYSIS_NOT_FOUND", "분석 결과를 찾을 수 없습니다.");
        }
        var filtered = SOURCES.stream().filter(source -> type == null || source.type().equals(type)).toList();
        return new EvidenceResponse(filtered,
                new EvidenceResponse.Calculation("GI × availableCarbohydrateG / 100",
                        "carbohydrateG - fiberG"),
                "의료 진단·처방 또는 개인 혈당 예측 서비스가 아닙니다.");
    }
}
