<div class="form-cell">
    <style>
        .progress-container {
            margin-top: 20px;
            display: none;
        }
        .progress-bar-outer {
            background-color: #f0f0f0;
            border-radius: 5px;
            overflow: hidden;
        }
        .progress-bar {
            height: 20px;
            background: linear-gradient(to right, #4CAF50, #8BC34A);
            width: 0%%;
            transition: width 0.5s ease;
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
        /* Header help styles */
        .headers-help-toggle {
            color: #2a6496;
            cursor: pointer;
            font-size: 13px;
            margin: 10px 0;
            display: inline-block;
        }
        .headers-help-toggle:hover {
            text-decoration: underline;
        }
        .headers-help-content {
            display: none;
            margin-top: 8px;
            padding: 10px;
            background: #f8f9fa;
            border-radius: 4px;
            border: 1px solid #ddd;
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
        var elementId = "${elementId}";
        var postUrl = "${postUrl}";
        var progressUrl = "${progressUrl}";
        var processIdField = "${processIdField}";
        var progressFields = "${progressFields}";
        var statusField = "${statusField}";
        var headers = ${urlHeaders!"[]"};
        var pollInterval = parseInt("${pollInterval!5000}");

        // Toggle help visibility
        window.toggleHelp = function(link) {
            var content = link.nextElementSibling;
            if (content.style.display === "block") {
                content.style.display = "none";
                link.innerHTML = "▼ Show Headers Format Instructions";
            } else {
                content.style.display = "block";
                link.innerHTML = "▲ Hide Headers Format Instructions";
            }
        };

        var container = document.getElementById(elementId);
        if (!container) return;

        var button = container.querySelector("button");
        var progressContainer = container.querySelector(".progress-container");
        var progressBar = container.querySelector(".progress-bar");
        var statusText = container.querySelector(".status-text");

        function setStatus(message, type) {
            statusText.textContent = message;
            statusText.className = "status-text status-" + type;
        }

        function handleError(message) {
            button.disabled = false;
            setStatus(message, "error");
        }

        function complete() {
            setStatus("@@apiProgress.status.complete@@", "success");
            progressBar.style.width = "100%%";
            button.disabled = false;
        }

        function checkProgress(processId) {
            var xhr = new XMLHttpRequest();
            xhr.open("GET", progressUrl.replace("{processId}", encodeURIComponent(processId)));
            
            headers.forEach(function(header) {
                xhr.setRequestHeader(header.key, header.value);
            });
            
            xhr.onload = function() {
                if (xhr.status === 200) {
                    try {
                        var response = JSON.parse(xhr.responseText);
                        var progress = 0;
                        var fields = progressFields.split(",");
                        var validFields = 0;
                        
                        fields.forEach(function(field) {
                            var value = getJsonValue(response, field.trim());
                            if (typeof value === "number") {
                                progress += value;
                                validFields++;
                            }
                        });
                        
                        if (validFields > 0) {
                            progress = Math.min(100, Math.max(0, progress / validFields));
                            progressBar.style.width = progress + "%%";
                            setStatus(getJsonValue(response, statusField) || "@@apiProgress.status.processing@@", "processing");
                            
                            if (progress < 100) {
                                setTimeout(checkProgress, pollInterval, processId);
                            } else {
                                complete();
                            }
                        } else {
                            handleError("@@apiProgress.error.invalidResponse@@");
                        }
                    } catch (e) {
                        handleError("@@apiProgress.error.progressFailed@@");
                    }
                } else {
                    handleError("@@apiProgress.error.networkError@@ (" + xhr.status + ")");
                }
            };
            
            xhr.onerror = function() {
                handleError("@@apiProgress.error.networkError@@");
            };
            
            xhr.send();
        }

        function getJsonValue(obj, path) {
            return path.split("/").reduce(function(o, k) {
                return (o || {})[k];
            }, obj);
        }

        button.addEventListener("click", function() {
            button.disabled = true;
            progressContainer.style.display = "block";
            setStatus("@@apiProgress.status.starting@@", "processing");
            progressBar.style.width = "0%%";
            
            var xhr = new XMLHttpRequest();
            xhr.open("POST", postUrl);
            
            headers.forEach(function(header) {
                xhr.setRequestHeader(header.key, header.value);
            });
            
            xhr.onload = function() {
                if (xhr.status === 200) {
                    try {
                        var response = JSON.parse(xhr.responseText);
                        var processId = getJsonValue(response, processIdField);
                        if (processId) {
                            checkProgress(processId);
                        } else {
                            handleError("@@apiProgress.error.startFailed@@");
                        }
                    } catch (e) {
                        handleError("@@apiProgress.error.invalidResponse@@");
                    }
                } else {
                    handleError("@@apiProgress.error.startFailed@@ (" + xhr.status + ")");
                }
            };
            
            xhr.onerror = function() {
                handleError("@@apiProgress.error.networkError@@");
            };
            
            xhr.send();
        });
    })();
    </script>
</div>