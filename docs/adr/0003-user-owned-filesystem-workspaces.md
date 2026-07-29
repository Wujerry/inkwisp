# Use user-owned filesystem workspaces

InkWisp will organize documents around folders authorized through Android's system file access framework and will edit Markdown files in place; it will also open standalone files through the system picker. It will not make an app-private database the source of truth. This preserves user ownership and interoperability with Android document providers and third-party synchronization tools, at the cost of handling persisted permissions and provider-specific filesystem behavior.
