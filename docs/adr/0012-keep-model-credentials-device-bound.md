# Keep model credentials device-bound

Model credentials will be encrypted under Android Keystore protection and remain bound to the device on which the user entered them. Configuration export may include protocol, endpoint, model identifier, request settings, and Saved Instructions, but never API keys or other secrets; a restored configuration requires credentials to be entered again. This deliberately trades friction during migration for safer backups and a smaller credential-leak surface.
