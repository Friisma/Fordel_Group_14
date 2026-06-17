document.addEventListener('DOMContentLoaded', function () {
    var popups = document.querySelectorAll('.flash-popup');

    if (!popups.length) {
        return;
    }

    popups.forEach(function (popup) {
        var hideAfterMs = Number(popup.getAttribute('data-autohide-ms') || '4000');

        window.setTimeout(function () {
            popup.classList.add('is-hiding');

            window.setTimeout(function () {
                popup.remove();
            }, 250);
        }, hideAfterMs);
    });
});
