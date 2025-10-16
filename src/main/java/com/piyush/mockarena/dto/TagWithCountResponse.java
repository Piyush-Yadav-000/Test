// TagWithCountResponse.java
package com.piyush.mockarena.dto;

import lombok.Data;

@Data
public class TagWithCountResponse {
    private Long id;
    private String name;
    private String displayName;
    private String category;
    private String colorCode;
    private Integer problemCount;
}
