document.addEventListener('DOMContentLoaded', function () {
    var cookiesPopup = document.getElementById('cookies-popup');
    var acceptCookiesButton = document.getElementById('accept-cookies');

    if (!cookiesPopup || !acceptCookiesButton) {
        return;
    }

    var cookiesAccepted = false;

    try {
        cookiesAccepted = localStorage.getItem('cookiesAccepted') === 'true';
    } catch (error) {
        cookiesAccepted = false;
    }

    cookiesPopup.hidden = cookiesAccepted;

    acceptCookiesButton.addEventListener('click', function () {
        try {
            localStorage.setItem('cookiesAccepted', 'true');
        } catch (error) {
            // Ignore storage errors and still dismiss the banner.
        }

        cookiesPopup.hidden = true;
    });
});
