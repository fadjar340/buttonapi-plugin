<div class="form-cell">
  <style>
    .api-progress-button {
      background-color: #1a73e8;
      color: white;
      font-size: 14px;
      padding: 10px 18px;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      transition: background-color 0.3s ease, transform 0.2s ease;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }
    .api-progress-button:hover {
      background-color: #155cb0;
      transform: translateY(-1px);
    }
    .api-progress-button:disabled {
      background-color: #b0c4de;
      cursor: not-allowed;
      opacity: 0.7;
    }
    .api-progress-status {
      margin-top: 12px;
      font-size: 14px;
      color: #444;
    }
    .api-progress-status span {
      font-weight: bold;
    }
  </style>

  <div>
    <button id="api-progress-startButton" class="api-progress-button">
      Start Process
    </button>
  </div>
  <div class="api-progress-status">
    Status: <span id="api-progress-statusText">Not Started</span>
  </div>

  <script>
    (function () {
      const pluginUrl      = "${pluginUrl}";
      const postUrl        = "${postUrl!''}";
      const progressUrl    = "${progressUrl!''}";
      const baseUrl        = "${baseUrl!''}";
      const timeout        = parseInt("${timeout!5}");
      const resultField    = "${resultField!"result"}";
      const progressField  = "${progressField!"inProgress"}";
      const authHeader     = "${authorizationHeader!''}";

      const btn = document.getElementById("api-progress-startButton");
      const status = document.getElementById("api-progress-statusText");
      let interval;

      btn.addEventListener("click", function () {
        if (!postUrl || !progressUrl || !baseUrl) {
          alert("Jenkins URLs not configured properly.");
          return;
        }

        btn.disabled = true;
        status.innerText = "Requesting Crumb…";

        fetch(pluginUrl + "?action=crumb&baseUrl=" + encodeURIComponent(baseUrl), {
          method: "GET",
          headers: { "Authorization": authHeader },
          credentials: "same-origin"
        })
        .then(r => {
          if (!r.ok) throw new Error("Crumb fetch failed: " + r.status);
          return r.json();
        })
        .then(json => {
          const crumbField = json.crumbRequestField;
          const crumbValue = json.crumb;

          status.innerText = "Triggering Jenkins job…";

          const params = new URLSearchParams();
          params.append("action", "trigger");
          params.append("crumbField", crumbField);
          params.append("crumbValue", crumbValue);
          params.append("postUrl", postUrl);
          params.append("baseUrl", baseUrl);

          return fetch(pluginUrl, {
            method: "POST",
            headers: {
              "Authorization": authHeader,
              "Content-Type": "application/x-www-form-urlencoded"
            },
            body: params.toString(),
            credentials: "same-origin"
          });
        })
        .then(r => {
          if (!r.ok) throw new Error("Trigger failed: " + r.status);
          status.innerText = "Build triggered. Monitoring…";

          interval = setInterval(() => {
            fetch(pluginUrl + "?action=progress&progressUrl=" + encodeURIComponent(progressUrl), {
              method: "GET",
              headers: { "Authorization": authHeader },
              credentials: "same-origin"
            })
            .then(r => r.json())
            .then(data => {
              if (data[progressField]) {
                status.innerText = "Building…";
              } else {
                clearInterval(interval);
                btn.disabled = false;
                const result = data[resultField];
                status.innerText = result === "SUCCESS" ? "✅ Completed"
                                 : result === "FAILURE" ? "❌ Failed"
                                 : "⚠️ Status: " + result;
              }
            })
            .catch(e => {
              clearInterval(interval);
              btn.disabled = false;
              status.innerText = "Progress error: " + e.message;
            });
          }, timeout * 1000);
        })
        .catch(e => {
          btn.disabled = false;
          status.innerText = "Error: " + e.message;
        });
      });
    })();
  </script>
</div>
