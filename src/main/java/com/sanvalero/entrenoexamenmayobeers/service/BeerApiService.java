package com.sanvalero.entrenoexamenmayobeers.service;

import com.sanvalero.entrenoexamenmayobeers.domain.Beer;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

import java.util.List;

/**
 * Creado por @ author: Pedro Orós
 * el 13/05/2021
 */
public interface BeerApiService {

    @GET("beers")
    Observable<List<Beer>> getAllBeers();

    @GET("beers")
    Observable<List<Beer>> getPageBeers(@Query("page") int page);
}
