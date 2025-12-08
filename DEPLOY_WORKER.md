# Cloudflare Worker Deployment Instructions

## Prerequisites

1. Install Wrangler CLI globally:
   ```bash
   npm install -g wrangler
   ```

2. Login to Cloudflare:
   ```bash
   wrangler login
   ```

## Deployment Steps

1. Navigate to the worker directory:
   ```bash
   cd worker
   ```

2. Update the API key in `worker.js`:
   - Open `worker/worker.js`
   - Replace `REPLACE_WITH_MY_OPENAI_KEY` with your actual OpenAI API key

3. Deploy the worker:
   ```bash
   wrangler deploy
   ```

4. After deployment, you'll get a URL like:
   ```
   https://fastmind-openai-proxy.YOUR_USER.workers.dev
   ```

5. Update `www/index.html`:
   - Find the line: `const PROXY_URL = "https://fastmind-openai-proxy.YOUR_USER.workers.dev";`
   - Replace `YOUR_USER` with your actual Cloudflare username/subdomain

## Verification

1. Test the worker directly:
   ```bash
   curl -X POST https://fastmind-openai-proxy.YOUR_USER.workers.dev \
     -H "Content-Type: application/json" \
     -d '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"Hello"}]}'
   ```

2. Test from your PWA:
   - Open your app
   - Try using the AI chat feature
   - Check browser console for any errors

## Important Notes

- The API key is visible in the worker code (by design)
- CORS is enabled for all origins
- The worker proxies all requests to OpenAI API
- Works across all platforms: browser, PWA, TWA, iOS, App Store






