<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import {
  createFirewallRule,
  executeFilePipeline,
  executeQuantumCircuit,
  fetchFirewallRules,
  fetchHoneypotIncidents,
  provisionServer,
  runTerminalCommand,
  scrambleIdentity,
  spawnSocket,
  stageFileMetadata,
  tracePacket
} from './api/fabricApi';
import FabricCanvas from './components/FabricCanvas.vue';
import PlatformFunctionsPanel from './components/PlatformFunctionsPanel.vue';
import SmsDispatchPanel from './components/SmsDispatchPanel.vue';

const tabs = [
  ['dashboard', 'Main Dashboard'],
  ['servers', 'Enterprise Provisioning'],
  ['sms-gateway', 'SMS Gateway Hub'],
  ['terminal-access', 'SSH / Telnet Terminals'],
  ['security-plane', 'Firewall & Honeypots'],
  ['packet-tracing', 'CCNP Packet Tracing'],
  ['file-transport', 'File Transport Plane'],
  ['quantum', 'Quantum Core'],
  ['networking', 'L7 Sockets'],
  ['polyglot-functions', 'Polyglot Functions']
];

const tabCopy = {
  dashboard: ['System Infrastructure Control Room', 'Unified master plane lifecycle validation interface.'],
  servers: ['Enterprise Cluster Provisioning Bay', 'Inject dynamic microcode metadata parameters across server resource blueprints.'],
  'sms-gateway': ['SMS Telecom Gateway Aggregator Hub', 'Realtime critical SMS alerting dispatcher framework and transaction monitor.'],
  'terminal-access': ['Secure Remote VTY Terminals Console', 'Direct cryptographic console loop pipelines into cluster edge endpoints.'],
  'security-plane': ['Distributed Firewall Policy Engine & HoneyMesh Planes', 'Ingest attack signature matrices and state filtering configurations.'],
  'packet-tracing': ['CCNP Enterprise Multi-Hop Packet Inspection Tracing System', 'Simulate routing paths, encapsulation tags, and frame headers.'],
  'file-transport': ['Asynchronous Data Broker File Transport Plane', 'Attach network diagnostic packages directly to running clusters.'],
  quantum: ['Quantum Telemetry Core Grid Sandbox', 'Dispatch experimental algorithmic circuits to superconducting loops.'],
  networking: ['Polymorphic Layer 7 Emulation Socket Bindings', 'Manage API reverse-proxy hooks and transport dialect bindings.'],
  'polyglot-functions': ['GraalVM Polyglot Function Factory', 'Generate platform functions and test metadata-only routing scripts.']
};

const activeTab = ref('dashboard');
const toast = ref('');
const inventory = ref([
  { id: 'vm-1', name: 'edge-ingress-router-r1', role: 'CCNP_EDGE_CORE', platform: 'QEMU / KVM Host Bridge', ip: '10.194.24.102', uptime: '142h 11m', state: 'RUNNING' },
  { id: 'vm-2', name: 'honeypot-trap-router-vty', role: 'HONEYPOT_DECOY', platform: 'Docker Container Daemon', ip: '10.194.24.220', uptime: '89h 04m', state: 'RUNNING' },
  { id: 'vm-3', name: 'distributed-db-shard-01', role: 'DATABASE_SERVER', platform: 'VMware ESXi Hypervisor', ip: '192.168.1.45', uptime: '00h 00m', state: 'PROVISIONED' }
]);
const firewallRules = ref([]);
const incidents = ref([]);
const terminalOutput = ref(['[SYSTEM] Terminal transport idle. Establish a secure session and execute parser commands.']);
const traceResult = ref(null);
const stagedFiles = ref([]);
const pipelines = ref([]);
const quantumOutput = ref('System core idle. Submit a compiled circuit mapping array to monitor gate transformations...');
const socketOutput = ref('');

const serverForm = reactive({
  serverName: 'prod-zone-edge-hypervisor',
  parentNodeId: '4fa218ce-bc31-4122-aa11-82ff3b1029da',
  deploymentFramework: 'VIRTUAL_MACHINE',
  executionScope: 'REAL',
  targetRoleClass: 'DATABASE_SERVER',
  databaseEngine: 'POSTGRESQL',
  localListeningPort: 5432,
  runtimeEnvironment: 'NODEJS_V8',
  targetServicePort: 8080,
  simulatedVulnerabilityPersona: 'VULNERABLE_SSH',
  telemetryAttackIncidentAlertingQueue: 'amqp://sec-broker/alerts.honeypot'
});
const terminalForm = reactive({ targetedHostEndpoint: '10.194.24.102', protocolDialectMode: 'SSH', rawCommandText: 'show interface brief' });
const firewallForm = reactive({ sourceCidrBlock: '10.0.0.0/8', networkDestinationPort: '23/TCP', policyAction: 'DROP' });
const packetForm = reactive({ sourcePointNode: 'Host-Alpha (VLAN 10)', destinationIpNode: '10.194.24.102', protocolDialectType: 'TCP_SYN', taggingLayerParam: 'IEEE_802_1Q_TAG_10' });
const quantumForm = reactive({ physicalTargetQpu: 'SIMULATED_POLY_MATH_MATRIX', sequenceGateDepth: 64, processingAlgorithmClass: 'VQE_MOLECULAR_SIMULATION' });
const socketForm = reactive({ localPort: 9000, proxyTarget: 'GRPC_PROTO' });

const pageTitle = computed(() => tabCopy[activeTab.value][0]);
const pageSubtitle = computed(() => tabCopy[activeTab.value][1]);
const runningCount = computed(() => inventory.value.filter((item) => item.state === 'RUNNING').length);
const firewallDropCounter = computed(() => (1842 + firewallRules.value.filter((rule) => rule.policyAction === 'DROP').length * 4).toLocaleString());

onMounted(async () => {
  try {
    firewallRules.value = await fetchFirewallRules();
    incidents.value = await fetchHoneypotIncidents();
  } catch {
    notify('Backend control-plane APIs are still warming up.');
  }
});

function notify(message) {
  toast.value = message;
  window.setTimeout(() => {
    if (toast.value === message) toast.value = '';
  }, 3200);
}

async function scrambleCoordinates() {
  await scrambleIdentity();
  inventory.value = inventory.value.map((node) => ({
    ...node,
    ip: node.ip.startsWith('10.')
      ? `10.${Math.floor(Math.random() * 254)}.${Math.floor(Math.random() * 254)}.${Math.floor(Math.random() * 254)}`
      : `192.168.${Math.floor(Math.random() * 254)}.${Math.floor(Math.random() * 254)}`
  }));
  notify('Asynchronous cluster coordinate scramble committed safely.');
}

async function submitServer() {
  const payload = {
    serverName: serverForm.serverName,
    parentNodeId: serverForm.parentNodeId,
    deploymentFramework: serverForm.deploymentFramework,
    executionScope: serverForm.executionScope,
    targetRoleClass: serverForm.targetRoleClass,
    dbRoleConfig: serverForm.targetRoleClass === 'DATABASE_SERVER' ? {
      databaseEngine: serverForm.databaseEngine,
      localListeningPort: Number(serverForm.localListeningPort)
    } : undefined,
    appRoleConfig: serverForm.targetRoleClass === 'APPLICATION_SERVER' ? {
      runtimeEnvironment: serverForm.runtimeEnvironment,
      targetServicePort: Number(serverForm.targetServicePort)
    } : undefined,
    honeypotConfig: serverForm.targetRoleClass === 'HONEYPOT_DECOY' ? {
      simulatedVulnerabilityPersona: serverForm.simulatedVulnerabilityPersona,
      telemetryAttackIncidentAlertingQueue: serverForm.telemetryAttackIncidentAlertingQueue
    } : undefined
  };
  const response = await provisionServer(payload);
  inventory.value = [{
    id: response.serverId,
    name: payload.serverName,
    role: payload.targetRoleClass,
    platform: `${payload.deploymentFramework} (${payload.executionScope})`,
    ip: response.allocationMetadata.assignedClusterIp,
    uptime: '00h 01m',
    state: response.runtimeState
  }, ...inventory.value];
  notify(`Node allocation loop '${payload.serverName}' triggered successfully.`);
  activeTab.value = 'dashboard';
}

async function commitFirewallRule() {
  const rule = await createFirewallRule({ ...firewallForm });
  firewallRules.value = [rule, ...firewallRules.value];
  notify(`Firewall structural rule sequence injected -> ${rule.policyAction}.`);
}

async function executeTrace() {
  traceResult.value = await tracePacket({ ...packetForm });
  notify('Multi-hop diagnostic route verification trace resolved.');
}

async function stageFile(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  const response = await stageFileMetadata({
    assetName: file.name,
    assetByteSizeBytes: file.size,
    mediaMimeType: file.type || 'application/octet-stream'
  });
  stagedFiles.value = [{ ...response, name: file.name, size: file.size, type: file.type || 'application/octet-stream' }, ...stagedFiles.value];
  notify(`Resource payload '${file.name}' staged to control storage array.`);
}

async function pipeFile(file) {
  const response = await executeFilePipeline({
    targetAssetToken: file.stagedAssetToken,
    deploymentDestinationHost: '10.194.24.102'
  });
  pipelines.value = [{ ...response, assetName: file.name, progress: 100 }, ...pipelines.value];
  notify(`Asynchronous stream injection finalized for asset: ${file.name}.`);
}

async function executeTerminalCommand() {
  const response = await runTerminalCommand({ ...terminalForm });
  terminalOutput.value = [`${terminalForm.protocolDialectMode.toLowerCase()}$ ${terminalForm.rawCommandText}`, response.stdoutBuffer, ...terminalOutput.value];
}

async function executeQuantum() {
  quantumOutput.value = '[PROCESSING] Dispatching quantum state register layout frames directly to optimization array...';
  const response = await executeQuantumCircuit({ ...quantumForm, sequenceGateDepth: Number(quantumForm.sequenceGateDepth) });
  quantumOutput.value = JSON.stringify(response, null, 2);
  notify('Quantum acceleration matrix calculation block accepted.');
}

async function submitSocket() {
  const response = await spawnSocket({ ...socketForm, localPort: Number(socketForm.localPort) });
  socketOutput.value = `Socket ${response.socketInterfaceId} -> ${response.bindingState}`;
  notify(`Socket framework provisioned over port ${socketForm.localPort}.`);
}
</script>

<template>
  <main class="control-room">
    <aside class="nav-rail">
      <div>
        <div class="brand-block">
          <div class="brand-mark">FE</div>
          <div>
            <h1>Fabric Engine</h1>
            <p>v2.1.0 · MULTI-CLUSTER</p>
          </div>
        </div>

        <nav class="nav-list">
          <button
            v-for="[id, label] in tabs"
            :key="id"
            :class="['nav-button', { active: activeTab === id }]"
            @click="activeTab = id"
          >
            {{ label }}
          </button>
        </nav>
      </div>

      <div class="nav-footer">
        <button class="utility-button" @click="activeTab = 'packet-tracing'">Packet Canvas</button>
        <button class="utility-button amber" @click="activeTab = 'terminal-access'">VDI Workspace</button>
        <label>
          Gateway Sync Target
          <select>
            <option>http://localhost:8080/api/v1</option>
            <option>https://api.sdn.network.engine.internal/api/v1</option>
          </select>
        </label>
      </div>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div>
          <h2>{{ pageTitle }}</h2>
          <p>{{ pageSubtitle }}</p>
        </div>
        <span class="status-chip green">Ollama Local · Reactive</span>
      </header>

      <section v-if="activeTab === 'dashboard'" class="tab-page">
        <div class="metric-grid">
          <article class="metric-card"><span>Hypervisor VMs</span><strong>{{ runningCount }} / {{ inventory.length }} Running</strong></article>
          <article class="metric-card emerald"><span>Honeypot Decoys</span><strong>{{ incidents.length || 3 }} Active</strong></article>
          <article class="metric-card red"><span>Firewall Drops</span><strong>{{ firewallDropCounter }}</strong></article>
          <article class="metric-card purple"><span>CCNP Node Links</span><strong>4 Active Pipes</strong></article>
          <article class="metric-card amber"><span>Quantum Volume</span><strong>2,048</strong></article>
        </div>

        <section class="panel">
          <div class="panel-title-row">
            <div>
              <h3>Hypervisor Cluster Nodes & VM Compute Instances</h3>
              <p class="muted">Live hypervisor states showing microcode architectures and dynamic startup routines.</p>
            </div>
            <button class="danger-button" @click="scrambleCoordinates">Scramble IP Coordinates</button>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr><th>Instance / VM Label</th><th>Role</th><th>Platform</th><th>Target IP</th><th>Uptime</th><th>State</th></tr>
              </thead>
              <tbody>
                <tr v-for="item in inventory" :key="item.id">
                  <td>{{ item.name }}</td><td>{{ item.role }}</td><td>{{ item.platform }}</td><td>{{ item.ip }}</td><td>{{ item.uptime }}</td><td><span class="status-chip blue">{{ item.state }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <FabricCanvas />
      </section>

      <section v-if="activeTab === 'servers'" class="tab-page">
        <section class="panel narrow">
          <div class="endpoint-line"><span class="method post">POST</span><code>/systems/servers</code></div>
          <h3>Provision New Cluster Core Node Instance</h3>
          <form class="form-grid" @submit.prevent="submitServer">
            <div class="form-pair">
              <label>Server Display Tag<input v-model="serverForm.serverName" required /></label>
              <label>Parent Hardware UUID Mapping<input v-model="serverForm.parentNodeId" required /></label>
            </div>
            <div class="form-pair">
              <label>Deployment Framework Target<select v-model="serverForm.deploymentFramework"><option>BARE_METAL_PHYSICAL</option><option>VIRTUAL_MACHINE</option></select></label>
              <label>Execution Framework Scope<select v-model="serverForm.executionScope"><option>REAL</option><option>SIMULATED</option></select></label>
            </div>
            <div class="segmented">
              <button type="button" :class="{ active: serverForm.targetRoleClass === 'DATABASE_SERVER' }" @click="serverForm.targetRoleClass = 'DATABASE_SERVER'">Database Engine</button>
              <button type="button" :class="{ active: serverForm.targetRoleClass === 'APPLICATION_SERVER' }" @click="serverForm.targetRoleClass = 'APPLICATION_SERVER'">Application Host</button>
              <button type="button" :class="{ active: serverForm.targetRoleClass === 'HONEYPOT_DECOY' }" @click="serverForm.targetRoleClass = 'HONEYPOT_DECOY'">Honeypot Decoy Target</button>
            </div>
            <div v-if="serverForm.targetRoleClass === 'DATABASE_SERVER'" class="subschema">
              <label>Database Engine<select v-model="serverForm.databaseEngine"><option>POSTGRESQL</option><option>MONGO_DB</option><option>MYSQL</option><option>MS_SQL</option></select></label>
              <label>Local Listening Target Port<input v-model="serverForm.localListeningPort" type="number" /></label>
            </div>
            <div v-if="serverForm.targetRoleClass === 'APPLICATION_SERVER'" class="subschema">
              <label>Runtime Environment<input v-model="serverForm.runtimeEnvironment" /></label>
              <label>Target Application Service Port<input v-model="serverForm.targetServicePort" type="number" /></label>
            </div>
            <div v-if="serverForm.targetRoleClass === 'HONEYPOT_DECOY'" class="subschema">
              <label>Simulated Vulnerability<select v-model="serverForm.simulatedVulnerabilityPersona"><option>VULNERABLE_SSH</option><option>EXPOSED_TELNET</option><option>EXPOSED_REDIS</option></select></label>
              <label>Incident Queue<input v-model="serverForm.telemetryAttackIncidentAlertingQueue" /></label>
            </div>
            <button class="primary-button">Spark Lifecycle Provisioning Instance</button>
          </form>
        </section>
      </section>

      <section v-if="activeTab === 'sms-gateway'" class="tab-page">
        <SmsDispatchPanel />
      </section>

      <section v-if="activeTab === 'terminal-access'" class="tab-page two-col">
        <section class="panel">
          <h3>Terminal Session</h3>
          <form class="form-grid" @submit.prevent="executeTerminalCommand">
            <label>Target IP<select v-model="terminalForm.targetedHostEndpoint"><option>10.194.24.102</option><option>10.194.24.220</option><option>192.168.1.1</option></select></label>
            <label>Protocol<select v-model="terminalForm.protocolDialectMode"><option>SSH</option><option>TELNET</option></select></label>
            <label>Command<input v-model="terminalForm.rawCommandText" /></label>
            <button class="primary-button">Execute Command</button>
          </form>
        </section>
        <pre class="console-frame">{{ terminalOutput.join('\n\n') }}</pre>
      </section>

      <section v-if="activeTab === 'security-plane'" class="tab-page two-col">
        <section class="panel">
          <h3>Firewall Protection Rules</h3>
          <form class="form-grid" @submit.prevent="commitFirewallRule">
            <label>Source CIDR<input v-model="firewallForm.sourceCidrBlock" /></label>
            <label>Destination Port<input v-model="firewallForm.networkDestinationPort" /></label>
            <label>Policy Action<select v-model="firewallForm.policyAction"><option>DROP</option><option>ACCEPT</option></select></label>
            <button class="primary-button">Commit Firewall Rule</button>
          </form>
          <div class="event-stack">
            <article v-for="rule in firewallRules" :key="rule.sourceCidrBlock + rule.networkDestinationPort" class="event-row">
              <strong>{{ rule.sourceCidrBlock }}</strong><span>{{ rule.networkDestinationPort }} · {{ rule.policyAction }}</span>
            </article>
          </div>
        </section>
        <section class="panel">
          <h3>HoneyMesh Incidents</h3>
          <article v-for="incident in incidents" :key="incident.incidentUuid" class="event-row danger">
            <strong>{{ incident.attackerIpSource }}</strong>
            <span>{{ incident.vectorProfile }} · :{{ incident.targetedPort }}</span>
          </article>
        </section>
      </section>

      <section v-if="activeTab === 'packet-tracing'" class="tab-page">
        <section class="panel">
          <h3>CCNP Multi-Hop Packet Inspection</h3>
          <form class="form-grid inline-grid" @submit.prevent="executeTrace">
            <label>Source Node<input v-model="packetForm.sourcePointNode" /></label>
            <label>Destination IP<input v-model="packetForm.destinationIpNode" /></label>
            <label>Protocol<select v-model="packetForm.protocolDialectType"><option>TCP_SYN</option><option>UDP_BROADCAST</option><option>ICMP_ECHO_REQUEST</option></select></label>
            <label>Tag<select v-model="packetForm.taggingLayerParam"><option>IEEE_802_1Q_TAG_10</option><option>MPLS_LABEL_404</option><option>NONE</option></select></label>
            <button class="primary-button">Execute Trace</button>
          </form>
        </section>
        <FabricCanvas />
        <pre v-if="traceResult" class="console-frame">{{ JSON.stringify(traceResult, null, 2) }}</pre>
      </section>

      <section v-if="activeTab === 'file-transport'" class="tab-page two-col">
        <section class="panel">
          <h3>Diagnostic Resource Asset Payload</h3>
          <input class="file-input" type="file" @change="stageFile" />
          <article v-for="file in stagedFiles" :key="file.stagedAssetToken" class="event-row">
            <strong>{{ file.name }}</strong><span>{{ file.stagedAssetToken }} · {{ file.type }}</span>
            <button class="secondary-button" @click="pipeFile(file)">Pipe into SDN</button>
          </article>
        </section>
        <section class="panel">
          <h3>Streaming Allocations</h3>
          <article v-for="pipe in pipelines" :key="pipe.pipelineWidgetId" class="event-row">
            <strong>{{ pipe.assetName }}</strong><span>{{ pipe.pipelineWidgetId }} · {{ pipe.initialState }}</span>
          </article>
        </section>
      </section>

      <section v-if="activeTab === 'quantum'" class="tab-page two-col">
        <section class="panel">
          <h3>Quantum Circuit Execution</h3>
          <form class="form-grid" @submit.prevent="executeQuantum">
            <label>Physical Target QPU<select v-model="quantumForm.physicalTargetQpu"><option>IBM_BRYCE_CANYON_Q3</option><option>RIGETTI_ASPEN_M3</option><option>SIMULATED_POLY_MATH_MATRIX</option></select></label>
            <label>Sequence Gate Depth<input v-model="quantumForm.sequenceGateDepth" type="number" min="1" max="1000" /></label>
            <label>Algorithm Class<select v-model="quantumForm.processingAlgorithmClass"><option>VQE_MOLECULAR_SIMULATION</option><option>SHORS_FACTORIZATION</option><option>GROVERS_AMPLIFICATION_SEARCH</option></select></label>
            <button class="primary-button">Dispatch Quantum Circuit</button>
          </form>
        </section>
        <pre class="console-frame">{{ quantumOutput }}</pre>
      </section>

      <section v-if="activeTab === 'networking'" class="tab-page two-col">
        <section class="panel">
          <h3>Layer 7 Emulation Socket Binding</h3>
          <form class="form-grid" @submit.prevent="submitSocket">
            <label>Local Port<input v-model="socketForm.localPort" type="number" min="1" max="65535" /></label>
            <label>Proxy Target<select v-model="socketForm.proxyTarget"><option>HTTP_JSON</option><option>GRPC_PROTO</option><option>GRAPHQL</option></select></label>
            <button class="primary-button">Spawn Listener</button>
          </form>
        </section>
        <pre class="console-frame">{{ socketOutput || 'Socket interface idle.' }}</pre>
      </section>

      <section v-if="activeTab === 'polyglot-functions'" class="tab-page">
        <PlatformFunctionsPanel />
      </section>
    </section>

    <div v-if="toast" class="toast">{{ toast }}</div>
  </main>
</template>
