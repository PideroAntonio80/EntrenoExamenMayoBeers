package com.sanvalero.entrenoexamenmayobeers;

import com.sanvalero.entrenoexamenmayobeers.domain.Beer;
import com.sanvalero.entrenoexamenmayobeers.service.BeerService;
import com.sanvalero.entrenoexamenmayobeers.util.AlertUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Creado por @ author: Pedro Orós
 * el 13/05/2021
 */
public class AppController implements Initializable {

    public Label lName, lSlogan, lDate, lDegrees, lVolVal, lVolUnit;
    public TextField tfSearch, tfMas, tfMenos, tfPage;
    public TextArea taDescription;
    public TableView<Beer> tvData;
    public ListView<Beer> lvBitter;
    public ProgressIndicator piLoading;
    public ComboBox<String> cbChoose;
    public ComboBox<String> cbDate;
    public WebView wbImage;
    public WebEngine engine;

    private ObservableList<Beer> allBeers;
    private ObservableList<Beer> bitterBeers;
    private List<Beer> beerList;

    private BeerService beerService;

    private int myPage = 1;
    private int countPage = 1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        putTableColumnsSw();

        String[] options = new String[]{"<<All>>", "Degrees"};
        cbChoose.setValue("<<All>>");
        cbChoose.setItems(FXCollections.observableArrayList(options));

        engine = wbImage.getEngine();

        beerService = new BeerService();

        allBeers = FXCollections.observableArrayList();
        bitterBeers = FXCollections.observableArrayList();
        beerList = new ArrayList<>();

        completeLoad();
    }

    @FXML
    public void show() {
        String option = cbChoose.getValue();

        switch (option) {
            case "<<All>>":
                completeLoad();
                break;

            case "Degrees":
                orderByDegrees();
                break;
        }
    }

    @FXML
    public void showData() {
        Beer beer = tvData.getSelectionModel().getSelectedItem();

        String image = beer.getImage_url();

        engine.load(image);

        lName.setText(beer.getName());
        lSlogan.setText(beer.getTagline());
        lDate.setText(beer.getFirst_brewed());
        lDegrees.setText(String.valueOf(beer.getDegrees()));
        lVolVal.setText(String.valueOf(beer.getVolume().getValue()));
        lVolUnit.setText(String.valueOf(beer.getVolume().getUnit()));
        taDescription.setWrapText(true);
        taDescription.setMaxSize(200,300);
        taDescription.setText(beer.getDescription());
    }

    @FXML
    public void search() {
        String myBeer = tfSearch.getText();

        searchBeer(myBeer);
    }

    @FXML
    public void verBitter() {
        float min = Float.parseFloat(tfMenos.getText());
        float max = Float.parseFloat(tfMas.getText());
        searchBeerByBitter(max, min);
    }

    @FXML
    public void page() {
        if (tfPage.getText().isBlank() || tfPage.getText().equals("0")) {
            AlertUtils.mostrarError("Debes rellenar el campo Página con un número mayor que cero");
            return;
        }
        myPage = Integer.parseInt(tfPage.getText());
        completeLoadPage(myPage);
        countPage = myPage;
    }

    @FXML
    public void next() {
        countPage++;
        completeLoadPage(countPage);
    }

    @FXML
    public void prev() {
        countPage--;
        completeLoadPage(countPage);
    }

    @FXML
    public void csv() {
        export();
        AlertUtils.mostrarInformacion("Datos transferidos a fichero");
    }

    @FXML
    public void zip() throws ExecutionException, InterruptedException {
        File file = export();

        CompletableFuture.supplyAsync(() -> file.getAbsolutePath().concat(".zip"))
                .thenAccept(System.out::println)
                .whenComplete((unused, throwable) -> {
                    System.out.println("Archivo .zip generado en: " + file.getAbsolutePath().concat(".zip"));
                    Platform.runLater(() -> {
                        compressToZip(file);
                        AlertUtils.mostrarInformacion("Datos comprimidos en Zip");
                    });
                }).get();
    }

    @FXML
    public void dates() {
        lvBitter.getItems().clear();
        String selectedYear = cbDate.getSelectionModel().getSelectedItem();
        lvBitter.setItems(beerList.stream()
                .filter(beer -> beer.getFirst_brewed().contains(selectedYear))
                .collect(Collectors.toCollection(FXCollections::observableArrayList)));
    }

    public void putTableColumnsSw() {
        Field[] fields = Beer.class.getDeclaredFields();
        for (Field field : fields) {
            if(field.getName().equals("id") || field.getName().equals("description") || field.getName().equals("image_url") || field.getName().equals("volume")) continue;

            TableColumn<Beer, String> column = new TableColumn<>(field.getName());
            column.setCellValueFactory(new PropertyValueFactory<>(field.getName()));
            tvData.getColumns().add(column);
        }
        tvData.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadingBeers() {
        allBeers.clear();
        tvData.setItems(allBeers);

        beerService.getAllBeers()
                .flatMap(Observable::from)
                .doOnCompleted(() -> {
                    System.out.println("Listado completo de cervezas");
                    cbDate.setItems(beerList.stream()
                    .map(beer -> beer.getFirst_brewed().substring(3,7))
                    .sorted()
                    .distinct()
                    .collect(Collectors.toCollection(FXCollections::observableArrayList)));
                })
                .doOnError(throwable -> System.out.println(throwable.getMessage()))
                .subscribeOn(Schedulers.from(Executors.newCachedThreadPool()))
                .subscribe(b -> {
                    allBeers.add(b);
                    beerList.add(b);
                });

        /*beerList = allBeers;*/
    }

    public void completeLoad() {
        piLoading.setVisible(true);
        CompletableFuture.runAsync(() -> {
            piLoading.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
           /* try {    NO LE GUSTA A SANTI
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            loadingBeers();})
                .whenComplete((string, throwable) -> piLoading.setVisible(false));
    }

    public void orderByDegrees() {
        tvData.setItems(beerList.stream()
                .sorted(Comparator.comparing(Beer::getDegrees).reversed())
                .collect(Collectors.toCollection(FXCollections::observableArrayList)));

        /*allBeers.clear();

        beerService.getAllBeers()
                .flatMap(Observable::from)
                .doOnCompleted(() -> System.out.println("Listado completo de cervezas"))
                .doOnError(Throwable::printStackTrace)
                .subscribe(b -> allBeers.add(b));

        tvData.setItems(allBeers.stream()
                .sorted(Comparator.comparing(Beer::getDegrees).reversed())
                .collect(Collectors.toCollection(FXCollections::observableArrayList)));*/
    }

    public void searchBeer(String name) {
        tvData.setItems(beerList.stream()
                .filter(beer -> beer.getName().equalsIgnoreCase(name))
                .distinct()
                .collect(Collectors.toCollection(FXCollections::observableArrayList)));
        /*allBeers.clear();

        beerService.getAllBeers()
                .flatMap(Observable::from)
                .filter(beer -> beer.getName().equalsIgnoreCase(name))
                .doOnCompleted(() -> System.out.println("Listado completo de cervezas"))
                .doOnError(Throwable::printStackTrace)
                .subscribe(b -> allBeers.add(b));*/
    }

    public void searchBeerByBitter(float max, float min) {
        lvBitter.getItems().clear();
        lvBitter.setItems(beerList.stream()
                .filter(beer -> beer.getBitterness() >= min && beer.getBitterness() <= max)
                .distinct()
                .collect(Collectors.toCollection(FXCollections::observableArrayList)));

        /*bitterBeers.clear();
        lvBitter.setItems(bitterBeers);

        beerService.getAllBeers()
                .flatMap(Observable::from)
                .filter(beer -> beer.getBitterness() >= min && beer.getBitterness() <= max)
                .doOnCompleted(() -> System.out.println("Listado completo de cervezas"))
                .doOnError(Throwable::printStackTrace)
                .subscribe(b -> bitterBeers.add(b));*/
    }

    public void getPageBeer(int page) {
        allBeers.clear();
        beerList.clear();
        tvData.setItems(allBeers);

        beerService.getPageBeers(page)
                .flatMap(Observable::from)
                .doOnCompleted(() -> System.out.println("Nueva página de la lista de cervezas"))
                .subscribeOn(Schedulers.from(Executors.newCachedThreadPool()))
                .doOnError(Throwable::printStackTrace)
                .subscribe(b -> {allBeers.add(b);
                    beerList.add(b);
                });
    }

    public void completeLoadPage(int page) {
        piLoading.setVisible(true);
        CompletableFuture.runAsync(() -> {
            piLoading.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            getPageBeer(page);})
                .whenComplete((string, throwable) -> piLoading.setVisible(false));
    }

    public File export() {
        File file = null;
        FileChooser fileChooser = new FileChooser();
        file = fileChooser.showSaveDialog(null);

        try {
            FileWriter fileWriter = new FileWriter(file + ".csv");
            CSVPrinter printer = new CSVPrinter(fileWriter, CSVFormat.TDF.withHeader("Nombre;", "Fecha;", "Graduacion;","Amargor;"));
            for (Beer beer : beerList) {
                printer.printRecord(beer.getName(), ';', beer.getFirst_brewed(), ';', beer.getDegrees(), ';', beer.getBitterness());
            }
            printer.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
            AlertUtils.mostrarError("Error al exportar los datos");
        }
        return file;
    }

    public void compressToZip(File file) {

        try {
            FileOutputStream fos = new FileOutputStream(file.getAbsolutePath().concat(".zip"));
            ZipOutputStream zos = new ZipOutputStream(fos);
            FileInputStream fis = new FileInputStream(file.getAbsolutePath().concat(".csv"));
            ZipEntry zipEntry = new ZipEntry(file.getName().concat(".csv"));

            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >=0){
                zos.write(bytes, 0, length);
            }
            zos.close();
            fis.close();
            fos.close();

            Files.delete(Path.of(file.getAbsolutePath().concat(".csv")));

        } catch (IOException ex) {
            AlertUtils.mostrarError("Error al exportar  Zip");
        }
    }
}
