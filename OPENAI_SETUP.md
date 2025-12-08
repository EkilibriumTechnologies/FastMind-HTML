# 🤖 Configuración de ChatGPT (OpenAI) como Principal

## 📋 Requisitos

1. **API Key de OpenAI**
   - Ve a: https://platform.openai.com/api-keys
   - Crea una nueva API key o usa una existente
   - La key comienza con `sk-...`

2. **Configurar en config.js**
   ```javascript
   window.OPENAI_API_KEY = 'sk-tu-api-key-aqui';
   window.GEMINI_API_KEY = 'tu-gemini-key-aqui'; // Como backup
   ```

## 💰 Costos (aproximados)

- **GPT-4o-mini**: ~$0.15 / 1M tokens input, ~$0.60 / 1M tokens output
- **GPT-4o**: ~$2.50 / 1M tokens input, ~$10.00 / 1M tokens output  
- **GPT-3.5-turbo**: ~$0.50 / 1M tokens input, ~$1.50 / 1M tokens output

**Recomendación:** Usa `gpt-4o-mini` para mejor relación calidad/precio.

## 🔒 Seguridad

- ⚠️ NUNCA subas tu API key a Git
- ⚠️ Configura límites de uso en OpenAI Dashboard
- ⚠️ Monitorea el uso regularmente

## 📊 Configurar Límites

1. Ve a: https://platform.openai.com/settings/organization/limits
2. Configura límites diarios/mensuales
3. Configura alertas de uso







