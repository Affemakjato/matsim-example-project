package org.matsim.drtScenario;

import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

public class RunDRT {
    public static void main(String[] args) {
        String configPath = "C:\\Users\\tekuh\\OneDrive\\Master\\AdvancedMATSim\\MATSim\\scenarios\\Nyc-drt-scenario\\nyc_drt_config.xml";
        Config config = ConfigUtils.loadConfig(configPath, new MultiModeDrtConfigGroup(), new DvrpConfigGroup());
        Controler controler = DrtControlerCreator.createControler(config, false);
        controler.run();
    }
}
