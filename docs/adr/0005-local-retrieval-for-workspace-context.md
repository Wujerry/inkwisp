# Retrieve workspace context locally

InkWisp may use documents beyond the active file for AI assistance, but it will index authorized Workspace content on the device and send only a small set of locally retrieved or explicitly referenced passages with each request. It will not upload an entire Workspace for provider-side indexing. The editor will disclose which files contributed context and let users exclude files or folders, trading some server-side retrieval sophistication for provider independence, bounded requests, and a clearer privacy model.
