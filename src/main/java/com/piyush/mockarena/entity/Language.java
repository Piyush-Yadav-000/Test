package com.piyush.mockarena.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "languages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Language {

    @Id
    private Integer id; // Judge0 language ID

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "file_extension")
    private String fileExtension;

    @Column(name = "code_template", columnDefinition = "TEXT")
    private String codeTemplate;

    // FIXED: Use 'active' instead of 'isActive' to match Java Bean naming convention
    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    // Constructor for common languages
    public Language(Integer id, String name, String displayName, String fileExtension) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.fileExtension = fileExtension;
        this.active = true;
    }
}