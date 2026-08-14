package com.example.blog.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 글(post) 도메인 객체.
 */
public class Post {
    private long id;
    private String slug;
    private String title;
    private String summary;
    private String contentMd;
    private String contentHtml;
    /** 본문 원문의 형식: MD | HTML. content_md에 담긴 내용을 어떻게 다룰지 결정한다. */
    private String contentType = "MD";
    private String thumbnailUrl;
    private String metaDescription;
    private String status;          // DRAFT | PUBLISHED
    private long viewCount;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    /** 연결된 태그. 상세/편집 화면에서만 채운다(목록 조회에서는 비어 있음). */
    private List<Tag> tags = new ArrayList<>();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getContentMd() { return contentMd; }
    public void setContentMd(String contentMd) { this.contentMd = contentMd; }

    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) {
        this.contentType = "HTML".equals(contentType) ? "HTML" : "MD";
    }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getMetaDescription() { return metaDescription; }
    public void setMetaDescription(String metaDescription) { this.metaDescription = metaDescription; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategorySlug() { return categorySlug; }
    public void setCategorySlug(String categorySlug) { this.categorySlug = categorySlug; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public List<Tag> getTags() { return tags; }
    public void setTags(List<Tag> tags) { this.tags = (tags == null) ? new ArrayList<>() : tags; }

    /** 편집 폼용: 태그 이름을 "a, b, c" 형태로 반환. */
    public String getTagNames() {
        return tags.stream().map(Tag::getName).collect(java.util.stream.Collectors.joining(", "));
    }
}
