package com.sanvalero.entrenoexamenmayobeers.service;

import com.sanvalero.entrenoexamenmayobeers.domain.Beer;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;

import java.util.List;

import static com.sanvalero.entrenoexamenmayobeers.util.Constants.URL;

/**
 * Creado por @ author: Pedro Orós
 * el 13/05/2021
 */
public class BeerService {

    private BeerApiService api;

    public BeerService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();

        api = retrofit.create(BeerApiService.class);
    }

    public Observable<List<Beer>> getAllBeers() {
        return api.getAllBeers();
    }

    public Observable<List<Beer>> getPageBeers(int page) {
        return api.getPageBeers(page);
    }
}
