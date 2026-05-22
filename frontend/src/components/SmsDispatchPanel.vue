<script setup>
import { computed, reactive, ref } from 'vue';
import { useFabricStore } from '../stores/fabricStore';

const store = useFabricStore();
const busy = ref(false);
const error = ref('');
const form = reactive({
  recipient: '+15551234567',
  severity: 'CRITICAL',
  correlationId: '',
  message: 'Fabric anomaly detected. Ollama remediation has started.'
});

const latestCritical = computed(() => store.events.find((event) => event.severity === 'CRITICAL'));

function useLatestCritical() {
  if (!latestCritical.value) return;
  form.correlationId = latestCritical.value.id;
  form.message = `${latestCritical.value.nodeId}: ${latestCritical.value.message}`;
  form.severity = latestCritical.value.severity;
}

async function submit() {
  busy.value = true;
  error.value = '';
  try {
    await store.submitSms({ ...form });
  } catch (err) {
    error.value = err.message;
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <aside class="side-panel">
    <form class="sms-form" @submit.prevent="submit">
      <header class="panel-header">
        <h2>SMS Dispatch</h2>
        <button type="button" class="ghost-button" @click="useLatestCritical">Use latest critical</button>
      </header>

      <label>
        Recipient
        <input v-model="form.recipient" type="tel" autocomplete="tel" required />
      </label>

      <label>
        Severity
        <select v-model="form.severity">
          <option>INFO</option>
          <option>WARNING</option>
          <option>CRITICAL</option>
        </select>
      </label>

      <label>
        Correlation ID
        <input v-model="form.correlationId" placeholder="event or incident id" />
      </label>

      <label>
        Message
        <textarea v-model="form.message" maxlength="480" rows="5" required />
      </label>

      <button class="primary-button" type="submit" :disabled="busy">
        {{ busy ? 'Dispatching...' : 'Dispatch Alert' }}
      </button>
      <p v-if="error" class="form-error">{{ error }}</p>
    </form>

    <section class="event-list">
      <h2>Recent Fabric Events</h2>
      <article v-for="event in store.events" :key="event.id" class="event-row">
        <strong>{{ event.nodeId }}</strong>
        <span>{{ event.severity }} · {{ event.type }}</span>
        <p>{{ event.message }}</p>
        <small v-if="event.recommendation">{{ event.recommendation }}</small>
      </article>
    </section>

    <section class="event-list">
      <h2>SMS Dispatches</h2>
      <article v-for="dispatch in store.dispatches" :key="dispatch.id" class="event-row">
        <strong>{{ dispatch.status }}</strong>
        <span>{{ dispatch.providerMessageId }}</span>
      </article>
    </section>
  </aside>
</template>
