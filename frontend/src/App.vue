<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import {
  askAiGuide,
  createFirewallRule,
  createLocalVm,
  executeFilePipeline,
  executeQuantumCircuit,
  fetchHardwareOverview,
  fetchFirewallRules,
  fetchHoneypotIncidents,
  fetchLocalVmProviders,
  provisionServer,
  registerDevice,
  runTerminalCommand,
  scrambleIdentity,
  spawnSocket,
  stageFileMetadata,
  tracePacket
} from './api/fabricApi';
import FabricCanvas from './components/FabricCanvas.vue';
import PlatformFunctionsPanel from './components/PlatformFunctionsPanel.vue';
import SmsDispatchPanel from './components/SmsDispatchPanel.vue';

const navGroups = [
  {
    label: 'Overview',
    tabs: [
      ['dashboard', 'Main Dashboard'],
      ['360-view', '360 View'],
      ['approvals', 'Approval Dashboard']
    ]
  },
  {
    label: 'Infrastructure',
    tabs: [
      ['servers', 'Enterprise Provisioning'],
      ['local-vms', 'Local VM Workstation'],
      ['terminal-access', 'SSH / Telnet Terminals']
    ]
  },
  {
    label: 'Network & Security',
    tabs: [
      ['security-plane', 'Firewall & Honeypots'],
      ['packet-tracing', 'CCNP Packet Tracing'],
      ['file-transport', 'File Transport Plane'],
      ['networking', 'L7 Sockets']
    ]
  },
  {
    label: 'Automation',
    tabs: [
      ['sms-gateway', 'SMS Gateway Hub'],
      ['quantum', 'Quantum Core'],
      ['polyglot-functions', 'Polyglot Functions']
    ]
  },
  {
    label: 'Product',
    tabs: [
      ['about', 'About Reactor']
    ]
  }
];

const tabCopy = {
  dashboard: ['System Infrastructure Control Room', 'Unified master plane lifecycle validation interface.'],
  '360-view': ['360 View', 'Physical host diagnostics, actuator-style runtime panels, and AI operations guidance.'],
  approvals: ['Founder Approval Dashboard', 'Review CEO-agent recommendations before Reactor changes infrastructure state.'],
  servers: ['Enterprise Cluster Provisioning Bay', 'Inject dynamic microcode metadata parameters across server resource blueprints.'],
  'local-vms': ['Local VM Workstation', 'Create physical-host VMs through VMware vmrun or QEMU with guarded execution.'],
  'sms-gateway': ['SMS Telecom Gateway Aggregator Hub', 'Realtime critical SMS alerting dispatcher framework and transaction monitor.'],
  'terminal-access': ['Secure Remote VTY Terminals Console', 'Direct cryptographic console loop pipelines into cluster edge endpoints.'],
  'security-plane': ['Distributed Firewall Policy Engine & HoneyMesh Planes', 'Ingest attack signature matrices and state filtering configurations.'],
  'packet-tracing': ['CCNP Enterprise Multi-Hop Packet Inspection Tracing System', 'Simulate routing paths, encapsulation tags, and frame headers.'],
  'file-transport': ['Asynchronous Data Broker File Transport Plane', 'Attach network diagnostic packages directly to running clusters.'],
  quantum: ['Quantum Telemetry Core Grid Sandbox', 'Dispatch experimental algorithmic circuits to superconducting loops.'],
  networking: ['Polymorphic Layer 7 Emulation Socket Bindings', 'Manage API reverse-proxy hooks and transport dialect bindings.'],
  'polyglot-functions': ['GraalVM Polyglot Function Factory', 'Generate platform functions and test metadata-only routing scripts.'],
  about: ['About Reactor', 'Why this control plane exists and how it helps operators move faster.']
};

const activeTab = ref('dashboard');
const themeMode = ref('dark');
const openNavGroups = reactive(Object.fromEntries(navGroups.map((group) => [
  group.label,
  group.tabs.some(([id]) => id === activeTab.value)
])));
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
const quantumStatus = ref('STANDBY');
const socketOutput = ref('');
const hardwareOverview = ref(null);
const hardwareHistory = ref([]);
const hardwareLastRefresh = ref('');
const hardwareRefreshState = ref('starting');
const localVmProviders = ref([]);
const localVmResult = ref(null);
const deviceRegistration = ref(null);
const aiGuideOpen = ref(true);
const aiGuideInput = ref('How do I provision an AI VM and register it on the fabric?');
const aiGuideMessages = ref([
  { role: 'guide', text: 'Namaste, Aarohi here. I operate as Reactor CEO-agent: I set product direction, prioritize risk, and prepare decisions. You are the founder/owner, so I will ask your approval before any action changes the platform.' }
]);
const aiVoiceEnabled = ref(true);
const aiVoiceGender = ref('female');
const aiLanguage = ref('auto');
const aiPendingActions = ref([]);
const approvalHistory = ref([]);
const aiListening = ref(false);
const aiVoiceMode = ref('natural');
const aiInterimTranscript = ref('');
const aiGuideStatus = ref('Ready to listen');
const browserVoices = ref([]);
let speechRecognition;
let speechRestartTimer;
let speakingUtterances = [];
let diagnosticsTimer;
const packetCanvasOpen = ref(false);
const vdiOpen = ref(false);
const vdiConnected = ref(false);
const vdiOutput = ref([
  '[SYSTEM] Initiating virtual core engine lifecycle state loop over cluster link adapter...',
  '[SYSTEM] Session Authenticated. Welcome to Ubuntu Core Appliance Base (x86_64 architecture node link)',
  "Type 'help' to review simulated operating system guest terminal actions."
]);
const vdiCommand = ref('');
const vdiForm = reactive({ targetNodeId: 'vm-1', sessionMode: 'CLI' });
const activeVdiApp = ref('workbench');
const vdiSharedFiles = ref([]);
const terminalConnected = ref(false);
const terminalUser = ref('cisco_admin');
const terminalPrompt = computed(() => (terminalForm.protocolDialectMode === 'SSH' ? 'ssh$' : 'telnet>'));
const packetHops = ref([]);
const packetHexDump = ref('Awaiting SDN Packet Dispatch Trigger Matrix...');
const packetByteWeight = ref('0 bytes parsed');
const dragOver = ref(false);
const folderUploadInput = ref(null);

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
  telemetryAttackIncidentAlertingQueue: 'amqp://sec-broker/alerts.honeypot',
  placementMode: 'PHYSICAL_HOST_VM',
  registeredMacAddress: '02:42:ac:11:00:21',
  internetIpAddress: '192.168.1.120',
  fabricIpAddress: '10.194.24.180',
  modelRuntime: 'OLLAMA_LLAMA_3_2',
  acceleratorProfile: 'CPU_LOCAL',
  inferencePort: 11434,
  qpuSimulator: 'QISKIT_AER_VM',
  qubitCount: 32,
  circuitRuntime: 'OPENQASM_3',
  bootstrapEndpoint: 'ssh://fabric-node-02.local',
  checkScript: 'scripts/check-node-eligibility.sh',
  installScript: 'scripts/promote-node-server.sh'
});
const terminalForm = reactive({ targetedHostEndpoint: '10.194.24.102', protocolDialectMode: 'SSH', rawCommandText: 'show interface brief' });
const firewallForm = reactive({ sourceCidrBlock: '10.0.0.0/8', networkDestinationPort: '23/TCP', policyAction: 'DROP' });
const packetForm = reactive({ sourcePointNode: 'Host-Alpha (VLAN 10)', destinationIpNode: '10.194.24.102', protocolDialectType: 'TCP_SYN', taggingLayerParam: 'IEEE_802_1Q_TAG_10' });
const quantumForm = reactive({ physicalTargetQpu: 'SIMULATED_POLY_MATH_MATRIX', sequenceGateDepth: 64, processingAlgorithmClass: 'VQE_MOLECULAR_SIMULATION' });
const socketForm = reactive({ localPort: 9000, proxyTarget: 'GRPC_PROTO' });
const transferForm = reactive({ sourceNodeId: 'vm-1', destinationNodeId: 'vm-2' });
const localVmForm = reactive({
  vmName: 'reactor-ai-vm-01',
  provider: 'VMWARE',
  guestOsType: 'ubuntu-64',
  cpuCores: 4,
  memoryMb: 8192,
  diskGb: 80,
  isoPath: '',
  networkMode: 'NAT',
  startAfterCreate: false
});

const pageTitle = computed(() => tabCopy[activeTab.value][0]);
const pageSubtitle = computed(() => tabCopy[activeTab.value][1]);
const runningCount = computed(() => inventory.value.filter((item) => item.state === 'RUNNING').length);
const firewallDropCounter = computed(() => (1842 + firewallRules.value.filter((rule) => rule.policyAction === 'DROP').length * 4).toLocaleString());
const honeypotCount = computed(() => Math.max(3, incidents.value.length || 0));
const transferSourceNode = computed(() => inventory.value.find((node) => node.id === transferForm.sourceNodeId) ?? inventory.value[0]);
const transferDestinationNode = computed(() => inventory.value.find((node) => node.id === transferForm.destinationNodeId) ?? inventory.value[1] ?? inventory.value[0]);
const selectedVdiNode = computed(() => inventory.value.find((node) => node.id === vdiForm.targetNodeId) ?? inventory.value[0]);
const aiPersonaName = computed(() => (aiVoiceGender.value === 'male' ? 'Gabbar' : 'Aarohi'));
const resolvedTheme = computed(() => {
  if (themeMode.value !== 'system') return themeMode.value;
  if (typeof window === 'undefined') return 'dark';
  return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
});
const diagnosticCards = computed(() => {
  const overview = hardwareOverview.value ?? {};
  return [
    {
      label: 'CPU',
      value: `${metricValue(overview.cpu?.loadPercent, 42)}%`,
      detail: `${overview.cpu?.cores ?? 8} cores · ${overview.cpu?.thermalState ?? 'NOMINAL'}`
    },
    {
      label: 'Memory',
      value: `${metricValue(overview.memory?.usedGb, 18)} / ${metricValue(overview.memory?.totalGb, 32)} GB`,
      detail: `${metricValue(overview.memory?.usedPercent, 56)}% used · ${overview.memory?.pressure ?? 'LOW'}`
    },
    {
      label: 'JVM Heap',
      value: `${metricValue(overview.jvm?.heapUsedMb, 256)} MB`,
      detail: `${metricValue(overview.jvm?.heapUsedPercent, 25)}% heap · ${overview.jvm?.loadedClasses ?? 0} classes`
    },
    {
      label: 'Threads',
      value: `${overview.threads?.live ?? 0} live`,
      detail: `${overview.threads?.daemon ?? 0} daemon · ${overview.threads?.peak ?? 0} peak`
    },
    {
      label: 'Process',
      value: `PID ${overview.process?.pid ?? 'local'}`,
      detail: `${formatDuration(overview.process?.uptimeSeconds ?? 0)} uptime`
    },
    {
      label: 'VM Pool',
      value: `${metricValue(overview.storage?.usedGb, 226)} / ${metricValue(overview.storage?.vmPoolGb, 512)} GB`,
      detail: `${overview.vmPool?.capacityState ?? overview.storage?.iopsState ?? 'READY'} · ${overview.vmPool?.providerMode ?? 'guarded'}`
    }
  ];
});
const diagnosticCharts = computed(() => [
  { key: 'cpu', label: 'CPU %', color: '#20c997' },
  { key: 'memory', label: 'Memory %', color: '#ffd166' },
  { key: 'heap', label: 'Heap %', color: '#7c9cff' },
  { key: 'threads', label: 'Threads', color: '#f06c9b' }
]);

onMounted(async () => {
  applyTheme();
  window.matchMedia?.('(prefers-color-scheme: light)').addEventListener?.('change', applyTheme);
  loadBrowserVoices();
  if (typeof window !== 'undefined' && window.speechSynthesis) {
    window.speechSynthesis.onvoiceschanged = loadBrowserVoices;
  }
  try {
    firewallRules.value = await fetchFirewallRules();
    incidents.value = await fetchHoneypotIncidents();
    await refreshHardwareOverview();
    localVmProviders.value = await fetchLocalVmProviders();
  } catch {
    notify('Backend control-plane APIs are still warming up.');
  }
  diagnosticsTimer = window.setInterval(refreshHardwareOverview, 5000);
});

onBeforeUnmount(() => {
  if (typeof window !== 'undefined' && window.speechSynthesis) {
    window.speechSynthesis.cancel();
  }
  speakingUtterances = [];
  if (diagnosticsTimer) {
    window.clearInterval(diagnosticsTimer);
  }
  if (speechRestartTimer) {
    window.clearTimeout(speechRestartTimer);
  }
  speechRecognition?.stop();
  window.matchMedia?.('(prefers-color-scheme: light)').removeEventListener?.('change', applyTheme);
});

watch(themeMode, applyTheme);

function toggleNavGroup(groupLabel) {
  openNavGroups[groupLabel] = !openNavGroups[groupLabel];
}

function selectTab(tabId, groupLabel) {
  activeTab.value = tabId;
  openNavGroups[groupLabel] = true;
}

function notify(message) {
  toast.value = message;
  window.setTimeout(() => {
    if (toast.value === message) toast.value = '';
  }, 3200);
}

function applyTheme() {
  if (typeof document === 'undefined') return;
  document.documentElement.dataset.theme = resolvedTheme.value;
}

async function refreshHardwareOverview() {
  try {
    hardwareRefreshState.value = 'refreshing';
    const overview = await fetchHardwareOverview();
    hardwareOverview.value = overview;
    hardwareLastRefresh.value = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
    hardwareRefreshState.value = 'live';
    recordHardwareSample(overview);
  } catch {
    hardwareRefreshState.value = 'retrying';
  }
}

function recordHardwareSample(overview) {
  const graphs = overview?.graphs ?? {};
  const cpu = Number(graphs.cpuPercent ?? overview?.cpu?.loadPercent ?? 0);
  const memory = Number(graphs.memoryPercent ?? overview?.memory?.usedPercent ?? 0);
  const heap = Number(graphs.heapPercent ?? overview?.jvm?.heapUsedPercent ?? 0);
  const threads = Number(graphs.threadCount ?? overview?.threads?.live ?? 0);
  const next = [...hardwareHistory.value, {
    time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }),
    cpu,
    memory,
    heap,
    threads
  }];
  hardwareHistory.value = next.slice(-24);
}

function chartPoints(key) {
  const values = hardwareHistory.value.map((sample) => Number(sample[key] ?? 0));
  if (!values.length) return '';
  const max = key === 'threads' ? Math.max(20, ...values) : 100;
  return values.map((value, index) => {
    const x = values.length === 1 ? 0 : (index / (values.length - 1)) * 100;
    const y = 54 - Math.min(54, Math.max(0, (value / max) * 54));
    return `${x.toFixed(1)},${y.toFixed(1)}`;
  }).join(' ');
}

function latestChartValue(key) {
  const last = hardwareHistory.value.at(-1);
  if (!last) return key === 'threads' ? '0' : '0%';
  return key === 'threads' ? String(Math.round(last[key] ?? 0)) : `${metricValue(last[key], 0)}%`;
}

function metricValue(value, fallback) {
  const number = Number(value ?? fallback ?? 0);
  return Number.isInteger(number) ? number : number.toFixed(1);
}

function formatDuration(seconds) {
  const safeSeconds = Number(seconds ?? 0);
  const hours = Math.floor(safeSeconds / 3600);
  const minutes = Math.floor((safeSeconds % 3600) / 60);
  return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`;
}

async function submitLocalVm() {
  localVmResult.value = await createLocalVm({
    ...localVmForm,
    cpuCores: Number(localVmForm.cpuCores),
    memoryMb: Number(localVmForm.memoryMb),
    diskGb: Number(localVmForm.diskGb)
  });
  notify(localVmResult.value.executed
    ? `Local VM ${localVmForm.vmName} creation command executed.`
    : `Local VM ${localVmForm.vmName} plan generated in guarded dry-run mode.`);
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
    placementMode: serverForm.placementMode,
    registeredMacAddress: serverForm.registeredMacAddress,
    internetIpAddress: serverForm.internetIpAddress,
    fabricIpAddress: serverForm.fabricIpAddress,
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
    } : undefined,
    aiVmConfig: serverForm.targetRoleClass === 'AI_VM' ? {
      modelRuntime: serverForm.modelRuntime,
      acceleratorProfile: serverForm.acceleratorProfile,
      inferencePort: Number(serverForm.inferencePort)
    } : undefined,
    quantumVmConfig: serverForm.targetRoleClass === 'QUANTUM_VM' ? {
      qpuSimulator: serverForm.qpuSimulator,
      qubitCount: Number(serverForm.qubitCount),
      circuitRuntime: serverForm.circuitRuntime
    } : undefined,
    nodeEligibilityConfig: serverForm.placementMode === 'REMOTE_NODE' ? {
      bootstrapEndpoint: serverForm.bootstrapEndpoint,
      checkScript: serverForm.checkScript,
      installScript: serverForm.installScript
    } : undefined
  };
  const response = await provisionServer(payload);
  deviceRegistration.value = await registerDevice({
    nodeName: payload.serverName,
    macAddress: payload.registeredMacAddress,
    internetIpAddress: payload.internetIpAddress,
    fabricIpAddress: payload.fabricIpAddress,
    deviceClass: payload.targetRoleClass
  });
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
  openActiveNavGroup();
}

async function sendGuideMessage() {
  const message = aiGuideInput.value.trim();
  if (!message) return;
  aiGuideMessages.value = [...aiGuideMessages.value, { role: 'user', text: message }];
  aiGuideInput.value = '';
  const response = await askAiGuide({
    message,
    persona: aiPersonaName.value,
    language: aiLanguage.value,
    context: { activeTab: activeTab.value, inventory: inventory.value }
  });
  aiPendingActions.value = response.actionProposals ?? [];
  aiGuideMessages.value = [...aiGuideMessages.value, { role: 'guide', text: response.response, actions: aiPendingActions.value.map((action) => action.label) }];
  speakGuideResponse(response.response);
}

function loadBrowserVoices() {
  if (typeof window === 'undefined' || !window.speechSynthesis) return;
  browserVoices.value = window.speechSynthesis.getVoices();
}

function preferredVoice() {
  const voices = browserVoices.value;
  const preferredLangs = aiLanguage.value === 'hi-IN'
    ? ['hi-in', 'hi']
    : aiLanguage.value === 'en-US'
      ? ['en-us', 'en']
      : ['en-in', 'hi-in', 'en', 'hi'];
  const languageVoices = voices.filter((voice) => preferredLangs.some((lang) => voice.lang?.toLowerCase().startsWith(lang)));
  const maleHints = ['ravi', 'male', 'amit', 'raj', 'daniel', 'alex', 'fred', 'david', 'mark', 'george', 'arthur'];
  const femaleHints = ['veena', 'female', 'lekha', 'heera', 'samantha', 'victoria', 'karen', 'susan', 'zira', 'sara', 'ava'];
  const hints = aiVoiceGender.value === 'male' ? maleHints : femaleHints;
  return languageVoices.find((voice) => hints.some((hint) => voice.name.toLowerCase().includes(hint)))
    ?? languageVoices[0]
    ?? voices.find((voice) => hints.some((hint) => voice.name.toLowerCase().includes(hint)))
    ?? voices[0];
}

function speakGuideResponse(text) {
  if (!aiVoiceEnabled.value || typeof window === 'undefined' || !window.speechSynthesis) return;
  window.speechSynthesis.cancel();
  speakingUtterances = buildSpeechChunks(text).map((chunk) => {
    const utterance = new SpeechSynthesisUtterance(chunk);
    utterance.voice = preferredVoice();
    utterance.lang = speechLanguage();
    utterance.rate = aiVoiceMode.value === 'natural' ? 0.88 : 0.98;
    utterance.pitch = aiVoiceGender.value === 'male' ? 0.82 : 1.05;
    utterance.volume = 1;
    return utterance;
  });
  speakNextChunk();
}

function buildSpeechChunks(text) {
  const cleanText = humanizeGuideText(text);
  const sentences = cleanText.match(/[^.!?।]+[.!?।]?/g) ?? [cleanText];
  const chunks = [];
  let current = '';
  sentences.forEach((sentence) => {
    const next = `${current} ${sentence}`.trim();
    if (next.length > 180 && current) {
      chunks.push(current);
      current = sentence.trim();
    } else {
      current = next;
    }
  });
  if (current) chunks.push(current);
  return chunks;
}

function humanizeGuideText(text) {
  const prefix = aiLanguage.value === 'hi-IN'
    ? `${aiPersonaName.value}: theek hai, suno. `
    : `${aiPersonaName.value}: alright, I am with you. `;
  return text.startsWith(aiPersonaName.value) ? text : `${prefix}${text}`;
}

function speakNextChunk() {
  const utterance = speakingUtterances.shift();
  if (!utterance || typeof window === 'undefined' || !window.speechSynthesis) {
    aiGuideStatus.value = aiListening.value ? 'Listening continuously' : 'Ready to listen';
    return;
  }
  aiGuideStatus.value = `${aiPersonaName.value} speaking`;
  utterance.onend = () => window.setTimeout(speakNextChunk, 120);
  utterance.onerror = () => {
    aiGuideStatus.value = 'Voice output paused';
  };
  window.speechSynthesis.speak(utterance);
}

function speechLanguage() {
  if (aiLanguage.value === 'hi-IN') return 'hi-IN';
  if (aiLanguage.value === 'en-US') return 'en-US';
  return 'en-IN';
}

function startVoiceInput() {
  if (aiListening.value) {
    stopVoiceInput();
    return;
  }
  const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!Recognition) {
    notify('Speech input is not supported in this browser.');
    return;
  }
  if (typeof window !== 'undefined' && window.speechSynthesis) {
    window.speechSynthesis.cancel();
  }
  aiGuideStatus.value = 'Listening continuously';
  aiInterimTranscript.value = '';
  beginRecognition(Recognition);
}

function beginRecognition(Recognition) {
  speechRecognition?.stop();
  speechRecognition = new Recognition();
  speechRecognition.lang = speechLanguage();
  speechRecognition.interimResults = true;
  speechRecognition.continuous = true;
  speechRecognition.maxAlternatives = 3;
  speechRecognition.onstart = () => {
    aiListening.value = true;
    aiGuideStatus.value = 'Listening continuously';
  };
  speechRecognition.onend = () => {
    if (aiListening.value) {
      speechRestartTimer = window.setTimeout(() => beginRecognition(Recognition), 350);
    } else {
      aiGuideStatus.value = 'Ready to listen';
    }
  };
  speechRecognition.onerror = (event) => {
    if (event.error === 'no-speech') {
      aiGuideStatus.value = 'Still listening, please continue';
      return;
    }
    aiListening.value = false;
    aiGuideStatus.value = 'Voice capture stopped';
    notify('Voice capture stopped. Check microphone permission.');
  };
  speechRecognition.onresult = (event) => {
    let finalTranscript = '';
    let interimTranscript = '';
    for (let index = event.resultIndex; index < event.results.length; index += 1) {
      const transcript = event.results[index]?.[0]?.transcript ?? '';
      if (event.results[index].isFinal) {
        finalTranscript += transcript;
      } else {
        interimTranscript += transcript;
      }
    }
    if (finalTranscript.trim()) {
      aiGuideInput.value = `${aiGuideInput.value} ${finalTranscript}`.trim();
      aiGuideStatus.value = 'Captured voice. Keep speaking or press Send.';
    }
    aiInterimTranscript.value = interimTranscript.trim();
  };
  speechRecognition.start();
}

function stopVoiceInput() {
  aiListening.value = false;
  aiGuideStatus.value = 'Ready to listen';
  aiInterimTranscript.value = '';
  if (speechRestartTimer) {
    window.clearTimeout(speechRestartTimer);
  }
  speechRecognition?.stop();
}

function approveAgentAction(action) {
  approvalHistory.value = [{
    ...action,
    decision: 'APPROVED',
    decidedAt: new Date().toLocaleString(),
    owner: 'Founder / Owner'
  }, ...approvalHistory.value].slice(0, 20);
  if (action.commandType === 'OPEN_TAB') {
    activeTab.value = action.payload.target;
    openActiveNavGroup();
  }
  if (action.commandType === 'SET_PROVISIONING_ROLE') {
    activeTab.value = 'servers';
    openActiveNavGroup();
    serverForm.targetRoleClass = action.payload.targetRoleClass;
    serverForm.placementMode = action.payload.placementMode;
    serverForm.deploymentFramework = 'VIRTUAL_MACHINE';
  }
  aiPendingActions.value = aiPendingActions.value.filter((item) => item.actionId !== action.actionId);
  aiGuideMessages.value = [...aiGuideMessages.value, { role: 'guide', text: `${aiPersonaName.value}: founder approval received. I applied the approved CEO-agent recommendation: ${action.label}` }];
}

function rejectAgentAction(action) {
  approvalHistory.value = [{
    ...action,
    decision: 'REJECTED',
    decidedAt: new Date().toLocaleString(),
    owner: 'Founder / Owner'
  }, ...approvalHistory.value].slice(0, 20);
  aiPendingActions.value = aiPendingActions.value.filter((item) => item.actionId !== action.actionId);
  aiGuideMessages.value = [...aiGuideMessages.value, { role: 'guide', text: `${aiPersonaName.value}: founder decision noted. I will not perform: ${action.label}` }];
}

function openActiveNavGroup() {
  navGroups.forEach((group) => {
    if (group.tabs.some(([id]) => id === activeTab.value)) {
      openNavGroups[group.label] = true;
    }
  });
}

function terminateInstance(id) {
  inventory.value = inventory.value.filter((item) => item.id !== id);
  notify('Instance matching parent reference index cluster dropped.');
}

async function commitFirewallRule() {
  const rule = await createFirewallRule({ ...firewallForm });
  firewallRules.value = [rule, ...firewallRules.value];
  notify(`Firewall structural rule sequence injected -> ${rule.policyAction}.`);
}

async function executeTrace() {
  packetHops.value = [{
    index: 1,
    title: `${packetForm.sourcePointNode} (Source Root Node Frame)`,
    detail: `Encapsulating ${packetForm.protocolDialectType} with tag ${packetForm.taggingLayerParam}.`
  }];
  packetByteWeight.value = '0 bytes parsed';
  packetHexDump.value = '[PROCESSING] SDN Controller dispatching path validation probe packet streams over autonomous network interfaces...';
  traceResult.value = await tracePacket({ ...packetForm });
  packetHops.value = [
    ...packetHops.value,
    {
      index: 2,
      title: 'Autonomous Gateway Bridge Transit Node (AS-65001)',
      detail: 'Evaluating BGP/OSPF vector routing maps. Hop cost metric resolved to 12.'
    },
    {
      index: 3,
      title: `Destination Target Reached -> [${packetForm.destinationIpNode}]`,
      detail: 'Frame validation success. Immutable encrypted payload remained untouched; metadata route chain was inspected.'
    }
  ];
  packetByteWeight.value = '64 bytes parsed cleanly';
  packetHexDump.value = [
    '0000  00 11 22 33 44 55 66 77  88 99 aa bb cc dd ee ff',
    '0010  08 00 45 00 00 3c 1a 2b  40 00 40 01 af 12 0a c2',
    '0020  18 66 0a c2 18 dd 08 00  4d 5a 00 01 00 02 61 62',
    '0030  63 64 65 66 67 68 69 6a  6b 6c 6d 6e 6f 70 71 72',
    '0040  73 74 75 76 77 61 62 63  64 65 66 67 68 69 00 00'
  ].join('\n');
  notify('Multi-hop diagnostic route verification trace resolved.');
}

async function stageFile(eventOrFile) {
  const files = eventOrFile?.target?.files ? Array.from(eventOrFile.target.files) : [eventOrFile].filter(Boolean);
  if (!files.length) return;
  await stageTransferAssets(files);
  if (eventOrFile?.target) eventOrFile.target.value = '';
}

async function stageTransferAssets(files) {
  const hasFolder = files.some((file) => file.webkitRelativePath?.includes('/'));
  const shouldCompress = hasFolder || files.length > 5;
  const totalSize = files.reduce((sum, file) => sum + file.size, 0);
  const baseMetadata = {
    sourceNode: transferSourceNode.value?.name,
    destinationNode: transferDestinationNode.value?.name,
    sourceIp: transferSourceNode.value?.ip,
    destinationIp: transferDestinationNode.value?.ip
  };

  if (shouldCompress) {
    const bundleName = hasFolder ? `folder-bundle-${Date.now()}.zip` : `multi-file-bundle-${Date.now()}.zip`;
    const response = await stageFileMetadata({
      assetName: bundleName,
      assetByteSizeBytes: totalSize,
      mediaMimeType: 'application/zip'
    });
    stagedFiles.value = [{
      ...response,
      ...baseMetadata,
      name: bundleName,
      size: totalSize,
      type: 'application/zip',
      compressed: true,
      memberCount: files.length,
      members: files.map((file) => file.webkitRelativePath || file.name).slice(0, 12)
    }, ...stagedFiles.value];
    notify(`${files.length} assets bundled as ${bundleName} for ${baseMetadata.sourceNode} -> ${baseMetadata.destinationNode}.`);
    return;
  }

  const responses = await Promise.all(files.map(async (file) => {
    const response = await stageFileMetadata({
      assetName: file.name,
      assetByteSizeBytes: file.size,
      mediaMimeType: file.type || 'application/octet-stream'
    });
    return {
      ...response,
      ...baseMetadata,
      name: file.name,
      size: file.size,
      type: file.type || 'application/octet-stream',
      compressed: false,
      memberCount: 1
    };
  }));
  stagedFiles.value = [...responses, ...stagedFiles.value];
  notify(`${responses.length} asset${responses.length === 1 ? '' : 's'} staged for ${baseMetadata.sourceNode} -> ${baseMetadata.destinationNode}.`);
}

async function handleDrop(event) {
  event.preventDefault();
  dragOver.value = false;
  await stageTransferAssets(Array.from(event.dataTransfer.files ?? []));
}

function purgeStagedFile(token) {
  stagedFiles.value = stagedFiles.value.filter((file) => file.stagedAssetToken !== token);
  notify('Payload attachment asset matrix purged cleanly.');
}

async function pipeFile(file) {
  const widget = {
    pipelineWidgetId: `pipe-${Date.now()}`,
    assetName: file.name,
    route: `${file.sourceNode ?? transferSourceNode.value?.name} -> ${file.destinationNode ?? transferDestinationNode.value?.name}`,
    initialState: 'STREAMING',
    progress: 0
  };
  pipelines.value = [widget, ...pipelines.value];
  const timer = window.setInterval(() => {
    widget.progress = Math.min(95, widget.progress + Math.floor(Math.random() * 18) + 8);
  }, 350);
  const response = await executeFilePipeline({
    targetAssetToken: file.stagedAssetToken,
    deploymentDestinationHost: file.destinationIp ?? transferDestinationNode.value?.ip ?? '10.194.24.102'
  });
  window.clearInterval(timer);
  Object.assign(widget, response, { assetName: file.name, progress: 100, initialState: response.initialState ?? 'COMPLETED' });
  notify(`Asynchronous stream injection finalized for asset: ${file.name}.`);
}

function openFolderPicker() {
  folderUploadInput.value?.click();
}

function launchTerminalSession() {
  terminalConnected.value = false;
  terminalOutput.value = [`[CONNECTING] Requesting transport socket lifecycle synchronization bound toward ${terminalForm.targetedHostEndpoint}:${terminalForm.protocolDialectMode === 'SSH' ? 22 : 23}...`];
  window.setTimeout(() => {
    terminalConnected.value = true;
    terminalOutput.value = [
      'Crypto exchange key verification matrix resolved safely. mTLS transport valid.',
      'Cisco Nexus IOS Enterprise Fabric Network Terminal Engine Shell Interface (v15.2.x-Core)',
      `Authenticated user principal session identity descriptor mapping key payload: ${terminalUser.value}`,
      "Type 'show runtime architecture' or 'help' to audit system controller settings."
    ];
  }, 800);
}

async function executeTerminalCommand() {
  if (!terminalConnected.value) {
    launchTerminalSession();
    return;
  }
  const command = terminalForm.rawCommandText.trim();
  if (!command) return;
  if (command.toLowerCase() === 'help') {
    terminalOutput.value = [`${terminalPrompt.value} ${command}`, 'Supported Sandbox Command Matrix:\n  - show interface brief\n  - show runtime architecture\n  - clear', ...terminalOutput.value];
    terminalForm.rawCommandText = '';
    return;
  }
  if (command.toLowerCase() === 'clear') {
    terminalOutput.value = [];
    terminalForm.rawCommandText = '';
    return;
  }
  const response = await runTerminalCommand({ ...terminalForm });
  terminalOutput.value = [`${terminalPrompt.value} ${command}`, response.stdoutBuffer, ...terminalOutput.value];
  terminalForm.rawCommandText = '';
}

async function executeQuantum() {
  quantumStatus.value = 'COMPILING';
  quantumOutput.value = '[PROCESSING] Dispatching quantum state register layout frames directly to optimization array...';
  const response = await executeQuantumCircuit({ ...quantumForm, sequenceGateDepth: Number(quantumForm.sequenceGateDepth) });
  quantumStatus.value = 'SUCCESS';
  quantumOutput.value = JSON.stringify(response, null, 2);
  notify('Quantum acceleration matrix calculation block accepted.');
}

async function submitSocket() {
  const response = await spawnSocket({ ...socketForm, localPort: Number(socketForm.localPort) });
  socketOutput.value = `Socket ${response.socketInterfaceId} -> ${response.bindingState}`;
  notify(`Socket framework provisioned over port ${socketForm.localPort}.`);
}

function triggerFakeHoneypotIntrusion(attackerIpSource, vectorProfile, targetedPort) {
  incidents.value = [{
    incidentUuid: `incident-${Date.now()}`,
    attackerIpSource,
    vectorProfile,
    targetedPort,
    observedAt: new Date().toISOString()
  }, ...incidents.value];
  notify(`Intrusion Alert Aggregator: Vector registered from ${attackerIpSource}`);
}

function launchVdiSession() {
  const node = selectedVdiNode.value;
  vdiConnected.value = true;
  activeVdiApp.value = vdiForm.sessionMode === 'GUI' ? 'workbench' : 'terminal';
  if (vdiForm.sessionMode === 'GUI') {
    vdiOutput.value = [
      `[GUI] Connected to ${node.name} (${node.ip})`,
      '[GUI] Desktop compositor ready: Fabric Workbench, Packet Console, File Share, Metrics Monitor',
      '[GUI] Use the session tiles below to inspect system state.'
    ];
  } else {
    vdiOutput.value = [
      `[CLI] Connected to ${node.name} (${node.ip})`,
      '[SYSTEM] Session Authenticated. Welcome to Ubuntu Core Appliance Base (x86_64 architecture node link)',
      "Type 'help' to review simulated operating system guest terminal actions."
    ];
  }
}

function openVdiApp(app) {
  if (!vdiConnected.value) {
    launchVdiSession();
  }
  activeVdiApp.value = app;
}

function stageVdiShare() {
  const node = selectedVdiNode.value;
  vdiSharedFiles.value = [{
    name: `diagnostic-${Date.now()}.tar.gz`,
    route: `local-workstation -> ${node.name}`,
    state: 'READY_TO_TRANSFER'
  }, ...vdiSharedFiles.value].slice(0, 6);
  notify(`VDI file share staged for ${node.name}.`);
}

function handleVdiCommand() {
  if (!vdiConnected.value) {
    launchVdiSession();
    return;
  }
  const command = vdiCommand.value.trim();
  if (!command) return;
  const lower = command.toLowerCase();
  if (lower === 'clear') {
    vdiOutput.value = [];
  } else if (lower === 'help') {
    vdiOutput.value = [...vdiOutput.value, `guest_vm@fabric-node:~$ ${command}`, 'Available VDI Emulation Actions:\n- sysinfo\n- netstat\n- clear'];
  } else if (lower === 'sysinfo') {
    vdiOutput.value = [...vdiOutput.value, `guest_vm@fabric-node:~$ ${command}`, 'Architecture: x86_64 Linux Subsystem\nKernel: 5.15.0-88-generic\nVirtualization Platform: QEMU Hypervisor Core Engine'];
  } else if (lower === 'netstat') {
    vdiOutput.value = [...vdiOutput.value, `guest_vm@fabric-node:~$ ${command}`, 'Proto  Local IP Address      Foreign IP Address    State\ntcp    10.194.24.102:22       10.194.24.1:49210     ESTABLISHED\ntcp    0.0.0.0:8080           0.0.0.0:*             LISTEN'];
  } else {
    vdiOutput.value = [...vdiOutput.value, `guest_vm@fabric-node:~$ ${command}`, `bash: command not found: ${command}. Input 'help' to review supported operational procedures.`];
  }
  vdiCommand.value = '';
}
</script>

<template>
  <main class="control-room">
    <aside class="nav-rail">
      <div>
        <div class="brand-block">
          <div class="brand-mark">
            <img src="https://imgs.search.brave.com/TYI2Iqlb7gAxB05WIYX8LvwJ7kKp_xER88boCAM-weo/rs:fit:500:0:1:0/g:ce/aHR0cHM6Ly9jZG4u/aWNvbnNjb3V0LmNv/bS9pY29uL3ByZW1p/dW0vcG5nLTI1Ni10/aHVtYi9jb3Ntb3Mt/c3RpY2tlci1pY29u/LXN2Zy1kb3dubG9h/ZC1wbmctODEzMzU2/NS5wbmc_Zj13ZWJw/Jnc9MTI4" alt="Reactor logo" />
          </div>
          <div>
            <h1>Reactor</h1>
            <p>v2.1.0 · MULTI-CLUSTER</p>
          </div>
        </div>

        <nav class="nav-list">
          <section v-for="group in navGroups" :key="group.label" class="nav-group">
            <button
              type="button"
              class="nav-group-toggle"
              :aria-expanded="openNavGroups[group.label]"
              @click="toggleNavGroup(group.label)"
            >
              <span>{{ group.label }}</span>
              <b>{{ openNavGroups[group.label] ? '−' : '+' }}</b>
            </button>
            <div v-show="openNavGroups[group.label]" class="nav-group-items">
              <button
                v-for="[id, label] in group.tabs"
                :key="id"
                :class="['nav-button', { active: activeTab === id }]"
                @click="selectTab(id, group.label)"
              >
                {{ label }}
              </button>
            </div>
          </section>
        </nav>
      </div>

      <div class="nav-footer">
        <button class="utility-button" @click="packetCanvasOpen = true">Packet Canvas</button>
        <button class="utility-button amber" @click="vdiOpen = true">VDI Workspace</button>
        <label>
          Theme
          <select v-model="themeMode">
            <option value="system">System Sync</option>
            <option value="dark">Dark</option>
            <option value="light">Bright</option>
          </select>
        </label>
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
          <article class="metric-card emerald"><span>Honeypot Decoys</span><strong>{{ honeypotCount }} Active</strong></article>
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
                <tr><th>Instance / VM Label</th><th>Role</th><th>Platform</th><th>Target IP</th><th>Uptime</th><th>State</th><th>Actions</th></tr>
              </thead>
              <tbody>
                <tr v-for="item in inventory" :key="item.id">
                  <td>{{ item.name }}</td><td>{{ item.role }}</td><td>{{ item.platform }}</td><td>{{ item.ip }}</td><td>{{ item.uptime }}</td><td><span class="status-chip blue">{{ item.state }}</span></td>
                  <td><button class="icon-action danger" title="Terminate instance" @click="terminateInstance(item.id)">Terminate</button></td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <FabricCanvas mode="static" />

      </section>

      <section v-if="activeTab === '360-view'" class="tab-page">
        <section class="panel">
          <div class="panel-title-row">
            <div>
              <h3>360 Physical System Diagnostics</h3>
              <p class="muted">Spring Boot Admin-style operating view for host health, actuator-like telemetry, JVM/runtime state, and AI suggestions.</p>
            </div>
            <span class="status-chip" :class="hardwareOverview?.health?.status === 'UP' ? 'green' : 'amber'">{{ hardwareOverview?.health?.status || 'UP' }}</span>
          </div>
          <p class="muted live-source">
            Live source: {{ hardwareOverview?.health?.metricsSource ?? 'PHYSICAL_HOST_PROCESS' }} · Refresh: every 5 seconds · Last sample: {{ hardwareLastRefresh || 'waiting' }} · {{ hardwareRefreshState }}
            <br />
            {{ hardwareOverview?.health?.physicalHostNote ?? 'Streaming live runtime metrics from Reactor backend.' }}
          </p>
          <div class="hardware-grid">
            <article v-for="card in diagnosticCards" :key="card.label">
              <span>{{ card.label }}</span>
              <strong>{{ card.value }}</strong>
              <small>{{ card.detail }}</small>
            </article>
          </div>
          <div class="metric-chart-grid">
            <article v-for="chart in diagnosticCharts" :key="chart.key" class="metric-chart">
              <div class="chart-head">
                <span>{{ chart.label }}</span>
                <strong>{{ latestChartValue(chart.key) }}</strong>
              </div>
              <svg viewBox="0 0 100 56" preserveAspectRatio="none" role="img">
                <line x1="0" y1="54" x2="100" y2="54" />
                <line x1="0" y1="28" x2="100" y2="28" />
                <polyline :points="chartPoints(chart.key)" :stroke="chart.color" />
              </svg>
              <small>{{ hardwareHistory.at(-1)?.time || 'waiting for sample' }}</small>
            </article>
          </div>
        </section>
        <section class="admin-grid">
          <article class="panel"><h3>Health</h3><pre class="console-frame compact">status: {{ hardwareOverview?.health?.status ?? 'UP' }}
db: {{ hardwareOverview?.health?.db ?? 'UP' }}
r2dbc: {{ hardwareOverview?.health?.r2dbc ?? 'UP' }}
ollama: {{ hardwareOverview?.health?.ollama ?? 'LOCAL_OR_FALLBACK_READY' }}
sse: {{ hardwareOverview?.health?.sse ?? 'UP' }}
checked: {{ hardwareOverview?.health?.checkedAt ?? 'warming' }}</pre></article>
          <article class="panel"><h3>Process & Threads</h3><pre class="console-frame compact">host: {{ hardwareOverview?.physicalHost ?? 'local-physical-host' }}
pid: {{ hardwareOverview?.process?.pid ?? 'local' }}
uptime: {{ formatDuration(hardwareOverview?.process?.uptimeSeconds ?? 0) }}
live threads: {{ hardwareOverview?.threads?.live ?? 0 }}
daemon threads: {{ hardwareOverview?.threads?.daemon ?? 0 }}
total started: {{ hardwareOverview?.threads?.totalStarted ?? 0 }}</pre></article>
          <article class="panel"><h3>JVM / VM</h3><pre class="console-frame compact">vm: {{ hardwareOverview?.jvm?.name ?? 'JVM' }}
vendor: {{ hardwareOverview?.jvm?.vendor ?? 'Eclipse Adoptium' }}
heap: {{ hardwareOverview?.jvm?.heapUsedMb ?? 0 }} / {{ hardwareOverview?.jvm?.heapMaxMb ?? 0 }} MB
non-heap: {{ hardwareOverview?.jvm?.nonHeapUsedMb ?? 0 }} MB
classes: {{ hardwareOverview?.jvm?.loadedClasses ?? 0 }}
vm-pool: {{ hardwareOverview?.vmPool?.capacityState ?? 'READY' }}</pre></article>
          <article class="panel"><h3>Actuator</h3><pre class="console-frame compact">/actuator/health
/actuator/metrics/jvm.memory.used
/actuator/metrics/process.cpu.usage
/actuator/metrics/jvm.threads.live
/api/v1/hardware/overview</pre></article>
          <article class="panel"><h3>AI Suggestions & Alerts</h3><div class="suggestion-list"><p v-for="item in [...(hardwareOverview?.aiSuggestions ?? []), ...(hardwareOverview?.alerts ?? [])]" :key="item">{{ item }}</p></div></article>
        </section>
      </section>

      <section v-if="activeTab === 'approvals'" class="tab-page">
        <section class="panel">
          <div class="panel-title-row">
            <div>
              <h3>Founder / Owner Approval Queue</h3>
              <p class="muted">The AI guide acts as Reactor CEO-agent, but infrastructure decisions stay gated by your approval.</p>
            </div>
            <span class="status-chip amber">{{ aiPendingActions.length }} pending</span>
          </div>
          <div class="approval-grid">
            <article v-for="action in aiPendingActions" :key="action.actionId" class="approval-card">
              <span class="status-chip">CEO-agent proposal</span>
              <h4>{{ action.label }}</h4>
              <p>{{ action.description }}</p>
              <dl>
                <dt>Decision Type</dt><dd>{{ action.commandType }}</dd>
                <dt>Authority</dt><dd>Founder / Owner approval required</dd>
              </dl>
              <pre>{{ JSON.stringify(action.payload, null, 2) }}</pre>
              <div class="button-row">
                <button class="primary-button" @click="approveAgentAction(action)">Approve</button>
                <button class="danger-button" @click="rejectAgentAction(action)">Reject</button>
              </div>
            </article>
            <p v-if="!aiPendingActions.length" class="empty-state">No CEO-agent decisions are waiting for approval.</p>
          </div>
        </section>
        <section class="panel">
          <h3>Decision Audit Trail</h3>
          <article v-for="item in approvalHistory" :key="`${item.actionId}-${item.decidedAt}`" class="event-row">
            <strong>{{ item.label }}</strong>
            <span>{{ item.decision }} by {{ item.owner }} · {{ item.decidedAt }}</span>
            <small>{{ item.description }}</small>
          </article>
          <p v-if="!approvalHistory.length" class="empty-state">Approval history will appear after you approve or reject a CEO-agent proposal.</p>
        </section>
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
              <label>Deployment Framework Target<select v-model="serverForm.deploymentFramework" :disabled="serverForm.targetRoleClass === 'QUANTUM_VM'"><option>BARE_METAL_PHYSICAL</option><option>VIRTUAL_MACHINE</option></select></label>
              <label>Execution Framework Scope<select v-model="serverForm.executionScope"><option>REAL</option><option>SIMULATED</option></select></label>
            </div>
            <div class="form-pair">
              <label>Provisioning Placement<select v-model="serverForm.placementMode"><option value="PHYSICAL_HOST_VM">Physical System VM Pool</option><option value="REMOTE_NODE">Remote Node Server Promotion</option></select></label>
              <label>Registered MAC Address<input v-model="serverForm.registeredMacAddress" required /></label>
            </div>
            <div class="form-pair">
              <label>Internet IP Address<input v-model="serverForm.internetIpAddress" required /></label>
              <label>Fabric Network IP Address<input v-model="serverForm.fabricIpAddress" required /></label>
            </div>
            <div class="segmented">
              <button type="button" :class="{ active: serverForm.targetRoleClass === 'DATABASE_SERVER' }" @click="serverForm.targetRoleClass = 'DATABASE_SERVER'">Database Engine</button>
              <button type="button" :class="{ active: serverForm.targetRoleClass === 'APPLICATION_SERVER' }" @click="serverForm.targetRoleClass = 'APPLICATION_SERVER'">Application Host</button>
              <button type="button" :class="{ active: serverForm.targetRoleClass === 'HONEYPOT_DECOY' }" @click="serverForm.targetRoleClass = 'HONEYPOT_DECOY'">Honeypot Decoy Target</button>
              <button type="button" :class="{ active: serverForm.targetRoleClass === 'AI_VM' }" @click="serverForm.targetRoleClass = 'AI_VM'; serverForm.deploymentFramework = 'VIRTUAL_MACHINE'">AI VM</button>
              <button type="button" :class="{ active: serverForm.targetRoleClass === 'QUANTUM_VM' }" @click="serverForm.targetRoleClass = 'QUANTUM_VM'; serverForm.deploymentFramework = 'VIRTUAL_MACHINE'">Quantum VM</button>
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
            <div v-if="serverForm.targetRoleClass === 'AI_VM'" class="subschema">
              <label>Model Runtime<select v-model="serverForm.modelRuntime"><option>OLLAMA_LLAMA_3_2</option><option>OLLAMA_MISTRAL</option><option>LOCAL_ONNX_RUNTIME</option></select></label>
              <label>Accelerator Profile<select v-model="serverForm.acceleratorProfile"><option>CPU_LOCAL</option><option>GPU_PASSTHROUGH</option><option>NPU_EDGE</option></select></label>
              <label>Inference Port<input v-model="serverForm.inferencePort" type="number" /></label>
            </div>
            <div v-if="serverForm.targetRoleClass === 'QUANTUM_VM'" class="subschema">
              <label>QPU Simulator<select v-model="serverForm.qpuSimulator"><option>QISKIT_AER_VM</option><option>BRAKET_LOCAL_VM</option><option>CIRQ_SIMULATOR_VM</option></select></label>
              <label>Qubit Count<input v-model="serverForm.qubitCount" type="number" /></label>
              <label>Circuit Runtime<input v-model="serverForm.circuitRuntime" /></label>
            </div>
            <div v-if="serverForm.placementMode === 'REMOTE_NODE'" class="subschema">
              <label>Node Bootstrap Endpoint<input v-model="serverForm.bootstrapEndpoint" /></label>
              <label>Eligibility Check Script<input v-model="serverForm.checkScript" /></label>
              <label>Promotion Install Script<input v-model="serverForm.installScript" /></label>
            </div>
            <p v-if="deviceRegistration" class="empty-state">Last registration: {{ deviceRegistration.networkState }} · {{ deviceRegistration.assignedPolicies.join(' | ') }}</p>
            <button class="primary-button">Spark Lifecycle Provisioning Instance</button>
          </form>
        </section>
      </section>

      <section v-if="activeTab === 'local-vms'" class="tab-page">
        <section class="panel">
          <div class="panel-title-row">
            <div>
              <h3>VMware-Style Local VM Creator</h3>
              <p class="muted">Creates VM plans for your physical system using VMware vmrun or QEMU. Real execution is guarded by REACTOR_VM_EXECUTION_ENABLED.</p>
            </div>
            <span class="status-chip">Guarded Executor</span>
          </div>
          <div class="provider-grid">
            <article v-for="provider in localVmProviders" :key="provider.provider" class="event-row">
              <strong>{{ provider.provider }}</strong>
              <span>{{ provider.available ? 'Available' : 'Not detected' }} · {{ provider.detectedBinary || 'No binary on backend PATH' }}</span>
              <small>{{ provider.notes }}</small>
            </article>
          </div>
        </section>

        <section class="two-col">
          <form class="panel form-grid" @submit.prevent="submitLocalVm">
            <h3>New Virtual Machine</h3>
            <div class="form-pair">
              <label>VM Name<input v-model="localVmForm.vmName" required /></label>
              <label>Provider<select v-model="localVmForm.provider"><option>VMWARE</option><option>QEMU</option></select></label>
            </div>
            <div class="form-pair">
              <label>Guest OS Type<input v-model="localVmForm.guestOsType" placeholder="ubuntu-64" /></label>
              <label>Network Mode<select v-model="localVmForm.networkMode"><option>NAT</option><option>BRIDGED</option><option>HOST_ONLY</option></select></label>
            </div>
            <div class="form-pair">
              <label>CPU Cores<input v-model="localVmForm.cpuCores" type="number" min="1" /></label>
              <label>Memory MB<input v-model="localVmForm.memoryMb" type="number" min="512" step="512" /></label>
            </div>
            <div class="form-pair">
              <label>Disk GB<input v-model="localVmForm.diskGb" type="number" min="8" /></label>
              <label>Installer ISO Path<input v-model="localVmForm.isoPath" placeholder="/Users/ajay/Downloads/ubuntu.iso" /></label>
            </div>
            <label class="checkbox-line"><input v-model="localVmForm.startAfterCreate" type="checkbox" /> Start VM after create when execution is enabled</label>
            <button class="primary-button">Create / Generate VM Plan</button>
          </form>

          <section class="panel">
            <h3>Execution Plan</h3>
            <pre class="console-frame">{{ localVmResult ? JSON.stringify(localVmResult, null, 2) : 'Submit the form to generate VMware/QEMU commands. By default Reactor returns a dry-run plan so your physical machine is not changed without explicit enablement.' }}</pre>
          </section>
        </section>
      </section>

      <section v-if="activeTab === 'sms-gateway'" class="tab-page">
        <SmsDispatchPanel />
      </section>

      <section v-if="activeTab === 'terminal-access'" class="tab-page two-col">
        <section class="panel">
          <h3>Initiate Remote VTY Core Session</h3>
          <form class="form-grid" @submit.prevent="executeTerminalCommand">
            <label>Target IP<select v-model="terminalForm.targetedHostEndpoint"><option>10.194.24.102</option><option>10.194.24.220</option><option>192.168.1.1</option></select></label>
            <label>Protocol<select v-model="terminalForm.protocolDialectMode"><option>SSH</option><option>TELNET</option></select></label>
            <label>Access Username<input v-model="terminalUser" /></label>
            <label>Command<input v-model="terminalForm.rawCommandText" /></label>
            <div class="button-row">
              <button type="button" class="secondary-button" @click="launchTerminalSession">Open Remote Terminal Frame</button>
              <button class="primary-button">Execute Command</button>
            </div>
          </form>
        </section>
        <section class="panel terminal-panel">
          <div class="panel-title-row compact">
            <h3>Interactive Shell Access Buffer Terminal</h3>
            <span :class="['status-chip', terminalConnected ? 'green' : '']">{{ terminalConnected ? `${terminalForm.protocolDialectMode} connected` : 'Disconnected' }}</span>
          </div>
          <pre class="console-frame">{{ terminalOutput.join('\n\n') }}</pre>
        </section>
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
          <div class="attack-grid">
            <button class="secondary-button" @click="triggerFakeHoneypotIntrusion('198.51.100.42', 'VULNERABLE_SSH', 22)">Inject SSH Brute Force</button>
            <button class="secondary-button amber" @click="triggerFakeHoneypotIntrusion('203.0.113.89', 'EXPOSED_TELNET', 23)">Inject Telnet Command</button>
          </div>
          <article v-for="incident in incidents" :key="incident.incidentUuid" class="event-row danger">
            <strong>{{ incident.attackerIpSource }}</strong>
            <span>{{ incident.vectorProfile }} · :{{ incident.targetedPort }}</span>
          </article>
          <p v-if="!incidents.length" class="empty-state">No active high-severity intrusion indicators triggered in this epoch frame window.</p>
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
        <FabricCanvas mode="static" />
        <section class="trace-grid">
          <div class="panel">
            <h3>Active Route Propagation Steps</h3>
            <div class="hop-chain">
              <article v-for="hop in packetHops" :key="hop.index" class="hop-row">
                <b>{{ hop.index }}</b>
                <div><strong>{{ hop.title }}</strong><span>{{ hop.detail }}</span></div>
              </article>
              <p v-if="!packetHops.length" class="empty-state">Trigger trace configuration to display hop vector progressions.</p>
            </div>
          </div>
          <div class="panel">
            <div class="panel-title-row compact">
              <h3>Hex Protocol Encapsulation Decoder</h3>
              <span class="status-chip">{{ packetByteWeight }}</span>
            </div>
            <pre class="console-frame compact">{{ packetHexDump }}</pre>
          </div>
        </section>
        <pre v-if="traceResult" class="console-frame compact">{{ JSON.stringify(traceResult, null, 2) }}</pre>
      </section>

      <section v-if="activeTab === 'file-transport'" class="tab-page two-col">
        <section class="panel">
          <h3>Node-to-Node File Share</h3>
          <div class="form-pair">
            <label>Source System Node
              <select v-model="transferForm.sourceNodeId">
                <option v-for="node in inventory" :key="node.id" :value="node.id">{{ node.name }} · {{ node.ip }}</option>
              </select>
            </label>
            <label>Destination System Node
              <select v-model="transferForm.destinationNodeId">
                <option v-for="node in inventory" :key="node.id" :value="node.id">{{ node.name }} · {{ node.ip }}</option>
              </select>
            </label>
          </div>
          <label
            :class="['dropzone', { over: dragOver }]"
            @dragover.prevent="dragOver = true"
            @dragleave.prevent="dragOver = false"
            @drop="handleDrop"
          >
            <strong>Stage Diagnostic Resource Payload</strong>
            <span>Drop files/folders here. More than 5 files or any folder is bundled as a ZIP transfer.</span>
            <input class="file-input hidden-input" type="file" multiple @change="stageFile" />
          </label>
          <div class="button-row">
            <label class="secondary-button file-button-label">Upload Files<input class="hidden-input" type="file" multiple @change="stageFile" /></label>
            <button class="secondary-button amber" type="button" @click="openFolderPicker">Upload Folder</button>
            <input ref="folderUploadInput" class="hidden-input" type="file" webkitdirectory directory multiple @change="stageFile" />
          </div>
          <article v-for="file in stagedFiles" :key="file.stagedAssetToken" class="event-row">
            <strong>{{ file.name }}</strong>
            <span>{{ file.sourceNode }} -> {{ file.destinationNode }} · {{ file.type }} · {{ file.memberCount }} item{{ file.memberCount === 1 ? '' : 's' }}</span>
            <small v-if="file.compressed">Compressed bundle: {{ file.members.join(', ') }}</small>
            <div class="button-row">
              <button class="secondary-button" @click="pipeFile(file)">Pipe into SDN</button>
              <button class="danger-button" @click="purgeStagedFile(file.stagedAssetToken)">Purge</button>
            </div>
          </article>
          <p v-if="!stagedFiles.length" class="empty-state">Cluster attachment storage array empty.</p>
        </section>
        <section class="panel">
          <h3>Streaming Allocations</h3>
          <article v-for="pipe in pipelines" :key="pipe.pipelineWidgetId" class="event-row">
            <strong>{{ pipe.assetName }}</strong><span>{{ pipe.route }} · {{ pipe.pipelineWidgetId }} · {{ pipe.initialState }}</span>
            <div class="progress-track"><i :style="{ width: `${pipe.progress}%` }"></i></div>
          </article>
          <p v-if="!pipelines.length" class="empty-state">No streaming allocations provisioned.</p>
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
        <section class="panel">
          <div class="panel-title-row compact">
            <h3>Telemetry Feedback Array</h3>
            <span class="status-chip">{{ quantumStatus }}</span>
          </div>
          <pre class="console-frame">{{ quantumOutput }}</pre>
        </section>
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

      <section v-if="activeTab === 'about'" class="tab-page">
        <section class="about-hero panel">
          <div>
            <span class="status-chip green">Reactor</span>
            <h3>One control plane for real infrastructure, packet truth, and guided operations.</h3>
            <p>
              Reactor exists to make complex infrastructure feel operable from one place: provision VMs,
              register devices by MAC identity, model network paths, trace immutable encrypted packet metadata,
              and react to anomalies with local AI guidance.
            </p>
          </div>
        </section>
        <section class="about-grid">
          <article class="panel">
            <h3>Why Reactor</h3>
            <p class="muted">Infrastructure work usually scatters across hypervisors, shells, scripts, dashboards, packet tools, and alert consoles. Reactor pulls those loops into a single reactive workspace so teams can see what is connected, what changed, and what action is safe next.</p>
          </article>
          <article class="panel">
            <h3>How It Helps</h3>
            <p class="muted">It gives operators a Packet Tracer-style canvas, server and AI VM provisioning, quantum VM simulation, file movement between nodes, SMS alert dispatch, VDI access, hardware 360 monitoring, and a conversational guide that explains decisions in plain language.</p>
          </article>
          <article class="panel">
            <h3>Operating Model</h3>
            <p class="muted">Devices can keep an internet IP while joining the Reactor fabric with a second fabric IP. MAC registration anchors identity, while eligibility scripts decide whether a remote node can become a server host.</p>
          </article>
          <article class="panel">
            <h3>AI Philosophy</h3>
            <p class="muted">Reactor favors local Ollama-backed AI and deterministic fallbacks, so suggestions stay useful even when model calls are slow. The guide is here to reduce friction, not hide the underlying infrastructure.</p>
          </article>
        </section>
      </section>
    </section>

    <div v-if="packetCanvasOpen" class="modal-backdrop">
      <section class="packet-modal">
        <header class="modal-header">
          <div>
            <h3>CCNP Real-Time Payload Topo-Map Designer</h3>
            <p>Interactive workspace canvas node generator environment</p>
          </div>
          <button class="secondary-button" @click="packetCanvasOpen = false">Close</button>
        </header>
        <div class="modal-body full">
          <FabricCanvas mode="designer" />
        </div>
        <footer>Canvas Engine Render Model: VueFlow node and edge matrices <b>Status: Packet Tracer Sandbox Ready</b></footer>
      </section>
    </div>

    <section v-if="vdiOpen" class="vdi-window">
      <header>
        <span class="window-dot red"></span><span class="window-dot yellow"></span><span class="window-dot green"></span>
        <strong>Guest VM Host VDI Session</strong>
        <button @click="vdiOpen = false">Power</button>
      </header>
      <div class="vdi-picker">
        <label>Connect Device
          <select v-model="vdiForm.targetNodeId">
            <option v-for="node in inventory" :key="node.id" :value="node.id">{{ node.name }} · {{ node.ip }}</option>
          </select>
        </label>
        <label>Session Type
          <select v-model="vdiForm.sessionMode">
            <option value="GUI">VDI GUI</option>
            <option value="CLI">CLI Terminal</option>
          </select>
        </label>
        <button class="primary-button" @click="launchVdiSession">Connect</button>
      </div>
      <div v-if="vdiForm.sessionMode === 'GUI' && vdiConnected" class="vdi-desktop">
        <aside class="vdi-dock">
          <button :class="{ active: activeVdiApp === 'workbench' }" @click="openVdiApp('workbench')">Workbench</button>
          <button :class="{ active: activeVdiApp === 'terminal' }" @click="openVdiApp('terminal')">Terminal</button>
          <button :class="{ active: activeVdiApp === 'files' }" @click="openVdiApp('files')">Files</button>
          <button :class="{ active: activeVdiApp === 'packets' }" @click="openVdiApp('packets')">Packets</button>
          <button :class="{ active: activeVdiApp === 'metrics' }" @click="openVdiApp('metrics')">Metrics</button>
        </aside>
        <section class="vdi-app-window">
          <header>
            <strong>{{ selectedVdiNode.name }}</strong>
            <span>{{ selectedVdiNode.ip }} · {{ selectedVdiNode.role }} · {{ selectedVdiNode.state }}</span>
          </header>
          <div v-if="activeVdiApp === 'workbench'" class="vdi-app-grid">
            <article><strong>Node Identity</strong><span>{{ selectedVdiNode.name }}</span><small>{{ selectedVdiNode.platform }}</small></article>
            <article><strong>Access Mode</strong><span>GUI session active</span><small>CLI available from dock</small></article>
            <article><strong>Network</strong><span>{{ selectedVdiNode.ip }}</span><small>Fabric bridge linked</small></article>
            <article><strong>Operations</strong><span>Ready</span><small>Files, packets, metrics enabled</small></article>
          </div>
          <div v-if="activeVdiApp === 'terminal'" class="vdi-terminal-pane">
            <pre>{{ vdiOutput.join('\n\n') }}</pre>
            <label class="vdi-input embedded"><span>guest_vm@{{ selectedVdiNode.name }}:~$</span><input v-model="vdiCommand" @keydown.enter="handleVdiCommand" placeholder="help, sysinfo, netstat, clear" /></label>
          </div>
          <div v-if="activeVdiApp === 'files'" class="vdi-app-list">
            <button class="primary-button" @click="stageVdiShare">Stage File Transfer</button>
            <article v-for="file in vdiSharedFiles" :key="file.name">
              <strong>{{ file.name }}</strong>
              <span>{{ file.route }} · {{ file.state }}</span>
            </article>
            <p v-if="!vdiSharedFiles.length" class="empty-state">No GUI file transfers staged yet.</p>
          </div>
          <div v-if="activeVdiApp === 'packets'" class="vdi-app-list">
            <article><strong>Route Probe</strong><span>{{ selectedVdiNode.ip }} -> fabric gateway · reachable</span></article>
            <article><strong>Immutable Payload Policy</strong><span>Encrypted payload locked; metadata visible only</span></article>
            <article><strong>Fallback Tunnel</strong><span>Available if risk score is elevated</span></article>
          </div>
          <div v-if="activeVdiApp === 'metrics'" class="vdi-app-grid">
            <article><strong>CPU</strong><span>{{ hardwareOverview?.cpu?.loadPercent ?? 0 }}%</span><small>{{ hardwareOverview?.cpu?.thermalState ?? 'NOMINAL' }}</small></article>
            <article><strong>Memory</strong><span>{{ hardwareOverview?.memory?.usedPercent ?? 0 }}%</span><small>{{ hardwareOverview?.memory?.pressure ?? 'LOW' }}</small></article>
            <article><strong>Threads</strong><span>{{ hardwareOverview?.threads?.live ?? 0 }}</span><small>live JVM threads</small></article>
            <article><strong>VM Pool</strong><span>{{ hardwareOverview?.vmPool?.capacityState ?? 'READY' }}</span><small>{{ hardwareOverview?.storage?.freeGb ?? 0 }} GB free</small></article>
          </div>
        </section>
      </div>
      <pre v-else>{{ vdiOutput.join('\n\n') }}</pre>
      <label v-if="vdiForm.sessionMode === 'CLI'" class="vdi-input"><span>guest_vm@fabric-node:~$</span><input v-model="vdiCommand" @keydown.enter="handleVdiCommand" placeholder="Input guest OS subcommands..." /></label>
    </section>

    <section :class="['ai-guide', { collapsed: !aiGuideOpen }]">
      <button class="guide-avatar" @click="aiGuideOpen = !aiGuideOpen">AI</button>
      <div v-if="aiGuideOpen" class="guide-panel">
        <header>
          <strong>{{ aiPersonaName }}</strong>
          <span>CEO-agent · founder approval required</span>
        </header>
        <div class="guide-settings">
          <label>Agent
            <select v-model="aiVoiceGender">
              <option value="female">Aarohi · Female</option>
              <option value="male">Gabbar · Male</option>
            </select>
          </label>
          <label>Language
            <select v-model="aiLanguage">
              <option value="auto">Auto / Any</option>
              <option value="en-IN">English India</option>
              <option value="hi-IN">Hindi India</option>
              <option value="en-US">English US</option>
            </select>
          </label>
          <label>Talk
            <select v-model="aiVoiceEnabled">
              <option :value="true">On</option>
              <option :value="false">Off</option>
            </select>
          </label>
          <label>Voice Style
            <select v-model="aiVoiceMode">
              <option value="natural">Natural Guide</option>
              <option value="fast">Fast Brief</option>
            </select>
          </label>
          <button type="button" :class="{ listening: aiListening }" @click="startVoiceInput">{{ aiListening ? 'Stop Listening' : 'Speak' }}</button>
        </div>
        <div class="voice-status">
          <span>{{ aiGuideStatus }}</span>
          <small v-if="aiInterimTranscript">{{ aiInterimTranscript }}</small>
        </div>
        <div class="guide-messages">
          <article v-for="(message, index) in aiGuideMessages" :key="index" :class="message.role">
            <p>{{ message.text }}</p>
            <small v-if="message.actions">{{ message.actions.join(' · ') }}</small>
          </article>
        </div>
        <div v-if="aiPendingActions.length" class="agent-actions">
          <strong>Founder approval required</strong>
          <article v-for="action in aiPendingActions" :key="action.actionId">
            <div>
              <b>{{ action.label }}</b>
              <span>{{ action.description }}</span>
            </div>
            <button @click="approveAgentAction(action)">Approve</button>
            <button class="danger" @click="rejectAgentAction(action)">Reject</button>
          </article>
        </div>
        <form @submit.prevent="sendGuideMessage">
          <input v-model="aiGuideInput" placeholder="Ask about VMs, nodes, wiring, alerts..." />
          <button>Send</button>
        </form>
      </div>
    </section>

    <div v-if="toast" class="toast">{{ toast }}</div>
  </main>
</template>
