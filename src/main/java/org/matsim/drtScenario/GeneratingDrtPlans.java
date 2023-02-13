package org.matsim.drtScenario;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class GeneratingDrtPlans {

        public static void main(String[] args) throws IOException{
            String outputPath = "C:\\Users\\tekuh\\OneDrive\\Master\\AdvancedMATSim\\MATSim\\scenarios\\Nyc-drt-scenario\\nyc-plans.xml";
            Network network = NetworkUtils.readNetwork("C:\\Users\\tekuh\\OneDrive\\Master\\AdvancedMATSim\\MATSim\\scenarios\\Nyc-drt-scenario\\network.xml.gz");
            Path inputPlansPath = Path.of("C:\\Users\\tekuh\\OneDrive\\Master\\AdvancedMATSim\\manhattan_trips.csv");

            Config config = ConfigUtils.createConfig();
            Population outputPlans = PopulationUtils.createPopulation(config);
            PopulationFactory populationFactory = outputPlans.getFactory();

            try (
                    CSVParser parser =
                            new CSVParser(Files.newBufferedReader(inputPlansPath),
                            CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
                int rowNumber = 0;
                for (CSVRecord record : parser.getRecords()) {

                    Person person = populationFactory.createPerson(Id.createPersonId("drt_person_" + rowNumber));
                    rowNumber++;
                    Plan plan = populationFactory.createPlan();

                    Coord fromCoord = new Coord(Double.parseDouble(record.get(3)), Double.parseDouble(record.get(4)));
                    Coord toCoord = new Coord(Double.parseDouble(record.get(5)), Double.parseDouble(record.get(6)));
                    Link fromLink = NetworkUtils.getNearestLink(network, fromCoord);
                    Link toLink = NetworkUtils.getNearestLink(network, toCoord);

                    Activity activity0 = populationFactory.createActivityFromLinkId("dummy", fromLink.getId());
                    Activity activity1 = populationFactory.createActivityFromLinkId("dummy", toLink.getId());
                    GregorianCalendar c = new GregorianCalendar();
                    Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(record.get(0));
                    c.setTime(date);
                    int PUTime = c.get(Calendar.HOUR_OF_DAY) * 3600 + 60 * c.get(Calendar.MINUTE);
                    activity0.setEndTime(PUTime);

                    Leg leg = populationFactory.createLeg(TransportMode.drt);

                    plan.addActivity(activity0);
                    plan.addLeg(leg);
                    plan.addActivity(activity1);

                    person.addPlan(plan);

                    outputPlans.addPerson(person);


                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

            PopulationWriter populationWriter = new PopulationWriter(outputPlans);
            populationWriter.write(outputPath);
        }
    }
