# Deployment guide (100% free): GitHub + Neon PostgreSQL + Render

## 0. Run locally with PostgreSQL (optional)
    docker compose up --build        # http://localhost:8080
Without Docker, `./mvnw spring-boot:run` still works using in-memory H2 (dev profile).

## 1. Put the code on your GitHub
    git init && git add . && git commit -m "UPI mesh: PostgreSQL + Render"
    git branch -M main
    git remote add origin https://github.com/<you>/<repo>.git
    git push -u origin main
GitHub Actions (CI) runs automatically: H2 tests, PostgreSQL+Flyway tests, Docker build.

## 2. Free PostgreSQL on Neon (neon.tech)
1. Sign up, create a project + database.
2. Copy host, user, password, database name from the connection details.
3. Build the JDBC URL:  `jdbc:postgresql://<HOST>/<DBNAME>?sslmode=require`

## 3. Generate the server RSA key (once)
    java scripts/GenerateRsaKey.java
Copy the printed line. Keep it secret; never commit it.

## 4. Deploy on Render (render.com)
1. New > Web Service > connect your GitHub repo.
2. Runtime: **Docker**, Instance type: **Free**.
3. Environment variables:
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `DB_URL`  = the JDBC URL from step 2
   - `DB_USER` = Neon user
   - `DB_PASS` = Neon password
   - `UPI_RSA_PRIVATE_KEY` = the line from step 3
4. Health Check Path: `/actuator/health`
5. Deploy. Tables are created automatically by Flyway on first start.

## Notes
- Free Render services sleep after ~15 min idle; the first request then takes ~30-60 s.
  Neon's free compute also auto-suspends and wakes on the first query.
- Free-tier limits and terms change; check each provider's current pricing page.
- Demo endpoints (/api/demo/*, /api/mesh/*) are public, as in the original demo.
