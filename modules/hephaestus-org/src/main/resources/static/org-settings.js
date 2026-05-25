(function () {
    async function initOrgSettings() {
        if (window.hephaestusOrgSettingsInitialized) {
            return;
        }

        const mount = document.getElementById("orgSettingsMount");
        if (mount && !mount.hasChildNodes()) {
            const panelResponse = await fetch("./org-settings-panel.html?v=20260525-inputfix");
            mount.innerHTML = await panelResponse.text();
        }

        const settingsButton = document.getElementById("settingsButton");
        const settingsBackdrop = document.getElementById("settingsBackdrop");
        const settingsDrawer = document.getElementById("settingsDrawer");
        if (!settingsButton || !settingsBackdrop || !settingsDrawer) {
            return;
        }

        window.hephaestusOrgSettingsInitialized = true;

        const closeSettingsButton = document.getElementById("closeSettingsButton");
        const refreshSettingsButton = document.getElementById("refreshSettingsButton");
        const refreshPeopleButton = document.getElementById("refreshPeopleButton");
        const settingsError = document.getElementById("settingsError");
        const settingsPanelTitle = document.getElementById("settingsPanelTitle");
        const settingsDrawerBody = document.querySelector(".settings-drawer__body");
        const generalTabButton = document.getElementById("generalTabButton");
        const unitsTabButton = document.getElementById("unitsTabButton");
        const peopleTabButton = document.getElementById("peopleTabButton");
        const generalPanel = document.getElementById("generalPanel");
        const unitsPanel = document.getElementById("unitsPanel");
        const peoplePanel = document.getElementById("peoplePanel");
        const unitTreeSearchInput = document.getElementById("unitTreeSearchInput");
        const peopleTreeSearchInput = document.getElementById("peopleTreeSearchInput");
        const unitsTree = document.getElementById("unitsTree");
        const peopleList = document.getElementById("peopleList");
        const unitEditorForm = document.getElementById("unitEditorForm");
        const unitEditIdInput = document.getElementById("unitEditIdInput");
        const unitParentIdInput = document.getElementById("unitParentIdInput");
        const unitParentSelect = document.getElementById("unitParentSelect");
        const unitCodeInput = document.getElementById("unitCodeInput");
        const unitNameInput = document.getElementById("unitNameInput");
        const unitSortOrderInput = document.getElementById("unitSortOrderInput");
        const unitEnabledInput = document.getElementById("unitEnabledInput");
        const showUnitCreateButton = document.getElementById("showUnitCreateButton");
        const resetUnitFormButton = document.getElementById("resetUnitFormButton");
        const personEditorForm = document.getElementById("personEditorForm");
        const personEditIdInput = document.getElementById("personEditIdInput");
        const personCodeInput = document.getElementById("personCodeInput");
        const personUsernameInput = document.getElementById("personUsernameInput");
        const personPasswordInput = document.getElementById("personPasswordInput");
        const personNameInput = document.getElementById("personNameInput");
        const personUnitIdInput = document.getElementById("personUnitIdInput");
        const personMobileInput = document.getElementById("personMobileInput");
        const personEmailInput = document.getElementById("personEmailInput");
        const personRemarkInput = document.getElementById("personRemarkInput");
        const personEnabledInput = document.getElementById("personEnabledInput");
        const personAvatarInput = document.getElementById("personAvatarInput");
        const personAvatarButton = document.getElementById("personAvatarButton");
        const personAvatarPreview = document.getElementById("personAvatarPreview");
        const personAvatarUploadButton = document.getElementById("personAvatarUploadButton");
        const personAvatarDeleteButton = document.getElementById("personAvatarDeleteButton");
        const showPersonCreateButton = document.getElementById("showPersonCreateButton");
        const resetPersonFormButton = document.getElementById("resetPersonFormButton");

        let settingsOpen = false;
        let currentSettingsTab = "general";
        let settingsScope = null;
        let settingsPeople = [];
        let selectedUnitId = null;
        let selectedPersonId = null;
        let currentAvatarMediaId = null;
        let isSettingsLoading = false;
        let unitTreeKeyword = "";
        let peopleTreeKeyword = "";

        const expandedUnitIds = new Set();
        const expandedPeopleGroupIds = new Set();

        const panelTitleMap = {
            general: "常规",
            units: "部门",
            people: "人员"
        };

        const basePath = window.location.pathname.replace(/\/[^/]*$/, "");

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

        function showSettingsError(message) {
            settingsError.hidden = !message;
            settingsError.textContent = message || "";
        }

        function flattenUnitTree(units, depth) {
            const result = [];
            (units || []).forEach((unit) => {
                const item = { ...unit, depth: depth || 0 };
                result.push(item);
                result.push(...flattenUnitTree(unit.children || [], (depth || 0) + 1));
            });
            return result;
        }

        function initializeExpandedSets(units) {
            expandedUnitIds.clear();
            expandedPeopleGroupIds.clear();
            flattenUnitTree(units || []).forEach((unit) => {
                if ((unit.children || []).length) {
                    expandedUnitIds.add(String(unit.id));
                    expandedPeopleGroupIds.add(String(unit.id));
                }
            });
        }

        function findUnitById(units, id) {
            const target = String(id);
            return flattenUnitTree(units).find((unit) => String(unit.id) === target) || null;
        }

        function isExpanded(set, id) {
            return set.has(String(id));
        }

        function toggleExpanded(set, id) {
            const key = String(id);
            if (set.has(key)) {
                set.delete(key);
            } else {
                set.add(key);
            }
        }

        function filterUnitTree(units, keyword) {
            const normalized = (keyword || "").trim().toLowerCase();
            if (!normalized) {
                return units || [];
            }
            return (units || []).map((unit) => {
                const children = filterUnitTree(unit.children || [], normalized);
                const matched = String(unit.unitName || "").toLowerCase().includes(normalized);
                if (!matched && !children.length) {
                    return null;
                }
                return { ...unit, children };
            }).filter(Boolean);
        }

        function setSettingsDrawerOpen(open) {
            settingsOpen = open;
            settingsDrawer.classList.toggle("is-open", open);
            settingsDrawer.setAttribute("aria-hidden", String(!open));
            settingsBackdrop.hidden = !open;
            document.body.style.overflow = open ? "hidden" : "";
            showSettingsError("");
            if (open) {
                loadSettingsData();
            }
        }

        function setSettingsTab(tab) {
            currentSettingsTab = tab;
            generalTabButton.classList.toggle("active", tab === "general");
            unitsTabButton.classList.toggle("active", tab === "units");
            peopleTabButton.classList.toggle("active", tab === "people");
            generalPanel.hidden = tab !== "general";
            unitsPanel.hidden = tab !== "units";
            peoplePanel.hidden = tab !== "people";
            settingsPanelTitle.textContent = panelTitleMap[tab] || "设置";
            showSettingsError("");
            if (settingsDrawerBody) {
                settingsDrawerBody.scrollTop = 0;
            }
        }

        function buildSettingsHeaders(extraHeaders) {
            const headers = new Headers(extraHeaders || {});
            headers.set("X-Person-Id", "100");
            return headers;
        }

        async function requestJson(path, options) {
            const response = await fetch(apiUrl(path), {
                ...options,
                headers: buildSettingsHeaders(options && options.headers ? options.headers : {})
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
            return response.json();
        }

        function populateUnitSelect(select, units, includeAll) {
            if (!select) {
                return;
            }
            const options = [];
            if (includeAll) {
                options.push('<option value="">全部部门</option>');
            }
            flattenUnitTree(units).forEach((unit) => {
                const indent = "\u00A0".repeat(unit.depth * 4);
                options.push(`<option value="${unit.id}">${indent}${escapeHtml(unit.unitName)}</option>`);
            });
            select.innerHTML = options.join("");
        }

        function resetUnitForm(parentId) {
            const firstRoot = settingsScope ? (settingsScope.units || [])[0] : null;
            const defaultParentId = parentId || (firstRoot ? firstRoot.id : "");
            selectedUnitId = null;
            unitEditIdInput.value = "";
            unitParentIdInput.value = defaultParentId ? String(defaultParentId) : "";
            unitParentSelect.value = defaultParentId ? String(defaultParentId) : "";
            unitCodeInput.value = "";
            unitNameInput.value = "";
            unitSortOrderInput.value = "0";
            unitEnabledInput.checked = true;
            renderUnitsTree((settingsScope && settingsScope.units) || []);
        }

        function fillUnitForm(unit) {
            selectedUnitId = String(unit.id);
            unitEditIdInput.value = String(unit.id);
            unitParentIdInput.value = unit.parentId ? String(unit.parentId) : "";
            unitParentSelect.value = unit.parentId ? String(unit.parentId) : "";
            unitCodeInput.value = unit.unitCode || "";
            unitNameInput.value = unit.unitName || "";
            unitSortOrderInput.value = String(unit.sortOrder || 0);
            unitEnabledInput.checked = Boolean(unit.enabled);
            renderUnitsTree((settingsScope && settingsScope.units) || []);
            setSettingsTab("units");
            unitCodeInput.focus();
        }

        function resetAvatarPreview() {
            currentAvatarMediaId = null;
            personAvatarPreview.innerHTML = "头像";
            personAvatarDeleteButton.disabled = true;
            personAvatarInput.value = "";
        }

        function renderAvatarPreview(url) {
            personAvatarPreview.innerHTML = url
                ? `<img src="${escapeHtml(url)}" alt="头像">`
                : "头像";
            personAvatarDeleteButton.disabled = !url;
        }

        function resetPersonForm() {
            selectedPersonId = null;
            personEditIdInput.value = "";
            personCodeInput.value = "";
            personUsernameInput.value = "";
            personPasswordInput.value = "";
            personNameInput.value = "";
            personUnitIdInput.value = "";
            personMobileInput.value = "";
            personEmailInput.value = "";
            personRemarkInput.value = "";
            personEnabledInput.checked = true;
            resetAvatarPreview();
            renderPeopleList(settingsPeople);
        }

        function fillPersonForm(person) {
            selectedPersonId = String(person.id);
            personEditIdInput.value = String(person.id);
            personCodeInput.value = person.personCode || "";
            personUsernameInput.value = person.username || "";
            personPasswordInput.value = person.password || "";
            personNameInput.value = person.personName || "";
            personUnitIdInput.value = person.unitId ? String(person.unitId) : "";
            personMobileInput.value = person.mobile || "";
            personEmailInput.value = person.email || "";
            personRemarkInput.value = person.remark || "";
            personEnabledInput.checked = Boolean(person.enabled);
            currentAvatarMediaId = person.avatarMediaId || null;
            renderAvatarPreview(person.avatarAccessUrl || "");
            renderPeopleList(settingsPeople);
            setSettingsTab("people");
            personCodeInput.focus();
        }

        function renderUnitsTree(units) {
            const visibleUnits = filterUnitTree(units, unitTreeKeyword);
            if (!visibleUnits || !visibleUnits.length) {
                unitsTree.innerHTML = '<div class="settings-empty">当前权限范围内暂无部门。</div>';
                return;
            }

            function renderNode(node) {
                const hasChildren = (node.children || []).length > 0;
                const expanded = hasChildren ? isExpanded(expandedUnitIds, node.id) : false;
                const activeClass = String(selectedUnitId) === String(node.id) ? " active" : "";
                const toggleIcon = hasChildren ? (expanded ? "▾" : "▸") : "•";
                const children = hasChildren && expanded
                    ? `<ul>${node.children.map(renderNode).join("")}</ul>`
                    : "";
                return `
                    <li>
                        <div class="settings-tree__row${activeClass}">
                            <button class="settings-tree__label" type="button" data-action="edit-unit" data-unit-id="${node.id}">
                                <span class="settings-tree__toggle" data-action="${hasChildren ? "toggle-unit" : ""}" data-unit-id="${node.id}">${toggleIcon}</span>
                                <span class="settings-tree__meta">
                                    <strong>${escapeHtml(node.unitName || "")}</strong>
                                </span>
                            </button>
                            <div class="settings-tree__actions">
                                <button class="settings-tree__action" type="button" data-action="create-unit" data-unit-id="${node.id}">新增下级</button>
                                <button class="settings-tree__action settings-action--danger" type="button" data-action="delete-unit" data-unit-id="${node.id}">删除</button>
                            </div>
                        </div>
                        ${children}
                    </li>
                `;
            }

            unitsTree.innerHTML = `<ul>${visibleUnits.map(renderNode).join("")}</ul>`;
        }

        function renderPeopleList(items) {
            if (!items || !items.length) {
                peopleList.innerHTML = '<div class="settings-empty">当前权限范围内暂无人员。</div>';
                return;
            }

            const units = flattenUnitTree((settingsScope && settingsScope.units) || []);
            const html = units.map((unit) => {
                const persons = items.filter((person) => {
                    if (String(person.unitId) !== String(unit.id)) {
                        return false;
                    }
                    if (!peopleTreeKeyword) {
                        return true;
                    }
                    return String(person.personName || "").toLowerCase().includes(peopleTreeKeyword);
                });
                const hasPersons = persons.length > 0;
                const expanded = hasPersons ? isExpanded(expandedPeopleGroupIds, unit.id) : false;
                const toggleIcon = hasPersons ? (expanded ? "▾" : "▸") : "•";
                const personItems = hasPersons && expanded
                    ? persons.map((person) => {
                        const avatar = person.avatarAccessUrl
                            ? `<img src="${escapeHtml(person.avatarAccessUrl)}" alt="${escapeHtml(person.personName || "头像")}">`
                            : `<span>${escapeHtml((person.personName || "?").slice(0, 1).toUpperCase())}</span>`;
                        const activeClass = String(selectedPersonId) === String(person.id) ? " active" : "";
                        return `
                            <li class="settings-person-tree__person${activeClass}">
                                <button class="settings-person-tree__item" type="button" data-action="edit-person" data-person-id="${person.id}">
                                    <span class="settings-avatar settings-avatar--list">${avatar}</span>
                                    <span class="settings-person-tree__meta">
                                        <strong>${escapeHtml(person.personName || "")}</strong>
                                    </span>
                                </button>
                                <button class="settings-tree__action settings-action--danger" type="button" data-action="delete-person" data-person-id="${person.id}">删除</button>
                            </li>
                        `;
                    }).join("")
                    : "";
                return `
                    <li class="settings-person-tree__unit" data-depth="${unit.depth || 0}">
                        <div class="settings-tree__row settings-tree__row--group">
                            <button class="settings-tree__label" type="button" data-action="${hasPersons ? "toggle-people-group" : ""}" data-unit-id="${unit.id}">
                                <span class="settings-tree__toggle">${toggleIcon}</span>
                                <span class="settings-tree__meta">
                                    <strong>${escapeHtml(unit.unitName || "")}</strong>
                                </span>
                            </button>
                        </div>
                        ${hasPersons && expanded ? `<ul class="settings-person-tree__list">${personItems}</ul>` : ""}
                    </li>
                `;
            }).join("");

            peopleList.innerHTML = `<ul class="settings-person-tree">${html}</ul>`;
        }

        async function loadPeople() {
            settingsPeople = await requestJson("/api/org/persons");
            renderPeopleList(settingsPeople);
        }

        async function loadSettingsData() {
            if (!settingsOpen || isSettingsLoading) {
                return;
            }
            isSettingsLoading = true;
            showSettingsError("");
            try {
                settingsScope = await requestJson("/api/org/persons/current-scope");
                initializeExpandedSets(settingsScope.units || []);
                renderUnitsTree(settingsScope.units || []);
                populateUnitSelect(personUnitIdInput, settingsScope.units || [], false);
                populateUnitSelect(unitParentSelect, settingsScope.units || [], false);
                await loadPeople();
                const firstUnit = flattenUnitTree(settingsScope.units || [])[0];
                if (firstUnit && !personUnitIdInput.value) {
                    personUnitIdInput.value = String(firstUnit.id);
                }
                if (firstUnit && !unitParentSelect.value) {
                    unitParentSelect.value = String(firstUnit.id);
                    unitParentIdInput.value = String(firstUnit.id);
                }
            } catch (error) {
                showSettingsError(error && error.message ? error.message : "加载设置数据失败");
                renderUnitsTree([]);
                renderPeopleList([]);
            } finally {
                isSettingsLoading = false;
            }
        }

        settingsButton.addEventListener("click", () => setSettingsDrawerOpen(true));
        closeSettingsButton.addEventListener("click", () => setSettingsDrawerOpen(false));
        settingsBackdrop.addEventListener("click", () => setSettingsDrawerOpen(false));
        generalTabButton.addEventListener("click", () => setSettingsTab("general"));
        unitsTabButton.addEventListener("click", () => setSettingsTab("units"));
        peopleTabButton.addEventListener("click", () => setSettingsTab("people"));
        refreshSettingsButton.addEventListener("click", () => loadSettingsData());
        refreshPeopleButton.addEventListener("click", () => loadPeople().catch((error) => showSettingsError(error.message)));

        if (unitTreeSearchInput) {
            unitTreeSearchInput.addEventListener("input", () => {
                unitTreeKeyword = unitTreeSearchInput.value.trim().toLowerCase();
                renderUnitsTree((settingsScope && settingsScope.units) || []);
            });
        }

        if (peopleTreeSearchInput) {
            peopleTreeSearchInput.addEventListener("input", () => {
                peopleTreeKeyword = peopleTreeSearchInput.value.trim().toLowerCase();
                renderPeopleList(settingsPeople);
            });
        }

        unitParentSelect.addEventListener("change", () => {
            unitParentIdInput.value = unitParentSelect.value || "";
        });

        showUnitCreateButton.addEventListener("click", () => {
            resetUnitForm("");
            setSettingsTab("units");
        });

        resetUnitFormButton.addEventListener("click", () => resetUnitForm(""));

        showPersonCreateButton.addEventListener("click", () => {
            resetPersonForm();
            setSettingsTab("people");
        });

        resetPersonFormButton.addEventListener("click", () => resetPersonForm());
        personAvatarButton.addEventListener("click", () => personAvatarInput.click());
        personAvatarUploadButton.addEventListener("click", () => personAvatarInput.click());

        personAvatarDeleteButton.addEventListener("click", async () => {
            if (!personEditIdInput.value || !currentAvatarMediaId) {
                resetAvatarPreview();
                return;
            }
            try {
                const updated = await requestJson(`/api/org/persons/${personEditIdInput.value}/avatar`, {
                    method: "DELETE"
                });
                currentAvatarMediaId = updated.avatarMediaId || null;
                renderAvatarPreview(updated.avatarAccessUrl || "");
                await loadPeople();
            } catch (error) {
                showSettingsError(error.message);
            }
        });

        personAvatarInput.addEventListener("change", () => {
            const file = personAvatarInput.files && personAvatarInput.files[0];
            if (!file) {
                return;
            }
            const objectUrl = URL.createObjectURL(file);
            renderAvatarPreview(objectUrl);
        });

        unitsTree.addEventListener("click", async (event) => {
            const button = event.target.closest("[data-action]");
            if (!button) {
                return;
            }
            const action = button.dataset.action;
            const unitId = button.dataset.unitId;
            const unit = settingsScope ? findUnitById(settingsScope.units || [], unitId) : null;
            if (!unit) {
                return;
            }

            if (action === "toggle-unit") {
                toggleExpanded(expandedUnitIds, unit.id);
                renderUnitsTree((settingsScope && settingsScope.units) || []);
                return;
            }
            if (action === "create-unit") {
                resetUnitForm(unit.id);
                unitCodeInput.focus();
                return;
            }
            if (action === "edit-unit") {
                fillUnitForm(unit);
                return;
            }
            if (action === "delete-unit") {
                if (!window.confirm(`确认删除部门“${unit.unitName}”吗？`)) {
                    return;
                }
                try {
                    await requestJson(`/api/org/units/${unit.id}`, { method: "DELETE" });
                    resetUnitForm("");
                    await loadSettingsData();
                } catch (error) {
                    showSettingsError(error.message);
                }
            }
        });

        peopleList.addEventListener("click", async (event) => {
            const button = event.target.closest("[data-action]");
            if (!button) {
                return;
            }
            const action = button.dataset.action;
            if (action === "toggle-people-group") {
                toggleExpanded(expandedPeopleGroupIds, button.dataset.unitId);
                renderPeopleList(settingsPeople);
                return;
            }
            const person = settingsPeople.find((item) => String(item.id) === String(button.dataset.personId));
            if (!person) {
                return;
            }
            if (action === "edit-person") {
                fillPersonForm(person);
                return;
            }
            if (action === "delete-person") {
                if (!window.confirm(`确认删除人员“${person.personName}”吗？`)) {
                    return;
                }
                try {
                    await requestJson(`/api/org/persons/${person.id}`, { method: "DELETE" });
                    resetPersonForm();
                    await loadPeople();
                } catch (error) {
                    showSettingsError(error.message);
                }
            }
        });

        unitEditorForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const payload = {
                unitCode: unitCodeInput.value.trim(),
                unitName: unitNameInput.value.trim(),
                parentId: unitParentSelect.value ? Number(unitParentSelect.value) : null,
                sortOrder: Number(unitSortOrderInput.value || 0),
                enabled: unitEnabledInput.checked
            };
            try {
                if (unitEditIdInput.value) {
                    await requestJson(`/api/org/units/${unitEditIdInput.value}`, {
                        method: "PUT",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({
                            unitCode: payload.unitCode,
                            unitName: payload.unitName,
                            sortOrder: payload.sortOrder,
                            enabled: payload.enabled
                        })
                    });
                } else {
                    await requestJson("/api/org/units", {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify(payload)
                    });
                }
                resetUnitForm("");
                await loadSettingsData();
            } catch (error) {
                showSettingsError(error.message);
            }
        });

        personEditorForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const payload = {
                personCode: personCodeInput.value.trim(),
                personName: personNameInput.value.trim(),
                username: personUsernameInput.value.trim(),
                password: personPasswordInput.value.trim(),
                unitId: Number(personUnitIdInput.value),
                mobile: personMobileInput.value.trim(),
                email: personEmailInput.value.trim(),
                remark: personRemarkInput.value.trim(),
                enabled: personEnabledInput.checked
            };
            try {
                let savedPerson = null;
                if (personEditIdInput.value) {
                    savedPerson = await requestJson(`/api/org/persons/${personEditIdInput.value}`, {
                        method: "PUT",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify(payload)
                    });
                } else {
                    savedPerson = await requestJson("/api/org/persons", {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify(payload)
                    });
                    personEditIdInput.value = String(savedPerson.id);
                }

                if (personAvatarInput.files && personAvatarInput.files[0] && savedPerson && savedPerson.id) {
                    const formData = new FormData();
                    formData.append("file", personAvatarInput.files[0]);
                    const updated = await requestJson(`/api/org/persons/${savedPerson.id}/avatar`, {
                        method: "POST",
                        body: formData
                    });
                    currentAvatarMediaId = updated.avatarMediaId || null;
                    renderAvatarPreview(updated.avatarAccessUrl || "");
                }

                await loadPeople();
            } catch (error) {
                showSettingsError(error.message);
            }
        });

        setSettingsTab("general");
    }

    window.initOrgSettings = initOrgSettings;
    initOrgSettings().catch((error) => {
        console.error("初始化组织设置面板失败", error);
    });
})();
