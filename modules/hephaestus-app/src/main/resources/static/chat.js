(function () {
    const DEFAULT_SESSION_TITLE = "新聊天";
    const EMPTY_STATE_HTML = [
        '<div class="messages-inner">',
        '  <div class="empty">',
        '    <strong>今天想聊点什么？</strong>',
        '    <span>Hephaestus 可以理解附件、持续流式回复，也可以根据描述生成图片。</span>',
        '  </div>',
        '</div>'
    ].join("");

    const basePath = window.location.pathname.replace(/\/[^/]*$/, "");
    const streamUrl = `${basePath}/api/chat/multimodal/stream`;

    const messages = document.getElementById("messages");
    const form = document.getElementById("chatForm");
    const input = document.getElementById("messageInput");
    const sendButton = document.getElementById("sendButton");
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

    let selectedFile = null;
    let previewUrl = null;
    let pendingUserMessage = null;
    let activeSessionId = "";
    let busy = false;
    let streamingSessionId = null;
    let streamingState = null;
    const sessions = [];

    function createSessionId() {
        return `hephaestus_${Date.now()}_${Math.random().toString(16).slice(2, 8)}`;
    }

    function createSession(title) {
        return {
            id: createSessionId(),
            title: title || DEFAULT_SESSION_TITLE,
            createdAt: Date.now(),
            messagesHtml: EMPTY_STATE_HTML
        };
    }

    function getActiveSession() {
        return sessions.find((item) => item.id === activeSessionId) || null;
    }

    function saveActiveSessionSnapshot() {
        const session = getActiveSession();
        if (!session) {
            return;
        }
        session.messagesHtml = messages.innerHTML || EMPTY_STATE_HTML;
    }

    function showSession(session) {
        messages.innerHTML = session && session.messagesHtml ? session.messagesHtml : EMPTY_STATE_HTML;
        pendingUserMessage = null;
        scrollMessagesToBottom();
    }

    function renderEmptyState() {
        messages.innerHTML = EMPTY_STATE_HTML;
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
        }
    }

    function formatSessionTime(timestamp) {
        const date = new Date(timestamp);
        const now = new Date();
        if (date.toDateString() === now.toDateString()) {
            return date.toLocaleTimeString("zh-CN", {
                hour: "2-digit",
                minute: "2-digit"
            });
        }
        return date.toLocaleDateString("zh-CN", {
            month: "numeric",
            day: "numeric"
        });
    }

    function renderSessionList() {
        sessionList.innerHTML = "";
        sessions.forEach((session) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = `session-item${session.id === activeSessionId ? " active" : ""}`;
            button.dataset.sessionId = session.id;
            button.disabled = busy;

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

    function activateSession(sessionId) {
        if (!sessionId || sessionId === activeSessionId) {
            return;
        }
        saveActiveSessionSnapshot();
        activeSessionId = sessionId;
        showSession(getActiveSession());
        clearAttachment();
        input.value = "";
        resizeInput();
        renderSessionList();
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
        input.focus();
    }

    function updateSessionTitleFromMessage(message) {
        const session = getActiveSession();
        if (!session || session.title !== DEFAULT_SESSION_TITLE) {
            return;
        }
        const source = (message || "").trim() || "附件对话";
        session.title = source.length > 18 ? `${source.slice(0, 18)}…` : source;
        renderSessionList();
    }

    function appendMessage(role, text, mediaItems) {
        clearEmptyState();
        const inner = ensureMessagesInner();
        const row = document.createElement("div");
        row.className = `message-row ${role}`;

        const bubble = document.createElement("div");
        bubble.className = "bubble";

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
        scrollMessagesToBottom();
        saveActiveSessionSnapshot();
        return { row, bubble, content, mediaContainer };
    }

    function createStreamingAssistantMessage() {
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
        scrollMessagesToBottom();
        saveActiveSessionSnapshot();
        return {
            bubble,
            status,
            content,
            mediaContainer: null,
            text: ""
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
                img.alt = item.fileName || "图片";
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
                link.href = item.downloadUrl || item.url;
                link.target = "_blank";
                link.rel = "noreferrer";
                link.textContent = "下载";

                meta.append(name, size);
                fileCard.append(meta, link);
                card.appendChild(fileCard);
            }

            list.appendChild(card);
        });
        return list;
    }

    function ensureMediaContainer(messageView) {
        if (!messageView.mediaContainer) {
            messageView.mediaContainer = document.createElement("div");
            messageView.bubble.appendChild(messageView.mediaContainer);
        }
        return messageView.mediaContainer;
    }

    function setBusy(isBusy) {
        busy = isBusy;
        sendButton.disabled = isBusy;
        statusText.textContent = isBusy ? "Hephaestus 正在思考…" : "Enter 发送，Shift + Enter 换行";
        statusText.classList.toggle("busy", isBusy);
        statusProgress.hidden = !isBusy;
        renderSessionList();
    }

    function buildAssistantRowHtml(text, status, attachments) {
        const mediaHtml = attachments && attachments.length > 0
            ? `<div>${renderMediaListHtml(attachments)}</div>`
            : "";
        const statusHtml = status ? `<div class="status-note">${escapeHtml(status)}</div>` : "";
        return `<div class="message-row assistant"><div class="bubble"><div class="message-name">Hephaestus</div>${statusHtml}<div class="bubble-content">${renderRichText(text || "")}</div>${mediaHtml}</div></div>`;
    }

    function updateStreamingSessionHtml() {
        if (!streamingSessionId || !streamingState) {
            return;
        }
        const session = sessions.find((item) => item.id === streamingSessionId);
        if (!session) {
            return;
        }
        const host = document.createElement("div");
        host.innerHTML = streamingState.baseHtml || EMPTY_STATE_HTML;
        let inner = host.querySelector(".messages-inner");
        if (!inner) {
            host.innerHTML = EMPTY_STATE_HTML;
            inner = host.querySelector(".messages-inner");
        }
        const rowHost = document.createElement("div");
        rowHost.innerHTML = buildAssistantRowHtml(streamingState.text, streamingState.status, streamingState.attachments);
        inner.appendChild(rowHost.firstElementChild);
        session.messagesHtml = host.innerHTML;
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

    function scrollMessagesToBottom() {
        messages.scrollTop = messages.scrollHeight;
    }

    function resizeInput() {
        input.style.height = "auto";
        input.style.height = `${Math.min(input.scrollHeight, 180)}px`;
    }

    async function sendMessage(message) {
        const content = message.trim();
        if (!content && !selectedFile) {
            input.focus();
            return;
        }

        const fileForRequest = selectedFile;
        if (fileForRequest && fileForRequest.size > 2 * 1024 * 1024) {
            appendMessage("error", `附件“${fileForRequest.name}”大小 ${formatFileSize(fileForRequest.size)}，超过 2MB 限制，请压缩后重试。`);
            clearAttachment();
            input.focus();
            return;
        }

        updateSessionTitleFromMessage(content);
        pendingUserMessage = appendMessage("user", content || "");
        input.value = "";
        resizeInput();
        const requestSessionId = activeSessionId;
        clearAttachment();
        setBusy(true);
        statusText.textContent = fileForRequest ? "正在上传附件…" : "正在发送消息…";
        streamingSessionId = requestSessionId;
        streamingState = {
            baseHtml: messages.innerHTML,
            text: "",
            status: "",
            attachments: []
        };

        try {
            const formData = new FormData();
            formData.append("message", content);
            if (fileForRequest) {
                formData.append("file", fileForRequest);
            }

            const response = await fetch(streamUrl, {
                method: "POST",
                headers: {
                    "X-Session-Id": activeSessionId
                },
                body: formData
            });

            if (!response.ok) {
                throw new Error("发送失败，请稍后重试。");
            }

            statusText.textContent = "正在等待回复…";
            await consumeEventStream(response, requestSessionId);
        } catch (error) {
            pendingUserMessage = null;
            appendMessage("error", error.message || "发送失败，请稍后重试。");
        } finally {
            updateStreamingSessionHtml();
            streamingSessionId = null;
            streamingState = null;
            setBusy(false);
            saveActiveSessionSnapshot();
            input.focus();
        }
    }

    async function consumeEventStream(response, requestSessionId) {
        const reader = response.body.getReader();
        const decoder = new TextDecoder("utf-8");
        const assistant = requestSessionId === activeSessionId ? createStreamingAssistantMessage() : null;
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

        if (streamingSessionId === requestSessionId && streamingState) {
            if (eventName === "status") {
                streamingState.status = payload.message || "";
            } else if (eventName === "delta") {
                streamingState.text += payload.content || "";
            } else if (eventName === "image" && payload.generatedImage) {
                streamingState.attachments.push(payload.generatedImage);
            } else if (eventName === "attachments" && Array.isArray(payload.attachments) && pendingUserMessage) {
                pendingUserMessage = null;
                streamingState.baseHtml = messages.innerHTML;
            } else if (eventName === "done" || eventName === "error") {
                streamingState.status = "";
            }
        }

        if (requestSessionId !== activeSessionId || !assistant) {
            updateStreamingSessionHtml();
            return;
        }

        if (eventName === "status") {
            assistant.status.textContent = payload.message || "";
        } else if (eventName === "delta") {
            assistant.text += payload.content || "";
            assistant.content.innerHTML = renderRichText(assistant.text);
        } else if (eventName === "attachments" && Array.isArray(payload.attachments)) {
            if (pendingUserMessage) {
                ensureMediaContainer(pendingUserMessage).appendChild(renderMediaList(payload.attachments));
                pendingUserMessage = null;
            } else {
                ensureMediaContainer(assistant).appendChild(renderMediaList(payload.attachments));
            }
        } else if (eventName === "image" && payload.generatedImage) {
            ensureMediaContainer(assistant).appendChild(renderMediaList([payload.generatedImage]));
        } else if (eventName === "done") {
            assistant.status.textContent = "";
            pendingUserMessage = null;
        } else if (eventName === "error") {
            assistant.status.textContent = "";
            assistant.text += (assistant.text ? "\n" : "") + (payload.message || "处理请求时发生错误。");
            assistant.content.innerHTML = renderRichText(assistant.text);
            pendingUserMessage = null;
        }

        scrollMessagesToBottom();
        saveActiveSessionSnapshot();
    }

    function escapeHtml(text) {
        return text
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function renderRichText(text) {
        if (!text) {
            return "";
        }
        const safe = escapeHtml(text);
        const withCodeBlocks = safe.replace(/```([a-zA-Z0-9_-]+)?\n([\s\S]*?)```/g, (_, lang, code) => {
            const label = lang ? `<div>${lang}</div>` : "";
            return `<pre><code>${label}${code.trim()}</code></pre>`;
        });
        const withInlineCode = withCodeBlocks.replace(/`([^`]+)`/g, "<code>$1</code>");
        return withInlineCode
            .split(/\n{2,}/)
            .map((part) => {
                if (part.startsWith("<pre>")) {
                    return part;
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
            .join("");
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
            attachmentThumb.appendChild(img);
        }

        attachmentChip.classList.add("visible");
    }

    function clearAttachment() {
        selectedFile = null;
        attachmentInput.value = "";
        attachmentChip.classList.remove("visible");
        attachmentName.textContent = "";
        attachmentSize.textContent = "";
        attachmentThumb.textContent = "+";

        if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
            previewUrl = null;
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
        if (sendButton.disabled) {
            return;
        }
        const items = event.clipboardData && event.clipboardData.items;
        if (!items) {
            return;
        }
        for (const item of items) {
            if (item.kind === "file") {
                event.preventDefault();
                selectedFile = item.getAsFile();
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
    });

    removeFileButton.addEventListener("click", () => {
        clearAttachment();
        input.focus();
    });

    sessions.push(createSession(DEFAULT_SESSION_TITLE));
    activeSessionId = sessions[0].id;
    renderEmptyState();
    renderSessionList();
    resizeInput();
})();
