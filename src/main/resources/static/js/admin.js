// src/main/resources/static/js/admin.js

document.addEventListener('DOMContentLoaded', function() {
    // DOM elements
    const usersList = document.getElementById('usersList'); // Assumes <tbody> element for users table
    const adminContactsList = document.getElementById('adminContactsList'); // Assumes <tbody> element for admin contacts table
    const contactModalElement = document.getElementById('contactModal'); // The main modal element
    const contactForm = document.getElementById('contactForm'); // The form element inside the modal
    const saveBtn = document.getElementById('saveContactBtn'); // Button to save changes in modal
    const deleteModalElement = document.getElementById('deleteModal'); // The delete confirmation modal
    const confirmDeleteBtn = document.getElementById('confirmDeleteBtn'); // Button to confirm deletion
    // const modalTitle = document.getElementById('contactModalTitle'); // Title element within the modal (can get directly when needed)

    // Reused form inputs within the modal (IDs from index.html)
    const userIdInput = document.getElementById('contactId'); // Used for user ID or contact ID
    const userNameInput = document.getElementById('contactName'); // Used for username or contact name
    const userEmailInput = document.getElementById('contactEmail'); // Used for user email or contact email
    const contactPhoneInput = document.getElementById('contactPhone'); // Specific input for contact phone number

    // Groups of fields in the modal to show/hide based on entity type (User/Contact)
    const contactPhoneGroup = document.getElementById('contactPhoneGroup'); // Container for phone-related fields
    const contactCountryCodeSelect = document.getElementById('contactCountryCode'); // Assuming a country code select exists
    const contactPhoneInvalidFeedback = document.getElementById('contactPhoneInvalidFeedback'); // Feedback for phone validation

    const userRolesGroup = document.getElementById('userRolesGroup'); // Container for user role fields
    const userRoleSelect = document.getElementById('userRoleSelect'); // Select dropdown for user roles


    // Bootstrap modal instances
    const entityModal = contactModalElement ? new bootstrap.Modal(contactModalElement) : null; // Initialize modal if element exists
    const deleteModal = deleteModalElement ? new bootstrap.Modal(deleteModalElement) : null; // Initialize modal if element exists

    // State to keep track of the item being considered for deletion
    let itemToDelete = { id: null, type: null, username: null }; // Stores ID, type ('user'/'contact'), and username for users


    // IMPORTANT: Rely on shared authentication and fetch helper functions from auth.js
    // These are defined and exposed globally in auth.js and should NOT be defined here.
    // Use them via the window object: window.fetchWithAuth, window.getCurrentUser, etc.


    console.log("[admin.js] Initializing.");
    // The initial data load logic is triggered by auth.js after authentication check.
    // The loadAdminData function is exposed globally below for auth.js to call.


    // --- Event Listeners ---
    if (saveBtn && contactForm && entityModal) { // Check if essential elements exist
        saveBtn.addEventListener('click', function() {
            const entityId = userIdInput ? userIdInput.value : null; // Get ID (user or contact)
            const entityType = contactForm.dataset.type; // Get type ('contact' or 'user') from form data attribute

            console.log(`[admin.js] Save button clicked. Entity ID: ${entityId}, Type: ${entityType}`);

            // Basic checks
            if (!entityType) {
                console.error("[admin.js] Cannot save: Entity type data-type attribute is missing on the form.");
                //alert("An internal error occurred. Item type not set."); // User feedback
                return;
            }
             // For updates, an ID is required
            if (!entityId) {
                 console.error("[admin.js] Cannot save: Missing entity ID in form.");
                 //alert("An internal error occurred. Item ID not set."); // User feedback
                 return;
            }


            if (entityType === 'contact') {
                updateContact(entityId); // Call function to update contact
            } else if (entityType === 'user') {
                updateUser(entityId); // Call function to update user
            } else {
                 console.error("[admin.js] Unknown entity type:", entityType);
                 //alert("An internal error occurred. Unknown item type."); // User feedback
            }
        });
    } else { console.error("[admin.js] Save button, Contact form, or Entity modal not found!"); }


    if (confirmDeleteBtn && deleteModal) { // Check if essential elements exist
        confirmDeleteBtn.addEventListener('click', function() {
            console.log(`[admin.js] Confirm Delete button clicked for item:`, itemToDelete);
            // Check if itemToDelete state has valid information
            if (itemToDelete && itemToDelete.id && itemToDelete.type) {
                 if (itemToDelete.type === 'contact') {
                     deleteContact(itemToDelete.id); // Call function to delete contact
                 } else if (itemToDelete.type === 'user') {
                     deleteUser(itemToDelete.id); // Call function to delete user
                 } else {
                     console.error("[admin.js] Item to delete type is invalid in state:", itemToDelete.type);
                     alert("An internal error occurred. Invalid item type for deletion."); // User feedback
                     deleteModal.hide(); // Hide modal
                 }
            } else {
                console.error("[admin.js] Item to delete state is invalid:", itemToDelete);
                 //alert("An internal error occurred. Cannot determine item to delete."); // User feedback
                 deleteModal.hide(); // Hide modal
            }
            // itemToDelete state is reset in the delete functions (success or error)
        });
    } else { console.error("[admin.js] Confirm Delete button or Delete modal not found!"); }


    // --- Main Data Loading Function ---
    // This function is called by auth.js after the admin user is authenticated
    async function loadAdminData() {
        console.log("[admin.js] Loading Admin data...");
        // Use the shared getCurrentUser helper from auth.js if needed for welcome message etc.
         const currentUser = window.getCurrentUser();
         console.log("[admin.js] Logged-in User from shared helper:", currentUser);

        loadUsers(); // Load the list of users
        loadAllContacts(); // Load the list of all contacts managed by admins
    }

    // --- User Management Functions (Admin View) ---

    // Fetches all users from the backend
    async function loadUsers() {
        console.log("[admin.js] Loading all users...");
        try {
             // Display a loading message in the users table
            if (usersList) usersList.innerHTML = '<tr><td colspan="6" class="text-center">Loading users...</td></tr>';

            // Use the shared fetchWithAuth function from auth.js
            const users = await window.fetchWithAuth('/api/admin/users');
            console.log("[admin.js] Received users:", users);
            renderUsers(users || []); // Render the received users (handle null/undefined)
        } catch (error) {
            console.error('[admin.js] Error loading users:', error);
            // Display error message to the user
            alert(`Error loading users: ${error.message}`);
             if (usersList) usersList.innerHTML = `<tr><td colspan="6" class="text-danger">Failed to load users: ${error.message}</td></tr>`; // Provide feedback in the table
        }
    }

    // Renders the list of users in the users table
    function renderUsers(users) {
         console.log("[admin.js] Rendering users:", users);
        if (!usersList) {
            console.error("[admin.js] usersList element not found. Cannot render users.");
            return;
        }
        usersList.innerHTML = ''; // Clear previous entries

        if (!Array.isArray(users) || users.length === 0) {
             console.log("[admin.js] No users found to render.");
             if (usersList) usersList.innerHTML = '<tr><td colspan="6">No users found.</td></tr>';
             return;
         }

        console.log(`[admin.js] Rendering ${users.length} users.`);
        // Use the shared getCurrentUser helper from auth.js to check logged-in user
        const loggedInUser = window.getCurrentUser();
        const loggedInUsername = loggedInUser?.username;


        users.forEach(user => {
            const row = usersList.insertRow(); // Insert a new row for each user

            // Create HTML badges for user roles
            const rolesBadges = (user.roles || []).map(role => {
                // Assuming roles are returned as strings (e.g., "ROLE_ADMIN") based on our backend logic
                const roleName = (typeof role === 'string' ? role : role?.name) || 'UNKNOWN_ROLE'; // Defensive check
                const badgeClass = roleName === 'ROLE_ADMIN' ? 'bg-danger' : (roleName === 'ROLE_USER' ? 'bg-primary' : 'bg-secondary'); // Customize colors
                const displayRoleName = roleName.replace('ROLE_', ''); // Display "USER", "ADMIN"
                return `<span class="badge ${badgeClass} me-1">${displayRoleName}</span>`;
            }).join(' ');

            // Check if the current user being rendered is the logged-in admin user
            const isCurrentUser = loggedInUsername && loggedInUsername === user.username;

            // Populate the row with user data and action buttons
            row.innerHTML = `
                <td>${user.id || 'N/A'}</td>
                <td>${user.username || user.name || 'N/A'}</td>
                <td>${user.email || 'N/A'}</td>
                <td>${rolesBadges || 'No Roles'}</td>
                <td>${user.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'N/A'}</td>
                <td>
                     <button class="btn btn-sm btn-primary edit-user me-1" data-id="${user.id}" title="Edit User">
                         <i class="fas fa-edit"></i> Edit
                     </button>
                     <button class="btn btn-sm btn-danger delete-user ${isCurrentUser ? 'disabled' : ''}"
                             data-id="${user.id}"
                             data-username="${user.username}"
                             title="${isCurrentUser ? 'Cannot delete your own account' : 'Delete User'}"
                             ${isCurrentUser ? 'disabled' : ''}>
                         <i class="fas fa-trash"></i> Delete
                     </button>
                </td>
            `;
        });

        // Use event delegation on the usersList table body to handle clicks on edit/delete buttons
        // This is efficient as we only attach one listener to the table body.
        // Remove and re-add listener to prevent duplicates if renderUsers is called multiple times.
        usersList.removeEventListener('click', handleUserListClick);
        usersList.addEventListener('click', handleUserListClick);

    }

    // Event delegation handler for clicks within the users table body
    function handleUserListClick(event) {
        const target = event.target;
        // Use closest() to find the button element, even if the icon (<i>) or text is clicked
        const editButton = target.closest('.edit-user');
        const deleteButton = target.closest('.delete-user');

        if (editButton && usersList.contains(editButton)) { // Ensure the clicked button is within our delegated area
            const userId = editButton.getAttribute('data-id'); // Get user ID from data attribute
            console.log(`[admin.js] Edit User button clicked (delegation) for ID: ${userId}`);
            editUser(userId); // Call the edit user function
        } else if (deleteButton && usersList.contains(deleteButton)) { // Ensure the clicked button is within our delegated area
             // Check if the delete button is disabled (prevents trying to delete self)
             if (deleteButton.disabled) {
                 console.log("[admin.js] Clicked on disabled delete button (Cannot delete self).");
                 // alert(deleteButton.title); // Optional: Show the tooltip message as an alert
                 return; // Stop execution if button is disabled
             }
            const userId = deleteButton.getAttribute('data-id'); // Get user ID
            const username = deleteButton.getAttribute('data-username'); // Get username (used for self-delete check)
            console.log(`[admin.js] Delete User button clicked (delegation) for ID: ${userId}, Username: ${username}`);
            confirmDeleteUser(userId, username); // Call the confirmation function
        }
    }


    // Fetches a single user's data and populates the modal for editing
    async function editUser(userId) {
        console.log(`[admin.js] Editing user ID: ${userId}`);
         // Reset any previous validation feedback when opening modal
         clearUserFormValidation();
         clearContactFormValidation(); // Also clear contact validation if modal reuses elements

         try {
             // Use the shared fetchWithAuth function from auth.js to fetch user data
            const user = await window.fetchWithAuth('/api/admin/users/' + userId);
            if (!user) {
                 throw new Error("User data not received from API.");
            }
             console.log("[admin.js] Received user data for edit:", user);

            // Populate form fields in the modal
            if (contactForm) contactForm.dataset.type = 'user'; // Set the form's data-type to 'user'
            if (userIdInput) userIdInput.value = user.id;
            if (userNameInput) userNameInput.value = user.username || user.name || ''; // Populate username field
            if (userEmailInput) userEmailInput.value = user.email || ''; // Populate email field

            // --- Handle Roles (Populate the dropdown and set selected role) ---
            if (userRolesGroup && userRoleSelect) {
                userRoleSelect.innerHTML = ''; // Clear previous options

                // Define the available roles for the dropdown. These MUST match your backend ERole enum names.
                const availableRoles = ['ROLE_USER', 'ROLE_ADMIN'];

                // Add a default/placeholder option at the top of the dropdown
                const defaultOption = document.createElement('option');
                defaultOption.value = ''; // Empty value for placeholder
                defaultOption.textContent = '-- Select Role --';
                userRoleSelect.appendChild(defaultOption);

                // Add options for each available role
                availableRoles.forEach(roleName => {
                    const option = document.createElement('option');
                    option.value = roleName; // The value is the backend role name string
                    option.textContent = roleName.replace('ROLE_', ''); // Display "USER", "ADMIN" in the dropdown
                    userRoleSelect.appendChild(option);
                });

                // Set the currently selected role in the dropdown based on user data from backend
                 // Assuming user.roles from backend is an array of role objects {name: "ROLE_USER"} or strings "ROLE_USER"
                 // Find the first role that matches one of our available roles
                 const currentUserRoleName = (user.roles || []).map(role => typeof role === 'string' ? role : role?.name).find(roleName => availableRoles.includes(roleName));

                 if (currentUserRoleName) {
                    userRoleSelect.value = currentUserRoleName; // Set the dropdown value to the matched role name string
                 } else {
                     userRoleSelect.value = ''; // Select the placeholder if no matching role found
                 }

                 // Show the user roles section and hide the contact phone section in the modal
                 userRolesGroup.style.display = '';
                 if (contactPhoneGroup) {
                     contactPhoneGroup.style.display = 'none';
                 }

                 // ** Prevent admin from changing their OWN role (Security/Prevent accidental lockout) **
                 // Use the shared getCurrentUser helper from auth.js
                 const loggedInUser = window.getCurrentUser();
                 const loggedInUsername = loggedInUser?.username;
                 if (loggedInUsername && loggedInUsername === user.username) {
                    userRoleSelect.disabled = true; // Disable the dropdown for the logged-in admin's own user record
                    console.warn("[admin.js] Admin cannot edit their own role.");
                 } else {
                    userRoleSelect.disabled = false; // Ensure dropdown is enabled for other users
                 }

            } else {
                console.warn("[admin.js] User roles dropdown elements (userRolesGroup or userRoleSelect) not found in modal.");
                // Still hide phone section even if roles elements aren't found
                 if (contactPhoneGroup) {
                     contactPhoneGroup.style.display = 'none';
                 }
            }

            // Update the modal title
            const modalTitleElement = document.getElementById('contactModalTitle');
            if (modalTitleElement) modalTitleElement.textContent = 'Edit User';

            // Show the modal
            if (entityModal) entityModal.show();

        } catch (error) {
            console.error(`[admin.js] Error loading user ${userId} for edit:`, error);
            // Display error message to the user
            alert(`Error loading user details: ${error.message}`);
        }
    }

    // Validates the user form inputs
    function validateUserForm(username, email, selectedRole) {
        let isValid = true;

        // Clear previous validation feedback
        clearUserFormValidation();

        // Basic username validation (Add your specific rules here)
         if (!username || username.length < 3 || username.length > 50) {
             isValid = false;
             if(userNameInput) { // Check if element exists before adding class/feedback
                 userNameInput.classList.add('is-invalid');
                 const feedback = userNameInput.nextElementSibling;
                 if (feedback && feedback.classList.contains('invalid-feedback')) {
                     feedback.textContent = 'Username must be between 3 and 50 characters.';
                 }
             }
         }

         // Basic email validation (Add your specific rules here)
         const emailRegex = /^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/;
         if (!email || !emailRegex.test(email)) {
             isValid = false;
             if(userEmailInput) { // Check if element exists
                  userEmailInput.classList.add('is-invalid');
                  const feedback = userEmailInput.nextElementSibling;
                  if (feedback && feedback.classList.contains('invalid-feedback')) {
                     feedback.textContent = 'Please enter a valid email address.';
                 }
             }
         }

         // Basic role selection validation
         if (userRoleSelect && !selectedRole) { // Check if element exists and if a role is selected
              isValid = false;
              userRoleSelect.classList.add('is-invalid');
              const feedback = userRoleSelect.nextElementSibling;
              if (feedback && feedback.classList.contains('invalid-feedback')) {
                 feedback.textContent = 'Please select a role.';
             }
         }


        return isValid; // Return overall validation status
    }

    // Clears validation feedback for user form inputs
    function clearUserFormValidation() {
         if(userNameInput) {
             userNameInput.classList.remove('is-invalid');
             const feedback = userNameInput.nextElementSibling;
             if (feedback && feedback.classList.contains('invalid-feedback')) feedback.textContent = '';
         }
         if(userEmailInput) {
             userEmailInput.classList.remove('is-invalid');
              const feedback = userEmailInput.nextElementSibling;
              if (feedback && feedback.classList.contains('invalid-feedback')) feedback.textContent = '';
         }
         if(userRoleSelect) {
              userRoleSelect.classList.remove('is-invalid');
              const feedback = userRoleSelect.nextElementSibling;
              if (feedback && feedback.classList.contains('invalid-feedback')) feedback.textContent = '';
         }
    }


    // Sends updated user data to the backend
    async function updateUser(userId) {
        // Get form values from the modal inputs
        const username = userNameInput ? userNameInput.value.trim() : ''; // Added trim()
        const email = userEmailInput ? userEmailInput.value.trim() : '';   // Added trim()
        // Get the selected role name string from the dropdown
        const selectedRole = userRoleSelect ? userRoleSelect.value : '';

        console.log(`[admin.js] Preparing to save user ID ${userId}. Username: ${username}, Email: ${email}, Role: ${selectedRole}`);

        // Validate the form data before sending
        if (!validateUserForm(username, email, selectedRole)) {
             console.warn("[admin.js] User form validation failed. Aborting save.");
            return; // Stop if validation fails
        }

        // Create the data object to be sent as JSON to the backend
        const userData = {
            // Include fields only if they are part of the update DTO and needed
            // Added checks for element existence before getting value and adding to object
            ...(username && { username: username }),
            ...(email && { email: email }),
             // Send roles as an array of strings ["ROLE_USER"] or ["ROLE_ADMIN"] etc.
             // Based on your single select dropdown, it will be an array with one string or empty if placeholder is selected
             // Only include the roles field if a role is selected (selectedRole is not '')
             ...(selectedRole && { roles: [selectedRole] }) // Send the selected role string in an array if selected
        };

        console.log("[admin.js] User data payload being sent:", userData);


        // ** Optional frontend check: Prevent saving if trying to change the logged-in admin's OWN role **
        // This is a frontend convenience/safety check. Backend validation is essential for security.
         const loggedInUser = window.getCurrentUser();
         const loggedInUsername = loggedInUser?.username;
         // Check if we are editing the logged-in admin AND the role select is disabled (meaning we intended to block changes)
         if (userRoleSelect && userRoleSelect.disabled && loggedInUsername && loggedInUsername === username) {
              console.warn("[admin.js] Blocked attempt to save own role (frontend check).");
              alert("You cannot change your own role.");
              // Close the modal as no changes can be saved anyway
              if (entityModal) entityModal.hide();
              return; // Prevent API call
         }


        try {
            console.log(`[admin.js] Calling window.fetchWithAuth for PUT /api/admin/users/${userId}`);
            // Use the shared fetchWithAuth function from auth.js
            const updatedUser = await window.fetchWithAuth(`/api/admin/users/${userId}`, {
                 method: 'PUT',
                 body: JSON.stringify(userData) // Stringify the corrected payload
            });
            console.log(`[admin.js] window.fetchWithAuth completed. Updated User:`, updatedUser);

            // On successful update
            if (entityModal) entityModal.hide(); // Hide the modal
            loadUsers(); // Reload the users list to show changes
            alert('User updated successfully!'); // User feedback
        } catch (error) {
            console.error(`[admin.js] Catch block caught error during user update:`, error);
             // Display error message to the user (error.message includes backend message or status)
             alert(`Error updating user: ${error.message}`);
             // Keep modal open or close on error based on preference
             // if (entityModal) entityModal.hide(); // Optional: hide modal on error
        }
    }

    // Confirms user deletion and shows delete modal
     function confirmDeleteUser(userId, username) {
        // Use the shared getCurrentUser helper from auth.js to check logged-in user
        const loggedInUser = window.getCurrentUser();
        const loggedInUsername = loggedInUser?.username;

        // ** Prevent deleting the currently logged-in admin user (Frontend check) **
        if (loggedInUsername && loggedInUsername === username) {
            console.warn(`[admin.js] Blocked attempt to delete logged-in admin user: ${username}`);
            alert("You cannot delete your own user account from this list."); // User feedback
            // Do NOT proceed with deletion or show modal
            itemToDelete = { id: null, type: null, username: null }; // Reset state
            return; // Stop here
        }

        console.log(`[admin.js] Confirming delete for user ID: ${userId}, Username: ${username}`);
        // Set the state for the delete confirmation modal
        itemToDelete = { id: userId, type: 'user', username: username }; // Store user details for deletion

        // Customize delete modal message
        const deleteModalBody = deleteModalElement ? deleteModalElement.querySelector('.modal-body') : null;
        if (deleteModalBody) {
            deleteModalBody.textContent = `Are you sure you want to delete user "${username}" (ID: ${userId})? This action cannot be undone.`;
        }
        // Show the delete confirmation modal
        if (deleteModal) deleteModal.show();
     }

    // Sends the user deletion request to the backend
    async function deleteUser(userId) {
        console.log(`[admin.js] Deleting user ID: ${userId}`);

         // ** Defensive Frontend check: Prevent deleting the currently logged-in admin user **
         // This adds another layer of safety, though confirmDeleteUser should prevent reaching here.
         const loggedInUser = window.getCurrentUser();
         const loggedInUsername = loggedInUser?.username;
         if (itemToDelete.username && loggedInUsername && loggedInUsername === itemToDelete.username) {
              console.warn("[admin.js] Blocked attempt to delete logged-in admin user (deleteUser function check).");
              alert("You cannot delete your own user account."); // User feedback
              if (deleteModal) deleteModal.hide(); // Hide confirmation modal
              itemToDelete = { id: null, type: null, username: null }; // Reset state
              return; // Prevent API call
         }

         // Check if userId is valid before sending request
         if (!userId) {
             console.error("[admin.js] Cannot delete: User ID is missing.");
             alert("An internal error occurred. User ID not set for deletion.");
             if (deleteModal) deleteModal.hide();
             itemToDelete = { id: null, type: null, username: null }; // Reset state
             return;
         }


         try {
            console.log(`[admin.js] Calling window.fetchWithAuth for DELETE /api/admin/users/${userId}`);
            // Use the shared fetchWithAuth function from auth.js
            await window.fetchWithAuth(`/api/admin/users/${userId}`, {
                 method: 'DELETE'
            });
            console.log(`[admin.js] window.fetchWithAuth completed for user delete.`);

            // On successful deletion
            if (deleteModal) deleteModal.hide(); // Hide confirmation modal
            itemToDelete = { id: null, type: null, username: null }; // Reset state
            loadUsers(); // Reload the users list
            alert('User deleted successfully.'); // User feedback
        } catch (error) {
            console.error(`[admin.js] Catch block caught error during user delete:`, error);
             alert(`Error deleting user: ${error.message}`); // Error message includes backend message or status
             if (deleteModal) deleteModal.hide(); // Hide modal on error
             itemToDelete = { id: null, type: null, username: null }; // Reset state
        }
    }


    // --- Contact Management Functions (Admin View - for managing ALL contacts) ---

    // Fetches all contacts from the backend (Admin view)
    async function loadAllContacts() {
         console.log("[admin.js] Loading all contacts (Admin View)...");
        try {
             // Display a loading message in the contacts table
            if (adminContactsList) adminContactsList.innerHTML = '<tr><td colspan="6" class="text-center">Loading contacts...</td></tr>';

            // Use the shared fetchWithAuth function from auth.js
            const contacts = await window.fetchWithAuth('/api/admin/contacts');
             console.log("[admin.js] Received contacts (Admin View):", contacts);
            renderAdminContacts(contacts || []); // Render the received contacts
        } catch (error) {
            console.error('[admin.js] Error loading contacts (Admin View):', error);
            alert(`Error loading contacts: ${error.message}`); // User feedback
             if (adminContactsList) adminContactsList.innerHTML = `<tr><td colspan="6" class="text-danger">Failed to load contacts: ${error.message}</td></tr>`;
        }
    }

    // Renders the list of contacts in the admin contacts table
    function renderAdminContacts(contacts) {
        console.log("[admin.js] Rendering contacts (Admin View):", contacts);
         if (!adminContactsList) {
            console.error("[admin.js] adminContactsList element not found. Cannot render contacts.");
            return;
        }
        adminContactsList.innerHTML = ''; // Clear previous entries

         if (!Array.isArray(contacts) || contacts.length === 0) {
             console.log("[admin.js] No contacts found to render (Admin View).");
             if (adminContactsList) adminContactsList.innerHTML = '<tr><td colspan="6">No contacts found.</td></tr>';
             return;
         }

        console.log(`[admin.js] Rendering ${contacts.length} contacts (Admin View).`);

        contacts.forEach(contact => {
            const row = adminContactsList.insertRow(); // Insert a new row

            // Populate the row with contact data and action buttons
            row.innerHTML = `
                <td>${contact.name || 'N/A'}</td>
                <td>${contact.email || 'N/A'}</td>
                <td>${contact.phoneNumber || 'N/A'}</td>
                <td>${contact.createdBy || 'N/A'}</td>
                <td>${contact.createdAt ? new Date(contact.createdAt).toLocaleDateString() : 'N/A'}</td>
                <td>
                    <button class="btn btn-sm btn-primary edit-admin-contact me-1" data-id="${contact.id}" title="Edit Contact">
                        <i class="fas fa-edit"></i> Edit
                    </button>
                    <button class="btn btn-sm btn-danger delete-admin-contact" data-id="${contact.id}" title="Delete Contact">
                        <i class="fas fa-trash"></i> Delete
                    </button>
                </td>
            `;
        });

         // Use event delegation on the adminContactsList table body to handle clicks on edit/delete buttons
        // Remove and re-add listener to prevent duplicates if renderAdminContacts is called multiple times.
         adminContactsList.removeEventListener('click', handleAdminContactListClick);
         adminContactsList.addEventListener('click', handleAdminContactListClick);
    }

    // Event delegation handler for clicks within the admin contacts table body
    function handleAdminContactListClick(event) {
        const target = event.target;
        // Use closest() to find the button element
        const editButton = target.closest('.edit-admin-contact');
        const deleteButton = target.closest('.delete-admin-contact');

        if (editButton && adminContactsList.contains(editButton)) { // Ensure the clicked button is within our delegated area
            const contactId = editButton.getAttribute('data-id'); // Get contact ID
            console.log(`[admin.js] Edit Contact (Admin) button clicked (delegation) for ID: ${contactId}`);
            editContact(contactId); // Call the admin edit contact function
        } else if (deleteButton && adminContactsList.contains(deleteButton)) { // Ensure the clicked button is within our delegated area
            const contactId = deleteButton.getAttribute('data-id'); // Get contact ID
             console.log(`[admin.js] Delete Contact (Admin) button clicked (delegation) for ID: ${contactId}`);
             // Admin can delete any contact, no self-delete check needed here
            confirmDeleteContact(contactId); // Call the confirmation function
        }
    }


    // Fetches a single contact's data for admin editing and populates the modal
    async function editContact(contactId) { // This function is for ADMIN editing ANY contact
        console.log(`[admin.js] Editing contact ID (Admin View): ${contactId}`);
        // Reset any previous validation feedback
         clearContactFormValidation();
         clearUserFormValidation(); // Also clear user validation if modal reuses elements


        try {
             // Use the shared fetchWithAuth function from auth.js to fetch contact data
             // Assumes an /api/admin/contacts/{id} endpoint exists for admin view
             const contact = await window.fetchWithAuth('/api/admin/contacts/' + contactId);
             if (!contact) {
                throw new Error("Contact data not received from API.");
             }
             console.log("[admin.js] Received contact data for admin edit:", contact);

            // Populate form fields in the modal (reusing user fields)
            if (contactForm) contactForm.dataset.type = 'contact'; // Set the form's data-type to 'contact'
            if (userIdInput) userIdInput.value = contact.id;       // Use shared ID input for contact ID
            if (userNameInput) userNameInput.value = contact.name || '';   // Use shared Name input
            if (userEmailInput) userEmailInput.value = contact.email || ''; // Use shared Email input
             // Populate phone input
             if (contactPhoneInput) contactPhoneInput.value = contact.phoneNumber || '';


            // Show/Hide relevant sections in modal: Show phone, Hide roles
             if (contactPhoneGroup) {
                 contactPhoneGroup.style.display = '';
             } else {
                console.warn("[admin.js] Contact phone group (contactPhoneGroup) not found in modal.");
             }
             if (userRolesGroup) {
                  userRolesGroup.style.display = 'none';
             } else {
                 console.warn("[admin.js] User roles group (userRolesGroup) not found in modal.");
             }

            // Update modal title
            const modalTitleElement = document.getElementById('contactModalTitle');
            if (modalTitleElement) modalTitleElement.textContent = 'Edit Contact (Admin)';

            // Show the modal
            if (entityModal) entityModal.show();

        } catch (error) {
            console.error(`[admin.js] Error loading contact ${contactId} for admin edit:`, error);
            alert(`Error loading contact details (Admin): ${error.message}`); // User feedback
        }
    }

     // Validates the contact form inputs
    function validateContactForm(name, email, phoneNumber) {
        let isValid = true;

        // Clear previous validation feedback
        clearContactFormValidation();

        // Basic name validation (Add your specific rules here)
        if (!name || name.length < 2 || name.length > 100) {
             isValid = false;
             if(userNameInput) {
                 userNameInput.classList.add('is-invalid');
                 const feedback = userNameInput.nextElementSibling;
                 if (feedback && feedback.classList.contains('invalid-feedback')) {
                     feedback.textContent = 'Name must be between 2 and 100 characters.';
                 }
             }
        }

        // Basic email validation (Add your specific rules here)
         const emailRegex = /^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/;
         if (!email || !emailRegex.test(email)) {
             isValid = false;
             if(userEmailInput) {
                 userEmailInput.classList.add('is-invalid');
                  const feedback = userEmailInput.nextElementSibling;
                  if (feedback && feedback.classList.contains('invalid-feedback')) {
                     feedback.textContent = 'Please enter a valid email address.';
                 }
             }
         }

        // Basic phone number validation (Optional: add your rules)
        // This is complex and depends on your country code logic and requirements.
        // Example: if phone number is required
        /*
        if (!phoneNumber || phoneNumber.length < 5) { // Example minimal length
             isValid = false;
             if(contactPhoneInput) contactPhoneInput.classList.add('is-invalid');
             if(contactPhoneInvalidFeedback) contactPhoneInvalidFeedback.textContent = 'Please enter a valid phone number.';
        }
        */

        return isValid; // Return overall validation status
    }

    // Clears validation feedback for contact form inputs
    function clearContactFormValidation() {
         if(userNameInput) {
             userNameInput.classList.remove('is-invalid');
             const feedback = userNameInput.nextElementSibling;
             if (feedback && feedback.classList.contains('invalid-feedback')) feedback.textContent = '';
         }
         if(userEmailInput) {
             userEmailInput.classList.remove('is-invalid');
              const feedback = userEmailInput.nextElementSibling;
              if (feedback && feedback.classList.contains('invalid-feedback')) feedback.textContent = '';
         }
         if(contactPhoneInput) {
             contactPhoneInput.classList.remove('is-invalid');
             if(contactPhoneInvalidFeedback) { // Check if feedback element exists
                  contactPhoneInvalidFeedback.textContent = '';
             }
         }
    }


    // Sends updated contact data to the backend (Admin view)
    async function updateContact(contactId) {
        // Get form values from the modal inputs
        const name = userNameInput ? userNameInput.value.trim() : ''; // Added trim()
        const email = userEmailInput ? userEmailInput.value.trim() : '';   // Added trim()
        const phoneNumber = contactPhoneInput ? contactPhoneInput.value.trim() : ''; // Added trim()


        console.log(`[admin.js] Preparing to save contact ID ${contactId} (Admin). Name: ${name}, Email: ${email}, Phone: ${phoneNumber}`);

        // Validate the form data before sending
        if (!validateContactForm(name, email, phoneNumber)) {
             console.warn("[admin.js] Contact form validation failed (Admin). Aborting save.");
            return; // Stop if validation fails
        }

        // Create the data object to be sent as JSON to the backend
        const contactData = {
            // Include fields only if they are part of the update DTO and needed
            ...(name && { name: name }),
            ...(email && { email: email }),
            ...(phoneNumber && { phoneNumber: phoneNumber }) // Include phone if it has a value
             // Admin contact update might also allow changing 'createdBy', check your backend API and DTO
        };

        console.log("[admin.js] Contact data payload being sent:", contactData);


        try {
            console.log(`[admin.js] Calling window.fetchWithAuth for PUT /api/admin/contacts/${contactId}`);
            // Use the shared fetchWithAuth function from auth.js
            // Ensure fetchWithAuth handles non-JSON body if you ever send non-JSON, but here it's JSON.
            const updatedContact = await window.fetchWithAuth(`/api/admin/contacts/${contactId}`, {
                method: 'PUT',
                body: JSON.stringify(contactData) // Stringify the contact data payload
            });
            console.log(`[admin.js] window.fetchWithAuth completed (Admin contact update). Updated Contact:`, updatedContact);

            // On successful update
            if (entityModal) entityModal.hide(); // Hide the modal
            loadAllContacts(); // Reload the admin contacts list
            alert('Contact updated successfully (Admin)!'); // User feedback
        } catch (error) {
             console.error(`[admin.js] Catch block caught error during admin contact update:`, error);
            // Display error message to the user
            alert(`Error updating contact (Admin): ${error.message}`);
            // Keep modal open or close on error based on preference
            // if (entityModal) entityModal.hide(); // Optional: hide modal on error
        }
    }

     // Confirms contact deletion and shows delete modal (Admin view)
     function confirmDeleteContact(contactId) {
        console.log(`[admin.js] Confirming delete for contact ID (Admin View): ${contactId}`);
        // Set the state for the delete confirmation modal
        itemToDelete = { id: contactId, type: 'contact', username: null }; // Store contact ID and type

        // Customize delete modal message
         const deleteModalBody = deleteModalElement ? deleteModalElement.querySelector('.modal-body') : null;
        if (deleteModalBody) {
            deleteModalBody.textContent = `Are you sure you want to delete contact with ID "${contactId}"? This action cannot be undone.`;
        }
        // Show the delete confirmation modal
        if (deleteModal) deleteModal.show();
     }


    // Sends the contact deletion request to the backend (Admin view)
    async function deleteContact(contactId) { // This function is for ADMIN deleting ANY contact
        console.log(`[admin.js] Deleting contact ID (Admin View): ${contactId}`);

         // Check if contactId is valid before sending request
         if (!contactId) {
              console.error("[admin.js] Cannot delete: Contact ID is missing.");
              alert("An internal error occurred. Contact ID not set for deletion.");
              if (deleteModal) deleteModal.hide();
              itemToDelete = { id: null, type: null, username: null }; // Reset state
              return;
         }


        try {
            console.log(`[admin.js] Calling window.fetchWithAuth for DELETE /api/admin/contacts/${contactId}`);
            // Use the shared fetchWithAuth function from auth.js
            await window.fetchWithAuth(`/api/admin/contacts/${contactId}`, {
                 method: 'DELETE'
            });
            console.log(`[admin.js] window.fetchWithAuth completed for admin contact delete.`);

            // On successful deletion
            if (deleteModal) deleteModal.hide(); // Hide confirmation modal
            itemToDelete = { id: null, type: null, username: null }; // Reset state
            loadAllContacts(); // Reload the admin contacts list
             alert('Contact deleted successfully (Admin).'); // User feedback
        } catch (error) {
            console.error(`[admin.js] Catch block caught error during admin contact delete:`, error);
             alert(`Error deleting contact (Admin): ${error.message}`); // Error message includes backend message or status
             if (deleteModal) deleteModal.hide(); // Hide modal on error
             itemToDelete = { id: null, type: null, username: null }; // Reset state
        }
    }


    // Expose functions to global scope if needed by other scripts (like auth.js)
    // Only expose functions that other scripts (specifically auth.js) need to call directly.
    // In this case, auth.js calls loadAdminData to populate the admin section.
    if (typeof window !== 'undefined') { // Check if window object exists (for browser environment)
        window.loadAdminData = loadAdminData; // Expose the main data loading function
        console.log("[admin.js] loadAdminData exposed globally.");
    } else {
        console.warn("[admin.js] Running in a non-browser environment, skipping global exposure.");
    }


}); // End of DOMContentLoaded listener