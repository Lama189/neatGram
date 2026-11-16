# Media Loading & Download Subsystem

Status: VERIFIED

## 1. Purpose

Responsible for downloading media files (photos, animations, videos, documents) from Telegram MTProto datacenters, managing chunked RPC retrieval, streaming, priority queues, and concurrency limits.

## 2. Entry Points & Key Classes

- `org.telegram.messenger.FileLoader`: Singleton coordinating download and upload queues across all DC instances.
- `org.telegram.messenger.FileLoadOperation`: Core worker handling chunked downloads, CDN fallbacks, ranges, and state transitions for a single file.
- `org.telegram.tgnet.ConnectionsManager`: MTProto connection manager managing socket pools per DC.
- `TMessagesProj/jni/tgnet/ConnectionsManager.cpp` & `Datacenter.cpp`: Native MTProto connection multiplexer and socket pool (`ConnectionTypeDownload`).
- `org.telegram.neatgram.config.NeatGramConfig`: Controls client-side speed boost parameters.

## 3. Verified Execution Flow & Concurrency

1. **Queue Dispatch**:
   - `FileLoaderPriorityQueue` dispatches operations according to priority and active operation limits (`smallQueueMaxActiveOperations` = 5, `largeQueueMaxActiveOperations` = 2).
2. **Operation Start & Parameter Resolution** (`FileLoadOperation.start()` -> `updateParams()`):
   - `downloadChunkSizeBig`: 512 KB (`1024 * 512`) when Premium or `NeatGramConfig.isPremiumDownloadBoost()` is active; 128 KB for default non-premium accounts.
   - `maxDownloadRequests` / `maxDownloadRequestsBig`: 8 concurrent requests for large/boosted files; 4 for default.
   - `maxDownloadRequestsAnimation`: 8 concurrent requests when boosted; 4 for default.
   - `maxCdnParts`: Recalculated as `(int) (FileLoader.DEFAULT_MAX_FILE_SIZE / downloadChunkSizeBig)`.
3. **Byte Budget & Connection Multiplexing** (`FileLoadOperation.updateDownloadByteBudget()`):
   - `byteBudget`: 16 MB (`16 * 1024 * 1024`) for Premium / boosted accounts; 2 MB for default non-premium accounts.
   - `currentMaxDownloadRequests`: Calculated as `byteBudget / currentDownloadChunkSize` (32 requests with 512 KB chunks).
   - `downloadConnectionsCount`: 8 (`ConnectionsManager.DownloadConnectionsCount`) for Premium / boosted accounts; 2 for default non-premium accounts.
   - Multi-socket striping: Download requests are distributed across DC download sockets via `connectionType = ConnectionTypeDownload | ((requestsCount & (downloadConnectionsCount - 1)) << 16)`.
4. **Native Scheduling (`DownloadRequestBudget.h`, `ConnectionsManager.cpp`)**:
   - `DownloadRequestBudget` allocates up to 16 MB in-flight requests per DC when `currentUserPremium` is true; 2 MB otherwise.
   - `MAX_DOWNLOAD_REQUESTS_PREMIUM` permits 64 active download requests per DC (vs 32 default).

## 4. MTProto Constraints

- All chunk sizes (`downloadChunkSizeBig` = 512 KB, `downloadChunkSizeAnimation` = 128 KB, `downloadChunkSize` = 32 KB) are strict multiples of 4096 bytes and exact divisors of 1 MB (1048576 bytes) as mandated by MTProto `upload.getFile`.
- Chunk offsets (`downloadOffset`) strictly maintain `offset % limit == 0`.

## 5. Integration Hooks

- Hooked in `FileLoadOperation.updateParams()` and `updateDownloadByteBudget()`.
- Hooked in `ConnectionsManager.java` during `init(...)` and via JNI dynamic delegate `native_setUserPremium(...)`.
- Isolated configuration managed by `org.telegram.neatgram.config.NeatGramConfig`.
