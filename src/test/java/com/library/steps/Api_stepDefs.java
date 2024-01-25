package com.library.steps;

import com.library.pages.BookPage;
import com.library.pages.LoginPage;
import com.library.utility.BrowserUtil;
import com.library.utility.ConfigurationReader;
import com.library.utility.DB_Util;
import com.library.utility.LibraryAPI_Util;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.response.ValidatableResponse;
import org.junit.Assert;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class Api_stepDefs {

    static String token;
    static RequestSpecification requestSpecification;
    static Response response;
    ValidatableResponse responseSpecification;
    String pathParam;



// ------------  USER Story --> 1  below implementation  ---------

    @Given("I logged Library api as a {string}")
    public void i_logged_library_api_as_a(String userType) {

        token = LibraryAPI_Util.getToken(userType);
        //System.out.println("Token = " + token);
    }
    @Given("Accept header is {string}")
    public void accept_header_is(String acceptHeader) {

        requestSpecification = given().log().uri()
                .headers("x-library-token", token)
                .accept(acceptHeader);

    }

    @When("I send GET request to {string} endpoint")
    public void i_send_get_request_to_endpoint(String endpoint) {

    response = requestSpecification
        .when()
        .get(ConfigurationReader.getProperty("library.baseUri") + endpoint);
    }

    @Then("status code should be {int}")
    public void status_code_should_be(int expectedStatusCode) {

        assertThat(response.statusCode(), is(expectedStatusCode));
    }

    @Then("Response Content type is {string}")
    public void response_content_type_is(String expectedContentType) {

        assertThat(response.contentType(), is(expectedContentType));
    }

    @Then("{string} field should not be null")
    public void field_should_not_be_null(String field) {

        assertThat(field, is(notNullValue()));
    }



// ------------  USER Story --> 2  below implementation  ---------



    @Given("Path param is {string}")
    public void path_param_is(String id) {
        pathParam = id;

        requestSpecification = requestSpecification.pathParams("id", id);
    }

    @Then("{string} field should be same with path param")
    public void field_should_be_same_with_path_param(String idValue) {

         responseSpecification.body(idValue, is(pathParam));
    }

    @Then("following fields should not be null")
    public void following_fields_should_not_be_null(List<String> fields) {

       responseSpecification.body(fields.get(0), is(notNullValue()))
                            .body(fields.get(1), is(notNullValue()))
                            .body(fields.get(2), is(notNullValue()));

    }


// ------------  USER Story --> 3  below implementation  ---------


    @Given("Request Content Type header is {string}")
    public void request_content_type_header_is(String requestHeader) {

       requestSpecification = requestSpecification.contentType(requestHeader);
    }

    Map<String,Object> randomDataMap;


    @Given("I create a random {string} as request body")
    public void i_create_a_random_as_request_body(String randomData) {

        Map<String,Object> requestBody = new LinkedHashMap<>();

        switch (randomData){
            case "user" :
                requestBody=LibraryAPI_Util.getRandomUserMap();
                break;
            case "book" :
                requestBody=LibraryAPI_Util.getRandomBookMap();
                break;
            default:
                throw new RuntimeException("Unexpected value: "+ randomData);
        }

        System.out.println("requestBody = " + requestBody);
        randomDataMap = requestBody;
        requestSpecification.formParams(requestBody);


    }
    @When("I send POST request to {string} endpoint")
    public void i_send_post_request_to_endpoint(String endpoint) {

        response = requestSpecification.when()
        .post(ConfigurationReader.getProperty("library.baseUri") + endpoint ).prettyPeek();
    }

    @Then("the field value for {string} path should be equal to {string}")
    public void the_field_value_for_path_should_be_equal_to(String key, String value) {

        responseSpecification = response.then().body(key, is(value));
    }


    @Then("UI, Database and API created book information must match")
    public void ui_database_and_api_created_book_information_must_match() {

        System.err.println(" UI - DB - API - Verification");

        // API DATA --> Expected --> Since we added data from API
        Response apiData =given().log().uri().header("x-library-token", LibraryAPI_Util.getToken("librarian"))
                .pathParam("id", response.path("book_id"))
                .when().get(ConfigurationReader.getProperty("library.baseUri")+ "/get_book_by_id/{id}");

        JsonPath jsonPath = apiData.jsonPath();
        System.err.println("--------- API DATA -------------");

        Map<String, Object> APIBook = new LinkedHashMap<>();
        APIBook.put("name", jsonPath.getString("name"));
        APIBook.put("isbn", jsonPath.getString("isbn"));
        APIBook.put("year", jsonPath.getString("year"));
        APIBook.put("author", jsonPath.getString("author"));
        APIBook.put("book_category_id", jsonPath.getString("book_category_id"));
        APIBook.put("description", jsonPath.getString("description"));

        System.out.println("APIBook = " + APIBook);

        // To find book in database we need ID information
        String bookID = jsonPath.getString("id");

        // DB data --> Actual --> DB needs to show data that we add through API
        String query = "select * from books where id='"+bookID+"'";
        DB_Util.runQuery(query);

        Map<String, Object> DBBook = DB_Util.getRowMap(1);

        System.err.println("--------- DB DATA -------------");

        // These fields are auto-generated so we need to remove
        DBBook.remove("added_date");
        DBBook.remove("id");
        System.out.println("DBBook = " + DBBook);


        // We need to find CategoryName, since the category can be found by ID only,
        // we need to run  query to find category as category_id

        String UIBookCategory = BrowserUtil.getSelectedOption(bookPage.categoryDropdown);
        DB_Util.runQuery("select id from book_categories where name ='"+UIBookCategory+"'");
        String UICategoryID = DB_Util.getFirstRowFirstColumn();

        System.err.println("--------- UI DATA -------------");

        // UI DATA  --> Actual --> needs to show data that we add through API
        BookPage bookPage = new BookPage();
        // we need bookName to find in UI.Make sure book name is unique.
        // Normally ISBN should be unique for each book

        String bookName = (String) randomDataMap.get("name");
        System.out.println("bookName = " + bookName);
        String isbn = (String) randomDataMap.get("isbn");

        // Find book in UI

        bookPage.search.sendKeys(bookName);
        BrowserUtil.waitFor(3);
        bookPage.editBookByISBN(isbn).click();
        BrowserUtil.waitFor(3);

        // Get book info
        String UIBookName = bookPage.bookName.getAttribute("value");
        String UIAuthorName = bookPage.author.getAttribute("value");
        String UIYear = bookPage.year.getAttribute("value");
        String UIIsbn = bookPage.isbn.getAttribute("value");
        String UIDescription = bookPage.description.getAttribute("value");


        Map<String, Object> UIBook = new LinkedHashMap<>();
        UIBook.put("name", UIBookName);
        UIBook.put("isbn", UIIsbn);
        UIBook.put("year", UIYear);
        UIBook.put("author", UIAuthorName);
        UIBook.put("book_category_id", UICategoryID);
        UIBook.put("description", UIDescription);

        System.out.println("UIBook = " + UIBook);

        // Assertion

        Assert.assertEquals(APIBook, DBBook);
        Assert.assertEquals(APIBook, UIBook);
        Assert.assertEquals(DBBook, UIBook);



    }



// ------------  USER Story --> 4  below implementation  ---------

    LoginPage loginPage;
    BookPage bookPage;

    @Then("created user information should match with Database")
    public void created_user_information_should_match_with_database() {

        Response apiUserData =given().log().uri().header("x-library-token", LibraryAPI_Util.getToken("librarian"))
                .pathParam("id", response.path("user_id"))
                .when().get(ConfigurationReader.getProperty("library.baseUri")+ "/get_user_by_id/{id}").prettyPeek();

        JsonPath jsonPath = apiUserData.jsonPath();
        Map<String, Object> APIUser = new LinkedHashMap<>();
        APIUser.put("id", jsonPath.getString("id"));
        APIUser.put("full_name", jsonPath.getString("full_name"));
        APIUser.put("email", jsonPath.getString("email"));
        APIUser.put("password", jsonPath.getString("password"));
        APIUser.put("user_group_id", jsonPath.getString("user_group_id"));
        APIUser.put("status", jsonPath.getString("status"));

        System.out.println("APIUser = " + APIUser); // expected API user info

        // To find book in database we need ID information
        String userID = jsonPath.getString("id");

        // DB DATA  --> Actual --> DB needs to show data that we add through API
        DB_Util.runQuery("select id, full_name, email, password, user_group_id, status from users where id ='"+userID+"'");
        Map<String, Object> DDUser = DB_Util.getRowMap(1);

        Assert.assertEquals(APIUser, DDUser);

    }


    @Then("created user should be able to login Library UI")
    public void created_user_should_be_able_to_login_library_ui() {

        loginPage = new LoginPage();
        String email = (String) randomDataMap.get("email");
        String password = (String) randomDataMap.get("password");

        loginPage.login(email, password);
        BrowserUtil.waitFor(3);

    }


    @Then("created user name should appear in Dashboard Page")
    public void created_user_name_should_appear_in_dashboard_page() {

        bookPage = new BookPage();
        String UIFullName = bookPage.accountHolderName.getText();
        String APIFullName = randomDataMap.get("full_name").toString();

        Assert.assertEquals(APIFullName, UIFullName);
    }


    // ------------  USER Story --> 5  below implementation  ---------

    String tokens;

    @Given("I logged Library api with credentials {string} and {string}")
    public void i_logged_library_api_with_credentials_and(String email, String password) {

        tokens = LibraryAPI_Util.getToken(email, password);

        requestSpecification = given().log().uri()
          .headers("x-library-token", LibraryAPI_Util.getToken(email, password));

    }
    @Given("I send token information as request body")
    public void i_send_token_information_as_request_body() {

        requestSpecification.pathParam("token", tokens);
    }

}

