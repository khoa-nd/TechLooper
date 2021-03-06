package com.techlooper.service.impl;

import com.techlooper.entity.userimport.UserImportEntity;
import com.techlooper.model.SocialProvider;
import com.techlooper.model.Talent;
import com.techlooper.model.TalentSearchRequest;
import com.techlooper.service.TalentSearchDataProcessor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by NguyenDangKhoa on 3/11/15.
 */
@Service("VIETNAMWORKSTalentSearchDataProcessor")
public class VietnamworksTalentSearchDataProcessor implements TalentSearchDataProcessor {

    @Override
    public List<Talent> process(List<UserImportEntity> users) {
        return users.stream().map(userImportEntity -> {
            Map<String,Object> profile = (Map<String,Object>) userImportEntity.getProfiles().get(SocialProvider.VIETNAMWORKS);

            if (profile == null) {
                return null;
            }

            Talent.Builder talentBuilder = new Talent.Builder();
            return talentBuilder.withEmail(userImportEntity.getEmail())
                     .withUsername(StringUtils.trimToEmpty((String) userImportEntity.getFullName()))
                     .withFullName(StringUtils.trimToEmpty((String) userImportEntity.getFullName()))
                     .withCompany(StringUtils.trimToEmpty((String) profile.get("mostRecentEmployer")))
                     .withDescription(StringUtils.trimToEmpty((String) profile.get("alias")))
                     .withLocation(StringUtils.trimToEmpty((String) profile.get("address")))
                     .withJobTitle(StringUtils.trimToEmpty((String) profile.get("mostRecentPosition")))
                     .build();
        }).filter(talent -> talent != null).collect(Collectors.toList());
    }

    @Override
    public void normalizeInputParameter(TalentSearchRequest param) {
        param.getSkills().removeAll(Arrays.asList(null, ""));
        param.getCompanies().removeAll(Arrays.asList(null, ""));
        param.getLocations().removeAll(Arrays.asList(null, ""));
    }

    @Override
    public TalentSearchRequest getSearchAllRequestParameter() {
        return new TalentSearchRequest();
    }

}
