package controllers;

import com.google.common.base.Strings;
import com.pezooworks.framework.log.LogHelper;
import org.codehaus.jackson.JsonNode;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * Created by LAP11313-local on 12/4/2017.
 */
public class LogController extends Controller {
    public static Result goLogEndpoint() {
        LogHelper.Log("on goLogEndpoint... request := " + request());
        return ok();
    }

    public static Result doLogEndpoint() {
        LogHelper.Log("on doLogEndpoint... request := " + request());
        JsonNode jsonNode = request().body().asJson();
        String message = jsonNode.toString();
        if (!Strings.isNullOrEmpty(message)) {
            LogHelper.Log("LOG_ENDPOINT", message);
        }
        return ok();
    }
}
