(function () {
    const basePath = window.location.pathname.replace(/\/[^/]*$/, "");
    const form = document.getElementById("loginForm");
    const showLoginButton = document.getElementById("showLoginButton");
    const showRegisterButton = document.getElementById("showRegisterButton");
    const usernameInput = document.getElementById("usernameInput");
    const emailInput = document.getElementById("emailInput");
    const emailLabel = document.getElementById("emailLabel");
    const codeInput = document.getElementById("codeInput");
    const passwordInput = document.getElementById("passwordInput");
    const confirmPasswordInput = document.getElementById("confirmPasswordInput");
    const confirmPasswordLabel = document.getElementById("confirmPasswordLabel");
    const sendCodeButton = document.getElementById("sendCodeButton");
    const loginButton = document.getElementById("loginButton");
    const loginError = document.getElementById("loginError");
    const loginTitle = document.getElementById("loginTitle");
    const loginSubtitle = document.getElementById("loginSubtitle");
    const forgotPasswordButton = document.getElementById("forgotPasswordButton");
    const backToLoginButton = document.getElementById("backToLoginButton");
    const resetAccountList = document.getElementById("resetAccountList");
    const authMessageDialog = document.getElementById("authMessageDialog");
    const authMessageText = document.getElementById("authMessageText");
    const authMessageCloseButton = document.getElementById("authMessageCloseButton");
    const authConfirmDialog = document.getElementById("authConfirmDialog");
    const authConfirmText = document.getElementById("authConfirmText");
    const authConfirmCancelButton = document.getElementById("authConfirmCancelButton");
    const authConfirmOkButton = document.getElementById("authConfirmOkButton");
    const LOGIN_CONFIG_KEYS = {
        passwordEncryptEnabled: "login.password.encrypt.enabled",
        passwordEncryptAlgorithm: "login.password.encrypt.algorithm",
        passwordEncryptPublicKey: "login.password.encrypt.public-key",
        pageTitle: "login.page.title",
        pageSubtitle: "login.page.subtitle",
        pageBackgroundGridEnabled: "login.page.background.grid.enabled",
        pageBackgroundMediaId: "login.page.background.media-id",
        mouseTrailEffect: "login.mouse.trail.effect"
    };
    const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    let publicConfig = {};
    let authMode = "login";
    let resetStep = "verify";
    let resetAccounts = [];
    let selectedResetAccount = null;
    let confirmResolver = null;
    let loginDraft = { username: "", password: "" };
    let codeTimer = 0;
    let codeCountdown = 0;

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
        loginError.textContent = "";
        loginError.hidden = true;
        if (message) {
            showMessageDialog(message);
        }
    }

    function showMessageDialog(message) {
        if (!authMessageDialog || !authMessageText) {
            window.alert(message);
            return;
        }
        authMessageText.textContent = message;
        authMessageDialog.hidden = false;
        if (authMessageCloseButton) {
            authMessageCloseButton.focus();
        }
    }

    function closeMessageDialog() {
        if (authMessageDialog) {
            authMessageDialog.hidden = true;
        }
    }

    function showConfirmDialog(message) {
        if (!authConfirmDialog || !authConfirmText || !authConfirmOkButton) {
            return Promise.resolve(window.confirm(message));
        }
        authConfirmText.textContent = message;
        authConfirmDialog.hidden = false;
        authConfirmOkButton.focus();
        return new Promise((resolve) => {
            confirmResolver = resolve;
        });
    }

    function closeConfirmDialog(result) {
        if (authConfirmDialog) {
            authConfirmDialog.hidden = true;
        }
        if (confirmResolver) {
            confirmResolver(result);
            confirmResolver = null;
        }
    }

    function getTrimmedValue(input) {
        return input ? input.value.trim() : "";
    }

    function clearAuthInputs() {
        usernameInput.value = "";
        emailInput.value = "";
        codeInput.value = "";
        passwordInput.value = "";
        confirmPasswordInput.value = "";
    }

    function rememberLoginDraft() {
        if (authMode === "login") {
            loginDraft = {
                username: usernameInput.value,
                password: passwordInput.value
            };
        }
    }

    function restoreLoginDraft() {
        usernameInput.value = loginDraft.username || "";
        passwordInput.value = loginDraft.password || "";
        emailInput.value = "";
        codeInput.value = "";
        confirmPasswordInput.value = "";
    }

    function requireEmail() {
        const email = getTrimmedValue(emailInput);
        if (!email) {
            throw new Error("请填写邮箱");
        }
        if (!EMAIL_PATTERN.test(email)) {
            throw new Error("请填写正确的邮箱地址");
        }
        return email;
    }

    function requireAuthForm() {
        const username = getTrimmedValue(usernameInput);
        const password = passwordInput.value;
        if (!username) {
            throw new Error("请填写用户名");
        }
        if (!password) {
            throw new Error(authMode === "reset" ? "请填写新密码" : "请填写密码");
        }
        if (authMode === "login") {
            return { username, password };
        }
        const email = requireEmail();
        const code = getTrimmedValue(codeInput);
        const confirmPassword = confirmPasswordInput.value;
        if (!code) {
            throw new Error("请填写验证码");
        }
        if (!confirmPassword) {
            throw new Error(authMode === "reset" ? "请确认新密码" : "请确认密码");
        }
        if (password !== confirmPassword) {
            throw new Error("两次输入的密码不一致");
        }
        return { username, password, confirmPassword, email, code };
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
        const algorithm = publicConfig[LOGIN_CONFIG_KEYS.passwordEncryptAlgorithm] || "RSA_OAEP_SHA256";
        if (!enabled) {
            return { password, encrypted: false };
        }
        if (!publicKey) {
            throw new Error("当前浏览器无法完成密码加密");
        }
        const normalizedAlgorithm = algorithm.toUpperCase();
        if (normalizedAlgorithm === "RSA_PKCS1") {
            if (!window.JSEncrypt || !publicKey) {
                throw new Error("当前浏览器无法完成密码加密");
            }
            const encryptor = new window.JSEncrypt();
            encryptor.setPublicKey(`-----BEGIN PUBLIC KEY-----\n${publicKey}\n-----END PUBLIC KEY-----`);
            const encryptedPassword = encryptor.encrypt(password);
            if (!encryptedPassword) {
                throw new Error("当前浏览器无法完成密码加密");
            }
            return { password: encryptedPassword, encrypted: true };
        }
        if (normalizedAlgorithm !== "RSA_OAEP_SHA256" && normalizedAlgorithm !== "RSA-OAEP") {
            throw new Error("当前登录密码加密模式不受支持");
        }
        const subtleCrypto = window.crypto && window.crypto.subtle;
        if (!subtleCrypto) {
            throw new Error("当前浏览器无法完成密码加密，请使用 HTTPS 访问或将登录密码加密模式配置为 RSA_PKCS1");
        }
        const key = await subtleCrypto.importKey(
            "spki",
            base64ToArrayBuffer(publicKey),
            { name: "RSA-OAEP", hash: "SHA-256" },
            false,
            ["encrypt"]
        );
        const encrypted = await subtleCrypto.encrypt(
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

    async function postJson(url, body) {
        const response = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body)
        });
        if (!response.ok) {
            let message = "请求失败";
            try {
                const payload = await response.json();
                message = payload.message || payload.detail || message;
            } catch (error) {
                message = response.statusText || message;
            }
            throw new Error(message);
        }
        return response.json().catch(() => ({}));
    }

    function updateCodeButton() {
        if (codeCountdown <= 0) {
            sendCodeButton.disabled = false;
            sendCodeButton.textContent = "发送验证码";
            window.clearInterval(codeTimer);
            codeTimer = 0;
            return;
        }
        sendCodeButton.disabled = true;
        sendCodeButton.textContent = `${codeCountdown}s 后重发`;
    }

    function startCodeCountdown() {
        codeCountdown = 60;
        updateCodeButton();
        codeTimer = window.setInterval(() => {
            codeCountdown -= 1;
            updateCodeButton();
        }, 1000);
    }

    async function sendCode() {
        showError("");
        try {
            const email = requireEmail();
            sendCodeButton.disabled = true;
            sendCodeButton.textContent = "发送中";
            const endpoint = authMode === "reset" ? "reset-password" : "register";
            await postJson(`${basePath}/auth/email-code/${endpoint}`, { email });
            codeInput.value = "";
            showError("验证码已发送，请查看邮箱");
            startCodeCountdown();
        } catch (error) {
            showError(error && error.message ? error.message : "验证码发送失败");
            if (!codeTimer) {
                sendCodeButton.disabled = false;
                sendCodeButton.textContent = "发送验证码";
            }
        }
    }

    form.addEventListener("submit", async (event) => {
        if (authMode === "reset") {
            return;
        }
        event.preventDefault();
        showError("");
        loginButton.disabled = true;
        loginButton.textContent = authMode === "login" ? "登录中" : (authMode === "reset" ? "重置中" : "注册中");
        try {
            const formValue = requireAuthForm();
            const encrypted = await encryptPassword(formValue.password);
            if (authMode === "login") {
                await postJson(`${basePath}/auth/login`, {
                    username: formValue.username,
                    password: encrypted.password,
                    encrypted: encrypted.encrypted
                });
                window.location.href = `${basePath}/chat.html`;
                return;
            }
            const payload = {
                email: formValue.email,
                username: formValue.username,
                password: encrypted.password,
                confirmPassword: encrypted.password,
                code: formValue.code,
                encrypted: encrypted.encrypted
            };
            if (authMode === "register") {
                payload.personName = formValue.username;
                await postJson(`${basePath}/auth/register`, payload);
                clearAuthInputs();
                showError("注册成功，请切换登录");
                setAuthMode("login");
            } else {
                await postJson(`${basePath}/auth/reset-password`, payload);
                showError("密码已重置，请登录");
                setAuthMode("login");
            }
        } catch (error) {
            showError(error && error.message ? error.message : "提交失败");
        } finally {
            loginButton.disabled = false;
            loginButton.textContent = authMode === "login" ? "登录" : (authMode === "reset" ? "确认重置" : "注册");
        }
    });

    function renderResetAccounts() {
        resetAccountList.innerHTML = "";
        resetAccountList.hidden = authMode !== "reset" || resetStep !== "password" || resetAccounts.length === 0;
        if (resetAccountList.hidden) {
            return;
        }
        const select = document.createElement("select");
        select.className = "reset-account-select";
        select.setAttribute("aria-label", "选择账号");
        resetAccounts.forEach((account) => {
            const option = document.createElement("option");
            option.value = account.username || "";
            option.textContent = account.personName
                ? `${account.personName}（${account.username || ""}）`
                : (account.username || "");
            option.selected = selectedResetAccount && selectedResetAccount.username === account.username;
            select.appendChild(option);
        });
        function syncSelectedAccount() {
            selectedResetAccount = resetAccounts.find((account) => account.username === select.value) || resetAccounts[0] || null;
            usernameInput.value = selectedResetAccount ? (selectedResetAccount.username || "") : "";
        }

        select.addEventListener("change", syncSelectedAccount);
        resetAccountList.appendChild(select);
        syncSelectedAccount();
    }

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function setResetPasswordStep(nextStep) {
        resetStep = nextStep === "password" ? "password" : "verify";
        document.body.dataset.resetStep = resetStep;
        const passwordStep = resetStep === "password";
        usernameInput.required = authMode !== "reset" || passwordStep;
        passwordInput.required = authMode !== "reset" || passwordStep;
        confirmPasswordInput.required = authMode !== "login" && (authMode !== "reset" || passwordStep);
        loginButton.textContent = authMode === "reset" ? (passwordStep ? "确认重置" : "下一步") : (authMode === "login" ? "登录" : "注册");
        renderResetAccounts();
    }

    function setAuthMode(nextMode) {
        const previousMode = authMode;
        rememberLoginDraft();
        authMode = ["login", "register", "reset"].includes(nextMode) ? nextMode : "login";
        if (previousMode !== authMode) {
            clearAuthInputs();
            if (authMode === "login") {
                restoreLoginDraft();
            }
        }
        resetAccounts = [];
        selectedResetAccount = null;
        document.body.dataset.authMode = authMode;
        showLoginButton.classList.toggle("auth-switch__item--active", authMode === "login");
        showRegisterButton.classList.toggle("auth-switch__item--active", authMode === "register");
        emailInput.required = authMode !== "login";
        codeInput.required = authMode !== "login";
        passwordInput.autocomplete = authMode === "login" ? "current-password" : "new-password";
        confirmPasswordLabel.textContent = authMode === "reset" ? "确认新密码" : "确认密码";
        if (emailLabel) {
            emailLabel.textContent = authMode === "reset" ? "注册时邮箱" : "邮箱";
        }
        usernameInput.placeholder = authMode === "register" ? "用户名" : "";
        emailInput.placeholder = authMode === "register" || authMode === "reset" ? "邮箱" : "";
        codeInput.placeholder = authMode === "register" || authMode === "reset" ? "验证码" : "";
        passwordInput.placeholder = authMode === "register" ? "密码" : (authMode === "reset" ? "新密码" : "");
        confirmPasswordInput.placeholder = authMode === "register" ? "确认密码" : (authMode === "reset" ? "确认新密码" : "");
        forgotPasswordButton.hidden = authMode !== "login";
        backToLoginButton.hidden = authMode === "login";
        setResetPasswordStep("verify");
        showError("");
    }

    async function verifyResetPasswordCode() {
        const email = requireEmail();
        const code = getTrimmedValue(codeInput);
        if (!code) {
            throw new Error("请填写验证码");
        }
        const payload = await postJson(`${basePath}/auth/reset-password/verify`, { email, code });
        resetAccounts = payload.accounts || [];
        if (resetAccounts.length === 0) {
            throw new Error("该邮箱未绑定可重置的账号");
        }
        selectedResetAccount = resetAccounts[0];
        usernameInput.value = selectedResetAccount.username || "";
        passwordInput.value = "";
        confirmPasswordInput.value = "";
        setResetPasswordStep("password");
    }

    async function submitResetPassword() {
        if (!selectedResetAccount) {
            throw new Error("请选择需要重置密码的账号");
        }
        const password = passwordInput.value;
        const confirmPassword = confirmPasswordInput.value;
        if (!password) {
            throw new Error("请填写新密码");
        }
        if (!confirmPassword) {
            throw new Error("请确认新密码");
        }
        if (password !== confirmPassword) {
            throw new Error("两次输入的密码不一致");
        }
        const confirmed = await showConfirmDialog("确认要重置所选账号的密码吗？");
        if (!confirmed) {
            return;
        }
        const encrypted = await encryptPassword(password);
        await postJson(`${basePath}/auth/reset-password`, {
            email: requireEmail(),
            username: selectedResetAccount.username,
            password: encrypted.password,
            confirmPassword: encrypted.password,
            code: getTrimmedValue(codeInput),
            encrypted: encrypted.encrypted
        });
        showError("密码已重置，请登录");
        setAuthMode("login");
    }

    sendCodeButton.addEventListener("click", (event) => {
        if (authMode !== "reset") {
            return;
        }
        event.preventDefault();
        event.stopImmediatePropagation();
        sendCode();
    }, true);

    form.addEventListener("submit", async (event) => {
        if (authMode !== "reset") {
            return;
        }
        event.preventDefault();
        event.stopImmediatePropagation();
        showError("");
        if (resetStep === "verify") {
            loginButton.disabled = true;
            loginButton.textContent = "验证中";
        }
        try {
            if (resetStep === "verify") {
                await verifyResetPasswordCode();
            } else {
                await submitResetPassword();
            }
        } catch (error) {
            showError(error && error.message ? error.message : "提交失败");
        } finally {
            loginButton.disabled = false;
            if (authMode === "reset") {
                loginButton.textContent = resetStep === "verify" ? "下一步" : "确认重置";
            }
        }
    }, true);

    showLoginButton.addEventListener("click", () => setAuthMode("login"));
    showRegisterButton.addEventListener("click", () => setAuthMode("register"));
    forgotPasswordButton.addEventListener("click", () => setAuthMode("reset"));
    backToLoginButton.addEventListener("click", () => setAuthMode("login"));
    sendCodeButton.addEventListener("click", sendCode);
    if (authMessageCloseButton) {
        authMessageCloseButton.addEventListener("click", closeMessageDialog);
    }
    if (authConfirmCancelButton) {
        authConfirmCancelButton.addEventListener("click", () => closeConfirmDialog(false));
    }
    if (authConfirmOkButton) {
        authConfirmOkButton.addEventListener("click", () => closeConfirmDialog(true));
    }
    if (authMessageDialog) {
        authMessageDialog.addEventListener("click", (event) => {
            if (event.target === authMessageDialog) {
                closeMessageDialog();
            }
        });
    }
    if (authConfirmDialog) {
        authConfirmDialog.addEventListener("click", (event) => {
            if (event.target === authConfirmDialog) {
                closeConfirmDialog(false);
            }
        });
    }
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && authMessageDialog && !authMessageDialog.hidden) {
            closeMessageDialog();
        }
        if (event.key === "Escape" && authConfirmDialog && !authConfirmDialog.hidden) {
            closeConfirmDialog(false);
        }
    });

    const params = new URLSearchParams(window.location.search);
    setAuthMode(params.get("mode") === "reset" ? "reset" : "login");
    loadPublicConfig()
        .catch(() => applyLoginBackground("true", "1"))
        .finally(() => initMouseTrail(publicConfig[LOGIN_CONFIG_KEYS.mouseTrailEffect] || "music"));
})();
