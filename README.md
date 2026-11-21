# TV Panel Launcher

Brought to you by [Fullstack Systems](https://fullstacksystems.com).

<a href="https://www.buymeacoffee.com/denov" target="_blank" rel="noreferrer noopener"><img src="https://img.buymeacoffee.com/button-api/?text=Buy%20me%20a%20coffee&emoji=&slug=denov&button_colour=FFDD00&font_colour=000000&font_family=Bree&outline_colour=000000&coffee_colour=ffffff" alt="Buy me a coffee" height="40"></a>

A home screen for Android TV and Google TV with a row of Home Assistant chips under the clock (weather, garage door,
cameras, a reef tank, solar), filled in by [TV Panel](https://github.com/denov/home-assistant-tv-panel).

It's a fork of the launcher from [CleanYourTV](https://github.com/Gathin23/cleanyourtv), by Gathin and the CleanYourTV
contributors, which does all the hard work: a fast, quiet home screen with no ads or recommendations, a dock and grid
of your apps, Inputs, a clock and an ambient glow. Read CleanYourTV's README for everything the launcher can do. This
repo is only the launcher: for debloating the TV first, use CleanYourTV itself.

## What's different from CleanYourTV's launcher

- **The widget row:** chips under the clock, read from the app named by `widget_provider` (TV Panel by default). OK
  on a chip opens it in that app. With TV Panel not installed, there's no row.
- **`ambient_glow_color`:** one colour for the glow whatever app is focused, e.g. `"#228B22"`. Empty (the default)
  keeps the focused app's colour.
- **`back_opens_screensaver`:** with `back_opens_clock`, Back on Home opens the TV's own screensaver (Ambient Mode,
  photos) where it has one, and the launcher's clock otherwise.
- **Its own app id, `com.fullstacksystems.tvhome`,** so it installs beside CleanYourTV's launcher instead of over it.
- The install script is bash only (Linux, macOS, WSL or Git Bash on Windows).

## Install

You need JDK 17, the Android SDK, Python 3, and the TV's network debugging on (Settings → System → Developer options →
USB debugging; the TV asks to allow this computer the first time).

```bash
python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt
TV_HOST=<the TV's IP address> ./build-and-install.sh
```

The script builds the launcher, installs it, makes it Home, and **disables** (never uninstalls) the launcher that had
Home before: the TV's stock launcher, or CleanYourTV's. If the switch doesn't take, it puts the old one back.

Then install [TV Panel](https://github.com/denov/home-assistant-tv-panel) and pair it with Home Assistant to get the
chips.

### Your own settings

Every setting in [`config.xml`](app/src/main/res/values/config.xml) that says "a theme can set" also works from a JSON
file, written to the TV with the install:

```json
{
  "dock_apps": ["com.netflix.ninja", "com.google.android.youtube.tv"],
  "columns": 6,
  "icon_only_buttons": true,
  "ambient_glow_color": "#228B22",
  "back_opens_clock": true,
  "back_opens_screensaver": true
}
```

```bash
CONFIG=my-tv.json TV_HOST=<the TV's IP address> ./build-and-install.sh
```

`*.json` is git-ignored, so a TV's own file stays out of commits.

### Undo

Give Home back to the launcher the script disabled (shown as "Stock launcher:" when it ran), for example:

```bash
tools/venv/bin/python tools/tv.py 'pm enable --user 0 com.google.android.apps.tv.launcherx'
tools/venv/bin/python tools/tv.py 'cmd package set-home-activity --user 0 $(cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME com.google.android.apps.tv.launcherx | tail -n 1)'
```

Put CleanYourTV's `com.gathin.tvhome` in both lines instead if that was the one. Set `TV_HOST` for `tools/tv.py` as
for the install.

## The widget row, for app authors

An app shows chips by declaring `<meta-data android:name="com.gathin.tvhome.widgets" android:value="<its provider
authority>" />` on its application and answering `content://<authority>/widgets` with the columns `key`, `label`,
`value`, `icon` (PNG bytes) and `open` (0 or 1). OK on a chip with `open = 1` sends the app
`com.gathin.tvhome.action.OPEN_WIDGET` with the chip's `key` as an extra. A chip with an empty `value` is drawn as its
icon alone, and tab-separated labels and values as columns. The names keep CleanYourTV's prefix, so an app works with
either launcher. [`Widgets.java`](app/src/main/java/com/gathin/tvhome/Widgets.java) has the details.

## Build and test

```bash
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

Release builds are signed with your machine's debug key, for installing over ADB.

## Licence

MIT, see [LICENSE](LICENSE). The launcher is © Gathin and the CleanYourTV contributors; the changes here are ©
Fullstack Systems. The fonts and icons keep their own licences: [FONT-LICENSE-OFL.txt](FONT-LICENSE-OFL.txt) and
[ICONS-LICENSE-FLUENT-EMOJI.txt](ICONS-LICENSE-FLUENT-EMOJI.txt). The first commit is CleanYourTV's launcher exactly as
published at the upstream commit it names.
