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

// Hero tile mosaic background
(function () {
  var mosaic = document.getElementById('hero-mosaic');
  if (!mosaic) return;

  var order = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16];

  order.forEach(function (n) {
    var tile = document.createElement('div');
    tile.className = 'tile-cell t' + n;
    mosaic.appendChild(tile);
  });
})();

// Scroll-reveal animation
(function () {
  var revealEls = document.querySelectorAll('.reveal');
  if (!revealEls.length || !('IntersectionObserver' in window)) return;

  var observer = new IntersectionObserver(function (entries) {
    entries.forEach(function (entry) {
      if (entry.isIntersecting) entry.target.classList.add('visible');
    });
  }, { threshold: 0.1 });

  revealEls.forEach(function (el) { observer.observe(el); });
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
