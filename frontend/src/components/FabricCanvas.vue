<script setup>
import { onBeforeUnmount, onMounted, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { Background } from '@vue-flow/background';
import { Controls } from '@vue-flow/controls';
import { VueFlow, useVueFlow } from '@vue-flow/core';
import { useFabricStore } from '../stores/fabricStore';

const store = useFabricStore();
const { vueFlowNodes, vueFlowEdges } = storeToRefs(store);
const { setNodes, setEdges, fitView } = useVueFlow();

watch(vueFlowNodes, (nodes) => setNodes(nodes), { immediate: true });
watch(vueFlowEdges, (edges) => setEdges(edges), { immediate: true });

onMounted(async () => {
  await store.loadTopology();
  store.connectStream();
  requestAnimationFrame(() => fitView({ padding: 0.2 }));
});

onBeforeUnmount(() => store.disconnectStream());
</script>

<template>
  <section class="canvas-shell">
    <div class="canvas-toolbar">
      <div>
        <h1>Fabric Engine Master Control Plane</h1>
        <p>{{ store.streamError || 'Reactive packet fabric with AI remediation telemetry' }}</p>
      </div>
      <span :class="['stream-pill', { live: store.connected }]">
        {{ store.connected ? 'SSE Live' : 'SSE Offline' }}
      </span>
    </div>

    <VueFlow
      class="fabric-canvas"
      :nodes="vueFlowNodes"
      :edges="vueFlowEdges"
      :fit-view-on-init="true"
      :nodes-draggable="true"
    >
      <Background gap="28" />
      <Controls />
    </VueFlow>
  </section>
</template>
