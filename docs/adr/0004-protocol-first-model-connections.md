# Use protocol-first model connections

InkWisp will support user-configured models through a small set of protocol adapters, initially targeting OpenAI Chat Completions, OpenAI Responses, Anthropic Messages, Google Gemini, and configurable OpenAI-compatible endpoints. It will not build a separate integration for every model vendor. Users can save multiple connections containing their own endpoint, credentials, model identifier, and optional request settings; this maximizes provider coverage while keeping protocol behavior testable and vendor churn outside the editor core.
