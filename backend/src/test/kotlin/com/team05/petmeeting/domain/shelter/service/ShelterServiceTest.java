package com.team05.petmeeting.domain.shelter.service;

import com.team05.petmeeting.domain.animal.service.AnimalExternalService;
import com.team05.petmeeting.domain.shelter.dto.ShelterCommand;
import com.team05.petmeeting.domain.shelter.entity.Shelter;
import com.team05.petmeeting.domain.shelter.repository.ShelterRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class ShelterServiceTest {

    @MockitoBean
    AnimalExternalService animalExternalService;  // 외부 의존 mock 처리

    @Autowired
    ShelterService shelterService;

    @Autowired
    ShelterRepository shelterRepository;

    @Test
    @DisplayName("보호소 생성")
    public void createShelter() {
        // given
        ShelterCommand cmd = new ShelterCommand(
            "123",
            "보호소1",
            "010",
            "주소",
            "소유자",
            "기관",
            LocalDateTime.now()
        );

        // when
        shelterService.createOrUpdateShelter(cmd);

        // then
        Shelter result = shelterRepository.findById("123").orElseThrow();
        assertThat(result.getCareNm()).isEqualTo("보호소1");
    }

    @Test
    @DisplayName("보호소 정보 업데이트")
    public void updateShelter(){
        // given
        LocalDateTime oldTime = LocalDateTime.of(2026, 1, 1, 0, 0); // 예전 보호소 업데이트 시간
        LocalDateTime newTime = LocalDateTime.of(2026, 2, 1, 0, 0); // 신규 보호소 업데이트 시간

        // 보호소 예전 데이터
        ShelterCommand oldCmd = new ShelterCommand("123", "보호소1", "01011111111", "주소1", "성함1", "도시1", oldTime);
        shelterRepository.save(Shelter.create(oldCmd));

        // when
        // 보호소 새 데이터
        ShelterCommand newCmd = new ShelterCommand("123", "보호소1_수정", "01022222222", "주소2", "성함2", "도시2", newTime);
        shelterService.createOrUpdateShelter(newCmd);

        // then
        Shelter result = shelterRepository.findById("123").orElseThrow();
        assertThat(result.getCareNm()).isEqualTo("보호소1_수정");
    }

}