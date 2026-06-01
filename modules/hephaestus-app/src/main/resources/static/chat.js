(function () {
    const DEFAULT_SESSION_TITLE = "新建聊天";
    const DEFAULT_HINT_TEXT = "询问任何问题";
    const CHAT_CONFIG_KEYS = {
        mouseTrailEffect: "login.mouse.trail.effect"
    };
    const EMPTY_STATE_HTML = [
        '<div class="messages-inner">',
        '  <div class="empty">',
        '    <strong>有什么可以帮忙的？</strong>',
        '  </div>',
        '</div>'
    ].join("");

    const basePath = window.location.pathname.replace(/\/[^/]*$/, "");
    const streamUrl = `${basePath}/api/chat/multimodal/stream`;

    const messages = document.getElementById("messages");
    const main = document.querySelector(".main");
    const form = document.getElementById("chatForm");
    const input = document.getElementById("messageInput");
    const sendButton = document.getElementById("sendButton");
    const liveStatusText = document.getElementById("liveStatusText");
    const liveStatusTrack = document.getElementById("liveStatusTrack");
    const statusText = document.getElementById("statusText");
    const statusProgress = document.getElementById("statusProgress");
    const newSessionButton = document.getElementById("newSessionButton");
    const sessionList = document.getElementById("sessionList");
    const chooseFileButton = document.getElementById("chooseFileButton");
    const removeFileButton = document.getElementById("removeFileButton");
    const attachmentInput = document.getElementById("attachmentInput");
    const attachmentChip = document.getElementById("attachmentChip");
    const attachmentThumb = document.getElementById("attachmentThumb");
    const attachmentName = document.getElementById("attachmentName");
    const attachmentSize = document.getElementById("attachmentSize");
    const logoutButton = document.getElementById("logoutButton");
    const settingsButton = document.getElementById("settingsButton");
    const profileInfoButton = document.getElementById("profileInfoButton");
    const helpButton = document.getElementById("helpButton");
    const userProfileButton = document.getElementById("userProfileButton");
    const userActionMenu = document.getElementById("userActionMenu");
    const userAvatar = document.getElementById("userAvatar");
    const userDisplayName = document.getElementById("userDisplayName");
    const sessionExpiredDialog = document.getElementById("sessionExpiredDialog");
    const sessionExpiredLoginButton = document.getElementById("sessionExpiredLoginButton");

    let selectedFile = null;
    let previewUrl = null;
    let activeSessionId = "";
    let pendingUserMessage = null;
    let cachedLocation = null;
    let suppressScrollStateSync = 0;
    let autoScrollTaskVersion = 0;
    let chatTrailController = null;
    let sessionExpired = false;
    let sessionCheckInFlight = false;

    const sessions = [];
    const streamStates = new Map();

    async function loadChatVisualConfig() {
        try {
            const response = await fetch(`${basePath}/api/system-config/public/main-system`);
            if (!response.ok) {
                applyChatVisualConfig({});
                return;
            }
            const payload = await response.json();
            applyChatVisualConfig(payload.items || {});
        } catch (error) {
            applyChatVisualConfig({});
        }
    }

    function applyChatVisualConfig(items) {
        initChatCurvedGrid();
        initChatGridRunners();
        initChatMouseTrail((items && items[CHAT_CONFIG_KEYS.mouseTrailEffect]) || "music");
    }

    function initChatCurvedGrid() {
        if (document.querySelector(".chat-curved-grid-canvas")) {
            return;
        }

        const canvas = document.createElement("canvas");
        canvas.className = "chat-curved-grid-canvas";
        canvas.setAttribute("aria-hidden", "true");
        document.body.prepend(canvas);

        const context = canvas.getContext("2d");
        const gridSize = 56;
        let width = 0;
        let height = 0;
        let pixelRatio = 1;

        function resizeCanvas() {
            pixelRatio = Math.min(window.devicePixelRatio || 1, 2);
            width = window.innerWidth;
            height = window.innerHeight;
            canvas.width = Math.floor(width * pixelRatio);
            canvas.height = Math.floor(height * pixelRatio);
            canvas.style.width = `${width}px`;
            canvas.style.height = `${height}px`;
            context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);
            drawCurvedGrid();
        }

        function drawCurvedGrid() {
            context.clearRect(0, 0, width, height);
            context.save();
            context.strokeStyle = "rgba(54, 224, 92, 0.105)";
            context.lineWidth = 1;
            context.shadowBlur = 2;
            context.shadowColor = "rgba(25, 210, 54, 0.12)";

            for (let y = 0; y <= height + gridSize; y += gridSize) {
                const curve = Math.sin(y * 0.018) * 10;
                context.beginPath();
                context.moveTo(0, y);
                context.quadraticCurveTo(width * 0.5, y + curve, width, y);
                context.stroke();
            }

            for (let x = 0; x <= width + gridSize; x += gridSize) {
                const curve = Math.cos(x * 0.018) * 10;
                context.beginPath();
                context.moveTo(x, 0);
                context.quadraticCurveTo(x + curve, height * 0.5, x, height);
                context.stroke();
            }

            context.restore();
        }

        window.addEventListener("resize", resizeCanvas);
        resizeCanvas();
    }

    function initChatGridRunners() {
        if (window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
            return;
        }
        if (document.querySelector(".chat-grid-runner-canvas")) {
            return;
        }

        const canvas = document.createElement("canvas");
        canvas.className = "chat-grid-runner-canvas";
        canvas.setAttribute("aria-hidden", "true");
        document.body.prepend(canvas);

        const context = canvas.getContext("2d");
        const gridSize = 56;
        const runners = [];
        const colors = [
            "rgba(255, 38, 110, 0.66)",
            "rgba(54, 224, 92, 0.68)",
            "rgba(67, 235, 244, 0.62)",
            "rgba(251, 191, 36, 0.64)",
            "rgba(168, 85, 247, 0.6)",
            "rgba(59, 130, 246, 0.62)",
            "rgba(249, 115, 22, 0.6)",
            "rgba(236, 72, 153, 0.6)"
        ];
        let width = 0;
        let height = 0;
        let pixelRatio = 1;
        let lastSpawnAt = 0;

        function resizeCanvas() {
            pixelRatio = Math.min(window.devicePixelRatio || 1, 2);
            width = window.innerWidth;
            height = window.innerHeight;
            canvas.width = Math.floor(width * pixelRatio);
            canvas.height = Math.floor(height * pixelRatio);
            canvas.style.width = `${width}px`;
            canvas.style.height = `${height}px`;
            context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);
        }

        function createRunner() {
            const horizontal = Math.random() > 0.5;
            const maxLine = Math.max(1, Math.floor((horizontal ? height : width) / gridSize));
            const lineIndex = Math.floor(Math.random() * (maxLine + 1));
            const direction = Math.random() > 0.5 ? 1 : -1;
            const length = 90 + Math.random() * 180;
            const speed = 1.25 + Math.random() * 2.5;
            const start = direction > 0 ? -length : (horizontal ? width : height) + length;
            runners.push({
                horizontal,
                line: lineIndex * gridSize,
                position: start,
                direction,
                length,
                speed,
                life: 1,
                color: colors[Math.floor(Math.random() * colors.length)]
            });
        }

        function drawRunner(runner) {
            const start = runner.position;
            const end = runner.position - runner.direction * runner.length;
            const gradient = runner.horizontal
                ? context.createLinearGradient(end, runner.line, start, runner.line)
                : context.createLinearGradient(runner.line, end, runner.line, start);

            gradient.addColorStop(0, "rgba(255, 255, 255, 0)");
            gradient.addColorStop(0.45, runner.color);
            gradient.addColorStop(1, "rgba(255, 255, 255, 0)");

            context.save();
            context.globalAlpha = Math.max(0, runner.life) * 0.58;
            context.strokeStyle = gradient;
            context.lineWidth = 1.5;
            context.lineCap = "round";
            context.shadowBlur = 6;
            context.shadowColor = runner.color;
            context.beginPath();
            if (runner.horizontal) {
                context.moveTo(end, runner.line);
                context.lineTo(start, runner.line);
            } else {
                context.moveTo(runner.line, end);
                context.lineTo(runner.line, start);
            }
            context.stroke();
            context.restore();
        }

        function animate(now) {
            context.clearRect(0, 0, width, height);
            if (now - lastSpawnAt > 180 + Math.random() * 260 && runners.length < 28) {
                lastSpawnAt = now;
                createRunner();
            }

            for (let index = runners.length - 1; index >= 0; index -= 1) {
                const runner = runners[index];
                runner.position += runner.direction * runner.speed;
                const boundary = runner.horizontal ? width : height;
                if (runner.direction > 0 && runner.position - runner.length > boundary) {
                    runner.life -= 0.08;
                }
                if (runner.direction < 0 && runner.position + runner.length < 0) {
                    runner.life -= 0.08;
                }
                if (runner.life <= 0) {
                    runners.splice(index, 1);
                    continue;
                }
                drawRunner(runner);
            }
            requestAnimationFrame(animate);
        }

        window.addEventListener("resize", resizeCanvas);
        resizeCanvas();
        requestAnimationFrame(animate);
    }

    function setUserMenuOpen(open) {
        if (!userProfileButton || !userActionMenu) {
            return;
        }
        userActionMenu.hidden = !open;
        userProfileButton.setAttribute("aria-expanded", open ? "true" : "false");
    }

    function getAvatarInitial(name) {
        const normalized = String(name || "").trim();
        return normalized ? normalized.slice(0, 1).toUpperCase() : "H";
    }

    function renderUserAvatar(url, displayName) {
        if (!userAvatar) {
            return;
        }
        userAvatar.innerHTML = url
            ? `<img src="${escapeAttribute(url)}" alt="">`
            : getAvatarInitial(displayName);
    }

    async function loadCurrentUser() {
        if (!userDisplayName || !userAvatar) {
            return;
        }
        try {
            const response = await fetch(`${basePath}/auth/me`);
            if (response.status === 401) {
                showSessionExpiredDialog();
                return;
            }
            if (!response.ok) {
                return;
            }
            const user = await response.json();
            window.hephaestusCurrentLoginUser = user;
            let profile = null;
            if (user.personId) {
                profile = await loadCurrentUserProfile(user.personId);
            }
            const displayName = (profile && profile.personName) || user.personName || user.username || "Hephaestus";
            userDisplayName.textContent = displayName;
            renderUserAvatar(profile && profile.avatarAccessUrl ? profile.avatarAccessUrl : "", displayName);
        } catch (error) {
            // Keep the default local fallback if the session user cannot be loaded.
        }
    }

    function redirectToLogin() {
        window.location.href = `${basePath}/login.html`;
    }

    function showSessionExpiredDialog() {
        if (sessionExpired) {
            return;
        }
        sessionExpired = true;
        if (sessionExpiredDialog) {
            sessionExpiredDialog.hidden = false;
        }
        input.disabled = true;
        sendButton.disabled = true;
        chooseFileButton.disabled = true;
        removeFileButton.disabled = true;
        attachmentInput.disabled = true;
    }

    async function checkSessionStillActive() {
        if (sessionExpired || sessionCheckInFlight) {
            return;
        }
        sessionCheckInFlight = true;
        try {
            const response = await fetch(`${basePath}/auth/me`, {
                cache: "no-store",
                headers: { "X-Session-Check": "1" }
            });
            if (response.status === 401) {
                showSessionExpiredDialog();
            }
        } catch (error) {
            // Network hiccups should not force logout. The next check will retry.
        } finally {
            sessionCheckInFlight = false;
        }
    }

    async function loadCurrentUserProfile(personId) {
        try {
            const response = await fetch(`${basePath}/api/org/persons/current-scope`);
            if (!response.ok) {
                return null;
            }
            const scope = await response.json();
            return scope && scope.currentPerson ? scope.currentPerson : null;
        } catch (error) {
            return null;
        }
    }

    function initChatMouseTrail(effectName) {
        initChatMouseTrailRuntime(effectName);
        return;
        if (window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
            return;
        }
        const canvas = document.createElement("canvas");
        canvas.className = "chat-trail-canvas";
        canvas.setAttribute("aria-hidden", "true");
        document.body.prepend(canvas);

        const context = canvas.getContext("2d");
        const particles = [];
        const symbols = ["♪", "♫", "♬", "✦", "✧"];
        const colors = ["#2c8fb4", "#37b899", "#22c55e", "#06b6d4", "#ec4899"];
        const effect = String(effectName || "music").toLowerCase();
        let width = 0;
        let height = 0;
        let pixelRatio = 1;
        let lastSpawnAt = 0;

        function resizeCanvas() {
            pixelRatio = Math.min(window.devicePixelRatio || 1, 2);
            width = window.innerWidth;
            height = window.innerHeight;
            canvas.width = Math.floor(width * pixelRatio);
            canvas.height = Math.floor(height * pixelRatio);
            canvas.style.width = `${width}px`;
            canvas.style.height = `${height}px`;
            context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);
        }

        function randomColor() {
            return colors[Math.floor(Math.random() * colors.length)];
        }

        function spawnTrail(x, y, burst) {
            const now = performance.now();
            if (!burst && now - lastSpawnAt < 24) {
                return;
            }
            lastSpawnAt = now;
            const useDots = effect === "firefly" || effect === "firework";
            const count = burst || effect === "firework" ? 10 : 2;
            for (let index = 0; index < count; index += 1) {
                const angle = Math.random() * Math.PI * 2;
                const speed = 0.5 + Math.random() * 1.8;
                particles.push({
                    type: useDots ? "dot" : "text",
                    x: x + (Math.random() - 0.5) * 10,
                    y: y + (Math.random() - 0.5) * 10,
                    vx: effect === "firework" ? Math.cos(angle) * speed : (Math.random() - 0.5) * 1.1,
                    vy: effect === "firework" ? Math.sin(angle) * speed : -0.3 - Math.random() * 1.1,
                    life: 0.82,
                    decay: effect === "firework" ? 0.022 : 0.016 + Math.random() * 0.008,
                    size: 10 + Math.random() * 9,
                    radius: 2.2 + Math.random() * 3.8,
                    rotate: (Math.random() - 0.5) * 0.8,
                    symbol: symbols[Math.floor(Math.random() * symbols.length)],
                    color: randomColor()
                });
            }
            if (particles.length > 110) {
                particles.splice(0, particles.length - 110);
            }
        }

        function drawParticle(particle) {
            context.save();
            context.globalAlpha = Math.max(0, particle.life) * 0.42;
            context.shadowBlur = particle.type === "dot" ? 10 : 7;
            context.shadowColor = particle.color;
            context.fillStyle = particle.color;
            if (particle.type === "dot") {
                context.beginPath();
                context.arc(particle.x, particle.y, particle.radius, 0, Math.PI * 2);
                context.fill();
            } else {
                context.translate(particle.x, particle.y);
                context.rotate(particle.rotate);
                context.font = `${particle.size}px "Times New Roman", serif`;
                context.textAlign = "center";
                context.textBaseline = "middle";
                context.fillText(particle.symbol, 0, 0);
            }
            context.restore();
        }

        function animate() {
            context.clearRect(0, 0, width, height);
            for (let index = particles.length - 1; index >= 0; index -= 1) {
                const particle = particles[index];
                particle.x += particle.vx;
                particle.y += particle.vy;
                particle.vy += 0.012;
                particle.rotate += 0.01;
                particle.life -= particle.decay;
                if (particle.life <= 0) {
                    particles.splice(index, 1);
                    continue;
                }
                drawParticle(particle);
            }
            requestAnimationFrame(animate);
        }

        window.addEventListener("resize", resizeCanvas);
        window.addEventListener("mousemove", (event) => spawnTrail(event.clientX, event.clientY), { passive: true });
        window.addEventListener("click", (event) => spawnTrail(event.clientX, event.clientY, true), { passive: true });
        resizeCanvas();
        animate();
    }

    function initChatMouseTrailRuntime(effectName) {
        if (window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
            return;
        }
        const effect = normalizeTrailEffect(effectName);
        if (chatTrailController && chatTrailController.effect === effect) {
            return;
        }
        if (chatTrailController) {
            chatTrailController.destroy();
            chatTrailController = null;
        }

        const canvas = document.createElement("canvas");
        canvas.className = "chat-trail-canvas";
        canvas.setAttribute("aria-hidden", "true");
        document.body.prepend(canvas);

        const context = canvas.getContext("2d");
        const particles = [];
        const doodlePoints = [];
        const symbols = ["♪", "♫", "♬", "✦", "✧"];
        const colors = ["#2c8fb4", "#37b899", "#22c55e", "#06b6d4", "#ec4899", "#f59e0b", "#8b5cf6"];
        let width = 0;
        let height = 0;
        let pixelRatio = 1;
        let lastSpawnAt = 0;
        let cursor = null;
        let alive = true;
        let animationFrame = 0;

        function normalizeTrailEffect(value) {
            const normalized = String(value || "music").toLowerCase();
            const aliases = {
                "floating-lines": "flowing-lines",
                "big-data": "wave"
            };
            const candidate = aliases[normalized] || normalized;
            const supported = ["music", "flowing-lines", "meteor", "wave", "doodle", "constellation", "firefly", "firework", "magnifier"];
            return supported.includes(candidate) ? candidate : "music";
        }

        function resizeCanvas() {
            pixelRatio = Math.min(window.devicePixelRatio || 1, 2);
            width = window.innerWidth;
            height = window.innerHeight;
            canvas.width = Math.floor(width * pixelRatio);
            canvas.height = Math.floor(height * pixelRatio);
            canvas.style.width = `${width}px`;
            canvas.style.height = `${height}px`;
            context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);
        }

        function randomColor() {
            return colors[Math.floor(Math.random() * colors.length)];
        }

        function trimParticles(limit) {
            if (particles.length > limit) {
                particles.splice(0, particles.length - limit);
            }
        }

        function pushParticle(particle, limit) {
            particles.push(particle);
            trimParticles(limit);
        }

        function spawnMusic(x, y) {
            for (let index = 0; index < 2; index += 1) {
                pushParticle({
                    type: "text",
                    x: x + (Math.random() - 0.5) * 10,
                    y: y + (Math.random() - 0.5) * 10,
                    vx: (Math.random() - 0.5) * 1.1,
                    vy: -0.3 - Math.random() * 1.1,
                    life: 0.82,
                    decay: 0.016 + Math.random() * 0.008,
                    size: 10 + Math.random() * 9,
                    rotate: (Math.random() - 0.5) * 0.8,
                    symbol: symbols[Math.floor(Math.random() * symbols.length)],
                    color: randomColor()
                }, 110);
            }
        }

        function spawnFlowingLines(x, y) {
            for (let index = 0; index < 3; index += 1) {
                pushParticle({
                    type: "line",
                    x,
                    y,
                    vx: (Math.random() - 0.5) * 2.2,
                    vy: (Math.random() - 0.5) * 2.2,
                    angle: Math.random() * Math.PI * 2,
                    length: 18 + Math.random() * 34,
                    life: 0.88,
                    decay: 0.018 + Math.random() * 0.01,
                    width: 0.8 + Math.random() * 1.2,
                    color: randomColor()
                }, 120);
            }
        }

        function spawnMeteor(x, y) {
            for (let index = 0; index < 2; index += 1) {
                pushParticle({
                    type: "meteor",
                    x: x + (Math.random() - 0.5) * 24,
                    y: y + (Math.random() - 0.5) * 24,
                    vx: -1.9 - Math.random() * 1.8,
                    vy: 1.1 + Math.random() * 1.5,
                    length: 34 + Math.random() * 48,
                    life: 0.9,
                    decay: 0.02 + Math.random() * 0.01,
                    color: randomColor()
                }, 90);
            }
        }

        function spawnWave(x, y) {
            pushParticle({
                type: "ring",
                x,
                y,
                radius: 4,
                growth: 2.4 + Math.random() * 2.2,
                life: 0.86,
                decay: 0.022,
                color: randomColor()
            }, 70);
        }

        function spawnDoodle(x, y) {
            doodlePoints.push({ x, y, life: 0.8, color: randomColor() });
            if (doodlePoints.length > 42) {
                doodlePoints.splice(0, doodlePoints.length - 42);
            }
        }

        function spawnConstellation(x, y) {
            pushParticle({
                type: "point",
                x,
                y,
                vx: (Math.random() - 0.5) * 0.8,
                vy: (Math.random() - 0.5) * 0.8,
                radius: 1.8 + Math.random() * 2.2,
                life: 0.9,
                decay: 0.011 + Math.random() * 0.008,
                color: randomColor()
            }, 44);
        }

        function spawnFirefly(x, y) {
            for (let index = 0; index < 2; index += 1) {
                pushParticle({
                    type: "dot",
                    x: x + (Math.random() - 0.5) * 14,
                    y: y + (Math.random() - 0.5) * 14,
                    vx: (Math.random() - 0.5) * 1.1,
                    vy: -0.25 - Math.random() * 0.8,
                    radius: 2.6 + Math.random() * 4.2,
                    life: 0.82,
                    decay: 0.014 + Math.random() * 0.01,
                    color: randomColor()
                }, 130);
            }
        }

        function spawnFirework(x, y, burst) {
            const count = burst ? 26 : 7;
            for (let index = 0; index < count; index += 1) {
                const angle = Math.random() * Math.PI * 2;
                const speed = (burst ? 1.6 : 0.7) + Math.random() * (burst ? 2.9 : 1.5);
                pushParticle({
                    type: "spark",
                    x,
                    y,
                    vx: Math.cos(angle) * speed,
                    vy: Math.sin(angle) * speed,
                    radius: 1.6 + Math.random() * 2.5,
                    life: 0.86,
                    decay: (burst ? 0.016 : 0.024) + Math.random() * 0.012,
                    color: randomColor()
                }, 180);
            }
        }

        function spawnTrail(x, y, burst) {
            cursor = { x, y };
            if (effect === "magnifier") {
                return;
            }
            const now = performance.now();
            if (!burst && now - lastSpawnAt < 18) {
                return;
            }
            lastSpawnAt = now;
            if (effect === "flowing-lines") {
                spawnFlowingLines(x, y);
            } else if (effect === "meteor") {
                spawnMeteor(x, y);
            } else if (effect === "wave") {
                spawnWave(x, y);
            } else if (effect === "doodle") {
                spawnDoodle(x, y);
            } else if (effect === "constellation") {
                spawnConstellation(x, y);
            } else if (effect === "firefly") {
                spawnFirefly(x, y);
            } else if (effect === "firework") {
                spawnFirework(x, y, burst);
            } else {
                spawnMusic(x, y);
            }
        }

        function drawParticle(particle) {
            const alpha = Math.max(0, particle.life);
            context.save();
            context.globalAlpha = alpha * 0.48;
            context.shadowColor = particle.color;
            context.strokeStyle = particle.color;
            context.fillStyle = particle.color;
            if (particle.type === "text") {
                const scale = 0.55 + alpha * 0.45;
                context.translate(particle.x, particle.y);
                context.rotate(particle.rotate);
                context.scale(scale, scale);
                context.shadowBlur = 7;
                context.font = `${particle.size}px "Times New Roman", serif`;
                context.textAlign = "center";
                context.textBaseline = "middle";
                context.fillText(particle.symbol, 0, 0);
            } else if (particle.type === "line") {
                const dx = Math.cos(particle.angle) * particle.length;
                const dy = Math.sin(particle.angle) * particle.length;
                context.lineWidth = particle.width;
                context.shadowBlur = 8;
                context.beginPath();
                context.moveTo(particle.x - dx * 0.5, particle.y - dy * 0.5);
                context.lineTo(particle.x + dx * 0.5, particle.y + dy * 0.5);
                context.stroke();
            } else if (particle.type === "meteor") {
                context.lineWidth = 1.8;
                context.shadowBlur = 12;
                const gradient = context.createLinearGradient(particle.x, particle.y, particle.x - particle.vx * particle.length, particle.y - particle.vy * particle.length);
                gradient.addColorStop(0, particle.color);
                gradient.addColorStop(1, "rgba(255,255,255,0)");
                context.strokeStyle = gradient;
                context.beginPath();
                context.moveTo(particle.x, particle.y);
                context.lineTo(particle.x - particle.vx * particle.length, particle.y - particle.vy * particle.length);
                context.stroke();
            } else if (particle.type === "ring") {
                context.lineWidth = 1.6;
                context.shadowBlur = 9;
                context.beginPath();
                context.arc(particle.x, particle.y, particle.radius, 0, Math.PI * 2);
                context.stroke();
            } else {
                context.shadowBlur = particle.type === "dot" ? 10 : 8;
                context.beginPath();
                context.arc(particle.x, particle.y, particle.radius, 0, Math.PI * 2);
                context.fill();
            }
            context.restore();
        }

        function drawConstellationLines() {
            if (effect !== "constellation") {
                return;
            }
            for (let leftIndex = 0; leftIndex < particles.length; leftIndex += 1) {
                for (let rightIndex = leftIndex + 1; rightIndex < particles.length; rightIndex += 1) {
                    const left = particles[leftIndex];
                    const right = particles[rightIndex];
                    const distance = Math.hypot(left.x - right.x, left.y - right.y);
                    if (distance > 108) {
                        continue;
                    }
                    context.save();
                    context.globalAlpha = Math.min(left.life, right.life, 1 - distance / 108) * 0.42;
                    context.strokeStyle = left.color;
                    context.lineWidth = 1;
                    context.beginPath();
                    context.moveTo(left.x, left.y);
                    context.lineTo(right.x, right.y);
                    context.stroke();
                    context.restore();
                }
            }
        }

        function drawDoodle() {
            if (effect !== "doodle") {
                return;
            }
            for (let index = doodlePoints.length - 1; index >= 0; index -= 1) {
                doodlePoints[index].life -= 0.018;
                if (doodlePoints[index].life <= 0) {
                    doodlePoints.splice(index, 1);
                }
            }
            for (let index = 1; index < doodlePoints.length; index += 1) {
                const previous = doodlePoints[index - 1];
                const point = doodlePoints[index];
                context.save();
                context.globalAlpha = Math.min(previous.life, point.life) * 0.42;
                context.strokeStyle = point.color;
                context.lineWidth = 2.2;
                context.lineCap = "round";
                context.shadowBlur = 8;
                context.shadowColor = point.color;
                context.beginPath();
                context.moveTo(previous.x, previous.y);
                context.lineTo(point.x, point.y);
                context.stroke();
                context.restore();
            }
        }

        function drawMagnifier() {
            if (effect !== "magnifier" || !cursor) {
                return;
            }
            const radius = 50;
            context.save();
            context.beginPath();
            context.arc(cursor.x, cursor.y, radius, 0, Math.PI * 2);
            context.clip();
            context.fillStyle = "rgba(255,255,255,0.10)";
            context.fillRect(cursor.x - radius, cursor.y - radius, radius * 2, radius * 2);
            context.strokeStyle = "rgba(44,143,180,0.14)";
            context.lineWidth = 1;
            for (let offset = -radius; offset <= radius; offset += 12) {
                context.beginPath();
                context.moveTo(cursor.x - radius, cursor.y + offset);
                context.lineTo(cursor.x + radius, cursor.y + offset);
                context.stroke();
                context.beginPath();
                context.moveTo(cursor.x + offset, cursor.y - radius);
                context.lineTo(cursor.x + offset, cursor.y + radius);
                context.stroke();
            }
            context.restore();
            context.save();
            context.strokeStyle = "rgba(44,143,180,0.42)";
            context.lineWidth = 2;
            context.shadowBlur = 10;
            context.shadowColor = "#2c8fb4";
            context.beginPath();
            context.arc(cursor.x, cursor.y, radius, 0, Math.PI * 2);
            context.stroke();
            context.restore();
        }

        function updateParticle(particle) {
            particle.x += particle.vx || 0;
            particle.y += particle.vy || 0;
            if (particle.type === "text") {
                particle.vy += 0.012;
                particle.rotate += 0.01;
            } else if (particle.type === "spark") {
                particle.vy += 0.035;
            } else if (particle.type === "ring") {
                particle.radius += particle.growth;
            }
            particle.life -= particle.decay;
        }

        function animate() {
            if (!alive) {
                return;
            }
            context.clearRect(0, 0, width, height);
            drawDoodle();
            drawConstellationLines();
            for (let index = particles.length - 1; index >= 0; index -= 1) {
                const particle = particles[index];
                updateParticle(particle);
                if (particle.life <= 0) {
                    particles.splice(index, 1);
                    continue;
                }
                drawParticle(particle);
            }
            drawMagnifier();
            animationFrame = requestAnimationFrame(animate);
        }

        function handleMouseMove(event) {
            spawnTrail(event.clientX, event.clientY);
        }

        function handleClick(event) {
            spawnTrail(event.clientX, event.clientY, true);
        }

        function handleTouch(event) {
            const touch = event.touches && event.touches[0];
            if (touch) {
                spawnTrail(touch.clientX, touch.clientY);
            }
        }

        window.addEventListener("resize", resizeCanvas);
        window.addEventListener("mousemove", handleMouseMove, { passive: true });
        window.addEventListener("click", handleClick, { passive: true });
        window.addEventListener("touchmove", handleTouch, { passive: true });
        window.addEventListener("touchstart", handleTouch, { passive: true });
        resizeCanvas();
        animationFrame = requestAnimationFrame(animate);
        chatTrailController = {
            effect,
            destroy() {
                alive = false;
                cancelAnimationFrame(animationFrame);
                window.removeEventListener("resize", resizeCanvas);
                window.removeEventListener("mousemove", handleMouseMove);
                window.removeEventListener("click", handleClick);
                window.removeEventListener("touchmove", handleTouch);
                window.removeEventListener("touchstart", handleTouch);
                canvas.remove();
            }
        };
    }

    function createSessionId() {
        return `hephaestus_${Date.now()}_${Math.random().toString(16).slice(2, 8)}`;
    }

    function createSession(title) {
        return {
            id: createSessionId(),
            title: title || DEFAULT_SESSION_TITLE,
            createdAt: Date.now(),
            messagesHtml: EMPTY_STATE_HTML,
            scrollTop: 0,
            autoScroll: true
        };
    }

    function getActiveSession() {
        return sessions.find((item) => item.id === activeSessionId) || null;
    }

    function getSessionStreamState(sessionId) {
        return streamStates.get(sessionId) || null;
    }

    function isSessionStreaming(sessionId) {
        const state = getSessionStreamState(sessionId);
        if (!state) {
            return false;
        }
        const startedAt = Number(state.startedAt || 0);
        if (startedAt > 0 && Date.now() - startedAt > 5 * 60 * 1000) {
            streamStates.delete(sessionId);
            return false;
        }
        return true;
    }

    function renderEmptyState() {
        messages.innerHTML = EMPTY_STATE_HTML;
        syncConversationLayoutState();
    }

    function syncConversationLayoutState() {
        if (!main) {
            return;
        }
        main.classList.toggle("is-empty-session", Boolean(messages.querySelector(".empty")));
    }

    function ensureMessagesInner() {
        let inner = messages.querySelector(".messages-inner");
        if (!inner) {
            renderEmptyState();
            inner = messages.querySelector(".messages-inner");
        }
        return inner;
    }

    function clearEmptyState() {
        const empty = messages.querySelector(".empty");
        if (empty) {
            empty.remove();
            syncConversationLayoutState();
        }
    }

    function formatSessionTime(timestamp) {
        const date = new Date(timestamp);
        const now = new Date();
        if (date.toDateString() === now.toDateString()) {
            return date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
        }
        return date.toLocaleDateString("zh-CN", { month: "numeric", day: "numeric" });
    }

    function isNearBottom() {
        const threshold = 48;
        return messages.scrollHeight - messages.clientHeight - messages.scrollTop <= threshold;
    }

    function scrollMessagesToBottom() {
        suppressScrollStateSync += 1;
        messages.scrollTop = messages.scrollHeight;
        requestAnimationFrame(() => {
            suppressScrollStateSync = Math.max(0, suppressScrollStateSync - 1);
        });
    }

    function scrollMessageIntoView(messageView) {
        if (!messageView || !messageView.row) {
            return;
        }
        const rowRect = messageView.row.getBoundingClientRect();
        const messagesRect = messages.getBoundingClientRect();
        const composerRect = form.getBoundingClientRect();
        const liveRect = liveStatusText && !liveStatusText.hidden
            ? liveStatusText.getBoundingClientRect()
            : null;
        const obstructionTop = liveRect ? liveRect.top : composerRect.top;
        const targetBottom = obstructionTop - 16;
        const overflow = rowRect.bottom - targetBottom;
        const underflow = messagesRect.top - rowRect.top;

        if (overflow > 0) {
            suppressScrollStateSync += 1;
            messages.scrollTop += overflow;
            requestAnimationFrame(() => {
                suppressScrollStateSync = Math.max(0, suppressScrollStateSync - 1);
            });
        } else if (underflow > 0) {
            suppressScrollStateSync += 1;
            messages.scrollTop -= underflow;
            requestAnimationFrame(() => {
                suppressScrollStateSync = Math.max(0, suppressScrollStateSync - 1);
            });
        }
    }

    function stickMessageToBottom(messageView) {
        if (!messageView || !messageView.row) {
            requestAnimationFrame(scrollMessagesToBottom);
            return;
        }
        requestAnimationFrame(() => scrollMessageIntoView(messageView));
    }

    function settleMessageIntoView(messageView, remainingFrames = 6, taskVersion = autoScrollTaskVersion) {
        if (remainingFrames <= 0) {
            return;
        }
        requestAnimationFrame(() => {
            if (taskVersion !== autoScrollTaskVersion) {
                return;
            }
            const activeSession = getActiveSession();
            if (!activeSession || activeSession.autoScroll) {
                if (messageView && messageView.row && messageView.row.isConnected) {
                    scrollMessageIntoView(messageView);
                } else {
                    scrollMessagesToBottom();
                }
                saveActiveSessionSnapshot();
            }
            settleMessageIntoView(messageView, remainingFrames - 1, taskVersion);
        });
    }

    function stopAutoScrollFollowing() {
        autoScrollTaskVersion += 1;
        const session = getActiveSession();
        if (!session) {
            return;
        }
        session.scrollTop = messages.scrollTop;
        session.autoScroll = false;
    }

    function stickToBottomAfterImageLoad(img) {
        const tryScroll = () => {
            const activeSession = getActiveSession();
            if (!activeSession || activeSession.autoScroll) {
                if (pendingUserMessage && pendingUserMessage.row && pendingUserMessage.row.contains(img)) {
                    scrollMessageIntoView(pendingUserMessage);
                    settleMessageIntoView(pendingUserMessage, 4);
                } else {
                    scrollMessagesToBottom();
                }
                saveActiveSessionSnapshot();
            }
        };

        if (img.complete) {
            requestAnimationFrame(tryScroll);
            return;
        }

        const handleLoad = () => {
            requestAnimationFrame(tryScroll);
        };

        img.addEventListener("load", handleLoad, { once: true });
        img.addEventListener("error", handleLoad, { once: true });
    }

    function resizeInput() {
        input.style.height = "auto";
        input.style.height = `${Math.min(input.scrollHeight, 120)}px`;
    }

    function saveActiveSessionSnapshot() {
        const session = getActiveSession();
        if (!session) {
            return;
        }
        session.messagesHtml = messages.innerHTML || EMPTY_STATE_HTML;
        session.scrollTop = messages.scrollTop;
    }

    function syncActiveSessionScrollState() {
        const session = getActiveSession();
        if (!session) {
            return;
        }
        session.scrollTop = messages.scrollTop;
        session.autoScroll = isNearBottom();
    }

    function renderSessionList() {
        sessionList.innerHTML = "";
        sessions.forEach((session) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = `session-item${session.id === activeSessionId ? " active" : ""}`;
            button.dataset.sessionId = session.id;

            const title = document.createElement("span");
            title.className = "session-title";
            title.textContent = session.title;

            const meta = document.createElement("span");
            meta.className = "session-meta";
            meta.textContent = formatSessionTime(session.createdAt);

            button.append(title, meta);
            sessionList.appendChild(button);
        });
    }

    function showSession(session) {
        messages.innerHTML = session && session.messagesHtml ? session.messagesHtml : EMPTY_STATE_HTML;
        syncConversationLayoutState();
        pendingUserMessage = null;
        requestAnimationFrame(() => {
            if (!session) {
                scrollMessagesToBottom();
                return;
            }
            if (session.autoScroll) {
                scrollMessagesToBottom();
            } else {
                messages.scrollTop = session.scrollTop || 0;
            }
        });
    }

    function isConnectedMessageView(messageView) {
        return Boolean(messageView && messageView.row && messageView.row.isConnected);
    }

    function buildMessageViewFromDom(row) {
        if (!row) {
            return null;
        }

        const bubble = row.querySelector(".bubble");
        const content = row.querySelector(".bubble-content");
        if (!bubble || !content) {
            return null;
        }

        let status = bubble.querySelector(".status-note");
        if (!status) {
            status = document.createElement("div");
            status.className = "status-note";
            bubble.insertBefore(status, content);
        }

        return {
            row,
            bubble,
            status,
            content,
            mediaContainer: content.nextElementSibling || null,
            text: content.textContent || "",
            renderedText: content.textContent || "",
            renderFrame: 0
        };
    }

    function renderAssistantContentNow(messageView) {
        if (!messageView || !messageView.content) {
            return;
        }
        const nextText = messageView.text || "";
        if (messageView.renderedText === nextText) {
            return;
        }
        messageView.content.innerHTML = renderRichText(nextText);
        messageView.renderedText = nextText;
    }

    function scheduleAssistantContentRender(messageView) {
        if (!messageView || messageView.renderFrame) {
            return;
        }
        messageView.renderFrame = requestAnimationFrame(() => {
            messageView.renderFrame = 0;
            renderAssistantContentNow(messageView);
            const activeSession = getActiveSession();
            if (!activeSession || activeSession.autoScroll) {
                scrollMessagesToBottom();
            }
            saveActiveSessionSnapshot();
        });
    }

    function rebindStreamAssistantView(sessionId) {
        if (!sessionId || sessionId !== activeSessionId) {
            return null;
        }

        const state = streamStates.get(sessionId);
        if (!state) {
            return null;
        }

        const assistantRows = messages.querySelectorAll(".message-row.assistant");
        let assistantView = buildMessageViewFromDom(assistantRows[assistantRows.length - 1] || null);
        if (!assistantView) {
            assistantView = createStreamingAssistantMessage();
        }

        assistantView.text = state.text || "";
        assistantView.status.textContent = state.status || "";
        renderAssistantContentNow(assistantView);

        if (state.attachments && state.attachments.length) {
            const container = ensureMediaContainer(assistantView);
            container.replaceChildren(renderMediaList(state.attachments));
            assistantView.mediaContainer = container;
        }

        state.assistantView = assistantView;
        return assistantView;
    }

    function getActiveStreamAssistantView(sessionId, fallbackAssistant) {
        if (sessionId !== activeSessionId) {
            return null;
        }

        if (isConnectedMessageView(fallbackAssistant)) {
            const state = streamStates.get(sessionId);
            if (state) {
                state.assistantView = fallbackAssistant;
            }
            return fallbackAssistant;
        }

        const state = streamStates.get(sessionId);
        if (state && isConnectedMessageView(state.assistantView)) {
            return state.assistantView;
        }

        return rebindStreamAssistantView(sessionId);
    }

    function activateSession(sessionId) {
        if (!sessionId || sessionId === activeSessionId) {
            return;
        }
        saveActiveSessionSnapshot();
        activeSessionId = sessionId;
        showSession(getActiveSession());
        rebindStreamAssistantView(activeSessionId);
        clearAttachment();
        input.value = "";
        resizeInput();
        renderSessionList();
        syncComposerState();
        input.focus();
    }

    function createSessionAndActivate() {
        saveActiveSessionSnapshot();
        const session = createSession();
        sessions.unshift(session);
        activeSessionId = session.id;
        showSession(session);
        clearAttachment();
        input.value = "";
        resizeInput();
        renderSessionList();
        syncComposerState();
        input.focus();
    }

    function shouldSendLocation(message) {
        const normalized = (message || "").trim().toLowerCase();
        if (!normalized) {
            return false;
        }
        return [
            "天气",
            "气温",
            "下雨",
            "温度",
            "穿什么",
            "冷不冷",
            "热不热",
            "附近",
            "当前位置",
            "定位"
        ].some((keyword) => normalized.includes(keyword));
    }

    function loadCachedLocation() {
        if (cachedLocation) {
            return cachedLocation;
        }
        try {
            const raw = window.sessionStorage.getItem("hephaestus_user_location");
            cachedLocation = raw ? JSON.parse(raw) : null;
        } catch (error) {
            cachedLocation = null;
        }
        return cachedLocation;
    }

    function saveCachedLocation(location) {
        cachedLocation = location;
        try {
            if (location) {
                window.sessionStorage.setItem("hephaestus_user_location", JSON.stringify(location));
            } else {
                window.sessionStorage.removeItem("hephaestus_user_location");
            }
        } catch (error) {
            cachedLocation = location;
        }
    }

    function resolveUserLocation() {
        const existing = loadCachedLocation();
        if (existing) {
            return Promise.resolve(existing);
        }
        if (!navigator.geolocation) {
            return Promise.resolve(null);
        }
        return new Promise((resolve) => {
            navigator.geolocation.getCurrentPosition(
                (position) => {
                    const location = {
                        latitude: String(position.coords.latitude),
                        longitude: String(position.coords.longitude)
                    };
                    saveCachedLocation(location);
                    resolve(location);
                },
                () => resolve(null),
                {
                    enableHighAccuracy: false,
                    timeout: 3000,
                    maximumAge: 10 * 60 * 1000
                }
            );
        });
    }

    function appendLocationToMessage(message, location) {
        if (!location || !location.latitude || !location.longitude) {
            return message;
        }
        const normalized = (message || "").trim();
        const suffix = `\n\n当前坐标：纬度 ${location.latitude}，经度 ${location.longitude}`;
        return normalized ? `${normalized}${suffix}` : suffix.trim();
    }

    function updateSessionTitleFromMessage(message, hasAttachment) {
        const session = getActiveSession();
        if (!session || session.title !== DEFAULT_SESSION_TITLE) {
            return;
        }
        const trimmed = (message || "").trim();
        const source = trimmed || (hasAttachment ? "附件对话" : DEFAULT_SESSION_TITLE);
        session.title = source.length > 18 ? `${source.slice(0, 18)}…` : source;
        renderSessionList();
    }

    function formatFileSize(bytes) {
        if (!bytes) {
            return "0 B";
        }
        if (bytes < 1024) {
            return `${bytes} B`;
        }
        if (bytes < 1024 * 1024) {
            return `${(bytes / 1024).toFixed(1)} KB`;
        }
        return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
    }

    function buildLocalMediaItem(file, objectUrl) {
        return {
            fileName: file.name || "附件",
            contentType: file.type || "application/octet-stream",
            fileSize: file.size || 0,
            url: objectUrl || "#",
            downloadUrl: objectUrl || "#",
            localPreview: true
        };
    }

    function renderMediaList(items) {
        const list = document.createElement("div");
        list.className = "media-list";
        items.forEach((item) => {
            const card = document.createElement("div");
            card.className = "media-card";

            if ((item.contentType || "").startsWith("image/")) {
                const img = document.createElement("img");
                img.src = item.url;
                img.alt = "图片";
                stickToBottomAfterImageLoad(img);
                card.appendChild(img);
            } else {
                const fileCard = document.createElement("div");
                fileCard.className = "file-card";

                const meta = document.createElement("div");
                meta.className = "file-meta";

                const name = document.createElement("span");
                name.className = "file-name";
                name.textContent = item.fileName || "附件";

                const size = document.createElement("span");
                size.className = "file-size";
                size.textContent = `${formatFileSize(item.fileSize || 0)} · ${item.contentType || "文件"}`;

                const link = document.createElement("a");
                link.href = item.downloadUrl || item.url || "#";
                link.target = "_blank";
                link.rel = "noreferrer";
                link.textContent = "打开";

                meta.append(name, size);
                fileCard.append(meta, link);
                card.appendChild(fileCard);
            }

            list.appendChild(card);
        });
        return list;
    }

    function renderMediaListHtml(items) {
        const host = document.createElement("div");
        host.appendChild(renderMediaList(items));
        return host.innerHTML;
    }

    function appendMessage(role, text, mediaItems) {
        const shouldStickBottom = isNearBottom();
        clearEmptyState();
        const inner = ensureMessagesInner();
        const row = document.createElement("div");
        row.className = `message-row ${role}`;

        const bubble = document.createElement("div");
        bubble.className = "bubble";

        if (role === "assistant") {
            const name = document.createElement("div");
            name.className = "message-name";
            name.textContent = "Hephaestus";
            bubble.appendChild(name);
        }

        const content = document.createElement("div");
        content.className = "bubble-content";
        content.innerHTML = renderRichText(text || "");
        bubble.appendChild(content);

        let mediaContainer = null;
        if (mediaItems && mediaItems.length > 0) {
            mediaContainer = document.createElement("div");
            mediaContainer.appendChild(renderMediaList(mediaItems));
            bubble.appendChild(mediaContainer);
        }

        row.appendChild(bubble);
        inner.appendChild(row);
        if (shouldStickBottom) {
            scrollMessagesToBottom();
        }
        saveActiveSessionSnapshot();
        return { row, bubble, content, mediaContainer };
    }

    function createStreamingAssistantMessage() {
        const shouldStickBottom = isNearBottom();
        clearEmptyState();
        const inner = ensureMessagesInner();
        const row = document.createElement("div");
        row.className = "message-row assistant";

        const bubble = document.createElement("div");
        bubble.className = "bubble";

        const name = document.createElement("div");
        name.className = "message-name";
        name.textContent = "Hephaestus";

        const status = document.createElement("div");
        status.className = "status-note";

        const content = document.createElement("div");
        content.className = "bubble-content";

        bubble.append(name, status, content);
        row.appendChild(bubble);
        inner.appendChild(row);
        if (shouldStickBottom) {
            scrollMessagesToBottom();
        }
        saveActiveSessionSnapshot();
        return {
            row,
            bubble,
            status,
            content,
            mediaContainer: null,
            text: "",
            renderedText: "",
            renderFrame: 0
        };
    }

    function ensureMediaContainer(messageView) {
        if (!messageView.mediaContainer) {
            messageView.mediaContainer = document.createElement("div");
            messageView.bubble.appendChild(messageView.mediaContainer);
        }
        return messageView.mediaContainer;
    }

    function syncComposerState() {
        const isSending = Boolean(getSessionStreamState(activeSessionId));
        sendButton.disabled = isSending;
        input.disabled = false;
        chooseFileButton.disabled = false;
        removeFileButton.disabled = false;
        attachmentInput.disabled = false;
        if (!isSending && liveStatusText) {
            liveStatusText.classList.remove("is-active");
            liveStatusText.hidden = true;
        }
        if (!isSending && liveStatusTrack) {
            liveStatusTrack.textContent = "";
        }
        statusText.classList.toggle("busy", isSending);
        statusProgress.hidden = true;
    }

    function syncLiveStatusAnimation() {
        if (!liveStatusText || !liveStatusTrack || liveStatusText.hidden) {
            return;
        }

        const viewport = liveStatusText.querySelector(".composer-status__viewport");
        if (!viewport) {
            return;
        }

        const viewportWidth = viewport.clientWidth || 0;
        const trackWidth = liveStatusTrack.scrollWidth || 0;
        if (viewportWidth <= 0 || trackWidth <= 0) {
            return;
        }

        liveStatusTrack.style.setProperty("--status-start-x", `${viewportWidth}px`);
        liveStatusTrack.style.setProperty("--status-end-x", `${-trackWidth}px`);
        liveStatusTrack.style.setProperty("--status-duration", `${Math.max(4, Math.min(10, (viewportWidth + trackWidth) / 80))}s`);
    }

    function showLiveStatus(message) {
        if (!liveStatusText || !liveStatusTrack) {
            return;
        }
        const shouldStickBottom = isNearBottom();
        liveStatusTrack.textContent = message || "";
        liveStatusText.hidden = !message;
        liveStatusText.classList.toggle("is-active", Boolean(message));
        requestAnimationFrame(syncLiveStatusAnimation);
        if (shouldStickBottom) {
            requestAnimationFrame(() => {
                if (pendingUserMessage) {
                    scrollMessageIntoView(pendingUserMessage);
                    settleMessageIntoView(pendingUserMessage, 4);
                } else {
                    scrollMessagesToBottom();
                }
            });
        }
    }

    function clearLiveStatus() {
        if (!liveStatusText || !liveStatusTrack) {
            return;
        }
        const shouldStickBottom = isNearBottom();
        liveStatusTrack.style.removeProperty("--status-start-x");
        liveStatusTrack.style.removeProperty("--status-end-x");
        liveStatusTrack.style.removeProperty("--status-duration");
        liveStatusTrack.textContent = "";
        liveStatusText.classList.remove("is-active");
        liveStatusText.hidden = true;
        if (shouldStickBottom) {
            requestAnimationFrame(scrollMessagesToBottom);
        }
    }

    function mapLiveStatusMessage(phase, message, hasAttachment) {
        if (phase === "accepted" || phase === "analyzing") {
            return hasAttachment ? "正在分析附件" : "正在分析消息";
        }
        if (phase === "image_generating") {
            return "正在生成图片";
        }
        return message || "";
    }

    function buildAssistantRowHtml(text, status, mediaItems) {
        const statusHtml = status ? `<div class="status-note">${escapeHtml(status)}</div>` : "";
        const mediaHtml = mediaItems && mediaItems.length
            ? `<div>${renderMediaListHtml(mediaItems)}</div>`
            : "";
        return `<div class="message-row assistant"><div class="bubble"><div class="message-name">Hephaestus</div>${statusHtml}<div class="bubble-content">${renderRichText(text || "")}</div>${mediaHtml}</div></div>`;
    }

    function updateStreamStateSnapshot(sessionId) {
        const session = sessions.find((item) => item.id === sessionId);
        const state = streamStates.get(sessionId);
        if (!session || !state) {
            return;
        }
        const host = document.createElement("div");
        host.innerHTML = state.baseHtml || EMPTY_STATE_HTML;
        let inner = host.querySelector(".messages-inner");
        if (!inner) {
            host.innerHTML = EMPTY_STATE_HTML;
            inner = host.querySelector(".messages-inner");
        }
        const rowHost = document.createElement("div");
        rowHost.innerHTML = buildAssistantRowHtml(state.text, state.status, state.attachments);
        inner.appendChild(rowHost.firstElementChild);
        session.messagesHtml = host.innerHTML;
        if (sessionId === activeSessionId) {
            session.scrollTop = messages.scrollTop;
            session.autoScroll = isNearBottom();
        }
    }

    function finalizeStreamState(sessionId) {
        updateStreamStateSnapshot(sessionId);
        streamStates.delete(sessionId);
        if (sessionId === activeSessionId) {
            syncComposerState();
        }
    }

    function escapeHtml(text) {
        return String(text)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function escapeAttribute(text) {
        return escapeHtml(text);
    }

    function renderRichText(text) {
        if (!text) {
            return "";
        }

        const safe = escapeHtml(text);
        const codeBlocks = [];
        const withCodePlaceholders = safe.replace(/```([a-zA-Z0-9_-]+)?\n([\s\S]*?)```/g, (_, lang, code) => {
            const label = lang ? `<div>${escapeHtml(lang)}</div>` : "";
            const html = `<pre><code>${label}${code.trim()}</code></pre>`;
            const token = `__CODE_BLOCK_${codeBlocks.length}__`;
            codeBlocks.push(html);
            return token;
        });

        const withInlineCode = withCodePlaceholders.replace(/`([^`]+)`/g, "<code>$1</code>");
        return withInlineCode
            .split(/\n{2,}/)
            .map((part) => {
                if (/^__CODE_BLOCK_\d+__$/.test(part.trim())) {
                    const index = Number(part.trim().match(/\d+/)[0]);
                    return codeBlocks[index];
                }

                const lines = part.split("\n");
                if (lines.every((line) => /^[-*]\s+/.test(line))) {
                    return `<ul>${lines.map((line) => `<li>${line.replace(/^[-*]\s+/, "")}</li>`).join("")}</ul>`;
                }
                if (lines.every((line) => /^\d+\.\s+/.test(line))) {
                    return `<ol>${lines.map((line) => `<li>${line.replace(/^\d+\.\s+/, "")}</li>`).join("")}</ol>`;
                }
                return `<p>${lines.join("<br>")}</p>`;
            })
            .join("")
            .replace(/__CODE_BLOCK_(\d+)__/g, (_, index) => codeBlocks[Number(index)] || "");
    }

    function showAttachmentChip(file) {
        attachmentName.textContent = file.name;
        attachmentSize.textContent = `${formatFileSize(file.size)} · ${file.type || "文件"}`;
        attachmentThumb.textContent = "+";

        if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
            previewUrl = null;
        }

        if ((file.type || "").startsWith("image/")) {
            previewUrl = URL.createObjectURL(file);
            const img = document.createElement("img");
            img.src = previewUrl;
            img.alt = file.name;
            attachmentThumb.textContent = "";
            attachmentThumb.replaceChildren(img);
        } else {
            attachmentThumb.replaceChildren(document.createTextNode("文"));
        }

        attachmentChip.classList.add("visible");
    }

    function clearAttachment() {
        selectedFile = null;
        attachmentInput.value = "";
        attachmentChip.classList.remove("visible");
        attachmentName.textContent = "";
        attachmentSize.textContent = "";
        attachmentThumb.replaceChildren(document.createTextNode("+"));

        if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
            previewUrl = null;
        }
    }

    async function sendMessage(message) {
        const requestSessionId = activeSessionId;
        const content = message.trim();
        const fileForRequest = selectedFile;

        if (isSessionStreaming(requestSessionId)) {
            appendMessage("error", "当前会话仍在回复中，请稍等片刻后再发送。");
            return;
        }

        if (!content && !fileForRequest) {
            input.focus();
            return;
        }

        if (fileForRequest && fileForRequest.size > 2 * 1024 * 1024) {
            appendMessage("error", `附件“${fileForRequest.name}”大小 ${formatFileSize(fileForRequest.size)}，超过 2MB 限制，请压缩后重试。`);
            clearAttachment();
            input.focus();
            return;
        }

        updateSessionTitleFromMessage(content, Boolean(fileForRequest));

        let localPreviewUrl = null;
        let optimisticMedia = [];
        if (fileForRequest) {
            localPreviewUrl = URL.createObjectURL(fileForRequest);
            optimisticMedia = [buildLocalMediaItem(fileForRequest, localPreviewUrl)];
        }

        pendingUserMessage = appendMessage("user", content || "", optimisticMedia);
        const activeSession = getActiveSession();
        if (activeSession) {
            activeSession.autoScroll = true;
        }
        input.value = "";
        resizeInput();
        clearAttachment();
        requestAnimationFrame(() => scrollMessageIntoView(pendingUserMessage));
        settleMessageIntoView(pendingUserMessage);

        const initialMessagesHtml = messages.innerHTML;
        streamStates.set(requestSessionId, {
            baseHtml: initialMessagesHtml,
            text: "",
            status: "",
            phase: "",
            attachments: [],
            assistantView: null,
            requestHasAttachment: Boolean(fileForRequest),
            startedAt: Date.now()
        });

        const initialStatus = fileForRequest ? "正在上传附件…" : "正在发送消息…";
        await new Promise((resolve) => requestAnimationFrame(resolve));

        try {
            const formData = new FormData();
            const location = shouldSendLocation(content) ? await resolveUserLocation() : null;
            formData.append("message", appendLocationToMessage(content, location));
            if (fileForRequest) {
                formData.append("file", fileForRequest);
            }

            const state = streamStates.get(requestSessionId);
            if (state) {
                state.status = initialStatus;
            }
            if (requestSessionId === activeSessionId) {
                showLiveStatus(fileForRequest ? "正在分析附件" : "正在分析消息");
                syncComposerState();
            }

            const response = await fetch(streamUrl, {
                method: "POST",
                headers: {
                    "X-Session-Id": requestSessionId
                },
                body: formData
            });

            if (response.status === 401) {
                showSessionExpiredDialog();
                return;
            }
            if (!response.ok || !response.body) {
                throw new Error("发送失败，请稍后重试。");
            }

            if (state) {
                state.status = "正在等待回复…";
            }
            if (requestSessionId === activeSessionId) {
                syncComposerState();
            }

            await consumeEventStream(response, requestSessionId);
        } catch (error) {
            appendMessage("error", error && error.message ? error.message : "发送失败，请稍后重试。");
        } finally {
            if (localPreviewUrl) {
                URL.revokeObjectURL(localPreviewUrl);
            }
            pendingUserMessage = null;
            finalizeStreamState(requestSessionId);
            if (requestSessionId === activeSessionId) {
                saveActiveSessionSnapshot();
            }
            syncComposerState();
            input.focus();
        }
    }

    async function consumeEventStream(response, requestSessionId) {
        const reader = response.body.getReader();
        const decoder = new TextDecoder("utf-8");
        const assistant = requestSessionId === activeSessionId ? createStreamingAssistantMessage() : null;
        const state = streamStates.get(requestSessionId);
        if (state) {
            state.assistantView = assistant;
            if (assistant && state.status) {
                assistant.status.textContent = state.status;
            }
        }

        let buffer = "";
        while (true) {
            const result = await reader.read();
            if (result.done) {
                break;
            }
            buffer += decoder.decode(result.value, { stream: true });
            const parts = buffer.split("\n\n");
            buffer = parts.pop() || "";
            parts.forEach((part) => handleSseChunk(part, assistant, requestSessionId));
        }

        if (buffer.trim()) {
            handleSseChunk(buffer, assistant, requestSessionId);
        }
    }

    function replaceUserAttachmentMedia(messageView, attachments) {
        if (!messageView || !attachments || !attachments.length) {
            return;
        }
        const container = ensureMediaContainer(messageView);
        container.replaceChildren(renderMediaList(attachments));
    }

    function handleSseChunk(chunk, assistant, requestSessionId) {
        const lines = chunk.split(/\r?\n/);
        let eventName = "message";
        const dataLines = [];

        lines.forEach((line) => {
            if (line.startsWith("event:")) {
                eventName = line.slice(6).trim();
            } else if (line.startsWith("data:")) {
                dataLines.push(line.slice(5).trim());
            }
        });

        let payload = {};
        try {
            payload = dataLines.length ? JSON.parse(dataLines.join("\n")) : {};
        } catch (error) {
            payload = {};
        }

        const state = streamStates.get(requestSessionId);
        const currentAssistant = getActiveStreamAssistantView(requestSessionId, assistant);
        const hasAttachment = Boolean(state && state.requestHasAttachment);
        if (state) {
            if (eventName === "status") {
                state.phase = payload.phase || "";
                state.status = payload.message || "";
            } else if (eventName === "delta") {
                state.text += payload.content || "";
                if (state.phase !== "image_generating") {
                    state.status = "";
                }
            } else if (eventName === "image" && payload.generatedImage) {
                state.attachments.push(payload.generatedImage);
            } else if (eventName === "done" || eventName === "error") {
                state.phase = "";
                state.status = "";
            }

            updateStreamStateSnapshot(requestSessionId);
            if (requestSessionId === activeSessionId && eventName !== "delta") {
                syncComposerState();
            }
            if (currentAssistant && eventName !== "delta") {
                currentAssistant.status.textContent = state.status || "";
            }
        }

        if (requestSessionId !== activeSessionId || !currentAssistant) {
            if (eventName === "attachments" && Array.isArray(payload.attachments)) {
                pendingUserMessage = null;
            }
            return;
        }

        if (eventName === "status") {
            currentAssistant.status.textContent = payload.message || "";
            showLiveStatus(mapLiveStatusMessage(payload.phase, payload.message || "", hasAttachment));
        } else if (eventName === "delta") {
            if (!state || state.phase !== "image_generating") {
                currentAssistant.status.textContent = "";
                clearLiveStatus();
            }
            currentAssistant.text += payload.content || "";
            scheduleAssistantContentRender(currentAssistant);
        } else if (eventName === "attachments" && Array.isArray(payload.attachments)) {
            replaceUserAttachmentMedia(pendingUserMessage, payload.attachments);
            pendingUserMessage = null;
        } else if (eventName === "image" && payload.generatedImage) {
            if (state) {
                state.phase = "";
            }
            currentAssistant.status.textContent = "";
            clearLiveStatus();
            ensureMediaContainer(currentAssistant).appendChild(renderMediaList([payload.generatedImage]));
            stickMessageToBottom(currentAssistant);
        } else if (eventName === "done") {
            renderAssistantContentNow(currentAssistant);
            currentAssistant.status.textContent = "";
            clearLiveStatus();
            pendingUserMessage = null;
            stickMessageToBottom(currentAssistant);
        } else if (eventName === "error") {
            currentAssistant.status.textContent = "";
            currentAssistant.text += `${currentAssistant.text ? "\n" : ""}${payload.message || "处理请求时发生错误。"}`;
            renderAssistantContentNow(currentAssistant);
            clearLiveStatus();
            pendingUserMessage = null;
        }

        const activeSession = getActiveSession();
        if (eventName !== "delta" && (!activeSession || activeSession.autoScroll)) {
            scrollMessagesToBottom();
        }
        if (eventName !== "delta") {
            saveActiveSessionSnapshot();
        }
    }

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        sendMessage(input.value);
    });

    input.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            form.requestSubmit();
        }
    });

    input.addEventListener("input", resizeInput);

    document.querySelectorAll(".preset").forEach((button) => {
        button.addEventListener("click", () => {
            input.value = button.dataset.message || "";
            resizeInput();
            input.focus();
        });
    });

    newSessionButton.addEventListener("click", createSessionAndActivate);

    sessionList.addEventListener("click", (event) => {
        const target = event.target.closest(".session-item");
        if (!target) {
            return;
        }
        activateSession(target.dataset.sessionId);
    });

    chooseFileButton.addEventListener("click", () => {
        attachmentInput.click();
    });

    document.addEventListener("paste", (event) => {
        const items = event.clipboardData && event.clipboardData.items;
        if (!items) {
            return;
        }

        for (const item of items) {
            if (item.kind === "file") {
                const file = item.getAsFile();
                if (!file) {
                    return;
                }
                event.preventDefault();
                selectedFile = file;
                attachmentInput.value = "";
                showAttachmentChip(selectedFile);
                input.focus();
                return;
            }
        }
    });

    attachmentInput.addEventListener("change", () => {
        selectedFile = attachmentInput.files && attachmentInput.files[0] ? attachmentInput.files[0] : null;
        if (!selectedFile) {
            clearAttachment();
            return;
        }
        showAttachmentChip(selectedFile);
        input.focus();
    });

    removeFileButton.addEventListener("click", () => {
        clearAttachment();
        input.focus();
    });

    if (userProfileButton && userActionMenu) {
        userProfileButton.addEventListener("click", (event) => {
            event.stopPropagation();
            setUserMenuOpen(userActionMenu.hidden);
        });

        userActionMenu.addEventListener("click", (event) => {
            event.stopPropagation();
        });

        document.addEventListener("click", () => setUserMenuOpen(false));
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                setUserMenuOpen(false);
            }
        });
    }

    if (settingsButton) {
        settingsButton.addEventListener("click", () => setUserMenuOpen(false));
    }

    if (helpButton) {
        helpButton.addEventListener("click", () => setUserMenuOpen(false));
    }

    if (profileInfoButton) {
        profileInfoButton.addEventListener("click", async () => {
            setUserMenuOpen(false);
            if (typeof window.openHephaestusProfileDialog !== "function" && typeof window.initOrgProfile === "function") {
                await window.initOrgProfile();
            }
            if (typeof window.openHephaestusProfileDialog === "function") {
                window.openHephaestusProfileDialog();
            }
        });
    }

    window.addEventListener("hephaestus:user-profile-updated", (event) => {
        const profile = event.detail || {};
        const displayName = profile.personName || profile.username || "Hephaestus";
        userDisplayName.textContent = displayName;
        renderUserAvatar(profile.avatarAccessUrl || "", displayName);
    });

    if (logoutButton) {
        logoutButton.addEventListener("click", async () => {
            try {
                await fetch(`${basePath}/auth/logout`, { method: "POST" });
            } finally {
                redirectToLogin();
            }
        });
    }

    if (sessionExpiredLoginButton) {
        sessionExpiredLoginButton.addEventListener("click", redirectToLogin);
    }

    window.addEventListener("hephaestus:system-config-updated", (event) => {
        const detail = event.detail || {};
        if (!detail.groupCode || detail.groupCode === "main-system") {
            loadChatVisualConfig();
        }
    });

    window.addEventListener("focus", checkSessionStillActive);
    document.addEventListener("visibilitychange", () => {
        if (!document.hidden) {
            checkSessionStillActive();
        }
    });
    window.setInterval(checkSessionStillActive, 30000);

    messages.addEventListener("scroll", () => {
        if (suppressScrollStateSync > 0) {
            return;
        }
        syncActiveSessionScrollState();
    });

    messages.addEventListener("wheel", () => {
        stopAutoScrollFollowing();
    }, { passive: true });

    messages.addEventListener("pointerdown", () => {
        stopAutoScrollFollowing();
    });

    messages.addEventListener("touchstart", () => {
        stopAutoScrollFollowing();
    }, { passive: true });

    sessions.push(createSession(DEFAULT_SESSION_TITLE));
    activeSessionId = sessions[0].id;
    renderEmptyState();
    renderSessionList();
    resizeInput();
    syncComposerState();
    loadChatVisualConfig();
    loadCurrentUser();
})();
