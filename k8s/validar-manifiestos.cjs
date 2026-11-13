// Validación local sin clúster; no reemplaza el esquema/API de Kubernetes.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = __dirname;
const objects = [];
for (const file of fs.readdirSync(root).filter(f => /^\d.*\.yaml$/.test(f))) {
  const text = fs.readFileSync(path.join(root, file), 'utf8');
  if (file === '00-namespace.yaml') {
    assert.match(text, /kind: Namespace/);
    assert.match(text, /name: appointech/);
    continue;
  }
  const doc = JSON.parse(text);
  objects.push(...(doc.kind === 'List' ? doc.items : [doc]));
}
const cm = objects.find(o => o.kind === 'ConfigMap' && o.metadata.name === 'appointech-config');
const secretKeys = fs.readFileSync(path.join(root, 'secret.env.example'), 'utf8')
  .split(/\r?\n/).filter(l => l && !l.startsWith('#')).map(l => l.split('=')[0]);
assert.equal(cm.data.SPRING_PROFILES_ACTIVE, 'prod');
assert.equal(cm.data.SPRINGDOC_API_DOCS_ENABLED, 'false');
assert.equal(cm.data.SPRINGDOC_SWAGGER_UI_ENABLED, 'false');
assert(!objects.some(o => o.kind === 'Secret'));
const ids = new Set();
for (const obj of objects) {
  assert(obj.apiVersion && obj.kind && obj.metadata.name);
  assert.equal(obj.metadata.namespace, 'appointech');
  const id = obj.kind + '/' + obj.metadata.name;
  assert(!ids.has(id)); ids.add(id);
  if (!['Deployment', 'StatefulSet'].includes(obj.kind)) continue;
  const spec = obj.spec.template.spec;
  assert.equal(spec.nodeSelector['kubernetes.io/arch'], 'amd64');
  assert.deepEqual(obj.spec.selector.matchLabels, obj.spec.template.metadata.labels);
  for (const c of spec.containers) {
    for (const e of c.env || []) {
      if (e.valueFrom?.configMapKeyRef) assert(Object.hasOwn(cm.data, e.valueFrom.configMapKeyRef.key));
      if (e.valueFrom?.secretKeyRef) assert(secretKeys.includes(e.valueFrom.secretKeyRef.key));
    }
    for (const p of ['startupProbe', 'readinessProbe', 'livenessProbe']) assert(c[p]);
    assert(c.resources.requests && c.resources.limits);
  }
}
const pg = objects.find(o => o.kind === 'StatefulSet');
assert.equal(pg.spec.volumeClaimTemplates[0].spec.storageClassName, 'local-path');
assert.equal(pg.spec.volumeClaimTemplates[0].spec.resources.requests.storage, '5Gi');
for (const ingress of objects.filter(o => o.kind === 'Ingress')) {
  assert.equal(ingress.spec.ingressClassName, 'traefik');
  assert.equal(cm.data.CORS_ORIGINS, 'https://' + ingress.spec.rules[0].host);
  for (const p of ingress.spec.rules[0].http.paths) {
    assert(objects.some(o => o.kind === 'Service' && o.metadata.name === p.backend.service.name));
  }
}
const issuers = JSON.parse(fs.readFileSync(path.join(root, 'infra/clusterissuers.yaml'), 'utf8'));
assert.equal(issuers.items.length, 2);
console.log('OK: JSON/YAML, referencias, probes, recursos, PVC, rutas, host, perfiles y ausencia de Secrets.');
console.log('Pendiente: esquema/API del clúster y publicación de frontend-pendiente.');
