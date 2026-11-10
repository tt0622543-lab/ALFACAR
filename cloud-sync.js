/* ALFACAR Cloud Sync - Supabase Auth + realtime, preserving local records */
(function () {
  if (window.__alfacarCloudSyncLoaded) return;
  window.__alfacarCloudSyncLoaded = true;

  const SUPABASE_URL = "https://xdxhfbsdcegxuccmlxlm.supabase.co";
  const SUPABASE_KEY = "sb_publishable_VGooHty4PF0a6melNgsevQ_BzjH_TrD";
  const STORAGE_KEY = "alfacar-v1";
  const TABLE = "alfacar_cloud_state";
  const loadLibrary = (done) => {
    if (window.supabase && window.supabase.createClient) return done();
    const s = document.createElement("script");
    s.src = "https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2";
    s.onload = done;
    s.onerror = () => console.error("ALFACAR: não foi possível carregar a biblioteca de sincronização.");
    document.head.appendChild(s);
  };

  function start() {
    const client = window.supabase.createClient(SUPABASE_URL, SUPABASE_KEY, {
      auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: true }
    });
    let syncing = false, lastLocal = "", lastCloud = "", channel = null, debounce = null;
    const readLocal = () => localStorage.getItem(STORAGE_KEY) || "{}";
    const parse = (v) => { try { return JSON.parse(v || "{}") || {}; } catch (_) { return {}; } };
    const stable = (v) => { try { return JSON.stringify(v); } catch (_) { return String(v); } };

    function mergeData(local, cloud) {
      const out = Object.assign({}, cloud || {}, local || {});
      const keys = new Set(Object.keys(local || {}).concat(Object.keys(cloud || {})));
      keys.forEach((key) => {
        const a = local && local[key], b = cloud && cloud[key];
        if (Array.isArray(a) || Array.isArray(b)) {
          const merged = [], seen = new Set();
          (Array.isArray(b) ? b : []).concat(Array.isArray(a) ? a : []).forEach((item) => {
            const sig = stable(item);
            if (!seen.has(sig)) { seen.add(sig); merged.push(item); }
          });
          out[key] = merged;
        } else if (a === undefined) out[key] = b;
        else if (b === undefined) out[key] = a;
      });
      return out;
    }

    async function push(data) {
      if (syncing) return;
      const user = (await client.auth.getUser()).data.user;
      if (!user) return;
      syncing = true;
      try {
        const payload = { user_id: user.id, data: data, updated_at: new Date().toISOString() };
        const { error } = await client.from(TABLE).upsert(payload, { onConflict: "user_id" });
        if (error) throw error;
        lastCloud = stable(data);
        lastLocal = readLocal();
        setStatus("Sincronizado com a nuvem");
      } catch (e) {
        console.error("ALFACAR sync:", e);
        setStatus("Sem sincronização: " + (e.message || "verifique a internet"));
      } finally { syncing = false; }
    }

    async function pullAndMerge() {
      const user = (await client.auth.getUser()).data.user;
      if (!user) return;
      setStatus("Sincronizando dados...");
      const { data: row, error } = await client.from(TABLE).select("data").eq("user_id", user.id).maybeSingle();
      if (error) { setStatus("Erro ao ler nuvem: " + error.message); return; }
      const local = parse(readLocal());
      if (!row) { await push(local); return; }
      const merged = mergeData(local, row.data || {});
      const encoded = stable(merged);
      if (encoded !== stable(local)) {
        syncing = true;
        localStorage.setItem(STORAGE_KEY, encoded);
        syncing = false;
        if (typeof window.render === "function") window.render();
      }
      lastCloud = encoded;
      lastLocal = readLocal();
      if (stable(merged) !== stable(row.data || {})) await push(merged);
      else setStatus("Sincronizado com a nuvem");
    }

    function setStatus(message) {
      const el = document.getElementById("alfacar-cloud-status");
      if (el) el.textContent = message;
    }

    function showPanel(session) {
      let panel = document.getElementById("alfacar-cloud-panel");
      if (!panel) {
        panel = document.createElement("div");
        panel.id = "alfacar-cloud-panel";
        panel.style.cssText = "position:fixed;z-index:999999;right:12px;bottom:12px;width:min(340px,calc(100vw - 24px));background:#fff;border:1px solid #d5dce5;border-radius:12px;box-shadow:0 5px 24px #0002;padding:12px;font:14px Arial;color:#172b4d";
        panel.innerHTML = '<div style="font-weight:700;margin-bottom:6px">☁️ ALFACAR na nuvem</div><div id="alfacar-cloud-status" style="font-size:12px;color:#475569;margin-bottom:8px">Conecte sua conta para sincronizar</div><div id="alfacar-cloud-auth"><input id="alfacar-cloud-email" type="email" autocomplete="email" placeholder="Seu e-mail" style="box-sizing:border-box;width:100%;padding:9px;margin:3px 0;border:1px solid #cbd5e1;border-radius:7px"><input id="alfacar-cloud-password" type="password" autocomplete="current-password" placeholder="Crie uma senha (mín. 6 caracteres)" style="box-sizing:border-box;width:100%;padding:9px;margin:3px 0;border:1px solid #cbd5e1;border-radius:7px"><div style="display:flex;gap:6px;margin-top:5px"><button id="alfacar-cloud-login" style="flex:1;padding:9px;border:0;border-radius:7px;background:#174575;color:#fff">Entrar</button><button id="alfacar-cloud-signup" style="flex:1;padding:9px;border:0;border-radius:7px;background:#e8eef5;color:#174575">Criar conta</button></div></div><div id="alfacar-cloud-user" style="display:none"><button id="alfacar-cloud-sync" style="padding:8px 10px;border:0;border-radius:7px;background:#174575;color:#fff">Sincronizar agora</button> <button id="alfacar-cloud-out" style="padding:8px 10px;border:1px solid #cbd5e1;border-radius:7px;background:#fff;color:#334155">Sair</button></div><div style="font-size:11px;color:#64748b;margin-top:8px">Use a mesma conta no Windows e no Android. Seus dados locais serão mesclados com a nuvem.</div>';
        document.body.appendChild(panel);
        document.getElementById("alfacar-cloud-login").onclick = async () => {
          const email = document.getElementById("alfacar-cloud-email").value.trim();
          const password = document.getElementById("alfacar-cloud-password").value;
          if (!email || !password) return setStatus("Informe e-mail e senha.");
          setStatus("Entrando...");
          const { error } = await client.auth.signInWithPassword({ email, password });
          if (error) setStatus(error.message); else await afterLogin();
        };
        document.getElementById("alfacar-cloud-signup").onclick = async () => {
          const email = document.getElementById("alfacar-cloud-email").value.trim();
          const password = document.getElementById("alfacar-cloud-password").value;
          if (!email || password.length < 6) return setStatus("Informe e-mail e senha com pelo menos 6 caracteres.");
          setStatus("Criando conta...");
          const { data, error } = await client.auth.signUp({ email, password });
          if (error) setStatus(error.message);
          else if (!data.session) setStatus("Conta criada. Confirme o e-mail enviado e depois entre.");
          else await afterLogin();
        };
        document.getElementById("alfacar-cloud-sync").onclick = pullAndMerge;
        document.getElementById("alfacar-cloud-out").onclick = async () => { await client.auth.signOut(); showPanel(null); setStatus("Você saiu da nuvem."); };
      }
      const auth = document.getElementById("alfacar-cloud-auth");
      const user = document.getElementById("alfacar-cloud-user");
      if (session && session.user) {
        auth.style.display = "none"; user.style.display = "block";
        setStatus("Conectado: " + session.user.email);
      } else {
        auth.style.display = "block"; user.style.display = "none";
      }
    }

    async function afterLogin() {
      const { data } = await client.auth.getSession();
      showPanel(data.session);
      await pullAndMerge();
      if (channel) client.removeChannel(channel);
      const user = data.session.user;
      channel = client.channel("alfacar-cloud-" + user.id)
        .on("postgres_changes", { event: "*", schema: "public", table: TABLE, filter: "user_id=eq." + user.id }, async (payload) => {
          if (syncing || !payload.new || !payload.new.data) return;
          const local = parse(readLocal());
          const merged = mergeData(local, payload.new.data);
          const enc = stable(merged);
          if (enc !== stable(local)) {
            syncing = true; localStorage.setItem(STORAGE_KEY, enc); syncing = false;
            if (typeof window.render === "function") window.render();
          }
          lastCloud = enc; lastLocal = readLocal();
          if (enc !== stable(payload.new.data)) await push(merged);
          else setStatus("Sincronizado com a nuvem");
        }).subscribe();
    }

    const originalSetItem = Storage.prototype.setItem;
    Storage.prototype.setItem = function (key, value) {
      originalSetItem.apply(this, arguments);
      if (key === STORAGE_KEY && !syncing) {
        clearTimeout(debounce);
        debounce = setTimeout(() => {
          const current = readLocal();
          if (current !== lastLocal) push(parse(current));
        }, 900);
      }
    };

    client.auth.getSession().then(({ data }) => {
      showPanel(data.session);
      if (data.session) afterLogin();
    });
    client.auth.onAuthStateChange((event, session) => {
      showPanel(session);
      if (event === "SIGNED_IN") setTimeout(afterLogin, 0);
    });
    setInterval(() => {
      client.auth.getSession().then(({ data }) => {
        if (!data.session || syncing) return;
        const current = readLocal();
        if (current !== lastLocal) push(parse(current));
      });
    }, 3000);
  }

  function boot() { loadLibrary(start); }
  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", boot);
  else boot();
})();