OTP STUDIO — Android project

Features
- Bengali OTP number dashboard (bundled HTML page)
- Floating logo bubble and movable floating panel
- Start/stop from the corner button
- Number and OTP clipboard copy
- Range selection, OTP polling, and local history
- GitHub Actions workflow builds a debug APK

Build on GitHub
1. Create a new GitHub repository.
2. Upload the contents of this folder (the OtpApp folder contents, not the outer zip wrapper).
3. Open Actions and run "Build APK", or push a commit.
4. Open the completed workflow run and download the "OtpApp-debug-apk" artifact.
5. Extract the artifact ZIP to get app-debug.apk.

First launch
1. Open Settings inside the OTP page.
2. Enter your own API base URL and API key.
3. Save, then select a range and request a number.
4. Tap the corner Start button and allow "Display over other apps".
5. Tap the floating logo bubble to open the panel. Drag it to move it; use the minimize/close controls.

Important safety and privacy notes
- The app copies the generated number and OTP to the clipboard.
- It does not inspect, detect, or automatically fill login/OTP fields on third-party websites or other apps.
- Do not use generated numbers to access accounts that you do not own or have permission to use.
- Keep API keys private. A key embedded in a client-side app can be extracted; use a restricted key or a server-side proxy for production.
- The API endpoint and logo URL are external services and require internet access.
