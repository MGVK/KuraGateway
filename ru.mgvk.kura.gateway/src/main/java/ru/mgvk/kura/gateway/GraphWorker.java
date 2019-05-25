package ru.mgvk.kura.gateway;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.wire.graph.WireComponentConfiguration;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;
import org.eclipse.kura.wire.graph.WireGraphService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GraphWorker {


    private static final Logger           logger        = LogManager.getLogger(GraphWorker.class);
    private              WireGraphService graphService;
    private              int              MIN_E_COUNT   = 0;
    private              int              MAX_E_COUNT   = 10;
    private              int              MIN_R_COUNT   = 0;
    private              int              MAX_R_COUNT   = 10;
    private              String           currentPID    = "";
    private              int              currentCountE = 0;
    private              int              currentCountR = 0;


    public GraphWorker(String currentPID) {
        this.currentPID = currentPID;
    }

    public GraphWorker(WireGraphService graphService, String currentPID) {
        this.graphService = graphService;
        this.currentPID = currentPID;
    }

    //    public static void setGraphService(WireGraphService graphService) {
//        GraphWorker.graphService = graphService;
//    }

    private void updateConfigs(ComponentConfiguration configuration, Map<String, Object> props) {
        try {
            WireComponentConfiguration wireComponentConfiguration = new WireComponentConfiguration(
                    configuration, props);

            List<WireComponentConfiguration> wc
                    = graphService.get()
                    .getWireComponentConfigurations()
                    .stream()
                    .filter(wireComponentConfiguration1 ->
                            !wireComponentConfiguration1.getConfiguration().getPid().equals(currentPID))
                    .collect(Collectors.toList());

            wc.add(wireComponentConfiguration);

            graphService.update(new WireGraphConfiguration(wc, graphService.get().getWireConfigurations()));

        } catch (KuraException e) {

            e.printStackTrace();
        }

    }

    WireComponentConfiguration getCurrentPIDConfiguration() {
        List<WireComponentConfiguration> configurations = null;
        WireComponentConfiguration       current        = null;
        try {
            configurations = graphService.get().getWireComponentConfigurations();
            current = configurations.stream()
                    .filter(wireComponentConfiguration -> wireComponentConfiguration.getConfiguration().getPid()
                            .equals(currentPID))
                    .findFirst().get();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return current;
    }


    void setEmmitersCount(int count) {
        if (count > MIN_E_COUNT && count < MAX_E_COUNT) {

            WireComponentConfiguration current = getCurrentPIDConfiguration();
            if (current != null) {

                HashMap<String, Object> props = new HashMap<>();
                props.putAll(current.getProperties());
                props.put("outputPortCount", count);

                updateConfigs(current.getConfiguration(), props);
            }
        }

    }

    void setPortsCount(int eCount, int rCount) {

        if (eCount > MIN_E_COUNT && eCount < MAX_E_COUNT
            && rCount > MIN_R_COUNT && rCount < MAX_R_COUNT) {

            WireComponentConfiguration current = getCurrentPIDConfiguration();
            if (current != null) {

                HashMap<String, Object> props = new HashMap<>();
                props.putAll(current.getProperties());
                props.put("outputPortCount", eCount);
                props.put("inputPortCount", rCount);


                updateConfigs(current.getConfiguration(), props);
            }
        }

    }

    void setRecieversCount(int count) {

        if (count > MIN_R_COUNT && count < MAX_R_COUNT) {
            WireComponentConfiguration current = getCurrentPIDConfiguration();

            if (current != null) {

                HashMap<String, Object> props = new HashMap<>();
                props.putAll(current.getProperties());
                props.put("inputPortCount", count);

                updateConfigs(current.getConfiguration(), props);
            }
        }

    }


    void addComponent() {

    }

    void removeComponent() {

    }

    List<WireComponentConfiguration> listComponents() {
        if (graphService != null) {
            try {
                return graphService.get().getWireComponentConfigurations();
            } catch (KuraException e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            }
        }
        return Collections.emptyList();
    }

    public void updatePorts(int emitterWiresCount, int recieverWiresCount) {
        logger.info("updating ports: em:{} rec:{}", emitterWiresCount, recieverWiresCount);
        if (currentCountR != recieverWiresCount || currentCountE != emitterWiresCount) {
            setPortsCount(currentCountE = emitterWiresCount,
                    currentCountR = recieverWiresCount);
        }
    }
}
