(() => {
    const history = document.getElementById("messages-thread-history");
    if (!history) {
        return;
    }

    const conversationId = history.dataset.conversationId;
    const currentUserId = history.dataset.userId;
    const form = document.querySelector(".messages-compose");
    const input = document.getElementById("message-body");
    const status = document.getElementById("messages-connection-status");
    let socket;
    let frameBuffer = "";
    let subscribed = false;
    let reconnectTimer;
    let heartbeatTimer;

    const setStatus = (message) => {
        if (status) {
            status.textContent = message;
        }
    };

    const sendFrame = (command, headers, body = "") => {
        if (!socket || socket.readyState !== WebSocket.OPEN) {
            return;
        }
        const serializedHeaders = Object.entries(headers)
            .map(([key, value]) => `${key}:${value}`)
            .join("\n");
        socket.send(`${command}\n${serializedHeaders}\n\n${body}\0`);
    };

    const renderMessage = (message) => {
        const bubble = document.createElement("article");
        const mine = String(message.senderId) === currentUserId;
        bubble.className = mine ? "messages-bubble messages-bubble-mine" : "messages-bubble";

        const meta = document.createElement("div");
        meta.className = "messages-bubble-meta";
        const sender = document.createElement("span");
        sender.textContent = mine ? "You" : message.senderName;
        const time = document.createElement("time");
        time.dateTime = message.sentAt;
        const parsedTime = new Date(message.sentAt);
        time.textContent = Number.isNaN(parsedTime.getTime())
            ? message.sentAt
            : parsedTime.toLocaleString();
        meta.append(sender, time);

        const body = document.createElement("p");
        body.textContent = message.body;
        bubble.append(meta, body);
        history.querySelector(".messages-thread-empty")?.remove();
        history.append(bubble);
        history.scrollTop = history.scrollHeight;

        if (!mine) {
            sendFrame("SEND", {
                destination: `/app/conversations/${conversationId}/read`,
                "content-type": "application/json"
            }, "{}");
            document.querySelector(".messages-conversation-current .messages-unread")?.remove();
        }
    };

    const processFrame = (frame) => {
        const separator = frame.indexOf("\n\n");
        if (separator < 0) {
            return;
        }
        const command = frame.slice(0, frame.indexOf("\n"));
        const headerLines = frame.slice(frame.indexOf("\n") + 1, separator).split("\n");
        const headers = Object.fromEntries(headerLines.map((line) => {
            const delimiter = line.indexOf(":");
            return delimiter < 0 ? [line, ""] : [line.slice(0, delimiter), line.slice(delimiter + 1)];
        }));
        const body = frame.slice(separator + 2);

        if (command === "CONNECTED") {
            subscribed = true;
            setStatus("Live connection active");
            window.clearInterval(heartbeatTimer);
            heartbeatTimer = window.setInterval(() => {
                if (socket && socket.readyState === WebSocket.OPEN) {
                    socket.send("\n");
                }
            }, 10000);
            sendFrame("SUBSCRIBE", {
                id: `conversation-${conversationId}`,
                destination: `/topic/conversations/${conversationId}`,
                ack: "auto"
            });
            sendFrame("SEND", {
                destination: `/app/conversations/${conversationId}/read`,
                "content-type": "application/json"
            }, "{}");
        } else if (command === "MESSAGE") {
            try {
                renderMessage(JSON.parse(body));
            } catch {
                setStatus("Received an unreadable message. Reload the conversation to refresh.");
            }
        } else if (command === "ERROR") {
            setStatus(headers.message || body || "Live messaging encountered an error.");
        }
    };

    const consumeFrames = () => {
        while (frameBuffer.length > 0) {
            frameBuffer = frameBuffer.replace(/^\n+/, "");
            if (frameBuffer.length === 0) {
                return;
            }
            const end = frameBuffer.indexOf("\0");
            if (end < 0) {
                return;
            }
            processFrame(frameBuffer.slice(0, end));
            frameBuffer = frameBuffer.slice(end + 1);
        }
    };

    const connect = () => {
        if (!("WebSocket" in window)) {
            setStatus("Live messaging is not supported by this browser. You can still send using the form.");
            return;
        }
        const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
        socket = new WebSocket(`${protocol}//${window.location.host}/ws`);
        frameBuffer = "";
        subscribed = false;
        socket.onopen = () => sendFrame("CONNECT", {
            "accept-version": "1.2",
            host: window.location.host,
            "heart-beat": "10000,10000"
        });
        socket.onmessage = (event) => {
            frameBuffer += event.data;
            consumeFrames();
        };
        socket.onerror = () => setStatus("Live messaging connection failed. Retrying...");
        socket.onclose = () => {
            subscribed = false;
            window.clearInterval(heartbeatTimer);
            setStatus("Disconnected from live messages. Retrying...");
            window.clearTimeout(reconnectTimer);
            reconnectTimer = window.setTimeout(connect, 3000);
        };
    };

    form?.addEventListener("submit", (event) => {
        if (!subscribed || !socket || socket.readyState !== WebSocket.OPEN) {
            return;
        }
        event.preventDefault();
        const body = input.value.trim();
        if (!body) {
            setStatus("Message cannot be empty.");
            input.focus();
            return;
        }
        sendFrame("SEND", {
            destination: `/app/conversations/${conversationId}/messages`,
            "content-type": "application/json"
        }, JSON.stringify({body}));
        input.value = "";
        input.focus();
    });

    connect();
})();
