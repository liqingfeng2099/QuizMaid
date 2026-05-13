package com.kanade.backend.service;

import com.kanade.backend.model.vo.PersonalStatsVO;

public interface PersonalStatsService {

    PersonalStatsVO getPersonalStats(String subject, String startDate, String endDate);

    PersonalStatsVO getPersonalStatsWithPeriod(String subject, String period);
}
