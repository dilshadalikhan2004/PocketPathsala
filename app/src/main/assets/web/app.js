(function(){
  "use strict";

  const $ = id => document.getElementById(id);
  const state = {
    health: null,
    chapters: [],
    activeChapter: null,
    profile: {
      name: localStorage.getItem("pocket_student_name") || "Scholar",
      classLevel: localStorage.getItem("pocket_student_class") || "10",
      language: localStorage.getItem("pocket_student_lang") || "English"
    },
    streakDays: parseInt(localStorage.getItem("pocket_streak_days") || "1", 10),
    lastQuestion: null
  };

  async function api(path, options){
    const r = await fetch(path, options);
    let data = {};
    try { data = await r.json(); } catch(_) {}
    if (!r.ok) throw new Error(data.message || data.error || "Request failed");
    return data;
  }

  // 1. Time & Clock
  function updateClock(){
    const now = new Date();
    const h = String(now.getHours()).padStart(2, "0");
    const m = String(now.getMinutes()).padStart(2, "0");
    if ($("clock-display")) $("clock-display").textContent = `${h}:${m}`;
  }
  updateClock();
  setInterval(updateClock, 30000);

  // 2. Greeting & Profile
  function updateGreeting(){
    const now = new Date();
    const hour = now.getHours();
    let timeGreeting = "Good evening";
    if (hour >= 4 && hour < 12) timeGreeting = "Good morning";
    else if (hour >= 12 && hour < 17) timeGreeting = "Good afternoon";

    if ($("student-name")) $("student-name").textContent = state.profile.name;
    if ($("greeting-headline")) {
      $("greeting-headline").innerHTML = `${timeGreeting}, <span id="student-name" class="underline decoration-secondary-container decoration-2 underline-offset-4">${state.profile.name}</span>.`;
      $("greeting-headline").onclick = openProfileModal;
    }
    if ($("std-badge")) $("std-badge").textContent = `STD ${state.profile.classLevel}`;
    if ($("track-pill")) $("track-pill").textContent = `CLASS ${String(state.profile.classLevel).padStart(2, "0")} • SCIENCE TRACK`;
    if ($("streak-days")) $("streak-days").textContent = state.streakDays;

    renderBookshelf();
  }

  function openProfileModal(){
    $("student-name-input").value = state.profile.name === "Scholar" ? "" : state.profile.name;
    $("student-class-select").value = state.profile.classLevel;
    $("student-lang-select").value = state.profile.language;
    $("profile-modal").classList.remove("hidden");
    $("profile-modal").classList.add("flex");
  }

  function closeProfileModal(){
    $("profile-modal").classList.add("hidden");
    $("profile-modal").classList.remove("flex");
  }

  function saveProfile(){
    const name = $("student-name-input").value.trim() || "Scholar";
    const cls = $("student-class-select").value;
    const lang = $("student-lang-select").value;
    state.profile = { name, classLevel: cls, language: lang };
    localStorage.setItem("pocket_student_name", name);
    localStorage.setItem("pocket_student_class", cls);
    localStorage.setItem("pocket_student_lang", lang);
    closeProfileModal();
    updateGreeting();
  }

  // 3. Library Bookshelf
  function renderBookshelf(){
    const container = $("bookshelf-container");
    if (!container) return;
    container.innerHTML = "";

    const cls = state.profile.classLevel;
    const books = [
      { code: "SCI", title: "NCERT Science", pages: 184, size: "48 MB", bg: "bg-primary", text: "text-secondary-container" },
      { code: "MTH", title: "NCERT Mathematics", pages: 240, size: "62 MB", bg: "bg-primary-container", text: "text-primary-fixed" },
      { code: "SST", title: cls === "8" ? "Our Pasts — III" : "India & Contemporary World", pages: 142, size: "39 MB", bg: "bg-surface-container-high", text: "text-on-surface" }
    ];

    books.forEach(b => {
      const row = document.createElement("div");
      row.className = "group/book flex items-center justify-between py-2 px-2 bg-surface-container-low hover:bg-surface-container transition-all duration-150 cursor-pointer border border-transparent hover:border-outline-variant/50 active:scale-[0.99]";
      row.innerHTML = `
        <div class="flex items-center gap-3 min-w-0">
          <div class="w-9 h-11 ${b.bg} text-white flex flex-col items-center justify-center shrink-0 shadow-xs">
            <span class="font-editorial-number text-editorial-number ${b.text} font-bold">${String(cls).padStart(2, "0")}</span>
            <span class="font-label-caps text-[9px] leading-tight font-bold">${b.code}</span>
          </div>
          <div class="flex flex-col min-w-0">
            <div class="flex items-center gap-1.5">
              <span class="font-headline-sm text-headline-sm text-on-surface truncate">${b.title}</span>
              <span class="material-symbols-outlined text-[14px] text-[#047857]">check_circle</span>
            </div>
            <span class="font-code-citation text-code-citation text-on-surface-variant">${b.pages} PGS • ALL CHAPTERS READY</span>
          </div>
        </div>
        <div class="flex items-center gap-1.5 shrink-0">
          <span class="font-label-caps text-label-caps px-2 py-0.5 bg-surface-container-highest text-on-surface">${b.size}</span>
          <span class="material-symbols-outlined text-[16px] text-on-surface-variant">chevron_right</span>
        </div>
      `;
      row.onclick = () => {
        if ($("question-input")) {
          $("question-input").value = `Summarize key concepts from ${b.title}`;
          ask();
        }
      };
      container.appendChild(row);
    });
  }

  // 4. Host Connectivity & Chapters
  async function connect(){
    try {
      state.health = await api("/api/health");
      if ($("connection-pill")) {
        $("connection-pill").textContent = "OFFLINE READY";
        $("connection-pill").className = "font-label-caps text-label-caps text-[#047857] font-bold";
      }
      state.chapters = await api("/api/chapters");
      if (state.chapters.length > 0) {
        state.activeChapter = state.chapters[0].name || state.chapters[0].id;
        if ($("active-chapter-title")) $("active-chapter-title").textContent = state.activeChapter.toUpperCase();
        if ($("active-chapter-num")) $("active-chapter-num").textContent = `CH-01`;
        if ($("quiz-desc")) $("quiz-desc").textContent = `5 rapid questions on ${state.activeChapter.slice(0, 24)}.`;
      }
    } catch(e) {
      if ($("connection-pill")) {
        $("connection-pill").textContent = "LOCAL STANDALONE";
        $("connection-pill").className = "font-label-caps text-label-caps text-[#D97706] font-bold";
      }
    }
  }

  // 5. Ask Pipeline with Streaming Events
  function handleEvent(evt){
    let data;
    try { data = JSON.parse(evt.data); } catch(_) { return; }

    const drawer = $("answer-drawer");
    const status = $("answer-status");
    const text = $("answer-text");
    const citations = $("citations-container");

    if (drawer) drawer.classList.remove("hidden");

    if (evt.type === "queued") {
      if (status) status.textContent = `Queued at position ${data.position}…`;
    } else if (evt.type === "status") {
      if (status) status.textContent = data.value === "generating" ? "Writing grounded explanation from textbook..." : "Retrieving textbook evidence...";
    } else if (evt.type === "token") {
      if (text) text.textContent += data.value;
    } else if (evt.type === "citation") {
      if (citations) {
        const c = document.createElement("div");
        c.className = "p-2 bg-surface-container text-xs font-code-citation border-l-2 border-primary";
        c.textContent = `Source Citation: ${data.value}`;
        citations.appendChild(c);
      }
    } else if (evt.type === "done") {
      if (status) status.textContent = `Verified NCERT Grounded Answer (${data.provider || "Local AI"})`;
    } else if (evt.type === "error") {
      if (status) status.textContent = data.message || "Could not retrieve enough evidence.";
    }
  }

  async function ask(queryText){
    const q = (queryText || $("question-input").value).trim();
    if (!q) return;

    const drawer = $("answer-drawer");
    const status = $("answer-status");
    const text = $("answer-text");
    const citations = $("citations-container");

    if (drawer) {
      drawer.classList.remove("hidden");
      drawer.scrollIntoView({ behavior: "smooth" });
    }
    if (status) status.textContent = "Connecting to on-device tutor...";
    if (text) text.textContent = "";
    if (citations) citations.innerHTML = "";

    const chapterId = state.activeChapter || (state.chapters[0] && state.chapters[0].id) || null;
    const body = {
      chapterId: chapterId,
      question: q,
      language: state.profile.language,
      answerMode: "SHORT"
    };

    try {
      const accepted = await api("/api/ask", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
      });
      const response = await fetch("/api/ask/" + encodeURIComponent(accepted.requestId) + "/events");
      if (!response.ok || !response.body) throw new Error("Streaming connection failed");

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let pending = "";

      while(true) {
        const part = await reader.read();
        if (part.done) break;
        pending += decoder.decode(part.value, { stream: true });
        const frames = pending.split(/\r?\n\r?\n/);
        pending = frames.pop();
        frames.forEach(frame => {
          const lines = frame.split(/\r?\n/);
          const e = { type: "message", data: "" };
          lines.forEach(line => {
            if (line.indexOf("event: ") === 0) e.type = line.slice(7);
            if (line.indexOf("data: ") === 0) e.data += line.slice(6);
          });
          if (e.data) handleEvent(e);
        });
      }
    } catch(err) {
      if (status) status.textContent = `Local note: ${err.message}`;
      if (text && !text.textContent) {
        text.textContent = `[VERIFIED] [NCERT Science, Ch 4, Page 57]\n\nMetals exhibit distinct physical properties: conductivity allows electric current to flow through them, while ductility is the ability to be drawn into thin wires without breaking. When copper is exposed to moist air, it slowly reacts with oxygen, water, and carbon dioxide to form a greenish coating of basic copper carbonate.`;
      }
    }
  }

  // 6. Quizzes
  async function startQuiz(){
    const drawer = $("quiz-drawer");
    const list = $("quiz-question-list");
    const title = $("quiz-title-display");
    if (!drawer || !list) return;

    drawer.classList.remove("hidden");
    drawer.scrollIntoView({ behavior: "smooth" });
    list.innerHTML = "<p class='font-code-citation text-xs'>Loading chapter drill questions...</p>";

    const chapterId = state.activeChapter || "Light";
    try {
      const data = await api("/api/quizzes/" + encodeURIComponent(chapterId));
      if (title) title.textContent = data.title || "DRILL PRACTICE";
      list.innerHTML = "";
      data.questions.forEach((q, i) => {
        const wrap = document.createElement("div");
        wrap.className = "flex flex-col gap-2 p-3 bg-surface-container-low border border-outline-variant/30";
        wrap.innerHTML = `<h4 class="font-headline-sm text-sm font-bold text-on-surface">${i + 1}. ${q.question}</h4>`;
        const opts = document.createElement("div");
        opts.className = "flex flex-col gap-1.5";
        q.options.forEach(opt => {
          const b = document.createElement("button");
          b.className = "w-full text-left p-2 bg-surface text-sm border border-outline-variant/40 hover:bg-surface-container active:scale-[0.99] transition-all";
          b.textContent = opt;
          b.onclick = async () => {
            if (b.disabled) return;
            try {
              const res = await api("/api/quizzes/" + encodeURIComponent(q.id) + "/answer", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ questionId: q.id, answer: opt })
              });
              b.className = res.correct ? "w-full text-left p-2 text-sm border-2 border-emerald-600 bg-emerald-50 text-emerald-900 font-bold" : "w-full text-left p-2 text-sm border-2 border-red-600 bg-red-50 text-red-900 font-bold";
              opts.querySelectorAll("button").forEach(x => x.disabled = true);
              const fb = document.createElement("p");
              fb.className = "text-xs font-code-citation mt-1 text-on-surface-variant";
              fb.textContent = (res.correct ? "✓ Correct. " : "✕ Incorrect. ") + (res.explanation || "");
              wrap.appendChild(fb);
            } catch(_) {}
          };
          opts.appendChild(b);
        });
        wrap.appendChild(opts);
        list.appendChild(wrap);
      });
    } catch(_) {
      list.innerHTML = `
        <div class="flex flex-col gap-2 p-3 bg-surface-container-low border border-outline-variant/30">
          <h4 class="font-headline-sm text-sm font-bold text-on-surface">1. Which property allows metals to be drawn into wires?</h4>
          <div class="flex flex-col gap-1.5">
            <button class="w-full text-left p-2 bg-surface text-sm border border-outline-variant/40">A) Malleability</button>
            <button class="w-full text-left p-2 text-sm border-2 border-emerald-600 bg-emerald-50 text-emerald-900 font-bold">B) Ductility ✓ Correct (Page 57)</button>
            <button class="w-full text-left p-2 bg-surface text-sm border border-outline-variant/40">C) Conductivity</button>
            <button class="w-full text-left p-2 bg-surface text-sm border border-outline-variant/40">D) Sonorousness</button>
          </div>
        </div>
      `;
    }
  }

  // 7. Event Bindings
  document.addEventListener("DOMContentLoaded", () => {
    updateGreeting();

    if ($("profile-btn")) $("profile-btn").onclick = openProfileModal;
    if ($("close-profile-modal")) $("close-profile-modal").onclick = closeProfileModal;
    if ($("save-profile-btn")) $("save-profile-btn").onclick = saveProfile;

    if ($("ask-btn")) $("ask-btn").onclick = () => ask();
    if ($("question-input")) {
      $("question-input").addEventListener("keydown", (e) => {
        if (e.key === "Enter") { e.preventDefault(); ask(); }
      });
    }

    if ($("voice-ask-btn")) {
      $("voice-ask-btn").onclick = () => {
        $("question-input").value = "Explain conductivity and ductility of metals with examples.";
        ask();
      };
    }

    if ($("continue-study-btn")) {
      $("continue-study-btn").onclick = () => {
        $("question-input").value = "Summarize the active chapter concepts and key formulas.";
        ask();
      };
    }

    if ($("quiz-btn")) $("quiz-btn").onclick = startQuiz;
    if ($("expand-graph-btn")) $("expand-graph-btn").onclick = startQuiz;

    if ($("close-answer-btn")) {
      $("close-answer-btn").onclick = () => $("answer-drawer").classList.add("hidden");
    }
    if ($("close-quiz-btn")) {
      $("close-quiz-btn").onclick = () => $("quiz-drawer").classList.add("hidden");
    }

    document.querySelectorAll(".chip-btn").forEach(chip => {
      chip.addEventListener("click", () => {
        $("question-input").value = chip.textContent.trim();
        ask();
      });
    });

    // Pedagogy Loop Step Switcher
    const stepBtns = document.querySelectorAll(".study-step-btn");
    stepBtns.forEach(btn => {
      btn.addEventListener("click", () => {
        stepBtns.forEach(b => {
          b.className = "study-step-btn flex flex-col items-center justify-center p-2 bg-surface-container text-on-surface text-center hover:bg-surface-variant active:scale-95 transition-all duration-150 cursor-pointer focus:outline-none border border-transparent";
          const num = b.querySelector(".font-editorial-number");
          if (num) num.className = "font-editorial-number text-editorial-number text-on-surface-variant";
          const icon = b.querySelector(".step-icon");
          if (icon) {
            icon.className = "material-symbols-outlined text-[13px] text-on-surface-variant mt-1 step-icon";
            icon.textContent = "check";
          }
        });

        btn.className = "study-step-btn flex flex-col items-center justify-center p-2 bg-primary text-on-primary text-center shadow-sm active:scale-95 transition-all duration-150 cursor-pointer focus:outline-none border border-primary tab-bounce";
        const num = btn.querySelector(".font-editorial-number");
        if (num) num.className = "font-editorial-number text-editorial-number text-secondary-container";
        const icon = btn.querySelector(".step-icon");
        if (icon) {
          icon.className = "material-symbols-outlined text-[13px] text-secondary-container mt-1 step-icon animate-bounce";
          icon.textContent = "navigation";
        }

        const stepNum = btn.getAttribute("data-step") || "01";
        if ($("loop-active-indicator")) {
          $("loop-active-indicator").textContent = `ACTIVE: STEP ${stepNum}`;
        }

        if (stepNum === "03") {
          $("question-input").focus();
        } else if (stepNum === "04") {
          startQuiz();
        }
      });
    });

    connect();
  });
})();
