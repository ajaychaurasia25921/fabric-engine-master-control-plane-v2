<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { Background } from '@vue-flow/background';
import { Controls } from '@vue-flow/controls';
import { VueFlow, useVueFlow } from '@vue-flow/core';
import { useFabricStore } from '../stores/fabricStore';

const props = defineProps({
  mode: {
    type: String,
    default: 'static'
  }
});

const store = useFabricStore();
const { vueFlowNodes, vueFlowEdges } = storeToRefs(store);
const { setNodes, setEdges, fitView, addEdges } = useVueFlow();
const designerNodes = ref([]);
const designerEdges = ref([]);
const selectedElement = ref(null);
const activeWire = ref('ETHERNET');
const isDesigner = computed(() => props.mode === 'designer');
const canvasNodes = computed(() => (isDesigner.value ? designerNodes.value : vueFlowNodes.value));
const canvasEdges = computed(() => (isDesigner.value ? designerEdges.value : vueFlowEdges.value));

const devicePalette = [
  { type: 'ROUTER', label: 'Core Router', role: 'CCNP_EDGE_CORE', status: 'RUNNING' },
  { type: 'SWITCH', label: 'Layer 3 Switch', role: 'L3_SWITCH', status: 'RUNNING' },
  { type: 'FIREWALL', label: 'Security Appliance', role: 'FIREWALL_APPLIANCE', status: 'RUNNING' },
  { type: 'SERVER', label: 'Linux Server', role: 'APPLICATION_SERVER', status: 'PROVISIONED' },
  { type: 'DATABASE', label: 'Database Server', role: 'DATABASE_SERVER', status: 'PROVISIONED' },
  { type: 'HONEYPOT', label: 'Honeypot Decoy', role: 'HONEYPOT_DECOY', status: 'RUNNING' },
  { type: 'QPU', label: 'Quantum Core', role: 'QUANTUM_ACCELERATOR', status: 'INITIALIZING' },
  { type: 'AI_NODE', label: 'AI System', role: 'AI_INFERENCE_NODE', status: 'RUNNING' }
];

const wirePalette = [
  { type: 'ETHERNET', label: 'Copper Ethernet', stroke: '#22c55e' },
  { type: 'FIBER', label: 'Fiber Link', stroke: '#38bdf8' },
  { type: 'SERIAL', label: 'Serial WAN', stroke: '#f59e0b' },
  { type: 'TUNNEL', label: 'Fallback Tunnel', stroke: '#a855f7' }
];

watch(canvasNodes, (nodes) => setNodes(nodes), { immediate: true, deep: true });
watch(canvasEdges, (edges) => setEdges(edges), { immediate: true, deep: true });
watch(vueFlowNodes, (nodes) => {
  if (!designerNodes.value.length) designerNodes.value = nodes.map((node) => ({ ...node, draggable: true }));
}, { immediate: true });
watch(vueFlowEdges, (edges) => {
  if (!designerEdges.value.length) designerEdges.value = edges.map((edge) => ({ ...edge, animated: false }));
}, { immediate: true });

onMounted(async () => {
  await store.loadTopology();
  store.connectStream();
  requestAnimationFrame(() => fitView({ padding: 0.2 }));
});

onBeforeUnmount(() => store.disconnectStream());

function deviceStyle(device, status) {
  const colors = {
    ROUTER: '#a855f7',
    SWITCH: '#38bdf8',
    FIREWALL: '#ef4444',
    SERVER: '#22c55e',
    DATABASE: '#3b82f6',
    HONEYPOT: '#f59e0b',
    QPU: '#eab308',
    AI_NODE: '#14b8a6'
  };
  return {
    border: `2px solid ${colors[device] ?? '#64748b'}`,
    background: status === 'DOWN' ? '#2a0f12' : '#111827',
    color: '#f8fafc',
    borderRadius: '8px',
    minWidth: '164px',
    fontWeight: 800,
    boxShadow: '0 10px 22px rgba(0, 0, 0, 0.28)'
  };
}

function addDevice(device) {
  const count = designerNodes.value.filter((node) => node.data?.deviceType === device.type).length + 1;
  const id = `${device.type.toLowerCase()}-${Date.now()}`;
  designerNodes.value = [
    ...designerNodes.value,
    {
      id,
      type: 'default',
      label: `${device.label}-${count}`,
      position: { x: 100 + (count * 42) % 520, y: 96 + (designerNodes.value.length * 58) % 360 },
      data: {
        id,
        label: `${device.label}-${count}`,
        deviceType: device.type,
        role: device.role,
        status: device.status,
        managementIp: `10.194.${Math.floor(Math.random() * 200) + 20}.${Math.floor(Math.random() * 200) + 20}`,
        immutablePacketPolicy: 'metadata-only inspection',
        fallbackTunnel: device.type === 'FIREWALL' ? 'enabled' : 'available'
      },
      style: deviceStyle(device.type, device.status)
    }
  ];
}

function clearDesigner() {
  designerNodes.value = [];
  designerEdges.value = [];
  selectedElement.value = null;
}

function onConnect(connection) {
  const wire = wirePalette.find((item) => item.type === activeWire.value) ?? wirePalette[0];
  const edge = {
    ...connection,
    id: `${connection.source}-${connection.target}-${Date.now()}`,
    label: wire.label,
    animated: wire.type === 'TUNNEL',
    data: {
      wireType: wire.type,
      encrypted: true,
      mutable: false,
      inspectedMetadata: ['sourceNode', 'destinationNode', 'protocol', 'riskScore']
    },
    style: { stroke: wire.stroke, strokeWidth: 3 },
    labelStyle: { fontWeight: 800, fill: '#e5e7eb' }
  };
  designerEdges.value = [...designerEdges.value, edge];
  addEdges([edge]);
  selectedElement.value = {
    type: 'wire',
    title: edge.label,
    metadata: edge.data
  };
}

function onNodeClick({ node }) {
  selectedElement.value = {
    type: 'device',
    title: node.label,
    metadata: node.data
  };
}

function onEdgeClick({ edge }) {
  selectedElement.value = {
    type: 'wire',
    title: edge.label,
    metadata: edge.data ?? { source: edge.source, target: edge.target }
  };
}
</script>

<template>
  <section :class="['canvas-shell', { designer: isDesigner }]">
    <div class="canvas-toolbar">
      <div>
        <h1>{{ isDesigner ? 'Packet Canvas Designer' : 'Fabric Engine Master Control Plane' }}</h1>
        <p>{{ store.streamError || (isDesigner ? 'Device palette, wire palette, and immutable metadata inspection' : 'Static fabric topology with click-to-inspect metadata') }}</p>
      </div>
      <span :class="['stream-pill', { live: store.connected }]">
        {{ store.connected ? 'SSE Live' : 'SSE Offline' }}
      </span>
    </div>

    <div class="packet-workbench">
      <aside v-if="isDesigner" class="packet-palette">
        <section>
          <h2>Device Palette</h2>
          <button v-for="device in devicePalette" :key="device.type" @click="addDevice(device)">
            <strong>{{ device.label }}</strong>
            <span>{{ device.role }}</span>
          </button>
        </section>
        <section>
          <h2>Wire Palette</h2>
          <button
            v-for="wire in wirePalette"
            :key="wire.type"
            :class="{ active: activeWire === wire.type }"
            @click="activeWire = wire.type"
          >
            <strong>{{ wire.label }}</strong>
            <span>{{ wire.type }}</span>
          </button>
        </section>
        <button class="danger-button" @click="clearDesigner">Clear Canvas</button>
      </aside>

      <VueFlow
        class="fabric-canvas"
        :nodes="canvasNodes"
        :edges="canvasEdges"
        :fit-view-on-init="true"
        :nodes-draggable="isDesigner"
        :nodes-connectable="isDesigner"
        :elements-selectable="true"
        @connect="onConnect"
        @node-click="onNodeClick"
        @edge-click="onEdgeClick"
      >
        <Background gap="28" />
        <Controls />
      </VueFlow>

      <aside class="properties-panel">
        <h2>Properties</h2>
        <template v-if="selectedElement">
          <strong>{{ selectedElement.title }}</strong>
          <span class="status-chip">{{ selectedElement.type }}</span>
          <dl>
            <template v-for="(value, key) in selectedElement.metadata" :key="key">
              <dt>{{ key }}</dt>
              <dd>{{ Array.isArray(value) ? value.join(', ') : value }}</dd>
            </template>
          </dl>
        </template>
        <p v-else>Select a device or wire to inspect metadata.</p>
      </aside>
    </div>
  </section>
</template>
