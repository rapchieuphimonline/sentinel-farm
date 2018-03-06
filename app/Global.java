/**
 * Created by LAP10599-local on 1/5/2016.
 */

import com.pezooworks.framework.log.LogHelper;
import play.GlobalSettings;

public class Global extends GlobalSettings {
    @Override
    public void onStart(play.Application app) {
        LogHelper.Log("Application has started");
        controllers.Application.startServer();
    }

    @Override
    public void onStop(play.Application app) {
        LogHelper.Log("Application shutdown...");
    }
}
