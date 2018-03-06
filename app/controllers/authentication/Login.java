package controllers.authentication;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 11/17/13
 * Time: 12:14 PM
 * To change this template use File | Settings | File Templates.
 */


import com.google.common.base.Strings;
import com.pezooworks.framework.log.LogHelper;
import controllers.common.Helper;
import play.api.templates.Html;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;

import static play.data.Form.form;

public class Login extends Controller {
    public static Result goLogin() {
        StringBuilder log = new StringBuilder(40);
        log.append("GET");
        log.append(",").append("login");
        LogHelper.Log(log.toString());
        Html html = views.html.login.render("Sign in to start your session.");
        return ok(html);
    }

    public static Result doLogin() {
        DynamicForm bindedForm = form().bindFromRequest();
        String name = bindedForm.get("name");
        String passcode = bindedForm.get("passcode");

        StringBuilder log = new StringBuilder(40);
        log.append("POST");
        log.append(",").append("login");
        log.append(",").append(name);
        LogHelper.Log(log.toString());

        if (isValid(name, passcode)) {
            session("email", name);
            session(name + "_" + "timeout", Long.toString(Helper.SECONDS()));
            return redirect("/dashboard");
        } else {
            Html html = views.html.login.render("Incorrect password. Please try again.");
            return ok(html);
        }
    }

    public static boolean isValid(String name, String passcode) {
        if (Strings.isNullOrEmpty(name) || Strings.isNullOrEmpty(passcode)) {
            return false;
        }
        return name.equalsIgnoreCase("sentinel") && passcode.equalsIgnoreCase("sentinel?aH");
    }
}
