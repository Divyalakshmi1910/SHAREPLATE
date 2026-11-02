/**
 * SharePlate - Unified API & Data Service
 * 
 * Directly connects the frontend forms, buttons, and dashboards to the
 * Java 17 Servlets, JDBC, and MySQL database running on Apache Tomcat 10.1.
 * 
 * Also provides a resilient fallback preview mode for immediate standalone testing.
 */

(function (window) {
    'use strict';

    // Computes dynamic API base depending on deployment context
    function getApiBase() {
        const path = window.location.pathname;
        if (path.startsWith('/shareplate')) {
            return '/shareplate/api';
        }
        return '/api';
    }

    // Sample fallback data (used only when backend server is offline)
    const INITIAL_SAMPLE_USERS = [
        {
            userId: 1, id: 1,
            name: "System Administrator",
            email: "admin@shareplate.org",
            password: "admin123",
            role: "ADMIN",
            phone: "+91 98765 00001"
        },
        {
            userId: 2, id: 2,
            name: "Green Garden Bistro",
            email: "donor@greengarden.com",
            password: "donor123",
            role: "DONOR",
            phone: "+91 98765 11111"
        },
        {
            userId: 3, id: 3,
            name: "City College Central Canteen",
            email: "canteen@citycollege.edu",
            password: "donor123",
            role: "DONOR",
            phone: "+91 98765 22222"
        },
        {
            userId: 5, id: 5,
            name: "Hope Shelter Foundation",
            email: "relief@hopeshelter.org",
            password: "ngo123",
            role: "NGO",
            phone: "+91 98765 44444"
        },
        {
            userId: 6, id: 6,
            name: "Annapurna Care Network",
            email: "coordinator@annapurnacare.org",
            password: "ngo123",
            role: "NGO",
            phone: "+91 98765 55555"
        }
    ];

    function getOffsetIso(hoursOffset) {
        const d = new Date();
        d.setHours(d.getHours() + hoursOffset);
        return d.toISOString();
    }

    const INITIAL_SAMPLE_DONATIONS = [
        {
            donationId: 1, id: 1,
            donorId: 2,
            donorName: "Green Garden Bistro",
            donorPhone: "+91 98765 11111",
            foodName: "Vegetable Biryani & Mixed Dal",
            category: "Cooked Food",
            quantity: "45 meal portions (approx 18 kg)",
            preparationDatetime: getOffsetIso(-2),
            safeCollectionDeadline: getOffsetIso(4),
            collectionDeadline: getOffsetIso(4),
            pickupLocation: "Green Garden Bistro Kitchen Door 2, Park Avenue",
            storageConditions: "Hot Insulated Containers",
            allergenInformation: "Contains Dairy (ghee), no nuts",
            allergenInfo: "Contains Dairy (ghee), no nuts",
            status: "Available"
        },
        {
            donationId: 2, id: 2,
            donorId: 3,
            donorName: "City College Central Canteen",
            donorPhone: "+91 98765 22222",
            foodName: "Steamed Basmati Rice & Paneer Curry",
            category: "Cooked Food",
            quantity: "60 meal boxes",
            preparationDatetime: getOffsetIso(-1),
            safeCollectionDeadline: getOffsetIso(5),
            collectionDeadline: getOffsetIso(5),
            pickupLocation: "Campus Dining Hall A, Loading Dock 1",
            storageConditions: "Refrigerated Storage",
            allergenInformation: "Contains Dairy, Gluten-Free",
            allergenInfo: "Contains Dairy, Gluten-Free",
            status: "Requested"
        },
        {
            donationId: 3, id: 3,
            donorId: 2,
            donorName: "Green Garden Bistro",
            donorPhone: "+91 98765 11111",
            foodName: "Fresh Bakery Breads & Buns",
            category: "Bakery & Bread",
            quantity: "35 loaves & 50 rolls",
            preparationDatetime: getOffsetIso(-4),
            safeCollectionDeadline: getOffsetIso(14),
            collectionDeadline: getOffsetIso(14),
            pickupLocation: "Park Avenue Side Entrance",
            storageConditions: "Room Temperature",
            allergenInformation: "Contains Gluten & Sesame",
            allergenInfo: "Contains Gluten & Sesame",
            status: "Approved"
        }
    ];

    const INITIAL_SAMPLE_REQUESTS = [
        {
            requestId: 1, id: 1,
            donationId: 2,
            ngoId: 5,
            ngoName: "Hope Shelter Foundation",
            ngoPhone: "+91 98765 44444",
            foodName: "Steamed Basmati Rice & Paneer Curry",
            foodQuantity: "60 meal boxes",
            foodCategory: "Cooked Food",
            pickupLocation: "Campus Dining Hall A, Loading Dock 1",
            donorName: "City College Central Canteen",
            donorPhone: "+91 98765 22222",
            beneficiaryCount: 60,
            requestNotes: "Hope Shelter evening meal distribution for 60 displaced workers.",
            requestMessage: "Hope Shelter evening meal distribution for 60 displaced workers.",
            requestDate: getOffsetIso(-1),
            requestStatus: "PENDING",
            status: "PENDING"
        }
    ];

    // Local Demonstration Storage
    class DemoStore {
        static get(key, defaultValue) {
            try {
                const item = localStorage.getItem('shareplate_' + key);
                return item ? JSON.parse(item) : defaultValue;
            } catch (e) {
                return defaultValue;
            }
        }
        static set(key, value) {
            try {
                localStorage.setItem('shareplate_' + key, JSON.stringify(value));
            } catch (e) {
                console.warn('Local storage write warning:', e);
            }
        }
        static init() {
            if (!DemoStore.get('users', null)) DemoStore.set('users', INITIAL_SAMPLE_USERS);
            if (!DemoStore.get('donations', null)) DemoStore.set('donations', INITIAL_SAMPLE_DONATIONS);
            if (!DemoStore.get('requests', null)) DemoStore.set('requests', INITIAL_SAMPLE_REQUESTS);
        }
    }

    DemoStore.init();

    let isLiveBackend = false;

    // Checks connectivity with Tomcat Servlet backend
    async function detectBackend() {
        try {
            const base = getApiBase();
            const resp = await fetch(base + '/auth/me', {
                method: 'GET',
                credentials: 'include'
            });
            // 200 (authenticated) or 401 (unauthenticated but endpoint active) means backend is alive!
            if (resp.status === 200 || resp.status === 401) {
                isLiveBackend = true;
                return true;
            }
        } catch (e) {
            // Server offline or file opened directly in browser
        }
        isLiveBackend = false;
        return false;
    }

    function renderEnvironmentBanner() {
        const existing = document.getElementById('shareplate-env-banner');
        if (existing) existing.remove();

        const banner = document.createElement('div');
        banner.id = 'shareplate-env-banner';
        banner.className = 'env-banner';

        if (isLiveBackend) {
            banner.innerHTML = `
                <div>
                    <span class="badge" style="background:#059669; color:#ffffff; border-color:#34d399;">
                        ● LIVE MYSQL &amp; SERVLETS ACTIVE
                    </span>
                    Connected to Apache Tomcat 10.1 &amp; MySQL (Database: shareplate_db).
                </div>
                <div style="font-size:0.8rem; opacity:0.9;">JDBC Connection: Active</div>
            `;
        } else {
            banner.innerHTML = `
                <div>
                    <span class="badge" style="background:#b45309; color:#ffffff; border-color:#fde68a;">
                        ○ FRONTEND PREVIEW MODE
                    </span>
                    Running frontend demonstration. Start Apache Tomcat 10.1 &amp; MySQL for live database transactions.
                </div>
                <div style="font-size:0.8rem; opacity:0.85;">Tomcat 10.1 / JDBC Ready</div>
            `;
        }

        document.body.insertAdjacentElement('afterbegin', banner);
    }

    const api = {
        async init() {
            await detectBackend();
            renderEnvironmentBanner();

            // If backend is active, sync active session user
            if (isLiveBackend) {
                try {
                    const res = await fetch(getApiBase() + '/auth/me', { credentials: 'include' });
                    if (res.ok) {
                        const data = await res.json();
                        if (data && data.data && data.data.user) {
                            DemoStore.set('current_user', data.data.user);
                        }
                    } else if (res.status === 401) {
                        DemoStore.set('current_user', null);
                    }
                } catch (e) {}
            }

            this.updateNavAuthStatus();
        },

        // --- AUTHENTICATION ---
        async login(email, password) {
            if (isLiveBackend) {
                const res = await fetch(getApiBase() + '/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify({ email, password })
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.message || 'Login failed.');
                const user = data.data.user;
                DemoStore.set('current_user', user);
                return user;
            }

            // Fallback for offline browser preview
            const users = DemoStore.get('users', INITIAL_SAMPLE_USERS);
            const user = users.find(u => u.email.toLowerCase() === email.toLowerCase().trim() && (u.password === password || password === 'donor123' || password === 'ngo123' || password === 'admin123'));
            if (!user) {
                throw new Error('Invalid email or password.');
            }
            const safeUser = { ...user };
            delete safeUser.password;
            delete safeUser.passwordHash;
            DemoStore.set('current_user', safeUser);
            return safeUser;
        },

        async register(userData) {
            if (isLiveBackend) {
                const res = await fetch(getApiBase() + '/auth/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify(userData)
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.message || 'Registration failed.');
                const user = data.data.user;
                DemoStore.set('current_user', user);
                return user;
            }

            // Fallback for offline browser preview
            const users = DemoStore.get('users', INITIAL_SAMPLE_USERS);
            if (users.some(u => u.email.toLowerCase() === userData.email.toLowerCase().trim())) {
                throw new Error('An account with this email address already exists.');
            }
            const newId = users.length > 0 ? Math.max(...users.map(u => u.userId || u.id || 0)) + 1 : 1;
            const newUser = {
                userId: newId, id: newId,
                ...userData,
                email: userData.email.toLowerCase().trim(),
                createdAt: new Date().toISOString()
            };
            users.push(newUser);
            DemoStore.set('users', users);

            const safeUser = { ...newUser };
            delete safeUser.password;
            DemoStore.set('current_user', safeUser);
            return safeUser;
        },

        async logout() {
            if (isLiveBackend) {
                try {
                    await fetch(getApiBase() + '/auth/logout', { 
                        method: 'POST',
                        credentials: 'include'
                    });
                } catch (e) {}
            }
            DemoStore.set('current_user', null);
            window.location.href = 'login.html';
        },

        getCurrentUser() {
            return DemoStore.get('current_user', null);
        },

        requireAuth(allowedRoles) {
            const user = this.getCurrentUser();
            if (!user) {
                window.location.href = 'login.html';
                return null;
            }
            if (allowedRoles && !allowedRoles.includes(user.role)) {
                alert('Access Restricted: Your account role (' + user.role + ') is not authorized for this dashboard.');
                if (user.role === 'DONOR') window.location.href = 'donor-dashboard.html';
                else if (user.role === 'NGO') window.location.href = 'ngo-dashboard.html';
                else if (user.role === 'ADMIN') window.location.href = 'admin-dashboard.html';
                else window.location.href = 'index.html';
                return null;
            }
            return user;
        },

        // --- DONATIONS ---
        async getDonations(filter = {}) {
            if (isLiveBackend) {
                let url = getApiBase() + '/donations';
                const params = new URLSearchParams();
                if (filter.scope) params.append('scope', filter.scope);
                if (filter.donorId) params.append('donorId', filter.donorId);
                if (filter.id) params.append('id', filter.id);
                if (params.toString()) url += '?' + params.toString();

                const res = await fetch(url, { credentials: 'include' });
                const data = await res.json();
                return data.data || [];
            }

            // Fallback
            let list = DemoStore.get('donations', INITIAL_SAMPLE_DONATIONS);
            if (filter.id) {
                return list.find(d => (d.donationId || d.id) === parseInt(filter.id)) || null;
            }
            if (filter.donorId) {
                list = list.filter(d => d.donorId === parseInt(filter.donorId));
            }
            if (filter.scope === 'available') {
                list = list.filter(d => d.status === 'Available');
            }
            return list;
        },

        async getDonationById(id) {
            if (isLiveBackend) {
                const res = await fetch(getApiBase() + '/donations?id=' + id, { credentials: 'include' });
                const data = await res.json();
                return data.data || null;
            }
            const donations = DemoStore.get('donations', INITIAL_SAMPLE_DONATIONS);
            return donations.find(d => (d.donationId || d.id) === parseInt(id)) || null;
        },

        async createDonation(donationData) {
            const user = this.getCurrentUser();
            if (!user) throw new Error('You must be logged in as a Donor to add a donation.');

            if (isLiveBackend) {
                const res = await fetch(getApiBase() + '/donations', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify(donationData)
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.message || 'Failed to save donation in MySQL.');
                return data.data;
            }

            // Fallback
            const donations = DemoStore.get('donations', INITIAL_SAMPLE_DONATIONS);
            const newId = donations.length > 0 ? Math.max(...donations.map(d => d.donationId || d.id || 0)) + 1 : 1;
            const newDonation = {
                donationId: newId, id: newId,
                donorId: user.userId || user.id,
                donorName: user.name,
                donorPhone: user.phone,
                status: 'Available',
                ...donationData
            };
            donations.unshift(newDonation);
            DemoStore.set('donations', donations);
            return newDonation;
        },

        async updateDonation(donationData) {
            if (isLiveBackend) {
                const res = await fetch(getApiBase() + '/donations', {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify(donationData)
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.message || 'Failed to update donation in MySQL.');
                return data.data;
            }

            // Fallback
            const donations = DemoStore.get('donations', INITIAL_SAMPLE_DONATIONS);
            const targetId = parseInt(donationData.donationId || donationData.id);
            const idx = donations.findIndex(d => (d.donationId || d.id) === targetId);
            if (idx === -1) throw new Error('Donation not found.');

            donations[idx] = { ...donations[idx], ...donationData };
            DemoStore.set('donations', donations);
            return donations[idx];
        },

        async deleteDonation(id) {
            if (isLiveBackend) {
                const res = await fetch(getApiBase() + '/donations?id=' + id, { 
                    method: 'DELETE',
                    credentials: 'include'
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.message || 'Failed to delete donation from MySQL.');
                return true;
            }

            // Fallback
            let donations = DemoStore.get('donations', INITIAL_SAMPLE_DONATIONS);
            donations = donations.filter(d => (d.donationId || d.id) !== parseInt(id));
            DemoStore.set('donations', donations);
            return true;
        },

        // --- REQUESTS ---
        async getRequests(filter = {}) {
            if (isLiveBackend) {
                let url = getApiBase() + '/requests';
                const res = await fetch(url, { credentials: 'include' });
                const data = await res.json();
                return data.data || [];
            }

            // Fallback
            let requests = DemoStore.get('requests', INITIAL_SAMPLE_REQUESTS);
            if (filter.ngoId) {
                requests = requests.filter(r => r.ngoId === parseInt(filter.ngoId));
            }
            if (filter.donorId) {
                const donations = DemoStore.get('donations', INITIAL_SAMPLE_DONATIONS);
                const donorDonationIds = donations.filter(d => d.donorId === parseInt(filter.donorId)).map(d => d.donationId || d.id);
                requests = requests.filter(r => donorDonationIds.includes(r.donationId));
            }
            return requests;
        },

        async createRequest(reqData) {
            const user = this.getCurrentUser();
            if (!user) throw new Error('You must be logged in as an NGO to request food.');

            if (isLiveBackend) {
                const res = await fetch(getApiBase() + '/requests', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify(reqData)
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.message || 'Failed to place request.');
                return data.data;
            }

            // Fallback
            const donations = DemoStore.get('donations', INITIAL_SAMPLE_DONATIONS);
            const donation = donations.find(d => (d.donationId || d.id) === parseInt(reqData.donationId));
            if (!donation) throw new Error('Target donation not found.');
            if (donation.status !== 'Available') throw new Error('This donation is no longer available.');

            donation.status = 'Requested';
            DemoStore.set('donations', donations);

            const requests = DemoStore.get('requests', INITIAL_SAMPLE_REQUESTS);
            const newId = requests.length > 0 ? Math.max(...requests.map(r => r.requestId || r.id || 0)) + 1 : 1;
            const newReq = {
                requestId: newId, id: newId,
                donationId: donation.donationId || donation.id,
                ngoId: user.userId || user.id,
                ngoName: user.name,
                ngoPhone: user.phone,
                foodName: donation.foodName,
                foodQuantity: donation.quantity,
                foodCategory: donation.category,
                pickupLocation: donation.pickupLocation,
                donorName: donation.donorName,
                donorPhone: donation.donorPhone,
                requestNotes: reqData.requestNotes || reqData.requestMessage,
                requestMessage: reqData.requestNotes || reqData.requestMessage,
                beneficiaryCount: reqData.beneficiaryCount || 20,
                requestStatus: 'PENDING',
                status: 'PENDING',
                requestDate: new Date().toISOString()
            };
            requests.unshift(newReq);
            DemoStore.set('requests', requests);
            return newReq;
        },

        async updateRequestStatus(requestId, newStatus) {
            if (isLiveBackend) {
                const res = await fetch(getApiBase() + '/requests', {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify({ id: requestId, status: newStatus })
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.message || 'Failed to update request in MySQL.');
                return true;
            }

            // Fallback
            const requests = DemoStore.get('requests', INITIAL_SAMPLE_REQUESTS);
            const req = requests.find(r => (r.requestId || r.id) === parseInt(requestId));
            if (!req) throw new Error('Request not found.');

            req.requestStatus = newStatus.toUpperCase();
            req.status = newStatus.toUpperCase();

            const donations = DemoStore.get('donations', INITIAL_SAMPLE_DONATIONS);
            const donation = donations.find(d => (d.donationId || d.id) === req.donationId);
            if (donation) {
                if (newStatus.toUpperCase() === 'APPROVED') donation.status = 'Approved';
                else if (newStatus.toUpperCase() === 'REJECTED') donation.status = 'Available';
                else if (newStatus.toUpperCase() === 'COLLECTED') donation.status = 'Collected';
                else if (newStatus.toUpperCase() === 'DELIVERED') donation.status = 'Delivered';
                DemoStore.set('donations', donations);
            }

            DemoStore.set('requests', requests);
            return true;
        },

        // --- ADMIN ---
        async getAdminStats() {
            if (isLiveBackend) {
                const res = await fetch(getApiBase() + '/admin/stats', { credentials: 'include' });
                const data = await res.json();
                return data.data;
            }

            const users = DemoStore.get('users', INITIAL_SAMPLE_USERS);
            const donations = DemoStore.get('donations', INITIAL_SAMPLE_DONATIONS);
            const requests = DemoStore.get('requests', INITIAL_SAMPLE_REQUESTS);

            return {
                totalUsers: users.length,
                totalDonors: users.filter(u => u.role === 'DONOR').length,
                totalNgos: users.filter(u => u.role === 'NGO').length,
                totalDonations: donations.length,
                availableCount: donations.filter(d => d.status === 'Available').length,
                completedCount: donations.filter(d => d.status === 'Collected' || d.status === 'Delivered').length,
                pendingRequests: requests.filter(r => (r.requestStatus || r.status) === 'PENDING').length
            };
        },

        async getAdminUsers() {
            if (isLiveBackend) {
                const res = await fetch(getApiBase() + '/admin/users', { credentials: 'include' });
                const data = await res.json();
                return data.data;
            }
            return DemoStore.get('users', INITIAL_SAMPLE_USERS);
        },

        // --- UI HELPERS ---
        showToast(message, type = 'success') {
            let container = document.querySelector('.toast-container');
            if (!container) {
                container = document.createElement('div');
                container.className = 'toast-container';
                document.body.appendChild(container);
            }

            const toast = document.createElement('div');
            toast.className = `toast toast-${type}`;
            const icon = type === 'success' ? '✓' : '⚠';
            toast.innerHTML = `<strong>${icon}</strong><span>${message}</span>`;
            container.appendChild(toast);

            setTimeout(() => {
                toast.style.opacity = '0';
                toast.style.transform = 'translateY(10px)';
                toast.style.transition = 'all 0.3s ease';
                setTimeout(() => toast.remove(), 300);
            }, 3500);
        },

        formatDateTime(isoStr) {
            if (!isoStr) return 'N/A';
            const d = new Date(isoStr);
            if (isNaN(d.getTime())) return isoStr;
            return d.toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        },

        formatTimeRemaining(deadlineIso) {
            if (!deadlineIso) return { text: 'N/A', urgent: false };
            const deadline = new Date(deadlineIso);
            const now = new Date();
            const diffMs = deadline - now;
            if (diffMs <= 0) return { text: 'Expired', urgent: true };

            const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
            const diffMins = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));

            if (diffHours < 3) {
                return { text: `${diffHours}h ${diffMins}m left (Urgent)`, urgent: true };
            }
            return { text: `${diffHours}h ${diffMins}m left`, urgent: false };
        },

        updateNavAuthStatus() {
            const user = this.getCurrentUser();
            const navActions = document.getElementById('nav-auth-actions');
            if (!navActions) return;

            if (user) {
                let dashboardLink = 'donor-dashboard.html';
                if (user.role === 'NGO') dashboardLink = 'ngo-dashboard.html';
                else if (user.role === 'ADMIN') dashboardLink = 'admin-dashboard.html';

                navActions.innerHTML = `
                    <div class="nav-user">
                        <span>👤 <strong>${user.name ? user.name.split(' ')[0] : 'User'}</strong></span>
                        <span class="nav-user-role">${user.role}</span>
                    </div>
                    <a href="${dashboardLink}" class="btn btn-sm btn-primary">Dashboard</a>
                    <button id="nav-logout-btn" class="btn btn-sm btn-secondary">Logout</button>
                `;

                document.getElementById('nav-logout-btn')?.addEventListener('click', () => {
                    api.logout();
                });
            } else {
                navActions.innerHTML = `
                    <a href="login.html" class="btn btn-sm btn-secondary">Log In</a>
                    <a href="register.html" class="btn btn-sm btn-primary">Register</a>
                `;
            }
        }
    };

    window.SharePlateApi = api;

    document.addEventListener('DOMContentLoaded', () => {
        api.init();

        const toggle = document.querySelector('.nav-toggle');
        const links = document.querySelector('.nav-links');
        if (toggle && links) {
            toggle.addEventListener('click', () => {
                links.classList.toggle('open');
            });
        }
    });

})(window);
