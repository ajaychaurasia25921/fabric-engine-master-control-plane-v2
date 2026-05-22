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
  const response = await fetch(`${API_BASE}/sms/dispatches`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return parseJson(response, 'SMS dispatch failed');
}

export function openFabricStream(onEvent) {
  const source = new EventSource(`${API_BASE}/api/v1/fabric/stream`);
  const parse = (message) => onEvent(JSON.parse(message.data));

  source.onmessage = parse;
  source.addEventListener('NODE_STATUS', parse);
  source.addEventListener('SECURITY_ALERT', parse);
  source.addEventListener('AI_REMEDIATION', parse);

  return source;
}
