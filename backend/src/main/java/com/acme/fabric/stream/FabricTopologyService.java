package com.acme.fabric.stream;

import static com.acme.fabric.domain.FabricModels.NodeRole.APPLICATION_SERVER;
import static com.acme.fabric.domain.FabricModels.NodeRole.CCNP_EDGE_CORE;
import static com.acme.fabric.domain.FabricModels.NodeRole.DATABASE_SERVER;
import static com.acme.fabric.domain.FabricModels.NodeRole.FIREWALL_APPLIANCE;
import static com.acme.fabric.domain.FabricModels.NodeRole.HONEYPOT_DECOY;
import static com.acme.fabric.domain.FabricModels.NodeStatus.PROVISIONED;
import static com.acme.fabric.domain.FabricModels.NodeStatus.RUNNING;

import java.util.List;

import com.acme.fabric.domain.FabricModels.FabricEdge;
import com.acme.fabric.domain.FabricModels.FabricNode;
import com.acme.fabric.domain.FabricModels.FabricTopology;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class FabricTopologyService {
    private final FabricTopology topology = new FabricTopology(
            List.of(
                    new FabricNode("edge-ingress-router-r1", "edge-ingress-router-r1", CCNP_EDGE_CORE, RUNNING, 150, 120),
                    new FabricNode("honeypot-trap-router-vty", "honeypot-trap-router-vty", HONEYPOT_DECOY, RUNNING, 420, 120),
                    new FabricNode("distributed-db-shard-01", "distributed-db-shard-01", DATABASE_SERVER, PROVISIONED, 690, 120),
                    new FabricNode("fabric-firewall-acl", "fabric-firewall-acl", FIREWALL_APPLIANCE, RUNNING, 285, 330),
                    new FabricNode("l7-socket-gateway", "l7-socket-gateway", APPLICATION_SERVER, RUNNING, 560, 330)
            ),
            List.of(
                    new FabricEdge("edge-r1-fw", "edge-ingress-router-r1", "fabric-firewall-acl", "ACL"),
                    new FabricEdge("edge-fw-honey", "fabric-firewall-acl", "honeypot-trap-router-vty", "DECOY"),
                    new FabricEdge("edge-r1-db", "edge-ingress-router-r1", "distributed-db-shard-01", "BGP"),
                    new FabricEdge("edge-db-l7", "distributed-db-shard-01", "l7-socket-gateway", "VXLAN")
            )
    );

    public Mono<FabricTopology> current() {
        return Mono.just(topology);
    }
}
