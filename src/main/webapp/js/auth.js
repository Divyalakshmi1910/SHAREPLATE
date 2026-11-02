/**
 * SharePlate - Authentication & User Registration Controller
 */

document.addEventListener('DOMContentLoaded', () => {
    // -------------------------------------------------------------
    // LOGIN PAGE LOGIC
    // -------------------------------------------------------------
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const emailInput = document.getElementById('login-email');
            const passwordInput = document.getElementById('login-password');
            const submitBtn = loginForm.querySelector('button[type="submit"]');

            const email = emailInput.value.trim();
            const password = passwordInput.value;

            if (!email || !password) {
                SharePlateApi.showToast('Please enter both your email and password.', 'error');
                return;
            }

            try {
                submitBtn.disabled = true;
                submitBtn.innerText = 'Signing In...';

                const user = await SharePlateApi.login(email, password);
                SharePlateApi.showToast(`Welcome back, ${user.name}! Redirecting...`, 'success');

                setTimeout(() => {
                    if (user.role === 'DONOR') {
                        window.location.href = 'donor-dashboard.html';
                    } else if (user.role === 'NGO') {
                        window.location.href = 'ngo-dashboard.html';
                    } else if (user.role === 'ADMIN') {
                        window.location.href = 'admin-dashboard.html';
                    } else {
                        window.location.href = 'index.html';
                    }
                }, 800);
            } catch (err) {
                SharePlateApi.showToast(err.message || 'Login failed. Please verify credentials.', 'error');
                submitBtn.disabled = false;
                submitBtn.innerText = 'Log In to SharePlate';
            }
        });

        // Quick-Fill Demo Buttons
        document.querySelectorAll('.btn-quick-fill').forEach(btn => {
            btn.addEventListener('click', () => {
                const email = btn.getAttribute('data-email');
                const password = btn.getAttribute('data-password');
                const emailInput = document.getElementById('login-email');
                const passwordInput = document.getElementById('login-password');
                if (emailInput && passwordInput) {
                    emailInput.value = email;
                    passwordInput.value = password;
                    SharePlateApi.showToast(`Filled credentials for ${btn.innerText.trim()}`, 'success');
                }
            });
        });
    }

    // -------------------------------------------------------------
    // REGISTRATION PAGE LOGIC
    // -------------------------------------------------------------
    const registerForm = document.getElementById('register-form');
    if (registerForm) {
        // Role Selection Pill Radios
        const roleOptions = document.querySelectorAll('.role-option');
        const roleInputs = document.querySelectorAll('input[name="userRole"]');
        const orgTypeSelect = document.getElementById('reg-org-type');

        function updateRoleSelection(selectedRole) {
            roleOptions.forEach(opt => {
                const radio = opt.querySelector('input[type="radio"]');
                if (radio.value === selectedRole) {
                    opt.classList.add('active');
                    radio.checked = true;
                } else {
                    opt.classList.remove('active');
                }
            });

            // Adjust organization types according to role
            if (orgTypeSelect) {
                orgTypeSelect.innerHTML = '';
                if (selectedRole === 'DONOR') {
                    const donorTypes = [
                        'Restaurant / Cafe',
                        'College / University Canteen',
                        'Corporate Cafeteria',
                        'Event / Wedding Caterer',
                        'Bakery / Pastry Shop',
                        'Supermarket / Grocery Store',
                        'Individual Food Donor'
                    ];
                    donorTypes.forEach(t => {
                        const opt = document.createElement('option');
                        opt.value = t;
                        opt.textContent = t;
                        orgTypeSelect.appendChild(opt);
                    });
                } else {
                    const ngoTypes = [
                        'Registered Non-Profit (NGO)',
                        'Community Food Bank',
                        'Homeless Shelter',
                        'Disaster Relief Organization',
                        'Community Kitchen',
                        'Youth & Child Welfare Home',
                        'Elderly Care Caretaker'
                    ];
                    ngoTypes.forEach(t => {
                        const opt = document.createElement('option');
                        opt.value = t;
                        opt.textContent = t;
                        orgTypeSelect.appendChild(opt);
                    });
                }
            }
        }

        roleOptions.forEach(opt => {
            opt.addEventListener('click', () => {
                const radio = opt.querySelector('input[type="radio"]');
                updateRoleSelection(radio.value);
            });
        });

        // Initialize default role
        updateRoleSelection('DONOR');

        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const name = document.getElementById('reg-name').value.trim();
            const email = document.getElementById('reg-email').value.trim();
            const password = document.getElementById('reg-password').value;
            const confirmPassword = document.getElementById('reg-confirm-password').value;
            const phone = document.getElementById('reg-phone').value.trim();
            const address = document.getElementById('reg-address').value.trim();
            const organizationType = orgTypeSelect ? orgTypeSelect.value : 'Individual';

            const activeRoleRadio = document.querySelector('input[name="userRole"]:checked');
            const role = activeRoleRadio ? activeRoleRadio.value : 'DONOR';

            if (!name || !email || !password || !phone || !address) {
                SharePlateApi.showToast('Please fill out all required fields.', 'error');
                return;
            }

            if (password.length < 6) {
                SharePlateApi.showToast('Password must be at least 6 characters long.', 'error');
                return;
            }

            if (password !== confirmPassword) {
                SharePlateApi.showToast('Passwords do not match. Please verify.', 'error');
                return;
            }

            const submitBtn = registerForm.querySelector('button[type="submit"]');

            try {
                submitBtn.disabled = true;
                submitBtn.innerText = 'Creating Account...';

                const newUser = await SharePlateApi.register({
                    name,
                    email,
                    password,
                    role,
                    organizationType,
                    phone,
                    address
                });

                SharePlateApi.showToast(`Account created successfully! Welcome, ${newUser.name}.`, 'success');

                setTimeout(() => {
                    if (role === 'DONOR') {
                        window.location.href = 'donor-dashboard.html';
                    } else {
                        window.location.href = 'ngo-dashboard.html';
                    }
                }, 800);
            } catch (err) {
                SharePlateApi.showToast(err.message || 'Registration failed.', 'error');
                submitBtn.disabled = false;
                submitBtn.innerText = 'Complete Registration';
            }
        });
    }
});
