// src/main/resources/static/js/kitchensink.js

document.addEventListener('DOMContentLoaded', function() {
    // DOM elements
    const addContactBtn = document.getElementById('addContactBtn');
    const saveContactBtn = document.getElementById('saveContactBtn');
    const contactForm = document.getElementById('contactForm');
    const contactsList = document.getElementById('contactsList'); // Assumes <tbody>
    const noContactsMessage = document.getElementById('noContactsMessage');
    const contactsTable = document.getElementById('contactsTable');
    const welcomeMessageElement = document.getElementById('welcomeMessage'); // Get welcome message element

    // Contact form specific inputs (IDs are reused by admin.js, but elements are used here)
    const contactIdInput = document.getElementById('contactId'); // Hidden input for ID
    const contactNameInput = document.getElementById('contactName');
    const contactEmailInput = document.getElementById('contactEmail');
    const contactPhoneInput = document.getElementById('contactPhone');

    // Get references to the phone and roles groups in the modal (IDs are reused)
    const contactPhoneGroup = document.getElementById('contactPhoneGroup');
    const userRolesGroup = document.getElementById('userRolesGroup'); // This will be hidden in kitchensink.js


    // Delete confirmation modal elements (IDs are reused)
    const deleteModalElement = document.getElementById('deleteModal');
    const confirmDeleteBtn = document.getElementById('confirmDeleteBtn');

    // Validation elements (used in validateContactForm)
    const contactCountryCodeSelect = document.getElementById('contactCountryCode');
    const contactPhoneInvalidFeedback = document.getElementById('contactPhoneInvalidFeedback');
     const contactNameInputFeedback = document.querySelector('#contactName + .invalid-feedback'); // Get feedback element
     const contactEmailInputFeedback = document.querySelector('#contactEmail + .invalid-feedback'); // Get feedback element


    // Bootstrap modals (IDs are reused)
    const contactModal = new bootstrap.Modal(document.getElementById('contactModal'));
    const deleteModal = new bootstrap.Modal(deleteModalElement);

    // State for deletion
    let contactToDeleteId = null;

    // --- Helper Function for Authenticated Fetch (REMOVED - USING SHARED FUNCTION FROM AUTH.JS) ---
    // The fetchWithAuth function is now provided by auth.js and accessible via window.fetchWithAuth
    // Token helper functions (getToken, getAccessToken, etc.) are also removed and will be accessed via window.


    // Event listeners
    addContactBtn.addEventListener('click', function() {
        console.log("[kitchensink.js] Add Contact button clicked.");
        resetContactForm(); // Reset the form state
        document.getElementById('contactModalTitle').textContent = 'Add Contact'; // Set modal title for adding

        // Explicitly show phone and hide roles for CONTACTS form in the modal
        if (contactPhoneGroup) contactPhoneGroup.style.display = ''; // Show phone section
        if (userRolesGroup) userRolesGroup.style.display = 'none'; // Hide roles section

        contactForm.dataset.type = 'create'; // Set form type to 'create'
        contactModal.show(); // Show the modal
    });

    saveContactBtn.addEventListener('click', saveContact); // Save button listener

    // Event listener for delete confirmation button in the delete modal
    confirmDeleteBtn.addEventListener('click', function() {
        console.log(`[kitchensink.js] Confirm Delete button clicked for ID: ${contactToDeleteId}`);
        if (contactToDeleteId !== null) {
            deleteContact(contactToDeleteId); // Call the delete function if ID is set
        }
    });

    // Event delegation for edit/delete buttons on the contacts list table body
    // This listens for clicks on the tbody and checks if the click target is an edit or delete button (or its icon)
    if (contactsList) { // Ensure the element exists before adding listener
         contactsList.addEventListener('click', function(event) {
             const target = event.target;
             // Use closest for robustness if the click is on the icon inside the button
             const editButton = target.closest('.edit-contact');
             const deleteButton = target.closest('.delete-contact');

             if (editButton) {
                 const contactId = editButton.getAttribute('data-id');
                 console.log(`[kitchensink.js] Edit Contact button clicked (delegation) for ID: ${contactId}`);
                 editContact(contactId); // Call edit contact function
             } else if (deleteButton) {
                 const contactId = deleteButton.getAttribute('data-id');
                 console.log(`[kitchensink.js] Delete Contact button clicked (delegation) for ID: ${contactId}`);
                 confirmDeleteContact(contactId); // Call confirm delete function
             }
         });
    } else {
        console.error("[kitchensink.js] contactsList element (#contactsList) not found.");
    }


    // Initialize - This only runs on initial page load IF auth.js loads it
    console.log("[kitchensink.js] Initializing.");
    // The initial data load logic is now handled by auth.js after it confirms authentication state.
    // The loadUserContacts function is exposed globally below for auth.js to call.


    // --- Core Kitchen Sink Functions ---

    // Make loadUserContacts accessible if needed externally by auth.js after login
    // This is required so auth.js can trigger the initial load of contacts
    // after a successful login or page load if the user is authenticated (and is NOT admin).
    async function loadUserContacts() { // Make async as it uses await fetchWithAuth
        console.log("[kitchensink.js] Loading user contacts...");
        // Use the shared getCurrentUser helper from auth.js to get the username for the welcome message
        // Ensure window.getCurrentUser is available (exposed by auth.js)
         const currentUser = window.getCurrentUser ? window.getCurrentUser() : null;
         const username = currentUser?.username || 'User'; // Use optional chaining for safety
         if (welcomeMessageElement) {
             welcomeMessageElement.textContent = `Hello ${username}, welcome to the MongoDB Kitchensink application!`;
         } else {
              console.warn("[kitchensink.js] Welcome message element #welcomeMessage not found.");
         }

        try {
            // Clear current list immediately when starting load
            if (contactsList) contactsList.innerHTML = '<tr><td colspan="5" class="text-center">Loading contacts...</td></tr>';
             // Hide no contacts message and show table structure when loading
             if (noContactsMessage) noContactsMessage.style.display = 'none';
             if (contactsTable) contactsTable.style.display = 'table';


            // ** Use the shared fetchWithAuth function from auth.js **
            // Ensure window.fetchWithAuth is available (exposed by auth.js)
            const contacts = await (window.fetchWithAuth ? window.fetchWithAuth('/api/kitchensink/contacts') : Promise.reject("fetchWithAuth not available"));

            console.log("[kitchensink.js] Received contacts:", contacts);
            renderContacts(contacts || []); // Render the fetched contacts (or empty array if null/undefined)
        } catch (error) {
            console.error('[kitchensink.js] Error loading contacts:', error);
            // Display error message to the user
            const displayMessage = `Failed to load contacts: ${error.message || error}`; // Use error message if available
            if (contactsList) contactsList.innerHTML = `<tr><td colspan="5" class="text-danger">${displayMessage}</td></tr>`;
            // Ensure message and table display are consistent with error state
             if (noContactsMessage) noContactsMessage.style.display = 'none'; // Ensure message is hidden
             if (contactsTable) contactsTable.style.display = 'table'; // Keep table structure visible for the error msg
             // Optionally show the "no contacts" message if the error indicates empty data or specific status
             // if (error.status === 404) { // Example: if backend returns 404 for no data (less common for lists)
             //      if (noContactsMessage) { noContactsMessage.style.display = 'block'; if (contactsTable) contactsTable.style.display = 'none'; }
             // }
        }
    }

    function renderContacts(contacts) {
        console.log("[kitchensink.js] Rendering contacts:", contacts);
        if (!contactsList) {
            console.error("[kitchensink.js] contactsList element not found");
            return;
        }
        contactsList.innerHTML = ''; // Clear previous entries

        if (!Array.isArray(contacts) || contacts.length === 0) {
             console.log("[kitchensink.js] No contacts found to render.");
            if (noContactsMessage) noContactsMessage.style.display = 'block'; // Show the "no contacts" message
            if (contactsTable) contactsTable.style.display = 'none'; // Hide the table if empty
            return;
        }

        console.log(`[kitchensink.js] Rendering ${contacts.length} contacts.`);
        if (noContactsMessage) noContactsMessage.style.display = 'none'; // Hide message if contacts are present
        if (contactsTable) contactsTable.style.display = 'table'; // Ensure table is displayed


        contacts.forEach(contact => {
            const row = contactsList.insertRow(); // More robust way to add rows

            row.innerHTML = `
                <td>${contact.name || 'N/A'}</td>
                <td>${contact.email || 'N/A'}</td>
                <td>${contact.phoneNumber || 'N/A'}</td>
                <td>${contact.createdAt ? new Date(contact.createdAt).toLocaleDateString() : 'N/A'}</td>
                 <td>
                     <button class="btn btn-sm btn-primary edit-contact me-1" data-id="${contact.id}" title="Edit Contact">
                         <i class="fas fa-edit"></i> Edit
                     </button>
                     <button class="btn btn-sm btn-danger delete-contact" data-id="${contact.id}" title="Delete Contact">
                         <i class="fas fa-trash"></i> Delete
                     </button>
                 </td>
            `;
             // No need to add individual listeners here thanks to event delegation on contactsList
        });
    }

    async function editContact(contactId) {
        console.log(`[kitchensink.js] Editing contact ID: ${contactId}`);
         try {
             // ** Use the shared fetchWithAuth function from auth.js **
             const contact = await (window.fetchWithAuth ? window.fetchWithAuth(`/api/kitchensink/contacts/${contactId}`) : Promise.reject("fetchWithAuth not available"));
             if (!contact) {
                 throw new Error("Contact data not received.");
             }
            console.log("[kitchensink.js] Received contact data for edit:", contact);

            // Populate form with contact data
            contactIdInput.value = contact.id;
            contactNameInput.value = contact.name;
            contactEmailInput.value = contact.email;
            contactPhoneInput.value = contact.phoneNumber;

            // Set form type for update
            contactForm.dataset.type = 'update';

            document.getElementById('contactModalTitle').textContent = 'Edit Contact'; // Set modal title for editing

            // Explicitly show phone and hide roles for CONTACTS form in the modal
            if (contactPhoneGroup) contactPhoneGroup.style.display = ''; // Show phone section
            if (userRolesGroup) userRolesGroup.style.display = 'none'; // Hide roles section

            contactModal.show(); // Show the modal
        } catch (error) {
            console.error(`[kitchensink.js] Error loading contact ${contactId} for editing:`, error);
            alert(`Error loading contact details: ${error.message}`); // Error message includes backend message or status
        }
    }

    async function saveContact() {
        // Get form values
        const contactId = contactIdInput.value;
        const name = contactNameInput.value;
        const email = contactEmailInput.value;
        const phoneNumber = contactPhoneInput.value;
        const formType = contactForm.dataset.type;

        console.log(`[kitchensink.js] Saving contact. ID: ${contactId}, Type: ${formType}`);

        // Validate form
        // Pass values to validation function as it expects them
        if (!validateContactForm(name, email, phoneNumber)) {
             console.warn("[kitchensink.js] Validation failed. Aborting save.");
            return;
        }

        // Create contact object
        const contactData = {
            name,
            email,
            phoneNumber
        };

        let url = '/api/kitchensink/contacts';
        let method = 'POST';

        if (formType === 'update' && contactId) {
             url = `/api/kitchensink/contacts/${contactId}`;
             method = 'PUT';
             // Include ID in body for robustness, depending on backend (optional if backend gets from path)
             // contactData.id = contactId;
        } else if (formType === 'create' && contactId) {
            console.error("[kitchensink.js] Logic Error: Create form unexpectedly has a contact ID.");
            //alert("An internal error occurred during contact creation."); // Provide user feedback
            return;
        } else if (formType !== 'create' && formType !== 'update') {
            console.error("[kitchensink.js] Logic Error: Unknown form type:", formType);
            //alert("An internal error occurred saving contact."); // Provide user feedback
            return;
        }


        try {
            console.log(`[kitchensink.js] Calling fetchWithAuth for ${url} with method ${method}`);
            // ** Use the shared fetchWithAuth function from auth.js **
            // Ensure window.fetchWithAuth is available (exposed by auth.js)
            const response = await (window.fetchWithAuth ? window.fetchWithAuth(url, {
                method: method,
                body: JSON.stringify(contactData)
            }) : Promise.reject("fetchWithAuth not available"));

            console.log(`[kitchensink.js] fetchWithAuth completed. Response:`, response);


            contactModal.hide(); // Hide the modal
            loadUserContacts(); // Reload contacts list using the function exposed below
            alert(`Contact ${formType === 'create' ? 'created' : 'updated'} successfully!`); // Show success message

        } catch (error) {
            console.error(`[kitchensink.js] Catch block caught error during save:`, error);
            alert(`Error ${formType === 'create' ? 'creating' : 'updating'} contact: ${error.message}`); // Error message includes backend message or status
             // Optionally keep modal open on error
        }
    }

    // Validation function remains the same, specific to the Contact fields in the modal
    function validateContactForm(name, email, phone) {
        let isValid = true;

        // Get element references here for adding validation classes
        const nameEl = document.getElementById('contactName');
        const emailEl = document.getElementById('contactEmail');
        const phoneEl = document.getElementById('contactPhone'); // The phone input

        // Reset previous validation styles and feedback messages
        [nameEl, emailEl, phoneEl, contactCountryCodeSelect].forEach(el => {
             if(el) el.classList.remove('is-invalid');
        });
         if(contactPhoneInvalidFeedback) contactPhoneInvalidFeedback.textContent = ''; // Clear previous phone message
          [nameEl, emailEl].forEach(input => { // Clear feedback for name/email if they have next sibling feedback elements
               const feedback = input ? input.nextElementSibling : null;
               if(feedback && feedback.classList.contains('invalid-feedback')) feedback.textContent = '';
          });


        // Validate name (Using regex from your code)
        const nameRegex = /^[A-Za-z]|[A-Za-z][A-Za-z\s]*[A-Za-z]$/;
        // Ensure name is not empty/whitespace and meets length/regex requirements
        if (!name || name.trim().length === 0 || name.length < 2 || name.length > 100 || (name.trim().length > 0 && !nameRegex.test(name.trim()))) {
            isValid = false;
            if(nameEl) {
                 nameEl.classList.add('is-invalid');
                 const feedbackEl = nameEl.nextElementSibling; // Assuming feedback is next sibling
                 if(feedbackEl && feedbackEl.classList.contains('invalid-feedback')) {
                     if (!name || name.trim().length === 0) feedbackEl.textContent = 'Name is required.';
                     else if (name.length < 2) feedbackEl.textContent = 'Name must be at least 2 characters.';
                     else if (name.length > 100) feedbackEl.textContent = 'Name must be less than 100 characters.';
                     else feedbackEl.textContent = 'Name can only contain alphabets and spaces between words.';
                 } else {
                      console.warn("[kitchensink.js] Validation feedback element not found for contact name.");
                 }
            } else {
                 console.warn("[kitchensink.js] Contact name input element not found for validation.");
            }
        }


        // Validate email (Using regex from your code)
        const emailRegex = /^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/; // Example regex
         // Email is optional, but if provided, must be valid format
         const feedbackEl = emailEl.nextElementSibling;
         if (!email || email.trim().length === 0){
         console.warn("[kitchensink.js] Validation feedback element not found for contact email.");
         feedbackEl.textContent = 'Email is empty';
         }
        if (email && email.trim().length > 0 && !emailRegex.test(email.trim())) {
            isValid = false;
            if(emailEl) {
                 emailEl.classList.add('is-invalid');
                 // Assuming feedback is next sibling
                 if(feedbackEl && feedbackEl.classList.contains('invalid-feedback')) {
                    feedbackEl.textContent = 'Please enter a valid email address.';
                 } else {
                      console.warn("[kitchensink.js] Validation feedback element not found for contact email.");
                 }
            } else {
                 console.warn("[kitchensink.js] Contact email input element not found for validation.");
            }
        }


        // Validate phone number based on country code
        // Only validate if the phone group is visible (i.e., this is a contact form)
        const isPhoneFieldVisible = contactPhoneGroup && contactPhoneGroup.style.display !== 'none';

        if (isPhoneFieldVisible && phoneEl && contactCountryCodeSelect && contactPhoneInvalidFeedback) { // Check all elements exist
            const selectedOption = contactCountryCodeSelect.options[contactCountryCodeSelect.selectedIndex];
            const countryRegex = selectedOption.dataset.regex;
            const countryCode = selectedOption.value; // e.g. "+91"
            const countryLength = selectedOption.dataset.length ? parseInt(selectedOption.dataset.length) : null; // e.g. 10
            const defaultPhoneFeedback = 'Please enter a valid phone number.';

            const trimmedPhone = phone ? phone.trim() : '';

            let phoneIsValid = true;
            let phoneFeedbackMessage = defaultPhoneFeedback;


            if (!selectedOption.value) {
                // No country code selected
                phoneIsValid = false;
                phoneFeedbackMessage = 'Please select a country code.';
                 if(contactCountryCodeSelect) contactCountryCodeSelect.classList.add('is-invalid');
                if(phoneEl) phoneEl.classList.add('is-invalid');
            } else if (!trimmedPhone) {
                // Country selected but phone number is empty (assuming required if country selected)
                 phoneIsValid = false;
                 phoneFeedbackMessage = 'Phone number is required for the selected country.';
                 if(phoneEl) phoneEl.classList.add('is-invalid');
            }
             else if (countryRegex && countryLength !== null) {
                 // Validate using provided length
                 if (countryCode === '+91') {
                    const indiaRegexHardcoded = /^[6789]\d{9}$/;
                    const hardcodedTestResult = indiaRegexHardcoded.test(trimmedPhone);
                    console.log(`[Validation Debug] Hardcoded India Regex Test Result: ${hardcodedTestResult}`);
                    if (!hardcodedTestResult) { // If hardcoded test fails, the input is likely incorrect
                                              phoneIsValid = false;
                                              phoneFeedbackMessage = selectedOption.dataset.note || `Please check the 10-digit number starting with 6, 7, 8, or 9.`;
                                              if(phoneEl) phoneEl.classList.add('is-invalid');
                                         }
                 }
                  if (trimmedPhone.length !== countryLength) {
                      phoneIsValid = false;
                       phoneFeedbackMessage = `Phone number must be exactly ${countryLength} digits long for ${selectedOption.textContent.trim().split('(')[0]}.`;
                      if(phoneEl) phoneEl.classList.add('is-invalid');
                  }
              } else {
                  // Basic check if no regex or length is provided for the country
                  const basicPhoneRegex = /^\+?[0-9\s()-]{7,}$/; // Basic check for at least 7 digits and common phone chars
                  if (!basicPhoneRegex.test(trimmedPhone)) {
                      phoneIsValid = false;
                      phoneFeedbackMessage = defaultPhoneFeedback;
                      if(phoneEl) phoneEl.classList.add('is-invalid');
                  }
              }

              if (!phoneIsValid) {
                 isValid = false; // Overall form is invalid
                 if(contactPhoneInvalidFeedback) contactPhoneInvalidFeedback.textContent = phoneFeedbackMessage;
              } else {
                 // Phone is valid, clear feedback
                 if(contactPhoneInvalidFeedback) contactPhoneInvalidFeedback.textContent = '';
              }
         } else if (isPhoneFieldVisible) {
             // Phone group is visible but some expected elements not found - log error, assume invalid
              console.error("[kitchensink.js] Phone validation elements (input, select, feedback) not found when phone group is visible.");
              isValid = false; // Treat as invalid if validation cannot be performed
              if(contactPhoneInvalidFeedback) contactPhoneInvalidFeedback.textContent = 'Validation elements missing.';
         }
         // If phone group is NOT visible (e.g., User form), this validation block is skipped, which is correct.


         return isValid; // Return overall validity
     }


    function confirmDeleteContact(contactId) {
        console.log(`[kitchensink.js] Confirming delete for contact ID: ${contactId}`);
        contactToDeleteId = contactId; // Set the ID of the contact to delete
        const deleteModalBody = deleteModalElement.querySelector('.modal-body');
         if (deleteModalBody) {
             deleteModalBody.textContent = `Are you sure you want to delete this contact? This action cannot be undone.`; // Set confirmation message
         }
        deleteModal.show(); // Show the delete confirmation modal
    }

    async function deleteContact(contactId) {
        console.log(`[kitchensink.js] Deleting contact ID: ${contactId}`);
         try {
            console.log(`[kitchensink.js] Calling fetchWithAuth for DELETE /api/kitchensink/contacts/${contactId}`);
             // ** Use the shared fetchWithAuth function from auth.js **
            await (window.fetchWithAuth ? window.fetchWithAuth(`/api/kitchensink/contacts/${contactId}`, {
                 method: 'DELETE'
             }) : Promise.reject("fetchWithAuth not available"));

            console.log(`[kitchensink.js] fetchWithAuth completed for delete.`);


            deleteModal.hide(); // Hide confirmation modal
            contactToDeleteId = null; // Reset state
            loadUserContacts(); // Reload contacts list using the function exposed below
            alert('Contact deleted successfully.'); // Show success message

        } catch (error) {
            console.error(`[kitchensink.js] Catch block caught error during delete:`, error);
            alert(`Error deleting contact: ${error.message}`); // Error message includes backend message or status
            deleteModal.hide(); // Also hide modal on error
            contactToDeleteId = null; // Reset state
        }
    }

    // Reset form function adapted to clear hidden ID and set default display
    // This function is used for the shared modal when adding a new contact.
    function resetContactForm() {
        console.log("[kitchensink.js] Resetting contact form.");
        contactForm.reset(); // Reset form inputs
        contactIdInput.value = ''; // Clear the hidden ID input
        contactForm.dataset.type = ''; // Clear the form type dataset attribute
        document.getElementById('contactModalTitle').textContent = 'Contact'; // Reset modal title

        // Reset validation styling on form elements (referencing the reused input IDs)
        const formInputs = [contactNameInput, contactEmailInput, contactPhoneInput, contactCountryCodeSelect];
        formInputs.forEach(input => {
             if(input) input.classList.remove('is-invalid');
        });

        // Clear feedback messages next to inputs
         [contactNameInput, contactEmailInput].forEach(input => {
              const feedback = input ? input.nextElementSibling : null;
              if(feedback && feedback.classList.contains('invalid-feedback')) feedback.textContent = '';
         });
         if(contactPhoneInvalidFeedback) contactPhoneInvalidFeedback.textContent = ''; // Clear phone specific feedback


        // IMPORTANT: Set default display for a blank form (like Add Contact)
        // This ensures phone is shown and roles are hidden when starting a new form for a contact.
        if (contactPhoneGroup) contactPhoneGroup.style.display = ''; // Show phone section
        if (userRolesGroup) userRolesGroup.style.display = 'none'; // Hide roles section
         // Also reset role select options defensively (shouldn't be visible for contacts)
        const userRoleSelect = document.getElementById('userRoleSelect');
         if (userRoleSelect) {
             userRoleSelect.innerHTML = ''; // Clear options
             userRoleSelect.value = ''; // Ensure empty value
         }
    }


    // Expose functions so auth.js can call them after login
    // The loadUserContacts function is needed by auth.js to trigger the initial load
    // when a regular user logs in or the page loads with existing user tokens.
    window.loadUserContacts = loadUserContacts; // Expose globally


});