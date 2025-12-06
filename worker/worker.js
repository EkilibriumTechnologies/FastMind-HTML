export default {
  async fetch(request, env) {

    // CORS preflight support
    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 200,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Headers": "*",
          "Access-Control-Allow-Methods": "POST, GET, OPTIONS",
        }
      });
    }

    const apiKey = "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";

    try {
      const requestBody = await request.json();
      
      // Handle new format: { model, input } -> convert to OpenAI format
      // Support RAG by parsing system prompt and user question from input
      let openaiBody;
      if (requestBody.input) {
        // New format: single input message (may contain system prompt + user question)
        const inputText = requestBody.input;
        
        // Try to extract system prompt if it's in the format "SYSTEM PROMPT\n\nUSER QUESTION"
        let systemMessage = null;
        let userMessage = inputText;
        
        // Check if input contains a system prompt pattern
        const systemPromptMatch = inputText.match(/^(You are FastMind[^\n]*(?:\n(?!USER QUESTION:)[^\n]*)*)/i);
        const userQuestionMatch = inputText.match(/USER QUESTION:\s*(.+)$/i);
        
        if (systemPromptMatch && userQuestionMatch) {
          // Extract system prompt and user question
          systemMessage = systemPromptMatch[1].trim();
          userMessage = userQuestionMatch[1].trim();
        } else if (inputText.includes("You are FastMind") && inputText.includes("USER QUESTION:")) {
          // Alternative parsing: split by "USER QUESTION:"
          const parts = inputText.split(/USER QUESTION:\s*/i);
          if (parts.length === 2) {
            systemMessage = parts[0].trim();
            userMessage = parts[1].trim();
          }
        }
        
        // Build messages array with system message if found
        const messages = [];
        if (systemMessage) {
          messages.push({
            role: "system",
            content: systemMessage
          });
        }
        messages.push({
          role: "user",
          content: userMessage
        });
        
        openaiBody = {
          model: requestBody.model || "gpt-4o-mini",
          messages: messages,
          temperature: 0.7,
          max_tokens: 1000
        };
      } else {
        // Legacy format: already in OpenAI format
        openaiBody = requestBody;
      }

      const openaiResponse = await fetch("https://api.openai.com/v1/chat/completions", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${apiKey}`
        },
        body: JSON.stringify(openaiBody)
      });

      // Check if OpenAI response is ok
      if (!openaiResponse.ok) {
        const errorData = await openaiResponse.json().catch(() => ({ error: { message: "Unknown error" } }));
        console.error("OpenAI API error:", openaiResponse.status, errorData);
        
        return new Response(JSON.stringify({
          error: errorData.error?.message || `OpenAI API error: ${openaiResponse.status}`,
          output: [{ content: [{ text: `Error: ${errorData.error?.message || "AI service unavailable"}` }] }]
        }), {
          status: openaiResponse.status,
          headers: {
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Headers": "*",
            "Access-Control-Allow-Methods": "POST",
            "Content-Type": "application/json"
          }
        });
      }

      const openaiData = await openaiResponse.json();

      // Check if response has error
      if (openaiData.error) {
        console.error("OpenAI response error:", openaiData.error);
        return new Response(JSON.stringify({
          error: openaiData.error.message,
          output: [{ content: [{ text: `Error: ${openaiData.error.message}` }] }]
        }), {
          status: 500,
          headers: {
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Headers": "*",
            "Access-Control-Allow-Methods": "POST",
            "Content-Type": "application/json"
          }
        });
      }

      // Transform OpenAI response to match frontend expected format
      // Frontend expects: data?.output?.[0]?.content?.[0]?.text
      const text = openaiData.choices?.[0]?.message?.content || "";
      
      if (!text) {
        console.error("No text in OpenAI response:", openaiData);
        return new Response(JSON.stringify({
          error: "No response from AI",
          output: [{ content: [{ text: "No response from AI model" }] }]
        }), {
          status: 500,
          headers: {
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Headers": "*",
            "Access-Control-Allow-Methods": "POST",
            "Content-Type": "application/json"
          }
        });
      }

      const transformedResponse = {
        output: [
          {
            content: [
              {
                text: text
              }
            ]
          }
        ],
        // Also include original format for backward compatibility
        choices: openaiData.choices,
        model: openaiData.model,
        usage: openaiData.usage
      };

      return new Response(JSON.stringify(transformedResponse), {
        status: 200,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Headers": "*",
          "Access-Control-Allow-Methods": "POST",
          "Content-Type": "application/json"
        }
      });

    } catch (err) {
      return new Response(JSON.stringify({ 
        error: err.message,
        output: [{ content: [{ text: "Error processing request" }] }]
      }), {
        status: 500,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Content-Type": "application/json"
        }
      });
    }
  }
};





