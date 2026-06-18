document.addEventListener('DOMContentLoaded', function () {
    var modal = document.getElementById('modal-password-expiry-reminder');
    var trigger = document.getElementById('open-password-expiry-reminder');

    if (!modal || !trigger) {
        return;
    }

    if (modal.dataset.show !== 'true') {
        return;
    }

    trigger.click();
});