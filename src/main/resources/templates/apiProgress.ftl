<div class="form-cell">
    <style>
        .progress-container {
            margin-top: 20px;
            display: none;
        }
        .status-text {
            margin-top: 8px;
            font-size: 14px;
            padding: 5px;
            border-radius: 4px;
        }
        .status-processing {
            color: #31708f;
            background-color: #d9edf7;
        }
        .status-error {
            color: #a94442;
            background-color: #f2dede;
        }
        .status-success {
            color: #3c763d;
            background-color: #dff0d8;
        }
    </style>

    <div class="api-progress-plugin" id="${elementId}">
        <button type="button" class="btn btn-primary">
            Start Process
        </button>
        
        <div class="progress-container">
            <div class="progress-bar-outer">
                <div class="progress-bar"></div>
            </div>
            <div class="status-text"></div>
        </div>
    </div>

    <script type="text/javascript">
    (function() {
        // Configuration - unchanged from your original
        var elementId = "${elementId}";
        var postUrlFieldId = "${postUrlFieldId}";
        var progressUrlFieldId = "${progressUrlFieldId}";
        var progressMethod = "${progressMethod}";
        var inProgressField = "${inProgressField}";
        var resultField = "${resultField}";
        var headers = ${urlHeaders!"[]"};
        var pollInterval = parseInt("${pollInterval!5000}");

        // DOM Elements - unchanged
        var container = document.getElementById(elementId);
        if (!container) return;

        var button = container.querySelector("button");
        var progressContainer = container.querySelector(".progress-container");
        var statusText = container.querySelector(".status-text");

        // Helper Functions - only minor safety improvements
        function setStatus(message, type) {
            statusText.textContent = message;
            statusText.className = "status-text status-" + type;
        }

        function handleError(message) {
            button.disabled = false;
            setStatus(message, "error");
            console.error(message);
        }

        function getJsonValue(obj, path) {
            try {
                return path.split('.').reduce(function(o, k) {
                    return (o || {})[k];
                }, obj);
            } catch (e) {
                console.error("JSON path error:", e);
                return null;
            }
        }

        // MAIN CHANGE: Improved request handling with CORS support
        function makeRequest(url, method, callback) {
            button.disabled = true;
            progressContainer.style.display = "block";
            setStatus(method === "POST" ? "Starting process..." : "Checking progress...", "processing");

            // Convert headers array to object
            var headersObj = {};
            headers.forEach(function(header) {
                if (header.key && header.value) {
                    headersObj[header.key] = header.value;
                }
            });

            // Add CORS headers
            headersObj['X-Requested-With'] = 'XMLHttpRequest';
            
            fetch(url, {
                method: method,
                headers: headersObj,
                credentials: 'include' // Important for CORS with auth
            })
            .then(function(response) {
                if (!response.ok) throw new Error(response.statusText);
                return response.json();
            })
            .then(function(data) {
                callback(null, data);
            })
            .catch(function(error) {
                callback(error);
            });
        }

        // Event Listeners - simplified
        button.addEventListener("click", function() {
            var postUrl = document.querySelector('[name="' + postUrlFieldId + '"]').value;
            if (!postUrl) {
                handleError("POST URL is required");
                return;
            }

            makeRequest(postUrl, "POST", function(error, data) {
                if (error) {
                    handleError("Failed to start: " + error.message);
                } else {
                    setTimeout(pollProgress, pollInterval);
                }
            });
        });

        function pollProgress() {
            var progressUrl = document.querySelector('[name="' + progressUrlFieldId + '"]').value;
            if (!progressUrl) {
                handleError("Progress URL is required");
                return;
            }

            makeRequest(progressUrl, "GET", function(error, data) {
                if (error) {
                    handleError("Progress check failed: " + error.message);
                } else {
                    var isInProgress = getJsonValue(data, inProgressField);
                    var result = getJsonValue(data, resultField);

                    if (isInProgress) {
                        setTimeout(pollProgress, pollInterval);
                    } else {
                        button.disabled = false;
                        setStatus("Result: " + result, result === "SUCCESS" ? "success" : "error");
                    }
                }
            });
        }

        // [Keep your existing toggleHelp function if needed]
    })();
    </script>
</div>