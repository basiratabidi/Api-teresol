# Frontend

The web UI is a single static page, served by Quarkus from
`meraApnaBank/core-api-accounts/src/main/resources/META-INF/resources/`.
There's no framework and no build step — edit the files and refresh (Quarkus
dev mode serves them live).

```
resources/
├── index.html            # markup, CSS and JavaScript for the whole UI
└── assets/
    ├── logo.png          # bank logo (sidebar, login, coins, favicon)
    ├── skyline.svg       # drifting city skyline
    ├── card.svg          # animated bank card illustration
    ├── shield.svg        # shield with a self-drawing tick
    └── three.min.js      # Three.js r160, bundled so the UI works offline
```

## Screens

- **Login / Sign up** — full-screen overlay with a 3D background, a
  Log in / Sign up toggle, show/hide password, a password-strength meter on
  signup, and a shake animation on errors. On phones the side panel is hidden
  and the skyline shows behind the form.
- **Dashboard** — greeting with the user's first name, hero with a 3D coin,
  a scrolling ticker, overview stats, per-region branch/account counts (loaded
  live from the API), and quick-access cards that link to the other pages.
- **Branches / Accounts / Transactions** — the forms and tables that call the
  REST API (see the main README for endpoints).

The app shell is hidden until authenticated (`body.authed` toggles between the
login overlay and the app). See [authentication.md](authentication.md).

## Fonts

**Montserrat** (weights 400–800) from Google Fonts, with system fonts as a
fallback. It needs internet access on first load; without it the fallback
fonts are used.

## 3D scenes

Two Three.js scenes are created by `createScene()`:

| Scene | Canvas | Contents |
|-------|--------|----------|
| `scenes.auth` | `#auth-canvas` (full screen) | five spinning logo coins, a torus ring, a wireframe icosahedron, drifting gold sparkles; the camera follows the mouse |
| `scenes.hero` | `#hero-canvas` (dashboard hero) | three spinning coins and sparkles |

- The coin face is drawn on a canvas and the bank logo is stamped onto it once
  `assets/logo.png` loads.
- Only the visible scene renders (`pause()` / `resume()` on login and logout).
- If WebGL isn't available, `createScene()` returns a no-op and the rest of the
  page works normally.
- With `prefers-reduced-motion`, the coins stay still.

## Animations and interactions

CSS: staggered card entrance (`rise`, delay from `--i`), animated hero
gradient, floating card and shield, drifting skyline, logo shine, pulsing
status dots, ticker marquee, page-title fade between views.

JavaScript: eased number count-up on the dashboard, 3D hover tilt on `.tilt`
cards, button click ripple, table-row fade-in.

All CSS animation is shortened to near-zero under `prefers-reduced-motion`.

## Behaviour details

- **API calls** go through `callApi()`, which adds the bearer token and, on a
  `401`, logs out and shows "Your session has expired."
- **Validation** — `requireFields()` / `requireAmount()` run before each
  request: empty fields get a red outline and a message, amounts must be
  greater than zero (opening/updated balances may be zero), and a transfer to
  the same account is refused. The server validates again.
- **Confirmations** — deleting a branch, closing an account and transfers use
  the modal from `askConfirm()` (Escape cancels).
- **Enter** in any form row clicks that row's primary button.
- **Tables** show a spinner while loading, an empty-state message, or an error
  message.
- **Money** is formatted by `fmtMoney()` as `PKR 15,000.00`.
- **Safety** — values from the API are escaped with `esc()` before being
  written into table HTML.

## Changing the look

Colours and radii are CSS variables at the top of the `<style>` block
(`--green`, `--gold`, `--radius`, …). To swap the logo, replace
`assets/logo.png` (keep a square-ish image; it's also used on the coins).

## Updating Three.js

`assets/three.min.js` is Three.js r160 with its deprecation `console.warn`
removed (the file starts with `!function(t,e){…`). To upgrade, either
download a newer classic build, or switch the page to ES modules and remove
the bundled file.
