/*
 * TBalance: vistas Hoy, cuaderno de caja, frases y acento por tipo de negocio.
 * Se carga después del script principal y solo cambia la presentación:
 * los datos siguen llegando por window.javaBridge igual que en la v3.4.0.
 */
(function () {
  'use strict';

  // ── Acento por tipo de negocio (contraste revisado en claro y oscuro) ──
  const ACENTOS = {
    TIENDA_BARRIO: { acc: '#f6e016', ink: '#0d2a1c', mk: '#0d2a1c', mkD: '#f6e016', nombre: 'Amarillo' },
    SUPERMERCADO:  { acc: '#d92b21', ink: '#ffffff', mk: '#d92b21', mkD: '#ff7a6e', nombre: 'Rojo' },
    FERRETERIA:    { acc: '#c2501b', ink: '#ffffff', mk: '#c2501b', mkD: '#f08a55', nombre: 'Naranja' },
    FARMACIA:      { acc: '#0d7480', ink: '#ffffff', mk: '#0d7480', mkD: '#5cc0cc', nombre: 'Verde azulado' },
    BOUTIQUE:      { acc: '#9a3c4c', ink: '#ffffff', mk: '#9a3c4c', mkD: '#e8939f', nombre: 'Vino' },
    TECNOLOGIA:    { acc: '#1f2de6', ink: '#ffffff', mk: '#1f2de6', mkD: '#8f98ff', nombre: 'Azul' },
    NINGUNO:       { acc: '#315d53', ink: '#ffffff', mk: '#315d53', mkD: '#7fc8aa', nombre: 'Verde TBalance' },
  };
  const LISTA = Object.values(ACENTOS);
  const porColor = c => LISTA.find(a => a.acc.toLowerCase() === String(c || '').toLowerCase());

  function acentoDe(perfil) {
    if (!perfil) return ACENTOS.NINGUNO;
    const tipo = ACENTOS[perfil.tipoNegocio] || ACENTOS.NINGUNO;
    const elegido = porColor(perfil.color);
    // #315D53 fue el color por defecto de todos los perfiles en la v3.4.0: se usa el del tipo de negocio
    if (!elegido || (elegido === ACENTOS.NINGUNO && tipo !== ACENTOS.NINGUNO)) return tipo;
    return elegido;
  }
  function aplicarAcento() {
    const a = acentoDe(state.perfilActivo);
    const oscuro = document.documentElement.classList.contains('dark');
    const r = document.documentElement.style;
    r.setProperty('--acc', a.acc);
    r.setProperty('--acc-ink', a.ink);
    r.setProperty('--mark', oscuro ? a.mkD : a.mk);
  }

  // ── Formato colombiano: $ 2.800, 22,5 % ──
  window.fmt = function (n, dec) {
    const max = dec === undefined ? 2 : dec;
    return Number(n || 0).toLocaleString('es-CO', { minimumFractionDigits: 0, maximumFractionDigits: max });
  };
  const pesos = n => '$ ' + Math.round(Number(n) || 0).toLocaleString('es-CO');
  const e = s => String(s == null ? '' : s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

  // ── Fechas ──
  const DIAS = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];
  const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
  const iso = d => d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
  const hoy0 = () => { const d = new Date(); d.setHours(0, 0, 0, 0); return d; };
  const desdeIso = s => { const p = String(s).slice(0, 10).split('-'); return new Date(+p[0], +p[1] - 1, +p[2]); };
  const diasEntre = (a, b) => Math.round((b - a) / 86400000);
  function cuando(f) {
    const n = diasEntre(hoy0(), desdeIso(f));
    if (n < 0) return n === -1 ? 'ayer' : 'hace ' + (-n) + ' días';
    if (n === 0) return 'hoy';
    if (n === 1) return 'mañana';
    if (n < 7) return 'el ' + DIAS[desdeIso(f).getDay()];
    if (n < 28) return 'en ' + Math.round(n / 7) + (Math.round(n / 7) === 1 ? ' semana' : ' semanas');
    if (n < 365) return 'en ' + Math.round(n / 30) + (Math.round(n / 30) === 1 ? ' mes' : ' meses');
    return 'en ' + Math.round(n / 365) + (Math.round(n / 365) === 1 ? ' año' : ' años');
  }
  // La hora sale del ID de la venta (VTA-<milisegundos>) cuando existe
  function horaVenta(v) {
    const m = /(\d{13})/.exec(String(v.id || ''));
    if (!m) return '';
    const d = new Date(+m[1]);
    let h = d.getHours(); const mm = String(d.getMinutes()).padStart(2, '0');
    const ap = h >= 12 ? 'p. m.' : 'a. m.'; h = h % 12 || 12;
    return h + ':' + mm + ' ' + ap;
  }
  const msVenta = v => { const m = /(\d{13})/.exec(String(v.id || '')); return m ? +m[1] : 0; };

  // ── Cálculos sobre los datos del perfil ──
  const ganancia = v => (v.items || []).reduce((t, it) => t + (it.costoUnitario > 0 ? (it.precioUnitario - it.costoUnitario) * it.cantidad : 0), 0);
  const ventasDe = f => state.ventas.filter(v => String(v.fecha || '').slice(0, 10) === f);
  const esCredito = v => /fiado|cr[eé]dito$/i.test(v.metodoPago || '') && !/tarjeta/i.test(v.metodoPago || '');
  const esDigital = v => !esCredito(v) && !/efectivo/i.test(v.metodoPago || 'Efectivo');
  const resumenProductos = v => (v.items || []).map(it => it.cantidad + ' ' + (it.nombreProducto || getNombreProd(it.productoId))).join(', ');
  // Versión corta para Hoy: los primeros productos sin la presentación entre paréntesis y "y N más"
  // (la lista completa queda en el título y en la sección Ventas).
  const MAX_PRODUCTOS_HOY = 3;
  function resumenCorto(v) {
    const items = v.items || [];
    const nombre = it => String(it.nombreProducto || getNombreProd(it.productoId)).replace(/\s*\([^)]*\)\s*$/, '');
    const primeros = items.slice(0, MAX_PRODUCTOS_HOY).map(it => (it.cantidad > 1 ? it.cantidad + ' ' : '') + nombre(it));
    const resto = items.length - primeros.length;
    return primeros.join(', ') + (resto > 0 ? ' y ' + resto + ' más' : '');
  }
  const productosVenta = v => (v.items || []).length;
  const ideal = p => Math.max((p.stockMin || 0) * 2, (p.stockMin || 0) + 1);
  const tipoActual = () => (state.perfilActivo && state.perfilActivo.tipoNegocio) || 'NINGUNO';
  const palabraCredito = () => tipoActual() === 'TIENDA_BARRIO' ? 'fiado' : 'a crédito';

  const ICO = {
    reloj: '<circle cx="12" cy="13" r="8"/><path d="M12 9v4l2.5 2M9 2h6"/>',
    caja: '<path d="M21 8 12 3 3 8v8l9 5 9-5z"/><path d="M3 8l9 5 9-5M12 13v8"/>',
    agotado: '<circle cx="12" cy="12" r="9"/><path d="M5.6 5.6l12.8 12.8"/>',
    cobrar: '<path d="M16 4h3a1 1 0 0 1 1 1v15a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1h3"/><rect x="8" y="2" width="8" height="4" rx="1"/><path d="M9 12h6M9 16h4"/>',
    costo: '<circle cx="12" cy="12" r="9"/><path d="M12 7v10M15 9.5c0-1.4-1.3-2.5-3-2.5s-3 1-3 2.5 1.3 2 3 2.5 3 1.1 3 2.5-1.3 2.5-3 2.5-3-1.1-3-2.5"/>',
    mas: '<path d="M12 5v14M5 12h14"/>',
    ok: '<path d="m5 12 5 5L20 7"/>',
  };
  const ic = (n, s) => '<svg width="' + (s || 22) + '" height="' + (s || 22) + '" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + ICO[n] + '</svg>';
  const link = (txt, js) => '<button type="button" class="tbv-link" onclick="' + js + '">' + txt + '</button>';

  // Pendientes priorizados: vencidos/por vencer → agotados → por agotarse → cobros → sin costo
  function pendientes() {
    const r = [], hoy = hoy0();
    state.productos.forEach(p => {
      if (!p.fechaVencimiento || !(p.stock > 0)) return;
      const n = diasEntre(hoy, desdeIso(p.fechaVencimiento));
      if (n <= 7) r.push({ o: n < 0 ? 0 : 1, n,
        t: n < 0 ? 'Se venció el <b>' + e(p.nombre) + '</b> y quedan ' + p.stock + '.' : 'El <b>' + e(p.nombre) + '</b> se vence ' + cuando(p.fechaVencimiento) + ' y quedan ' + p.stock + '.',
        s: (p.proveedorId ? getNombreProv(p.proveedorId) + ' · ' : '') + 'vence el ' + desdeIso(p.fechaVencimiento).getDate() + ' de ' + MESES[desdeIso(p.fechaVencimiento).getMonth()],
        i: 'reloj', a: link('Ver producto', "tbvVerProducto('" + p.id + "')") });
    });
    state.productos.filter(p => (p.stock || 0) === 0).forEach(p => r.push({ o: 2, n: 0, t: 'Se agotó el <b>' + e(p.nombre) + '</b>.', s: p.proveedorId ? getNombreProv(p.proveedorId) : 'Sin proveedor asignado', i: 'agotado', a: link('Pedir', "registrarEntradaDe('" + p.id + "')") }));
    state.productos.filter(p => p.stock > 0 && p.stock <= (p.stockMin || 0)).forEach(p => r.push({ o: 3, n: p.stock, t: 'Quedan <b class="tbv-num">' + p.stock + '</b> de ' + ideal(p) + ' <b>' + e(p.nombre) + '</b>.', s: 'Mínimo configurado: ' + p.stockMin, i: 'caja', a: link('Pedir', "registrarEntradaDe('" + p.id + "')") }));
    const deudas = {};
    // lo que deben sale de Fiados (ventas fiadas menos abonos); si no se puede consultar, de las ventas
    const cuentasFiado = typeof window.tbFiadosResumen === 'function' ? window.tbFiadosResumen() : null;
    if (cuentasFiado) cuentasFiado.forEach(c => { deudas[c.cliente || 'Sin nombre'] = { total: c.saldo, desde: c.desde || new Date().toISOString().slice(0, 10) }; });
    else state.ventas.filter(esCredito).forEach(v => { const c = (v.cliente || 'Sin nombre').trim(); (deudas[c] = deudas[c] || { total: 0, desde: v.fecha }); deudas[c].total += v.total || 0; if (v.fecha < deudas[c].desde) deudas[c].desde = v.fecha; });
    const ordenadas = Object.entries(deudas).sort((a, b) => b[1].total - a[1].total);
    if (ordenadas.length) {
      const [c, d] = ordenadas[0], total = ordenadas.reduce((t, x) => t + x[1].total, 0), dias = -diasEntre(hoy, desdeIso(d.desde));
      r.push({ o: 4, n: 0, t: '<b>' + e(c) + '</b> debe <b class="tbv-num">' + pesos(d.total) + '</b> ' + palabraCredito() + (dias > 0 ? ' desde hace ' + dias + (dias === 1 ? ' día' : ' días') : ' desde hoy') + '.',
        s: ordenadas.length > 1 ? 'En total hay ' + pesos(total) + ' por cobrar a ' + ordenadas.length + ' clientes' : 'Es el único saldo por cobrar', i: 'cobrar', a: link(cuentasFiado ? 'Ver fiados' : 'Ver ventas', cuentasFiado ? "showSection('fiados')" : "showSection('ventas')") });
    }
    const sinCosto = state.productos.filter(p => !(p.precioCompra > 0));
    if (sinCosto.length) r.push({ o: 5, n: 0, t: sinCosto.length === 1 ? 'Al <b>' + e(sinCosto[0].nombre) + '</b> le falta el precio de compra.' : 'A <b class="tbv-num">' + sinCosto.length + '</b> productos les falta el precio de compra.', s: 'Su ganancia no se cuenta en los informes', i: 'costo', a: link('Completar', "tbvVerProducto('" + sinCosto[0].id + "')") });
    return r.sort((a, b) => a.o - b.o || a.n - b.n);
  }

  function pedidosPorProveedor() {
    const g = {};
    state.productos.filter(p => (p.stock || 0) <= (p.stockMin || 0)).forEach(p => {
      const k = p.proveedorId || '';
      (g[k] = g[k] || []).push({ p, cant: Math.max(ideal(p) - (p.stock || 0), 1) });
    });
    return Object.entries(g).map(([k, items]) => ({ id: k, nombre: k ? getNombreProv(k) : 'Sin proveedor asignado', items })).sort((a, b) => (a.id ? 0 : 1) - (b.id ? 0 : 1) || b.items.length - a.items.length).slice(0, 3);
  }

  // ── Hoy ──
  function renderHoy() {
    const sec = document.getElementById('sec-dashboard');
    if (!sec) return;
    let box = document.getElementById('tbv-hoy');
    if (!box) { box = document.createElement('div'); box.id = 'tbv-hoy'; sec.prepend(box); }
    const p = state.perfilActivo;
    if (!p) { box.innerHTML = '<div class="tbv-top"><div><p role="heading" aria-level="1" class="tbv-h1">Cree su primer negocio</p><p class="tbv-lead">TBalance guarda el inventario, las ventas y los proveedores de cada negocio por separado.</p></div><div class="tbv-acciones"><button class="tbv-btn" onclick="openModalPerfil()">' + ic('mas', 18) + 'Crear negocio</button></div></div>'; return; }
    const hoy = hoy0(), fHoy = iso(hoy), semana = new Date(hoy); semana.setDate(semana.getDate() - 7);
    const vHoy = ventasDe(fHoy), vAnt = ventasDe(iso(semana));
    const total = vHoy.reduce((t, v) => t + (v.total || 0), 0), totalAnt = vAnt.reduce((t, v) => t + (v.total || 0), 0), gan = vHoy.reduce((t, v) => t + ganancia(v), 0);
    let comp = '';
    if (totalAnt > 0) { const pct = Math.round((total - totalAnt) / totalAnt * 100); comp = pct === 0 ? 'Igual que el ' + DIAS[semana.getDay()] + ' pasado' : Math.abs(pct) + ' % ' + (pct > 0 ? 'más' : 'menos') + ' que el ' + DIAS[semana.getDay()] + ' pasado'; }
    const fechaTxt = DIAS[hoy.getDay()].charAt(0).toUpperCase() + DIAS[hoy.getDay()].slice(1) + ' ' + hoy.getDate() + ' de ' + MESES[hoy.getMonth()];
    const titulo = vHoy.length ? 'Hoy van <span class="tbv-num" data-tbv-contar="' + Math.round(total) + '">' + pesos(total) + '</span> en <span class="tbv-num">' + vHoy.length + '</span> ' + (vHoy.length === 1 ? 'venta' : 'ventas') + '.' : 'Todavía no hay ventas hoy.';
    const lead = vHoy.length ? [comp, gan > 0 ? 'De eso, <span class="tbv-num">' + pesos(gan) + '</span> son ganancia' : ''].filter(Boolean).join('. ') + '.' : (state.ventas.length ? 'La última venta fue ' + cuando(state.ventas.map(v => v.fecha).sort().pop()) + '.' : 'Registre la primera venta para ver aquí el resumen del día.');
    const pend = pendientes().slice(0, 3);
    const ultimas = (vHoy.length ? vHoy : state.ventas.slice()).slice().sort((a, b) => msVenta(b) - msVenta(a) || String(b.fecha).localeCompare(String(a.fecha))).slice(0, 4);
    const pedidos = pedidosPorProveedor();
    box.innerHTML =
      '<header class="tbv-top"><div><p class="tbv-meta">' + fechaTxt + ' · ' + e(p.nombre) + '</p><p role="heading" aria-level="1" class="tbv-h1">' + titulo + '</p><p class="tbv-lead">' + lead + '</p></div>' +
      '<div class="tbv-acciones"><button type="button" class="tbv-link" onclick="openModalVentaQR()">Vender con QR</button><button type="button" class="tbv-btn" onclick="openModalVenta()">' + ic('mas', 18) + 'Nueva venta</button></div></header>' +
      '<div class="tbv-g2"><section class="tbv-col"><h2 class="tbv-h2">Por resolver hoy</h2><div class="tbv-card" style="flex:1">' +
      (pend.length ? '<ul class="tbv-list">' + pend.map(x => '<li>' + ic(x.i) + '<p>' + x.t + '<small>' + e(x.s) + '</small></p>' + x.a + '</li>').join('') + '</ul>'
                   : '<p class="tbv-vacio">' + ic('ok') + ' Nada pendiente: el stock está por encima del mínimo y no hay vencimientos ni cobros.</p>') +
      '</div></section><section class="tbv-col"><h2 class="tbv-h2">' + (vHoy.length ? 'Ventas de hoy' : 'Últimas ventas') + '</h2><div class="tbv-card" style="flex:1;padding:8px 0 20px">' +
      (ultimas.length ? ultimas.map(v => '<div class="tbv-sale"><time class="tbv-num">' + (vHoy.length ? horaVenta(v) : cuando(v.fecha)) + '</time><div><div class="p" title="' + e(resumenProductos(v)) + '">' + e(resumenCorto(v)) + '</div><div class="tbv-sub">' + (productosVenta(v) > 1 ? productosVenta(v) + ' productos · ' : '') + e(v.metodoPago || 'Efectivo') + (esCredito(v) && v.cliente ? ' · ' + e(v.cliente) : '') + '</div></div><b class="tbv-num">' + pesos(v.total) + '</b></div>').join('') +
        '<div style="padding:8px 28px 0">' + link('Ver ' + (vHoy.length === 1 ? 'la venta de hoy en Ventas' : vHoy.length ? 'las ' + vHoy.length + ' ventas de hoy' : 'todas las ventas'), "showSection('ventas')") + '</div>'
        : '<p class="tbv-vacio">Aún no hay ventas registradas.</p>') +
      '</div></section></div>' +
      (pedidos.length ? '<section><h2 class="tbv-h2">Pedidos por proveedor</h2><div class="tbv-g3">' + pedidos.map(g => '<article class="tbv-card go tbv-pad tbv-col"><h3 class="tbv-h3">' + e(g.nombre) + '</h3><p class="tbv-sub" style="margin:2px 0 18px">' + (g.id ? g.items.length + (g.items.length === 1 ? ' producto' : ' productos') + ' por pedir' : 'Asígneles un proveedor para armar el pedido') + '</p><dl class="tbv-kv tbv-num">' + g.items.slice(0, 5).map(x => '<dt>' + e(x.p.nombre) + '</dt><dd>' + x.cant + '</dd>').join('') + '</dl><div class="tbv-foot">' + link('Copiar lista', "tbvCopiarPedido('" + g.id + "')") + link('Registrar entrada', "registrarEntradaDe('" + g.items[0].p.id + "')") + '</div></article>').join('') + '</div></section>' : '');
    box.querySelectorAll('[data-tbv-contar]').forEach(contar);
  }
  function contar(el) {
    if (typeof TB_SIN_MOVIMIENTO !== 'undefined' && TB_SIN_MOVIMIENTO) return;
    const fin = +el.dataset.tbvContar, t0 = performance.now(), dur = 700;
    setTimeout(() => { el.textContent = pesos(fin); }, dur + 80);
    (function paso(t) { const k = Math.min(1, (t - t0) / dur), q = 1 - Math.pow(1 - k, 3); el.textContent = pesos(fin * q); if (k < 1) requestAnimationFrame(paso); })(t0);
  }
  window.tbvVerProducto = function (id) {
    const p = state.productos.find(x => x.id === id);
    showSection('productos');
    const s = document.getElementById('header-search');
    if (p && s) { s.value = p.nombre; filtrarProductos(); }
  };
  window.tbvCopiarPedido = function (id) {
    const g = pedidosPorProveedor().find(x => x.id === id);
    if (!g) return;
    const texto = 'Pedido de ' + (state.perfilActivo ? state.perfilActivo.nombre : 'TBalance') + (g.id ? ' para ' + g.nombre : '') + ':\n' + g.items.map(x => '- ' + x.cant + ' × ' + x.p.nombre).join('\n');
    const ok = () => showToast('Lista copiada: péguela en WhatsApp o en un correo');
    try { navigator.clipboard.writeText(texto).then(ok, () => { prompt('Copie la lista:', texto); }); } catch (err) { prompt('Copie la lista:', texto); }
  };

  // ── Ventas: cuaderno de caja ──
  let diasVisibles = 7;
  function renderCuaderno() {
    const sec = document.getElementById('sec-ventas');
    if (!sec) return;
    let box = document.getElementById('tbv-cuaderno');
    if (!box) { box = document.createElement('div'); box.id = 'tbv-cuaderno'; sec.appendChild(box); }
    const h1 = sec.querySelector('h1'), sub = h1 && h1.nextElementSibling;
    if (sub) sub.textContent = 'El cuaderno de caja: cada venta con su forma de pago y el total de cada día.';
    if (!state.ventas.length) { box.innerHTML = '<div class="tbv-card"><p class="tbv-vacio">No hay ventas registradas. Use «Nueva venta» o «Venta por QR» arriba.</p></div>'; return; }
    const dias = {};
    state.ventas.forEach(v => { const f = String(v.fecha || '').slice(0, 10); (dias[f] = dias[f] || []).push(v); });
    const claves = Object.keys(dias).sort().reverse();
    const hoy = hoy0();
    box.innerHTML = claves.slice(0, diasVisibles).map(f => {
      const vs = dias[f].sort((a, b) => msVenta(b) - msVenta(a));
      const suma = fn => vs.filter(fn).reduce((t, v) => t + (v.total || 0), 0);
      const d = desdeIso(f), n = diasEntre(d, hoy);
      const nombre = (n === 0 ? 'Hoy, ' : n === 1 ? 'Ayer, ' : '') + DIAS[d.getDay()] + ' ' + d.getDate() + (n > 6 ? ' de ' + MESES[d.getMonth()] : '');
      const cred = suma(esCredito);
      return '<section><div class="tbv-dia"><h2 class="tbv-h2" style="margin:0">' + nombre.charAt(0).toUpperCase() + nombre.slice(1) + '</h2><span class="tot tbv-num">' + vs.length + (vs.length === 1 ? ' venta' : ' ventas') +
        ' · Efectivo <b>' + pesos(suma(v => !esCredito(v) && !esDigital(v))) + '</b> · Digital <b>' + pesos(suma(esDigital)) + '</b>' + (cred ? ' · ' + (palabraCredito() === 'fiado' ? 'Fiado' : 'A crédito') + ' <b>' + pesos(cred) + '</b>' : '') + ' · Total <b>' + pesos(suma(() => true)) + '</b></span></div>' +
        '<div class="tbv-card">' + vs.map(v => '<div class="tbv-venta"><time class="tbv-num">' + (horaVenta(v) || '—') + '</time><span class="p">' + e(resumenProductos(v)) + '</span><span class="pago">' + e(v.metodoPago || 'Efectivo') + (v.referenciaPago ? ' · ' + e(v.referenciaPago) : '') + (esCredito(v) && v.cliente ? ' · ' + e(v.cliente) : '') + '</span><b class="tbv-num">' + pesos(v.total) + '</b>' + link('Anular', "confirmarEliminar('" + v.id + "','ventas')") + '</div>').join('') + '</div></section>';
    }).join('') + (claves.length > diasVisibles ? '<div>' + link('Ver ' + Math.min(7, claves.length - diasVisibles) + ' días más', 'tbvMasDias()') + '</div>' : '');
  }
  window.tbvMasDias = function () { diasVisibles += 7; renderCuaderno(); };

  // ── Productos: frase principal ──
  function fraseProductos() {
    const h1 = document.querySelector('#sec-productos h1');
    if (!h1) return;
    const ps = state.productos, hoy = hoy0();
    const agot = ps.filter(p => (p.stock || 0) === 0).length, bajos = ps.filter(p => p.stock > 0 && p.stock <= (p.stockMin || 0)).length;
    const venc = ps.filter(p => p.fechaVencimiento && p.stock > 0 && diasEntre(hoy, desdeIso(p.fechaVencimiento)) <= 7).length, sinC = ps.filter(p => !(p.precioCompra > 0)).length;
    const n = new Set(ps.filter(p => (p.stock || 0) <= (p.stockMin || 0) || !(p.precioCompra > 0) || (p.fechaVencimiento && p.stock > 0 && diasEntre(hoy, desdeIso(p.fechaVencimiento)) <= 7)).map(p => p.id)).size;
    h1.classList.add('tbv-frase');
    h1.textContent = ps.length + (ps.length === 1 ? ' producto' : ' productos') + (n ? ', ' + n + (n === 1 ? ' pide' : ' piden') + ' atención' : '');
    const partes = [];
    if (venc) partes.push(venc === 1 ? 'uno se vence esta semana' : venc + ' se vencen esta semana');
    if (agot) partes.push(agot === 1 ? 'uno se agotó' : agot + ' se agotaron');
    if (bajos) partes.push(bajos === 1 ? 'uno está por agotarse' : bajos + ' están por agotarse');
    if (sinC) partes.push(sinC === 1 ? 'a uno le falta el precio de compra' : 'a ' + sinC + ' les falta el precio de compra');
    let sub = h1.parentElement.querySelector('.tbv-sub-frase');
    if (!sub) { sub = document.createElement('p'); sub.className = 'tbv-sub-frase'; sub.style.cssText = 'font-size:18px;line-height:28px;color:var(--c-text);margin:10px 0 0'; h1.after(sub); }
    sub.textContent = partes.length ? partes.join(', ').replace(/^./, c => c.toUpperCase()).replace(/, ([^,]*)$/, ' y $1') + '.' : 'Todo el inventario está en orden.';
  }

  // ── Informes: frase principal ──
  function fraseInforme() {
    const h1 = document.querySelector('#sec-informes h1');
    if (!h1 || typeof informe === 'undefined' || !informe.datos) return;
    const d = informe.datos, a = d.actual, b = d.anterior;
    h1.classList.add('tbv-frase');
    h1.innerHTML = a.numeroVentas ? 'Van <span class="tbv-num">' + pesos(a.ventas) + '</span> en ' + a.numeroVentas + (a.numeroVentas === 1 ? ' venta' : ' ventas') + '.' : 'No hay ventas en este período.';
    const p = document.getElementById('inf-etiqueta');
    if (p && a.numeroVentas) {
      const pct = b && b.ventas > 0 ? Math.round((a.ventas - b.ventas) / b.ventas * 1000) / 10 : null;
      p.textContent = 'Ticket promedio de ' + pesos(a.ticketPromedio) + (pct === null ? '' : ', ' + String(Math.abs(pct)).replace('.', ',') + ' % ' + (pct >= 0 ? 'más' : 'menos') + ' que el período anterior') + '. La ganancia real es de ' + pesos(a.utilidad) + (a.ventas > 0 ? ', un margen del ' + String(Math.round(a.utilidad / a.ventas * 1000) / 10).replace('.', ',') + ' %' : '') + '.';
      p.style.cssText = 'font-size:18px;line-height:28px;color:var(--c-text);margin-top:10px';
    }
  }

  // ── Selector de color del perfil: solo los acentos con contraste revisado ──
  function selectorColores() {
    const primero = document.querySelector('.color-opt');
    if (!primero || primero.dataset.tbv) return;
    const caja = primero.parentElement;
    caja.innerHTML = Object.entries(ACENTOS).map(([k, a]) => '<button type="button" data-tbv="1" data-color="' + a.acc + '" title="' + a.nombre + '" class="color-opt w-9 h-9 rounded-xl ring-0" style="background:' + a.acc + '"></button>').join('');
  }

  // ── Enganches sobre las funciones de la v3.4.0 ──
  function envolver(nombre, despues) {
    const original = window[nombre];
    if (typeof original !== 'function') return;
    window[nombre] = function () { const r = original.apply(this, arguments); try { despues.apply(this, arguments); } catch (err) { console.error('[tbalance]', nombre, err); } return r; };
  }
  // Pinta tarjetas y menú de perfiles con el acento resuelto, sin tocar lo guardado
  function conAcentos(nombre) {
    const original = window[nombre];
    if (typeof original !== 'function') return;
    window[nombre] = function () {
      const lista = (state.perfiles || []).concat(state.perfilActivo ? [state.perfilActivo] : []);
      const previos = lista.map(pf => pf.color);
      lista.forEach(pf => { try { pf.color = acentoDe(pf).acc; } catch (err) { } });
      try { return original.apply(this, arguments); } finally { lista.forEach((pf, i) => { pf.color = previos[i]; }); }
    };
  }
  conAcentos('renderTarjetasPerfiles'); conAcentos('renderMenuPerfiles');
  envolver('actualizarDashboard', () => { renderHoy(); fraseProductos(); });
  envolver('renderTablaVentas', renderCuaderno);
  envolver('renderTablaProductos', fraseProductos);
  envolver('renderSelectorPerfilActivo', aplicarAcento);
  const totalCero = () => ['ven-total-display', 'vqr-total'].forEach(id => { const el = document.getElementById(id); if (el && /^\$\s?0([.,]00)?$/.test(el.textContent.trim())) el.textContent = pesos(0); });
  envolver('openModalVenta', function () { totalCero(); const p = document.getElementById('ven-precio'); if (p) p.placeholder = '0'; });
  envolver('calcularTotalVenta', function () { const el = document.getElementById('ven-total-display'); if (el) el.textContent = el.textContent.replace(/^\$\s?/, '$ '); });
  envolver('openModalVentaQR', totalCero);
  envolver('renderVentaQR', function () { const el = document.getElementById('vqr-total'); if (el) el.textContent = el.textContent.replace(/^\$\s?/, '$ '); });
  envolver('aplicarTema', function (tema) { aplicarAcento(); const l = document.getElementById('theme-label'); if (l) l.textContent = 'Modo oscuro';
    const osc = document.documentElement.classList.contains('dark');
    const d = document.getElementById('theme-toggle-dot'), t = document.getElementById('theme-toggle');
    if (d) { d.classList.remove('translate-x-1', 'translate-x-6'); d.classList.add(osc ? 'translate-x-6' : 'translate-x-1'); }
    if (t) { t.classList.remove('bg-brand-500', 'bg-gray-600'); t.classList.add(osc ? 'bg-brand-500' : 'bg-gray-600'); }
    if (t) t.setAttribute('aria-checked', osc ? 'true' : 'false'); });
  envolver('dibujarInforme', fraseInforme);
  envolver('onTipoNegocioChange', function () {
    if (document.getElementById('perfil-id').value) return;          // al editar se respeta el color elegido
    const a = ACENTOS[document.getElementById('perfil-tipo').value] || ACENTOS.NINGUNO;
    colorSeleccionado = a.acc; actualizarColoresUI();
  });
  envolver('openModalPerfil', function (perfil) { if (perfil && !porColor(perfil.color)) { colorSeleccionado = acentoDe(perfil).acc; actualizarColoresUI(); } });

  // ── Textos del menú ──
  function textosMenu() {
    const nombres = { dashboard: 'Hoy', perfiles: 'Mis negocios', respaldos: 'Copias de seguridad', categorias: 'Categorías' };
    document.querySelectorAll('#sidebar .nav-item').forEach(a => {
      const n = nombres[a.dataset.section];
      if (!n) return;
      const nodo = [...a.childNodes].reverse().find(x => x.nodeType === 3 && x.textContent.trim());
      if (nodo) nodo.textContent = ' ' + n; else { const s = a.querySelector('span'); if (s) s.textContent = n; }
    });
    const per = document.querySelector('#sec-perfiles h1'); if (per) per.textContent = 'Mis negocios';
  }

  function iniciar() {
    textosMenu(); selectorColores(); aplicarAcento();
    try { renderHoy(); renderCuaderno(); fraseProductos(); } catch (err) { console.error('[tbalance]', err); }
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', iniciar); else iniciar();
})();

/*
 * Vidrio en las ventanas: el desenfoque cuesta en PCs sin buena tarjeta gráfica.
 * La primera ventana que se abre se mide; si el PC no alcanza a moverla con suavidad,
 * se apaga el desenfoque en ese PC (queda el vidrio con un velo de color, sin desenfocar) y se recuerda.
 */
(function () {
  'use strict';
  const CLAVE = 'tb-vidrio';
  let decidido = null;
  try { decidido = localStorage.getItem(CLAVE); } catch (e) { decidido = null; }
  if (decidido === 'no') document.documentElement.classList.add('tb-sin-vidrio');
  window.tbMedirVidrio = function () {
    if (decidido || !window.requestAnimationFrame) return;
    decidido = 'midiendo';
    const cuadros = []; let antes = performance.now(); const inicio = antes;
    requestAnimationFrame(function paso(ahora) {
      cuadros.push(ahora - antes); antes = ahora;
      if (ahora - inicio < 400) { requestAnimationFrame(paso); return; }
      const utiles = cuadros.slice(1);
      const promedio = utiles.reduce((a, b) => a + b, 0) / Math.max(1, utiles.length);
      const lento = promedio > 45 || Math.max.apply(null, utiles.concat([0])) > 140;
      decidido = lento ? 'no' : 'si';
      if (lento) document.documentElement.classList.add('tb-sin-vidrio');
      try { localStorage.setItem(CLAVE, decidido); } catch (e) { /* sin almacenamiento: se vuelve a medir la próxima vez */ }
    });
  };
})();

/*
 * Figuras del inicio de sesión: flotan despacio y siguen el mouse con profundidad (las de adelante se
 * mueven más) y el brillo de las esferas mira hacia el puntero. Solo se mueven con transform, se detienen
 * al entrar a la app o si la pestaña no se ve, y se quedan quietas si el PC no alcanza o si la persona
 * pidió reducir el movimiento en su sistema.
 */
(function () {
  'use strict';
  const CLAVE = 'tb-arte';
  // profundidad (cuánto sigue al mouse), amplitud y período del flote, y desfase de cada figura
  const FIG = { c1: [0.35, 7, 9.0, 0.0], c2: [0.6, 9, 7.5, 1.7], o1: [1.0, 11, 6.2, 3.1], o2: [0.8, 8, 8.4, 4.6], o3: [1.25, 6, 5.4, 2.2] };
  let corriendo = false, quieto = false, medido = false, objetivo = { x: 0, y: 0 }, actual = { x: 0, y: 0 }, cuadros = [], antes = 0, inicio = 0;
  try { quieto = localStorage.getItem(CLAVE) === 'quieto' || localStorage.getItem('tb-vidrio') === 'no'; } catch (e) { quieto = false; }
  const reducir = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)');

  function alMover(e) {
    const p = e.touches ? e.touches[0] : e;
    objetivo.x = (p.clientX / window.innerWidth) * 2 - 1;
    objetivo.y = (p.clientY / window.innerHeight) * 2 - 1;
  }
  function alSalir() { objetivo.x = 0; objetivo.y = 0; }

  function cuadro(ahora) {
    const login = document.getElementById('tb-login'), arte = login && login.querySelector('.tb-lg-arte');
    if (!arte || login.hidden || document.hidden || quieto) { detener(arte); return; }
    // Medición del primer segundo y medio: si va a menos de ~25 cuadros por segundo, se queda quieto en este PC
    if (!medido) {
      if (antes) cuadros.push(ahora - antes);
      antes = ahora;
      if (ahora - inicio > 1500) {
        medido = true;
        const util = cuadros.slice(5), prom = util.reduce((a, b) => a + b, 0) / Math.max(1, util.length);
        if (prom > 40) { quieto = true; try { localStorage.setItem(CLAVE, 'quieto'); } catch (e) { /* sin almacenamiento */ } detener(arte); return; }
      }
    }
    actual.x += (objetivo.x - actual.x) * 0.06;
    actual.y += (objetivo.y - actual.y) * 0.06;
    const t = ahora / 1000;
    for (const n in FIG) {
      const el = arte.querySelector('.' + n); if (!el) continue;
      const [prof, amp, per, fase] = FIG[n];
      const fx = Math.sin((t / per) * 2 * Math.PI + fase) * amp * 0.6;
      const fy = Math.cos((t / per) * 2 * Math.PI + fase) * amp;
      const x = fx + actual.x * 22 * prof, y = fy + actual.y * 16 * prof;
      const giro = n === 'c2' ? actual.x * 5 + Math.sin(t / per * 2 * Math.PI) * 1.5 : 0;
      el.style.transform = 'translate3d(' + x.toFixed(2) + 'px,' + y.toFixed(2) + 'px,0)' + (giro ? ' rotate(' + giro.toFixed(2) + 'deg)' : '');
      if (n === 'o1' || n === 'o2') {
        el.style.setProperty('--lx', (32 + actual.x * 14).toFixed(1) + '%');
        el.style.setProperty('--ly', (28 + actual.y * 14).toFixed(1) + '%');
      }
    }
    requestAnimationFrame(cuadro);
  }

  function detener(arte) {
    corriendo = false;
    window.removeEventListener('pointermove', alMover);
    document.removeEventListener('pointerleave', alSalir);
    if (arte) arte.classList.remove('vivo');
  }

  window.tbArteIniciar = function () {
    if (corriendo || quieto || (reducir && reducir.matches) || !window.requestAnimationFrame) return;
    const arte = document.querySelector('#tb-login .tb-lg-arte');
    if (!arte) return;
    corriendo = true; antes = 0; cuadros = []; inicio = performance.now();
    arte.classList.add('vivo');
    window.addEventListener('pointermove', alMover, { passive: true });
    document.addEventListener('pointerleave', alSalir);
    requestAnimationFrame(cuadro);
  };
  document.addEventListener('visibilitychange', function () {
    const login = document.getElementById('tb-login');
    if (!document.hidden && login && !login.hidden) window.tbArteIniciar();
  });
})();

/*
 * Buscador de productos en las ventanas de venta, entrada y salida: se escribe parte del nombre o del
 * código y la lista se reduce; Enter elige el primero. En entradas y salidas también se filtra por categoría.
 */
(function () {
  'use strict';
  const sinTildes = t => String(t || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

  function opciones(select, lista, elegido) {
    select.innerHTML = '<option value="">' + (lista.length ? 'Seleccionar producto...' : 'Ningún producto coincide') + '</option>' +
      lista.map(p => '<option value="' + esc(p.id) + '">' + esc(p.nombre) + ' (stock: ' + p.stock + ')</option>').join('');
    if (elegido && lista.some(p => p.id === elegido)) select.value = elegido;
  }

  window.tbFiltroProductos = function (prefijo, conCategoria) {
    const select = document.getElementById(prefijo + '-producto');
    if (!select) return;
    let caja = document.getElementById(prefijo + '-filtro');
    if (!caja) {
      caja = document.createElement('div');
      caja.id = prefijo + '-filtro';
      caja.className = 'tb-filtro-prod' + (conCategoria ? ' con-cat' : '');
      caja.innerHTML = '<input type="search" autocomplete="off" placeholder="Escriba para buscar por nombre o código" aria-label="Buscar producto">'
        + (conCategoria ? '<select aria-label="Filtrar por categoría"></select>' : '');
      select.parentNode.insertBefore(caja, select);
    }
    const texto = caja.querySelector('input'), categoria = caja.querySelector('select');
    if (categoria) {
      const cats = (Array.isArray(state.categorias) ? state.categorias : []).filter(c => c.activo !== false);
      categoria.innerHTML = '<option value="">Todas las categorías</option>' + cats.map(c => '<option value="' + esc(c.id) + '">' + esc(c.nombre) + '</option>').join('');
    }
    texto.value = '';
    const filtrar = () => {
      const q = sinTildes(texto.value.trim()), cat = categoria ? categoria.value : '';
      const lista = (state.productos || []).filter(p =>
        (!cat || p.categoriaId === cat) &&
        (!q || sinTildes(p.nombre).includes(q) || sinTildes(p.codigo).includes(q) || sinTildes(p.id).includes(q)));
      const antes = select.value;
      opciones(select, lista, antes);
      if (q && lista.length === 1 && select.value !== lista[0].id) { select.value = lista[0].id; select.dispatchEvent(new Event('change')); }
      return lista;
    };
    texto.oninput = filtrar;
    if (categoria) categoria.onchange = filtrar;
    texto.onkeydown = e => {
      if (e.key !== 'Enter') return;
      e.preventDefault();
      const lista = filtrar();
      if (lista.length) {
        select.value = lista[0].id; select.dispatchEvent(new Event('change'));
        const cantidad = document.getElementById(prefijo + '-cantidad'); if (cantidad) cantidad.focus();
      }
    };
    filtrar();
    setTimeout(() => { try { texto.focus(); } catch (e) { /* ventana cerrada */ } }, 80);
  };
})();
