
const termsCheckbox = document.getElementById('terms');
const loginBtn = document.getElementById('login-btn');

termsCheckbox.addEventListener('change', function() {
    loginBtn.disabled = !this.checked;
});