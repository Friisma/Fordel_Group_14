/*
if you are reading this javascript file, 
you are probably wondering why i did not do this with java and thymeleaf.

it's done this way for two reasons:
1. my experience with react, typescript and javascript in general is much greater than my experience with java, and the deadline if approaching fast.
2. this is a very small feature which requires updating the DOM in real time. doing this with java and thymeleaf would require a lot of extra work, and would not be as smooth as with javascript.

enjoy the javascript magic...
*/

document.addEventListener('DOMContentLoaded', function () {
    var form = document.getElementById('change-password-form');
    var openButton = document.getElementById('open-update-password-modal');
    var confirmButton = document.getElementById('update-password-button');

    if (!form || !openButton || !confirmButton) {
        return;
    }

    var currentPassword = form.elements.namedItem('currentPassword');
    var newPassword = form.elements.namedItem('newPassword');
    var confirmNewPassword = form.elements.namedItem('confirmNewPassword');

    function showValidationPopup(message) {
        var existingPopup = document.querySelector('[data-password-mismatch-popup="true"]');
        if (existingPopup) {
            existingPopup.remove();
        }

        var popup = document.createElement('div');
        popup.className = 'flash-popup error';
        popup.setAttribute('role', 'alert');
        popup.setAttribute('aria-live', 'assertive');
        popup.setAttribute('data-autohide-ms', '5000');
        popup.setAttribute('data-password-mismatch-popup', 'true');
        popup.innerHTML = '<p>' + message + '</p>';
        document.body.appendChild(popup);

        window.setTimeout(function () {
            popup.classList.add('is-hiding');

            window.setTimeout(function () {
                popup.remove();
            }, 250);
        }, 5000);
    }

    function updateConfirmFieldState() {
        if (!(currentPassword instanceof HTMLInputElement) || !(newPassword instanceof HTMLInputElement) || !(confirmNewPassword instanceof HTMLInputElement)) {
            return;
        }

        if (!newPassword.value) {
            newPassword.removeAttribute('aria-invalid');
            newPassword.setCustomValidity('');
            confirmNewPassword.removeAttribute('aria-invalid');
            confirmNewPassword.setCustomValidity('');
            return;
        }

        if (newPassword.value === currentPassword.value) {
            newPassword.setAttribute('aria-invalid', 'true');
            newPassword.setCustomValidity('New password must be different from your current password');
            confirmNewPassword.setAttribute('aria-invalid', 'true');
            confirmNewPassword.setCustomValidity('New password must be different from your current password');
            return;
        }

        if (!confirmNewPassword.value) {
            newPassword.setAttribute('aria-invalid', 'false');
            confirmNewPassword.removeAttribute('aria-invalid');
            confirmNewPassword.setCustomValidity('');
            return;
        }

        if (newPassword.value !== confirmNewPassword.value) {
            newPassword.setAttribute('aria-invalid', 'false');
            confirmNewPassword.setAttribute('aria-invalid', 'true');
            confirmNewPassword.setCustomValidity('Passwords do not match');
            return;
        }

        newPassword.setAttribute('aria-invalid', 'false');
        confirmNewPassword.setAttribute('aria-invalid', 'false');
        newPassword.setCustomValidity('');
        confirmNewPassword.setCustomValidity('');
    }

    openButton.addEventListener('click', function (event) {
        updateConfirmFieldState();

        if (!(currentPassword instanceof HTMLInputElement) || !(newPassword instanceof HTMLInputElement) || !(confirmNewPassword instanceof HTMLInputElement)) {
            return;
        }

        if (newPassword.value === currentPassword.value) {
            event.preventDefault();
            showValidationPopup('New password must be different from your current password');
            return;
        }

        toggleModal(event);
    });

    if (newPassword instanceof HTMLInputElement) {
        newPassword.addEventListener('input', updateConfirmFieldState);
    }

    if (confirmNewPassword instanceof HTMLInputElement) {
        confirmNewPassword.addEventListener('input', updateConfirmFieldState);
    }

    if (currentPassword instanceof HTMLInputElement) {
        currentPassword.addEventListener('input', updateConfirmFieldState);
    }

    updateConfirmFieldState();

    confirmButton.addEventListener('click', function (event) {
        updateConfirmFieldState();

        if (!(currentPassword instanceof HTMLInputElement) || !(newPassword instanceof HTMLInputElement) || !(confirmNewPassword instanceof HTMLInputElement)) {
            return;
        }

        if (newPassword.value !== confirmNewPassword.value) {
            event.preventDefault();
            showValidationPopup('Passwords do not match');
            return;
        }
    });
});