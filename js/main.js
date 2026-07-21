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

// Quote form validation + Formspree submission
(function () {
  var form = document.getElementById('quote-form');
  var messageEl = document.getElementById('quote-form-message');
  if (!form || !messageEl) return;

  function clearFieldErrors() {
    form.querySelectorAll('.field-error').forEach(function (el) { el.remove(); });
  }

  function showFieldError(field, text) {
    var error = document.createElement('span');
    error.className = 'field-error';
    error.textContent = text;
    field.insertAdjacentElement('afterend', error);
  }

  function validate() {
    clearFieldErrors();
    var valid = true;

    ['name', 'phone', 'message'].forEach(function (fieldName) {
      var field = form.elements[fieldName];
      if (!field.value.trim()) {
        showFieldError(field, 'This field is required.');
        valid = false;
      }
    });

    var email = form.elements.email;
    if (email.value.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
      showFieldError(email, 'Please enter a valid email address.');
      valid = false;
    }

    return valid;
  }

  form.addEventListener('submit', function (e) {
    e.preventDefault();

    if (form.elements.company.value) return; // honeypot triggered, silently drop

    if (!validate()) {
      messageEl.textContent = 'Please fix the errors above.';
      messageEl.className = 'quote-form-message is-error';
      return;
    }

    messageEl.textContent = 'Sending...';
    messageEl.className = 'quote-form-message';

    fetch(form.action, {
      method: 'POST',
      body: new FormData(form),
      headers: { Accept: 'application/json' }
    })
      .then(function (response) {
        if (response.ok) {
          messageEl.textContent = 'Thanks! Your quote request has been sent.';
          messageEl.className = 'quote-form-message is-success';
          form.reset();
        } else {
          messageEl.textContent = 'Something went wrong. Please call us instead.';
          messageEl.className = 'quote-form-message is-error';
        }
      })
      .catch(function () {
        messageEl.textContent = 'Something went wrong. Please call us instead.';
        messageEl.className = 'quote-form-message is-error';
      });
  });
})();
