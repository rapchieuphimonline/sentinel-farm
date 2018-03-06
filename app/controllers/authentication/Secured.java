package controllers.authentication;

import controllers.common.Helper;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

public class Secured extends Security.Authenticator {
    private final int SESSION_TIMEOUT = 7200;

    @Override
    public String getUsername(Context ctx) {
        String username = ctx.session().get("email");

        // handle session timeout
        if (username != null && username.length() > 0) {
            // get last access time
            long last_access_time = -1;
            try {
                last_access_time = Long.parseLong(ctx.session().get(username + "_" + "timeout"));
            } catch (Exception e) {
//                e.printStackTrace();
                last_access_time = -1;
            }

            if (last_access_time > 0 && (last_access_time + SESSION_TIMEOUT > Helper.SECONDS())) {
                // valid session time -> set new session time
                ctx.session().put(username + "_" + "timeout", Long.toString(Helper.SECONDS()));
            } else {
                // invalid session time
                ctx.session().remove(username);
                ctx.session().remove(username + "_" + "timeout");
                ctx.session().remove(username + "_" + "role");
                username = null;
            }
        }
        return username;
    }

    @Override
    public Result onUnauthorized(Context ctx) {
        return redirect("/login");
    }
}
