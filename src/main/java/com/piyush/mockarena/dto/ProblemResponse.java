package com.piyush.mockarena.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.piyush.mockarena.entity.Problem;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemResponse {

    private Long id;
    private String title;
    private String description;
    private Problem.Difficulty difficulty;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean active;

    // ✅ ADDED: Missing fields that controller tries to set
    private String slug;
    private Integer totalSubmissions;
    private Integer acceptedSubmissions;
    private Double acceptanceRate;
    private String[] tags;
    private Boolean isPremium;

    // ✅ ADDED: Problem detail fields
    private String sampleInput;
    private String sampleOutput;
    private String explanation;
    private String constraints;
    private String inputFormat;
    private String outputFormat;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;

    // ✅ ADDED: Template-related fields
    private String functionName;
    private String returnType;
    private Boolean usesTemplate;

    // ✅ ADDED: Additional fields for completeness
    private String companyTags;
    private String editorial;
    private String hints;
    private String followUp;
    private Integer likesCount;
    private Integer dislikesCount;

    // ✅ Manual setters (Lombok should handle these, but adding for safety)
    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDifficulty(Problem.Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // ✅ ADDED: Setters for new fields
    public void setSampleInput(String sampleInput) {
        this.sampleInput = sampleInput;
    }

    public void setSampleOutput(String sampleOutput) {
        this.sampleOutput = sampleOutput;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public void setConstraints(String constraints) {
        this.constraints = constraints;
    }

    public void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public void setTimeLimitMs(Integer timeLimitMs) {
        this.timeLimitMs = timeLimitMs;
    }

    public void setMemoryLimitMb(Integer memoryLimitMb) {
        this.memoryLimitMb = memoryLimitMb;
    }

    public void setAcceptanceRate(Double acceptanceRate) {
        this.acceptanceRate = acceptanceRate;
    }

    public void setTotalSubmissions(Integer totalSubmissions) {
        this.totalSubmissions = totalSubmissions;
    }

    public void setAcceptedSubmissions(Integer acceptedSubmissions) {
        this.acceptedSubmissions = acceptedSubmissions;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public void setIsPremium(Boolean isPremium) {
        this.isPremium = isPremium;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public void setUsesTemplate(Boolean usesTemplate) {
        this.usesTemplate = usesTemplate;
    }

    public void setCompanyTags(String companyTags) {
        this.companyTags = companyTags;
    }

    public void setEditorial(String editorial) {
        this.editorial = editorial;
    }

    public void setHints(String hints) {
        this.hints = hints;
    }

    public void setFollowUp(String followUp) {
        this.followUp = followUp;
    }

    public void setLikesCount(Integer likesCount) {
        this.likesCount = likesCount;
    }

    public void setDislikesCount(Integer dislikesCount) {
        this.dislikesCount = dislikesCount;
    }

    // ✅ Getters (Lombok should handle these, but adding for safety)
    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Problem.Difficulty getDifficulty() {
        return difficulty;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public Boolean isActive() {
        return active;
    }

    // ✅ ADDED: Getters for new fields
    public String getSampleInput() {
        return sampleInput;
    }

    public String getSampleOutput() {
        return sampleOutput;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getConstraints() {
        return constraints;
    }

    public String getInputFormat() {
        return inputFormat;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public Integer getTimeLimitMs() {
        return timeLimitMs;
    }

    public Integer getMemoryLimitMb() {
        return memoryLimitMb;
    }

    public Double getAcceptanceRate() {
        return acceptanceRate;
    }

    public Integer getTotalSubmissions() {
        return totalSubmissions;
    }

    public Integer getAcceptedSubmissions() {
        return acceptedSubmissions;
    }

    public String getSlug() {
        return slug;
    }

    public String[] getTags() {
        return tags;
    }

    public Boolean getIsPremium() {
        return isPremium;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getReturnType() {
        return returnType;
    }

    public Boolean getUsesTemplate() {
        return usesTemplate;
    }

    public String getCompanyTags() {
        return companyTags;
    }

    public String getEditorial() {
        return editorial;
    }

    public String getHints() {
        return hints;
    }

    public String getFollowUp() {
        return followUp;
    }

    public Integer getLikesCount() {
        return likesCount;
    }

    public Integer getDislikesCount() {
        return dislikesCount;
    }
}
