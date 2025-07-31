#!/bin/sh

# Debug environment variables
echo "=== Railway Environment Variables ==="
echo "OPENAI_API_KEY exists: $([ -n "$OPENAI_API_KEY" ] && echo "YES" || echo "NO")"
echo "GOOGLE_CLIENT_ID exists: $([ -n "$GOOGLE_CLIENT_ID" ] && echo "YES" || echo "NO")"
echo "GOOGLE_CLIENT_SECRET exists: $([ -n "$GOOGLE_CLIENT_SECRET" ] && echo "YES" || echo "NO")"
echo "WEAVIATE_URL: $WEAVIATE_URL"
echo "===================================="

# Start the application with all environment variables
exec java \
  -Dopenai.api.key="$OPENAI_API_KEY" \
  -Dgoogle.client.id="$GOOGLE_CLIENT_ID" \
  -Dgoogle.client.secret="$GOOGLE_CLIENT_SECRET" \
  -Dgoogle.redirect.uri="$GOOGLE_REDIRECT_URI" \
  -Dweaviate.url="$WEAVIATE_URL" \
  -Dweaviate.api-key="$WEAVIATE_API_KEY" \
  -Dfrontend.redirect.uri="$FRONTEND_REDIRECT_URI" \
  -Dupload.external.endpoint="$UPLOAD_EXTERNAL_ENDPOINT" \
  -jar app.jar