package org.matsim.drtScenario;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.osm.networkReader.OsmTags;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RunCreateNetworkFromOSM {
    /**
     * Example on how to convert osm data from e.g. http://download.geofabrik.de into a MATSim network. This examle puts all
     * motorways and primary roads into the MATSim network. If a link is contained in the supplied shape, also minor and
     * residential raods are put into the MATsim network.
     * <p>
     * After parsing the OSM-data, unreachable areas of the network are removed by using the network cleaner
     */

        private static final String UTM32nAsEpsg = "EPSG:2263";
        private static final Path input = Paths.get("C:\\Users\\tekuh\\OneDrive\\Master\\AdvancedMATSim\\MATSim\\scenarios\\Nyc-drt-scenario\\new-york-network.osm.pbf");
        private static final Path filterShape = Paths.get("C:\\Users\\tekuh\\OneDrive\\Master\\AdvancedMATSim\\nyc-manhattan.shp");

        public static void main(String[] args) throws MalformedURLException {
            new RunCreateNetworkFromOSM().create();
        }

        private void create() throws MalformedURLException {

            // choose an appropriate coordinate transformation. OSM Data is in WGS84. When working in central Germany,
            // EPSG:25832 or EPSG:25833 as target system is a good choice
            CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(
                    TransformationFactory.WGS84, UTM32nAsEpsg
            );

            // load the geometries of the shape file, so they can be used as a filter during network creation
            // using PreparedGeometry instead of Geometry increases speed a lot (usually)
            List<PreparedGeometry> filterGeometries = ShpGeometryUtils.loadPreparedGeometries(filterShape.toUri().toURL());

            // create an osm network reader with a filter
            SupersonicOsmNetworkReader reader = new SupersonicOsmNetworkReader.Builder()
                    .setCoordinateTransformation(transformation)
                    .setIncludeLinkAtCoordWithHierarchy((coord, hierarchyLevel) -> {

                        // take all links which are motorway, trunk, or primary-street regardless of their location
                        //if (hierarchyLevel <= LinkProperties.LEVEL_PRIMARY) return true;

                        // whithin the shape, take all links which are contained in the osm-file
                        return ShpGeometryUtils.isCoordInPreparedGeometries(coord, filterGeometries);
                    })
                    .setAfterLinkCreated((link, osmTags, direction) -> {

                        // if the original osm-link contains a cycleway tag, add bicycle as allowed transport mode
                        // although for serious bicycle networks use OsmBicycleNetworkReader
                        if (osmTags.containsKey(OsmTags.CYCLEWAY)) {
                            Set<String> modes = new HashSet<>(link.getAllowedModes());
                            modes.add(TransportMode.bike);
                            link.setAllowedModes(modes);
                        }
                    })
                    .build();

            // the actual work is done in this call. Depending on the data size this may take a long time
            Network network = reader.read(input.toString());

            // clean the network to remove unconnected parts where agents might get stuck
            new NetworkCleaner().run(network);

            // write out the network into a file
            new NetworkWriter(network).write("C:\\Users\\tekuh\\OneDrive\\Master\\AdvancedMATSim\\MATSim\\scenarios\\Nyc-drt-scenario\\network.xml.gz");
        }
    }
