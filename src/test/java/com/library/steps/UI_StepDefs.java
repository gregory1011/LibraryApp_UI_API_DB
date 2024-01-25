package com.library.steps;

import com.library.pages.BookPage;
import com.library.pages.LoginPage;
import com.library.utility.BrowserUtil;
import com.library.utility.Driver;
import io.cucumber.java.en.Given;

public class UI_StepDefs {

    LoginPage loginPage = new LoginPage();
    BookPage bookPage = new BookPage();

    @Given("I logged in Library UI as {string}")
    public void i_logged_in_library_ui_as(String user) {

        loginPage.login(user);
        BrowserUtil.waitFor(2);
    }
    @Given("I navigate to {string} page")
    public void i_navigate_to_page(String pageDomain) {

        bookPage.navigateModule(pageDomain);
        BrowserUtil.waitFor(1);
    }


}
