# LENTERA 2.0 — LOCAL P2P CLASSROOM SYSTEM

## Architecture
Teacher phone hosts a local WebSocket / HTTP server using `org.java-websocket` over a local Wi-Fi router or Android Hotspot. No internet connectivity is required.

```
                  ┌──────────────────────────────┐
                  │   TEACHER PHONE (Host)       │
                  │   Local WebSocket Server     │
                  │   Room Code: [ 8 4 9 2 0 1 ] │
                  └──────────────┬───────────────┘
                                 │
           ┌─────────────────────┼─────────────────────┐
           ▼                     ▼                     ▼
    ┌─────────────┐       ┌─────────────┐       ┌─────────────┐
    │  Student 1  │       │  Student 2  │       │  Student 3  │
    └─────────────┘       └─────────────┘       └─────────────┘
```

## Protocol & Flow
1. **Teacher Creates Room**: Server starts on port `8080`, generates a unique 6-digit room code.
2. **Students Join**: Students connect via local IP & room code over WebSocket.
3. **Quiz Launch**: Teacher selects quiz topic. Server broadcasts question JSON to connected student clients.
4. **Live Responses**: Students answer on their devices. Server receives instant answer payloads.
5. **Classroom Diagnostics**: Teacher dashboard displays real-time submission percentage, correct vs wrong stats, and class-level weak concepts.
6. **Remedial Broadcast**: Teacher taps "Broadcast AI Explanation" to send instant remedial explanations to all student screens.
