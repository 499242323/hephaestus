(function () {
    async function initOrgProfile() {
        if (window.hephaestusOrgProfileInitialized) {
            return;
        }

        const mount = document.getElementById("orgProfileMount");
        if (mount && !mount.hasChildNodes()) {
            const response = await fetch("./org-profile-dialog.html?v=20260529-actions-center1");
            mount.innerHTML = await response.text();
        }

        const backdrop = document.getElementById("profileDialogBackdrop");
        const closeButton = document.getElementById("closeProfileDialogButton");
        const cancelButton = document.getElementById("profileCancelButton");
        const form = document.getElementById("profileForm");
        const errorBox = document.getElementById("profileError");
        const toastBox = document.getElementById("profileToast");
        const avatarButton = document.getElementById("profileAvatarButton");
        const avatarInput = document.getElementById("profileAvatarInput");
        const avatarPreview = document.getElementById("profileAvatarPreview");
        const usernameInput = document.getElementById("profileUsernameInput");
        const nameInput = document.getElementById("profileNameInput");
        const unitNameInput = document.getElementById("profileUnitNameInput");
        const mobileInput = document.getElementById("profileMobileInput");
        const emailInput = document.getElementById("profileEmailInput");
        const remarkInput = document.getElementById("profileRemarkInput");

        if (!backdrop || !form) {
            return;
        }

        window.hephaestusOrgProfileInitialized = true;

        const basePath = window.location.pathname.replace(/\/[^/]*$/, "");
        let currentUserPromise = null;
        let currentPerson = null;
        let toastTimer = null;

        function apiUrl(path) {
            return `${basePath}${path}`;
        }

        function escapeHtml(text) {
            return String(text)
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;")
                .replace(/'/g, "&#39;");
        }

        function setError(message) {
            if (!errorBox) {
                return;
            }
            errorBox.textContent = message || "";
            errorBox.hidden = !message;
        }

        function showToast(message) {
            if (!toastBox) {
                return;
            }
            if (toastTimer) {
                window.clearTimeout(toastTimer);
            }
            toastBox.textContent = message || "";
            toastBox.hidden = !message;
            if (message) {
                toastTimer = window.setTimeout(() => {
                    toastBox.hidden = true;
                    toastBox.textContent = "";
                }, 1800);
            }
        }

        function getInitial(person) {
            const source = (person && (person.personName || person.username)) || "B";
            return String(source).trim().slice(0, 1).toUpperCase() || "B";
        }

        function renderAvatar(person, previewUrl) {
            if (!avatarPreview) {
                return;
            }
            const url = previewUrl || (person && person.avatarAccessUrl) || "";
            if (url) {
                const image = document.createElement("img");
                image.src = url;
                image.alt = "头像";
                avatarPreview.replaceChildren(image);
                return;
            }
            avatarPreview.textContent = getInitial(person);
        }

        async function loadCurrentUser() {
            if (window.hephaestusCurrentLoginUser) {
                return window.hephaestusCurrentLoginUser;
            }
            if (!currentUserPromise) {
                currentUserPromise = fetch(apiUrl("/auth/me"))
                    .then((response) => response.ok ? response.json() : null)
                    .then((user) => {
                        window.hephaestusCurrentLoginUser = user;
                        return user;
                    })
                    .catch(() => null);
            }
            return currentUserPromise;
        }

        function buildHeaders(extraHeaders) {
            return new Headers(extraHeaders || {});
        }

        async function requestJson(path, options) {
            const response = await fetch(apiUrl(path), {
                ...options,
                headers: buildHeaders(options && options.headers ? options.headers : {})
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
            if (response.status === 204) {
                return null;
            }
            const text = await response.text();
            return text && text.trim() ? JSON.parse(text) : null;
        }

        function fillProfile(person) {
            currentPerson = person;
            usernameInput.value = person.username || "";
            nameInput.value = person.personName || "";
            unitNameInput.value = person.unitName || "";
            mobileInput.value = person.mobile || "";
            emailInput.value = person.email || "";
            remarkInput.value = person.remark || "";
            if (avatarInput) {
                avatarInput.value = "";
            }
            renderAvatar(person);
        }

        async function loadProfile() {
            await loadCurrentUser();
            const scope = await requestJson("/api/org/persons/current-scope");
            fillProfile(scope && scope.currentPerson ? scope.currentPerson : {});
        }

        function setOpen(open) {
            backdrop.hidden = !open;
            document.body.classList.toggle("settings-effects-muted", open);
            document.body.style.overflow = open ? "hidden" : "";
            setError("");
            showToast("");
            if (open) {
                loadProfile().catch((error) => setError(error.message || "加载个人信息失败"));
            }
        }

        async function saveProfile(event) {
            event.preventDefault();
            if (!currentPerson || !currentPerson.id) {
                setError("未获取到当前人员信息");
                return;
            }
            const payload = {
                personCode: currentPerson.personCode || "",
                personName: nameInput.value.trim(),
                username: currentPerson.username || usernameInput.value.trim(),
                password: currentPerson.password || "",
                unitId: currentPerson.unitId,
                mobile: mobileInput.value.trim(),
                email: emailInput.value.trim(),
                remark: remarkInput.value.trim(),
                enabled: currentPerson.enabled !== false
            };
            try {
                let saved = await requestJson(`/api/org/persons/${currentPerson.id}`, {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });
                if (avatarInput && avatarInput.files && avatarInput.files[0]) {
                    const formData = new FormData();
                    formData.append("file", avatarInput.files[0]);
                    saved = await requestJson(`/api/org/persons/${currentPerson.id}/avatar`, {
                        method: "POST",
                        body: formData
                    });
                }
                fillProfile(saved || currentPerson);
                showToast("保存成功");
                window.dispatchEvent(new CustomEvent("hephaestus:user-profile-updated", { detail: saved || currentPerson }));
            } catch (error) {
                setError(error.message || "保存个人信息失败");
            }
        }

        window.openHephaestusProfileDialog = () => setOpen(true);

        closeButton.addEventListener("click", () => setOpen(false));
        cancelButton.addEventListener("click", () => setOpen(false));
        backdrop.addEventListener("click", (event) => {
            if (event.target === backdrop) {
                setOpen(false);
            }
        });
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && !backdrop.hidden) {
                setOpen(false);
            }
        });
        avatarButton.addEventListener("click", () => avatarInput.click());
        avatarInput.addEventListener("change", () => {
            const file = avatarInput.files && avatarInput.files[0];
            if (file) {
                renderAvatar(currentPerson, URL.createObjectURL(file));
            }
        });
        form.addEventListener("submit", saveProfile);
    }

    window.initOrgProfile = initOrgProfile;
    initOrgProfile().catch((error) => {
        console.error("初始化个人信息弹窗失败", error);
    });
})();
