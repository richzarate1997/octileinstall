// Mobile nav toggle
(function () {
  var toggle = document.getElementById('navbar-toggle');
  var nav = document.getElementById('primary-nav');
  if (!toggle || !nav) return;

  toggle.addEventListener('click', function () {
    var isOpen = nav.classList.toggle('is-open');
    toggle.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
  });

  nav.querySelectorAll('a').forEach(function (link) {
    link.addEventListener('click', function () {
      nav.classList.remove('is-open');
      toggle.setAttribute('aria-expanded', 'false');
    });
  });
})();

// Hero mosaic background
(function () {
  var mosaic = document.getElementById('hero-mosaic');
  if (!mosaic) return;

  var cols = 8;
  var rows = 6;
  var colors = ['#0f4c5c', '#c86b3c', '#1a6478', '#0a3945'];

  for (var i = 0; i < cols * rows; i++) {
    var tile = document.createElement('span');
    tile.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
    tile.style.animationDelay = (Math.random() * 6).toFixed(2) + 's';
    mosaic.appendChild(tile);
  }
})();

// Footer copyright year
(function () {
  var yearEl = document.getElementById('current-year');
  if (yearEl) yearEl.textContent = new Date().getFullYear();
})();
