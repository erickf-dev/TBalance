/*
 * TBalance — inicio de sesión, roles y usuarios.
 *
 * Java decide quién puede hacer qué (AccessPolicy). Esta parte de la página:
 *  - no carga ningún dato hasta que alguien inicia sesión (tbArrancar),
 *  - oculta lo que el rol no puede usar (para no mostrar botones que darían error),
 *  - cierra la sesión tras un rato sin uso,
 *  - muestra la sección Usuarios y sus ventanas.
 */
(function () {
  'use strict';

  const TB = { sesion: null, primeraVez: false, minutos: 30, ultimoUso: Date.now(), avisoInactividad: null, usuarios: [] };
  window.tbSesion = function () { return TB.sesion; };

  const $ = id => document.getElementById(id);
  const esc = t => String(t == null ? '' : t).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const puede = p => !!(TB.sesion && (TB.sesion.permisos || []).indexOf(p) >= 0);
  window.tbPuede = puede;

  function llamar(metodo) {
    const args = Array.prototype.slice.call(arguments, 1);
    try { return window.javaBridge[metodo].apply(null, args); }
    catch (e) { return 'ERROR: ' + (e && e.message ? e.message : e); }
  }
  const esError = r => typeof r === 'string' && r.startsWith('ERROR');
  const textoError = r => String(r).replace(/^ERROR:\s*/, '');

  // ── Arranque ───────────────────────────────────────────────
  // Lo llama el DOMContentLoaded de la página (Edge) y WebViewMain (JavaFX).
  window.tbArrancar = function () {
    envolverPuente();     // en JavaFX el puente aparece después de cargar este archivo
    const estado = leerEstado();
    if (!estado) return;
    if (TB.sesion) { entrarALaApp(); } else { mostrarLogin(); }
  };

  function leerEstado() {
    const r = llamar('estadoSesion');
    if (esError(r)) { console.error('[TBalance] sesión:', r); return null; }
    const e = JSON.parse(r);
    TB.primeraVez = !!e.primeraVez;
    TB.sesion = e.sesion || null;
    TB.minutos = e.minutosInactividad || 30;
    return e;
  }

  // Mientras se ve el inicio de sesión, la app de atrás no recibe clics ni Tab
  function bloquearApp(si) {
    const app = document.querySelector('body > .flex.h-screen');
    if (app) { if (si) app.setAttribute('inert', ''); else app.removeAttribute('inert'); }
  }

  function entrarALaApp() {
    bloquearApp(false);
    $('tb-login').hidden = true;
    document.body.classList.remove('tb-sin-sesion');
    TB.ultimoUso = Date.now();
    aplicarPermisos();
    if (typeof cargarTiposNegocio === 'function' && (typeof tiposNegocioCache === 'undefined' || !tiposNegocioCache.length)) cargarTiposNegocio();
    cargarEstadoDesdeJava();
  }

  // ── Pantalla de inicio de sesión ───────────────────────────
  function mostrarLogin(mensaje) {
    document.body.classList.add('tb-sin-sesion');
    document.querySelectorAll('.fixed:not(.hidden)').forEach(m => { if (m.id && m.id.startsWith('modal')) m.classList.add('hidden'); });
    ['tb-dlg-usuario', 'tb-dlg-clave'].forEach(id => { const d = $(id); if (d) d.hidden = true; });
    const login = $('tb-login');
    ponerSaludo();
    if (window.tbArteIniciar) tbArteIniciar();
    bloquearApp(true);
    login.hidden = false;
    $('tb-login-entrar').hidden = TB.primeraVez;
    $('tb-login-crear').hidden = !TB.primeraVez;
    if ($('tb-login-nube')) $('tb-login-nube').hidden = true;
    $('tb-login-error').textContent = mensaje || '';
    $('tb-crear-error').textContent = '';
    $('tb-login-clave').value = '';
    $('tb-login-sub').textContent = mensaje ? 'Por seguridad, vuelva a entrar.' : 'Cada persona entra con su propio usuario.';
    setTimeout(() => { const f = TB.primeraVez ? $('tb-dueno-nombre') : ($('tb-login-usuario').value ? $('tb-login-clave') : $('tb-login-usuario')); if (f) f.focus(); }, 60);
  }

  // Saludo del panel izquierdo: cambia con la hora y muestra la fecha de hoy
  function ponerSaludo() {
    const ahora = new Date(), h = ahora.getHours();
    const saludo = h < 12 ? 'Buenos días.' : h < 19 ? 'Buenas tardes.' : 'Buenas noches.';
    let fecha = '';
    try { fecha = ahora.toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long' }); } catch (e) { fecha = ''; }
    if ($('tb-lg-saludo')) $('tb-lg-saludo').textContent = saludo;
    if ($('tb-lg-fecha')) $('tb-lg-fecha').textContent = (fecha ? 'Hoy es ' + fecha + '. ' : '') + 'Sus negocios lo esperan.';
  }

  function ocupado(boton, si, texto) {
    boton.disabled = si;
    if (si) { boton.dataset.texto = boton.textContent; boton.textContent = texto; } else if (boton.dataset.texto) { boton.textContent = boton.dataset.texto; }
  }

  window.tbIniciarSesionForm = function (ev) {
    ev.preventDefault();
    const boton = $('tb-login-btn');
    ocupado(boton, true, 'Entrando…');
    // un respiro para que se pinte "Entrando…" (revisar la contraseña tarda medio segundo)
    setTimeout(() => {
      const r = llamar('iniciarSesion', $('tb-login-usuario').value, $('tb-login-clave').value);
      ocupado(boton, false);
      if (esError(r)) { $('tb-login-error').textContent = textoError(r); $('tb-login-clave').select(); return; }
      const e = JSON.parse(r);
      TB.sesion = e.sesion;
      $('tb-login-clave').value = '';
      entrarALaApp();
      if (typeof showToast === 'function') showToast('Hola, ' + primerNombre(TB.sesion.nombre) + '.');
    }, 30);
  };

  window.tbCrearDueno = function (ev) {
    ev.preventDefault();
    const clave = $('tb-dueno-clave').value;
    if (clave !== $('tb-dueno-clave2').value) { $('tb-crear-error').textContent = 'Las dos contraseñas no coinciden.'; return; }
    const boton = $('tb-crear-btn');
    ocupado(boton, true, 'Creando…');
    setTimeout(() => {
      const r = llamar('crearPrimerDueno', $('tb-dueno-nombre').value, $('tb-dueno-usuario').value, clave);
      ocupado(boton, false);
      if (esError(r)) { $('tb-crear-error').textContent = textoError(r); return; }
      const e = JSON.parse(r);
      TB.primeraVez = false;
      TB.sesion = e.sesion;
      ['tb-dueno-clave', 'tb-dueno-clave2'].forEach(id => { $(id).value = ''; });
      entrarALaApp();
      if (typeof showToast === 'function') showToast('Cuenta creada. Bienvenido, ' + primerNombre(TB.sesion.nombre) + '.');
    }, 30);
  };

  // Computador nuevo de un negocio que ya tiene su base (por ejemplo, en la nube)
  window.tbNubeAbrir = function (si) {
    $('tb-login-crear').hidden = si;
    $('tb-login-nube').hidden = !si;
    $('tb-nube-error').textContent = '';
    setTimeout(() => { const f = si ? $('tb-nube-url') : $('tb-dueno-nombre'); if (f) f.focus(); }, 60);
  };

  window.tbNubeConectar = function (ev) {
    ev.preventDefault();
    const boton = $('tb-nube-btn');
    ocupado(boton, true, 'Conectando…');
    setTimeout(() => {
      const datos = { motor: $('tb-nube-motor').value, url: $('tb-nube-url').value.trim(), usuario: $('tb-nube-usuario').value.trim(), clave: $('tb-nube-clave').value };
      const r = llamar('conectarBaseExistente', JSON.stringify(datos));
      ocupado(boton, false);
      if (esError(r)) { $('tb-nube-error').textContent = textoError(r); return; }
      $('tb-nube-clave').value = '';
      leerEstado();
      mostrarLogin();
      if (typeof showToast === 'function') showToast(TB.primeraVez ? 'Conectado. La base está vacía: cree la cuenta del dueño.' : 'Conectado a la base del negocio. Entre con su usuario.');
    }, 30);
  };

  window.tbVerClave = function (id, boton) {
    const campo = $(id);
    const ver = campo.type === 'password';
    campo.type = ver ? 'text' : 'password';
    boton.textContent = ver ? 'Ocultar' : 'Ver';
    boton.setAttribute('aria-label', ver ? 'Ocultar contraseña' : 'Mostrar contraseña');
  };

  window.tbCerrarSesion = function (mensaje) {
    llamar('cerrarSesion');
    TB.sesion = null;
    // se vacía lo que había en pantalla para que el siguiente no lo vea
    if (typeof state !== 'undefined') {
      state.perfiles = []; state.perfilActivo = null;
      state.productos = state.categorias = state.proveedores = state.entradas = state.salidas = state.ventas = state.prodFiltrados = [];
      state.archivo = {};
    }
    if (typeof showSection === 'function') showSection('dashboard');
    mostrarLogin(mensaje);
  };

  const primerNombre = n => String(n || '').trim().split(/\s+/)[0];

  // ── Permisos en pantalla ───────────────────────────────────
  // Qué permiso pide cada sección del menú (las que no están, solo piden sesión).
  const SECCIONES = {
    categorias: 'EDITAR_CATALOGO', proveedores: 'EDITAR_CATALOGO', entradas: 'MOVIMIENTOS', salidas: 'MOVIMIENTOS',
    informes: 'INFORMES', costos: 'COSTOS', perfiles: 'NEGOCIOS', respaldos: 'RESPALDOS', usuarios: 'USUARIOS', historial: 'HISTORIAL', fiados: 'VENDER'
  };
  const PERMISOS = ['VENDER', 'VER_GANANCIA', 'EDITAR_CATALOGO', 'MOVIMIENTOS', 'ANULAR', 'DESHABILITAR', 'INFORMES', 'COSTOS', 'USUARIOS', 'NEGOCIOS', 'RESPALDOS', 'CONFIGURACION', 'HISTORIAL'];

  function aplicarPermisos() {
    const b = document.body;
    PERMISOS.forEach(p => b.classList.toggle('tb-sin-' + p, !puede(p)));
    b.classList.toggle('tb-sin-crear-negocios', !(TB.sesion && TB.sesion.puedeCrearNegocios));
    document.querySelectorAll('#sidebar .nav-item').forEach(a => {
      const p = SECCIONES[a.dataset.section];
      a.hidden = !!p && !puede(p) && !(a.dataset.section === 'perfiles' && TB.sesion && TB.sesion.puedeCrearNegocios);
    });
    // si la sección abierta ya no se puede usar (cambió el negocio o el rol), se vuelve a Hoy
    const activa = document.querySelector('.section-panel.active');
    const nombre = activa ? activa.id.replace(/^sec-/, '') : '';
    if (SECCIONES[nombre] && !puede(SECCIONES[nombre]) && typeof showSection === 'function') showSection('dashboard');
    if ($('tb-sesion-nombre')) {
      $('tb-sesion-nombre').textContent = TB.sesion ? TB.sesion.nombre : '';
      $('tb-sesion-rol').textContent = TB.sesion ? (TB.sesion.rolEtiqueta || 'Sin negocio') + ' · ' + TB.sesion.usuario : '';
    }
    marcarAnulables();
  }

  // El cajero solo ve "Anular" en sus propias ventas de los últimos minutos
  function marcarAnulables() {
    if (typeof state === 'undefined' || !TB.sesion) return;
    const limite = (TB.sesion.minutosAnularPropia || 10) * 60000;
    document.querySelectorAll('[onclick*="confirmarEliminar"][onclick*="ventas"]').forEach(el => {
      const m = /confirmarEliminar\('([^']+)'/.exec(el.getAttribute('onclick') || '');
      const v = m && (state.ventas || []).find(x => x.id === m[1]);
      let propia = false;
      if (v && v.usuarioId === TB.sesion.id && v.fechaRegistro) {
        const t = new Date(v.fechaRegistro).getTime();
        propia = Date.now() - t <= limite;
      }
      el.classList.toggle('tb-anular-propia', propia);
    });
  }
  window.tbMarcarAnulables = marcarAnulables;

  // Después de cambiar de negocio el rol puede ser otro: se vuelve a leer la sesión
  const cargarOriginal = window.cargarEstadoDesdeJava;
  window.cargarEstadoDesdeJava = function () {
    if (!TB.sesion) return;
    leerEstado();
    if (!TB.sesion) { mostrarLogin('Su sesión se cerró.'); return; }
    aplicarPermisos();
    const r = cargarOriginal.apply(this, arguments);
    if ($('sec-usuarios') && $('sec-usuarios').classList.contains('active')) renderUsuarios();
    marcarAnulables();
    return r;
  };

  ['renderTablaVentas', 'actualizarDashboard'].forEach(nombre => {
    const f = window[nombre];
    if (typeof f !== 'function') return;
    window[nombre] = function () { const r = f.apply(this, arguments); try { marcarAnulables(); } catch (e) { } return r; };
  });

  // Sin negocios: el dueño crea el primero; los demás no pueden
  const primerArranqueOriginal = window.checkPrimerArranque;
  window.checkPrimerArranque = function () {
    if (!TB.sesion) return;
    if (TB.sesion.puedeCrearNegocios) { if (typeof primerArranqueOriginal === 'function') primerArranqueOriginal(); return; }
    if (typeof state !== 'undefined' && state.perfiles.length === 0 && typeof showToast === 'function') {
      showToast('Su usuario todavía no tiene negocios asignados. Pídale al dueño que le dé acceso.', 'error');
    }
  };

  // Mostrar Usuarios al entrar a esa sección
  const showOriginal = window.showSection;
  window.showSection = function (sec) {
    if (SECCIONES[sec] && !puede(SECCIONES[sec]) && !(sec === 'perfiles' && TB.sesion && TB.sesion.puedeCrearNegocios)) {
      if (typeof showToast === 'function') showToast('Su rol no tiene acceso a esa sección.', 'error');
      return;
    }
    const r = showOriginal.apply(this, arguments);
    if (sec === 'usuarios') renderUsuarios();
    if (sec === 'historial') window.tbHistorialCargar(true);
    return r;
  };

  // ── Sesión vencida ─────────────────────────────────────────
  // Cualquier respuesta de Java que diga que la sesión se cerró lleva al inicio de sesión.
  let puenteEnvuelto = false;
  function envolverPuente() {
    if (puenteEnvuelto || !window.javaBridge) return;
    puenteEnvuelto = true;
    const original = window.javaBridge;
    window.javaBridge = new Proxy(original, {
      get: function (objetivo, nombre) {
        const f = objetivo[nombre];
        if (typeof f !== 'function') return f;
        return function () {
          const r = f.apply(objetivo, arguments);
          if (typeof r === 'string' && r.startsWith('ERROR: Su sesión se cerró') && TB.sesion) {
            setTimeout(() => window.tbCerrarSesion('Su sesión se cerró por inactividad.'), 0);
          }
          return r;
        };
      }
    });
  }
  envolverPuente();

  // Cierre por inactividad: lo decide Java, la página solo avisa antes y cierra a tiempo
  ['mousemove', 'mousedown', 'keydown', 'wheel', 'touchstart'].forEach(ev =>
    document.addEventListener(ev, () => { TB.ultimoUso = Date.now(); }, { passive: true, capture: true }));
  setInterval(() => {
    if (!TB.sesion) return;
    const quieto = Date.now() - TB.ultimoUso;
    if (quieto > TB.minutos * 60000) {
      window.tbCerrarSesion('Su sesión se cerró por inactividad.');
    } else if (quieto > (TB.minutos - 1) * 60000 && !TB.avisoInactividad && typeof showToast === 'function') {
      TB.avisoInactividad = true;
      showToast('Por seguridad, la sesión se cerrará en un minuto si no hay actividad.');
    } else if (quieto < 60000) {
      TB.avisoInactividad = false;
    }
  }, 15000);

  // Varios equipos sobre la misma base (por ejemplo, en la nube): cada 5 segundos se
  // pregunta a Java si otro equipo guardó algo; si es así, se recargan los datos sin
  // cerrar nada que la persona tenga abierto (si hay una ventana abierta, se espera).
  let versionVista = null;
  setInterval(() => {
    if (!TB.sesion || document.hidden || !window.javaBridge) return;
    let r;
    try { r = window.javaBridge.revisarCambios(); } catch (e) { return; }
    if (typeof r !== 'string' || r.startsWith('ERROR')) return;
    if (versionVista === null || r === '0') { versionVista = r; return; }
    if (r === versionVista) return;
    const ventanaAbierta = document.querySelector('div[id^="modal-"].fixed.inset-0:not(.hidden), .tb-dlg:not([hidden])');
    if (ventanaAbierta) return;
    versionVista = r;
    const actual = document.querySelector('.section-panel.active');
    const sec = actual ? actual.id.replace(/^sec-/, '') : '';
    if (typeof cargarEstadoDesdeJava === 'function') cargarEstadoDesdeJava();
    if (sec === 'historial' && window.tbHistorialCargar) window.tbHistorialCargar(true);
    if (sec === 'fiados' && window.tbFiadosCargar) window.tbFiadosCargar();
    if (sec === 'usuarios') renderUsuarios();
    if (typeof showToast === 'function') showToast('Se actualizaron los datos: hubo cambios desde otro equipo.');
  }, 5000);

  // ── Sección Usuarios ───────────────────────────────────────
  function renderUsuarios() {
    const lista = $('usr-lista');
    if (!lista || !TB.sesion) return;
    if ($('usr-negocio')) $('usr-negocio').textContent = (typeof state !== 'undefined' && state.perfilActivo) ? state.perfilActivo.nombre : '';
    if (!puede('USUARIOS')) { lista.innerHTML = ''; return; }
    const r = llamar('listarUsuarios');
    if (esError(r)) { lista.innerHTML = '<p class="tb-usr-vacio">' + esc(textoError(r)) + '</p>'; return; }
    TB.usuarios = JSON.parse(r);
    const activos = TB.usuarios.filter(u => u.activo).length;
    $('usr-titulo').textContent = activos === 1 ? 'Solo usted entra a este negocio.' : activos + ' personas entran a este negocio.';
    $('usr-lead').textContent = puede('NEGOCIOS')
      ? 'Cada persona entra con su usuario y ve solo lo que su rol permite.'
      : 'Como administrador, usted puede crear y administrar cajeros.';
    lista.innerHTML = TB.usuarios.map(u => {
      const acciones = u.puedeAdministrar
        ? '<button type="button" class="tbv-link" onclick="tbUsuarioEditar(\'' + u.id + '\')">Editar</button>'
          + '<button type="button" class="tbv-link" onclick="tbUsuarioClave(\'' + u.id + '\')">Nueva contraseña</button>'
          + '<button type="button" class="tbv-link" onclick="tbUsuarioEstado(\'' + u.id + '\',' + (!u.activo) + ')">' + (u.activo ? 'Deshabilitar' : 'Habilitar') + '</button>'
        : (u.esUsted ? '<button type="button" class="tbv-link" onclick="tbAbrirCambiarClave()">Cambiar mi contraseña</button>' : '');
      const ingreso = u.ultimoIngreso ? 'Entró ' + cuandoFue(u.ultimoIngreso) : 'Todavía no ha entrado';
      return '<div class="tb-usr' + (u.activo ? '' : ' off') + '">'
        + '<span class="tb-usr-ini" aria-hidden="true">' + esc(String(u.nombre || '?').trim().charAt(0).toUpperCase()) + '</span>'
        + '<div class="tb-usr-quien"><b>' + esc(u.nombre) + (u.esUsted ? ' <em>(usted)</em>' : '') + '</b><span>' + esc(u.usuario) + ' · ' + esc(ingreso) + (u.activo ? '' : ' · Deshabilitado') + '</span></div>'
        + '<span class="tb-usr-rol rol-' + esc(u.rol) + '">' + esc(u.rolEtiqueta) + '</span>'
        + '<div class="tb-usr-acc">' + acciones + '</div></div>';
    }).join('');
  }
  window.tbRenderUsuarios = renderUsuarios;

  function cuandoFue(iso) {
    const d = new Date(iso);
    if (isNaN(d)) return '';
    const hoy = new Date(); hoy.setHours(0, 0, 0, 0);
    const dia = new Date(d); dia.setHours(0, 0, 0, 0);
    const dias = Math.round((hoy - dia) / 86400000);
    const hora = d.toLocaleTimeString('es-CO', { hour: 'numeric', minute: '2-digit' });
    if (dias === 0) return 'hoy a las ' + hora;
    if (dias === 1) return 'ayer a las ' + hora;
    return 'el ' + d.toLocaleDateString('es-CO', { day: 'numeric', month: 'long' });
  }

  const ROLES = [
    { r: 'DUENO', t: 'Dueño', d: 'Todo, también copias, base de datos y usuarios' },
    { r: 'ADMINISTRADOR', t: 'Administrador', d: 'Maneja la tienda y crea cajeros' },
    { r: 'CAJERO', t: 'Cajero', d: 'Vende y consulta precios' }
  ];

  function abrirDlg(id) { const d = $(id); d.hidden = false; setTimeout(() => { const f = d.querySelector('input:not([type=hidden]):not([disabled])'); if (f) f.focus(); }, 40); }
  window.tbCerrarDlg = function (id) { $(id).hidden = true; };
  document.addEventListener('keydown', e => {
    if (e.key !== 'Escape') return;
    ['tb-dlg-usuario', 'tb-dlg-clave'].forEach(id => { const d = $(id); if (d && !d.hidden) d.hidden = true; });
  });

  function pintarRoles(elegido) {
    const caja = $('tb-usr-roles');
    // el administrador solo puede crear cajeros
    const posibles = puede('NEGOCIOS') ? ROLES : ROLES.filter(x => x.r === 'CAJERO');
    caja.innerHTML = '<legend>Rol en este negocio</legend>' + posibles.map(x =>
      '<label class="tb-rol"><input type="radio" name="tb-usr-rol" value="' + x.r + '"' + (x.r === elegido ? ' checked' : '') + '><span><b>' + x.t + '</b><small>' + x.d + '</small></span></label>').join('');
    if (!caja.querySelector('input:checked')) caja.querySelector('input').checked = true;
  }

  window.tbUsuarioNuevo = function () {
    $('tb-usr-dlg-titulo').textContent = 'Nuevo usuario';
    $('tb-usr-id').value = '';
    $('tb-usr-nombre').value = '';
    $('tb-usr-usuario').value = '';
    $('tb-usr-usuario').disabled = false;
    $('tb-usr-clave').value = '';
    $('tb-usr-clave').required = true;
    $('tb-usr-clave-campo').hidden = false;
    $('tb-usr-error').textContent = '';
    pintarRoles('CAJERO');
    abrirDlg('tb-dlg-usuario');
  };

  window.tbUsuarioEditar = function (id) {
    const u = TB.usuarios.find(x => x.id === id);
    if (!u) return;
    $('tb-usr-dlg-titulo').textContent = 'Editar a ' + primerNombre(u.nombre);
    $('tb-usr-id').value = u.id;
    $('tb-usr-nombre').value = u.nombre;
    $('tb-usr-usuario').value = u.usuario;
    $('tb-usr-usuario').disabled = true;
    $('tb-usr-clave').required = false;
    $('tb-usr-clave-campo').hidden = true;
    $('tb-usr-error').textContent = '';
    pintarRoles(u.rol);
    abrirDlg('tb-dlg-usuario');
  };

  window.tbGuardarUsuario = function (ev) {
    ev.preventDefault();
    const rol = document.querySelector('input[name="tb-usr-rol"]:checked');
    const datos = { id: $('tb-usr-id').value || null, nombre: $('tb-usr-nombre').value, usuario: $('tb-usr-usuario').value,
      clave: $('tb-usr-clave').value, rol: rol ? rol.value : 'CAJERO' };
    const r = llamar('guardarUsuario', JSON.stringify(datos));
    if (esError(r)) { $('tb-usr-error').textContent = textoError(r); return; }
    tbCerrarDlg('tb-dlg-usuario');
    renderUsuarios();
    if (typeof showToast === 'function') showToast(datos.id ? 'Cambios guardados.' : 'Usuario creado. Ya puede entrar con ' + JSON.parse(r).usuario + '.');
  };

  window.tbUsuarioEstado = function (id, activo) {
    const u = TB.usuarios.find(x => x.id === id);
    const r = llamar('cambiarEstadoUsuario', id, String(activo));
    if (esError(r)) { if (typeof showToast === 'function') showToast(textoError(r), 'error'); return; }
    renderUsuarios();
    if (typeof showToast === 'function') showToast(activo ? primerNombre(u && u.nombre) + ' puede volver a entrar.' : primerNombre(u && u.nombre) + ' ya no puede entrar. Sus ventas se conservan.');
  };

  window.tbUsuarioClave = function (id) {
    const u = TB.usuarios.find(x => x.id === id);
    $('tb-clave-titulo').textContent = 'Nueva contraseña para ' + primerNombre(u ? u.nombre : '');
    $('tb-clave-sub').textContent = 'Désela en persona. Podrá cambiarla después desde su menú.';
    $('tb-clave-para').value = id;
    $('tb-clave-actual-campo').hidden = true;
    limpiarClave();
    abrirDlg('tb-dlg-clave');
  };

  window.tbAbrirCambiarClave = function () {
    $('tb-clave-titulo').textContent = 'Cambiar mi contraseña';
    $('tb-clave-sub').textContent = '';
    $('tb-clave-para').value = '';
    $('tb-clave-actual-campo').hidden = false;
    limpiarClave();
    abrirDlg('tb-dlg-clave');
  };

  function limpiarClave() { ['tb-clave-actual', 'tb-clave-nueva', 'tb-clave-nueva2'].forEach(id => { $(id).value = ''; }); $('tb-clave-error').textContent = ''; }

  window.tbGuardarClave = function (ev) {
    ev.preventDefault();
    const nueva = $('tb-clave-nueva').value;
    if (nueva !== $('tb-clave-nueva2').value) { $('tb-clave-error').textContent = 'Las dos contraseñas nuevas no coinciden.'; return; }
    const para = $('tb-clave-para').value;
    const r = para ? llamar('restablecerClaveUsuario', para, nueva) : llamar('cambiarMiClave', $('tb-clave-actual').value, nueva);
    if (esError(r)) { $('tb-clave-error').textContent = textoError(r); return; }
    tbCerrarDlg('tb-dlg-clave');
    limpiarClave();
    if (typeof showToast === 'function') showToast('Contraseña guardada.');
  };
})();

// ── Historial de acciones ────────────────────────────────────
(function () {
  'use strict';
  const $h = id => document.getElementById(id);
  const escH = t => String(t == null ? '' : t).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
  let personas = {};

  function dia(fecha) {
    const d = String(fecha || '').slice(0, 10);
    const hoy = new Date(), ayer = new Date(Date.now() - 864e5);
    const iso = x => x.getFullYear() + '-' + String(x.getMonth() + 1).padStart(2, '0') + '-' + String(x.getDate()).padStart(2, '0');
    if (d === iso(hoy)) return 'Hoy';
    if (d === iso(ayer)) return 'Ayer';
    const [a, m, dd] = d.split('-');
    return Number(dd) + ' de ' + (MESES[Number(m) - 1] || m) + ' de ' + a;
  }
  function hora(fecha) {
    const t = String(fecha || '').slice(11, 16).split(':');
    if (t.length < 2) return '';
    let h = Number(t[0]); const ampm = h >= 12 ? 'p. m.' : 'a. m.'; h = h % 12 || 12;
    return h + ':' + t[1] + ' ' + ampm;
  }
  function tono(accion) {
    if (/Anul|Deshabilit|Elimin|sin permiso|fallido/i.test(accion)) return 'alerta';
    if (/venta/i.test(accion)) return 'venta';
    return '';
  }

  window.tbHistorialCargar = function (reiniciar) {
    if (!window.javaBridge || !window.javaBridge.listarHistorial) return;
    if (reiniciar) personas = {};
    const filtros = { usuarioId: $h('his-usuario').value, desde: $h('his-desde').value, hasta: $h('his-hasta').value, soloEsteNegocio: $h('his-solo').checked };
    const raw = window.javaBridge.listarHistorial(JSON.stringify(filtros));
    if (typeof raw === 'string' && raw.startsWith('ERROR')) { $h('his-lista').innerHTML = '<p class="tb-his-vacio">' + escH(raw.replace(/^ERROR:\s*/, '')) + '</p>'; return; }
    let lista = [];
    try { lista = JSON.parse(raw) || []; } catch (e) { lista = []; }
    lista.forEach(e => { if (e.usuarioId && !personas[e.usuarioId]) personas[e.usuarioId] = e.usuario; });
    const sel = $h('his-usuario'), elegido = sel.value;
    sel.innerHTML = '<option value="">Todas</option>' + Object.keys(personas).map(id => '<option value="' + escH(id) + '">' + escH(personas[id]) + '</option>').join('');
    sel.value = elegido;
    const negocio = (typeof state !== 'undefined' && state.perfilActivo && state.perfilActivo.nombre) || '';
    $h('his-negocio').textContent = negocio;
    $h('his-titulo').textContent = lista.length ? (lista.length >= 300 ? 'Las últimas 300 acciones.' : lista.length + (lista.length === 1 ? ' acción registrada.' : ' acciones registradas.')) : 'Historial';
    if (!lista.length) { $h('his-lista').innerHTML = '<p class="tb-his-vacio">No hay acciones con esos filtros.</p>'; return; }
    let html = '', actual = '';
    lista.forEach(e => {
      const d = dia(e.fecha);
      if (d !== actual) { actual = d; html += '<div class="tb-his-dia">' + escH(d) + '</div>'; }
      html += '<div class="tb-his-fila ' + tono(e.accion) + '"><span class="h">' + escH(hora(e.fecha)) + '</span>'
        + '<div class="q"><b>' + escH(e.usuario || 'Sin sesión') + '</b>' + (e.rol ? '<em>' + escH(e.rol) + '</em>' : '') + '</div>'
        + '<div class="a"><span class="ac">' + escH(e.accion) + '</span>' + (e.detalle ? '<span class="de">' + escH(e.detalle) + '</span>' : '') + '</div>'
        + '<span class="n">' + escH(e.perfil || 'General') + '</span></div>';
    });
    $h('his-lista').innerHTML = html;
  };
})();
