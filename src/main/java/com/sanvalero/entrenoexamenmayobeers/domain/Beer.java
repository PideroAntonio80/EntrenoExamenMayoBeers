package com.sanvalero.entrenoexamenmayobeers.domain;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * Creado por @ author: Pedro Orós
 * el 13/05/2021
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Beer {
    private int id;
    private String name;
    private String description;
    private String tagline;
    private String first_brewed;
    private String image_url;
    @SerializedName(value = "abv")
    private float degrees;
    @SerializedName(value = "ibu")
    private float bitterness;
    private Volume volume;
}
