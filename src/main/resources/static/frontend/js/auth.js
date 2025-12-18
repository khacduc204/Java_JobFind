document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.toggle-password').forEach(button => {
        button.addEventListener('click', () => {
            const input = button.parentElement.querySelector('.js-password');
            const icon = button.querySelector('i');
            if (!input) {
                return;
            }
            const isMasked = input.type === 'password';
            input.type = isMasked ? 'text' : 'password';
            if (icon) {
                icon.classList.toggle('fa-eye', !isMasked);
                icon.classList.toggle('fa-eye-slash', isMasked);
            }
        });
    });
});
