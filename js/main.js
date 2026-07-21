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

// Gallery lightbox
(function () {
  var lightbox = document.getElementById('lightbox');
  var lightboxImg = document.getElementById('lightbox-img');
  var lightboxPlaceholder = document.getElementById('lightbox-placeholder');
  var lightboxCaption = document.getElementById('lightbox-caption');
  var closeBtn = document.getElementById('lightbox-close');
  var items = document.querySelectorAll('.gallery-item');
  if (!lightbox || !items.length) return;

  function open(item) {
    var img = item.querySelector('img');
    var placeholder = item.querySelector('.gallery-placeholder');
    var caption = item.getAttribute('data-caption') || '';

    if (placeholder && placeholder.classList.contains('is-visible')) {
      lightboxImg.setAttribute('src', '');
      lightboxImg.alt = '';
      lightboxPlaceholder.textContent = caption;
      lightboxPlaceholder.classList.add('is-visible');
    } else {
      lightboxImg.src = img.src;
      lightboxImg.alt = img.alt;
      lightboxPlaceholder.classList.remove('is-visible');
    }

    lightboxCaption.textContent = caption;
    lightbox.classList.add('is-open');
    lightbox.setAttribute('aria-hidden', 'false');
  }

  function close() {
    lightbox.classList.remove('is-open');
    lightbox.setAttribute('aria-hidden', 'true');
  }

  items.forEach(function (item) {
    item.addEventListener('click', function () { open(item); });
  });

  closeBtn.addEventListener('click', close);

  lightbox.addEventListener('click', function (e) {
    if (e.target === lightbox) close();
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && lightbox.classList.contains('is-open')) close();
  });
})();
