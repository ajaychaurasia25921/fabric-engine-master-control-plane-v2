const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';

async function parseJson(response, fallbackMessage) {
  if (!response.ok) {
    const body = await response.text();
    throw new Error(body || fallbackMessage);
  }
  return response.json();
}

export async function fetchTopology() {
  const response = await fetch(`${API_BASE}/api/v1/fabric/topology`);
  return parseJson(response, 'Unable to load fabric topology');
}

export async function dispatchSms(payload) {
  const response = await fetch(`${API_BASE}/api/v1/sms/dispatches`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return parseJson(response, 'SMS dispatch failed');
}

export async function getSmsGatewayStatus() {
  const response = await fetch(`${API_BASE}/api/v1/sms/gateways/status`);
  return parseJson(response, 'SMS gateway status unavailable');
}

export async function provisionServer(payload) {
  const response = await fetch(`${API_BASE}/api/v1/systems/servers`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return parseJson(response, 'Server provisioning failed');
}

export async function scrambleIdentity() {
  const response = await fetch(`${API_BASE}/api/v1/network/identity/scramble`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ triggerConfirmation: true })
  });
  return parseJson(response, 'Coordinate scramble failed');
}

export async function fetchFirewallRules() {
  const response = await fetch(`${API_BASE}/api/v1/security/firewall/rules`);
  return parseJson(response, 'Firewall rules unavailable');
}

export async function createFirewallRule(payload) {
  const response = await fetch(`${API_BASE}/api/v1/security/firewall/rules`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return parseJson(response, 'Firewall rule commit failed');
}

export async function fetchHoneypotIncidents() {
  const response = await fetch(`${API_BASE}/api/v1/security/honeypots/incidents`);
  return parseJson(response, 'Honeypot incidents unavailable');
}

export async function tracePacket(payload) {
  const response = await fetch(`${API_BASE}/api/v1/networking/packet-trace`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return parseJson(response, 'Packet trace failed');
}

export async function stageFileMetadata(payload) {
  const response = await fetch(`${API_BASE}/api/v1/transfers/staged-payloads`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return parseJson(response, 'File staging failed');
}

export async function executeFilePipeline(payload) {
  const response = await fetch(`${API_BASE}/api/v1/transfers/pipelines`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return parseJson(response, 'Pipeline execution failed');
}

export async function runTerminalCommand(payload) {
  const response = await fetch(`${API_BASE}/api/v1/networking/telnet/command`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return parseJson(response, 'Terminal command failed');
}

export async function executeQuantumCircuit(payload) {
  const response = await fetch(`${API_BASE}/api/v1/quantum/circuits/executions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return parseJson(response, 'Quantum circuit dispatch failed');
}

export async function spawnSocket(payload) {
  const response = await fetch(`${API_BASE}/api/v1/networking/socket-listeners`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return parseJson(response, 'Socket listener spawn failed');
}

export async function createPolyglotFunctionBlueprint(payload) {
  const response = await fetch(`${API_BASE}/api/v1/platform/functions/blueprints`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return parseJson(response, 'Polyglot function blueprint generation failed');
}

export async function evaluatePolyglotScript(payload) {
  const response = await fetch(`${API_BASE}/api/v1/platform/polyglot/scripts/evaluate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return parseJson(response, 'Polyglot script evaluation failed');
}

export function openFabricStream(onEvent) {
  const source = new EventSource(`${API_BASE}/api/v1/fabric/stream`);
  const parse = (message) => onEvent(JSON.parse(message.data));

  source.onmessage = parse;
  source.addEventListener('NODE_STATUS', parse);
  source.addEventListener('SECURITY_ALERT', parse);
  source.addEventListener('AI_REMEDIATION', parse);
  source.addEventListener('PIPELINE_UPDATE', parse);

  return source;
}
