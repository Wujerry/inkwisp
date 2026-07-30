# Separate Inline Prediction protocol from general model requests

A Model Connection will keep one shared identity and credential while allowing Inline Prediction to select its own protocol, endpoint, model override, and optional Prompt Format. Assisted Edits remain on the connection's general chat-style adapter; native FIM services and self-hosted formatted FIM models can therefore use their actual contracts without vendor-specific conditionals or forcing the entire connection onto a beta completion endpoint.
