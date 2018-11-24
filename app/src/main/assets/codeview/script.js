function highlightCode() {
  android.onStartCodeHighlight();
  var codeHolder = document.getElementById('code-holder');
  var result = hljs.highlightBlock(codeHolder);
  android.onFinishCodeHighlight();
}