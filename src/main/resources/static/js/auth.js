// src/main/resources/static/js/auth.js

document.addEventListener('DOMContentLoaded', function() {
    // Base API URLs
    const API_BASE_URL = '/api';
    const AUTH_API_BASE_URL = `${API_BASE_URL}/auth`;

    // DOM elements
    const loginLink = document.getElementById('loginLink');
    const registerLink = document.getElementById('registerLink');
    const homeLoginBtn = document.getElementById('homeLoginBtn');
    const homeRegisterBtn = document.getElementById('homeRegisterBtn');
    const goToLoginLink = document.getElementById('goToLoginLink');
    const goToRegisterLink = document.getElementById('goToRegisterLink');
    const goToLoginAfterRegister = document.getElementById('goToLoginAfterRegister');
    const logoutBtn = document.getElementById('logoutBtn');
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');
    const loginErrorEl = document.getElementById('loginError');
    const registerErrorEl = document.getElementById('registerError');
    const registerSuccessEl = document.getElementById('registerSuccess');
    const navbarUsernameSpan = document.getElementById('navbarUsername');

    // Section elements
    const homeSection = document.getElementById('homeSection');
    const loginSection = document.getElementById('loginSection');
    const registerSection = document.getElementById('registerSection');
    const kitchensinkSection = document.getElementById('kitchensinkSection');
    const adminSection = document.getElementById('adminSection');

    // --- Token and User Storage Helper Functions ---
    // These are defined and used ONLY within auth.js, but EXPOSED globally via window.
    const ACCESS_TOKEN_STORAGE_KEY = 'accessToken';
    const REFRESH_TOKEN_STORAGE_KEY = 'refreshToken';
    const CURRENT_USER_STORAGE_KEY = 'currentUser';

    const getAccessToken = () => localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY);
    const getRefreshToken = () => localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);

    // Stores both tokens in Local Storage
    const setTokens = (accessToken, refreshToken) => {
        console.log("[auth.js] Storing tokens.");
        localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, accessToken);
        localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, refreshToken);
    };

    // Removes all tokens and user info from Local Storage
    const removeTokens = () => {
        console.log("[auth.js] Removing tokens and user info.");
        localStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY);
        localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
        localStorage.removeItem(CURRENT_USER_STORAGE_KEY);
         // Also clear table content (optional, but good practice on logout)
         const contactsList = document.getElementById('contactsList');
         const usersList = document.getElementById('usersList');
         const adminContactsList = document.getElementById('adminContactsList');
         if(contactsList) contactsList.innerHTML = '';
         if(usersList) usersList.innerHTML = '';
         if(adminContactsList) adminContactsList.innerHTML = '';
    };

    // Retrieves current user info (username, roles) from Local Storage
    const getCurrentUser = () => {
        const userJson = localStorage.getItem(CURRENT_USER_STORAGE_KEY);
        return userJson ? JSON.parse(userJson) : null;
    };

    // Stores current user info (username, roles) in Local Storage and updates navbar
    const setCurrentUser = (username, roles) => {
         console.log("[auth.js] Storing current user info:", { username, roles });
         localStorage.setItem(CURRENT_USER_STORAGE_KEY, JSON.stringify({ username, roles }));
         if (navbarUsernameSpan) {
             navbarUsernameSpan.textContent = username || ''; // Set username in navbar
         }
    };

     // Helper function to log out and redirect (calls backend to invalidate refresh token)
     const logoutAndRedirect = async () => {
         console.log("[auth.js] Logging out and redirecting...");
         const refreshToken = getRefreshToken();
         // Call backend logout endpoint to invalidate the refresh token server-side
         if (refreshToken) {
             try {
                 console.log("[auth.js] Calling backend logout endpoint.");
                 await fetch(`${AUTH_API_BASE_URL}/logout`, {
                      method: 'POST',
                      headers: { 'Content-Type': 'application/json' },
                      body: JSON.stringify({ refreshToken: refreshToken })
                 });
                 console.log("[auth.js] Backend logout call completed.");
             } catch (error) {
                  // Log error but don't block frontend logout if backend call fails
                  console.error("Error during backend logout call:", error);
             }
         } else {
             console.warn("[auth.js] No refresh token found for backend logout call.");
         }
         // Perform frontend logout steps
         removeTokens(); // Clear tokens and user info from storage
         updateUIForUnauthenticatedUser(); // Update UI elements
         showLogin(); // Navigate to login section
     };


    // --- Authenticated Fetch Helper Function (Handles Token Refresh) ---
    const fetchWithAuth = async (url, options = {}, retry = false) => {
        console.log(`[FETCH] Using fetchWithAuth for: ${options.method || 'GET'} ${url} (Retry: ${retry})`);
        const accessToken = getAccessToken(); // Get the current access token

        // Initial check: If no access token is present and it's not a retry, user might be logged out
        // However, the 401 handling below is the primary mechanism for expired tokens.
        if (!accessToken && !retry) {
             console.warn("[FETCH] No access token found for initial request.");
             // If tokens were somehow cleared, ensure full logout flow
             logoutAndRedirect();
             throw new Error("Authentication required."); // Stop the fetch
        }

        // Set Authorization header for the initial request
        const headers = {
            ...options.headers, // Copy any original headers provided
            'Authorization': `Bearer ${accessToken}` // Add/overwrite Authorization header
        };

         // Ensure Content-Type is application/json if sending a body (POST, PUT, PATCH) and not already set
         if (options.body && options.method && ['POST', 'PUT', 'PATCH'].includes(options.method.toUpperCase())) {
             if (!headers['Content-Type'] && !headers['content-type']) { // Check for both lowercase/uppercase
                 headers['Content-Type'] = 'application/json';
             }
         }

        let response;
        try {
            // ===> Make the initial fetch request <===
            response = await fetch(url, { ...options, headers });

            // ===> Check for 401 Unauthorized - This happens if the access token is expired or invalid <===
            if (response.status === 401 && !retry) {
                console.warn("[FETCH] Received 401 Unauthorized. Attempting to refresh token...");
                const refreshToken = getRefreshToken(); // Get the refresh token

                // Check if a refresh token is available
                if (refreshToken) {
                    console.log("[FETCH] Refresh token found. Proceeding to refresh attempt.");
                    try { // <--- Inner try block for the refresh token fetch
                        // Call the backend's refresh token endpoint
                        const refreshResponse = await fetch(`${AUTH_API_BASE_URL}/refresh-token`, {
                             method: 'POST',
                             headers: {
                                'Content-Type': 'application/json' // Refresh endpoint expects JSON
                             },
                             body: JSON.stringify({ refreshToken: refreshToken }) // Send the refresh token in the body
                        });

                        console.log(`[FETCH] Refresh token response status: ${refreshResponse.status}`);

                        // Check if the refresh token request was successful (HTTP 2xx status)
                        if (refreshResponse.ok) {
                            const tokenPair = await refreshResponse.json(); // Get the new token pair (access + refresh + user info)
                            console.log("[FETCH] Token refreshed successfully. Storing new tokens and user info.");
                            setTokens(tokenPair.accessToken, tokenPair.refreshToken); // Store the new tokens
                             // Store updated user info received from the refresh endpoint (ID, username, roles)
                            setCurrentUser(tokenPair.username, tokenPair.roles);

                            // ** Retry the original failed request with the new access token **
                            console.log(`[FETCH] Retrying original request: ${options.method || 'GET'} ${url} with new access token.`);
                            // Build headers for the retry request - ensure Content-Type and Authorization are correct
                            const retryHeaders = { ...options.headers }; // Start by copying original headers

                            // Ensure Content-Type is application/json if sending a body and it wasn't already set
                            if (options.body && options.method && ['POST', 'PUT', 'PATCH'].includes(options.method.toUpperCase())) {
                                 if (!retryHeaders['Content-Type'] && !retryHeaders['content-type']) {
                                      retryHeaders['Content-Type'] = 'application/json';
                                 }
                             }

                            // Always update/add the Authorization header with the NEW access token
                             retryHeaders['Authorization'] = `Bearer ${tokenPair.accessToken}`;


                            const retryResponse = await fetch(url, { ...options, headers: retryHeaders });

                            console.log(`[FETCH] Received retry response for ${url}: Status ${retryResponse.status}`);

                            // Check if the retry request was successful
                            if (!retryResponse.ok) {
                                console.error("[FETCH] Original request retry failed after token refresh.");
                                // If retry also fails, treat this as an overall request failure
                                // Throw the retry response so the main error handling block below can process it.
                                throw new Error(`Retry request failed with status ${retryResponse.status}`); // Or throw the retryResponse object
                            } else {
                                // If the retry succeeds, return its result (don't process the initial 401 response)
                                console.log("[FETCH] Original request retry succeeded. Returning retry response.");
                                // Handle successful response body from the retry
                                if (retryResponse.status === 204 || retryResponse.headers.get("content-length") === "0") {
                                     console.log("[FETCH] Retry Success: 204 No Content or Empty Body.");
                                     return null;
                                 }
                                 const retryContentType = retryResponse.headers.get("content-type");
                                 if (retryContentType && retryContentType.includes("application/json")) {
                                     console.log("[FETCH] Retry Success: Parsing response as JSON.");
                                     return retryResponse.json();
                                 } else {
                                      console.log("[FETCH] Retry Success: Reading response as text.");
                                     return retryResponse.text();
                                 }
                            }

                        } else { // Refresh token endpoint returned an error (e.g., refresh token expired or invalid)
                            console.error("[FETCH] Refresh token request failed. Status:", refreshResponse.status);
                            // Attempt to read error body from refresh response
                            const refreshErrorBody = await refreshResponse.text().catch(() => "No refresh error body available");
                            console.error("[FETCH] Refresh token error response body:", refreshErrorBody);
                            // Since refresh failed, the user's session is no longer valid.
                            logoutAndRedirect();
                            // Throw an error to stop further execution and signal failure
                            throw new Error(`Token refresh failed with status ${refreshResponse.status}`);
                        }

                    } catch (refreshFetchError) { // <--- Catch block for the INNER refresh token fetch
                        // This catches network errors or exceptions during the fetch call to the refresh endpoint itself
                        console.error("[FETCH] Exception during refresh token fetch attempt:", refreshFetchError);
                         // Since the refresh fetch failed due to an exception, the user's session is likely invalid.
                         throw new Error("Network or unexpected error during token refresh attempt.");
                        logoutAndRedirect();
                         // Re-throw a new error to signal the overall failure to the outer catch or caller

                    }

                } else { // No refresh token found in storage
                    console.warn("[FETCH] No refresh token available (value is null or empty). Proceeding to logout.");
                    logoutAndRedirect(); // Perform logout since session cannot be refreshed
                     // Throw an error to signal failure
                    throw new Error("No refresh token available.");
                }
                 // If the code reaches here after the 401 block, it means the refresh failed or wasn't attempted.
                 // The main error handling below will process the original 401 response if no new tokens were obtained.
            }

            // ===> Handle non-OK responses (including the original 401 if refresh failed/was skipped,
            //      or any other non-2xx status from the initial or retried request) <===
            if (!response.ok) {
                console.warn(`[FETCH] Request failed with non-OK status: ${response.status}`);
                // --- Standard Error Handling ---
                let errorText = "No response body provided";
                try {
                     // Clone response body stream before reading
                     const errorResponseClone = response.clone();
                     errorText = await errorResponseClone.text(); // Read response body as text
                     console.log("[FETCH] Error Response Body (Text):", errorText);
                } catch (e) {
                     console.warn("[FETCH] Could not read error response body as text:", e);
                }

                let errorJson = null;
                try {
                    // Attempt to parse the text body as JSON if Content-Type indicates JSON
                    const contentType = response.headers.get("content-type");
                     if (contentType && contentType.includes("application/json") && errorText && errorText.trim().startsWith('{')) {
                         errorJson = JSON.parse(errorText);
                         console.log("[FETCH] Error Response Body (JSON):", errorJson);
                     }
                } catch (e) {
                     console.warn("[FETCH] Could not parse error body text as JSON:", e);
                }

                // Construct a meaningful error message
                const errorMessage = errorJson?.message || errorJson?.error || errorText || `HTTP error! status: ${response.status}`;
                const errorToThrow = new Error(errorMessage);
                errorToThrow.status = response.status; // Attach the HTTP status to the error object
                // Throw the structured error
                throw errorToThrow;

            } else {
                // ===> Success path for the initial request (if not 401) <===
                 console.log("[FETCH] Initial request successful (not 401).");
                // Handle successful response body
                 // Check for 204 No Content or empty body explicitly
                if (response.status === 204 || response.headers.get("content-length") === "0") {
                     console.log("[FETCH] Success: 204 No Content or Empty Body.");
                     return null; // Return null for no content
                 }
                 // Check Content-Type to decide how to parse the body
                 const contentType = response.headers.get("content-type");
                 if (contentType && contentType.includes("application/json")) {
                     console.log("[FETCH] Success: Parsing response as JSON.");
                     return response.json(); // Parse as JSON
                 } else {
                     console.log("[FETCH] Success: Reading response as text (non-JSON).");
                     return response.text(); // Read as text for other types
                 }
            }

        } catch (error) { // <--- The main catch block for any errors during the entire process
            // This catches network errors during the initial fetch, errors thrown from within the try block,
            // or errors thrown from the inner refresh fetch/handling that weren't caught there.
            console.error(`[FETCH] Global catch block caught error for ${url}:`, error);
            // Re-throw the error so the calling function (like updateContact, loadUsers, etc.) can handle it in its own catch block.
            throw error;
        }
    };


    // --- Event Listeners ---
    // Check if elements exist before adding listeners to prevent errors if HTML is missing
    if(loginLink) loginLink.addEventListener('click', function(e) { e.preventDefault(); showLogin(); });
    if(registerLink) registerLink.addEventListener('click', function(e) { e.preventDefault(); showRegister(); });
    if(homeLoginBtn) homeLoginBtn.addEventListener('click', function() { showLogin(); });
    if(homeRegisterBtn) homeRegisterBtn.addEventListener('click', function() { showRegister(); });
    if(goToLoginLink) goToLoginLink.addEventListener('click', function(e) { e.preventDefault(); showLogin(); });
    if(goToRegisterLink) goToRegisterLink.addEventListener('click', function(e) { e.preventDefault(); showRegister(); });
    if(goToLoginAfterRegister) goToLoginAfterRegister.addEventListener('click', function(e) { e.preventDefault(); showLogin(); }); // Calls showLogin
    if(logoutBtn) logoutBtn.addEventListener('click', function(e) { e.preventDefault(); logout(); });

     // Listeners for navigation links in the navbar (assuming IDs adminLink and kitchensinkLink)
    const adminLink = document.getElementById('adminLink');
     if(adminLink) adminLink.addEventListener('click', function(e) { e.preventDefault(); showAdmin(); });

     const kitchensinkLink = document.getElementById('kitchensinkLink');
     if(kitchensinkLink) kitchensinkLink.addEventListener('click', function(e) { e.preventDefault(); showKitchenSink(); });


    // Form submissions
    if(loginForm) {
        loginForm.addEventListener('submit', function(e) {
            e.preventDefault(); // Prevent default form submission
            login(); // Call login function
        });
    } else { console.warn("[auth.js] Login form element not found!"); }

    if(registerForm) {
        registerForm.addEventListener('submit', function(e) {
            e.preventDefault(); // Prevent default form submission
            register(); // Call register function
        });
    } else { console.warn("[auth.js] Register form element not found!"); }


    // --- Initialize Authentication State on Page Load ---
    // Call this function after all elements and listeners are set up and shared functions are defined.
    initializeAuth();


    // --- Functions ---

    // Initializes the authentication state and updates UI on page load
    function initializeAuth() {
        console.log("[auth.js] Initializing auth state based on stored tokens.");
        const accessToken = getAccessToken(); // Get access token from storage
        const user = getCurrentUser(); // Get user info from storage

        // Check if both tokens and user info are present and seem valid (basic check)
        if (accessToken && user && user.username && user.roles && Array.isArray(user.roles)) {
            console.log("[auth.js] Access token and user info found. User appears authenticated.");
            updateUIForAuthenticatedUser(); // Update UI for logged-in state

            // ===> Add a small delay before showing sections and loading data <===
            // This gives other scripts (kitchensink.js, admin.js) a moment to finish their DOMContentLoaded logic
            // and expose their load functions globally.
            // If timing issues persist, you might need to adjust this delay or revisit script loading/execution order.
            setTimeout(() => {
                console.log("[auth.js] Delayed initialization logic executing.");
                // Check user roles to determine which section to show initially
                if (user.roles.includes('ROLE_ADMIN')) {
                    console.log("[auth.js] User has ADMIN role. Showing Admin section.");
                     // Check if the admin load function is globally available before calling
                    if (typeof window.loadAdminData === 'function') {
                         window.loadAdminData(); // Call function exposed by admin.js
                         showAdmin(); // Navigate to the admin section
                     } else {
                         console.warn("[auth.js] window.loadAdminData function not available globally after delay. Showing Kitchen Sink fallback.");
                         // Fallback: if admin load function isn't ready, show kitchen sink
                         showKitchenSink(); // Use showKitchenSink which also checks its load function
                     }
                } else { // Assuming any non-admin role gets the standard user view
                    console.log("[auth.js] User has USER role. Showing Kitchen Sink section.");
                    // Check if the user load function is globally available before calling
                    if (typeof window.loadUserContacts === 'function') {
                        window.loadUserContacts(); // Call function exposed by kitchensink.js
                        showKitchenSink(); // Navigate to the kitchen sink section
                     } else {
                         console.warn("[auth.js] window.loadUserContacts function not available globally after delay. Hiding sections.");
                         // Fallback: if user load function isn't ready, hide all sections
                         showSection(null);
                     }
                }
            }, 100); // 100 milliseconds delay

        } else {
            // No valid tokens or user info found, treat as unauthenticated
            console.log("[auth.js] No valid authentication info found. User is unauthenticated.");
            removeTokens(); // Ensure any leftover tokens are cleared
            updateUIForUnauthenticatedUser(); // Update UI for logged-out state
            showLogin(); // Navigate to the login section
        }
        console.log("[auth.js] Initialization complete (sync part).");
    }

    // Updates the visibility of UI elements based on authentication status
    function updateUIForAuthenticatedUser() {
        // Elements visible only when authenticated
        document.querySelectorAll('.auth-required').forEach(elem => { if(elem) elem.style.display = 'block'; });
        // Elements visible only when NOT authenticated
        document.querySelectorAll('.auth-not-required').forEach(elem => { if(elem) elem.style.display = 'none'; });

        // Check for admin role to show admin-specific elements
        const user = getCurrentUser();
        if (user && user.roles && Array.isArray(user.roles) && user.roles.includes('ROLE_ADMIN')) {
            document.querySelectorAll('.admin-only').forEach(elem => { if(elem) elem.style.display = 'block'; });
        } else {
            document.querySelectorAll('.admin-only').forEach(elem => { if(elem) elem.style.display = 'none'; });
        }
         // Update username in navbar
         if (navbarUsernameSpan && user) {
             navbarUsernameSpan.textContent = user.username || '';
         }
    }

    // Updates the visibility of UI elements for the unauthenticated state
    function updateUIForUnauthenticatedUser() {
        // Elements visible only when authenticated
        document.querySelectorAll('.auth-required').forEach(elem => { if(elem) elem.style.display = 'none'; });
        // Elements visible only when NOT authenticated
        document.querySelectorAll('.auth-not-required').forEach(elem => { if(elem) elem.style.display = 'block'; });
        // Admin-only elements are always hidden when unauthenticated
        document.querySelectorAll('.admin-only').forEach(elem => { if(elem) elem.style.display = 'none'; });

         // Clear username in navbar
         if (navbarUsernameSpan) {
             navbarUsernameSpan.textContent = '';
         }
         // Note: Table content clearing is handled in removeTokens() now.
    }

    // Controls which main section of the page is visible
    function showSection(sectionElement) {
        const sections = [homeSection, loginSection, registerSection, kitchensinkSection, adminSection];
        sections.forEach(sec => {
            if(sec) sec.style.display = 'none'; // Hide all sections
        });
        // Show the specified section if it's a valid element
        if (sectionElement) {
           sectionElement.style.display = 'block';
        }
    }

    // Navigation functions to show specific sections
    function showHome() { showSection(homeSection); }
    function showLogin() {
        console.log("[auth.js] >> showLogin function executed <<");
        showSection(loginSection);
        // Reset login form and clear previous errors/success messages
        if(loginErrorEl) loginErrorEl.style.display = 'none';
        if(loginForm) loginForm.reset();
    }
    function showRegister() {
        console.log("[auth.js] >> showRegister function executed <<");
        showSection(registerSection);

         // Reset register form and clear previous errors/success messages
        if(registerErrorEl) registerErrorEl.style.display = 'none';
        if(registerSuccessEl) registerSuccessEl.style.display = 'none';
        if(registerForm) registerForm.reset();
    }

    // Navigation functions that show sections and trigger external data loading
    function showKitchenSink() {
        console.log("[auth.js] >> showKitchenSink function executed <<");
        showSection(kitchensinkSection); // Show the Kitchen Sink section
        // Check if the data loading function is globally available before calling it
        if (typeof window.loadUserContacts === 'function') {
            window.loadUserContacts(); // Call the function exposed by kitchensink.js
        } else {
            console.warn("[auth.js] window.loadUserContacts function not available globally.");
             // Optional: display a message in the kitchen sink section or handle error
        }
    }

    function showAdmin() {
        console.log("[auth.js] >> showAdmin function executed <<");
        showSection(adminSection); // Show the Admin section
         // Check if the data loading function is globally available before calling it
        if (typeof window.loadAdminData === 'function') {
             window.loadAdminData(); // Call the function exposed by admin.js
        } else {
            console.warn("[auth.js] window.loadAdminData function not available globally.");
             // Optional: display a message in the admin section or handle error
        }
    }

    // --- Authentication API Calls ---
    // Use standard fetch for login/register as they don't require an existing access token initially

    async function login() {
        console.log("[auth.js] Attempting login...");
        // Get input values from the login form
        const usernameInput = document.getElementById('loginUsername');
        const passwordInput = document.getElementById('loginPassword');

        const username = usernameInput ? usernameInput.value.trim() : ''; // Added trim()
        const password = passwordInput ? passwordInput.value : ''; // Don't trim password

        // Clear previous error message
        if(loginErrorEl) loginErrorEl.style.display = 'none';

        // Basic frontend validation
        if (!username || !password) {
            console.warn("[auth.js] Login validation failed: Username and password required.");
            if(loginErrorEl) { loginErrorEl.textContent = 'Username and password are required.'; loginErrorEl.style.display = 'block'; }
             // Add Bootstrap is-invalid classes if needed
             if(usernameInput) usernameInput.classList.add('is-invalid');
             if(passwordInput) passwordInput.classList.add('is-invalid');
            return;
        } else {
            // Clear is-invalid classes if validation passes
             if(usernameInput) usernameInput.classList.remove('is-invalid');
             if(passwordInput) passwordInput.classList.remove('is-invalid');
        }


        try {
            console.log("[auth.js] Calling backend login endpoint.");
            // Send login credentials to the backend
            const response = await fetch(`${AUTH_API_BASE_URL}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password }),
            });

            // Check if the response status is OK (2xx)
            if (!response.ok) {
                 // Attempt to read error details from the response body
                 const errorData = await response.json().catch(() => null) || await response.text().catch(() => `Login failed with status: ${response.status}`);
                 const errorMessage = errorData?.message || errorData?.error || `Login failed with status: ${response.status}`;
                 throw new Error(errorMessage); // Throw an error with a meaningful message
            }

            // Parse the successful response body (assuming JwtTokenPairResponse)
            const data = await response.json();
            console.log("[auth.js] Login successful. Received tokens and user info.", data);

            // Store the received tokens (access and refresh)
            setTokens(data.accessToken, data.refreshToken);

             // Store received user info (ID, username, roles)
             // Assuming login response *always* includes username and roles
            if (data.username && data.roles && Array.isArray(data.roles)) {
                setCurrentUser(data.username, data.roles);
            } else {
                 console.warn("[auth.js] Login response did not include expected username or roles format.");
                 // Fallback/Error handling if backend doesn't return expected user data
                 // You might need to fetch user info separately or handle this case.
                 // For now, assume a basic user role if none provided.
                 setCurrentUser(data.username || "Authenticated User", data.roles || ["ROLE_USER"]);
            }


            // Update UI for the authenticated user state
            updateUIForAuthenticatedUser();

            // Navigate to the appropriate section based on user roles
            const user = getCurrentUser(); // Get the user info just stored
            if (user && user.roles && Array.isArray(user.roles) && user.roles.includes('ROLE_ADMIN')) {
                 showAdmin(); // Show admin section if user has ADMIN role
            } else {
                 showKitchenSink(); // Show kitchen sink for other roles (or default)
            }

        } catch (error) {
            // Handle errors during the fetch call or backend response
            console.error("[auth.js] Login failed:", error);
            // Display error message to the user
            if(loginErrorEl) { loginErrorEl.textContent = 'Login failed: ' + error.message; loginErrorEl.style.display = 'block'; }

            // Add Bootstrap is-invalid classes to indicate failure
            if(usernameInput) usernameInput.classList.add('is-invalid');
            if(passwordInput) passwordInput.classList.add('is-invalid');
        }
    }

async function register() {
    console.log("[auth.js] Attempting registration...");
    // Get input values and error/success elements (Ensure these are correctly referenced)
    const usernameInput = document.getElementById('registerUsername');
    const emailInput = document.getElementById('registerEmail');
    const passwordInput = document.getElementById('registerPassword');
    const confirmPasswordInput = document.getElementById('registerConfirmPassword');

    const registerErrorEl = document.getElementById('registerError'); // Element to display general errors
    const registerSuccessEl = document.getElementById('registerSuccess'); // Element to display success message

    // Get elements for field-specific feedback (Assuming IDs like usernameFeedback, etc. - or using nextElementSibling)
    const usernameFeedbackEl = usernameInput ? usernameInput.nextElementSibling : null; // Assuming feedback is the next sibling div with class invalid-feedback
    const emailFeedbackEl = emailInput ? emailInput.nextElementSibling : null;
    const passwordFeedbackEl = passwordInput ? passwordInput.nextElementSibling : null;
    const confirmPasswordFeedbackEl = confirmPasswordInput ? confirmPasswordInput.nextElementSibling : null;


    // Clear previous error/success messages and validation feedback from *all* fields
    if(registerErrorEl) registerErrorEl.style.display = 'none';
    if(registerSuccessEl) registerSuccessEl.style.display = 'none';
    clearRegisterFormValidation(); // Call helper function to clear all previous validation states


    // Get trimmed values from inputs (check if elements exist)
    const username = usernameInput ? usernameInput.value.trim() : '';
    const email = emailInput ? emailInput.value.trim() : '';
    const password = passwordInput ? passwordInput.value : ''; // Don't trim password
    const confirmPassword = confirmPasswordInput ? confirmPasswordInput.value : '';


    // --- Frontend Validation Logic ---
    let formIsValid = true;

    // Username Validation: Not Blank, Size 8-30, Alphanumeric Only
    if (!username) {
        formIsValid = false;
        if(usernameInput) usernameInput.classList.add('is-invalid');
        if(usernameFeedbackEl) usernameFeedbackEl.textContent = 'Username is required.';
    } else if (username.length < 8 || username.length > 30) { // Adjust size limits if backend is different
        formIsValid = false;
        if(usernameInput) usernameInput.classList.add('is-invalid');
        if(usernameFeedbackEl) usernameFeedbackEl.textContent = 'Username must be between 8 and 30 characters.';
    } else if (!/^[a-zA-Z0-9]+$/.test(username)) { // Alphanumeric check using regex
        formIsValid = false;
        if(usernameInput) usernameInput.classList.add('is-invalid');
        if(usernameFeedbackEl) usernameFeedbackEl.textContent = 'Username can only contain letters and numbers.';
    } else {
        if(usernameInput) usernameInput.classList.remove('is-invalid');
        if(usernameFeedbackEl) usernameFeedbackEl.textContent = ''; // Clear feedback
    }


    // Email Validation: Not Blank, Valid Email Format
    const emailRegex = /^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/;
    if (!email) {
        formIsValid = false;
        if(emailInput) emailInput.classList.add('is-invalid');
        if(emailFeedbackEl) emailFeedbackEl.textContent = 'Email is required.';
    } else if (!emailRegex.test(email)) {
        formIsValid = false;
        if(emailInput) emailInput.classList.add('is-invalid');
        if(emailFeedbackEl) emailFeedbackEl.textContent = 'Please enter a valid email address.';
    } else {
        if(emailInput) emailInput.classList.remove('is-invalid');
        if(emailFeedbackEl) emailFeedbackEl.textContent = ''; // Clear feedback
    }


    // Password Validation: Not Blank, Complexity (Special, Capital, Small, Number), Min Length (e.g., 8)
    // Regex explained:
    // ^                 - Start of string
    // (?=.*[A-Z])       - Lookahead: requires at least one uppercase letter
    // (?=.*[a-z])       - Lookahead: requires at least one lowercase letter
    // (?=.*\d)          - Lookahead: requires at least one digit (0-9)
    // (?=.*[\W_])       - Lookahead: requires at least one non-word character (includes common symbols). Use (?=.*[!@#$%^&*().,_+-=|]) for specific symbols. Added _ as \W doesn't include it.
    // .                 - Match any character (except newline)
    // {8,}              - Match the previous character (.) 8 or more times (minimum length 8)
    // $                 - End of string
    const passwordComplexityRegex = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[\W_]).{8,}$/; // Adjust minimum length {8,} as needed

    if (!password) {
        formIsValid = false;
        if(passwordInput) passwordInput.classList.add('is-invalid');
        if(passwordFeedbackEl) passwordFeedbackEl.textContent = 'Password is required.';
    } else if (!passwordComplexityRegex.test(password)) {
        formIsValid = false;
        if(passwordInput) passwordInput.classList.add('is-invalid');
        if(passwordFeedbackEl) passwordFeedbackEl.textContent = 'Password must be at least 8 characters long and include uppercase, lowercase, number, and special character.'; // Customize message based on specific rules
    } else {
        if(passwordInput) passwordInput.classList.remove('is-invalid');
        if(passwordFeedbackEl) passwordFeedbackEl.textContent = ''; // Clear feedback
    }


    // Confirm Password Validation: Must match Password
    if (!confirmPassword) {
        formIsValid = false;
        if(confirmPasswordInput) confirmPasswordInput.classList.add('is-invalid');
        if(confirmPasswordFeedbackEl) confirmPasswordFeedbackEl.textContent = 'Please confirm your password.';
    } else if (password !== confirmPassword) {
        formIsValid = false;
        if(confirmPasswordInput) confirmPasswordInput.classList.add('is-invalid');
        if(confirmPasswordFeedbackEl) confirmPasswordFeedbackEl.textContent = 'Passwords do not match.';
    } else {
         if(confirmPasswordInput) confirmPasswordInput.classList.remove('is-invalid');
        if(confirmPasswordFeedbackEl) confirmPasswordFeedbackEl.textContent = ''; // Clear feedback
    }


    // If any frontend validation failed, stop the process
    if (!formIsValid) {
        console.warn("[auth.js] Registration frontend validation failed. Aborting API call.");
        // Display a general message if needed, or rely on field-specific messages
        // if(registerErrorEl) { registerErrorEl.textContent = 'Please correct the errors in the form.'; registerErrorEl.style.display = 'block'; }
        return;
    }

    // --- End Frontend Validation Logic ---


    // --- Backend API Call (if frontend validation passed) ---
    try {
        console.log("[auth.js] Frontend validation passed. Calling backend register endpoint.");
        // Send registration data to the backend
        const response = await fetch(`${AUTH_API_BASE_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, email, password }), // Include username, email, password in the body
        });

        // Check if the response status is OK (2xx). Non-OK status will be handled below.
        if (!response.ok) {
            // Attempt to read the response body as JSON first. If that fails, read as text.
             // Use response.clone() to avoid consuming the body stream twice
             const errorResponseClone = response.clone();
             const errorData = await errorResponseClone.json().catch(() => null); // Try parsing as JSON

             if (errorData && typeof errorData === 'object') {
                 // ===> Backend returned a JSON object (likely validation errors or custom API errors) <===
                 console.warn("[auth.js] Backend returned JSON error data:", errorData);

                 // Check if the error data is the validation error map (has keys like field names)
                 // Based on your GlobalExceptionHandler, MethodArgumentNotValidException returns a Map<String, String>
                 // which is a plain JSON object like {"fieldName": "errorMessage"}.
                 // Your BadRequestException returns an ErrorResponse DTO with timestamp, status, etc.
                 // We check if it looks like the validation error map by checking for lack of common ErrorResponse fields.
                 if (!errorData.timestamp && !errorData.status && Object.keys(errorData).length > 0) {
                     console.log("[auth.js] Processing backend validation errors.");
                     // ===> Process the backend validation error map <===
                     let firstErrorField = null; // To scroll to the first error
                     for (const fieldName in errorData) {
                         // Ensure it's the object's own property and not inherited
                         if (errorData.hasOwnProperty(fieldName)) {
                             const errorMessage = errorData[fieldName];
                             console.log(`[auth.js] Backend field error for ${fieldName}: ${errorMessage}`);

                             // Find the corresponding input element and its next sibling (feedback) by field name
                             // This requires consistent naming conventions in your HTML IDs.
                             // Example: If backend returns {"username": "..."} and your input ID is "registerUsername",
                             // and feedback ID is "usernameFeedback".
                             const inputElement = document.getElementById('register' + fieldName.charAt(0).toUpperCase() + fieldName.slice(1)); // Attempts to build ID like registerUsername
                             // Fallback if ID building fails or is different
                             let feedbackElement = null;
                             if (inputElement) {
                                  inputElement.classList.add('is-invalid');
                                  feedbackElement = inputElement.nextElementSibling; // Assuming feedback is the next sibling
                                  // Alternative: Get feedback by ID like document.getElementById(fieldName + 'Feedback');
                             }

                             if (feedbackElement && feedbackElement.classList.contains('invalid-feedback')) {
                                 feedbackElement.textContent = errorMessage; // Display the backend error message
                             }
                             if (!firstErrorField && inputElement) firstErrorField = fieldName; // Remember the first field with an error
                         }
                     }
                     // Optionally, scroll to the first field with an error
                     if (firstErrorField) {
                         const firstErrorInput = document.getElementById('register' + firstErrorField.charAt(0).toUpperCase() + firstErrorField.slice(1));
                         if (firstErrorInput) firstErrorInput.scrollIntoView({ behavior: 'smooth', block: 'center' });
                     }

                      // Stop processing after displaying field errors
                     return;
                 } else {
                     // ===> Backend returned JSON, but it's not the validation error map structure
                     //      (e.g., ErrorResponse from BadRequestException for duplicate username/email) <===
                     // Handle other API error responses that return JSON with 'message' or 'error' fields
                     const errorMessage = errorData.message || errorData.error || `Registration failed with status: ${response.status}`;
                     console.warn("[auth.js] Backend returned structured API error:", errorMessage);
                     // Throw this error to be caught by the main catch block below for general display
                     throw new Error(errorMessage);
                 }

             } else {
                 // ===> Backend response body was NOT JSON (e.g., plain text error, HTML error page) <===
                 const errorText = await errorResponseClone.text().catch(() => `Registration failed with status: ${response.status}`); // Read as text
                 console.warn("[auth.js] Backend response body was not JSON:", errorText);
                 // Throw the text as the error message to be caught below
                 throw new Error(errorText || `Registration failed with status: ${response.status}`);
             }
        }

        // If response.ok (status is 2xx)
        // Parse the successful response body (assuming MessageResponse { message: "..." })
        const data = await response.json();
        console.log("[auth.js] Registration successful.", data);

        // Reset the registration form
        if(registerForm) registerForm.reset();
        // Display success message
        if(registerSuccessEl) {
             registerSuccessEl.textContent = data.message || 'Registration successful! You can now log in.';
             registerSuccessEl.style.display = 'block';
         }
         // Clear any lingering validation classes/feedback on success
         clearRegisterFormValidation();

         // Optional: redirect to login page automatically after a delay
         // setTimeout(() => { showLogin(); }, 3000); // Redirect after 3 seconds

    } catch (error) {
        // This catch block handles:
        // - Network errors during the fetch call.
        // - Errors explicitly thrown from the !response.ok block above (including structured errors).
        console.error("[auth.js] Registration failed:", error);
        // Display a general error message using the message from the thrown error
        if(registerErrorEl) { registerErrorEl.textContent = 'Registration failed: ' + error.message; registerErrorEl.style.display = 'block'; }

        // Note: For validation errors handled by the !response.ok block,
        // the field-specific feedback is already displayed there, and the function returns.
        // This catch block displays a general message for other types of errors (network, API error response not handled as validation map).
    }
}
function clearRegisterFormValidation() {
    // Get all input elements within the register form that might have validation classes
    const formInputs = registerForm ? registerForm.querySelectorAll('input') : [];

    formInputs.forEach(input => {
            // Remove the 'is-invalid' class from the input element
            input.classList.remove('is-invalid');
            // Find the next sibling that is a div with class 'invalid-feedback'
            let nextSibling = input.nextElementSibling;
            while (nextSibling && !(nextSibling.tagName === 'DIV' && nextSibling.classList.contains('invalid-feedback'))) {
                nextSibling = nextSibling.nextElementSibling;
            }
            if (nextSibling && nextSibling.classList.contains('invalid-feedback')) {
                 // Clear the text content of the feedback element
                 nextSibling.textContent = '';
            }
        });

}


    // Handles user logout (calls backend to invalidate refresh token)
    // This is the function called when the logout button is clicked
    async function logout() {
        console.log("[auth.js] Attempting logout.");
        // Call the shared logoutAndRedirect helper which handles backend call and frontend cleanup
        logoutAndRedirect();
        // Note: logoutAndRedirect is async, but we don't necessarily need to await it here
        // as frontend state is updated immediately and the backend call is best-effort on logout.
    }


    // --- Expose Shared Functions to Global Scope ---
    // These functions are defined and used within auth.js, but are needed by other scripts
    // like admin.js and kitchensink.js to perform authenticated actions or get user info.
    if (typeof window !== 'undefined') { // Check if running in a browser environment
        window.getAccessToken = getAccessToken;
        window.getRefreshToken = getRefreshToken;
        window.setTokens = setTokens; // Other scripts likely won't call this, but good to have
        window.removeTokens = removeTokens; // Other scripts likely won't call this directly
        window.getCurrentUser = getCurrentUser;
        window.setCurrentUser = setCurrentUser; // Useful if another script needs to update user state (less common)
        window.logoutAndRedirect = logoutAndRedirect; // Expose the full logout flow
        window.fetchWithAuth = fetchWithAuth; // Expose the shared authenticated fetch utility

        console.log("[auth.js] Shared functions exposed globally.");
    } else {
        console.warn("[auth.js] Running in a non-browser environment, skipping global exposure.");
    }


}); // End of DOMContentLoaded listener