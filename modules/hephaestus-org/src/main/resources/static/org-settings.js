(function () {
    async function initOrgSettings() {
        if (window.hephaestusOrgSettingsInitialized) {
            return;
        }

        const mount = document.getElementById("orgSettingsMount");
        if (mount && !mount.hasChildNodes()) {
            const panelResponse = await fetch("./org-settings-panel.html?v=20260528-log-time-range1");
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
        const logsTabButton = document.getElementById("logsTabButton");
        const generalPanel = document.getElementById("generalPanel");
        const unitsPanel = document.getElementById("unitsPanel");
        const peoplePanel = document.getElementById("peoplePanel");
        const logsPanel = document.getElementById("logsPanel");
        const systemConfigForm = document.getElementById("systemConfigForm");
        const systemConfigFields = document.getElementById("systemConfigFields");
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

        const expandedUnitIds = new Set();
        const expandedPeopleGroupIds = new Set();

        const panelTitleMap = {
            general: "常规",
            units: "部门",
            people: "人员",
            logs: "日志"
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
            settingsConfirmMessage.textContent = `确认删除部门“${unit.unitName}”吗？`;
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
                loadCurrentLoginUser().finally(() => loadSettingsData());
            }
        }

        function setSettingsTab(tab) {
            currentSettingsTab = tab;
            generalTabButton.classList.toggle("active", tab === "general");
            unitsTabButton.classList.toggle("active", tab === "units");
            peopleTabButton.classList.toggle("active", tab === "people");
            if (logsTabButton) {
                logsTabButton.classList.toggle("active", tab === "logs");
            }
            generalPanel.hidden = tab !== "general";
            unitsPanel.hidden = tab !== "units";
            peoplePanel.hidden = tab !== "people";
            if (logsPanel) {
                logsPanel.hidden = tab !== "logs";
            }
            settingsPanelTitle.textContent = panelTitleMap[tab] || "设置";
            showSettingsError("");
            hideUnitTreeContextMenu();
            hidePeopleTreeContextMenu();
            if (settingsDrawerBody) {
                settingsDrawerBody.scrollTop = 0;
            }
            if (tab === "logs") {
                loadLoginLogs().catch((error) => showSettingsError(error.message));
            }
        }

        function buildSettingsHeaders(extraHeaders) {
            const headers = new Headers(extraHeaders || {});
            const personId = window.hephaestusCurrentLoginUser && window.hephaestusCurrentLoginUser.personId;
            headers.set("X-Person-Id", personId ? String(personId) : "100");
            return headers;
        }

        async function loadCurrentLoginUser() {
            if (window.hephaestusCurrentLoginUser) {
                return window.hephaestusCurrentLoginUser;
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

        function syncLoginLogRangeFromInput() {
            if (!loginLogTimeRangeInput || !loginLogTimeRangeInput.value.trim()) {
                loginLogState.startTime = "";
                loginLogState.endTime = "";
                return true;
            }
            const parts = loginLogTimeRangeInput.value.split(/\s+-\s+/);
            if (parts.length !== 2) {
                return false;
            }
            const start = normalizeDateTimeText(parts[0]);
            const end = normalizeDateTimeText(parts[1]);
            if (!start || !end) {
                return false;
            }
            loginLogState.startTime = start;
            loginLogState.endTime = end;
            updateLoginLogTimeRangeText();
            const startDate = parseDateTimeValue(start);
            if (startDate) {
                loginLogState.rangePickerLeftMonth = new Date(startDate.getFullYear(), startDate.getMonth(), 1);
                loginLogState.rangePickerRightMonth = new Date(startDate.getFullYear(), startDate.getMonth() + 1, 1);
                loginLogState.rangePickerMode = "date";
            }
            return true;
        }

        function updateLoginLogTimeRangeText() {
            if (!loginLogTimeRangeInput) {
                return;
            }
            const start = formatDateTimeText(loginLogState.startTime);
            const end = formatDateTimeText(loginLogState.endTime);
            loginLogTimeRangeInput.value = start && end ? `${start} - ${end}` : "";
            loginLogTimeRangeInput.title = "";
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

        function renderLoginLogTimeRangePicker() {
            if (!loginLogTimeRangePicker) {
                return;
            }
            const startTimeParts = ((loginLogState.startTime || "T00:00:00").slice(11, 16) || "00:00").split(":");
            const endTimeParts = ((loginLogState.endTime || "T23:59:59").slice(11, 16) || "23:59").split(":");
            const timeColumn = (type, part, selected) => `
                <div class="settings-time-range-picker__time-column">
                    ${Array.from({ length: part === "hour" ? 24 : 60 }, (_, index) => {
                        const value = padTimePart(index);
                        const active = value === selected ? " is-selected" : "";
                        return `<button class="settings-time-range-picker__time-option${active}" type="button" data-range-time-type="${type}" data-range-time-part="${part}" data-range-time-value="${value}">${value}</button>`;
                    }).join("")}
                </div>
            `;
            const months = [loginLogState.rangePickerLeftMonth, loginLogState.rangePickerRightMonth];
            const startDate = parseDateTimeValue(loginLogState.startTime);
            const endDate = parseDateTimeValue(loginLogState.endTime);
            const monthPanels = months.map((monthDate) => {
                const panelIndex = monthDate.getTime() === months[0].getTime() ? 0 : 1;
                const isActivePanel = loginLogState.rangePickerPanelIndex === panelIndex;
                let selector = "";
                if (isActivePanel && loginLogState.rangePickerMode === "year") {
                    const startYear = Math.floor(monthDate.getFullYear() / 12) * 12;
                    selector = `<div class="settings-time-range-picker__selector">${Array.from({ length: 12 }, (_, index) => {
                        const year = startYear + index;
                        const active = year === monthDate.getFullYear() ? " is-selected" : "";
                        return `<button class="settings-time-range-picker__selector-item${active}" type="button" data-range-year="${year}" data-panel-index="${panelIndex}">${year}</button>`;
                    }).join("")}</div>`;
                } else if (isActivePanel && loginLogState.rangePickerMode === "month") {
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
                    <div class="settings-time-range-picker__time-group">${timeColumn("start", "hour", startTimeParts[0])}${timeColumn("start", "minute", startTimeParts[1])}</div>
                    <div class="settings-time-range-picker__time-group">${timeColumn("end", "hour", endTimeParts[0])}${timeColumn("end", "minute", endTimeParts[1])}</div>
                </div>
            `;
            loginLogTimeRangePicker.innerHTML = `
                ${loginLogState.showRangeTime ? timePanel : datePanel}
                <div class="settings-time-range-picker__footer">
                    <button type="button" data-range-action="time">${loginLogState.showRangeTime ? "选择日期" : "选择时间"}</button>
                    <button type="button" data-range-action="clear">清空</button>
                    <button type="button" data-range-action="confirm">确定</button>
                </div>
            `;
        }

        function setLoginLogTimeRangePickerOpen(open) {
            if (!loginLogTimeRangePicker) {
                return;
            }
            if (open) {
                loginLogState.showRangeTime = false;
                loginLogState.rangePickerMode = "date";
                renderLoginLogTimeRangePicker();
                if (loginLogTimeRangeInput && loginLogPane) {
                    const inputRect = loginLogTimeRangeInput.getBoundingClientRect();
                    const paneRect = loginLogPane.getBoundingClientRect();
                    const pickerWidth = Math.min(405, paneRect.width);
                    const left = Math.min(
                        Math.max(inputRect.left - paneRect.left, 0),
                        Math.max(paneRect.width - pickerWidth, 0)
                    );
                    loginLogTimeRangePicker.style.left = `${left}px`;
                    loginLogTimeRangePicker.style.top = `${inputRect.bottom - paneRect.top + 6}px`;
                }
                loginLogTimeRangePicker.hidden = false;
                return;
            }
            loginLogTimeRangePicker.hidden = true;
        }

        function chooseLoginLogRangeDate(value) {
            const date = parseDateTimeValue(value);
            if (!date) {
                return;
            }
            const currentStart = parseDateTimeValue(loginLogState.startTime);
            const currentEnd = parseDateTimeValue(loginLogState.endTime);
            if (!currentStart || currentEnd) {
                loginLogState.startTime = formatDateTimeValue(date, false);
                loginLogState.endTime = "";
            } else if (date < currentStart) {
                loginLogState.endTime = loginLogState.startTime;
                loginLogState.startTime = formatDateTimeValue(date, false);
            } else {
                loginLogState.endTime = formatDateTimeValue(date, true);
            }
            updateLoginLogTimeRangeText();
            renderLoginLogTimeRangePicker();
        }

        function applyLoginLogRangeTime() {
            if (!loginLogTimeRangePicker) {
                return;
            }
            const getPart = (type, part, fallback) => {
                const selected = loginLogTimeRangePicker.querySelector(`[data-range-time-type='${type}'][data-range-time-part='${part}'].is-selected`);
                return selected ? selected.dataset.rangeTimeValue : fallback;
            };
            if (loginLogState.startTime) {
                const hour = getPart("start", "hour", "00");
                const minute = getPart("start", "minute", "00");
                loginLogState.startTime = `${loginLogState.startTime.slice(0, 11)}${hour}:${minute}:00`;
            }
            if (loginLogState.endTime) {
                const hour = getPart("end", "hour", "23");
                const minute = getPart("end", "minute", "59");
                loginLogState.endTime = `${loginLogState.endTime.slice(0, 11)}${hour}:${minute}:59`;
            }
            updateLoginLogTimeRangeText();
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
                params.set("startTime", loginLogState.startTime);
            }
            if (loginLogState.endTime) {
                params.set("endTime", loginLogState.endTime);
            }
            if (loginLogOperationTypeSelect && loginLogOperationTypeSelect.value) {
                params.set("operationType", loginLogOperationTypeSelect.value);
            }
            return params;
        }

        function getLoginLogPageNumbers(totalPages) {
            const visibleCount = Math.min(10, totalPages);
            let start = Math.max(1, loginLogState.page - Math.floor(visibleCount / 2));
            let end = start + visibleCount - 1;
            if (end > totalPages) {
                end = totalPages;
                start = Math.max(1, end - visibleCount + 1);
            }
            return Array.from({ length: end - start + 1 }, (_, index) => start + index);
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
            const pageButtons = pages.map((page) => {
                const active = page === loginLogState.page;
                const directionClass = page < loginLogState.page ? " is-prev" : page > loginLogState.page ? " is-next" : "";
                const activeClass = active ? " is-active" : "";
                return `<button class="settings-log-page-card${directionClass}${activeClass}" type="button" data-page="${page}" ${active ? 'aria-current="page"' : ""}>${page}</button>`;
            }).join("");
            const totalCard = `<span class="settings-log-page-card settings-log-page-card--total">共${loginLogState.total}条</span>`;
            const pageSizeInput = `<label class="settings-log-page-size"><span>每页行数</span><input id="loginLogPageSizeInput" type="number" min="1" max="100" step="1" value="${loginLogState.pageSize}"></label>`;
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
            if (!loginLogTable) {
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
                            <td>${escapeHtml(item.username || "-")}</td>
                            <td>${escapeHtml(item.personName || "-")}</td>
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
                                <th>账号</th>
                                <th>人员</th>
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
            loginLogTable.innerHTML = '<div class="settings-empty">正在加载登录日志。</div>';
            const params = collectLoginLogFilters();
            const pageData = await requestJson(`/api/logs/login?${params.toString()}`);
            renderLoginLogs(pageData);
        }

        function setLogTab(tab) {
            const activeTab = tab === "operation" ? "operation" : "login";
            if (loginLogPane) {
                loginLogPane.hidden = activeTab !== "login";
            }
            if (operationLogPane) {
                operationLogPane.hidden = activeTab !== "operation";
            }
            if (refreshLoginLogsButton) {
                refreshLoginLogsButton.hidden = activeTab !== "login";
            }
            document.querySelectorAll("[data-log-tab]").forEach((button) => {
                button.classList.toggle("active", button.dataset.logTab === activeTab);
            });
            if (activeTab === "login") {
                loadLoginLogs().catch((error) => showSettingsError(error.message));
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
            const activeClass = tab === activeSystemConfigTab ? " active" : "";
            return `<button class="settings-config-tab${activeClass}" type="button" data-config-tab="${tab}">${getSystemConfigTabTitle(tab)}<span>${fields.length}</span></button>`;
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
            if (!grouped[activeSystemConfigTab] || !grouped[activeSystemConfigTab].length) {
                activeSystemConfigTab = grouped["system-login"].length ? "system-login" : "login-page";
            }
            const tabs = ["system-login", "login-page"]
                .filter((tab) => grouped[tab].length)
                .map((tab) => renderSystemConfigTabButton(tab, grouped[tab]))
                .join("");
            const fields = renderSystemConfigRows(grouped[activeSystemConfigTab] || []);
            systemConfigFields.innerHTML = `
                <div class="settings-config-tabs" role="tablist">${tabs}</div>
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
            let control = "";
            if (type === "textarea") {
                control = `<textarea name="${code}" rows="4"${placeholder}>${escapeHtml(value)}</textarea>`;
            } else if (type === "switch") {
                const checked = value === "true" || value === "1" || value === "yes";
                control = `<label class="settings-config-switch"><input name="${code}" type="checkbox" ${checked ? "checked" : ""}><span class="settings-config-switch__track"><span class="settings-config-switch__thumb"></span></span><span class="settings-config-switch__text">启用</span></label>`;
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
                control = `<select name="${code}">${options}</select>`;
            } else if (type === "number") {
                control = `<input name="${code}" type="number" value="${escapeHtml(value)}"${placeholder}>`;
            } else {
                const inputType = field.sensitive ? "password" : "text";
                control = `<input name="${code}" type="${inputType}" value="${escapeHtml(value)}"${placeholder}>`;
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
            renderAvatarPreview(person.avatarAccessUrl || "", person);
            renderPeopleList(settingsPeople);
            setSettingsTab("people");
            personCodeInput.focus();
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
            resetPersonForm();
            if (unitId && personUnitIdInput) {
                personUnitIdInput.value = String(unitId);
            }
            setSettingsTab("people");
            personCodeInput.focus();
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
                await loadSystemConfigForm();
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
                peopleDeleteMenuButton.hidden = false;
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
                peopleCreateMenuButton.hidden = false;
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
        generalTabButton.addEventListener("click", () => setSettingsTab("general"));
        unitsTabButton.addEventListener("click", () => setSettingsTab("units"));
        peopleTabButton.addEventListener("click", () => setSettingsTab("people"));
        if (logsTabButton) {
            logsTabButton.addEventListener("click", () => setSettingsTab("logs"));
        }
        refreshSettingsButton.addEventListener("click", () => loadSettingsData());
        refreshPeopleButton.addEventListener("click", () => loadPeople().catch((error) => showSettingsError(error.message)));

        if (refreshSystemConfigButton) {
            refreshSystemConfigButton.addEventListener("click", () => loadSystemConfigForm().catch((error) => showSettingsError(error.message)));
        }
        if (refreshLoginLogsButton) {
            refreshLoginLogsButton.addEventListener("click", () => loadLoginLogs().catch((error) => showSettingsError(error.message)));
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
                if (syncLoginLogRangeFromInput()) {
                    loginLogState.page = 1;
                    loadLoginLogs().catch((error) => showSettingsError(error.message));
                } else {
                    showSettingsError("时间范围格式应为：YYYY-MM-DD HH:mm:ss - YYYY-MM-DD HH:mm:ss");
                }
            });
        }
        if (loginLogTimeRangePicker) {
            loginLogTimeRangePicker.addEventListener("click", (event) => {
                const dayButton = event.target.closest("[data-date]");
                if (dayButton) {
                    chooseLoginLogRangeDate(dayButton.dataset.date);
                    return;
                }
                const navButton = event.target.closest("[data-range-nav]");
                if (navButton) {
                    const offset = Number(navButton.dataset.rangeNav) || 0;
                    loginLogState.rangePickerLeftMonth = new Date(loginLogState.rangePickerLeftMonth.getFullYear(), loginLogState.rangePickerLeftMonth.getMonth() + offset, 1);
                    loginLogState.rangePickerRightMonth = new Date(loginLogState.rangePickerRightMonth.getFullYear(), loginLogState.rangePickerRightMonth.getMonth() + offset, 1);
                    renderLoginLogTimeRangePicker();
                    return;
                }
                const timeOptionButton = event.target.closest("[data-range-time-value]");
                if (timeOptionButton) {
                    const selector = `[data-range-time-type='${timeOptionButton.dataset.rangeTimeType}'][data-range-time-part='${timeOptionButton.dataset.rangeTimePart}']`;
                    loginLogTimeRangePicker.querySelectorAll(selector).forEach((button) => button.classList.remove("is-selected"));
                    timeOptionButton.classList.add("is-selected");
                    return;
                }
                const modeButton = event.target.closest("[data-range-mode]");
                if (modeButton) {
                    loginLogState.rangePickerMode = modeButton.dataset.rangeMode || "date";
                    loginLogState.rangePickerPanelIndex = Number(modeButton.dataset.panelIndex) || 0;
                    renderLoginLogTimeRangePicker();
                    return;
                }
                const yearButton = event.target.closest("[data-range-year]");
                if (yearButton) {
                    const year = Number(yearButton.dataset.rangeYear);
                    const panelIndex = Number(yearButton.dataset.panelIndex) || 0;
                    const key = panelIndex === 1 ? "rangePickerRightMonth" : "rangePickerLeftMonth";
                    const current = loginLogState[key];
                    loginLogState[key] = new Date(year, current.getMonth(), 1);
                    loginLogState.rangePickerMode = "date";
                    renderLoginLogTimeRangePicker();
                    return;
                }
                const monthButton = event.target.closest("[data-range-month]");
                if (monthButton) {
                    const month = Number(monthButton.dataset.rangeMonth);
                    const panelIndex = Number(monthButton.dataset.panelIndex) || 0;
                    const key = panelIndex === 1 ? "rangePickerRightMonth" : "rangePickerLeftMonth";
                    const current = loginLogState[key];
                    loginLogState[key] = new Date(current.getFullYear(), month, 1);
                    loginLogState.rangePickerMode = "date";
                    renderLoginLogTimeRangePicker();
                    return;
                }
                const actionButton = event.target.closest("[data-range-action]");
                if (!actionButton) {
                    return;
                }
                if (actionButton.dataset.rangeAction === "clear") {
                    loginLogState.startTime = "";
                    loginLogState.endTime = "";
                    updateLoginLogTimeRangeText();
                    renderLoginLogTimeRangePicker();
                    return;
                }
                if (actionButton.dataset.rangeAction === "time") {
                    loginLogState.showRangeTime = !loginLogState.showRangeTime;
                    renderLoginLogTimeRangePicker();
                    return;
                }
                applyLoginLogRangeTime();
                setLoginLogTimeRangePickerOpen(false);
                loginLogState.page = 1;
                loadLoginLogs().catch((error) => showSettingsError(error.message));
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
        if (systemConfigFields) {
            systemConfigFields.addEventListener("click", (event) => {
                const tabButton = event.target.closest("[data-config-tab]");
                if (!tabButton) {
                    return;
                }
                activeSystemConfigTab = tabButton.dataset.configTab || "system-login";
                renderSystemConfigForm(systemConfigFormData);
            });
        }

        if (systemConfigForm) {
            systemConfigForm.addEventListener("submit", async (event) => {
                event.preventDefault();
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
                    beginCreateUnit(unit.id);
                    return;
                }
                if (button.dataset.action === "delete-unit") {
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
                    const unitId = contextMenuPeopleUnitId;
                    hidePeopleTreeContextMenu();
                    beginCreatePerson(unitId);
                    return;
                }

                const deleteButton = event.target.closest("[data-action='delete-person']");
                if (!deleteButton || !contextMenuPersonId) {
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
                showSettingsToast("保存成功");
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
                    currentAvatarMediaId = updated && updated.avatarMediaId ? updated.avatarMediaId : null;
                    renderAvatarPreview(updated && updated.avatarAccessUrl ? updated.avatarAccessUrl : "", updated);
                }

                await loadPeople();
                showSettingsToast("保存成功");
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
