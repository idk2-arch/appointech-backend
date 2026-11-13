'use strict';
// Lectura offline. Solo escribe el resumen nuevo en analisis; nunca en resultados.
const fs = require('fs'), path = require('path'), crypto = require('crypto'), assert = require('assert');
const perf = path.resolve(__dirname, '..');
function* csv(text) {
  let row = [], value = '', quoted = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (c === '"') { if (quoted && text[i + 1] === '"') { value += '"'; i++; } else quoted = !quoted; }
    else if (c === ',' && !quoted) { row.push(value); value = ''; }
    else if (c === '\n' && !quoted) { row.push(value.replace(/\r$/, '')); yield row; row = []; value = ''; }
    else value += c;
  }
  if (value || row.length) { row.push(value); yield row; }
  assert(!quoted, 'CSV sin cierre');
}
function stats(rr) {
  const v = rr.map(r => +r.elapsed).sort((a,b) => a-b), n = v.length;
  let a = Infinity, b = -Infinity;
  const codes = {};
  for (const r of rr) { a = Math.min(a, +r.timeStamp); b = Math.max(b, +r.timeStamp + +r.elapsed); codes[r.responseCode] = (codes[r.responseCode] || 0) + 1; }
  return { n, fallidas: rr.filter(r => r.success === 'false').length, codigos: codes,
    ventana_s: (b-a)/1000, media_ms: v.reduce((a,b) => a+b,0)/n,
    p95_ms: v[Math.ceil(.95*n)-1], p99_ms: v[Math.ceil(.99*n)-1], max_ms: v[n-1] };
}
const digest = p => crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
const selected = [];
for (const ent of fs.readdirSync(path.join(perf, 'resultados'), {withFileTypes:true})) {
  if (!ent.isDirectory()) continue;
  const m = /^e([1-8])-/.exec(ent.name);
  const sustained = /^e6-sostenido-/.test(ent.name);
  if (!m || (m[1] === '6' && !sustained)) continue;
  const dir = path.join(perf, 'resultados', ent.name), name = `e${m[1]}.jtl`;
  if (m[1] === '5' || m[1] === '6') {
    for (const sub of fs.readdirSync(dir)) if (sub.startsWith('fase-') && fs.existsSync(path.join(dir,sub,name))) selected.push(path.join(dir,sub,name));
  } else if (fs.existsSync(path.join(dir,name))) selected.push(path.join(dir,name));
}
const output = [];
for (const p of selected.sort()) {
  const hash = digest(p), iter = csv(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));
  const head = iter.next().value, rr = [];
  for (const row of iter) {
    if (row.length === 1 && !row[0]) continue;
    assert.equal(row.length, head.length, p);
    rr.push(Object.fromEntries(head.map((h,i) => [h,row[i]])));
  }
  if (!rr.length) continue;
  const item = { jtl:path.relative(perf,p).replace(/\\/g,'/'), sha256:hash, total:stats(rr), por_label:{} };
  for (const label of new Set(rr.map(r => r.label))) item.por_label[label] = stats(rr.filter(r => r.label === label));
  if (p.includes('e8-50-20261111-220757')) {
    const clock = JSON.parse(fs.readFileSync(path.join(path.dirname(p),'reloj.json'),'utf8').replace(/^\uFEFF/,''));
    item.medicion = stats(rr.filter(r => +r.timeStamp >= clock.inicioMedicion && +r.timeStamp < clock.finMedicion));
  }
  assert.equal(digest(p),hash,'JTL cambio durante lectura');
  output.push(item);
  console.log(item.jtl, JSON.stringify(item.total));
}
fs.writeFileSync(path.join(__dirname,'verificacion-informe-final.json'),JSON.stringify(output,null,2));
const report = path.join(perf,'informes','INFORME-FINAL-CARGA-ESTRES.md');
const links = [...fs.readFileSync(report,'utf8').matchAll(/\]\(([^)]+)\)/g)].map(m => m[1]);
const missing = links.filter(link => !fs.existsSync(path.resolve(path.dirname(report),link)));
assert.deepEqual(missing, [], 'Enlaces locales inexistentes');
console.log('Enlaces locales verificados:',links.length);
console.log('Porcentajes E6:',[77/102525*100,6741/100070*100,11846/102705*100]);
