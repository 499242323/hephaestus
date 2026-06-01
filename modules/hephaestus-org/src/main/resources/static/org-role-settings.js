(function () {
    async function initOrgRoleSettings(options) {
        const root = options && options.root ? options.root : document;
        const apiBasePath = options && options.apiBasePath ? options.apiBasePath : window.location.pathname.replace(/\/[^/]*$/, "");
        const getHeaders = options && options.buildHeaders ? options.buildHeaders : defaultHeaders;
        const notify = options && options.notify ? options.notify : function () {};
        const showError = options && options.showError ? options.showError : function () {};
        const currentPermissions = new Set((options && options.permissions) || []);
        const currentAdmin = Boolean(options && options.admin);

        const roleTree = root.querySelector("#roleTree");
        const roleTreeContextMenu = root.querySelector("#roleTreeContextMenu");
        const roleSearchInput = root.querySelector("#roleSearchInput");
        const roleEmptyState = root.querySelector("#roleEmptyState");
        const saveRoleButton = root.querySelector("#saveRoleButton");
        const refreshRolePermissionsButton = root.querySelector("#refreshRolePermissionsButton");
        const roleEditIdInput = root.querySelector("#roleEditIdInput");
        const roleIdDisplayInput = root.querySelector("#roleIdDisplayInput");
        const roleCodeInput = root.querySelector("#roleCodeInput");
        const roleShortNameInput = root.querySelector("#roleShortNameInput");
        const roleNameInput = root.querySelector("#roleNameInput");
        const roleDescInput = root.querySelector("#roleDescInput");
        const roleUnitIdInput = root.querySelector("#roleUnitIdInput");
        const roleGroupInput = root.querySelector("#roleGroupInput");
        const roleTypeInput = root.querySelector("#roleTypeInput");
        const rolePropertyInput = root.querySelector("#rolePropertyInput");
        const roleSortOrderInput = root.querySelector("#roleSortOrderInput");
        const roleEnabledInput = root.querySelector("#roleEnabledInput");
        const rolePeopleSummary = root.querySelector("#rolePeopleSummary");
        const rolePeopleList = root.querySelector("#rolePeopleList");
        const rolePermissionList = root.querySelector("#rolePermissionList");
        const tabButtons = Array.from(root.querySelectorAll("[data-role-tab]"));
        const panels = {
            basic: root.querySelector("#roleBasicPanel"),
            people: root.querySelector("#rolePeoplePanel"),
            permissions: root.querySelector("#rolePermissionsPanel")
        };

        let roleTreeData = [];
        let permissions = [];
        let people = [];
        let roleTypes = [];
        let selectedRole = null;
        let selectedUnitId = null;
        let contextMenuRoleId = null;
        let contextMenuUnitId = null;
        let selectedPersonIds = new Set();
        let editorMode = "none";
        let keyword = "";
        const expandedRoleUnitIds = new Set();
        const expandedRolePeopleUnitIds = new Set();

        function hasPermission(code) {
            return currentAdmin || currentPermissions.has(code);
        }

        function canAssignRolePeople() {
            return hasPermission("general.role.person.assign");
        }

        function canCreateRole() {
            return hasPermission("general.role.create");
        }

        function canUpdateRole() {
            return hasPermission("general.role.update");
        }

        function canDeleteRole() {
            return hasPermission("general.role.delete");
        }

        function canRefreshRolePermissions() {
            return hasPermission("general.role.permission.refresh");
        }

        function canEditRoleForm() {
            return roleEditIdInput && roleEditIdInput.value ? canUpdateRole() : canCreateRole();
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
            showError(`请填写${label}`);
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

        if (roleTreeContextMenu && roleTreeContextMenu.parentElement !== document.body) {
            document.body.appendChild(roleTreeContextMenu);
        }
        if (roleIdDisplayInput && roleIdDisplayInput.closest(".settings-field")) {
            roleIdDisplayInput.closest(".settings-field").hidden = true;
            roleIdDisplayInput.closest(".settings-field").style.display = "none";
        }
        if (rolePropertyInput && rolePropertyInput.closest(".settings-field")) {
            rolePropertyInput.closest(".settings-field").hidden = true;
            rolePropertyInput.closest(".settings-field").style.display = "none";
        }

        function defaultHeaders(extraHeaders) {
            const headers = new Headers(extraHeaders || {});
            const personId = window.hephaestusCurrentLoginUser && window.hephaestusCurrentLoginUser.personId;
            headers.set("X-Person-Id", personId ? String(personId) : "100");
            return headers;
        }

        function apiUrl(path) {
            return `${apiBasePath}${path}`;
        }

        async function requestJson(path, options) {
            const requestOptions = options || {};
            requestOptions.headers = getHeaders(requestOptions.headers);
            const response = await fetch(apiUrl(path), requestOptions);
            if (!response.ok) {
                let message = `请求失败：${response.status}`;
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
            return text ? JSON.parse(text) : null;
        }

        function escapeHtml(text) {
            return String(text == null ? "" : text)
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;")
                .replace(/'/g, "&#39;");
        }

        function renderRoleTypes(selectedValue) {
            const selected = selectedValue || roleTypeInput.value || "normal";
            const items = roleTypes.length ? roleTypes : [
                { typeCode: "normal", typeName: "普通岗位" },
                { typeCode: "admin", typeName: "管理员" },
                { typeCode: "operator", typeName: "业务岗" },
                { typeCode: "support", typeName: "支撑岗" }
            ];
            roleTypeInput.innerHTML = items.map((item) => `
                <option value="${escapeHtml(item.typeCode)}">${escapeHtml(item.typeName)}</option>
            `).join("");
            roleTypeInput.value = items.some((item) => item.typeCode === selected) ? selected : "normal";
        }

        function hideRoleContextMenu() {
            contextMenuRoleId = null;
            contextMenuUnitId = null;
            if (!roleTreeContextMenu) {
                return;
            }
            roleTreeContextMenu.hidden = true;
            roleTreeContextMenu.style.left = "";
            roleTreeContextMenu.style.top = "";
        }

        function positionRoleContextMenu(x, y) {
            roleTreeContextMenu.style.left = "0px";
            roleTreeContextMenu.style.top = "0px";
            roleTreeContextMenu.hidden = false;
            const gap = 6;
            const width = roleTreeContextMenu.offsetWidth || 120;
            const height = roleTreeContextMenu.offsetHeight || 64;
            const maxLeft = Math.max(gap, window.innerWidth - width - gap);
            const maxTop = Math.max(gap, window.innerHeight - height - gap);
            roleTreeContextMenu.style.left = `${Math.min(Math.max(gap, x), maxLeft)}px`;
            roleTreeContextMenu.style.top = `${Math.min(Math.max(gap, y + gap), maxTop)}px`;
        }

        function showRoleContextMenu(roleId, x, y) {
            if (!roleTreeContextMenu) {
                return;
            }
            hideRoleContextMenu();
            contextMenuRoleId = String(roleId);
            contextMenuUnitId = null;
            const createButton = roleTreeContextMenu.querySelector("[data-action='create-role']");
            const deleteButton = roleTreeContextMenu.querySelector("[data-action='delete-role']");
            if (createButton) {
                createButton.hidden = true;
            }
            if (deleteButton) {
                deleteButton.hidden = !canDeleteRole();
            }
            positionRoleContextMenu(x, y);
        }

        function showUnitContextMenu(unitId, x, y) {
            if (!roleTreeContextMenu) {
                return;
            }
            hideRoleContextMenu();
            contextMenuRoleId = null;
            contextMenuUnitId = String(unitId);
            const createButton = roleTreeContextMenu.querySelector("[data-action='create-role']");
            const deleteButton = roleTreeContextMenu.querySelector("[data-action='delete-role']");
            if (createButton) {
                createButton.hidden = !canCreateRole();
            }
            if (deleteButton) {
                deleteButton.hidden = true;
            }
            positionRoleContextMenu(x, y);
        }

        function flattenUnits(nodes, result) {
            (nodes || []).forEach((node) => {
                if (node.type === "unit") {
                    result.push({ id: node.unitId, name: node.label });
                }
                flattenUnits(node.children || [], result);
            });
            return result;
        }

        function flattenUnitNodes(nodes, result) {
            (nodes || []).forEach((node) => {
                if (node.type === "unit") {
                    result.push(node);
                }
                flattenUnitNodes(node.children || [], result);
            });
            return result;
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

        function initializeExpandedSets() {
            expandedRoleUnitIds.clear();
            expandedRolePeopleUnitIds.clear();
            flattenUnitNodes(roleTreeData, []).forEach((unit) => {
                const hasRole = (unit.children || []).some((child) => child.type === "role");
                const hasUnit = (unit.children || []).some((child) => child.type === "unit");
                const hasPerson = people.some((person) => String(person.unitId) === String(unit.unitId));
                if (hasRole || hasUnit) {
                    expandedRoleUnitIds.add(String(unit.unitId));
                }
                if (hasPerson || hasUnit) {
                    expandedRolePeopleUnitIds.add(String(unit.unitId));
                }
            });
        }

        function populateUnitSelect() {
            const units = flattenUnits(roleTreeData, []);
            roleUnitIdInput.innerHTML = units.map((unit) => `<option value="${unit.id}">${escapeHtml(unit.name)}</option>`).join("");
            if (selectedUnitId) {
                roleUnitIdInput.value = String(selectedUnitId);
            }
        }

        function setEditorVisible(visible) {
            const editable = visible && canEditRoleForm();
            if (roleEmptyState) {
                roleEmptyState.hidden = visible;
            }
            if (saveRoleButton) {
                saveRoleButton.hidden = !editable;
            }
            if (refreshRolePermissionsButton) {
                refreshRolePermissionsButton.hidden = !visible || !roleEditIdInput.value || !canRefreshRolePermissions();
            }
            tabButtons.forEach((button) => {
                button.hidden = !visible || (button.dataset.roleTab === "people" && !canAssignRolePeople());
            });
            Object.values(panels).forEach((panel) => {
                if (panel) {
                    panel.hidden = !visible || panel.id !== "roleBasicPanel";
                }
            });
            setFormInputsDisabled(roleEditorForm, !editable);
            if (rolePermissionList) {
                rolePermissionList.querySelectorAll("input[type='checkbox']").forEach((input) => {
                    input.disabled = !editable;
                });
            }
            if (rolePeopleList) {
                rolePeopleList.querySelectorAll("input[type='checkbox']").forEach((input) => {
                    input.disabled = !canAssignRolePeople();
                });
            }
        }

        function nodeText(node) {
            return `${node.label || ""} ${node.roleCode || ""}`.toLowerCase();
        }

        function matchesNode(node) {
            return !keyword || nodeText(node).includes(keyword);
        }

        function renderTree(nodes) {
            function renderNode(node, depth) {
                if (node.type === "role") {
                    if (!matchesNode(node)) {
                        return "";
                    }
                    const activeClass = selectedRole && String(node.roleId) === String(selectedRole.id) ? " active" : "";
                    return `
                        <li class="settings-person-tree__person${activeClass}" data-type="role" data-role-id="${node.roleId}" data-unit-id="${node.unitId || ""}">
                            <button class="settings-person-tree__item" type="button" data-type="role" data-role-id="${node.roleId}" data-unit-id="${node.unitId || ""}">
                                <span class="settings-person-tree__meta">
                                    <strong>${escapeHtml(node.label || "")}</strong>
                                </span>
                            </button>
                        </li>
                    `;
                }

                const childHtml = (node.children || [])
                    .map((child) => renderNode(child, (depth || 0) + 1))
                    .filter(Boolean);
                if (keyword && !matchesNode(node) && !childHtml.length) {
                    return "";
                }
                const expanded = Boolean(keyword) || isExpanded(expandedRoleUnitIds, node.unitId);
                const toggleIcon = childHtml.length ? (expanded ? "▾" : "▸") : "";
                return `
                    <li class="settings-person-tree__unit" data-depth="${depth || 0}" data-type="unit" data-unit-id="${node.unitId || ""}">
                        <div class="settings-tree__row settings-tree__row--group" data-type="unit" data-unit-id="${node.unitId || ""}">
                            <button class="settings-tree__label" type="button" data-type="unit" data-unit-id="${node.unitId || ""}">
                                <span class="settings-tree__toggle">${toggleIcon}</span>
                                <span class="settings-tree__meta">
                                    <strong>${escapeHtml(node.label || "")}</strong>
                                </span>
                            </button>
                        </div>
                        ${expanded && childHtml.length ? `<ul class="settings-person-tree__list">${childHtml.join("")}</ul>` : ""}
                    </li>
                `;
            }

            const treeHtml = (nodes || []).map((node) => renderNode(node, 0)).filter(Boolean).join("");
            roleTree.innerHTML = treeHtml ? `<ul class="settings-person-tree">${treeHtml}</ul>` : '<div class="settings-empty">暂无岗位。</div>';
        }

        function renderRolePeopleSummary(role) {
            const items = role && role.people ? role.people : [];
            rolePeopleSummary.innerHTML = items.length
                ? `<ul class="role-people-summary__list">${items.map((person) => `
                    <li>
                        <strong>${escapeHtml(person.personName || "")}</strong>
                        <span>${escapeHtml(person.unitName || "未分配部门")}</span>
                    </li>
                `).join("")}</ul>`
                : '<div class="settings-empty">暂无人员。</div>';
        }

        function renderPeople(selectedPeople) {
            if (!canAssignRolePeople()) {
                rolePeopleList.innerHTML = '<div class="settings-empty">暂无岗位人员维护权限。</div>';
                return;
            }
            if (selectedPeople) {
                selectedPersonIds = new Set((selectedPeople || []).map((person) => String(person.id)));
            }

            function renderPerson(person) {
                const avatar = person.avatarAccessUrl
                    ? `<img src="${escapeHtml(person.avatarAccessUrl)}" alt="${escapeHtml(person.personName || "头像")}">`
                    : `<span>${escapeHtml((person.personName || person.username || "B").slice(0, 1).toUpperCase())}</span>`;
                return `
                    <li class="settings-person-tree__person role-person-check" data-person-id="${person.id}">
                        <label class="settings-person-tree__item role-person-check__label">
                            <input type="checkbox" value="${person.id}" ${selectedPersonIds.has(String(person.id)) ? "checked" : ""}>
                            <span class="settings-avatar settings-avatar--list">${avatar}</span>
                            <span class="settings-person-tree__meta">
                                <strong>${escapeHtml(person.personName || "")}</strong>
                            </span>
                        </label>
                    </li>
                `;
            }

            function renderUnitNode(unit, depth) {
                const unitPeople = people.filter((person) => String(person.unitId) === String(unit.unitId));
                const childUnits = (unit.children || []).filter((child) => child.type === "unit");
                const nested = [
                    ...childUnits.map((child) => renderUnitNode(child, (depth || 0) + 1)).filter(Boolean),
                    ...unitPeople.map(renderPerson)
                ];
                if (!nested.length) {
                    return "";
                }
                const expanded = isExpanded(expandedRolePeopleUnitIds, unit.unitId);
                return `
                    <li class="settings-person-tree__unit" data-depth="${depth || 0}" data-unit-id="${unit.unitId || ""}">
                        <div class="settings-tree__row settings-tree__row--group" data-unit-id="${unit.unitId || ""}">
                            <button class="settings-tree__label" type="button" data-action="toggle-role-people-unit" data-unit-id="${unit.unitId || ""}">
                                <span class="settings-tree__toggle">${expanded ? "▾" : "▸"}</span>
                                <span class="settings-tree__meta">
                                    <strong>${escapeHtml(unit.label || "")}</strong>
                                </span>
                            </button>
                        </div>
                        ${expanded ? `<ul class="settings-person-tree__list">${nested.join("")}</ul>` : ""}
                    </li>
                `;
            }

            const treeHtml = (roleTreeData || []).map((unit) => renderUnitNode(unit, 0)).filter(Boolean).join("");
            rolePeopleList.innerHTML = treeHtml ? `<ul class="settings-person-tree">${treeHtml}</ul>` : '<div class="settings-empty">暂无人员。</div>';
        }

        function fillRole(role) {
            editorMode = role ? "edit" : "none";
            selectedRole = role;
            roleEditIdInput.value = role && role.id ? String(role.id) : "";
            roleIdDisplayInput.value = role && role.id ? String(role.id) : "";
            roleCodeInput.value = role ? role.roleCode || "" : "";
            roleShortNameInput.value = role ? role.roleShortName || "" : "";
            roleNameInput.value = role ? role.roleName || "" : "";
            roleDescInput.value = role ? role.roleDesc || "" : "";
            roleUnitIdInput.value = role && role.unitId ? String(role.unitId) : (selectedUnitId ? String(selectedUnitId) : roleUnitIdInput.value);
            roleGroupInput.value = role ? role.roleGroup || "" : "";
            renderRoleTypes(role ? role.roleType || "normal" : "normal");
            rolePropertyInput.value = role ? role.roleProperty || "other" : "other";
            roleSortOrderInput.value = role && role.sortOrder != null ? String(role.sortOrder) : "0";
            roleEnabledInput.checked = !role || role.enabled !== false;
            renderPermissions(role && role.permissions ? role.permissions.map((permission) => permission.id) : []);
            renderRolePeopleSummary(role);
            renderPeople(role ? role.people || [] : []);
            renderTree(roleTreeData);
            setEditorVisible(Boolean(role));
            setTab("basic");
            if (roleCodeInput && !roleCodeInput.disabled) {
                roleCodeInput.focus();
            }
        }

        function beginCreateRole(unitId) {
            if (!canCreateRole()) {
                return;
            }
            editorMode = "create";
            selectedRole = null;
            selectedUnitId = unitId || selectedUnitId;
            selectedPersonIds = new Set();
            roleEditIdInput.value = "";
            roleIdDisplayInput.value = "";
            roleCodeInput.value = "";
            roleShortNameInput.value = "";
            roleNameInput.value = "";
            roleDescInput.value = "";
            if (selectedUnitId) {
                roleUnitIdInput.value = String(selectedUnitId);
            }
            roleGroupInput.value = "";
            renderRoleTypes("normal");
            rolePropertyInput.value = "other";
            roleSortOrderInput.value = "0";
            roleEnabledInput.checked = true;
            renderPermissions([]);
            renderRolePeopleSummary(null);
            renderPeople([]);
            renderTree(roleTreeData);
            setTab("basic");
            setEditorVisible(true);
            if (!roleCodeInput.disabled) {
                roleCodeInput.focus();
            }
        }

        function collectPermissionIds() {
            return Array.from(rolePermissionList.querySelectorAll("input[type='checkbox']:checked")).map((item) => Number(item.value));
        }

        function syncSectionPermissionButton(input) {
            const button = input.parentElement ? input.parentElement.querySelector(".role-permission-section__button") : null;
            if (button) {
                button.textContent = input.checked ? "已显示" : "隐藏";
            }
        }

        function permissionMajorCode(permission) {
            const code = String((permission && permission.permissionCode) || "");
            if (code.startsWith("general.config")) {
                return "general.config";
            }
            if (code.startsWith("general.log")) {
                return "general.log";
            }
            if (code.startsWith("general.unit")) {
                return "general.unit";
            }
            if (code.startsWith("general.person")) {
                return "general.person";
            }
            if (code.startsWith("general.role")) {
                return "general.role";
            }
            return "other";
        }

        function permissionMajorTitle(code) {
            return {
                "general.config": "常规",
                "general.unit": "部门",
                "general.person": "人员",
                "general.role": "岗位",
                "general.log": "日志",
                other: "其他"
            }[code] || code;
        }

        function permissionNameForDisplay(permission) {
            const code = String((permission && permission.permissionCode) || "");
            const names = {
                "general.config": "常规",
                "general.config.login": "系统登录",
                "general.config.login.view": "系统登录-查看",
                "general.config.login.update": "系统登录-修改",
                "general.config.login-page": "登录页面",
                "general.config.login-page.view": "登录页面-查看",
                "general.config.login-page.update": "登录页面-修改",
                "general.unit": "部门",
                "general.unit.view": "部门-查看",
                "general.unit.create": "部门-新增",
                "general.unit.update": "部门-修改",
                "general.unit.delete": "部门-删除",
                "general.person": "人员",
                "general.person.view": "人员-查看",
                "general.person.create": "人员-新增",
                "general.person.update": "人员-修改",
                "general.person.delete": "人员-删除",
                "general.person.role.assign": "人员-分配岗位",
                "general.role": "岗位",
                "general.role.view": "岗位-查看",
                "general.role.create": "岗位-新增",
                "general.role.update": "岗位-修改",
                "general.role.delete": "岗位-删除",
                "general.role.person.assign": "岗位-分配人员",
                "general.role.permission.refresh": "岗位-刷新权限",
                "general.log": "日志",
                "general.log.login": "登录日志",
                "general.log.login.view": "登录日志-查看",
                "general.log.operation": "操作日志",
                "general.log.operation.view": "操作日志-查看"
            };
            return names[code] || String((permission && permission.permissionName) || code || "").replace(/^常规-/, "");
        }

        function permissionParentCodes(permissionCode) {
            const code = String(permissionCode || "");
            if (code.startsWith("general.config.login-page.")) {
                return ["general.config", "general.config.login-page"];
            }
            if (code.startsWith("general.config.login.")) {
                return ["general.config", "general.config.login"];
            }
            if (code.startsWith("general.log.login.")) {
                return ["general.log", "general.log.login"];
            }
            if (code.startsWith("general.log.operation.")) {
                return ["general.log", "general.log.operation"];
            }
            if (code.startsWith("general.unit.")) {
                return ["general.unit"];
            }
            if (code.startsWith("general.person.")) {
                return ["general.person"];
            }
            if (code.startsWith("general.role.")) {
                return ["general.role"];
            }
            return [];
        }

        function findPermissionInputByCode(permissionCode) {
            const code = String(permissionCode || "");
            return Array.from(rolePermissionList.querySelectorAll("input[type='checkbox']"))
                .find((item) => item.dataset.sectionCode === code || item.dataset.permissionCode === code) || null;
        }

        function findChildPermissionInputs(sectionCode) {
            const code = String(sectionCode || "");
            return Array.from(rolePermissionList.querySelectorAll("input[type='checkbox']"))
                .filter((item) => item.dataset.parentSection === code
                    || String(item.dataset.permissionCode || "").startsWith(`${code}.`));
        }

        function renderPermissions(selectedIds) {
            const selected = new Set((selectedIds || []).map(String));
            const order = ["general.config", "general.unit", "general.person", "general.role", "general.log"];
            const grouped = new Map(order.map((code) => [code, []]));
            permissions.forEach((permission) => {
                const majorCode = permissionMajorCode(permission);
                if (!grouped.has(majorCode)) {
                    grouped.set(majorCode, []);
                }
                grouped.get(majorCode).push(permission);
            });
            rolePermissionList.innerHTML = order
                .filter((majorCode) => (grouped.get(majorCode) || []).length)
                .map((majorCode) => {
                    const items = grouped.get(majorCode) || [];
                    const sectionPermission = items.find((permission) => String(permission.permissionCode || "") === majorCode);
                    const actions = items.filter((permission) => String(permission.permissionCode || "") !== majorCode);
                    return `
                        <section class="role-permission-group role-permission-group--major">
                            <div class="role-permission-group__header">
                                ${sectionPermission ? `
                                    <label class="role-permission-section">
                                        <span>
                                            <strong>${escapeHtml(permissionMajorTitle(majorCode))}</strong>
                                            <small>控制设置左侧栏目是否显示</small>
                                        </span>
                                        <input type="checkbox" value="${sectionPermission.id}" data-section-code="${escapeHtml(sectionPermission.permissionCode)}" ${selected.has(String(sectionPermission.id)) ? "checked" : ""}>
                                        <span class="role-permission-section__button">${selected.has(String(sectionPermission.id)) ? "已显示" : "隐藏"}</span>
                                    </label>
                                ` : `
                                    <div>
                                        <strong>${escapeHtml(permissionMajorTitle(majorCode))}</strong>
                                        <span>栏目权限未配置</span>
                                    </div>
                                `}
                            </div>
                            <div class="role-permission-group__items">
                                ${actions.map((permission) => `
                                    <label class="role-check-item">
                                        <input type="checkbox" value="${permission.id}" data-permission-code="${escapeHtml(permission.permissionCode || "")}" data-parent-section="${escapeHtml(majorCode)}" ${selected.has(String(permission.id)) ? "checked" : ""}>
                                        <span class="role-check-item__mark"></span>
                                        <span class="role-check-item__text">
                                            <strong>${escapeHtml(permissionNameForDisplay(permission))}</strong>
                                            <small>${escapeHtml(permission.permissionCode || "")}</small>
                                        </span>
                                    </label>
                                `).join("")}
                            </div>
                        </section>
                    `;
                }).join("") || '<div class="settings-empty">暂无权限项。</div>';
        }

        rolePermissionList.addEventListener("change", (event) => {
            const input = event.target.closest("input[type='checkbox']");
            if (!input) {
                return;
            }
            if (input.dataset.parentSection && input.checked) {
                permissionParentCodes(input.dataset.permissionCode || input.dataset.parentSection).forEach((parentCode) => {
                    const sectionInput = findPermissionInputByCode(parentCode);
                    if (sectionInput) {
                        sectionInput.checked = true;
                        if (sectionInput.dataset.sectionCode) {
                            syncSectionPermissionButton(sectionInput);
                        }
                    }
                });
            }
            if (input.dataset.sectionCode && !input.checked) {
                findChildPermissionInputs(input.dataset.sectionCode).forEach((childInput) => {
                    childInput.checked = false;
                });
            }
            if (input.dataset.sectionCode) {
                syncSectionPermissionButton(input);
            }
        });

        rolePermissionList.addEventListener("click", (event) => {
            const button = event.target.closest(".role-permission-section__button");
            if (!button) {
                return;
            }
            const section = button.closest(".role-permission-section");
            const input = section ? section.querySelector("input[type='checkbox']") : null;
            if (!input || input.disabled) {
                return;
            }
            event.preventDefault();
            event.stopPropagation();
            input.checked = !input.checked;
            input.dispatchEvent(new Event("change", { bubbles: true }));
        });

        function collectPersonIds() {
            return Array.from(selectedPersonIds).map((item) => Number(item));
        }

        function buildPayload() {
            return {
                roleCode: roleCodeInput.value.trim(),
                roleName: roleNameInput.value.trim(),
                roleShortName: roleShortNameInput.value.trim(),
                roleDesc: roleDescInput.value.trim(),
                unitId: Number(roleUnitIdInput.value),
                roleType: roleTypeInput.value,
                roleGroup: roleGroupInput.value.trim(),
                roleProperty: rolePropertyInput.value,
                enabled: roleEnabledInput.checked,
                sortOrder: Number(roleSortOrderInput.value || 0),
                permissionIds: collectPermissionIds()
            };
        }

        function getDefaultRoleUnitId() {
            if (selectedUnitId) {
                return selectedUnitId;
            }
            const firstUnit = flattenUnits(roleTreeData, [])[0];
            return firstUnit ? String(firstUnit.id) : "";
        }

        async function loadData() {
            tabButtons
                .filter((button) => button.dataset.roleTab === "people")
                .forEach((button) => {
                    button.hidden = !canAssignRolePeople();
                });
            if (!canAssignRolePeople() && panels.people && !panels.people.hidden) {
                setTab("basic");
            }
            const [treePayload, permissionPayload, peoplePayload, roleTypePayload] = await Promise.all([
                requestJson("/api/org/roles/tree"),
                requestJson("/api/org/permissions"),
                canAssignRolePeople() ? requestJson("/api/org/persons") : Promise.resolve([]),
                requestJson("/api/org/role-types")
            ]);
            roleTreeData = treePayload || [];
            permissions = permissionPayload || [];
            people = peoplePayload || [];
            roleTypes = roleTypePayload || [];
            renderRoleTypes(selectedRole ? selectedRole.roleType : roleTypeInput.value || "normal");
            initializeExpandedSets();
            populateUnitSelect();
            renderTree(roleTreeData);
            renderPermissions(selectedRole && selectedRole.permissions ? selectedRole.permissions.map((item) => item.id) : []);
            if (!selectedRole) {
                const defaultUnitId = getDefaultRoleUnitId();
                if (defaultUnitId && canCreateRole()) {
                    beginCreateRole(defaultUnitId);
                } else {
                    fillRole(null);
                }
            } else if (editorMode === "edit") {
                renderPeople(null);
            }
        }

        async function selectRole(roleId) {
            const role = await requestJson(`/api/org/roles/${roleId}`);
            fillRole(role);
        }

        async function saveRole() {
            showError("");
            const editingRole = Boolean(roleEditIdInput.value);
            if ((editingRole && !canUpdateRole()) || (!editingRole && !canCreateRole())) {
                showError("无权保存岗位信息");
                return;
            }
            setTab("basic");
            if (!requireValue(roleCodeInput, "岗位编码")
                || !requireValue(roleNameInput, "岗位名称")
                || !requireValue(roleUnitIdInput, "所属部门")
                || !requireValue(roleTypeInput, "岗位类型")) {
                return;
            }
            const payload = buildPayload();
            let saved;
            if (roleEditIdInput.value) {
                saved = await requestJson(`/api/org/roles/${roleEditIdInput.value}`, {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });
            } else {
                saved = await requestJson("/api/org/roles", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });
            }
            if (saved && saved.id) {
                if (canAssignRolePeople()) {
                    await requestJson(`/api/org/roles/${saved.id}/people`, {
                        method: "PUT",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ personIds: collectPersonIds() })
                    });
                }
                await loadData();
                await selectRole(saved.id);
                setTab("basic");
            }
            showError("");
            notify("保存成功");
        }

        async function deleteRole(roleId) {
            const targetRoleId = roleId || roleEditIdInput.value;
            if (!targetRoleId) {
                return;
            }
            await requestJson(`/api/org/roles/${targetRoleId}`, { method: "DELETE" });
            hideRoleContextMenu();
            selectedRole = null;
            await loadData();
            notify("删除成功");
        }

        function setTab(tab) {
            if (tab === "people" && !canAssignRolePeople()) {
                tab = "basic";
            }
            tabButtons.forEach((button) => button.classList.toggle("active", button.dataset.roleTab === tab));
            Object.entries(panels).forEach(([key, panel]) => {
                if (panel) {
                    panel.hidden = key !== tab;
                }
            });
        }

        roleTree.addEventListener("click", (event) => {
            const node = event.target.closest("[data-type][data-unit-id], [data-type][data-role-id]");
            if (!node) {
                return;
            }
            selectedUnitId = node.dataset.unitId || selectedUnitId;
            if (node.dataset.type === "role" && node.dataset.roleId) {
                selectRole(node.dataset.roleId).catch((error) => showError(error.message));
                return;
            }
            if (node.dataset.unitId) {
                toggleExpanded(expandedRoleUnitIds, node.dataset.unitId);
            }
            selectedRole = null;
            editorMode = "none";
            renderTree(roleTreeData);
            setEditorVisible(false);
        });

        roleTree.addEventListener("contextmenu", (event) => {
            const roleNode = event.target.closest("[data-type='role'][data-role-id]");
            if (roleNode) {
                event.preventDefault();
                selectRole(roleNode.dataset.roleId).catch((error) => showError(error.message));
                showRoleContextMenu(roleNode.dataset.roleId, event.clientX, event.clientY);
                return;
            }
            const unitNode = event.target.closest("[data-type='unit'][data-unit-id]");
            if (!unitNode) {
                hideRoleContextMenu();
                return;
            }
            if (!canCreateRole()) {
                hideRoleContextMenu();
                return;
            }
            event.preventDefault();
            selectedUnitId = unitNode.dataset.unitId;
            showUnitContextMenu(unitNode.dataset.unitId, event.clientX, event.clientY);
        });

        rolePeopleList.addEventListener("click", (event) => {
            const toggle = event.target.closest("[data-action='toggle-role-people-unit']");
            if (!toggle) {
                return;
            }
            event.preventDefault();
            toggleExpanded(expandedRolePeopleUnitIds, toggle.dataset.unitId);
            renderPeople(null);
        });

        rolePeopleList.addEventListener("change", (event) => {
            const checkbox = event.target.closest(".role-person-check input[type='checkbox']");
            if (!checkbox) {
                return;
            }
            if (checkbox.checked) {
                selectedPersonIds.add(String(checkbox.value));
            } else {
                selectedPersonIds.delete(String(checkbox.value));
            }
        });

        if (roleTreeContextMenu) {
            roleTreeContextMenu.addEventListener("click", (event) => {
                const createButton = event.target.closest("[data-action='create-role']");
                if (createButton && contextMenuUnitId) {
                    if (!canCreateRole()) {
                        hideRoleContextMenu();
                        return;
                    }
                    const unitId = contextMenuUnitId;
                    hideRoleContextMenu();
                    beginCreateRole(unitId);
                    return;
                }
                const deleteButton = event.target.closest("[data-action='delete-role']");
                if (!deleteButton || !contextMenuRoleId) {
                    return;
                }
                if (!canDeleteRole()) {
                    hideRoleContextMenu();
                    return;
                }
                const roleId = contextMenuRoleId;
                hideRoleContextMenu();
                deleteRole(roleId).catch((error) => showError(error.message));
            });
        }

        document.addEventListener("click", (event) => {
            if (roleTreeContextMenu && !roleTreeContextMenu.contains(event.target)) {
                hideRoleContextMenu();
            }
        });

        roleSearchInput.addEventListener("input", () => {
            keyword = roleSearchInput.value.trim().toLowerCase();
            renderTree(roleTreeData);
        });
        saveRoleButton.addEventListener("click", () => saveRole().catch((error) => showError(error.message)));
        refreshRolePermissionsButton.addEventListener("click", async () => {
            if (!roleEditIdInput.value) {
                return;
            }
            if (!canRefreshRolePermissions()) {
                showError("无权刷新岗位权限");
                return;
            }
            await requestJson(`/api/org/roles/${roleEditIdInput.value}/refresh-permissions`, { method: "POST" });
            notify("已刷新本岗位人员权限");
        });
        tabButtons.forEach((button) => button.addEventListener("click", () => setTab(button.dataset.roleTab)));

        await loadData();
        return {
            reload: loadData
        };
    }

    window.initOrgRoleSettings = initOrgRoleSettings;
})();
