package test.model;

import com.sun.jersey.api.client.GenericType;

import java.util.List;

/**
 * This is a model for the unit test dataset, located at:
 * https://sandbox.demo.socrata.com/d/8gex-q4ds
 */
public class UnitTestDataset {

    public static final GenericType<List<UnitTestDataset>> LIST_TYPE = new GenericType<List<UnitTestDataset>>() {
    };

    Integer id;
    String name;
    String another_name;
    String date;

    public Integer getId() {
        return id;
    }

    public void setId(Integer name) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAnother_name() {
        return another_name;
    }

    public void setAnother_name(String another_name) {
        this.another_name = another_name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
