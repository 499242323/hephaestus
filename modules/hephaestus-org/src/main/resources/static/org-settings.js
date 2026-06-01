(function () {
    async function initOrgSettings() {
        if (window.hephaestusOrgSettingsInitialized) {
            return;
        }

        const mount = document.getElementById("orgSettingsMount");
        if (mount && !mount.hasChildNodes()) {
            const panelResponse = await fetch("./org-settings-panel.html?v=20260601-blue-form-actions1");
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
        const settingsToast = document.getElementById("settingsToast");
        const settingsPanelTitle = document.getElementById("settingsPanelTitle");
        const settingsDrawerBody = document.querySelector(".settings-drawer__body");
        const settingsConfirmDialog = document.getElementById("settingsConfirmDialog");
        const settingsConfirmMessage = document.getElementById("settingsConfirmMessage");
        const settingsConfirmCancelButton = document.getElementById("settingsConfirmCancelButton");
        const settingsConfirmOkButton = document.getElementById("settingsConfirmOkButton");
        const generalTabButton = document.getElementById("generalTabButton");
        const unitsTabButton = document.getElementById("unitsTabButton");
        const peopleTabButton = document.getElementById("peopleTabButton");
        const rolesTabButton = document.getElementById("rolesTabButton");
        const logsTabButton = document.getElementById("logsTabButton");
        const generalPanel = document.getElementById("generalPanel");
        const unitsPanel = document.getElementById("unitsPanel");
        const peoplePanel = document.getElementById("peoplePanel");
        const rolesPanel = document.getElementById("rolesPanel");
        const roleSettingsMount = document.getElementById("roleSettingsMount");
        const logsPanel = document.getElementById("logsPanel");
        const systemConfigForm = document.getElementById("systemConfigForm");
        const systemConfigFields = document.getElementById("systemConfigFields");
        const systemConfigSectionTitle = document.getElementById("systemConfigSectionTitle");
        const refreshSystemConfigButton = document.getElementById("refreshSystemConfigButton");
        const unitTreeSearchInput = document.getElementById("unitTreeSearchInput");
        const peopleTreeSearchInput = document.getElementById("peopleTreeSearchInput");
        const unitsTree = document.getElementById("unitsTree");
        const unitTreeContextMenu = document.getElementById("unitTreeContextMenu");
        const unitsSection = unitsTree ? unitsTree.closest(".settings-section--side") : null;
        const peopleList = document.getElementById("peopleList");
        const peopleTreeContextMenu = document.getElementById("peopleTreeContextMenu");
        const peopleCreateMenuButton = peopleTreeContextMenu ? peopleTreeContextMenu.querySelector("[data-action='create-person']") : null;
        const peopleDeleteMenuButton = peopleTreeContextMenu ? peopleTreeContextMenu.querySelector("[data-action='delete-person']") : null;
        const peopleSection = peopleList ? peopleList.closest(".settings-section--side") : null;
        const unitEditorForm = document.getElementById("unitEditorForm");
        const unitEditIdInput = document.getElementById("unitEditIdInput");
        const unitParentIdInput = document.getElementById("unitParentIdInput");
        const unitParentSelect = document.getElementById("unitParentSelect");
        const unitCodeInput = document.getElementById("unitCodeInput");
        const unitNameInput = document.getElementById("unitNameInput");
        const unitSortOrderInput = document.getElementById("unitSortOrderInput");
        const unitEnabledInput = document.getElementById("unitEnabledInput");
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
        const personRolePickerList = document.getElementById("personRolePickerList");
        const showPersonCreateButton = document.getElementById("showPersonCreateButton");
        const resetPersonFormButton = document.getElementById("resetPersonFormButton");
        const refreshLoginLogsButton = document.getElementById("refreshLoginLogsButton");
        const loginLogKeywordInput = document.getElementById("loginLogKeywordInput");
        const loginLogTimeRangeInput = document.getElementById("loginLogTimeRangeInput");
        const loginLogTimeRangePicker = document.getElementById("loginLogTimeRangePicker");
        const loginLogOperationTypeSelect = document.getElementById("loginLogOperationTypeSelect");
        const loginLogPageSizeInput = document.getElementById("loginLogPageSizeInput");
        const loginLogTable = document.getElementById("loginLogTable");
        const loginLogPageCards = document.getElementById("loginLogPageCards");
        const loginLogPane = document.getElementById("loginLogPane");
        const operationLogPane = document.getElementById("operationLogPane");
        const operationLogKeywordInput = document.getElementById("operationLogKeywordInput");
        const operationLogModuleSelect = document.getElementById("operationLogModuleSelect");
        const operationLogTimeRangeInput = document.getElementById("operationLogTimeRangeInput");
        const operationLogTimeRangePicker = document.getElementById("operationLogTimeRangePicker");
        const operationLogTable = document.getElementById("operationLogTable");
        const operationLogPageCards = document.getElementById("operationLogPageCards");
        let settingsOpen = false;
        let currentSettingsTab = "general";
        let settingsScope = null;
        let settingsPeople = [];
        let settingsRoles = [];
        let currentOrgPermissions = {
            admin: false,
            permissions: new Set()
        };
        let orgPermissionsLoaded = false;
        let roleSettingsInitialized = false;
        let roleSettingsController = null;
        let selectedUnitId = null;
        let selectedPersonId = null;
        let currentAvatarMediaId = null;
        let isSettingsLoading = false;
        let unitTreeKeyword = "";
        let peopleTreeKeyword = "";
        let contextMenuUnitId = null;
        let contextMenuPersonId = null;
        let contextMenuPeopleUnitId = null;
        let pendingDeleteUnitId = null;
        let toastTimer = null;
        let systemConfigFormData = null;
        let activeSystemConfigTab = "system-login";
        let currentLoginUserPromise = null;
        let loginLogState = {
            page: 1,
            pageSize: 10,
            total: 0,
            keyword: "",
            startTime: "",
            endTime: "",
            operationType: "",
            rangePickerLeftMonth: new Date(new Date().getFullYear(), new Date().getMonth(), 1),
            rangePickerRightMonth: new Date(new Date().getFullYear(), new Date().getMonth() + 1, 1),
            showRangeTime: false,
            rangePickerMode: "date",
            rangePickerPanelIndex: 0
        };
        let operationLogState = {
            page: 1,
            pageSize: 10,
            total: 0,
            keyword: "",
            startTime: "",
            endTime: "",
            moduleCode: "",
            rangePickerLeftMonth: new Date(new Date().getFullYear(), new Date().getMonth(), 1),
            rangePickerRightMonth: new Date(new Date().getFullYear(), new Date().getMonth() + 1, 1),
            showRangeTime: false,
            rangePickerMode: "date",
            rangePickerPanelIndex: 0
        };
        let operationLogRangeQueryTimer = null;

        const expandedUnitIds = new Set();
        const expandedPeopleGroupIds = new Set();

        const panelTitleMap = {
            general: "常规",
            units: "部门",
            roles: "岗位",
            people: "人员",
            logs: "日志"
        };

        const basePath = window.location.pathname.replace(/\/[^/]*$/, "");

        function apiUrl(path) {
            return `${basePath}${path}`;
        }

        function hasOrgPermission(code) {
            return Boolean(currentOrgPermissions.admin || currentOrgPermissions.permissions.has(code));
        }

        function normalizeSettingsPanelText() {
            const setText = (element, text) => {
                if (element) {
                    element.textContent = text;
                }
            };
            const setMenuText = (button, text) => {
                if (!button) {
                    return;
                }
                setText(button.querySelector(".settings-menu__icon"), "•");
                setText(button.querySelector("span:last-child"), text);
            };
            setText(document.querySelector(".settings-shell__menu-title"), "设置");
            if (closeSettingsButton) {
                closeSettingsButton.textContent = "×";
                closeSettingsButton.setAttribute("aria-label", "关闭设置");
            }
            setMenuText(generalTabButton, "常规");
            setMenuText(unitsTabButton, "部门");
            setMenuText(peopleTabButton, "人员");
            setMenuText(rolesTabButton, "岗位");
            setMenuText(logsTabButton, "日志");
            setText(settingsPanelTitle, panelTitleMap[currentSettingsTab] || "设置");
            setText(document.querySelector(".settings-drawer__title span"), "按当前岗位权限展示可管理的系统配置、组织数据和日志内容。");
            setText(systemConfigSectionTitle, "主系统配置");
            setText(refreshSystemConfigButton, "刷新");
            setText(document.querySelector("[form='systemConfigForm']"), "保存配置");
            setText(systemConfigFields ? systemConfigFields.querySelector(".settings-empty") : null, "正在加载配置。");
            if (logsPanel) {
                setText(logsPanel.querySelector("[data-log-tab='login']"), "登录日志");
                setText(logsPanel.querySelector("[data-log-tab='operation']"), "操作日志");
                const labels = Array.from(logsPanel.querySelectorAll(".settings-log-filters .settings-field span"));
                setText(labels[0], "关键词");
                setText(labels[1], "操作类型");
                setText(labels[2], "时间范围");
            }
            setText(refreshLoginLogsButton, "刷新");
            if (loginLogKeywordInput) {
                loginLogKeywordInput.placeholder = "姓名 / 部门 / IP / 说明";
            }
            if (loginLogTimeRangeInput) {
                loginLogTimeRangeInput.placeholder = "请选择时间范围";
            }
            if (loginLogOperationTypeSelect) {
                ["全部", "登录成功", "登录失败", "退出登录"].forEach((label, index) => {
                    if (loginLogOperationTypeSelect.options[index]) {
                        loginLogOperationTypeSelect.options[index].textContent = label;
                    }
                });
            }
            setText(loginLogTable ? loginLogTable.querySelector(".settings-empty") : null, "正在加载登录日志。");
            if (loginLogPageCards) {
                loginLogPageCards.setAttribute("aria-label", "登录日志分页");
            }
            setText(operationLogTable ? operationLogTable.querySelector(".settings-empty") : null, "正在加载操作日志。");
            if (operationLogPageCards) {
                operationLogPageCards.setAttribute("aria-label", "操作日志分页");
            }
        }

        function canViewPeople() {
            return hasOrgPermission("general.person.view");
        }

        function canViewGeneral() {
            return hasOrgPermission("general.config");
        }

        function canUpdateGeneralConfig() {
            return canUpdateSystemLoginConfig() || canUpdateLoginPageConfig();
        }

        function canViewLogs() {
            return canViewLoginLog() || canViewOperationLog();
        }

        function canViewSystemLogin() {
            return hasOrgPermission("general.config.login");
        }

        function canViewLoginPage() {
            return hasOrgPermission("general.config.login-page");
        }

        function canUpdateSystemLoginConfig() {
            return hasOrgPermission("general.config.login.update");
        }

        function canUpdateLoginPageConfig() {
            return hasOrgPermission("general.config.login-page.update");
        }

        function canViewLoginLog() {
            return hasOrgPermission("general.log.login") && hasOrgPermission("general.log.login.view");
        }

        function canViewOperationLog() {
            return hasOrgPermission("general.log.operation") && hasOrgPermission("general.log.operation.view");
        }

        function firstAllowedLogTab() {
            if (canViewLoginLog()) {
                return "login";
            }
            if (canViewOperationLog()) {
                return "operation";
            }
            return "";
        }

        function loginLogRangeContext() {
            return {
                state: loginLogState,
                input: loginLogTimeRangeInput,
                picker: loginLogTimeRangePicker,
                pane: loginLogPane,
                load: loadLoginLogs
            };
        }

        function operationLogRangeContext() {
            return {
                state: operationLogState,
                input: operationLogTimeRangeInput,
                picker: operationLogTimeRangePicker,
                pane: operationLogPane,
                load: loadOperationLogs
            };
        }

        function canUpdateCurrentSystemConfig() {
            return activeSystemConfigTab === "login-page" ? canUpdateLoginPageConfig() : canUpdateSystemLoginConfig();
        }

        function canViewUnits() {
            return hasOrgPermission("general.unit.view");
        }

        function canCreateUnits() {
            return hasOrgPermission("general.unit.create");
        }

        function canUpdateUnits() {
            return hasOrgPermission("general.unit.update");
        }

        function canDeleteUnits() {
            return hasOrgPermission("general.unit.delete");
        }

        function canViewRoles() {
            return hasOrgPermission("general.role.view");
        }

        function canCreatePeople() {
            return hasOrgPermission("general.person.create");
        }

        function canUpdatePeople() {
            return hasOrgPermission("general.person.update");
        }

        function canDeletePeople() {
            return hasOrgPermission("general.person.delete");
        }

        function canAssignPersonRoles() {
            return hasOrgPermission("general.person.role.assign");
        }

        function setFormInputsDisabled(form, disabled) {
            if (!form) {
                return;
            }
            form.querySelectorAll("input, select, textarea").forEach((control) => {
                if (control.type !== "hidden") {
                    control.disabled = disabled;
                }
            });
        }

        function showRequiredMessage(label) {
            showSettingsError(`请填写${label}`);
        }

        function requireValue(control, label) {
            if (!control || !String(control.value || "").trim()) {
                showRequiredMessage(label);
                if (control && !control.disabled && typeof control.focus === "function") {
                    control.focus();
                }
                return false;
            }
            return true;
        }

        function canEditUnitForm() {
            return unitEditIdInput && unitEditIdInput.value ? canUpdateUnits() : canCreateUnits();
        }

        function canEditPersonForm() {
            return personEditIdInput && personEditIdInput.value ? canUpdatePeople() : canCreatePeople();
        }

        function firstAllowedSettingsTab() {
            if (canViewGeneral()) {
                return "general";
            }
            if (canViewUnits()) {
                return "units";
            }
            if (canViewPeople()) {
                return "people";
            }
            if (canViewRoles()) {
                return "roles";
            }
            if (canViewLogs()) {
                return "logs";
            }
            return "";
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

        function showSettingsToast(message) {
            if (!settingsToast) {
                return;
            }
            if (toastTimer) {
                window.clearTimeout(toastTimer);
            }
            settingsToast.textContent = message || "";
            settingsToast.hidden = !message;
            if (!message) {
                return;
            }
            toastTimer = window.setTimeout(() => {
                settingsToast.hidden = true;
                settingsToast.textContent = "";
            }, 2200);
        }

        function hideUnitTreeContextMenu() {
            contextMenuUnitId = null;
            if (!unitTreeContextMenu) {
                return;
            }
            unitTreeContextMenu.hidden = true;
            unitTreeContextMenu.style.left = "";
            unitTreeContextMenu.style.top = "";
        }

        function hidePeopleTreeContextMenu() {
            contextMenuPersonId = null;
            contextMenuPeopleUnitId = null;
            if (!peopleTreeContextMenu) {
                return;
            }
            peopleTreeContextMenu.hidden = true;
            peopleTreeContextMenu.style.left = "";
            peopleTreeContextMenu.style.top = "";
        }

        function closeDeleteConfirmDialog() {
            pendingDeleteUnitId = null;
            if (!settingsConfirmDialog) {
                return;
            }
            settingsConfirmDialog.hidden = true;
        }

        function openDeleteConfirmDialog(unit) {
            if (!settingsConfirmDialog || !settingsConfirmMessage || !unit) {
                return;
            }
            pendingDeleteUnitId = String(unit.id);
            settingsConfirmMessage.textContent = `确认删除部门"${unit.unitName}"吗？`;
            settingsConfirmDialog.hidden = false;
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
            const openingTab = currentSettingsTab;
            settingsDrawer.classList.toggle("is-open", open);
            settingsDrawer.setAttribute("aria-hidden", String(!open));
            settingsBackdrop.hidden = !open;
            document.body.classList.toggle("settings-effects-muted", open);
            document.body.style.overflow = open ? "hidden" : "";
            showSettingsError("");
            showSettingsToast("");
            hideUnitTreeContextMenu();
            hidePeopleTreeContextMenu();
            closeDeleteConfirmDialog();
            if (open) {
                normalizeSettingsPanelText();
                loadCurrentLoginUser().finally(() => loadSettingsData().then(() => {
                    if (!currentSettingsTab || currentSettingsTab === openingTab) {
                        setSettingsTab(firstAllowedSettingsTab());
                    } else {
                        setSettingsTab(currentSettingsTab);
                    }
                }));
            }
        }

        function setSettingsTab(tab) {
            if (tab === "general" && !canViewGeneral()) {
                tab = firstAllowedSettingsTab();
            }
            if (tab === "logs" && !canViewLogs()) {
                tab = firstAllowedSettingsTab();
            }
            if (tab === "people" && !canViewPeople()) {
                tab = firstAllowedSettingsTab();
            }
            if (tab === "units" && !canViewUnits()) {
                tab = firstAllowedSettingsTab();
            }
            if (tab === "roles" && !canViewRoles()) {
                tab = firstAllowedSettingsTab();
            }
            if (!tab) {
                currentSettingsTab = "";
                generalTabButton.classList.toggle("active", false);
                unitsTabButton.classList.toggle("active", false);
                peopleTabButton.classList.toggle("active", false);
                if (rolesTabButton) {
                    rolesTabButton.classList.toggle("active", false);
                }
                if (logsTabButton) {
                    logsTabButton.classList.toggle("active", false);
                }
                generalPanel.hidden = true;
                unitsPanel.hidden = true;
                peoplePanel.hidden = true;
                if (rolesPanel) {
                    rolesPanel.hidden = true;
                }
                if (logsPanel) {
                    logsPanel.hidden = true;
                }
                settingsPanelTitle.textContent = "设置";
                applyPeoplePermissionState();
                return;
            }
            currentSettingsTab = tab;
            generalTabButton.classList.toggle("active", tab === "general");
            unitsTabButton.classList.toggle("active", tab === "units");
            peopleTabButton.classList.toggle("active", tab === "people");
            if (rolesTabButton) {
                rolesTabButton.classList.toggle("active", tab === "roles");
            }
            if (logsTabButton) {
                logsTabButton.classList.toggle("active", tab === "logs");
            }
            generalPanel.hidden = tab !== "general" || !canViewGeneral();
            unitsPanel.hidden = tab !== "units" || !canViewUnits();
            peoplePanel.hidden = tab !== "people" || !canViewPeople();
            if (rolesPanel) {
                rolesPanel.hidden = tab !== "roles" || !canViewRoles();
            }
            if (logsPanel) {
                logsPanel.hidden = tab !== "logs" || !canViewLogs();
            }
            settingsPanelTitle.textContent = panelTitleMap[tab] || "设置";
            showSettingsError("");
            hideUnitTreeContextMenu();
            hidePeopleTreeContextMenu();
            if (settingsDrawerBody) {
                settingsDrawerBody.scrollTop = 0;
            }
            normalizeSettingsPanelText();
            if (tab === "general") {
                if (!canViewSystemLogin() && canViewLoginPage()) {
                    activeSystemConfigTab = "login-page";
                }
                if (systemConfigSectionTitle) {
                    systemConfigSectionTitle.textContent = "主系统配置";
                }
                renderSystemConfigForm(systemConfigFormData);
                applyPeoplePermissionState();
            }
            if (tab === "logs") {
                setLogTab(firstAllowedLogTab());
            }
            if (tab === "roles") {
                loadRoleSettingsPanel().catch((error) => showSettingsError(error.message));
            }
            applyPeoplePermissionState();
        }

        function applyPeoplePermissionState() {
            const visible = canViewPeople();
            if (generalTabButton) {
                generalTabButton.hidden = !canViewGeneral();
            }
            if (generalPanel) {
                generalPanel.hidden = !canViewGeneral() || currentSettingsTab !== "general";
            }
            if (logsTabButton) {
                logsTabButton.hidden = !canViewLogs();
            }
            if (logsPanel) {
                logsPanel.hidden = !canViewLogs() || currentSettingsTab !== "logs";
            }
            if (currentSettingsTab === "logs") {
                setLogTab(firstAllowedLogTab());
            }
            if (refreshSystemConfigButton) {
                refreshSystemConfigButton.hidden = !canViewGeneral();
            }
            const saveConfigButton = document.querySelector("[form='systemConfigForm']");
            if (saveConfigButton) {
                saveConfigButton.hidden = !canUpdateCurrentSystemConfig();
            }
            if (unitsTabButton) {
                unitsTabButton.hidden = !canViewUnits();
            }
            if (unitsPanel) {
                unitsPanel.hidden = !canViewUnits() || currentSettingsTab !== "units";
            }
            const unitFormEditable = canEditUnitForm();
            setFormInputsDisabled(unitEditorForm, !unitFormEditable);
            const saveUnitButton = unitEditorForm ? unitEditorForm.querySelector("button[type='submit']") : null;
            if (saveUnitButton) {
                saveUnitButton.hidden = !unitFormEditable;
            }
            if (resetUnitFormButton) {
                resetUnitFormButton.hidden = !canCreateUnits();
            }
            if (unitTreeContextMenu) {
                const createUnitButton = unitTreeContextMenu.querySelector("[data-action='create-unit']");
                const deleteUnitButton = unitTreeContextMenu.querySelector("[data-action='delete-unit']");
                if (createUnitButton) {
                    createUnitButton.hidden = !canCreateUnits();
                }
                if (deleteUnitButton) {
                    deleteUnitButton.hidden = !canDeleteUnits();
                }
            }
            if (peopleTabButton) {
                peopleTabButton.hidden = !visible;
            }
            if (rolesTabButton) {
                rolesTabButton.hidden = !canViewRoles();
            }
            if (rolesPanel) {
                rolesPanel.hidden = !canViewRoles() || currentSettingsTab !== "roles";
            }
            if (peoplePanel) {
                peoplePanel.hidden = !visible || currentSettingsTab !== "people";
            }
            if (!visible && currentSettingsTab === "people") {
                setSettingsTab(firstAllowedSettingsTab());
                return;
            }
            if (showPersonCreateButton) {
                showPersonCreateButton.hidden = !canCreatePeople();
            }
            const personFormEditable = canEditPersonForm();
            setFormInputsDisabled(personEditorForm, !personFormEditable);
            if (personAvatarButton) {
                personAvatarButton.disabled = !personFormEditable;
            }
            const savePersonButton = personEditorForm ? personEditorForm.querySelector("button[type='submit']") : null;
            if (savePersonButton) {
                savePersonButton.hidden = !personFormEditable;
            }
            if (resetPersonFormButton) {
                resetPersonFormButton.hidden = !canCreatePeople();
            }
            if (peopleCreateMenuButton) {
                peopleCreateMenuButton.hidden = !canCreatePeople();
            }
            if (peopleDeleteMenuButton) {
                peopleDeleteMenuButton.hidden = !canDeletePeople();
            }
            if (personRolePickerList) {
                syncPersonRolePickerPermissionState();
            }
        }

        async function loadRoleSettingsPanel() {
            if (!roleSettingsMount || roleSettingsInitialized) {
                return;
            }
            if (!roleSettingsMount.querySelector(".role-settings")) {
                const response = await fetch("./org-role-settings.html?v=20260601-role-form-actions1");
                roleSettingsMount.innerHTML = await response.text();
            }
            if (typeof window.initOrgRoleSettings !== "function") {
                throw new Error("岗位组件脚本未加载完成，请稍后重试");
            }
            roleSettingsController = await window.initOrgRoleSettings({
                root: roleSettingsMount,
                apiBasePath: basePath,
                buildHeaders: buildSettingsHeaders,
                notify: showSettingsToast,
                showError: showSettingsError,
                permissions: Array.from(currentOrgPermissions.permissions),
                admin: currentOrgPermissions.admin
            });
            roleSettingsInitialized = true;
        }

        async function reloadRoleSettingsPanel() {
            if (roleSettingsInitialized && roleSettingsController && typeof roleSettingsController.reload === "function") {
                await roleSettingsController.reload();
            }
        }

        function buildSettingsHeaders(extraHeaders) {
            return new Headers(extraHeaders || {});
        }

        async function loadCurrentLoginUser() {
            if (window.hephaestusCurrentLoginUser) {
                return window.hephaestusCurrentLoginUser;
            }
            if (window.hephaestusCurrentLoginUserPromise) {
                currentLoginUserPromise = window.hephaestusCurrentLoginUserPromise;
                return currentLoginUserPromise;
            }
            if (!currentLoginUserPromise) {
                currentLoginUserPromise = fetch(apiUrl("/auth/me"))
                    .then((response) => response.ok ? response.json() : null)
                    .then((user) => {
                        window.hephaestusCurrentLoginUser = user;
                        return user;
                    })
                    .catch(() => null);
            }
            return currentLoginUserPromise;
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
            const text = await response.text();
            if (!text || !text.trim()) {
                return null;
            }
            return JSON.parse(text);
        }

        async function loadCurrentOrgPermissions() {
            const payload = await requestJson("/api/org/permissions/current");
            currentOrgPermissions = {
                admin: Boolean(payload && payload.admin),
                permissions: new Set((payload && payload.permissions) || [])
            };
            orgPermissionsLoaded = true;
            applyPeoplePermissionState();
            return currentOrgPermissions;
        }

        function formatLoginLogTime(value) {
            if (!value) {
                return "-";
            }
            const normalized = String(value).replace("T", " ");
            return normalized.length > 19 ? normalized.slice(0, 19) : normalized;
        }

        function padTimePart(value) {
            return String(value).padStart(2, "0");
        }

        function formatDateValue(date) {
            return `${date.getFullYear()}-${padTimePart(date.getMonth() + 1)}-${padTimePart(date.getDate())}`;
        }

        function formatDateTimeValue(date, endOfDay) {
            return `${formatDateValue(date)}T${endOfDay ? "23:59:59" : "00:00:00"}`;
        }

        function formatDateTimeText(value) {
            return value ? value.replace("T", " ") : "";
        }

        function formatDateTimeParam(value) {
            return value ? String(value).replace(" ", "T") : "";
        }

        function parseDateTimeValue(value) {
            if (!value) {
                return null;
            }
            const parts = value.slice(0, 10).split("-").map(Number);
            if (parts.length !== 3 || parts.some((part) => Number.isNaN(part))) {
                return null;
            }
            return new Date(parts[0], parts[1] - 1, parts[2]);
        }

        function normalizeDateTimeText(value) {
            const normalized = String(value || "").trim().replace("T", " ");
            const match = normalized.match(/^(\d{4})-(\d{2})-(\d{2})(?:\s+(\d{2}):(\d{2})(?::(\d{2}))?)?$/);
            if (!match) {
                return "";
            }
            return `${match[1]}-${match[2]}-${match[3]}T${match[4] || "00"}:${match[5] || "00"}:${match[6] || "00"}`;
        }

        function syncLogRangeFromInput(context) {
            const input = context.input;
            const state = context.state;
            if (!input || !input.value.trim()) {
                state.startTime = "";
                state.endTime = "";
                return true;
            }
            const parts = input.value.split(/\s+-\s+/);
            if (parts.length !== 2) {
                return false;
            }
            const start = normalizeDateTimeText(parts[0]);
            const end = normalizeDateTimeText(parts[1]);
            if (!start || !end) {
                return false;
            }
            state.startTime = start;
            state.endTime = end;
            updateLogTimeRangeText(context);
            const startDate = parseDateTimeValue(start);
            if (startDate) {
                state.rangePickerLeftMonth = new Date(startDate.getFullYear(), startDate.getMonth(), 1);
                state.rangePickerRightMonth = new Date(startDate.getFullYear(), startDate.getMonth() + 1, 1);
                state.rangePickerMode = "date";
            }
            return true;
        }

        function syncLogRangeBeforeQuery(context) {
            if (syncLogRangeFromInput(context)) {
                return true;
            }
            showSettingsError("时间范围格式应为：YYYY-MM-DD HH:mm:ss - YYYY-MM-DD HH:mm:ss");
            return false;
        }

        function syncLoginLogRangeFromInput() {
            return syncLogRangeFromInput(loginLogRangeContext());
        }

        function syncLoginLogRangeBeforeQuery() {
            return syncLogRangeBeforeQuery(loginLogRangeContext());
        }

        function updateLogTimeRangeText(context) {
            const input = context.input;
            const state = context.state;
            if (!input) {
                return;
            }
            const start = formatDateTimeText(state.startTime);
            const end = formatDateTimeText(state.endTime);
            input.value = start && end ? `${start} - ${end}` : "";
            input.title = "";
        }

        function updateLoginLogTimeRangeText() {
            updateLogTimeRangeText(loginLogRangeContext());
        }

        function sameDate(left, right) {
            return left && right
                && left.getFullYear() === right.getFullYear()
                && left.getMonth() === right.getMonth()
                && left.getDate() === right.getDate();
        }

        function buildLoginLogMonthCells(monthDate) {
            const firstDay = new Date(monthDate.getFullYear(), monthDate.getMonth(), 1);
            const start = new Date(firstDay);
            start.setDate(firstDay.getDate() - firstDay.getDay());
            return Array.from({ length: 42 }, (_, index) => {
                const date = new Date(start);
                date.setDate(start.getDate() + index);
                return date;
            });
        }

        function renderLogTimeRangePicker(context) {
            const state = context.state;
            const picker = context.picker;
            if (!picker) {
                return;
            }
            const startTimeParts = ((state.startTime || "T00:00:00").slice(11, 16) || "00:00").split(":");
            const endTimeParts = ((state.endTime || "T23:59:59").slice(11, 16) || "23:59").split(":");
            const timeColumn = (type, part, selected) => `
                <div class="settings-time-range-picker__time-column">
                    ${Array.from({ length: part === "hour" ? 24 : 60 }, (_, index) => {
                        const value = padTimePart(index);
                        const active = value === selected ? " is-selected" : "";
                        return `<button class="settings-time-range-picker__time-option${active}" type="button" data-range-time-type="${type}" data-range-time-part="${part}" data-range-time-value="${value}">${value}</button>`;
                    }).join("")}
                </div>
            `;
            const months = [state.rangePickerLeftMonth, state.rangePickerRightMonth];
            const startDate = parseDateTimeValue(state.startTime);
            const endDate = parseDateTimeValue(state.endTime);
            const monthPanels = months.map((monthDate) => {
                const panelIndex = monthDate.getTime() === months[0].getTime() ? 0 : 1;
                const isActivePanel = state.rangePickerPanelIndex === panelIndex;
                let selector = "";
                if (isActivePanel && state.rangePickerMode === "year") {
                    const startYear = Math.floor(monthDate.getFullYear() / 12) * 12;
                    selector = `<div class="settings-time-range-picker__selector">${Array.from({ length: 12 }, (_, index) => {
                        const year = startYear + index;
                        const active = year === monthDate.getFullYear() ? " is-selected" : "";
                        return `<button class="settings-time-range-picker__selector-item${active}" type="button" data-range-year="${year}" data-panel-index="${panelIndex}">${year}</button>`;
                    }).join("")}</div>`;
                } else if (isActivePanel && state.rangePickerMode === "month") {
                    selector = `<div class="settings-time-range-picker__selector">${Array.from({ length: 12 }, (_, index) => {
                        const monthNumber = index + 1;
                        const active = index === monthDate.getMonth() ? " is-selected" : "";
                        return `<button class="settings-time-range-picker__selector-item${active}" type="button" data-range-month="${index}" data-panel-index="${panelIndex}">${monthNumber}月</button>`;
                    }).join("")}</div>`;
                }
                const cells = buildLoginLogMonthCells(monthDate).map((date) => {
                    const classes = ["settings-time-range-picker__day"];
                    if (date.getMonth() !== monthDate.getMonth()) {
                        classes.push("is-outside");
                    }
                    if (startDate && endDate && date >= startDate && date <= endDate) {
                        classes.push("is-in-range");
                    }
                    if (sameDate(date, startDate) || sameDate(date, endDate)) {
                        classes.push("is-selected");
                    }
                    return `<button class="${classes.join(" ")}" type="button" data-date="${formatDateValue(date)}">${date.getDate()}</button>`;
                }).join("");
                return `
                    <div class="settings-time-range-picker__month">
                        ${selector || `<div class="settings-time-range-picker__week"><span>日</span><span>一</span><span>二</span><span>三</span><span>四</span><span>五</span><span>六</span></div><div class="settings-time-range-picker__days">${cells}</div>`}
                    </div>
                `;
            }).join("");
            const datePanel = `
                <div class="settings-time-range-picker__nav">
                    <button type="button" data-range-nav="-12">«</button>
                    <button type="button" data-range-nav="-1">‹</button>
                    <span class="settings-time-range-picker__titles">
                        ${months.map((monthDate, panelIndex) => `<span><button type="button" data-range-mode="year" data-panel-index="${panelIndex}">${monthDate.getFullYear()}年</button><button type="button" data-range-mode="month" data-panel-index="${panelIndex}">${monthDate.getMonth() + 1}月</button></span>`).join("")}
                    </span>
                    <button type="button" data-range-nav="1">›</button>
                    <button type="button" data-range-nav="12">»</button>
                </div>
                <div class="settings-time-range-picker__months">${monthPanels}</div>
            `;
            const timePanel = `
                <div class="settings-time-range-picker__time">
                    <div class="settings-time-range-picker__time-title">开始时间</div>
                    <div class="settings-time-range-picker__time-title">结束时间</div>
                    ${timeColumn("start", "hour", startTimeParts[0])}
                    ${timeColumn("start", "minute", startTimeParts[1])}
                    ${timeColumn("end", "hour", endTimeParts[0])}
                    ${timeColumn("end", "minute", endTimeParts[1])}
                </div>
            `;
            picker.innerHTML = `
                ${state.showRangeTime ? timePanel : datePanel}
                <div class="settings-time-range-picker__footer">
                    <button type="button" data-range-action="time">${state.showRangeTime ? "选择日期" : "选择时间"}</button>
                    <button type="button" data-range-action="clear">清空</button>
                    <button type="button" data-range-action="confirm">确定</button>
                </div>
            `;
            if (state.showRangeTime) {
                requestAnimationFrame(() => scrollSelectedLogTimeColumns(context));
            }
        }

        function renderLoginLogTimeRangePicker() {
            renderLogTimeRangePicker(loginLogRangeContext());
        }

        function scrollSelectedLogTimeColumns(context) {
            const picker = context.picker;
            if (!picker) {
                return;
            }
            picker.querySelectorAll(".settings-time-range-picker__time-column").forEach((column) => {
                const selected = column.querySelector(".settings-time-range-picker__time-option.is-selected");
                if (selected) {
                    column.scrollTop = selected.offsetTop;
                }
            });
        }

        function scrollSelectedLoginLogTimeColumns() {
            scrollSelectedLogTimeColumns(loginLogRangeContext());
        }

        function setLogTimeRangePickerOpen(context, open) {
            const state = context.state;
            const input = context.input;
            const picker = context.picker;
            const pane = context.pane;
            if (!picker) {
                return;
            }
            if (open) {
                state.showRangeTime = false;
                state.rangePickerMode = "date";
                renderLogTimeRangePicker(context);
                if (input && pane) {
                    const inputRect = input.getBoundingClientRect();
                    const paneRect = pane.getBoundingClientRect();
                    const pickerWidth = Math.min(405, paneRect.width);
                    const left = Math.min(
                        Math.max(inputRect.left - paneRect.left, 0),
                        Math.max(paneRect.width - pickerWidth, 0)
                    );
                    picker.style.left = `${left}px`;
                    picker.style.top = `${inputRect.bottom - paneRect.top + 6}px`;
                }
                picker.hidden = false;
                return;
            }
            picker.hidden = true;
        }

        function setLoginLogTimeRangePickerOpen(open) {
            setLogTimeRangePickerOpen(loginLogRangeContext(), open);
        }

        function chooseLogRangeDate(context, value) {
            const date = parseDateTimeValue(value);
            if (!date) {
                return;
            }
            const state = context.state;
            const currentStart = parseDateTimeValue(state.startTime);
            const currentEnd = parseDateTimeValue(state.endTime);
            if (!currentStart || currentEnd) {
                state.startTime = formatDateTimeValue(date, false);
                state.endTime = "";
            } else if (date < currentStart) {
                state.endTime = state.startTime;
                state.startTime = formatDateTimeValue(date, false);
            } else {
                state.endTime = formatDateTimeValue(date, true);
            }
            updateLogTimeRangeText(context);
            renderLogTimeRangePicker(context);
        }

        function chooseLoginLogRangeDate(value) {
            chooseLogRangeDate(loginLogRangeContext(), value);
        }

        function applyLogRangeTime(context) {
            const state = context.state;
            const picker = context.picker;
            if (!picker) {
                return;
            }
            const getPart = (type, part, fallback) => {
                const selected = picker.querySelector(`[data-range-time-type='${type}'][data-range-time-part='${part}'].is-selected`);
                return selected ? selected.dataset.rangeTimeValue : fallback;
            };
            if (state.startTime) {
                const hour = getPart("start", "hour", "00");
                const minute = getPart("start", "minute", "00");
                state.startTime = `${state.startTime.slice(0, 11)}${hour}:${minute}:00`;
            }
            if (state.endTime) {
                const hour = getPart("end", "hour", "23");
                const minute = getPart("end", "minute", "59");
                state.endTime = `${state.endTime.slice(0, 11)}${hour}:${minute}:59`;
            }
            updateLogTimeRangeText(context);
        }

        function completeLogRangeBeforeQuery(context) {
            const state = context.state;
            if (state.startTime && !state.endTime) {
                state.endTime = `${state.startTime.slice(0, 11)}23:59:59`;
                updateLogTimeRangeText(context);
            }
        }

        function applyLoginLogRangeTime() {
            applyLogRangeTime(loginLogRangeContext());
        }

        function queryLoginLogsFromFilters() {
            completeLogRangeBeforeQuery(loginLogRangeContext());
            if (loginLogState.showRangeTime) {
                applyLoginLogRangeTime();
            }
            if (!syncLoginLogRangeBeforeQuery()) {
                return;
            }
            setLoginLogTimeRangePickerOpen(false);
            loginLogState.page = 1;
            loadLoginLogs().catch((error) => showSettingsError(error.message));
        }

        function handleLogRangePickerClick(event, context, queryCallback) {
            event.stopPropagation();
            const state = context.state;
            const picker = context.picker;
            const dayButton = event.target.closest("[data-date]");
            if (dayButton) {
                chooseLogRangeDate(context, dayButton.dataset.date);
                return;
            }
            const navButton = event.target.closest("[data-range-nav]");
            if (navButton) {
                const offset = Number(navButton.dataset.rangeNav) || 0;
                state.rangePickerLeftMonth = new Date(state.rangePickerLeftMonth.getFullYear(), state.rangePickerLeftMonth.getMonth() + offset, 1);
                state.rangePickerRightMonth = new Date(state.rangePickerRightMonth.getFullYear(), state.rangePickerRightMonth.getMonth() + offset, 1);
                renderLogTimeRangePicker(context);
                return;
            }
            const timeOptionButton = event.target.closest("[data-range-time-value]");
            if (timeOptionButton) {
                const selector = `[data-range-time-type='${timeOptionButton.dataset.rangeTimeType}'][data-range-time-part='${timeOptionButton.dataset.rangeTimePart}']`;
                picker.querySelectorAll(selector).forEach((button) => button.classList.remove("is-selected"));
                timeOptionButton.classList.add("is-selected");
                return;
            }
            const modeButton = event.target.closest("[data-range-mode]");
            if (modeButton) {
                state.rangePickerMode = modeButton.dataset.rangeMode || "date";
                state.rangePickerPanelIndex = Number(modeButton.dataset.panelIndex) || 0;
                renderLogTimeRangePicker(context);
                return;
            }
            const yearButton = event.target.closest("[data-range-year]");
            if (yearButton) {
                const year = Number(yearButton.dataset.rangeYear);
                const panelIndex = Number(yearButton.dataset.panelIndex) || 0;
                const key = panelIndex === 1 ? "rangePickerRightMonth" : "rangePickerLeftMonth";
                const current = state[key];
                state[key] = new Date(year, current.getMonth(), 1);
                state.rangePickerMode = "date";
                renderLogTimeRangePicker(context);
                return;
            }
            const monthButton = event.target.closest("[data-range-month]");
            if (monthButton) {
                const month = Number(monthButton.dataset.rangeMonth);
                const panelIndex = Number(monthButton.dataset.panelIndex) || 0;
                const key = panelIndex === 1 ? "rangePickerRightMonth" : "rangePickerLeftMonth";
                const current = state[key];
                state[key] = new Date(current.getFullYear(), month, 1);
                state.rangePickerMode = "date";
                renderLogTimeRangePicker(context);
                return;
            }
            const actionButton = event.target.closest("[data-range-action]");
            if (!actionButton) {
                return;
            }
            if (actionButton.dataset.rangeAction === "clear") {
                state.startTime = "";
                state.endTime = "";
                updateLogTimeRangeText(context);
                renderLogTimeRangePicker(context);
                return;
            }
            if (actionButton.dataset.rangeAction === "time") {
                state.showRangeTime = !state.showRangeTime;
                renderLogTimeRangePicker(context);
                return;
            }
            completeLogRangeBeforeQuery(context);
            queryCallback();
        }

        function loginLogOperationLabel(value) {
            const labels = {
                LOGIN_SUCCESS: "登录成功",
                LOGIN_FAILURE: "登录失败",
                LOGOUT: "退出登录"
            };
            return labels[value] || value || "-";
        }

        function collectLoginLogFilters() {
            const params = new URLSearchParams();
            if (loginLogPageSizeInput && loginLogPageSizeInput.value) {
                loginLogState.pageSize = Number(loginLogPageSizeInput.value) || 10;
            }
            params.set("page", String(loginLogState.page));
            params.set("pageSize", String(loginLogState.pageSize));
            if (loginLogKeywordInput && loginLogKeywordInput.value.trim()) {
                params.set("keyword", loginLogKeywordInput.value.trim());
            }
            if (loginLogState.startTime) {
                params.set("startTime", formatDateTimeParam(loginLogState.startTime));
            }
            if (loginLogState.endTime) {
                params.set("endTime", formatDateTimeParam(loginLogState.endTime));
            }
            if (loginLogOperationTypeSelect && loginLogOperationTypeSelect.value) {
                params.set("operationType", loginLogOperationTypeSelect.value);
            }
            return params;
        }

        function getLoginLogPageNumbers(totalPages) {
            const currentPage = Math.min(Math.max(loginLogState.page, 1), totalPages);
            const edgeVisibleCount = 6;
            if (totalPages <= edgeVisibleCount + 1) {
                return Array.from({ length: totalPages }, (_, index) => ({ type: "page", page: index + 1 }));
            }
            if (currentPage <= edgeVisibleCount) {
                return [
                    ...Array.from({ length: edgeVisibleCount }, (_, index) => ({ type: "page", page: index + 1 })),
                    { type: "jump-next", page: edgeVisibleCount + 1, label: "»" },
                    { type: "page", page: totalPages }
                ];
            }
            if (currentPage >= totalPages - edgeVisibleCount + 1) {
                return [
                    { type: "page", page: 1 },
                    { type: "jump-prev", page: totalPages - edgeVisibleCount, label: "«" },
                    ...Array.from({ length: edgeVisibleCount }, (_, index) => ({ type: "page", page: totalPages - edgeVisibleCount + 1 + index }))
                ];
            }
            return [
                { type: "page", page: 1 },
                { type: "jump-prev", page: currentPage - edgeVisibleCount, label: "«" },
                { type: "page", page: currentPage - 2 },
                { type: "page", page: currentPage - 1 },
                { type: "page", page: currentPage },
                { type: "page", page: currentPage + 1 },
                { type: "page", page: currentPage + 2 },
                { type: "jump-next", page: currentPage + edgeVisibleCount, label: "»" },
                { type: "page", page: totalPages }
            ];
        }

        function renderLoginLogPageCards(totalPages) {
            if (!loginLogPageCards) {
                return;
            }
            const pages = getLoginLogPageNumbers(totalPages);
            const prevDisabled = loginLogState.page <= 1;
            const nextDisabled = loginLogState.page >= totalPages;
            const prevButton = `<button class="settings-log-page-card settings-log-page-card--nav" type="button" data-page-action="prev" ${prevDisabled ? "disabled" : ""}>上一页</button>`;
            const nextButton = `<button class="settings-log-page-card settings-log-page-card--nav" type="button" data-page-action="next" ${nextDisabled ? "disabled" : ""}>下一页</button>`;
            const pageButtons = pages.map((item) => {
                const active = item.page === loginLogState.page;
                const directionClass = item.page < loginLogState.page ? " is-prev" : item.page > loginLogState.page ? " is-next" : "";
                const activeClass = active ? " is-active" : "";
                const action = item.type === "page" ? "" : ` data-page-action="${item.type}"`;
                return `<button class="settings-log-page-card${directionClass}${activeClass}" type="button" data-page="${item.page}"${action} ${active ? 'aria-current="page"' : ""}>${item.label || item.page}</button>`;
            }).join("");
            const totalCard = `<span class="settings-log-page-card settings-log-page-card--total">共 ${loginLogState.total} 条</span>`;
            const pageSizeInput = `<label class="settings-log-page-size"><span>每页</span><input id="loginLogPageSizeInput" type="number" min="1" max="100" step="1" value="${loginLogState.pageSize}" aria-label="每页行数"></label>`;
            loginLogPageCards.innerHTML = `<div class="settings-log-page-stage">${prevButton}${pageButtons}${nextButton}${totalCard}${pageSizeInput}</div>`;
            bindLoginLogPageSizeInput();
        }

        function bindLoginLogPageSizeInput() {
            const pageSizeInput = document.getElementById("loginLogPageSizeInput");
            if (!pageSizeInput) {
                return;
            }
            pageSizeInput.addEventListener("change", () => {
                loginLogState.pageSize = Number(pageSizeInput.value) || 10;
                loginLogState.page = 1;
                loadLoginLogs().catch((error) => showSettingsError(error.message));
            });
        }

        function renderLoginLogs(pageData) {
            if (!loginLogTable || !canViewLoginLog()) {
                return;
            }
            const items = (pageData && pageData.items) || [];
            loginLogState.page = (pageData && pageData.page) || loginLogState.page;
            loginLogState.pageSize = (pageData && pageData.pageSize) || loginLogState.pageSize;
            loginLogState.total = (pageData && pageData.total) || 0;

            if (!items.length) {
                loginLogTable.innerHTML = '<div class="settings-empty">暂无登录日志。</div>';
            } else {
                const rows = items.map((item) => {
                    const success = Boolean(item.success);
                    const statusClass = success ? "is-success" : "is-failed";
                    const statusText = success ? "成功" : "失败";
                    return `
                        <tr>
                            <td>${escapeHtml(item.id || "-")}</td>
                            <td>${formatLoginLogTime(item.createdAt)}</td>
                            <td>${escapeHtml(loginLogOperationLabel(item.operationType))}</td>
                            <td>${escapeHtml(item.personName || "-")}</td>
                            <td>${escapeHtml(item.unitName || "-")}</td>
                            <td><span class="settings-log-status ${statusClass}">${statusText}</span></td>
                            <td>${escapeHtml(item.clientIp || "-")}</td>
                            <td>${escapeHtml(item.message || "-")}</td>
                        </tr>
                    `;
                }).join("");
                loginLogTable.innerHTML = `
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>时间</th>
                                <th>操作</th>
                                <th>姓名</th>
                                <th>部门</th>
                                <th>结果</th>
                                <th>客户端 IP</th>
                                <th>说明</th>
                            </tr>
                        </thead>
                        <tbody>${rows}</tbody>
                    </table>
                `;
            }

            const totalPages = Math.max(1, Math.ceil(loginLogState.total / loginLogState.pageSize));
            renderLoginLogPageCards(totalPages);
        }

        async function loadLoginLogs() {
            if (!loginLogTable) {
                return;
            }
            if (!syncLoginLogRangeBeforeQuery()) {
                return;
            }
            loginLogTable.innerHTML = '<div class="settings-empty">正在加载登录日志。</div>';
            const params = collectLoginLogFilters();
            const pageData = await requestJson(`/api/logs/login?${params.toString()}`);
            renderLoginLogs(pageData);
        }

        function collectOperationLogFilters() {
            if (operationLogKeywordInput && operationLogKeywordInput.value.trim()) {
                operationLogState.keyword = operationLogKeywordInput.value.trim();
            } else {
                operationLogState.keyword = "";
            }
            operationLogState.moduleCode = operationLogModuleSelect ? operationLogModuleSelect.value : "";
            const params = new URLSearchParams();
            params.set("page", String(operationLogState.page));
            params.set("pageSize", String(operationLogState.pageSize));
            if (operationLogState.keyword) {
                params.set("keyword", operationLogState.keyword);
            }
            if (operationLogState.moduleCode) {
                params.set("moduleCode", operationLogState.moduleCode);
            }
            if (operationLogState.startTime) {
                params.set("startTime", formatDateTimeParam(operationLogState.startTime));
            }
            if (operationLogState.endTime) {
                params.set("endTime", formatDateTimeParam(operationLogState.endTime));
            }
            return params;
        }

        function updateOperationLogTimeRangeText() {
            updateLogTimeRangeText(operationLogRangeContext());
        }

        function syncOperationLogRangeFromInput() {
            return syncLogRangeFromInput(operationLogRangeContext());
        }

        function syncOperationLogRangeBeforeQuery() {
            return syncLogRangeBeforeQuery(operationLogRangeContext());
        }

        function setOperationLogTimeRangePickerOpen(open) {
            setLogTimeRangePickerOpen(operationLogRangeContext(), open);
        }

        function queryOperationLogsFromFilters() {
            if (operationLogState.showRangeTime) {
                applyLogRangeTime(operationLogRangeContext());
            }
            completeLogRangeBeforeQuery(operationLogRangeContext());
            if (!syncOperationLogRangeBeforeQuery()) {
                return;
            }
            setOperationLogTimeRangePickerOpen(false);
            operationLogState.page = 1;
            loadOperationLogs().catch((error) => showSettingsError(error.message));
        }

        function scheduleOperationLogRangeQuery() {
            if (!operationLogTimeRangeInput) {
                return;
            }
            window.clearTimeout(operationLogRangeQueryTimer);
            operationLogRangeQueryTimer = window.setTimeout(() => {
                const value = operationLogTimeRangeInput.value.trim();
                if (!value || /^\d{4}-\d{2}-\d{2}(?:\s+\d{2}:\d{2}(?::\d{2})?)?\s+-\s+\d{4}-\d{2}-\d{2}(?:\s+\d{2}:\d{2}(?::\d{2})?)?$/.test(value)) {
                    queryOperationLogsFromFilters();
                }
            }, 500);
        }

        function renderOperationLogPageCards(totalPages) {
            if (!operationLogPageCards) {
                return;
            }
            const pages = getLoginLogPageNumbers(totalPages).map((item) => {
                if (item.ellipsis) {
                    return '<span class="settings-log-page-card settings-log-page-card--ellipsis">...</span>';
                }
                const active = item.page === operationLogState.page;
                const directionClass = item.page < operationLogState.page ? " is-prev" : item.page > operationLogState.page ? " is-next" : "";
                return `<button type="button" class="settings-log-page-card${active ? " is-active" : ""}${directionClass}" data-operation-page="${item.page}">${item.page}</button>`;
            }).join("");
            const prevDisabled = operationLogState.page <= 1;
            const nextDisabled = operationLogState.page >= totalPages;
            const prevButton = `<button type="button" class="settings-log-page-card settings-log-page-card--nav" data-operation-page-action="prev" ${prevDisabled ? "disabled" : ""}>‹</button>`;
            const nextButton = `<button type="button" class="settings-log-page-card settings-log-page-card--nav" data-operation-page-action="next" ${nextDisabled ? "disabled" : ""}>›</button>`;
            const totalCard = `<span class="settings-log-page-card settings-log-page-card--total">共 ${operationLogState.total} 条</span>`;
            const pageSizeInput = `<label class="settings-log-page-size"><span>每页</span><input id="operationLogPageSizeInput" type="number" min="1" max="100" step="1" value="${operationLogState.pageSize}" aria-label="每页行数"></label>`;
            operationLogPageCards.innerHTML = `<div class="settings-log-page-stage">${prevButton}${pages}${nextButton}${totalCard}${pageSizeInput}</div>`;
            const input = document.getElementById("operationLogPageSizeInput");
            if (input) {
                input.addEventListener("change", () => {
                    operationLogState.pageSize = Number(input.value) || 10;
                    operationLogState.page = 1;
                    loadOperationLogs().catch((error) => showSettingsError(error.message));
                });
            }
        }

        function renderOperationLogs(pageData) {
            if (!operationLogTable || !canViewOperationLog()) {
                return;
            }
            operationLogState.page = (pageData && pageData.page) || operationLogState.page;
            operationLogState.pageSize = (pageData && pageData.pageSize) || operationLogState.pageSize;
            operationLogState.total = (pageData && pageData.total) || 0;
            const items = (pageData && (pageData.items || pageData.list)) || [];
            if (!items.length) {
                operationLogTable.innerHTML = '<div class="settings-empty">暂无操作日志。</div>';
                renderOperationLogPageCards(1);
                return;
            }
            const rows = items.map((item, index) => {
                const statusClass = item.success ? "is-success" : "is-failed";
                const statusText = item.success ? "成功" : "失败";
                const operator = item.operatorName || item.operatorUsername || "-";
                const target = [item.targetType, item.targetName || item.targetId].filter(Boolean).join(" / ") || "-";
                return `
                    <tr>
                        <td>${formatLoginLogTime(item.createdAt)}</td>
                        <td>${escapeHtml(operator)}</td>
                        <td>${escapeHtml(item.moduleName || "-")}</td>
                        <td>${escapeHtml(item.actionName || "-")}</td>
                        <td>${escapeHtml(target)}</td>
                        <td><span class="settings-log-status ${statusClass}">${statusText}</span></td>
                        <td><button type="button" class="settings-log-detail-toggle" data-operation-log-detail="${index}">${escapeHtml(item.summary || "-")}</button></td>
                    </tr>
                    <tr class="settings-log-detail-row" data-operation-log-detail-row="${index}" hidden>
                        <td colspan="7">
                            <div class="settings-log-detail">
                                <div><strong>明细</strong>${escapeHtml(item.detail || "-")}</div>
                                <div><strong>IP</strong>${escapeHtml(item.clientIp || "-")}</div>
                                <div><strong>路径</strong>${escapeHtml([item.requestMethod, item.requestUri].filter(Boolean).join(" ") || "-")}</div>
                                <div><strong>UA</strong>${escapeHtml(item.userAgent || "-")}</div>
                            </div>
                        </td>
                    </tr>`;
            }).join("");
            operationLogTable.innerHTML = `
                <table>
                    <thead>
                        <tr>
                            <th>时间</th>
                            <th>操作人</th>
                            <th>模块</th>
                            <th>动作</th>
                            <th>对象</th>
                            <th>结果</th>
                            <th>摘要</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>`;
            renderOperationLogPageCards(Math.max(1, Math.ceil(operationLogState.total / operationLogState.pageSize)));
        }

        async function loadOperationLogs() {
            if (!operationLogTable || !canViewOperationLog()) {
                return;
            }
            if (!syncOperationLogRangeBeforeQuery()) {
                return;
            }
            operationLogTable.innerHTML = '<div class="settings-empty">正在加载操作日志。</div>';
            const pageData = await requestJson(`/api/logs/operations?${collectOperationLogFilters().toString()}`);
            renderOperationLogs(pageData);
        }

        function setLogTab(tab) {
            if (!tab) {
                if (logsPanel) {
                    logsPanel.querySelectorAll("[data-log-tab]").forEach((button) => {
                        button.hidden = true;
                        button.classList.toggle("active", false);
                    });
                }
                if (loginLogPane) {
                    loginLogPane.hidden = true;
                }
                if (operationLogPane) {
                    operationLogPane.hidden = true;
                }
                if (refreshLoginLogsButton) {
                    refreshLoginLogsButton.hidden = true;
                }
                return;
            }
            const activeTab = tab === "operation" ? "operation" : "login";
            if (activeTab === "login" && !canViewLoginLog()) {
                return setLogTab(firstAllowedLogTab());
            }
            if (activeTab === "operation" && !canViewOperationLog()) {
                return setLogTab(firstAllowedLogTab());
            }
            if (logsPanel) {
                logsPanel.querySelectorAll("[data-log-tab]").forEach((button) => {
                    button.hidden = button.dataset.logTab === "login" ? !canViewLoginLog() : !canViewOperationLog();
                    button.classList.toggle("active", button.dataset.logTab === activeTab);
                });
            }
            if (loginLogPane) {
                loginLogPane.hidden = activeTab !== "login";
            }
            if (operationLogPane) {
                operationLogPane.hidden = activeTab !== "operation";
            }
            if (refreshLoginLogsButton) {
                refreshLoginLogsButton.hidden = activeTab !== "login";
            }
            if (activeTab === "login") {
                loadLoginLogs().catch((error) => showSettingsError(error.message));
            } else if (activeTab === "operation") {
                loadOperationLogs().catch((error) => showSettingsError(error.message));
            }
        }

        function flattenSystemConfigFields(formData) {
            return ((formData && formData.sections) || []).flatMap((section) => section.fields || []);
        }

        function getSystemConfigTab(field) {
            const code = String((field && field.code) || "");
            if (code.startsWith("login.page.") || code.startsWith("login.mouse.")) {
                return "login-page";
            }
            return "system-login";
        }

        function getSystemConfigTabTitle(tab) {
            return tab === "login-page" ? "登录页面" : "系统登录";
        }

        function renderSystemConfigTabButton(tab, fields) {
            const title = getSystemConfigTabTitle(tab);
            const visible = tab === "login-page" ? canViewLoginPage() : canViewSystemLogin();
            if (!visible || !fields.length) {
                return "";
            }
            return `<button class="settings-config-tab${activeSystemConfigTab === tab ? " active" : ""}" type="button" data-config-tab="${tab}">${title}<span>${fields.length}</span></button>`;
        }

        function renderSystemConfigRows(fields) {
            const rows = [];
            for (let index = 0; index < fields.length; index += 2) {
                rows.push(`<div class="settings-config-row">${fields.slice(index, index + 2).map(renderSystemConfigField).join("")}</div>`);
            }
            return rows.join("");
        }

        function renderSystemConfigForm(formData) {
            systemConfigFormData = formData;
            if (!systemConfigFields) {
                return;
            }
            const sections = (formData && formData.sections) || [];
            if (!sections.length) {
                systemConfigFields.innerHTML = '<div class="settings-empty">暂无主系统配置。</div>';
                return;
            }
            const grouped = flattenSystemConfigFields(formData).reduce((result, field) => {
                const tab = getSystemConfigTab(field);
                result[tab].push(field);
                return result;
            }, { "system-login": [], "login-page": [] });
            const allowedTabs = {
                "system-login": canViewSystemLogin(),
                "login-page": canViewLoginPage()
            };
            if (!allowedTabs[activeSystemConfigTab] || !grouped[activeSystemConfigTab] || !grouped[activeSystemConfigTab].length) {
                activeSystemConfigTab = allowedTabs["system-login"] && grouped["system-login"].length ? "system-login" : "login-page";
            }
            const tabs = ["system-login", "login-page"].map((tab) => renderSystemConfigTabButton(tab, grouped[tab])).join("");
            const fields = renderSystemConfigRows(grouped[activeSystemConfigTab] || []);
            systemConfigFields.innerHTML = `
                <div class="settings-config-tabs">${tabs}</div>
                <section class="settings-config-section">
                    <h3>${getSystemConfigTabTitle(activeSystemConfigTab)}</h3>
                    <div class="settings-config-grid">${fields}</div>
                </section>
            `;
        }

        function renderSystemConfigField(field) {
            const type = String(field.componentType || "text").toLowerCase();
            const value = field.value == null ? "" : String(field.value);
            const code = escapeHtml(field.code || "");
            const label = escapeHtml(field.label || field.code || "");
            const help = field.helpText ? `<small>${escapeHtml(field.helpText)}</small>` : "";
            const placeholder = field.placeholder ? ` placeholder="${escapeHtml(field.placeholder)}"` : "";
            const disabled = canUpdateCurrentSystemConfig() ? "" : " disabled";
            let control = "";
            if (type === "textarea") {
                control = `<textarea name="${code}" rows="4"${placeholder}${disabled}>${escapeHtml(value)}</textarea>`;
            } else if (type === "switch") {
                const checked = value === "true" || value === "1" || value === "yes";
                control = `<label class="settings-config-switch"><input name="${code}" type="checkbox" ${checked ? "checked" : ""}${disabled}><span class="settings-config-switch__track"><span class="settings-config-switch__thumb"></span></span><span class="settings-config-switch__text">启用</span></label>`;
                return `
                    <label class="settings-field settings-config-field settings-config-field--switch">
                        <span>${label}</span>
                        ${control}
                        ${help}
                    </label>
                `;
            } else if (type === "select") {
                const options = (field.options || []).map((option) => {
                    const optionValue = option.value == null ? "" : String(option.value);
                    const selected = optionValue === value ? " selected" : "";
                    return `<option value="${escapeHtml(optionValue)}"${selected}>${escapeHtml(option.label || optionValue)}</option>`;
                }).join("");
                control = `<select name="${code}"${disabled}>${options}</select>`;
            } else if (type === "number") {
                control = `<input name="${code}" type="number" value="${escapeHtml(value)}"${placeholder}${disabled}>`;
            } else {
                const inputType = field.sensitive ? "password" : "text";
                control = `<input name="${code}" type="${inputType}" value="${escapeHtml(value)}"${placeholder}${disabled}>`;
            }
            return `
                <label class="settings-field settings-config-field">
                    <span>${label}</span>
                    ${control}
                    ${help}
                </label>
            `;
        }

        function collectSystemConfigValues() {
            const values = {};
            flattenSystemConfigFields(systemConfigFormData).forEach((field) => {
                const control = systemConfigForm && systemConfigForm.elements ? systemConfigForm.elements[field.code] : null;
                if (!control) {
                    values[field.code] = field.value == null ? "" : String(field.value);
                    return;
                }
                values[field.code] = control.type === "checkbox" ? (control.checked ? "true" : "false") : control.value;
            });
            return values;
        }

        async function loadSystemConfigForm() {
            if (!canViewGeneral()) {
                return;
            }
            const formData = await requestJson("/api/system-config/forms/main-system");
            renderSystemConfigForm(formData);
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
            applyPeoplePermissionState();
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
            applyPeoplePermissionState();
            if (!unitCodeInput.disabled) {
                unitCodeInput.focus();
            }
        }

        function resetAvatarPreview() {
            currentAvatarMediaId = null;
            personAvatarPreview.textContent = "B";
            personAvatarInput.value = "";
        }

        function getPersonInitial(person) {
            const source = (person && (person.personName || person.username)) || personNameInput.value || personUsernameInput.value || "B";
            return String(source).trim().slice(0, 1).toUpperCase() || "B";
        }

        function renderAvatarPreview(url, person) {
            if (url) {
                const image = document.createElement("img");
                image.src = url;
                image.alt = "头像";
                personAvatarPreview.replaceChildren(image);
                return;
            }
            personAvatarPreview.textContent = getPersonInitial(person);
        }

        function resetPersonForm() {
            selectedPersonId = null;
            personEditIdInput.value = "";
            personCodeInput.value = "";
            personUsernameInput.value = "";
            personPasswordInput.value = "";
            personNameInput.value = "";
            if (personUnitIdInput.options.length) {
                personUnitIdInput.value = personUnitIdInput.value || personUnitIdInput.options[0].value || "";
            } else {
                personUnitIdInput.value = "";
            }
            personMobileInput.value = "";
            personEmailInput.value = "";
            personRemarkInput.value = "";
            personEnabledInput.checked = true;
            resetAvatarPreview();
            renderPersonRolePicker([]);
            syncPersonRolePickerPermissionState();
            renderPeopleList(settingsPeople);
            applyPeoplePermissionState();
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
            renderAvatarPreview(person.avatarAccessUrl || "", person);
            renderPersonRolePicker(person.roles || []);
            syncPersonRolePickerPermissionState();
            renderPeopleList(settingsPeople);
            setSettingsTab("people");
            applyPeoplePermissionState();
            if (!personCodeInput.disabled) {
                personCodeInput.focus();
            }
        }

        function renderUnitsTree(units) {
            const visibleUnits = filterUnitTree(units, unitTreeKeyword);
            hideUnitTreeContextMenu();
            if (!visibleUnits || !visibleUnits.length) {
                unitsTree.innerHTML = '<div class="settings-empty">当前权限范围内暂无部门。</div>';
                return;
            }

            function renderNode(node) {
                const hasChildren = (node.children || []).length > 0;
                const expanded = hasChildren ? isExpanded(expandedUnitIds, node.id) : false;
                const activeClass = String(selectedUnitId) === String(node.id) ? " active" : "";
                const toggleIcon = hasChildren ? (expanded ? "▾" : "▸") : "";
                const children = hasChildren && expanded
                    ? `<ul>${node.children.map(renderNode).join("")}</ul>`
                    : "";
                return `
                    <li>
                        <div class="settings-tree__row${activeClass}" data-unit-id="${node.id}">
                            <button class="settings-tree__label" type="button" data-action="edit-unit" data-unit-id="${node.id}" title="${escapeHtml(node.unitName || "")}">
                                <span class="settings-tree__toggle" data-action="${hasChildren ? "toggle-unit" : ""}" data-unit-id="${node.id}">${toggleIcon}</span>
                                <span class="settings-tree__meta">
                                    <strong>${escapeHtml(node.unitName || "")}</strong>
                                </span>
                            </button>
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

            const normalizedKeyword = (peopleTreeKeyword || "").trim().toLowerCase();

            function renderPerson(person) {
                const avatar = person.avatarAccessUrl
                    ? `<img src="${escapeHtml(person.avatarAccessUrl)}" alt="${escapeHtml(person.personName || "头像")}">`
                    : `<span>${escapeHtml((person.personName || person.username || "B").slice(0, 1).toUpperCase())}</span>`;
                const activeClass = String(selectedPersonId) === String(person.id) ? " active" : "";
                return `
                    <li class="settings-person-tree__person${activeClass}" data-person-id="${person.id}">
                        <button class="settings-person-tree__item" type="button" data-action="edit-person" data-person-id="${person.id}">
                            <span class="settings-avatar settings-avatar--list">${avatar}</span>
                            <span class="settings-person-tree__meta">
                                <strong>${escapeHtml(person.personName || "")}</strong>
                            </span>
                        </button>
                    </li>
                `;
            }

            function renderUnitNode(unit, depth) {
                const children = unit.children || [];
                const persons = items.filter((person) => {
                    if (String(person.unitId) !== String(unit.id)) {
                        return false;
                    }
                    if (!normalizedKeyword) {
                        return true;
                    }
                    return String(person.personName || "").toLowerCase().includes(normalizedKeyword)
                        || String(person.username || "").toLowerCase().includes(normalizedKeyword)
                        || String(person.personCode || "").toLowerCase().includes(normalizedKeyword);
                });
                const renderedChildren = children
                    .map((child) => renderUnitNode(child, (depth || 0) + 1))
                    .filter(Boolean);
                const unitMatched = normalizedKeyword
                    ? String(unit.unitName || "").toLowerCase().includes(normalizedKeyword)
                        || String(unit.unitCode || "").toLowerCase().includes(normalizedKeyword)
                    : true;
                const hasChildren = children.length > 0;
                const hasPersons = persons.length > 0;
                const hasVisibleChildren = renderedChildren.length > 0;
                const hasVisibleContent = unitMatched || hasPersons || hasVisibleChildren;
                if (!hasVisibleContent) {
                    return "";
                }
                const forceExpanded = Boolean(normalizedKeyword) && (hasPersons || hasVisibleChildren);
                const expanded = (hasChildren || hasPersons)
                    ? (forceExpanded || isExpanded(expandedPeopleGroupIds, unit.id))
                    : false;
                const toggleIcon = (hasChildren || hasPersons) ? (expanded ? "▾" : "▸") : "";
                const nested = [];
                if (expanded) {
                    renderedChildren.forEach((childHtml) => nested.push(childHtml));
                    persons.forEach((person) => nested.push(renderPerson(person)));
                }
                return `
                    <li class="settings-person-tree__unit" data-depth="${depth || 0}">
                        <div class="settings-tree__row settings-tree__row--group">
                            <button class="settings-tree__label" type="button" data-action="${(hasChildren || hasPersons) ? "toggle-people-group" : ""}" data-unit-id="${unit.id}">
                                <span class="settings-tree__toggle">${toggleIcon}</span>
                                <span class="settings-tree__meta">
                                    <strong>${escapeHtml(unit.unitName || "")}</strong>
                                </span>
                            </button>
                        </div>
                        ${expanded && nested.length ? `<ul class="settings-person-tree__list">${nested.join("")}</ul>` : ""}
                    </li>
                `;
            }

            const treeHtml = ((settingsScope && settingsScope.units) || [])
                .map((unit) => renderUnitNode(unit, 0))
                .filter(Boolean)
                .join("");
            if (!treeHtml) {
                peopleList.innerHTML = '<div class="settings-empty">当前权限范围内暂无人员。</div>';
                return;
            }
            peopleList.innerHTML = `<ul class="settings-person-tree">${treeHtml}</ul>`;
        }

        function renderPersonRolePicker(selectedRoles) {
            if (!personRolePickerList) {
                return;
            }
            const canAssignRoles = canAssignPersonRoles();
            const selectedRoleList = (selectedRoles || [])
                .filter((role) => role && role.id)
                .map((role) => {
                    const matchedRole = settingsRoles.find((item) => String(item.id) === String(role.id));
                    return Object.assign({}, matchedRole || {}, role);
                })
                .filter((role) => role && role.id && role.roleName);
            const selected = new Set(selectedRoleList.map((role) => String(role.id)));
            const availableRoles = canAssignRoles
                ? settingsRoles.filter((role) => role && role.id && role.roleName && selected.has(String(role.id)))
                : selectedRoleList;
            personRolePickerList.classList.toggle("is-readonly", !canAssignRoles);
            personRolePickerList.innerHTML = availableRoles.length
                ? availableRoles.map((role) => `
                    <label class="person-role-picker__item">
                        <input type="checkbox" value="${role.id}" checked ${canAssignRoles ? "" : "disabled"}>
                        <span>${escapeHtml(role.roleName || "")} (${escapeHtml(role.unitName || "未分配部门")})</span>
                    </label>
                `).join("")
                : '<div class="settings-empty">暂无岗位。</div>';
        }
        function syncPersonRolePickerPermissionState() {
            if (!personRolePickerList) {
                return;
            }
            personRolePickerList.classList.toggle("is-disabled", !canAssignPersonRoles());
            personRolePickerList.querySelectorAll("input[type='checkbox']").forEach((input) => {
                input.disabled = !canAssignPersonRoles();
            });
        }

        function collectSelectedPersonRoleIds() {
            if (!personRolePickerList) {
                return [];
            }
            if (!canAssignPersonRoles()) {
                const currentPerson = settingsPeople.find((person) => String(person.id) === String(selectedPersonId));
                return (currentPerson && currentPerson.roles ? currentPerson.roles : [])
                    .map((role) => Number(role.id))
                    .filter((id) => !Number.isNaN(id));
            }
            return Array.from(personRolePickerList.querySelectorAll("input[type='checkbox']:checked"))
                .map((item) => Number(item.value));
        }

        function scrollPersonIntoView(personId) {
            if (!peopleList || !personId) {
                return;
            }
            const target = peopleList.querySelector(`.settings-person-tree__person[data-person-id="${CSS.escape(String(personId))}"]`);
            if (target && typeof target.scrollIntoView === "function") {
                target.scrollIntoView({ block: "nearest", inline: "nearest" });
            }
        }

        function beginCreatePerson(unitId) {
            if (!canCreatePeople()) {
                return;
            }
            resetPersonForm();
            if (unitId && personUnitIdInput) {
                personUnitIdInput.value = String(unitId);
            }
            setSettingsTab("people");
            personCodeInput.focus();
        }

        async function loadPeople() {
            if (!canViewPeople()) {
                settingsPeople = [];
                settingsRoles = [];
                renderPeopleList([]);
                renderPersonRolePicker([]);
                return;
            }
            const [people, roles] = await Promise.all([
                requestJson("/api/org/persons"),
                canViewRoles() ? requestJson("/api/org/roles?enabled=true") : Promise.resolve([])
            ]);
            settingsPeople = people || [];
            settingsRoles = (roles || []).filter((role) => role && role.id && role.roleName);
            settingsPeople.forEach((person) => {
                if (person.unitId) {
                    expandedPeopleGroupIds.add(String(person.unitId));
                }
            });
            renderPeopleList(settingsPeople);
            const currentPerson = selectedPersonId
                ? settingsPeople.find((person) => String(person.id) === String(selectedPersonId))
                : null;
            renderPersonRolePicker(currentPerson ? currentPerson.roles || [] : []);
            syncPersonRolePickerPermissionState();
        }

        async function loadSettingsData() {
            if (!settingsOpen || isSettingsLoading) {
                return;
            }
            isSettingsLoading = true;
            showSettingsError("");
            try {
                await loadCurrentOrgPermissions();
                const scopePromise = (canViewUnits() || canViewPeople())
                    ? requestJson("/api/org/persons/current-scope")
                    : Promise.resolve({ units: [] });
                const peoplePromise = canViewPeople() ? loadPeople() : Promise.resolve(null);
                const configPromise = canViewGeneral() ? requestJson("/api/system-config/forms/main-system") : Promise.resolve(null);
                const [scope, configForm] = await Promise.all([scopePromise, configPromise]);
                settingsScope = scope || { units: [] };
                const units = settingsScope.units || [];
                initializeExpandedSets(units);
                renderUnitsTree(canViewUnits() ? units : []);
                populateUnitSelect(personUnitIdInput, canViewPeople() ? units : [], false);
                populateUnitSelect(unitParentSelect, canViewUnits() ? units : [], false);
                await peoplePromise;
                if (canViewPeople()) {
                    renderPeopleList(settingsPeople);
                    const currentPerson = selectedPersonId
                        ? settingsPeople.find((person) => String(person.id) === String(selectedPersonId))
                        : null;
                    renderPersonRolePicker(currentPerson ? currentPerson.roles || [] : []);
                    syncPersonRolePickerPermissionState();
                } else {
                    settingsPeople = [];
                    settingsRoles = [];
                    renderPeopleList([]);
                    renderPersonRolePicker([]);
                }
                const firstUnit = flattenUnitTree(units)[0];
                if (firstUnit && !personUnitIdInput.value) {
                    personUnitIdInput.value = String(firstUnit.id);
                }
                if (firstUnit && !unitParentSelect.value) {
                    unitParentSelect.value = String(firstUnit.id);
                    unitParentIdInput.value = String(firstUnit.id);
                }
                if (configForm) {
                    renderSystemConfigForm(configForm);
                }
            } catch (error) {
                showSettingsError(error && error.message ? error.message : "加载设置数据失败");
                renderUnitsTree([]);
                renderPeopleList([]);
            } finally {
                isSettingsLoading = false;
            }
        }

        function getDefaultCreateParentId() {
            if (selectedUnitId) {
                return selectedUnitId;
            }
            if (unitParentSelect.value) {
                return unitParentSelect.value;
            }
            const firstRoot = settingsScope ? (settingsScope.units || [])[0] : null;
            return firstRoot ? String(firstRoot.id) : "";
        }

        function beginCreateUnit(parentId) {
            if (!canCreateUnits()) {
                return;
            }
            resetUnitForm(parentId || getDefaultCreateParentId());
            setSettingsTab("units");
            unitCodeInput.focus();
        }

        async function deleteUnitById(unitId) {
            const unit = settingsScope ? findUnitById(settingsScope.units || [], unitId) : null;
            if (!unit) {
                return;
            }
            hideUnitTreeContextMenu();
            try {
                await requestJson(`/api/org/units/${unit.id}`, { method: "DELETE" });
                closeDeleteConfirmDialog();
                resetUnitForm("");
                await loadSettingsData();
                await reloadRoleSettingsPanel();
                showSettingsToast("删除成功");
            } catch (error) {
                showSettingsError(error.message);
            }
        }

        function showUnitTreeContextMenu(unitId, clientX, clientY) {
            if (!unitTreeContextMenu || !unitsSection) {
                return;
            }
            contextMenuUnitId = String(unitId);
            unitTreeContextMenu.hidden = false;
            const sectionRect = unitsSection.getBoundingClientRect();
            const menuWidth = unitTreeContextMenu.offsetWidth || 132;
            const menuHeight = unitTreeContextMenu.offsetHeight || 36;
            const left = Math.min(Math.max(clientX - sectionRect.left + 2, 6), sectionRect.width - menuWidth - 6);
            const top = Math.min(Math.max(clientY - sectionRect.top + 2, 6), sectionRect.height - menuHeight - 6);
            unitTreeContextMenu.style.left = `${left}px`;
            unitTreeContextMenu.style.top = `${top}px`;
        }

        function showPeopleTreeContextMenu(personId, clientX, clientY) {
            if (!peopleTreeContextMenu || !peopleSection) {
                return;
            }
            contextMenuPersonId = String(personId);
            contextMenuPeopleUnitId = null;
            if (peopleCreateMenuButton) {
                peopleCreateMenuButton.hidden = true;
            }
            if (peopleDeleteMenuButton) {
                peopleDeleteMenuButton.hidden = !canDeletePeople();
            }
            peopleTreeContextMenu.hidden = false;
            const sectionRect = peopleSection.getBoundingClientRect();
            const menuWidth = peopleTreeContextMenu.offsetWidth || 132;
            const menuHeight = peopleTreeContextMenu.offsetHeight || 36;
            const left = Math.min(Math.max(clientX - sectionRect.left + 2, 6), sectionRect.width - menuWidth - 6);
            const top = Math.min(Math.max(clientY - sectionRect.top + 2, 6), sectionRect.height - menuHeight - 6);
            peopleTreeContextMenu.style.left = `${left}px`;
            peopleTreeContextMenu.style.top = `${top}px`;
        }

        function showPeopleUnitContextMenu(unitId, clientX, clientY) {
            if (!peopleTreeContextMenu || !peopleSection) {
                return;
            }
            contextMenuPeopleUnitId = String(unitId);
            contextMenuPersonId = null;
            if (peopleCreateMenuButton) {
                peopleCreateMenuButton.hidden = !canCreatePeople();
            }
            if (peopleDeleteMenuButton) {
                peopleDeleteMenuButton.hidden = true;
            }
            peopleTreeContextMenu.hidden = false;
            const sectionRect = peopleSection.getBoundingClientRect();
            const menuWidth = peopleTreeContextMenu.offsetWidth || 132;
            const menuHeight = peopleTreeContextMenu.offsetHeight || 36;
            const left = Math.min(Math.max(clientX - sectionRect.left + 2, 6), sectionRect.width - menuWidth - 6);
            const top = Math.min(Math.max(clientY - sectionRect.top + 2, 6), sectionRect.height - menuHeight - 6);
            peopleTreeContextMenu.style.left = `${left}px`;
            peopleTreeContextMenu.style.top = `${top}px`;
        }

        settingsButton.addEventListener("click", () => setSettingsDrawerOpen(true));
        closeSettingsButton.addEventListener("click", () => setSettingsDrawerOpen(false));
        settingsBackdrop.addEventListener("click", () => setSettingsDrawerOpen(false));
        normalizeSettingsPanelText();
        generalTabButton.addEventListener("click", () => setSettingsTab("general"));
        unitsTabButton.addEventListener("click", () => setSettingsTab("units"));
        peopleTabButton.addEventListener("click", () => setSettingsTab("people"));
        if (rolesTabButton) {
            rolesTabButton.addEventListener("click", () => setSettingsTab("roles"));
        }
        if (logsTabButton) {
            logsTabButton.addEventListener("click", () => setSettingsTab("logs"));
        }
        refreshSettingsButton.addEventListener("click", () => loadSettingsData());
        refreshPeopleButton.addEventListener("click", () => loadPeople().catch((error) => showSettingsError(error.message)));

        if (refreshSystemConfigButton) {
            refreshSystemConfigButton.addEventListener("click", () => loadSystemConfigForm().catch((error) => showSettingsError(error.message)));
        }
        if (refreshLoginLogsButton) {
            refreshLoginLogsButton.addEventListener("click", () => {
                queryLoginLogsFromFilters();
            });
        }
        if (logsPanel) {
            logsPanel.addEventListener("click", (event) => {
                const tabButton = event.target.closest("[data-log-tab]");
                if (!tabButton) {
                    return;
                }
                setLogTab(tabButton.dataset.logTab);
            });
        }
        [loginLogKeywordInput, loginLogOperationTypeSelect].forEach((control) => {
            if (!control) {
                return;
            }
            control.addEventListener("input", () => {
                loginLogState.page = 1;
                loadLoginLogs().catch((error) => showSettingsError(error.message));
            });
            control.addEventListener("change", () => {
                loginLogState.page = 1;
                loadLoginLogs().catch((error) => showSettingsError(error.message));
            });
        });
        if (loginLogTimeRangeInput) {
            loginLogTimeRangeInput.addEventListener("click", () => setLoginLogTimeRangePickerOpen(true));
            loginLogTimeRangeInput.addEventListener("mouseenter", () => {
                loginLogTimeRangeInput.title = "";
            });
            loginLogTimeRangeInput.addEventListener("change", () => {
                if (syncLoginLogRangeBeforeQuery()) {
                    loginLogState.page = 1;
                    loadLoginLogs().catch((error) => showSettingsError(error.message));
                }
            });
            loginLogTimeRangeInput.addEventListener("keydown", (event) => {
                if (event.key !== "Enter") {
                    return;
                }
                event.preventDefault();
                if (!syncLoginLogRangeBeforeQuery()) {
                    return;
                }
                queryLoginLogsFromFilters();
            });
        }
        if (loginLogTimeRangePicker) {
            loginLogTimeRangePicker.addEventListener("click", (event) => {
                handleLogRangePickerClick(event, loginLogRangeContext(), queryLoginLogsFromFilters);
            });
        }
        if (loginLogPageCards) {
            loginLogPageCards.addEventListener("click", (event) => {
                const pageButton = event.target.closest("[data-page], [data-page-action]");
                if (!pageButton || pageButton.disabled) {
                    return;
                }
                const totalPages = Math.max(1, Math.ceil(loginLogState.total / loginLogState.pageSize));
                let page = Number(pageButton.dataset.page);
                if (pageButton.dataset.pageAction === "prev") {
                    page = Math.max(1, loginLogState.page - 1);
                } else if (pageButton.dataset.pageAction === "next") {
                    page = Math.min(totalPages, loginLogState.page + 1);
                }
                if (!page || page === loginLogState.page) {
                    return;
                }
                loginLogState.page = page;
                loadLoginLogs().catch((error) => showSettingsError(error.message));
            });
        }
        if (operationLogKeywordInput) {
            operationLogKeywordInput.addEventListener("keydown", (event) => {
                if (event.key !== "Enter") {
                    return;
                }
                event.preventDefault();
                operationLogState.page = 1;
                loadOperationLogs().catch((error) => showSettingsError(error.message));
            });
        }
        [operationLogModuleSelect].forEach((element) => {
            if (!element) {
                return;
            }
            element.addEventListener("change", () => {
                operationLogState.page = 1;
                loadOperationLogs().catch((error) => showSettingsError(error.message));
            });
        });
        if (operationLogTimeRangeInput) {
            operationLogTimeRangeInput.addEventListener("click", () => setOperationLogTimeRangePickerOpen(true));
            operationLogTimeRangeInput.addEventListener("mouseenter", () => {
                operationLogTimeRangeInput.title = "";
            });
            operationLogTimeRangeInput.addEventListener("change", () => {
                queryOperationLogsFromFilters();
            });
            operationLogTimeRangeInput.addEventListener("input", () => scheduleOperationLogRangeQuery());
            operationLogTimeRangeInput.addEventListener("keydown", (event) => {
                if (event.key !== "Enter") {
                    return;
                }
                event.preventDefault();
                queryOperationLogsFromFilters();
            });
        }
        if (operationLogTimeRangePicker) {
            operationLogTimeRangePicker.addEventListener("click", (event) => {
                handleLogRangePickerClick(event, operationLogRangeContext(), queryOperationLogsFromFilters);
            });
        }
        if (operationLogTable) {
            operationLogTable.addEventListener("click", (event) => {
                const toggle = event.target.closest("[data-operation-log-detail]");
                if (!toggle) {
                    return;
                }
                const row = operationLogTable.querySelector(`[data-operation-log-detail-row='${toggle.dataset.operationLogDetail}']`);
                if (row) {
                    row.hidden = !row.hidden;
                }
            });
        }
        if (operationLogPageCards) {
            operationLogPageCards.addEventListener("click", (event) => {
                const pageButton = event.target.closest("[data-operation-page], [data-operation-page-action]");
                if (!pageButton || pageButton.disabled) {
                    return;
                }
                const totalPages = Math.max(1, Math.ceil(operationLogState.total / operationLogState.pageSize));
                let page = Number(pageButton.dataset.operationPage);
                if (pageButton.dataset.operationPageAction === "prev") {
                    page = Math.max(1, operationLogState.page - 1);
                } else if (pageButton.dataset.operationPageAction === "next") {
                    page = Math.min(totalPages, operationLogState.page + 1);
                }
                if (!page || page === operationLogState.page) {
                    return;
                }
                operationLogState.page = page;
                loadOperationLogs().catch((error) => showSettingsError(error.message));
            });
        }
        if (systemConfigForm) {
            systemConfigForm.addEventListener("submit", async (event) => {
                event.preventDefault();
                if (!canUpdateCurrentSystemConfig()) {
                    showSettingsError("无权修改当前配置");
                    return;
                }
                try {
                    const saved = await requestJson("/api/system-config/forms/main-system", {
                        method: "PUT",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ values: collectSystemConfigValues() })
                    });
                    renderSystemConfigForm(saved);
                    window.dispatchEvent(new CustomEvent("hephaestus:system-config-updated", {
                        detail: { groupCode: "main-system", form: saved }
                    }));
                    showSettingsToast("保存成功");
                } catch (error) {
                    showSettingsError(error.message);
                }
            });
        }
        if (systemConfigFields) {
            systemConfigFields.addEventListener("click", (event) => {
                const tabButton = event.target.closest("[data-config-tab]");
                if (!tabButton) {
                    return;
                }
                activeSystemConfigTab = tabButton.dataset.configTab || "system-login";
                renderSystemConfigForm(systemConfigFormData);
                applyPeoplePermissionState();
            });
        }

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

        resetUnitFormButton.addEventListener("click", () => resetUnitForm(""));

        showPersonCreateButton.addEventListener("click", () => {
            if (!canCreatePeople()) {
                return;
            }
            resetPersonForm();
            setSettingsTab("people");
        });

        resetPersonFormButton.addEventListener("click", () => resetPersonForm());
        personAvatarButton.addEventListener("click", () => personAvatarInput.click());

        personAvatarInput.addEventListener("change", () => {
            const file = personAvatarInput.files && personAvatarInput.files[0];
            if (!file) {
                return;
            }
            const objectUrl = URL.createObjectURL(file);
            renderAvatarPreview(objectUrl);
        });

        document.addEventListener("click", (event) => {
            if (unitTreeContextMenu && !unitTreeContextMenu.hidden && !unitTreeContextMenu.contains(event.target)) {
                hideUnitTreeContextMenu();
            }
            if (peopleTreeContextMenu && !peopleTreeContextMenu.hidden && !peopleTreeContextMenu.contains(event.target)) {
                hidePeopleTreeContextMenu();
            }
            if (loginLogTimeRangePicker
                && !loginLogTimeRangePicker.hidden
                && !loginLogTimeRangePicker.contains(event.target)
                && loginLogTimeRangeInput
                && !loginLogTimeRangeInput.contains(event.target)) {
                setLoginLogTimeRangePickerOpen(false);
            }
            if (operationLogTimeRangePicker
                && !operationLogTimeRangePicker.hidden
                && !operationLogTimeRangePicker.contains(event.target)
                && operationLogTimeRangeInput
                && !operationLogTimeRangeInput.contains(event.target)) {
                setOperationLogTimeRangePickerOpen(false);
            }
        });

        document.addEventListener("scroll", () => {
            hideUnitTreeContextMenu();
            hidePeopleTreeContextMenu();
        }, true);

        if (settingsConfirmCancelButton) {
            settingsConfirmCancelButton.addEventListener("click", () => closeDeleteConfirmDialog());
        }

        if (settingsConfirmDialog) {
            settingsConfirmDialog.addEventListener("click", (event) => {
                if (event.target === settingsConfirmDialog) {
                    closeDeleteConfirmDialog();
                }
            });
        }

        if (settingsConfirmOkButton) {
            settingsConfirmOkButton.addEventListener("click", async () => {
                if (!pendingDeleteUnitId) {
                    return;
                }
                await deleteUnitById(pendingDeleteUnitId);
            });
        }

        unitsTree.addEventListener("contextmenu", (event) => {
            const row = event.target.closest(".settings-tree__row[data-unit-id]");
            if (!row) {
                hideUnitTreeContextMenu();
                return;
            }
            event.preventDefault();
            const unitId = row.dataset.unitId;
            const unit = settingsScope ? findUnitById(settingsScope.units || [], unitId) : null;
            if (!unit) {
                return;
            }
            fillUnitForm(unit);
            showUnitTreeContextMenu(unit.id, event.clientX, event.clientY);
        });

        unitsTree.addEventListener("click", (event) => {
            const button = event.target.closest("[data-action]");
            if (!button) {
                return;
            }
            hideUnitTreeContextMenu();
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
            if (action === "edit-unit") {
                fillUnitForm(unit);
            }
        });

        if (unitTreeContextMenu) {
            unitTreeContextMenu.addEventListener("click", (event) => {
                const button = event.target.closest("[data-action]");
                if (!button || !contextMenuUnitId) {
                    return;
                }
                const unit = settingsScope ? findUnitById(settingsScope.units || [], contextMenuUnitId) : null;
                hideUnitTreeContextMenu();
                if (!unit) {
                    return;
                }
            if (button.dataset.action === "create-unit") {
                    if (!canCreateUnits()) {
                        return;
                    }
                    beginCreateUnit(unit.id);
                    return;
                }
                if (button.dataset.action === "delete-unit") {
                    if (!canDeleteUnits()) {
                        return;
                    }
                    openDeleteConfirmDialog(unit);
                }
            });
        }

        peopleList.addEventListener("contextmenu", (event) => {
            const unitRow = event.target.closest(".settings-person-tree__unit .settings-tree__row[data-unit-id], .settings-person-tree__unit .settings-tree__label[data-unit-id]");
            if (unitRow) {
                event.preventDefault();
                const unitId = unitRow.dataset.unitId;
            const unit = settingsScope ? findUnitById(settingsScope.units || [], unitId) : null;
            if (!unit) {
                return;
            }
            contextMenuPeopleUnitId = String(unit.id);
            contextMenuPersonId = null;
            showPeopleUnitContextMenu(unit.id, event.clientX, event.clientY);
            return;
        }
            const row = event.target.closest(".settings-person-tree__person[data-person-id]");
            if (!row) {
                hidePeopleTreeContextMenu();
                return;
            }
            event.preventDefault();
            const personId = row.dataset.personId;
            const person = settingsPeople.find((item) => String(item.id) === String(personId));
            if (!person) {
                return;
            }
            fillPersonForm(person);
            showPeopleTreeContextMenu(person.id, event.clientX, event.clientY);
        });

        if (peopleTreeContextMenu) {
            peopleTreeContextMenu.addEventListener("click", async (event) => {
                const createButton = event.target.closest("[data-action='create-person']");
                if (createButton && contextMenuPeopleUnitId) {
                    if (!canCreatePeople()) {
                        hidePeopleTreeContextMenu();
                        return;
                    }
                    const unitId = contextMenuPeopleUnitId;
                    hidePeopleTreeContextMenu();
                    beginCreatePerson(unitId);
                    return;
                }

                const deleteButton = event.target.closest("[data-action='delete-person']");
                if (!deleteButton || !contextMenuPersonId) {
                    return;
                }
                if (!canDeletePeople()) {
                    hidePeopleTreeContextMenu();
                    return;
                }
                const person = settingsPeople.find((item) => String(item.id) === String(contextMenuPersonId));
                hidePeopleTreeContextMenu();
                if (!person) {
                    return;
                }
                try {
                    await requestJson(`/api/org/persons/${person.id}`, { method: "DELETE" });
                    resetPersonForm();
                    await loadPeople();
                    showSettingsToast("删除成功");
                } catch (error) {
                    showSettingsError(error.message);
                }
            });
        }

        peopleList.addEventListener("click", async (event) => {
            const button = event.target.closest("[data-action]");
            if (!button) {
                return;
            }
            hidePeopleTreeContextMenu();
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
                scrollPersonIntoView(person.id);
            }
        });

        unitEditorForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const editingUnit = Boolean(unitEditIdInput.value);
            if ((editingUnit && !canUpdateUnits()) || (!editingUnit && !canCreateUnits())) {
                showSettingsError("无权保存部门信息");
                return;
            }
            if (!requireValue(unitCodeInput, "部门编码") || !requireValue(unitNameInput, "部门名称")) {
                return;
            }
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
                            parentId: payload.parentId,
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
                await reloadRoleSettingsPanel();
                showSettingsToast("保存成功");
            } catch (error) {
                showSettingsError(error.message);
            }
        });

        personEditorForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const editingPerson = Boolean(personEditIdInput.value);
            if ((editingPerson && !canUpdatePeople()) || (!editingPerson && !canCreatePeople())) {
                showSettingsError("无权保存人员信息");
                return;
            }
            if (!requireValue(personCodeInput, "人员编码")
                || !requireValue(personUsernameInput, "用户名")
                || !requireValue(personNameInput, "人员姓名")
                || (!editingPerson && !requireValue(personPasswordInput, "密码"))
                || !requireValue(personUnitIdInput, "部门")) {
                return;
            }
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
                if (editingPerson) {
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
                    currentAvatarMediaId = updated && updated.avatarMediaId ? updated.avatarMediaId : null;
                    renderAvatarPreview(updated && updated.avatarAccessUrl ? updated.avatarAccessUrl : "", updated);
                }

                await loadPeople();
                showSettingsToast("保存成功");
            } catch (error) {
                showSettingsError(error.message);
            }
        });

        applyPeoplePermissionState();
    }

    window.initOrgSettings = initOrgSettings;
    initOrgSettings().catch((error) => {
        console.error("初始化组织设置面板失败", error);
    });
})();
