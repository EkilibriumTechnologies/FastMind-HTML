/**
 * Firebase Cloud Functions for FastMind
 * Secure API Key Management
 * 
 * IMPORTANT: Set your API keys as environment variables in Firebase Console:
 * - OPENAI_API_KEY
 * - GEMINI_API_KEY
 * 
 * To set environment variables:
 * firebase functions:config:set openai.key="your-key-here"
 * firebase functions:config:set gemini.key="your-key-here"
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');
const cors = require('cors')({ 
  origin: true, // Allow all origins for now
  methods: ['GET', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization']
});

admin.initializeApp();

/**
 * Get API Keys securely
 * This function returns API keys stored as Firebase environment variables
 * 
 * Security: Can be restricted to authenticated users or specific domains
 */
exports.getApiKeys = functions.https.onRequest((req, res) => {
  // Handle OPTIONS preflight request FIRST, before CORS wrapper
  if (req.method === 'OPTIONS') {
    res.set('Access-Control-Allow-Origin', '*');
    res.set('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.set('Access-Control-Allow-Headers', 'Content-Type');
    res.set('Access-Control-Max-Age', '3600');
    return res.status(204).send('');
  }
  
  return cors(req, res, () => {
    // Optional: Add authentication check here
    // const authHeader = req.headers.authorization;
    // if (!authHeader) {
    //   return res.status(401).json({ error: 'Unauthorized' });
    // }

    // Optional: Add domain/origin check
    // const origin = req.headers.origin || req.headers.referer;
    // const allowedOrigins = ['https://yourdomain.com', 'http://localhost:5500'];
    // if (!allowedOrigins.some(o => origin && origin.includes(o))) {
    //   return res.status(403).json({ error: 'Forbidden origin' });
    // }

    try {
      // Get API keys from environment variables (modern approach)
      // Fallback to functions.config() for backward compatibility
      let openaiKey = process.env.OPENAI_API_KEY;
      let geminiKey = process.env.GEMINI_API_KEY;
      
      // Fallback to deprecated functions.config() if env vars not set
      if (!openaiKey || !geminiKey) {
        try {
          const openaiConfig = functions.config().openai || {};
          const geminiConfig = functions.config().gemini || {};
          openaiKey = openaiKey || openaiConfig.key;
          geminiKey = geminiKey || geminiConfig.key;
        } catch (e) {
          // functions.config() may not be available
          console.warn('functions.config() not available, using env vars only');
        }
      }

      if (!openaiKey && !geminiKey) {
        return res.status(500).json({ 
          error: 'API keys not configured',
          message: 'Please configure OPENAI_API_KEY and GEMINI_API_KEY in Firebase Functions'
        });
      }

      // Return keys securely
      res.status(200).json({
        success: true,
        keys: {
          openai: openaiKey || null,
          gemini: geminiKey || null
        },
        timestamp: admin.firestore.Timestamp.now().toMillis()
      });

    } catch (error) {
      console.error('Error getting API keys:', error);
      res.status(500).json({ 
        error: 'Internal server error',
        message: error.message 
      });
    }
  });
});

/**
 * Health check endpoint
 */
exports.healthCheck = functions.https.onRequest((req, res) => {
  return cors(req, res, () => {
    res.status(200).json({ 
      status: 'ok',
      service: 'FastMind API Key Service',
      timestamp: Date.now()
    });
  });
});

