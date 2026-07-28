# OC Tile Install — Project Status & Confirmed Info

**Last updated:** July 20, 2026

## Confirmed Business Details

- **Business name:** OC Tile Install Inc
- **Owner:** Alberto Lozano
- **Phone:** (714) 624-3132 ✅
- **Location:** Orange County, CA ✅
- **Hours:** 7:00 AM – 6:00 PM ✅ (days of week still pending)
- **License:** CA Contractor License #1040037
- **Experience:** 35+ years, custom showers specialist
- **Instagram:** @octileinstall
- **Logo:** Blue geometric tile design ✅ (saved in outputs as logo.jpg)

## About Section (Confirmed Copy)

"My name is Alberto Lozano, and I bring over 35 years of experience in the tile industry. I specialize in custom-designed showers, combining craftsmanship, precision, and attention to detail to create high-quality, lasting results. I take pride in delivering work that reflects both functionality and style, tailored to each client's vision. Licensed California contractor #1040037."

## Still Pending

- **Email address** — needed for contact form and footer
- **Days of week** — confirm business operates Mon–Sat or other schedule
- **Final service list** — confirm beyond: custom showers, bathroom tile, backsplashes, flooring, tub surrounds, repairs
- **Domain** — confirm live URL when registered
- **Formspree ID** — create account, obtain form endpoint URL

## Development Plan Status

- ✅ **DEVELOPMENT_PLAN.md** created and saved (9 phases, Phase 1–8 for launch, Phase 9 post-launch)
- ✅ Concrete info integrated (phone, hours, logo, about copy)
- ✅ Phases 1–7 built: skeleton, navbar/hero/footer, content sections, gallery + lightbox, tile calculator, service-area map, quote form, SEO/schema/a11y
- ✅ Phase 8.1–8.2 responsive QA passed at 375/480/768/1024/1440px, no console errors, page weight ~43KB (HTML+CSS+JS, excluding images)
- ✅ **Visual redesign (2026-07-27)**: full re-skin to a dark navy/blue theme based on a reference mockup the user provided (`ZaraAI/index.html`) — Bebas Neue/Barlow/Barlow Condensed fonts, animated hero tile mosaic, glass nav, clip-path buttons, grout-line card grids, facts-only stats bar (35+ years, license #, 4 regions, 6 services — no invented numbers). All prior working features carried over unchanged: gallery lightbox + placeholder fallback, tile calculator math, Formspree quote form + honeypot, JSON-LD/SEO, mobile hamburger nav (the reference itself lacked one — kept ours). Re-verified via DOM/computed-style checks at all breakpoints (screenshot tool was down for this session; see note below).
- ⏳ Phase 8.3 (deploy to GitHub + Netlify) not yet done — requires explicit go-ahead since it pushes to a shared remote
- ⏳ Phase 9 (AI Design Advisor, real photos, real reviews) intentionally deferred post-launch — the reference mockup's AI Advisor JS is non-functional (broken direct-to-Anthropic fetch, no auth, would be CORS-blocked anyway); a real version needs a Netlify Function holding the API key server-side, per this plan's original Phase 9 note

**Known gap:** the Browser-pane screenshot tool timed out for the whole redesign session (confirmed unrelated to this codebase — even a blank external site failed to screenshot). Functionality was verified through DOM inspection, computed styles, and simulated interactions instead. Worth a quick visual pass in a real browser next session to confirm nothing looks off.

---

**Next step:** Share remaining items when ready — email address, confirmed days of week, final service list, Formspree form ID, confirmed domain, and real photos for the gallery/about section. Say the word when ready to push to GitHub and deploy to Netlify.
