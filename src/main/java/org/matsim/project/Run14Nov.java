package org.matsim.project;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import picocli.CommandLine;

public class Run14Nov extends MATSimApplication {
    @CommandLine.Option(names = "--speedReduction", description = "this reduces the speed of urban links", defaultValue = "0.5")
    private double speedReducion ;
    
    public Run14Nov() {

    super(String.format("scenarios/equil/config.xml"));
}
    public static void main(String[] args) {
        MATSimApplication.run(Run14Nov.class, args);
    }

    @Override
    protected Config prepareConfig(Config config) {
        config.controler().setLastIteration(2);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        return super.prepareConfig(config);
    }

    @Override
    protected void prepareScenario(Scenario scenario) {
        for (Link link : scenario.getNetwork().getLinks().values()){
            if(link.getFreespeed() <= 50/3.6){
                link.setFreespeed(link.getFreespeed()*speedReducion);
            }
        }
        super.prepareScenario(scenario);
    }
}
