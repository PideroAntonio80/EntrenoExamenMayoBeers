package com.sanvalero.entrenoexamenmayobeers.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Creado por @ author: Pedro Orós
 * el 13/05/2021
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Volume {

    private int value;
    private String unit;
}
