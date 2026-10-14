// Initialize Monaco Editor Loader
require.config({
  paths: {
    vs: 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.44.0/min/vs',
  },
});

const DEFAULT_CODE = `// Collaborative Code Editor - Live Demo
// Type here or open another browser tab to see real-time peer syncing!

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, collaborative coding world!");
    }
}
`;

// Generate unique Client ID for this browser tab
const clientId = 'User-' + Math.random().toString(36).substring(2, 6).toUpperCase();
document.getElementById('client-badge').textContent = clientId;

// BroadcastChannel creates zero-latency peer sync across browser tabs/windows
const syncChannel = new BroadcastChannel('codecollab_sync_channel');
let isRemoteUpdate = false;
let editorInstance = null;

require(['vs/editor/editor.main'], function () {
  editorInstance = monaco.editor.create(document.getElementById('editor-container'), {
    value: DEFAULT_CODE,
    language: 'java',
    theme: 'vs-dark',
    automaticLayout: true,
    fontSize: 14,
    fontFamily: "'JetBrains Mono', 'Fira Code', Consolas, monospace",
    lineNumbers: 'on',
    minimap: { enabled: true },
    scrollBeyondLastLine: false,
    tabSize: 4,
  });

  // Track cursor position on status bar
  editorInstance.onDidChangeCursorPosition((e) => {
    const pos = e.position;
    document.getElementById('cursor-position').textContent = `Ln ${pos.lineNumber}, Col ${pos.column}`;
  });

  // Local text input -> broadcast to other tabs
  editorInstance.onDidChangeModelContent((event) => {
    if (isRemoteUpdate) return;

    syncChannel.postMessage({
      type: 'DOC_EDIT',
      senderId: clientId,
      content: editorInstance.getValue(),
    });
  });

  // Language selector change
  const langSelect = document.getElementById('language-select');
  langSelect.addEventListener('change', (e) => {
    const selectedLang = e.target.value;
    monaco.editor.setModelLanguage(editorInstance.getModel(), selectedLang);

    syncChannel.postMessage({
      type: 'LANG_CHANGE',
      senderId: clientId,
      language: selectedLang,
    });
  });

  // Clear Editor
  document.getElementById('clear-btn').addEventListener('click', () => {
    if (confirm('Clear editor content?')) {
      editorInstance.setValue('');
    }
  });

  // Copy Code
  document.getElementById('copy-btn').addEventListener('click', () => {
    navigator.clipboard.writeText(editorInstance.getValue()).then(() => {
      const btn = document.getElementById('copy-btn');
      const originalText = btn.textContent;
      btn.textContent = 'Copied!';
      setTimeout(() => (btn.textContent = originalText), 1500);
    });
  });

  // Announce presence on open
  syncChannel.postMessage({ type: 'PEER_JOIN', senderId: clientId });
});

// Incoming messages from other browser tabs
syncChannel.onmessage = (event) => {
  const data = event.data;
  if (!data || data.senderId === clientId || !editorInstance) return;

  if (data.type === 'DOC_EDIT') {
    isRemoteUpdate = true;
    const currentState = editorInstance.saveViewState();
    editorInstance.setValue(data.content);
    if (currentState) {
      editorInstance.restoreViewState(currentState);
    }
    isRemoteUpdate = false;
  } else if (data.type === 'LANG_CHANGE') {
    const langSelect = document.getElementById('language-select');
    langSelect.value = data.language;
    monaco.editor.setModelLanguage(editorInstance.getModel(), data.language);
  }
};