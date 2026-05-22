<script setup>
import { computed, reactive, ref } from 'vue';
import { createPolyglotFunctionBlueprint, evaluatePolyglotScript } from '../api/fabricApi';

const blueprintBusy = ref(false);
const scriptBusy = ref(false);
const error = ref('');
const blueprint = ref(null);
const selectedPath = ref('');
const scriptResult = ref(null);

const blueprintForm = reactive({
  targetPlatform: 'AWS_LAMBDA',
  functionName: 'packet-risk-router',
  language: 'js',
  environmentJson: '{\n  "FALLBACK_ROUTE": "/fallback/quarantine"\n}',
  handlerSource: `const meta = input || {};
const suspicious = meta.severity === 'CRITICAL' || meta.destinationIpNode?.includes('honeypot');
JSON.stringify({
  immutablePayload: true,
  observedRoute: {
    source: meta.sourcePointNode || meta.source || 'unknown',
    destination: meta.destinationIpNode || meta.destination || 'unknown'
  },
  action: suspicious ? 'TUNNEL_TO_FALLBACK' : 'ALLOW_TO_DESTINATION',
  fallback: suspicious ? '/fallback/quarantine' : null
});`
});

const scriptForm = reactive({
  language: 'js',
  bindingsJson: '{\n  "sourcePointNode": "Host-Alpha (VLAN 10)",\n  "destinationIpNode": "10.194.24.102",\n  "severity": "WARNING"\n}',
  source: `const suspicious = input.severity === 'CRITICAL' || input.destinationIpNode === '10.194.24.220';
({
  routeDecision: suspicious ? 'TUNNEL_TO_FALLBACK' : 'ALLOW_TO_DESTINATION',
  canMutatePayload: false,
  metadataOnly: {
    source: input.sourcePointNode,
    destination: input.destinationIpNode
  }
});`
});

const selectedFile = computed(() => {
  if (!blueprint.value?.files?.length) return null;
  return blueprint.value.files.find((file) => file.path === selectedPath.value) ?? blueprint.value.files[0];
});

function parseJson(raw, label) {
  try {
    return raw?.trim() ? JSON.parse(raw) : {};
  } catch {
    throw new Error(`${label} must be valid JSON`);
  }
}

async function generateBlueprint() {
  blueprintBusy.value = true;
  error.value = '';
  try {
    const response = await createPolyglotFunctionBlueprint({
      targetPlatform: blueprintForm.targetPlatform,
      functionName: blueprintForm.functionName,
      language: blueprintForm.language,
      handlerSource: blueprintForm.handlerSource,
      environment: parseJson(blueprintForm.environmentJson, 'Environment')
    });
    blueprint.value = response;
    selectedPath.value = response.files?.[0]?.path ?? '';
  } catch (err) {
    error.value = err.message;
  } finally {
    blueprintBusy.value = false;
  }
}

async function evaluateScript() {
  scriptBusy.value = true;
  error.value = '';
  try {
    scriptResult.value = await evaluatePolyglotScript({
      language: scriptForm.language,
      source: scriptForm.source,
      bindings: parseJson(scriptForm.bindingsJson, 'Bindings')
    });
  } catch (err) {
    error.value = err.message;
  } finally {
    scriptBusy.value = false;
  }
}
</script>

<template>
  <div class="polyglot-grid">
    <section class="panel space-y">
      <div class="endpoint-line">
        <span class="method post">POST</span>
        <code>/platform/functions/blueprints</code>
      </div>
      <div class="panel-title-row">
        <div>
          <h3>GraalVM Function Blueprint Factory</h3>
          <p class="muted">Generate AWS Lambda or Azure Functions wrappers around a GraalJS handler.</p>
        </div>
        <span class="status-chip green">Source Artifact</span>
      </div>

      <form class="form-grid" @submit.prevent="generateBlueprint">
        <div class="form-pair">
          <label>
            Target Platform
            <select v-model="blueprintForm.targetPlatform">
              <option>AWS_LAMBDA</option>
              <option>AZURE_FUNCTIONS</option>
            </select>
          </label>
          <label>
            Function Name
            <input v-model="blueprintForm.functionName" required />
          </label>
        </div>

        <label>
          Environment JSON
          <textarea v-model="blueprintForm.environmentJson" class="code-editor short" rows="4" />
        </label>

        <label>
          GraalJS Handler Source
          <textarea v-model="blueprintForm.handlerSource" class="code-editor" rows="14" required />
        </label>

        <button class="primary-button" :disabled="blueprintBusy">
          {{ blueprintBusy ? 'Generating...' : 'Generate Function Blueprint' }}
        </button>
      </form>
    </section>

    <section class="panel space-y">
      <div class="endpoint-line">
        <span class="method post">POST</span>
        <code>/platform/polyglot/scripts/evaluate</code>
      </div>
      <div class="panel-title-row">
        <div>
          <h3>Constrained Polyglot Evaluator</h3>
          <p class="muted">Run GraalJS with supplied bindings and locked-down host access.</p>
        </div>
        <span class="status-chip blue">GraalJS</span>
      </div>

      <form class="form-grid" @submit.prevent="evaluateScript">
        <label>
          Bindings JSON
          <textarea v-model="scriptForm.bindingsJson" class="code-editor short" rows="6" />
        </label>

        <label>
          Script Source
          <textarea v-model="scriptForm.source" class="code-editor" rows="11" required />
        </label>

        <button class="primary-button" :disabled="scriptBusy">
          {{ scriptBusy ? 'Evaluating...' : 'Evaluate GraalJS Script' }}
        </button>
      </form>

      <pre class="console-frame compact">{{ scriptResult ? JSON.stringify(scriptResult, null, 2) : 'Execution result idle.' }}</pre>
    </section>

    <section v-if="blueprint" class="panel generated-panel">
      <div class="panel-title-row">
        <div>
          <h3>{{ blueprint.artifactId }}</h3>
          <p class="muted">{{ blueprint.targetPlatform }} · {{ blueprint.runtime }}</p>
        </div>
        <span class="status-chip green">Generated</span>
      </div>

      <div class="generated-layout">
        <div class="file-list">
          <button
            v-for="file in blueprint.files"
            :key="file.path"
            :class="['file-button', { active: selectedFile?.path === file.path }]"
            @click="selectedPath = file.path"
          >
            {{ file.path }}
          </button>
        </div>

        <pre class="console-frame source-preview">{{ selectedFile?.content }}</pre>
      </div>

      <div class="event-stack">
        <article v-for="hint in blueprint.deploymentHints" :key="hint" class="event-row">
          <strong>Deployment Hint</strong>
          <span>{{ hint }}</span>
        </article>
      </div>
    </section>

    <p v-if="error" class="form-error">{{ error }}</p>
  </div>
</template>
