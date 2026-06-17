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
    var button = document.getElementById('update-password-button');

    if (!form || !button) {
        return;
    }

    var newPassword = form.elements.namedItem('newPassword');
    var confirmNewPassword = form.elements.namedItem('confirmNewPassword');

    function showPasswordMismatchPopup() {
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
        popup.innerHTML = '<p>Passwords do not match</p>';
        document.body.appendChild(popup);

        window.setTimeout(function () {
            popup.classList.add('is-hiding');

            window.setTimeout(function () {
                popup.remove();
            }, 250);
        }, 5000);
    }

    function updateConfirmFieldState() {
        if (!(newPassword instanceof HTMLInputElement) || !(confirmNewPassword instanceof HTMLInputElement)) {
            return;
        }

        if (newPassword.value) {
            newPassword.setAttribute('aria-invalid', 'false');
        } else {
            newPassword.removeAttribute('aria-invalid');
        }

        if (!confirmNewPassword.value) {
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
        confirmNewPassword.setCustomValidity('');
    }

    if (newPassword instanceof HTMLInputElement) {
        newPassword.addEventListener('input', updateConfirmFieldState);
    }

    if (confirmNewPassword instanceof HTMLInputElement) {
        confirmNewPassword.addEventListener('input', updateConfirmFieldState);
    }

    updateConfirmFieldState();

    button.addEventListener('click', function (event) {
        updateConfirmFieldState();

        if (!(newPassword instanceof HTMLInputElement) || !(confirmNewPassword instanceof HTMLInputElement)) {
            return;
        }

        if (newPassword.value !== confirmNewPassword.value) {
            showPasswordMismatchPopup();
            return;
        }

        toggleModal(event);
    });
});