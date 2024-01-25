package com.library.utility;

import com.github.javafaker.Faker;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;

public class LibraryAPI_Util {


    private static String token;
    public String getToken(){

        return token;
    }


    /**
     * Return TOKEN as String by using provided username from /token endpoint
     * @param userType
     * @return
     */


    public static String getToken(String userType){

        String email = ConfigurationReader.getProperty( userType + "_username" );
        String password = "libraryUser";

        return getToken (email,password);
    }


    public static String getToken(String email, String password){

        return given()
                .contentType(ContentType.URLENC)
                .formParam("email" , email)
                .formParam("password" , password).
                when()
                .post(ConfigurationReader.getProperty("library.baseUri")+"/login")
                .prettyPeek()
                .path("token") ;
    }


    public static Map<String,Object> getRandomBookMap(){

        Faker faker = new Faker() ;
        Map<String,Object> bookMap = new LinkedHashMap<>();
        String randomBookName = faker.book().title() + faker.number().numberBetween(0, 10);
        bookMap.put("name", randomBookName);
        bookMap.put("isbn", faker.code().isbn10()   );
        bookMap.put("year", faker.number().numberBetween(1000,2021)   );
        bookMap.put("author",faker.book().author()   );
        bookMap.put("book_category_id", faker.number().numberBetween(1,20)   );  // in library app valid category_id is 1-20
        bookMap.put("description", faker.chuckNorris().fact() );

        return bookMap ;
    }

    public static Map<String,Object> getRandomUserMap(){

        Faker faker = new Faker() ;
        Map<String,Object> bookMap = new LinkedHashMap<>();
        String fullName = faker.name().fullName();
        String email=fullName.substring(0,fullName.indexOf(" "))+"@library";
        System.out.println(email);
        bookMap.put("full_name", fullName );
        bookMap.put("email", email);
        bookMap.put("password", "libraryUser");
        // 2 is librarian as role
        bookMap.put("user_group_id",2);
        bookMap.put("status", "ACTIVE");
        bookMap.put("start_date", "2023-03-11");
        bookMap.put("end_date", "2024-03-11");
        bookMap.put("address", faker.address().cityName());

        return bookMap ;
    }


    // role: librarian or student
    public static String getTokenByRole(String role) {
        String email = "";
        String password = "";

        switch (role) {

            case "librarian":
                email = ConfigurationReader.getProperty("librarian_username");
                password = ConfigurationReader.getProperty("librarian_password");
                break;

            case "student":
                email = ConfigurationReader.getProperty("student_username");
                password = ConfigurationReader.getProperty("student_password");
                break;

            default:
                // throws exception if we write a non-existing user
                throw new RuntimeException("Invalid Role Entry :\n>> " + role +" <<");

        }

        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", email);
        credentials.put("password", password);

        String accessToken = given()
                .queryParams(credentials)
                .when().get( "/sign").path("accessToken");

        return  "Bearer " + accessToken;

    }

    //Create one req spec for bookitUtils which accepts role
    public static RequestSpecification getReqSpec(String acceptHeader, String role){

        return given()
                .log().all()
                .accept(acceptHeader)
                .header("x-library-token", getTokenByRole(role));
    }


    //Create one res spec for status code and json verification
    public static ResponseSpecification getResSpec(int statusCode){

        return expect().statusCode(statusCode)
                .contentType(ContentType.JSON);
    }




}
