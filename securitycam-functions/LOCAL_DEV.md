# Local development (storage & health)

The Azure Functions host **requires a valid `AzureWebJobsStorage`** connection. If it is empty or wrong, the runtime will keep logging `Unable to access AzureWebJobsStorage` roughly every 30 seconds.

## Recommended: Azurite

1. **Install dependencies**

   ```powershell
   cd securitycam-functions
   npm install
   ```

2. **`local.settings.json`**

   - Missing file: `npm run init:settings`
   - Empty **`AzureWebJobsStorage`**: `npm run settings:azurite` (writes the standard Azurite connection string)

   Or paste the value from `local.settings.json.example` manually.

3. **Run Azurite + Functions together**

   ```powershell
   npm run dev
   ```

   Table API (**port 10002**) must be ready before `func start`. Stop with **Ctrl+C** (both processes).

4. **Separate terminals**

   Terminal A:

   ```powershell
   npm run azurite
   ```

   Terminal B:

   ```powershell
   npm run start
   ```

## Using cloud storage locally

Point `AzureWebJobsStorage` at a real Azure Storage connection string; Azurite is then optional.

## Face template storage

If `FACE_STORAGE_CONNECTION_STRING` is empty, **`AzureWebJobsStorage`** is reused. With Azurite, templates land in the local Table emulator.

Japanese version: [LOCAL_DEV.ja.md](LOCAL_DEV.ja.md)
