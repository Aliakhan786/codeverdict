# Setup guide (Windows laptop, step by step)

This takes about 30–45 minutes the first time. You need about 6 GB of free disk space.

---

## 1. Install the tools (once)

| Tool | Why | Download |
|---|---|---|
| **JDK 21** (Temurin) | runs and compiles Java | https://adoptium.net → Temurin 21 (LTS), Windows x64 `.msi`. In the installer, enable **"Set JAVA_HOME"** and **"Add to PATH"**. |
| **Maven 3.9+** | builds the backend | https://maven.apache.org/download.cgi → *Binary zip*. Unzip to `C:\tools\maven`, then add `C:\tools\maven\bin` to your PATH (Start → "Edit the system environment variables" → Environment Variables → Path → New). |
| **Node.js 22 LTS** | builds the frontend | https://nodejs.org |
| **Docker Desktop** | PostgreSQL + one-command run + sandbox | https://www.docker.com/products/docker-desktop (enable WSL 2 when asked, restart) |
| **Git** | version control | https://git-scm.com |
| IntelliJ IDEA Community | backend editor | https://www.jetbrains.com/idea/download |
| VS Code | frontend editor | https://code.visualstudio.com |

Open a **new** PowerShell window and check:

```powershell
java -version      # must say 21
mvn -v             # must say Java version: 21
node -v            # v22.x
docker --version
git --version
```

## 2. Get a free Gemini API key

1. Go to https://aistudio.google.com/apikey and sign in with Google.
2. Click **Create API key** and copy it.

## 3. Put the project on your laptop

Unzip `codeverdict.zip` somewhere without spaces in the path if possible, e.g. `C:\projects\codeverdict`.

```powershell
cd C:\projects\codeverdict
copy .env.example .env
notepad .env
```

In `.env` set `GEMINI_API_KEY=` to your key and change `JWT_SECRET` to any long random text (at least 32 characters). Save.

> `.env` is in `.gitignore` — your key will never be pushed to GitHub.

---

## Option A — run everything with Docker (easiest)

```powershell
cd C:\projects\codeverdict
docker compose up --build
```

The first build downloads a lot (5–10 min). When you see `Started CodeVerdictApplication`, open:

- App: **http://localhost:3000** → log in with `demo` / `demo1234`, or create your own account
- API docs: http://localhost:3000/swagger-ui.html

Stop with `Ctrl+C`. Your data stays in a Docker volume. `docker compose down -v` deletes it.

---

## Option B — development mode (hot reload, what you use daily while coding)

### 4. Start only the database

```powershell
docker compose up -d postgres
```

### 5. Build and test the backend once

```powershell
cd backend
mvn verify
```

This downloads dependencies, copies the JUnit launcher to `target/runner-lib/`, and runs all tests:

- unit tests (scoring, double-check rule, complexity, XP, safety checker …)
- `ProofEngineIntegrationTest` — the real Proof Engine with real javac, JVM processes and JUnit
- `ApiIntegrationTest` — the full app on a real PostgreSQL started by Testcontainers (needs Docker running; skipped otherwise)

> **If anything fails here, stop and copy the error.** The Proof Engine core was compiled and tested end to end while
> this project was being prepared, but the Spring Boot layer could only be type-checked there (no Maven access), so this
> first `mvn verify` on your machine is its first real build. Fixing a startup error is usually a one-line change.

### 6. Run the backend

In IntelliJ: **File → Open → `backend/pom.xml` → Open as Project**. Wait for Maven import, then run
`CodeVerdictApplication` (green ▶ next to `main`). Or from the terminal:

```powershell
cd backend
mvn spring-boot:run
```

It reads your `.env` automatically. Check http://localhost:8080/api/v1/health → `{"status":"ok", ... "ai":"gemini-2.5-flash"}`.

### 7. Run the frontend

```powershell
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**. Vite forwards `/api` to the backend on port 8080.

### 8. Try the full loop

1. Log in → **New Case** → *Maximum Subarray*.
2. Paste this (it has a real bug: it fails when all numbers are negative):

   ```java
   class Solution {
       public int maxSubArray(int[] nums) {
           int best = 0;
           int current = 0;
           for (int x : nums) {
               current = Math.max(x, current + x);
               best = Math.max(best, current);
           }
           return best;
       }
   }
   ```
3. Click **Run** (sample passes), then **Submit for Verdict** and watch the live stages.
4. On the Verdict page you should see a **GUILTY** charge with the failing test, then **Apply fix** → **Rematch** → higher score.

The first review of a problem is slower (it builds and verifies the reference solution); later attempts reuse it.

---

## 9. Push to GitHub

Create an empty repository named `codeverdict` on GitHub (no README), then:

```powershell
cd C:\projects\codeverdict
git init
git add .
git commit -m "CodeVerdict: AI code review that proves every bug"
git branch -M main
git remote add origin https://github.com/Aliakhan786/codeverdict.git
git push -u origin main
```

GitHub Actions (`.github/workflows/ci.yml`) will run all backend and frontend tests on every push — add the CI badge to the README.

**Before showing it to recruiters:** use it for real for a couple of weeks, take 3–4 screenshots and a GIF of the verdict reveal
(ScreenToGif on Windows), save them in `docs/`, run the evaluation (below) and put *your* numbers in the README.

## 10. Run the evaluation (for the README and resume numbers)

```powershell
cd backend
$env:GEMINI_API_KEY="your-key"
mvn test -Dtest=EvaluationTest -Deval=true -Deval.runs=3
```

It reviews 30 snippets three times (≈ 30–60 min because of free-tier rate limits) and writes `docs/eval/results.md`.
Use exactly the numbers you get. If a number is weak, say so and explain why — that is a great interview answer.

## 11. Deploy (optional)

- **Simplest:** run locally for daily use; deploy the frontend to Vercel and show the full flow in the demo video.
- **Full:** a small Linux VM (Oracle Cloud Always Free / AWS free tier) with Docker:
  ```bash
  git clone https://github.com/Aliakhan786/codeverdict.git && cd codeverdict
  cp .env.example .env && nano .env      # key, strong JWT_SECRET, DB_PASSWORD
  docker pull eclipse-temurin:21-jre      # image used for every sandboxed run
  docker compose -f docker-compose.yml -f docker-compose.sandbox.yml up -d --build
  ```
  Put Caddy in front for HTTPS (`deploy/Caddyfile`). The sandbox override makes the demo account read-only and turns off sign-up.
  Back up the database daily: `docker compose exec postgres pg_dump -U codeverdict codeverdict > backup.sql`.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `JUnit console launcher jar not found` | Run `mvn generate-resources` once in `backend/` (IntelliJ does not run Maven plugins by itself). |
| `No Java compiler found` | You started the backend with a JRE. Use the JDK 21 (IntelliJ: File → Project Structure → SDK). |
| `Connection refused` to PostgreSQL | `docker compose up -d postgres`, and make sure nothing else uses port 5432. |
| Verdict says "AI review unavailable" | Check `GEMINI_API_KEY` in `.env` and restart the backend. Settings page shows the key status. |
| `AI rate limit reached` | Free tier ≈ 10 requests/min. Wait a minute. Identical code is cached. |
| Review takes > 60 s | Normal on the very first review of a problem. Set `GEMINI_THINKING_BUDGET=0` (default) for speed. |
| `mvn` not found | Re-open PowerShell after editing PATH. |
| Port 3000 / 5173 / 8080 busy | Stop the other app, or change the port in `docker-compose.yml` / `vite.config.ts` / `PORT` env. |
