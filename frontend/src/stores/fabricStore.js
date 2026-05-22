import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { dispatchSms, fetchTopology, openFabricStream } from '../api/fabricApi';

const statusColors = {
  RUNNING: '#3b82f6',
  PROVISIONED: '#64748b',
  INITIALIZING: '#f59e0b',
  DEGRADED: '#f97316',
  DOWN: '#ef4444',
  REMEDIATING: '#8b5cf6'
};

const statusBackgrounds = {
  RUNNING: '#111827',
  PROVISIONED: '#0f172a',
  INITIALIZING: '#1f1a0b',
  DEGRADED: '#25140b',
  DOWN: '#2a0f12',
  REMEDIATING: '#17132f'
};

export const useFabricStore = defineStore('fabric', () => {
  const nodes = ref([]);
  const edges = ref([]);
  const events = ref([]);
  const dispatches = ref([]);
  const connected = ref(false);
  const streamError = ref('');
  let eventSource;

  const vueFlowNodes = computed(() => nodes.value.map((node) => ({
    id: node.id,
    type: 'default',
    label: `${node.label} · ${node.status}`,
    position: { x: node.x, y: node.y },
    data: { ...node },
    style: {
      border: `2px solid ${statusColors[node.status] ?? '#667085'}`,
      background: statusBackgrounds[node.status] ?? '#ffffff',
      color: '#f8fafc',
      borderRadius: '8px',
      minWidth: '150px',
      fontWeight: 800,
      boxShadow: node.status === 'DOWN' ? '0 10px 22px rgba(239, 68, 68, 0.28)' : '0 8px 18px rgba(0, 0, 0, 0.24)'
    }
  })));

  const vueFlowEdges = computed(() => edges.value.map((edge) => ({
    id: edge.id,
    source: edge.source,
    target: edge.target,
    label: edge.label,
    animated: true,
    style: { stroke: '#64748b', strokeWidth: 2 },
    labelStyle: { fontWeight: 700, fill: '#cbd5e1' }
  })));

  async function loadTopology() {
    const topology = await fetchTopology();
    nodes.value = topology.nodes;
    edges.value = topology.edges;
  }

  function applyEvent(event) {
    events.value = [event, ...events.value].slice(0, 30);
    if (!event.status) return;

    nodes.value = nodes.value.map((node) => (
      node.id === event.nodeId ? { ...node, status: event.status } : node
    ));
  }

  function connectStream() {
    if (eventSource) return;
    eventSource = openFabricStream(applyEvent);
    eventSource.onopen = () => {
      connected.value = true;
      streamError.value = '';
    };
    eventSource.onerror = () => {
      connected.value = false;
      streamError.value = 'Fabric SSE stream is reconnecting';
    };
  }

  function disconnectStream() {
    eventSource?.close();
    eventSource = undefined;
    connected.value = false;
  }

  async function submitSms(form) {
    const response = await dispatchSms(form);
    dispatches.value = [response, ...dispatches.value].slice(0, 12);
    return response;
  }

  return {
    nodes,
    edges,
    events,
    dispatches,
    connected,
    streamError,
    vueFlowNodes,
    vueFlowEdges,
    loadTopology,
    connectStream,
    disconnectStream,
    applyEvent,
    submitSms
  };
});
