(function () {
  var form = document.getElementById('calculator-form');
  if (!form) return;

  var errorEl = document.getElementById('calculator-error');
  var resultEl = document.getElementById('calculator-result');
  var sqftEl = document.getElementById('calc-result-sqft');
  var tilesEl = document.getElementById('calc-result-tiles');

  form.addEventListener('submit', function (e) {
    e.preventDefault();

    var length = parseFloat(form.length.value);
    var width = parseFloat(form.width.value);
    var tileArea = parseFloat(form.tileSize.value);
    var waste = parseFloat(form.waste.value);

    if (!(length > 0) || !(width > 0)) {
      errorEl.textContent = 'Please enter a length and width greater than 0.';
      resultEl.hidden = true;
      return;
    }

    if (!(waste >= 0)) {
      errorEl.textContent = 'Waste percentage cannot be negative.';
      resultEl.hidden = true;
      return;
    }

    errorEl.textContent = '';

    var area = length * width;
    var total = area * (1 + waste / 100);
    var tiles = Math.ceil(total / tileArea);

    sqftEl.textContent = Math.round(total * 100) / 100;
    tilesEl.textContent = tiles;
    resultEl.hidden = false;
  });
})();
