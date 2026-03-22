<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>Tap Choice & Tap Tap Match — UI Mockup</title>
<style>
  body {
    font-family: sans-serif;
    background: #f5f5f0;
    display: flex;
    justify-content: center;
    align-items: flex-start;
    padding: 40px 20px;
    min-height: 100vh;
    margin: 0;
  }
</style>
</head>
<body>

<div style="display:flex;gap:32px;flex-wrap:wrap;justify-content:center;padding:1.5rem 0;">

  <!-- CARD: TAP_CHOICE -->
  <div>
    <p style="font-size:12px;color:#888;margin:0 0 8px;text-transform:uppercase;letter-spacing:0.08em;">TAP_CHOICE</p>
    <div style="width:300px;background:#fff;border:0.5px solid #e0e0e0;border-radius:24px;padding:24px 20px;display:flex;flex-direction:column;align-items:center;gap:20px;">
      
      <!-- Subject emoji -->
      <div style="width:120px;height:120px;background:#FFF8EC;border-radius:20px;display:flex;align-items:center;justify-content:center;font-size:72px;line-height:1;">🍎</div>

      <!-- Question label -->
      <p style="margin:0;font-size:13px;color:#888;letter-spacing:0.04em;">what color is this?</p>

      <!-- Color circles -->
      <div style="display:flex;gap:16px;justify-content:center;">
        <div style="width:72px;height:72px;border-radius:50%;background:#E24B4A;border:3px solid transparent;"></div>
        <div style="width:72px;height:72px;border-radius:50%;background:#378ADD;border:3px solid transparent;"></div>
        <div style="width:72px;height:72px;border-radius:50%;background:#EF9F27;border:3px solid transparent;"></div>
      </div>

      <!-- State: correct tapped -->
      <div style="width:100%;border-top:0.5px solid #e0e0e0;padding-top:16px;">
        <p style="margin:0 0 8px;font-size:11px;color:#aaa;">↓ after correct tap</p>
        <div style="display:flex;gap:16px;justify-content:center;">
          <div style="width:72px;height:72px;border-radius:50%;background:#E24B4A;border:3px solid #3C3489;outline:4px solid #CECBF6;"></div>
          <div style="width:72px;height:72px;border-radius:50%;background:#378ADD;opacity:0.35;"></div>
          <div style="width:72px;height:72px;border-radius:50%;background:#EF9F27;opacity:0.35;"></div>
        </div>
      </div>

    </div>
  </div>

  <!-- CARD: TAP_TAP_MATCH -->
  <div>
    <p style="font-size:12px;color:#888;margin:0 0 8px;text-transform:uppercase;letter-spacing:0.08em;">TAP_TAP_MATCH</p>
    <div style="width:300px;background:#fff;border:0.5px solid #e0e0e0;border-radius:24px;padding:24px 20px;display:flex;flex-direction:column;align-items:center;gap:16px;">

      <!-- Question label -->
      <p style="margin:0;font-size:13px;color:#888;letter-spacing:0.04em;">find the matching pair</p>

      <!-- 2x3 grid -->
      <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:10px;width:100%;">
        <div style="background:#EAF3DE;border-radius:16px;height:80px;display:flex;align-items:center;justify-content:center;font-size:40px;">🔴</div>
        <div style="background:#E6F1FB;border-radius:16px;height:80px;display:flex;align-items:center;justify-content:center;font-size:40px;">⭐</div>
        <div style="background:#FAEEDA;border-radius:16px;height:80px;display:flex;align-items:center;justify-content:center;font-size:40px;">🔴</div>

        <div style="background:#EEEDFE;border-radius:16px;height:80px;border:2.5px solid #534AB7;display:flex;align-items:center;justify-content:center;font-size:40px;">⭐</div>
        <div style="background:#FAEEDA;border-radius:16px;height:80px;display:flex;align-items:center;justify-content:center;font-size:40px;">🟦</div>
        <div style="background:#E1F5EE;border-radius:16px;height:80px;display:flex;align-items:center;justify-content:center;font-size:40px;">🟦</div>
      </div>

      <!-- State: after match -->
      <div style="width:100%;border-top:0.5px solid #e0e0e0;padding-top:12px;">
        <p style="margin:0 0 8px;font-size:11px;color:#aaa;">↓ after matched pair</p>
        <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:10px;width:100%;">
          <div style="background:#EAF3DE;border-radius:16px;height:80px;display:flex;align-items:center;justify-content:center;font-size:40px;opacity:0.4;">🔴</div>
          <div style="background:#E6F1FB;border-radius:16px;height:80px;display:flex;align-items:center;justify-content:center;font-size:40px;opacity:0.4;">⭐</div>
          <div style="background:#EAF3DE;border:2.5px solid #3B6D11;border-radius:16px;height:80px;display:flex;flex-direction:column;align-items:center;justify-content:center;font-size:32px;">🔴<span style="font-size:14px;color:#3B6D11;font-weight:500;">✓</span></div>

          <div style="background:#EEEDFE;border:2.5px solid #3B6D11;border-radius:16px;height:80px;display:flex;flex-direction:column;align-items:center;justify-content:center;font-size:32px;">🔴<span style="font-size:14px;color:#3B6D11;font-weight:500;">✓</span></div>
          <div style="background:#FAEEDA;border-radius:16px;height:80px;display:flex;align-items:center;justify-content:center;font-size:40px;opacity:0.4;">🟦</div>
          <div style="background:#E1F5EE;border-radius:16px;height:80px;display:flex;align-items:center;justify-content:center;font-size:40px;opacity:0.4;">🟦</div>
        </div>
      </div>

    </div>
  </div>

</div>

</body>
</html>
