package ru.mgvk.kura.gateway;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.wire.*;
import org.eclipse.kura.wire.graph.CachingAggregatorFactory;
import org.eclipse.kura.wire.graph.MultiportWireSupport;
import org.eclipse.kura.wire.graph.PortAggregatorFactory;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.eclipse.kura.wire.multiport.MultiportWireReceiver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

public class Main implements WireEmitter, MultiportWireReceiver, ConfigurableComponent,
        NetWorker.OnDataReceiver {

    private static final Logger logger = LogManager.getLogger(Main.class);
    HashMap<Wire, Integer> wiresIndexes = new HashMap<>();
    Wire[]                 wires;
    private          ru.mgvk.kura.gateway.GatewayOptions timerOptions;
    private volatile WireHelperService                   wireHelperService;
    private volatile WireGraphService                    wireGraphService;
    private volatile ConfigurationService                configurationService;
    private volatile ComponentConfiguration              componentConfiguration;
    private          MultiportWireSupport                wireSupport;
    private          GraphWorker                         graphWorker;
    private          GatewayOptions                      gatewayOptions;
    private          NetWorker                           netWorker;

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (isNull(this.wireHelperService)) {
            this.wireHelperService = wireHelperService;

        }
    }

    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    public void bindWireGraphService(final WireGraphService wireGraphService) {
        if (isNull(this.wireGraphService)) {
            this.wireGraphService = wireGraphService;
        }
    }

    public void unbindWireGraphService(final WireGraphService wireGraphService) {
        if (this.wireGraphService == wireGraphService) {
            this.wireGraphService = null;
        }
    }

    public void bindConfigurationService(final ConfigurationService configurationService) {
        if (isNull(this.configurationService)) {
            this.configurationService = configurationService;
        }
    }

    public void unbindConfigurationService(final ConfigurationService configurationService) {
        if (this.configurationService == configurationService) {
            this.configurationService = null;
        }
    }

    void initReciever(BundleContext context) {
        PortAggregatorFactory factory = context
                .getService(context.getServiceReference(CachingAggregatorFactory.class));
        factory.build(wireSupport.getReceiverPorts()).onWireReceive(this::onWireReceive);

    }

    protected void activate(final ComponentContext ctx, final Map<String, Object> properties) {
        this.wireSupport = (MultiportWireSupport) this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) ctx.getServiceReference());
        initReciever(ctx.getBundleContext());
        modified(properties);

    }

    protected void modified(final Map<String, Object> properties) {
        logger.info("UPDATING CALLED {}", properties.get("kura.service.pid"));

        gatewayOptions = GatewayOptions.getInstance(properties);

        if (netWorker != null) {
            netWorker.stopAll();
        }

        netWorker = new NetWorker();

        if (graphWorker == null) {
            graphWorker = new GraphWorker(wireGraphService, gatewayOptions.getCurrentPID());
        }
        graphWorker.updatePorts(gatewayOptions.getEmitterWiresCount(), gatewayOptions.getRecieverWiresCount());
        netWorker.initServer(gatewayOptions.getLocalPort(), this);
        netWorker.initConnection(gatewayOptions.getRemoteIP(), gatewayOptions.getRemotePort(), 3);
    }

    protected void deactivate(final ComponentContext ctx) {
        logger.info("Dectivating gateway...");
        if (this.netWorker != null) {
            this.netWorker.stopAll();
        }
        logger.info("Dectivating gateway... Done");
    }

    @Override
    public void consumersConnected(final Wire[] wires) {
        logger.info("Consumers connected {}", wires.length);
        this.wireSupport.consumersConnected(wires);
//        this.wireSupport.consumersConnected((Wire[]) Arrays.stream(wires).filter(Objects::nonNull).toArray());
    }

    @Override
    public Object polled(final Wire wire) {
        return this.wireSupport.polled(wire);
    }

    public void onWireReceive(List<WireEnvelope> envelopes) {
//        logger.info("Wire Recieved:{}", envelopes.size());
////                envelopes.stream().filter(Objects::nonNull).findFirst().get().getRecords().toString());
//        envelopes.forEach(wireEnvelope -> {
//            netWorker.sendData(wireEnvelope.getRecords());
//        });
    }

    public void setWires(Wire[] wires) {
        this.wires = wires;
    }

    @Override
    public void producersConnected(final Wire[] wires) {
        for (int i = 0; i < wires.length; i++) {
            wiresIndexes.put(wires[i], i);
        }
        setWires(wires);
        logger.info("ProducersConnected:{}", Arrays.toString(wires));
        this.wireSupport.producersConnected(wires);
    }


    @Override
    public void updated(final Wire wire, final Object value) {
        logger.info("UPDATED {}", wire.toString());

        if (value instanceof WireEnvelope) {
            netWorker.sendData(wiresIndexes.get(wire), ((WireEnvelope) value).getRecords());
        }
//
        this.wireSupport.updated(wire, value);
    }


    @Override
    public void onRecieve(int wireIndex, List<WireRecord> wireRecords) {
        logger.info("emmiting data:{},{}", wireIndex, Arrays.toString(wireRecords.toArray()));
        try {
            this.wireSupport.getEmitterPorts().get(wireIndex).emit(
                    new WireEnvelope(gatewayOptions.getCurrentPID(), wireRecords));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //    @Override
    public void onWireReceive(WireEnvelope wireEnvelope) {
        logger.info("Wire Recieved:{}", wireEnvelope.getEmitterPid());
//                envelopes.stream().filter(Objects::nonNull).findFirst().get().getRecords().toString());
//        envelopes.forEach(wireEnvelope -> {
        netWorker.sendData(0, wireEnvelope.getRecords());
//        });
    }
}
