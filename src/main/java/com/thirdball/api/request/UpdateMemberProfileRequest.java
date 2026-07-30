package com.thirdball.api.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/** Fields a member may maintain on their own club profile. */
public class UpdateMemberProfileRequest {
    @Min(2000) @Max(2100)
    private Integer graduationYear;

    @Size(max = 30)
    private String skillLevel;

    @Size(max = 30)
    private String phone;

    public Integer getGraduationYear() { return graduationYear; }
    public void setGraduationYear(Integer graduationYear) { this.graduationYear = graduationYear; }
    public String getSkillLevel() { return skillLevel; }
    public void setSkillLevel(String skillLevel) { this.skillLevel = skillLevel; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
