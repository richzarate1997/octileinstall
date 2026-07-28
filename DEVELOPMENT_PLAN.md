# OC Tile Install Inc — Website Development Plan

**Purpose:** Step-by-step build plan for the OC Tile Install website. Written so any developer (or AI model) can execute it task-by-task without extra context. Work through phases in order. Do not skip acceptance criteria.

---

## 1. Rules for the Developer (read first)

1. **Never invent business facts.** All copy facts (license number, hours, phone, bio) come from Section 4 (Content Inventory). If a fact is missing, insert `<!-- TODO: [what's needed] -->` and move on.
2. **Photos come later.** Use the placeholder system in Section 5. Never embed base64 images and never hotlink stock photos.
3. **No frameworks, no build tools.** Plain HTML, CSS, and vanilla JavaScript only. Only external dependency allowed: Google Fonts.
4. **One task at a time.** Finish a task, verify its acceptance criteria, then move to the next.
5. **Mobile-first.** Every section must work at 375px width before it's considered done.
6. **Keep files small.** Separate CSS and JS files per Section 3. Do not inline everything into index.html.

---

## 2. Architecture

- **Type:** Static site, multi-file (HTML + separate CSS/JS + image files). Easier to edit incrementally than a single giant file.
- **Pages:** Single page (`index.html`) with anchor-linked sections for launch. Multi-page split is a post-launch option (Phase 8).
- **Hosting target:** Netlify (free tier) — supports custom domain, forms fallback, and serverless functions (needed later for the AI Design Advisor).
- **Version control:** Git repo from day one. Commit after each completed task with message `Phase X.Y: <task name>`.

## 3. File Structure

```
octileinstall/
├── index.html
├── css/
│   └── styles.css
├── js/
│   └── main.js          (nav, scroll-reveal, hero mosaic animation, lightbox, quote form)
├── images/
│   ├── logo.png                     (provided later)
│   └── gallery/                     (provided later — see Section 5)
├── favicon.ico                      (generate from logo later)
└── DEVELOPMENT_PLAN.md              (this file)
```

## 4. Content Inventory (single source of truth)

| Item | Value |
|---|---|
| Business name | OC Tile Install Inc |
| Owner | Alberto Lozano |
| Experience | 35+ years, specializing in custom-designed showers |
| License | CA Contractor License #1040037 — display in navbar area, About, and footer |
| Instagram | @octileinstall (https://instagram.com/octileinstall) |
| Phone | **(714) 624-3132** — clickable `tel:` link in navbar, hero, contact, quote form area, footer |
| Email | `<!-- TODO: email pending -->` |
| Hours | **7:00 AM – 6:00 PM** `<!-- TODO: confirm days of week -->` |
| Service area | Southern California: Orange County (home base), Los Angeles, Inland Empire, San Diego |
| Lead service | Custom Showers (always listed first) |
| Other services | Bathroom floors & walls, kitchen backsplashes, flooring (tile/marble/mosaic), tub surrounds, repairs `<!-- TODO: confirm final service list -->` |
| Domain | `<!-- TODO: confirm domain -->` |
| Form backend | Formspree — endpoint `<!-- TODO: create Formspree account, paste form ID -->` |
| Logo | ✅ Saved as `images/logo.png` (blue tile geometric design with "OC TILE Install Inc" + license #) |

**About section copy (updated):** My name is Alberto Lozano, and I bring over 35 years of experience in the tile industry. I specialize in custom-designed showers, combining craftsmanship, precision, and attention to detail to create high-quality, lasting results. I take pride in delivering work that reflects both functionality and style, tailored to each client's vision. Licensed California contractor #1040037.

**Testimonials:** 3 short testimonials referencing custom shower work. Mark each `<!-- TODO: replace with real review -->` until real reviews are provided.

## 5. Placeholder Image System

Photos will be dropped in later with **exact filenames** so no code changes are needed. Build gallery cards that reference these paths now; add CSS so a missing image shows a styled gray tile with the caption text instead of a broken icon (use `onerror` to swap in a `.placeholder` class).

| Filename | Caption | Aspect |
|---|---|---|
| `images/gallery/01-arched-hallway-mosaic.jpg` | Arched hallway mosaic | 3:4 |
| `images/gallery/02-starburst-shower-wall.jpg` | Starburst shower feature wall | 3:4 |
| `images/gallery/03-3d-geometric-shower.jpg` | 3D geometric shower | 3:4 |
| `images/gallery/04-hexagon-floor-medallion.jpg` | Hexagon floor medallion | 1:1 |
| `images/gallery/05-subway-shower-niche.jpg` | Subway tile shower with niche | 3:4 |
| `images/gallery/06-geometric-marble-floor.jpg` | Geometric marble floor | 1:1 |
| `images/gallery/07-tub-surround-penny-tile.jpg` | Tub surround with penny tile floor | 3:4 |

Logo: reference `images/logo.png` in navbar and footer; same `onerror` fallback showing the text "OC Tile Install".

## 6. Design System

**Updated 2026-07-27** — redesigned to a dark navy/blue system per reference mockup, replacing the original warm off-white palette below (kept here for history).

```css
:root {
  --navy: #0a0e1a;             /* page background */
  --navy-mid: #111827;         /* card/panel surfaces */
  --navy-light: #1c2535;       /* placeholder/alt surface */
  --blue: #1a6fd4;             /* primary — buttons, links */
  --blue-light: #3d9bff;       /* accent — highlights, hover, headings accent */
  --blue-dark: #0d3d7a;
  --white: #f0f4ff;            /* body text */
  --grey: #8a9ab5;             /* muted text */
  --font-display: 'Bebas Neue', sans-serif;      /* big headings */
  --font-body: 'Barlow', sans-serif;             /* paragraphs */
  --font-label: 'Barlow Condensed', sans-serif;  /* nav/buttons/labels, uppercase + letter-spacing */
  --radius: 2px;               /* mostly sharp corners; clip-path cut-corner on primary buttons */
  --container: 1200px;
}
```

Buttons: primary/accent = solid `--blue` with a cut-corner `clip-path`, hover → `--blue-light` + dark text; secondary = transparent outline. Section padding: large (6-8rem desktop) via `--section-padding` + `--section-padding-x`. Headings use `--font-display` (Bebas Neue renders as caps by design). Nav/labels/buttons use `--font-label` uppercase with letter-spacing. Load all three fonts from Google Fonts with `display=swap`.

<details>
<summary>Original design system (pre-redesign, warm palette)</summary>

```css
:root {
  --color-bg: #faf8f5;        /* warm off-white */
  --color-surface: #ffffff;
  --color-ink: #1f2933;       /* charcoal text */
  --color-primary: #0f4c5c;   /* deep teal — headers, buttons */
  --color-accent: #c86b3c;    /* terracotta — highlights, hover */
  --color-muted: #6b7280;
  --font-display: 'Playfair Display', serif;   /* headings */
  --font-body: 'Inter', sans-serif;            /* body */
  --radius: 8px;
  --shadow: 0 4px 16px rgba(0,0,0,0.08);
  --container: 1100px;
}
```
</details>

---

## 7. Build Phases

### Phase 1 — Skeleton & Design System
**1.1** Create file structure from Section 3; empty files OK.
**1.2** `index.html`: semantic skeleton with empty sections in this order, each with an `id`: `header/nav`, `#hero`, `#services`, `#gallery`, `#about`, `#service-area`, `#calculator`, `#testimonials`, `#quote`, `#contact`, `footer`. Include viewport meta, Google Fonts links, and links to `css/styles.css` and the three JS files (`defer`).
**1.3** `styles.css`: CSS reset, variables from Section 6, base typography, `.container` class, button classes, section spacing.
✅ *Accept:* Page loads with visible section headings, correct fonts/colors, no console errors, valid HTML (check with https://validator.w3.org).

### Phase 2 — Navbar, Hero, Footer
**2.1 Navbar:** logo left (with text fallback), anchor links (Services, Gallery, About, Calculator, Contact), clickable phone button right. Sticky on scroll. Mobile: hamburger menu toggled by JS (no library).
**2.2 Hero:** headline focused on custom showers (e.g., "Custom Showers & Tile Work, Built Right"), subline with 35+ years + license #, two CTAs: "Get a Free Quote" (→ `#quote`) and phone link. Background: CSS-animated tile mosaic — a grid of divs with staggered fade/color animation using the palette (pure CSS/JS, no images).
**2.3 Footer:** business name, license #, phone, hours, service area, Instagram link, copyright year via JS.
✅ *Accept:* Sticky nav works; hamburger opens/closes at 375px; all anchor links scroll to correct sections; `tel:` link present in nav, hero, footer.

### Phase 3 — Content Sections
**3.1 Services:** card grid (3 cols desktop / 1 col mobile). Custom Showers first with a "Specialty" badge. Each card: icon (inline SVG), title, 1–2 sentence description.
**3.2 About:** two-column (photo placeholder + text) using copy from Section 4. Include license # and Instagram link.
**3.3 Testimonials:** 3 cards with quote, name, city. TODO-marked placeholder reviews.
**3.4 Contact:** phone, email placeholder, hours, service area list, Instagram.
✅ *Accept:* All Section 4 facts appear correctly; no invented facts without TODO markers; grid collapses to 1 column at 480px.

### Phase 4 — Gallery
**4.1** Responsive grid (3/2/1 columns) of the 7 items from Section 5, each `<figure>` with `<img>` + `onerror` placeholder fallback + `<figcaption>`.
**4.2** Lightbox: click to open full-size overlay with caption, close on X/overlay/Esc. Vanilla JS, ~40 lines.
✅ *Accept:* With no image files present, gallery shows 7 styled captioned placeholders (no broken-image icons); dropping a correctly named file into `images/gallery/` makes it appear with zero code changes; lightbox opens/closes.

### Phase 5 — Interactive Features
**5.1 Tile calculator** — **removed 2026-07-27.** The original arithmetic calculator (length × width × waste ÷ tile size) shipped in Phases 1–8 but was pulled at the user's request: they want an **AI-powered tile estimator** instead (e.g. photo upload → room dimension/tile-count estimate), not a plain formula. Deferred to Phase 9 alongside the AI Design Advisor, since both need a backend (Netlify Function or similar) to call an LLM/vision API safely — can't be done as client-only JS like the old version.
**5.2 Service area map** — **replaced 2026-07-27.** The original canvas-drawn stylized SoCal coastline (`map.js`) was inaccurate. Replaced with a real embedded Google Map (keyless `https://maps.google.com/maps?...&output=embed` iframe, no API key needed) centered on the LA/OC/Inland Empire/San Diego region, with a CSS filter (`invert`/`grayscale`/`contrast`) to keep it visually dark-theme-consistent. `map.js` deleted — the iframe needs no JS.
✅ *Accept:* Map iframe loads real Google Maps imagery centered on the four service regions; no canvas/JS dependency.

### Phase 6 — Quote Form
**6.1** Form in `#quote`: name*, phone*, email, city, project type (dropdown of services, Custom Shower default), message*. HTML5 validation + minimal JS validation.
**6.2** POST to Formspree: `action="https://formspree.io/f/FORM_ID"` with `<!-- TODO: replace FORM_ID -->`. On submit, JS fetch with success/error message shown inline (no page reload). Honeypot field for spam.
✅ *Accept:* Empty required fields block submit with visible messages; with a test Formspree ID, submission shows success state. Form works with JS disabled (falls back to normal POST).

### Phase 7 — SEO & Launch Prep
**7.1 Meta:** `<title>` ("Custom Shower & Tile Installation Orange County | OC Tile Install Inc"), meta description (~155 chars), canonical URL (TODO domain), Open Graph + Twitter tags.
**7.2 JSON-LD:** `LocalBusiness` schema — name, owner, phone, hours (Mo–Sa 07:00–18:00), areaServed (the 4 regions), license in description, Instagram in `sameAs`.
**7.3 Keywords:** naturally place city/service combinations (custom shower installation + tile contractor × Orange County, Anaheim, Irvine, Santa Ana, Huntington Beach, Long Beach, LA, San Diego, Riverside) in headings, alt text, and a short "Areas We Serve" text block. No keyword-stuffed hidden text.
**7.4** `robots.txt` + minimal `sitemap.xml`; add favicon (TODO until logo provided); alt text on every image.
**7.5 Accessibility pass:** one `<h1>` only, logical heading order, labels on all inputs, visible focus states, color contrast ≥ 4.5:1.
✅ *Accept:* Schema passes https://validator.schema.org; Lighthouse (mobile): SEO ≥ 95, Accessibility ≥ 90, Performance ≥ 90.

### Phase 8 — Responsive & QA
**8.1** Test at 375px, 480px, 768px, 1024px, 1440px. Fix any horizontal scroll, overlapping text, or chopped layouts. Breakpoints: 768px (2-col), 480px (1-col, larger tap targets, reduced font sizes).
**8.2 Full QA checklist:** every link works; every `tel:` link dials; form validates; gallery fallbacks intact; no console errors; page weight < 500KB without photos.
**8.3 Deploy:** push to GitHub → connect Netlify → verify live URL → connect custom domain + HTTPS when domain confirmed.
✅ *Accept:* Site live on Netlify URL, QA checklist 100% pass.

### Phase 9 — Post-Launch (do NOT block launch on these)
- **AI Design Advisor:** ⚠️ Must NOT call the Anthropic API directly from the browser (exposes the API key). Build a Netlify Function (`/netlify/functions/advisor.js`) that holds the key in an env var, forwards the user's prompt to the API (claude-sonnet), and returns the reply. Front end: small chat box that fetches the function. Rate-limit + cap tokens.
- **AI-powered tile estimator:** replaces the plain-arithmetic calculator removed in the 2026-07-27 redesign (see Section 7, Phase 5.1). User wants something smarter than a formula — likely photo upload or richer project description → AI-estimated tile count/cost. Same constraint as the Design Advisor: needs a Netlify Function to hold the API key server-side, can't be client-only JS.
- Swap in real photos as Instagram posts come in (drop into `images/gallery/` per Section 5 naming).
- Real testimonials / Google Reviews link; before–after slider; email address; expanded About page or multi-page split if content grows.

---

## 8. Task Tracker

Copy this and check off as you go:

- [x] 1.1 File structure  - [x] 1.2 HTML skeleton  - [x] 1.3 Base CSS
- [x] 2.1 Navbar  - [x] 2.2 Hero  - [x] 2.3 Footer
- [x] 3.1 Services  - [x] 3.2 About  - [x] 3.3 Testimonials  - [x] 3.4 Contact
- [x] 4.1 Gallery grid  - [x] 4.2 Lightbox
- [x] 5.1 Calculator (removed 2026-07-27, deferred to Phase 9 as AI estimator)  - [x] 5.2 Service area map (real Google embed, replaced canvas 2026-07-27)
- [x] 6.1 Quote form  - [x] 6.2 Formspree hookup (awaiting real FORM_ID)
- [x] 7.1–7.5 SEO, schema, a11y
- [x] 8.1–8.2 Responsive QA  - [ ] 8.3 Deploy (needs GitHub push + Netlify connect — awaiting go-ahead)
- [ ] 9.x Post-launch items

**Open TODOs needed from Richard/Alberto:** phone number, email, final service list, Formspree form ID, domain, logo file, real reviews.
