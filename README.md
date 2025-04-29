# Button API Plugin (Joget DX8) - Jenkins Build Integration

This plugin provides a **Form Element** in Joget DX8 Community Edition that allows you to:
- **Trigger a Jenkins job** using `buildWithParameters` API via POST
- **Monitor Jenkins build progress** via periodic GET calls
- **Handle CSRF Crumb**, Authorization, timeout, and field mappings properly
- Display build status directly inside the Joget Form UI.

---

## Features

- **Start Jenkins Build** from Joget form button
- **Auto-monitor build status** (`result` and `inProgress`)
- **Handles Jenkins Crumb** fetching automatically
- **Dynamic configuration** via Property Panel
- **Authorization support** (Basic Auth header)
- **Timeout control** for API requests
- **Fully i18n-ready** (multi-language labels and messages)

---

## Requirements

| Item | Minimum Version |
|:---|:---|
| Joget Platform | DX8.1.12 (Community Edition or Enterprise) |
| Java Version | Java 8 or Java 11 |
| Jenkins Server | 2.x series recommended |

---

## Installation

1. Build the plugin:

    ```bash
    mvn clean install
    ```

2. Upload the generated `.jar` file (from `target/`) into:
    - Joget App Center → Admin → Settings → Manage Plugins → Upload

3. Restart Joget server if needed.

4. You will find the new element **Button API (Jenkins)** in Form Builder under **Advanced** category.

---

## Configuration

Inside **Form Builder → Button API (Jenkins)** element → Property Panel:

| Property Name | Description | Required | Validation | Default |
|:---|:---|:---|:---|:---|
| **Base URL** | Full Jenkins server URL (e.g., `http://jenkins.local:8080`) | ✅ Yes | URL | - |
| **POST URL** | Relative path for build trigger (e.g., `/job/myjob/buildWithParameters`) | ✅ Yes | URL | - |
| **Progress URL** | Relative path to check build status (e.g., `/job/myjob/lastBuild/api/json`) | ✅ Yes | URL | - |
| **Authorization Header** | Base64 encoded `username:token` (e.g., `Basic dXNlcm5hbWU6YXBpdG9rZW4=`) | ✅ Yes | Required | - |
| **Result Field** | JSON field to read build result (usually `result`) | ✅ Yes | Required | `result` |
| **Progress Field** | JSON field to read build running state (usually `building`) | ✅ Yes | Required | `building` |
| **Timeout (Seconds)** | Timeout for API calls to Jenkins | ⬜ Optional | Integer | `30` |

---

## Example Setup

| Example | Value |
|:---|:---|
| Base URL | `http://jenkins.example.com` |
| POST URL | `/job/ProjectX/buildWithParameters` |
| Progress URL | `/job/ProjectX/lastBuild/api/json` |
| Authorization Header | `Basic dXNlcjp0b2tlbjEyMzQ=` |
| Result Field | `result` |
| Progress Field | `building` |
| Timeout | `30` |

---

## How It Works

1. **User clicks "Start Process"** button.
2. Plugin sends **POST** to Jenkins to trigger the build.
3. Plugin starts **periodic GET** to Jenkins to check build status.
4. Displays **live status**:
    - "Build is running..."
    - "Build completed successfully!"
    - "Build failed."

5. If build is successful, button re-enables automatically.

---

## License

This plugin is released as open-source, under MIT License.

(You can modify or update license based on your company/needs.)

---

## Support

For any issues, please contact the original plugin author, or create GitHub issues if available.
# Button API Plugin (Joget DX8) - Jenkins Build Integration

This plugin provides a **Form Element** in Joget DX8 Community Edition that allows you to:
- **Trigger a Jenkins job** using `buildWithParameters` API via POST
- **Monitor Jenkins build progress** via periodic GET calls
- **Handle CSRF Crumb**, Authorization, timeout, and field mappings properly
- Display build status directly inside the Joget Form UI.

---

## Features

- **Start Jenkins Build** from Joget form button
- **Auto-monitor build status** (`result` and `inProgress`)
- **Handles Jenkins Crumb** fetching automatically
- **Dynamic configuration** via Property Panel
- **Authorization support** (Basic Auth header)
- **Timeout control** for API requests
- **Fully i18n-ready** (multi-language labels and messages)

---

## Requirements

| Item | Minimum Version |
|:---|:---|
| Joget Platform | DX8.1.12 (Community Edition or Enterprise) |
| Java Version | Java 8 or Java 11 |
| Jenkins Server | 2.x series recommended |

---

## Installation

1. Build the plugin:

    ```bash
    mvn clean install
    ```

2. Upload the generated `.jar` file (from `target/`) into:
    - Joget App Center → Admin → Settings → Manage Plugins → Upload

3. Restart Joget server if needed.

4. You will find the new element **Button API (Jenkins)** in Form Builder under **Advanced** category.

---

## Configuration

Inside **Form Builder → Button API (Jenkins)** element → Property Panel:

| Property Name | Description | Required | Validation | Default |
|:---|:---|:---|:---|:---|
| **Base URL** | Full Jenkins server URL (e.g., `http://jenkins.local:8080`) | ✅ Yes | URL | - |
| **POST URL** | Relative path for build trigger (e.g., `/job/myjob/buildWithParameters`) | ✅ Yes | URL | - |
| **Progress URL** | Relative path to check build status (e.g., `/job/myjob/lastBuild/api/json`) | ✅ Yes | URL | - |
| **Authorization Header** | Base64 encoded `username:token` (e.g., `Basic dXNlcm5hbWU6YXBpdG9rZW4=`) | ✅ Yes | Required | - |
| **Result Field** | JSON field to read build result (usually `result`) | ✅ Yes | Required | `result` |
| **Progress Field** | JSON field to read build running state (usually `building`) | ✅ Yes | Required | `building` |
| **Timeout (Seconds)** | Timeout for API calls to Jenkins | ⬜ Optional | Integer | `30` |

---

## Example Setup

| Example | Value |
|:---|:---|
| Base URL | `http://jenkins.example.com` |
| POST URL | `/job/ProjectX/buildWithParameters` |
| Progress URL | `/job/ProjectX/lastBuild/api/json` |
| Authorization Header | `Basic dXNlcjp0b2tlbjEyMzQ=` |
| Result Field | `result` |
| Progress Field | `building` |
| Timeout | `30` |

---

## How It Works

1. **User clicks "Start Process"** button.
2. Plugin sends **POST** to Jenkins to trigger the build.
3. Plugin starts **periodic GET** to Jenkins to check build status.
4. Displays **live status**:
    - "Build is running..."
    - "Build completed successfully!"
    - "Build failed."

5. If build is successful, button re-enables automatically.

---

## License

This plugin is released as open-source, under MIT License.

(You can modify or update license based on your company/needs.)

---

## Support

For any issues, please contact the original plugin author, or create GitHub issues if available.
