(function () {
    const DEFAULT_SESSION_TITLE = "新建聊天";
    const DEFAULT_HINT_TEXT = "Enter 发送，Shift + Enter 换行，Ctrl + V 可粘贴图片或文件";
    const EMPTY_STATE_HTML = [
        '<div class="messages-inner">',
        '  <div class="empty">',
        '    <strong>今天想聊点什么？</strong>',
        '    <span>Hephaestus 可以理解附件、流式回复，也可以生成图片。</span>',
        '  </div>',
        '</div>'
    ].join("");

    const basePath = window.location.pathname.replace(/\/[^/]*$/, "");
    const streamUrl = `${basePath}/api/chat/multimodal/stream`;

    const messages = document.getElementById("messages");
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

    let selectedFile = null;
    let previewUrl = null;
    let activeSessionId = "";
    let pendingUserMessage = null;

    const sessions = [];
    const streamStates = new Map();

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
            return date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
        }
        return date.toLocaleDateString("zh-CN", { month: "numeric", day: "numeric" });
    }

    function isNearBottom() {
        const threshold = 48;
        return messages.scrollHeight - messages.clientHeight - messages.scrollTop <= threshold;
    }

    function scrollMessagesToBottom() {
        messages.scrollTop = messages.scrollHeight;
    }

    function stickToBottomAfterImageLoad(img) {
        const tryScroll = () => {
            const activeSession = getActiveSession();
            if (!activeSession || activeSession.autoScroll) {
                scrollMessagesToBottom();
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
            text: content.textContent || ""
        };
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
        assistantView.content.innerHTML = renderRichText(assistantView.text);

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
            text: ""
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

    function showLiveStatus(message) {
        if (!liveStatusText || !liveStatusTrack) {
            return;
        }
        liveStatusTrack.textContent = message || "";
        liveStatusText.hidden = !message;
        liveStatusText.classList.toggle("is-active", Boolean(message));
    }

    function clearLiveStatus() {
        if (!liveStatusText || !liveStatusTrack) {
            return;
        }
        liveStatusTrack.textContent = "";
        liveStatusText.classList.remove("is-active");
        liveStatusText.hidden = true;
    }

    function mapLiveStatusMessage(phase, message) {
        if (phase === "accepted") {
            return "已收到消息";
        }
        if (phase === "analyzing") {
            return "正在分析附件";
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
        input.value = "";
        resizeInput();
        clearAttachment();

        const initialMessagesHtml = messages.innerHTML;
        streamStates.set(requestSessionId, {
            baseHtml: initialMessagesHtml,
            text: "",
            status: "",
            attachments: [],
            assistantView: null,
            startedAt: Date.now()
        });

        const initialStatus = fileForRequest ? "正在上传附件…" : "正在发送消息…";
        await new Promise((resolve) => requestAnimationFrame(resolve));

        try {
            const formData = new FormData();
            formData.append("message", content);
            if (fileForRequest) {
                formData.append("file", fileForRequest);
            }

            const state = streamStates.get(requestSessionId);
            if (state) {
                state.status = initialStatus;
            }
            if (requestSessionId === activeSessionId) {
                syncComposerState();
            }

            const response = await fetch(streamUrl, {
                method: "POST",
                headers: {
                    "X-Session-Id": requestSessionId
                },
                body: formData
            });

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
        if (state) {
            if (eventName === "status") {
                state.status = payload.message || "";
            } else if (eventName === "delta") {
                state.text += payload.content || "";
            } else if (eventName === "image" && payload.generatedImage) {
                state.attachments.push(payload.generatedImage);
            } else if (eventName === "done" || eventName === "error") {
                state.status = "";
            }

            updateStreamStateSnapshot(requestSessionId);
            if (requestSessionId === activeSessionId) {
                syncComposerState();
            }
            if (currentAssistant) {
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
            showLiveStatus(mapLiveStatusMessage(payload.phase, payload.message || ""));
        } else if (eventName === "delta") {
            currentAssistant.text += payload.content || "";
            currentAssistant.content.innerHTML = renderRichText(currentAssistant.text);
        } else if (eventName === "attachments" && Array.isArray(payload.attachments)) {
            replaceUserAttachmentMedia(pendingUserMessage, payload.attachments);
            pendingUserMessage = null;
        } else if (eventName === "image" && payload.generatedImage) {
            ensureMediaContainer(currentAssistant).appendChild(renderMediaList([payload.generatedImage]));
        } else if (eventName === "done") {
            currentAssistant.status.textContent = "";
            clearLiveStatus();
            pendingUserMessage = null;
        } else if (eventName === "error") {
            currentAssistant.status.textContent = "";
            currentAssistant.text += `${currentAssistant.text ? "\n" : ""}${payload.message || "处理请求时发生错误。"}`;
            currentAssistant.content.innerHTML = renderRichText(currentAssistant.text);
            clearLiveStatus();
            pendingUserMessage = null;
        }

        const activeSession = getActiveSession();
        if (!activeSession || activeSession.autoScroll) {
            scrollMessagesToBottom();
        }
        saveActiveSessionSnapshot();
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

    messages.addEventListener("scroll", () => {
        const session = getActiveSession();
        if (!session) {
            return;
        }
        session.scrollTop = messages.scrollTop;
        session.autoScroll = isNearBottom();
    });

    sessions.push(createSession(DEFAULT_SESSION_TITLE));
    activeSessionId = sessions[0].id;
    renderEmptyState();
    renderSessionList();
    resizeInput();
    syncComposerState();
})();
