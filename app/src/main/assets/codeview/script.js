function highlightCode() {
  try {
      android.onStartCodeHighlight();
  } catch(ignored) { }
  var codeHolder = document.getElementById('code-holder');
  var result = hljs.highlightBlock(codeHolder);
  try {
    android.onFinishCodeHighlight();
  } catch(ignored) { }
}