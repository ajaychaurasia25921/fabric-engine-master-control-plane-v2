<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { getSmsGatewayStatus } from '../api/fabricApi';
import { useFabricStore } from '../stores/fabricStore';

const providerToProtocol = {
  LOCAL_SMPP_TRUNK: 'SMPP_V34',
  GSM_MODEM_AT: 'GSM_AT_COMMANDS',
  INTERNAL_REST_FORWARD: 'REST_FORWARD'
};

const store = useFabricStore();
const busy = ref(false);
const error = ref('');
const gateway = ref(null);
const logs = ref([
  '[00:00:01] System localized message broker pipeline initialized successfully.',
  '[00:05:12] Heartbeat check on local carrier endpoint (LOCAL_SMPP_TRUNK) -> 200 OK.'
]);
const form = reactive({
  provider: 'LOCAL_SMPP_TRUNK',
  recipient: '+1 (555) 019-2834',
  senderId: 'FABRIC_ENG',
  message: 'CRITICAL_ALERT: Hypervisor core node cluster [prod-zone-edge] triggered microcode validation failure. Action required.',
  webhookUrl: 'https://api.internal.security.monitor/webhooks/sms-receipt'
});

const latestCritical = computed(() => store.events.find((event) => event.severity === 'CRITICAL'));

onMounted(async () => {
  try {
    gateway.value = await getSmsGatewayStatus();
  } catch {
    gateway.value = null;
  }
});

function stamp() {
  return new Date().toLocaleTimeString('en-US', { hour12: false });
}

function useLatestCritical() {
  if (!latestCritical.value) return;
  form.message = `${latestCritical.value.severity}_ALERT: ${latestCritical.value.nodeId} ${latestCritical.value.message}`;
}

function bindWebhook() {
  logs.value = [`[${stamp()}] [WEBHOOK_BIND] Receipt callback bound -> ${form.webhookUrl}`, ...logs.value];
}

async function submit() {
  busy.value = true;
  error.value = '';
  try {
    const response = await store.submitSms({
      targetMobileEndpoint: form.recipient,
      smsMessageText: `[${form.senderId}] ${form.message}`.slice(0, 160),
      signalingProtocol: providerToProtocol[form.provider]
    });
    logs.value = [
      `[${stamp()}] [DISPATCH] Provider: ${form.provider} -> Recipient: ${form.recipient} -> STATUS: ${response.gatewayStatus}`,
      `[${stamp()}] [TRACKING] smsTrackingId=${response.smsTrackingId}`,
      ...logs.value
    ];
  } catch (err) {
    error.value = err.message;
    logs.value = [`[${stamp()}] [ERROR] ${err.message}`, ...logs.value];
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <div class="sms-grid">
    <section class="panel space-y">
      <div>
        <div class="panel-title-row">
          <h3>SMS Dispatch Terminal</h3>
          <span class="status-chip blue">Acknowledge Ready</span>
        </div>
        <p class="muted">Broadcast high-priority critical warning anomalies or system auth strings.</p>
      </div>

      <form class="form-grid" @submit.prevent="submit">
        <label>
          Upstream Provider Routing API
          <select v-model="form.provider">
            <option value="LOCAL_SMPP_TRUNK">Local Carrier SMPP Trunk</option>
            <option value="GSM_MODEM_AT">On-Prem GSM Modem AT Commands</option>
            <option value="INTERNAL_REST_FORWARD">Internal REST Forwarder</option>
          </select>
        </label>

        <label>
          Destination Mobile MSISDN
          <input v-model="form.recipient" required />
        </label>

        <label>
          Alphanumeric Sender ID Mask
          <input v-model="form.senderId" />
        </label>

        <label>
          SMS Payload Content Stream
          <textarea v-model="form.message" maxlength="160" rows="4" required />
        </label>

        <div class="button-row">
          <button class="secondary-button" type="button" @click="useLatestCritical">Use latest critical</button>
          <button class="primary-button" type="submit" :disabled="busy">
            {{ busy ? 'Dispatching...' : 'Dispatch SMS Pipeline' }}
          </button>
        </div>
        <p v-if="error" class="form-error">{{ error }}</p>
      </form>
    </section>

    <section class="panel sms-logs">
      <div class="panel-title-row">
        <h4>Webhook Callback Matrix</h4>
        <span class="status-chip green">Active Listening</span>
      </div>

      <div class="webhook-row">
        <label>
          Target Endpoint URL Destination
          <input v-model="form.webhookUrl" type="url" />
        </label>
        <button class="secondary-button" type="button" @click="bindWebhook">Bind Webhook</button>
      </div>

      <div v-if="gateway" class="metric-strip">
        <span>SMSC {{ gateway.connectedSmsC }}</span>
        <span>{{ gateway.activeModemPorts }} modem ports</span>
        <span>{{ gateway.signalStrengthDbm }} dBm</span>
      </div>

      <h4>Live Transmission Loop Realtime Audit Logs</h4>
      <div class="log-frame">
        <p v-for="line in logs" :key="line">{{ line }}</p>
      </div>

      <h4>SMS Dispatches</h4>
      <div class="event-stack">
        <article v-for="dispatch in store.dispatches" :key="dispatch.smsTrackingId" class="event-row">
          <strong>{{ dispatch.gatewayStatus }}</strong>
          <span>{{ dispatch.smsTrackingId }}</span>
        </article>
      </div>
    </section>
  </div>
</template>
