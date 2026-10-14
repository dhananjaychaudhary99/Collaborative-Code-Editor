require.config({
  paths: {
    vs: 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.44.0/min/vs',
  },
});

const STORAGE_KEY = 'codecollab_local_doc';
const THIRTY_DAYS_MS = 30 * 24 * 60 * 60 * 1000;

const DEFAULT_CODE = \// Local Collaborative Code Editor
// Data persists in your browser/local system for 30 days.
// Use "Delete All Data" button to clear everything at any time.

public class Main {
    public static void main(String[] args) {
        System.out.println("Running locally without GitHub!");
    }
}
\;

function loadData() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return { code: DEFAULT_CODE, lang: 'java' };

    const parsed = JSON.parse(raw);
    const now = Date.now();

    // 30 din beet gaye toh delete karein
    if (now - parsed.timestamp > THIRTY_DAYS_MS) {
      localStorage.removeItem(STORAGE_KEY);
      return { code: DEFAULT_CODE, lang: 'java' };
    }
    return { code: parsed.code, lang: parsed.lang || 'java' };
  } catch (e) {
    return { code: DEFAULT_CODE, lang: 'java' };
  }
}

function saveData(code, lang) {
  const payload = {
    code: code,
    lang: lang,
    timestamp: Date.now()
  };
  localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
}

function deleteData() {
  localStorage.removeItem(STORAGE_KEY);
}

const initial = loadData();
const clientId = 'User-' + Math.random().toString(36).substring(2, 6).toUpperCase();
document.getElementById('client-badge').textContent = clientId;

const syncChannel = new BroadcastChannel('codecollab_sync_channel');
let isRemoteUpdate = false;
let editorInstance = null;

require(['vs/editor/editor.main'], function () {
  editorInstance = monaco.editor.create(document.getElementById('editor-container'), {
    value: initial.code,
    language: initial.lang,
    theme: 'vs-dark',
    automaticLayout: true,
    fontSize: 14,
    fontFamily: "'JetBrains Mono', Consolas, monospace",
    lineNumbers: 'on',
    minimap: { enabled: true }
  });

  const langSelect = document.getElementById('language-select');
  langSelect.value = initial.lang;

  // Track cursor
  editorInstance.onDidChangeCursorPosition((e) => {
    const pos = e.position;
    document.getElementById('cursor-position').textContent = \Ln \, Col \\;
  });

  // Save on every local keystroke
  editorInstance.onDidChangeModelContent(() => {
    if (isRemoteUpdate) return;
    const currentCode = editorInstance.getValue();
    const currentLang = langSelect.value;
    saveData(currentCode, currentLang);

    syncChannel.postMessage({
      type: 'DOC_EDIT',
      senderId: clientId,
      content: currentCode,
      lang: currentLang
    });
  });

  // Language switch
  langSelect.addEventListener('change', (e) => {
    const selectedLang = e.target.value;
    monaco.editor.setModelLanguage(editorInstance.getModel(), selectedLang);
    saveData(editorInstance.getValue(), selectedLang);

    syncChannel.postMessage({
      type: 'LANG_CHANGE',
      senderId: clientId,
      language: selectedLang
    });
  });

  // Save directly to a local .txt file on computer
  document.getElementById('download-btn').addEventListener('click', () => {
    const blob = new Blob([editorInstance.getValue()], { type: 'text/plain;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'code_backup.txt';
    a.click();
    URL.revokeObjectURL(a.href);
  });

  // Delete All Data button
  document.getElementById('delete-btn').addEventListener('click', () => {
    if (confirm('Kya aap sure hain? Yeh 30-day saved local data ko permanently delete kar dega.')) {
      deleteData();
      editorInstance.setValue('');
      saveData('', langSelect.value);

      syncChannel.postMessage({
        type: 'DOC_DELETE',
        senderId: clientId
      });
      alert('Data permanently delete ho gaya!');
    }
  });
});

syncChannel.onmessage = (event) => {
  const data = event.data;
  if (!data || data.senderId === clientId || !editorInstance) return;

  if (data.type === 'DOC_EDIT') {
    isRemoteUpdate = true;
    const state = editorInstance.saveViewState();
    editorInstance.setValue(data.content);
    if (state) editorInstance.restoreViewState(state);
    saveData(data.content, data.lang);
    isRemoteUpdate = false;
  } else if (data.type === 'LANG_CHANGE') {
    document.getElementById('language-select').value = data.language;
    monaco.editor.setModelLanguage(editorInstance.getModel(), data.language);
  } else if (data.type === 'DOC_DELETE') {
    isRemoteUpdate = true;
    deleteData();
    editorInstance.setValue('');
    isRemoteUpdate = false;
  }
};