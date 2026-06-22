/*
 * Modal
 *
 * Pico.css - https://picocss.com
 * Copyright 2019-2024 - Licensed under MIT
 */

// Config
const isOpenClass = "modal-is-open";
const openingClass = "modal-is-opening";
const closingClass = "modal-is-closing";
const scrollbarWidthCssVar = "--pico-scrollbar-width";
const animationDuration = 400; // ms
let visibleModal = null;

// Toggle modal
const toggleModal = (event) => {
  event.preventDefault();
  const modal = document.getElementById(event.currentTarget.dataset.target);
  if (!modal) return;
  modal && (modal.open ? closeModal(modal) : openModal(modal));
};

// Open modal
const openModal = (modal) => {
  const { documentElement: html } = document;
  const scrollbarWidth = getScrollbarWidth();
  if (scrollbarWidth) {
    html.style.setProperty(scrollbarWidthCssVar, `${scrollbarWidth}px`);
  }
  html.classList.add(isOpenClass, openingClass);
  setTimeout(() => {
    visibleModal = modal;
    html.classList.remove(openingClass);
  }, animationDuration);
  modal.showModal();
};

// Close modal
const closeModal = (modal) => {
  visibleModal = null;
  const { documentElement: html } = document;
  html.classList.add(closingClass);
  setTimeout(() => {
    html.classList.remove(closingClass, isOpenClass);
    html.style.removeProperty(scrollbarWidthCssVar);
    modal.close();
  }, animationDuration);
};

// Close with a click outside
document.addEventListener("click", (event) => {
  if (visibleModal === null) return;
  const modalContent = visibleModal.querySelector("article");
  const isClickInside = modalContent.contains(event.target);
  !isClickInside && closeModal(visibleModal);
});

// Close with Esc key
document.addEventListener("keydown", (event) => {
  if (event.key === "Escape" && visibleModal) {
    closeModal(visibleModal);
  }
});

// Get scrollbar width
const getScrollbarWidth = () => {
  const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth;
  return scrollbarWidth;
};

// Is scrollbar visible
const isScrollbarVisible = () => {
  return document.body.scrollHeight > screen.height;
};

// Used to open and know what role should be assigned to a specific user
function openRoleModal(event, buttonElement) {
  const userEmail = buttonElement.getAttribute('data-email');

  document.getElementById('assignRole-modal-email').value = userEmail;

  toggleModal(event);
}

// Used to open and know what role should be removed for a specific user
function openRemoveRoleModal(event, buttonElement) {
  const userEmail = buttonElement.getAttribute('data-email');
  const userRoles = buttonElement.getAttribute('data-roles');

  document.getElementById('removeRole-modal-email').value = userEmail;
  const tbody = document.getElementById('remove-role-tbody');
  tbody.innerHTML = '';

  if (userRoles && userRoles.trim() !== '') {
    const roles = userRoles.split(',');

    roles.forEach(role => {
      const cleanRole = role.trim();
      const tr = document.createElement('tr');

      tr.innerHTML = `
            <td>${cleanRole}</td>
            <td>
              <button type="submit" name="role" value="${cleanRole}">
                Remove
              </button>
            </td>
          `;
      tbody.appendChild(tr);
    });
  } else {
    tbody.innerHTML = '<tr><td colspan="2">No roles assigned.</td></tr>';
  }

  toggleModal(event);
}

function openConfirmRemoveStudyLineModal (event, buttonElement) {
  const userEmail = buttonElement.getAttribute('data-email');

  document.getElementById('confirm-remove-user-email').value = userEmail;

  toggleModal(event);
}

function openConfirmDeleteUserModal(event, buttonElement) {
  const userEmail = buttonElement.getAttribute('data-email');

  document.getElementById('confirm-delete-user-email').value = userEmail;

  toggleModal(event);
}

function openConfirmAdminUserModal(event, buttonElement) {
  const userEmail = buttonElement.getAttribute('data-email');

  document.getElementById('confirm-admin-user-email').value = userEmail;

  toggleModal(event);
}