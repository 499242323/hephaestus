(function () {
    const basePath = window.location.pathname.replace(/\/[^/]*$/, "");
    const form = document.getElementById("loginForm");
    const usernameInput = document.getElementById("usernameInput");
    const passwordInput = document.getElementById("passwordInput");
    const loginButton = document.getElementById("loginButton");
    const loginError = document.getElementById("loginError");
    const loginTitle = document.getElementById("loginTitle");
    const loginSubtitle = document.getElementById("loginSubtitle");
    const LOGIN_CONFIG_KEYS = {
        passwordEncryptEnabled: "login.password.encrypt.enabled",
        passwordEncryptPublicKey: "login.password.encrypt.public-key",
        pageTitle: "login.page.title",
        pageSubtitle: "login.page.subtitle",
        pageBackgroundGridEnabled: "login.page.background.grid.enabled",
        pageBackgroundMediaId: "login.page.background.media-id",
        mouseTrailEffect: "login.mouse.trail.effect"
    };

    let publicConfig = {};

    function initMouseTrail(effectName) {
        if (window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
            return;
        }

        const effect = normalizeTrailEffect(effectName);
        const canvas = document.createElement("canvas");
        canvas.className = "mouse-trail-canvas";
        canvas.setAttribute("aria-hidden", "true");
        document.body.prepend(canvas);

        const context = canvas.getContext("2d");
        const particles = [];
        const doodlePoints = [];
        const symbols = ["♪", "♫", "♬", "✦", "✧"];
        const colors = ["#1d4ed8", "#0f766e", "#f59e0b", "#ec4899", "#22c55e", "#06b6d4", "#8b5cf6"];
        let lastSpawnAt = 0;
        let width = 0;
        let height = 0;
        let pixelRatio = 1;
        let cursor = null;

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

        function randomColor() {
            return colors[Math.floor(Math.random() * colors.length)];
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

        function trimParticles(limit) {
            if (particles.length > limit) {
                particles.splice(0, particles.length - limit);
            }
        }

        function spawnMusic(x, y) {
            for (let index = 0; index < 2; index += 1) {
                particles.push({
                    type: "text",
                    x: x + (Math.random() - 0.5) * 10,
                    y: y + (Math.random() - 0.5) * 10,
                    vx: (Math.random() - 0.5) * 1.4,
                    vy: -0.4 - Math.random() * 1.4,
                    life: 1,
                    decay: 0.016 + Math.random() * 0.012,
                    size: 12 + Math.random() * 11,
                    rotate: (Math.random() - 0.5) * 0.8,
                    symbol: symbols[Math.floor(Math.random() * symbols.length)],
                    color: randomColor()
                });
            }
            trimParticles(96);
        }

        function spawnFlowingLines(x, y) {
            for (let index = 0; index < 3; index += 1) {
                particles.push({
                    type: "line",
                    x,
                    y,
                    vx: (Math.random() - 0.5) * 2.8,
                    vy: (Math.random() - 0.5) * 2.8,
                    angle: Math.random() * Math.PI * 2,
                    length: 18 + Math.random() * 36,
                    life: 1,
                    decay: 0.018 + Math.random() * 0.012,
                    width: 1 + Math.random() * 1.6,
                    color: randomColor()
                });
            }
            trimParticles(120);
        }

        function spawnMeteor(x, y) {
            for (let index = 0; index < 2; index += 1) {
                particles.push({
                    type: "meteor",
                    x: x + (Math.random() - 0.5) * 24,
                    y: y + (Math.random() - 0.5) * 24,
                    vx: -2.2 - Math.random() * 2,
                    vy: 1.4 + Math.random() * 1.7,
                    length: 36 + Math.random() * 56,
                    life: 1,
                    decay: 0.02 + Math.random() * 0.012,
                    color: randomColor()
                });
            }
            trimParticles(90);
        }

        function spawnWave(x, y) {
            particles.push({
                type: "ring",
                x,
                y,
                radius: 4,
                growth: 2.6 + Math.random() * 2.4,
                life: 1,
                decay: 0.022,
                color: randomColor()
            });
            trimParticles(70);
        }

        function spawnDoodle(x, y) {
            doodlePoints.push({ x, y, life: 1, color: randomColor() });
            if (doodlePoints.length > 42) {
                doodlePoints.splice(0, doodlePoints.length - 42);
            }
        }

        function spawnConstellation(x, y) {
            particles.push({
                type: "point",
                x,
                y,
                vx: (Math.random() - 0.5) * 0.9,
                vy: (Math.random() - 0.5) * 0.9,
                radius: 2 + Math.random() * 2.5,
                life: 1,
                decay: 0.011 + Math.random() * 0.008,
                color: randomColor()
            });
            trimParticles(44);
        }

        function spawnFirefly(x, y) {
            for (let index = 0; index < 2; index += 1) {
                particles.push({
                    type: "dot",
                    x: x + (Math.random() - 0.5) * 14,
                    y: y + (Math.random() - 0.5) * 14,
                    vx: (Math.random() - 0.5) * 1.2,
                    vy: -0.3 - Math.random() * 0.9,
                    radius: 3 + Math.random() * 5,
                    life: 1,
                    decay: 0.014 + Math.random() * 0.01,
                    color: randomColor()
                });
            }
            trimParticles(130);
        }

        function spawnFirework(x, y, burst) {
            const count = burst ? 30 : 7;
            for (let index = 0; index < count; index += 1) {
                const angle = Math.random() * Math.PI * 2;
                const speed = (burst ? 1.8 : 0.8) + Math.random() * (burst ? 3.2 : 1.7);
                particles.push({
                    type: "spark",
                    x,
                    y,
                    vx: Math.cos(angle) * speed,
                    vy: Math.sin(angle) * speed,
                    radius: 1.8 + Math.random() * 2.8,
                    life: 1,
                    decay: (burst ? 0.016 : 0.024) + Math.random() * 0.012,
                    color: randomColor()
                });
            }
            trimParticles(180);
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
            context.globalAlpha = alpha;
            context.shadowColor = particle.color;
            context.strokeStyle = particle.color;
            context.fillStyle = particle.color;
            if (particle.type === "text") {
                const scale = 0.55 + alpha * 0.45;
                context.translate(particle.x, particle.y);
                context.rotate(particle.rotate);
                context.scale(scale, scale);
                context.shadowBlur = 14;
                context.font = `${particle.size}px "Times New Roman", serif`;
                context.textAlign = "center";
                context.textBaseline = "middle";
                context.fillText(particle.symbol, 0, 0);
            } else if (particle.type === "line") {
                const dx = Math.cos(particle.angle) * particle.length;
                const dy = Math.sin(particle.angle) * particle.length;
                context.lineWidth = particle.width;
                context.shadowBlur = 10;
                context.beginPath();
                context.moveTo(particle.x - dx * 0.5, particle.y - dy * 0.5);
                context.lineTo(particle.x + dx * 0.5, particle.y + dy * 0.5);
                context.stroke();
            } else if (particle.type === "meteor") {
                context.lineWidth = 2.2;
                context.shadowBlur = 16;
                const gradient = context.createLinearGradient(particle.x, particle.y, particle.x - particle.vx * particle.length, particle.y - particle.vy * particle.length);
                gradient.addColorStop(0, particle.color);
                gradient.addColorStop(1, "rgba(255,255,255,0)");
                context.strokeStyle = gradient;
                context.beginPath();
                context.moveTo(particle.x, particle.y);
                context.lineTo(particle.x - particle.vx * particle.length, particle.y - particle.vy * particle.length);
                context.stroke();
            } else if (particle.type === "ring") {
                context.lineWidth = 2;
                context.shadowBlur = 12;
                context.beginPath();
                context.arc(particle.x, particle.y, particle.radius, 0, Math.PI * 2);
                context.stroke();
            } else {
                context.shadowBlur = particle.type === "dot" ? 20 : 12;
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
                    context.globalAlpha = Math.min(left.life, right.life, 1 - distance / 108) * 0.55;
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
                context.globalAlpha = Math.min(previous.life, point.life) * 0.75;
                context.strokeStyle = point.color;
                context.lineWidth = 3;
                context.lineCap = "round";
                context.shadowBlur = 12;
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
            const radius = 58;
            context.save();
            context.beginPath();
            context.arc(cursor.x, cursor.y, radius, 0, Math.PI * 2);
            context.clip();
            context.fillStyle = "rgba(255,255,255,0.20)";
            context.fillRect(cursor.x - radius, cursor.y - radius, radius * 2, radius * 2);
            context.strokeStyle = "rgba(29,78,216,0.18)";
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
            context.strokeStyle = "rgba(29,78,216,0.78)";
            context.lineWidth = 3;
            context.shadowBlur = 20;
            context.shadowColor = "#1d4ed8";
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
            window.requestAnimationFrame(animate);
        }

        window.addEventListener("resize", resizeCanvas);
        window.addEventListener("mousemove", (event) => spawnTrail(event.clientX, event.clientY), { passive: true });
        window.addEventListener("touchmove", (event) => {
            const touch = event.touches && event.touches[0];
            if (touch) {
                spawnTrail(touch.clientX, touch.clientY);
            }
        }, { passive: true });
        window.addEventListener("touchstart", (event) => {
            const touch = event.touches && event.touches[0];
            if (touch) {
                spawnTrail(touch.clientX, touch.clientY);
            }
        }, { passive: true });
        window.addEventListener("click", (event) => spawnTrail(event.clientX, event.clientY, true), { passive: true });

        resizeCanvas();
        animate();
    }

    function showError(message) {
        loginError.textContent = message || "";
        loginError.hidden = !message;
    }

    async function loadPublicConfig() {
        const response = await fetch(`${basePath}/api/system-config/public/main-system`);
        if (!response.ok) {
            return;
        }
        const payload = await response.json();
        publicConfig = payload.items || {};
        loginTitle.textContent = publicConfig[LOGIN_CONFIG_KEYS.pageTitle] || "Hephaestus";
        loginSubtitle.textContent = publicConfig[LOGIN_CONFIG_KEYS.pageSubtitle] || "登录后继续使用智能对话与组织配置能力";
        applyLoginBackground(
            publicConfig[LOGIN_CONFIG_KEYS.pageBackgroundGridEnabled] || "true",
            publicConfig[LOGIN_CONFIG_KEYS.pageBackgroundMediaId] || "1"
        );
    }

    function applyLoginBackground(gridEnabled, mediaId) {
        const enabled = ["true", "1", "yes"].includes(String(gridEnabled || "true").trim().toLowerCase());
        const normalizedMediaId = String(mediaId || "1").trim();
        document.body.classList.remove("login-background-grid", "login-background-media");
        document.body.style.removeProperty("--login-background-image");
        if (/^\d+$/.test(normalizedMediaId)) {
            document.body.style.setProperty("--login-background-image", `url("${basePath}/api/media/files/${normalizedMediaId}")`);
            document.body.classList.add("login-background-media");
        }
        if (enabled) {
            document.body.classList.add("login-background-grid");
        }
    }

    function base64ToArrayBuffer(base64) {
        const binary = window.atob(String(base64 || "").replace(/\s+/g, ""));
        const bytes = new Uint8Array(binary.length);
        for (let index = 0; index < binary.length; index += 1) {
            bytes[index] = binary.charCodeAt(index);
        }
        return bytes.buffer;
    }

    async function encryptPassword(password) {
        const enabled = String(publicConfig[LOGIN_CONFIG_KEYS.passwordEncryptEnabled] || "true") === "true";
        const publicKey = publicConfig[LOGIN_CONFIG_KEYS.passwordEncryptPublicKey] || "";
        if (!enabled) {
            return { password, encrypted: false };
        }
        if (!window.crypto || !window.crypto.subtle || !publicKey) {
            throw new Error("当前浏览器无法完成密码加密");
        }
        const key = await window.crypto.subtle.importKey(
            "spki",
            base64ToArrayBuffer(publicKey),
            { name: "RSA-OAEP", hash: "SHA-256" },
            false,
            ["encrypt"]
        );
        const encrypted = await window.crypto.subtle.encrypt(
            { name: "RSA-OAEP" },
            key,
            new TextEncoder().encode(password)
        );
        const bytes = new Uint8Array(encrypted);
        let binary = "";
        bytes.forEach((byte) => {
            binary += String.fromCharCode(byte);
        });
        return { password: window.btoa(binary), encrypted: true };
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        showError("");
        loginButton.disabled = true;
        loginButton.textContent = "登录中";
        try {
            const encrypted = await encryptPassword(passwordInput.value);
            const response = await fetch(`${basePath}/auth/login`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    username: usernameInput.value.trim(),
                    password: encrypted.password,
                    encrypted: encrypted.encrypted
                })
            });
            if (!response.ok) {
                let message = "登录失败";
                try {
                    const payload = await response.json();
                    message = payload.message || message;
                } catch (error) {
                    message = response.statusText || message;
                }
                throw new Error(message);
            }
            window.location.href = `${basePath}/chat.html`;
        } catch (error) {
            showError(error && error.message ? error.message : "登录失败");
        } finally {
            loginButton.disabled = false;
            loginButton.textContent = "登录";
        }
    });

    loadPublicConfig()
        .catch(() => applyLoginBackground("true", "1"))
        .finally(() => initMouseTrail(publicConfig[LOGIN_CONFIG_KEYS.mouseTrailEffect] || "music"));
})();
