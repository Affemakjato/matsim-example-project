package org.matsim.drtScenario;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

    @CommandLine.Command(
            name = "create-fleet",
            description = {"create drt fleet"}
    )
    public class RunGenerateVehicle implements MATSimAppCommand {
        @CommandLine.Option(
                names = {"--network"},
                description = {"path to network file"},
                required = true
        )
        private Path networkFile;
        @CommandLine.Option(
                names = {"--fleet-size-from"},
                description = {"number of vehicles to generate"},
                required = true
        )
        private int fleetSizeFrom;
        @CommandLine.Option(
                names = {"--fleet-size-to"},
                description = {"number of vehicles to generate"},
                required = true
        )
        private int fleetSizeTo;
        @CommandLine.Option(
                names = {"--fleet-size-interval"},
                description = {"number of vehicles to generate"},
                defaultValue = "10"
        )
        private int fleetSizeInterval;
        @CommandLine.Option(
                names = {"--capacity"},
                description = {"capacity of the vehicle"},
                required = true
        )
        private int capacity;
        @CommandLine.Option(
                names = {"--output-folder"},
                description = {"path to output folder"},
                required = true
        )
        private Path outputFolder;
        @CommandLine.Option(
                names = {"--operator"},
                description = {"name of the operator"},
                defaultValue = "drt"
        )
        private String operator;
        @CommandLine.Option(
                names = {"--start-time"},
                description = {"service starting time"},
                defaultValue = "0"
        )
        private double startTime;
        @CommandLine.Option(
                names = {"--end-time"},
                description = {"service ending time"},
                defaultValue = "86400"
        )
        private double endTime;
        @CommandLine.Option(
                names = {"--depots"},
                description = {"Path to the depots location file"},
                defaultValue = ""
        )
        private String depotsPath;
        @CommandLine.Mixin
        private ShpOptions shp = new ShpOptions();
        private static final Random random = MatsimRandom.getRandom();

        public RunGenerateVehicle() {
        }

        public static void main(String[] args) {
            (new org.matsim.drtScenario.RunGenerateVehicle()).execute(args);
        }

        public Integer call() throws Exception {
            if (!Files.exists(this.outputFolder, new LinkOption[0])) {
                Files.createDirectory(this.outputFolder);
            }

            Network network = NetworkUtils.readNetwork(this.networkFile.toString());
            List<Link> links = (List)network.getLinks().values().stream().filter((l) -> {
                return l.getAllowedModes().contains("car");
            }).collect(Collectors.toList());
            if (this.shp.isDefined()) {
                Geometry serviceArea = this.shp.getGeometry();
                links = (List)links.stream().filter((l) -> {
                    return MGC.coord2Point(l.getToNode().getCoord()).within(serviceArea);
                }).collect(Collectors.toList());
            }

            if (!this.depotsPath.equals("")) {
                CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(this.depotsPath), StandardCharsets.UTF_8), CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader());

                try {
                    links.clear();
                    Iterator var4 = parser.iterator();

                    while(var4.hasNext()) {
                        CSVRecord record = (CSVRecord)var4.next();
                        Link depotLink = (Link)network.getLinks().get(Id.createLinkId(record.get(0)));
                        links.add(depotLink);
                    }
                } catch (Throwable var9) {
                    try {
                        parser.close();
                    } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                    }

                    throw var9;
                }

                parser.close();
            }

            for(int fleetSize = this.fleetSizeFrom; fleetSize <= this.fleetSizeTo; fleetSize += this.fleetSizeInterval) {
                System.out.println("Creating fleet: " + fleetSize);
                List<DvrpVehicleSpecification> vehicleSpecifications = new ArrayList();

                for(int i = 0; i < fleetSize; ++i) {
                    Id startLinkId;
                    if (this.depotsPath.equals("")) {
                        startLinkId = ((Link)links.get(random.nextInt(links.size()))).getId();
                    } else {
                        startLinkId = ((Link)links.get(i % links.size())).getId();
                    }

                    DvrpVehicleSpecification vehicleSpecification = ImmutableDvrpVehicleSpecification.newBuilder().id(Id.create(this.operator + "_" + i, DvrpVehicle.class)).startLinkId(startLinkId).capacity(this.capacity).serviceBeginTime(this.startTime).serviceEndTime(this.endTime).build();
                    vehicleSpecifications.add(vehicleSpecification);
                }

                (new FleetWriter(vehicleSpecifications.stream())).write(this.outputFolder.toString() + "/" + fleetSize + "-" + this.capacity + "_seater-" + this.operator + "-vehicles.xml");
            }

            return 0;
        }
    }
