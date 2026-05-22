import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { dispatchSms, fetchTopology, openFabricStream } from '../api/fabricApi';

const statusColors = {
  healthy: '#16805d',
  degraded: '#b7791f',
  down: '#c7362f',
  remediating: '#2563eb'
};

const statusBackgrounds = {
  healthy: '#f7fffb',
  degraded: '#fff8eb',
  down: '#fff1f0',
  remediating: '#eff6ff'
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
      color: '#111827',
      borderRadius: '8px',
      minWidth: '150px',
      fontWeight: 800,
      boxShadow: node.status === 'down' ? '0 10px 22px rgba(199, 54, 47, 0.16)' : '0 8px 16px rgba(17, 24, 39, 0.08)'
    }
  })));

  const vueFlowEdges = computed(() => edges.value.map((edge) => ({
    id: edge.id,
    source: edge.source,
    target: edge.target,
    label: edge.label,
    animated: true,
    style: { stroke: '#607d8b', strokeWidth: 2 },
    labelStyle: { fontWeight: 700, fill: '#344054' }
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
