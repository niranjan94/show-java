var lines;

function highlightCode() {
  try {
    android.onStartCodeHighlight();
  } catch (ignored) { }
  var codeHolder = document.getElementById('code-holder');
  hljs.highlightBlock(codeHolder);
  try {
    android.onFinishCodeHighlight();
    lines = document.querySelectorAll('td.ln');
  } catch (ignored) { }
}


function updateStyleAndClasses(styleUri, bodyClass) {
  document.getElementById('stylesheet').setAttribute('href', styleUri);
  document.getElementById('body').setAttribute('class', bodyClass);
}

function showHideLineNumber(isShown) {
  for (var i = 0; i < lines.length; i++) {
    lines[i].style.display = isShown ? '' : 'none';
  }
}

function changeFontSize(size) {
  document.body.style.fontSize = size + 'px';
}

function fillLineNumbers() {
  for (var i = 0; i < lines.length; i++) {
    lines[i].innerHTML = lines[i].getAttribute('line');
  }
}

function highlightLineNumber(number) {
  var x = document.querySelectorAll('.highlighted-line');
  if(x && x.length === 1) {
    x[0].classList.remove('highlighted-line');
  }
  if (number > 0) {
    var x = document.querySelectorAll("td.line[line='" + number + "']");
    if(x && x.length === 1) {
      x[0].classList.add('highlighted-line')
    }
  }
}