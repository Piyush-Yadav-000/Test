package com.piyush.mockarena.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "problem_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProblemTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagCategory category;

    @Column(name = "color_code")
    private String colorCode;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    private Set<Problem> problems = new HashSet<>();

    public enum TagCategory {
        DATA_STRUCTURE("Data Structure"),
        ALGORITHM("Algorithm"),
        TECHNIQUE("Technique"),
        TOPIC("Topic"),
        COMPANY("Company"),
        DIFFICULTY("Difficulty");

        private final String displayName;

        TagCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @PreUpdate
    public void preUpdate() {
        // Can add update logic if needed
    }
}
