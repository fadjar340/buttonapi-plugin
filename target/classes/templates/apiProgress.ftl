<div class="form-cell">
    <style>
        .api-progress-button {
            background-color: #007bff;
            color: white;
            padding: 10px 20px;
            border: none;
            border-radius: 4px;
            font-size: 16px;
            cursor: pointer;
        }

        .api-progress-button:disabled {
            background-color: #cccccc;
            cursor: not-allowed;
        }

        .api-progress-status {
            margin-top: 10px;
            font-weight: bold;
            font-size: 14px;
        }
    </style>

    <div id="api-progress-container">
        <button type="button" id="api-progress-startButton" class="api-progress-button">
            ${buttonLabel!"Start Process"}
        </button>
        <div class="api-progress-status">
            Status: <span id="api-progress-statusText">Not Started</span>
        </div>
    </div>

    <script>
    (function(){
        var baseUrl = "${baseUrl}";
        var authorizationHeader = "${authorizationHeader}";
        var timeout = parseInt("${timeout!30}");
        var resultField = "${resultField!"result"}";
        var progressField = "${progressField!"inProgress"}";
        var postUrlFieldId = "${postUrl}";
        var progressUrlFieldId = "${progressUrl}";
        var startButtonId = "api-progress-startButton";
        var statusTextId = "api-progress-statusText";
        var progressInterval = null;

        document.getElementById(startButtonId).addEventListener("click", function() {
            var postField = document.querySelector('[name="' + postUrlFieldId + '"]');
            var postUrl = postField ? postField.value : "";

            var progressFieldEl = document.querySelector('[name="' + progressUrlFieldId + '"]');
            var progressUrl = progressFieldEl ? progressFieldEl.value : "";

            if (!postUrl || !progressUrl) {
                alert("Missing Jenkins URLs from form fields.");
                return;
            }

            document.getElementById(statusTextId).innerText = "Fetching Crumb...";
            document.getElementById(startButtonId).disabled = true;

            fetch(baseUrl + "/crumbIssuer/api/json", {
                method: "GET",
                headers: {
                    "Authorization": authorizationHeader
                }
            }).then(function(response) {
                if (!response.ok) throw new Error("Failed to fetch crumb");
                return response.json();
            }).then(function(crumbData) {
                var crumbField = crumbData.crumbRequestField;
                var crumbValue = crumbData.crumb;

                var headers = {
                    "Content-Type": "application/x-www-form-urlencoded",
                    "Authorization": authorizationHeader
                };
                headers[crumbField] = crumbValue;

                document.getElementById(statusTextId).innerText = "Triggering Build...";

                return fetch(postUrl, {
                    method: "POST",
                    headers: headers
                });
            }).then(function(response) {
                if (!response.ok) throw new Error("Failed to trigger build");
                document.getElementById(statusTextId).innerText = "Build triggered. Monitoring progress...";
                progressInterval = setInterval(function() {
                    checkProgress(progressUrl, 0);
                }, timeout * 1000);
            }).catch(function(error) {
                document.getElementById(statusTextId).innerText = "Error: " + error.message;
                document.getElementById(startButtonId).disabled = false;
            });
        });

        function checkProgress(progressUrl, retryCount) {
            fetch(progressUrl, {
                method: "GET",
                headers: {
                    "Authorization": authorizationHeader
                }
            }).then(function(response) {
                if (!response.ok) throw new Error("Failed to fetch progress");
                return response.json();
            }).then(function(data) {
                var inProgress = data[progressField];
                var result = data[resultField];

                if (inProgress) {
                    document.getElementById(statusTextId).innerText = "Build is running...";
                } else {
                    if (!result && retryCount < 3) {
                        document.getElementById(statusTextId).innerText = "Waiting for result...";
                        setTimeout(function() {
                            checkProgress(progressUrl, retryCount + 1);
                        }, 1000);
                        return;
                    }

                    clearInterval(progressInterval);
                    document.getElementById(startButtonId).disabled = false;

                    if (result === "SUCCESS") {
                        document.getElementById(statusTextId).innerText = "✅ Build completed successfully!";
                    } else if (result === "FAILURE") {
                        document.getElementById(statusTextId).innerText = "❌ Build failed.";
                    } else {
                        document.getElementById(statusTextId).innerText = "⚠️ Build finished with status: " + result;
                    }
                }
            }).catch(function(error) {
                clearInterval(progressInterval);
                document.getElementById(statusTextId).innerText = "Progress Error: " + error.message;
                document.getElementById(startButtonId).disabled = false;
            });
        }
    })();
    </script>
</div>
