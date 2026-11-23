// TBalance - Fiados: quién le debe al negocio, cuánto, y los abonos con que va pagando.
// La cuenta la calcula Java (CreditService): ventas fiadas vigentes menos abonos vigentes.
(function () {
  'use strict';

  const $f = id => document.getElementById(id);
  const escF = t => String(t == null ? '' : t).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const pesosF = n => '$ ' + Math.round(Number(n) || 0).toLocaleString('es-CO');
  const MESES_F = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
  const puedeF = p => (typeof window.tbPuede === 'function' ? window.tbPuede(p) : true);

  let cuentas = [];
  let abierta = null;

  function fechaCorta(iso) {
    if (!iso) return '';
    const d = String(iso).substring(0, 10).split('-');
    const hora = String(iso).length > 11 ? ' · ' + String(iso).substring(11, 16) : '';
    return Number(d[2]) + ' ' + MESES_F[Number(d[1]) - 1] + ' ' + d[0] + hora;
  }

  function hace(iso) {
    if (!iso) return '';
    const dias = Math.floor((Date.now() - new Date(String(iso).substring(0, 10) + 'T00:00:00').getTime()) / 86400000);
    return dias <= 0 ? 'hoy' : dias === 1 ? 'ayer' : 'hace ' + dias + ' días';
  }

  function leer(raw) {
    if (typeof raw === 'string' && raw.startsWith('ERROR')) return { error: raw.replace(/^ERROR:\s*/, '') };
    try { return { datos: JSON.parse(raw) }; } catch (e) { return { error: 'No se pudo leer la respuesta.' }; }
  }

  // ── Lista de clientes ───────────────────────────────────────
  window.tbFiadosCargar = function () {
    if (!window.javaBridge || !window.javaBridge.listarFiados) return;
    const r = leer(window.javaBridge.listarFiados());
    if (r.error) { $f('fia-lista').innerHTML = '<p class="tb-his-vacio">' + escF(r.error) + '</p>'; return; }
    cuentas = r.datos || [];
    const negocio = (typeof state !== 'undefined' && state.perfilActivo && state.perfilActivo.nombre) || '';
    $f('fia-negocio').textContent = negocio;
    const deben = cuentas.filter(c => c.saldo >= 0.5);
    const total = deben.reduce((t, c) => t + c.saldo, 0);
    $f('fia-titulo').textContent = deben.length ? 'Por cobrar: ' + pesosF(total) + '.' : 'Nadie le debe al negocio.';
    $f('fia-lead').textContent = deben.length
      ? (deben.length === 1 ? 'Un cliente debe.' : deben.length + ' clientes deben.') + ' Abra un cliente para ver lo que compró y registrar un abono.'
      : 'Cuando venda fiado, el cliente aparece aquí con lo que debe.';
    tbFiadosPintar();
  };

  window.tbFiadosPintar = function () {
    const q = ($f('fia-buscar').value || '').trim().toLowerCase();
    const todos = $f('fia-todos').checked;
    const lista = cuentas.filter(c => (todos || c.saldo >= 0.5)
      && (!q || String(c.cliente || '').toLowerCase().includes(q) || String(c.documento || '').includes(q)));
    if (!lista.length) {
      $f('fia-lista').innerHTML = '<p class="tb-his-vacio">' + (cuentas.length ? 'No hay clientes con ese filtro.' : 'Todavía no hay ventas fiadas en este negocio.') + '</p>';
      return;
    }
    $f('fia-lista').innerHTML = lista.map(function (c, i) {
      const alDia = c.saldo < 0.5;
      return '<button type="button" class="tb-fia-fila' + (alDia ? ' al-dia' : '') + '" onclick="tbFiadoAbrir(' + cuentas.indexOf(c) + ')">'
        + '<span class="q"><b>' + escF(c.cliente || 'Sin nombre') + '</b><em>' + (c.documento ? 'C.C. ' + escF(c.documento) : 'Sin cédula') + '</em></span>'
        + '<span class="d">Fiado ' + pesosF(c.totalFiado) + ' · Abonado ' + pesosF(c.totalAbonado) + '<br>Último movimiento ' + escF(hace(c.ultimaFecha)) + '</span>'
        + '<span class="s">' + (alDia ? 'Al día' : pesosF(c.saldo)) + '</span></button>';
    }).join('');
  };

  // ── Cuenta de un cliente ────────────────────────────────────
  window.tbFiadoAbrir = function (indice) {
    const c = cuentas[indice];
    if (!c) return;
    abrirClave(c.clave, true);
  };

  function abrirClave(clave, limpiar) {
    const r = leer(window.javaBridge.obtenerFiado(clave));
    if (r.error) { if (typeof showToast === 'function') showToast(r.error, 'error'); return; }
    abierta = r.datos;
    const c = abierta;
    $f('fia-m-nombre').textContent = c.cliente || 'Sin nombre';
    $f('fia-m-doc').textContent = c.documento ? 'C.C. ' + c.documento : 'Venta fiada sin cédula';
    $f('fia-m-saldos').innerHTML =
      '<div><span>Debe</span><b>' + (c.saldo < 0.5 ? 'Nada' : pesosF(c.saldo)) + '</b></div>'
      + '<div><span>Se le fió</span><b>' + pesosF(c.totalFiado) + '</b></div>'
      + '<div><span>Ha abonado</span><b>' + pesosF(c.totalAbonado) + '</b></div>';
    $f('fia-m-form').classList.toggle('hidden', c.saldo < 0.5);
    if (limpiar) {
      $f('fia-monto').value = '';
      $f('fia-ref').value = '';
      $f('fia-nota').value = '';
      llenarMetodos();
    }
    $f('fia-error').classList.add('hidden');
    pintarMovimientos();
    if ($f('modal-fiado').classList.contains('hidden')) openModal('modal-fiado');
    if (limpiar && c.saldo >= 0.5) setTimeout(function () { $f('fia-monto').focus(); }, 60);
  }

  function pintarMovimientos() {
    const c = abierta;
    const movs = [];
    (c.ventas || []).forEach(v => movs.push({ fecha: v.fechaRegistro || v.fecha, tipo: 'venta', v: v }));
    (c.abonos || []).forEach(a => movs.push({ fecha: a.fecha, tipo: 'abono', a: a }));
    movs.sort((x, y) => String(y.fecha).localeCompare(String(x.fecha)));
    const anular = puedeF('ANULAR');
    $f('fia-m-movs').innerHTML = movs.map(function (m) {
      if (m.tipo === 'venta') {
        const que = (m.v.items || []).map(i => i.cantidad + ' × ' + (i.nombreProducto || 'producto')).join(', ');
        return '<div class="tb-fia-mov venta"><span class="f">' + escF(fechaCorta(m.fecha)) + '</span>'
          + '<span class="t"><b>Compró fiado</b><em>' + escF(que) + (m.v.vendedor ? ' · vendió ' + escF(m.v.vendedor) : '') + '</em></span>'
          + '<span class="v">+ ' + pesosF(m.v.total) + '</span></div>';
      }
      const a = m.a;
      const detalle = a.metodoPago + (a.referenciaPago ? ' ' + a.referenciaPago : '') + (a.registradoPor ? ' · recibió ' + a.registradoPor : '') + (a.nota ? ' · ' + a.nota : '');
      return '<div class="tb-fia-mov abono"><span class="f">' + escF(fechaCorta(m.fecha)) + '</span>'
        + '<span class="t"><b>Abonó</b><em>' + escF(detalle) + '</em>'
        + (anular ? '<button type="button" onclick="tbFiadoAnular(\'' + escF(a.id) + '\')">Anular</button>' : '') + '</span>'
        + '<span class="v">− ' + pesosF(a.monto) + '</span></div>';
    }).join('') || '<p class="tb-his-vacio">Sin movimientos.</p>';
  }

  function llenarMetodos() {
    const todos = typeof tbMediosDelNegocio === 'function'
      ? tbMediosDelNegocio(typeof state !== 'undefined' ? state.perfilActivo : null)
      : ['Efectivo', 'Nequi', 'Daviplata', 'Bre-B', 'Transferencia Bancolombia', 'Tarjeta débito', 'Tarjeta crédito'];
    $f('fia-metodo').innerHTML = todos.filter(m => m !== 'Fiado').map(m => '<option>' + escF(m) + '</option>').join('');
    tbFiadoMetodo();
  }

  window.tbFiadoMetodo = function () {
    const m = $f('fia-metodo').value;
    const digital = m && m !== 'Efectivo';
    $f('fia-ref-campo').classList.toggle('hidden', !digital);
    $f('fia-ref').placeholder = m.indexOf('Tarjeta') === 0 ? 'N.º de voucher (opcional)' : 'N.º de comprobante (opcional)';
  };

  window.tbFiadoTodo = function () {
    if (abierta) $f('fia-monto').value = Math.round(abierta.saldo);
    $f('fia-monto').focus();
  };

  window.tbFiadoAbonar = function () {
    if (!abierta) return;
    const monto = Number($f('fia-monto').value);
    const err = $f('fia-error');
    if (!(monto > 0)) { err.textContent = 'Escriba cuánto abona el cliente.'; err.classList.remove('hidden'); return; }
    const abono = { clienteDocumento: abierta.clave, monto: monto, metodoPago: $f('fia-metodo').value,
      referenciaPago: $f('fia-ref').value.trim(), nota: $f('fia-nota').value.trim() };
    const r = leer(window.javaBridge.registrarAbono(JSON.stringify(abono)));
    if (r.error) { err.textContent = r.error; err.classList.remove('hidden'); return; }
    const nombre = abierta.cliente || 'El cliente';
    abrirClave(abierta.clave, true);
    window.tbFiadosCargar();
    if (typeof showToast === 'function') {
      showToast(abierta.saldo < 0.5 ? nombre + ' quedó al día.' : 'Abono de ' + pesosF(monto) + ' registrado. ' + nombre + ' debe ' + pesosF(abierta.saldo) + '.', 'success');
    }
  };

  // Anular un abono: usa la misma ventana de confirmación que las ventas, con motivo
  window.tbFiadoAnular = function (id) {
    const motivo = $f('confirm-motivo');
    const error = $f('confirm-error');
    error.classList.add('hidden');
    motivo.value = '';
    motivo.classList.remove('hidden');
    $f('confirm-title').textContent = '¿Anular el abono?';
    $f('confirm-msg').textContent = 'Lo que debe el cliente vuelve a subir. Queda en el historial como anulado.';
    const btn = $f('confirm-btn');
    btn.textContent = 'Anular';
    btn.onclick = function () {
      const texto = motivo.value.trim();
      if (!texto) { error.textContent = 'Escriba el motivo de la anulación'; error.classList.remove('hidden'); return; }
      const r = window.javaBridge.anularAbono(id, texto);
      if (typeof r === 'string' && r.startsWith('ERROR')) { error.textContent = r.replace(/^ERROR:\s*/, ''); error.classList.remove('hidden'); return; }
      closeModal('modal-confirm');
      if (abierta) abrirClave(abierta.clave, false);
      window.tbFiadosCargar();
      if (typeof showToast === 'function') showToast('Abono anulado. Queda en el historial.', 'success');
    };
    openModal('modal-confirm');
    setTimeout(function () { motivo.focus(); }, 50);
  };

  // Lo que deben los clientes, para la pantalla Hoy (null si no se puede consultar)
  window.tbFiadosResumen = function () {
    if (!window.javaBridge || !puedeF('VENDER')) return null;
    const r = leer(window.javaBridge.resumenFiados());
    return r.error ? null : (r.datos || []).filter(c => c.saldo >= 0.5);
  };

  // Abrir la sección carga la lista
  const showAntes = window.showSection;
  window.showSection = function (sec) {
    const r = showAntes.apply(this, arguments);
    if (sec === 'fiados') window.tbFiadosCargar();
    return r;
  };
})();
