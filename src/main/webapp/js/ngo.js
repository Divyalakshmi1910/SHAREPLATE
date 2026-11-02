/**
 * SharePlate - NGO Dashboard & Available Donations Controller
 */

document.addEventListener('DOMContentLoaded', async () => {
    // -------------------------------------------------------------
    // AVAILABLE DONATIONS PAGE LOGIC (available-donations.html)
    // -------------------------------------------------------------
    const availablePage = document.getElementById('available-donations-page');
    if (availablePage) {
        await loadAvailableDonations();

        // Search & Filter listeners
        const searchInput = document.getElementById('search-food');
        const categoryFilter = document.getElementById('filter-category');
        const storageFilter = document.getElementById('filter-storage');

        function applyFilters() {
            const query = (searchInput?.value || '').toLowerCase().trim();
            const category = categoryFilter?.value || 'ALL';
            const storage = storageFilter?.value || 'ALL';

            const cards = document.querySelectorAll('.available-donation-card');
            let matchCount = 0;

            cards.forEach(card => {
                const name = (card.getAttribute('data-name') || '').toLowerCase();
                const loc = (card.getAttribute('data-location') || '').toLowerCase();
                const cat = card.getAttribute('data-category') || '';
                const stor = card.getAttribute('data-storage') || '';

                const matchesQuery = !query || name.includes(query) || loc.includes(query);
                const matchesCat = category === 'ALL' || cat === category;
                const matchesStorage = storage === 'ALL' || stor === storage;

                if (matchesQuery && matchesCat && matchesStorage) {
                    card.style.display = 'flex';
                    matchCount++;
                } else {
                    card.style.display = 'none';
                }
            });

            const emptyEl = document.getElementById('available-empty-state');
            if (emptyEl) {
                emptyEl.style.display = matchCount === 0 ? 'block' : 'none';
            }
        }

        searchInput?.addEventListener('input', applyFilters);
        categoryFilter?.addEventListener('change', applyFilters);
        storageFilter?.addEventListener('change', applyFilters);
    }

    // -------------------------------------------------------------
    // NGO DASHBOARD PAGE LOGIC (ngo-dashboard.html)
    // -------------------------------------------------------------
    const ngoDashboard = document.getElementById('ngo-dashboard-page');
    if (ngoDashboard) {
        const user = SharePlateApi.requireAuth(['NGO', 'ADMIN']);
        if (!user) return;

        // Welcome banner
        const ngoNameEl = document.getElementById('ngo-profile-name');
        if (ngoNameEl) ngoNameEl.textContent = user.name;
        const ngoOrgEl = document.getElementById('ngo-profile-org');
        if (ngoOrgEl) ngoOrgEl.textContent = user.roleDisplayName || (user.role === 'ADMIN' ? 'Platform Administrator' : 'NGO / Community Charity');

        // Tab switching
        const tabBtns = document.querySelectorAll('.tab-btn');
        const tabContents = document.querySelectorAll('.tab-content');

        tabBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                tabBtns.forEach(b => b.classList.remove('active'));
                tabContents.forEach(c => c.classList.remove('active'));

                btn.classList.add('active');
                const targetId = btn.getAttribute('data-target');
                document.getElementById(targetId)?.classList.add('active');
            });
        });

        await loadNgoDashboardData(user);
    }

    // -------------------------------------------------------------
    // FOOD REQUEST MODAL LOGIC
    // -------------------------------------------------------------
    const requestModal = document.getElementById('request-modal');
    const requestForm = document.getElementById('food-request-form');

    if (requestModal && requestForm) {
        document.querySelectorAll('.modal-close, .btn-modal-cancel').forEach(btn => {
            btn.addEventListener('click', () => {
                requestModal.classList.remove('active');
            });
        });

        requestModal.addEventListener('click', (e) => {
            if (e.target === requestModal) {
                requestModal.classList.remove('active');
            }
        });

        requestForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const donationId = document.getElementById('req-donation-id').value;
            const beneficiaryCount = parseInt(document.getElementById('req-beneficiaries').value) || 20;
            const message = document.getElementById('req-notes').value.trim();

            const submitBtn = requestForm.querySelector('button[type="submit"]');

            try {
                submitBtn.disabled = true;
                submitBtn.innerText = 'Submitting Request to MySQL...';

                await SharePlateApi.createRequest({
                    donationId: parseInt(donationId),
                    beneficiaryCount,
                    requestNotes: message,
                    requestMessage: message
                });

                SharePlateApi.showToast('Food request placed in MySQL! Waiting for Donor approval.', 'success');
                requestModal.classList.remove('active');

                if (document.getElementById('available-donations-page')) {
                    await loadAvailableDonations();
                } else if (document.getElementById('ngo-dashboard-page')) {
                    const u = SharePlateApi.getCurrentUser();
                    if (u) await loadNgoDashboardData(u);
                }
            } catch (err) {
                SharePlateApi.showToast(err.message || 'Failed to submit request.', 'error');
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerText = 'Confirm Food Request';
            }
        });
    }
});

async function loadAvailableDonations() {
    const grid = document.getElementById('available-donations-grid');
    const emptyState = document.getElementById('available-empty-state');
    if (!grid) return;

    try {
        const donations = await SharePlateApi.getDonations({ scope: 'available' });

        if (!donations || donations.length === 0) {
            grid.innerHTML = '';
            if (emptyState) emptyState.style.display = 'block';
            return;
        }
        if (emptyState) emptyState.style.display = 'none';

        grid.innerHTML = donations.map(d => {
            const dId = d.donationId || d.id;
            const deadlineVal = d.safeCollectionDeadline || d.collectionDeadline;
            const timeInfo = SharePlateApi.formatTimeRemaining(deadlineVal);
            const allergenVal = d.allergenInformation || d.allergenInfo || 'None reported';

            return `
                <div class="donation-card available-donation-card" 
                     data-name="${d.foodName.toLowerCase()}" 
                     data-location="${d.pickupLocation.toLowerCase()}" 
                     data-category="${d.category}" 
                     data-storage="${d.storageConditions}">
                    <div class="donation-card-header">
                        <div>
                            <h3>${d.foodName}</h3>
                            <span class="tag-category" style="margin-top:4px;">${d.category}</span>
                        </div>
                        <span class="badge-status badge-available">Available</span>
                    </div>

                    <div class="donation-card-body">
                        <div class="donation-detail-row">
                            <strong>Quantity:</strong>
                            <span class="value" style="font-weight:700; color:var(--primary-darker);">${d.quantity}</span>
                        </div>
                        <div class="donation-detail-row">
                            <strong>Donor:</strong>
                            <span class="value">${d.donorName || 'Food Donor'}</span>
                        </div>
                        <div class="donation-detail-row">
                            <strong>Pickup Address:</strong>
                            <span class="value">📍 ${d.pickupLocation}</span>
                        </div>
                        <div class="donation-detail-row">
                            <strong>Storage:</strong>
                            <span class="value">❄️ ${d.storageConditions}</span>
                        </div>
                        <div class="donation-detail-row">
                            <strong>Allergens:</strong>
                            <span class="value">⚠️ ${allergenVal}</span>
                        </div>
                        <div class="donation-detail-row">
                            <strong>Prepared:</strong>
                            <span class="value">${SharePlateApi.formatDateTime(d.preparationDatetime)}</span>
                        </div>
                        <div class="donation-detail-row">
                            <strong>Collection By:</strong>
                            <div class="value">
                                <span class="deadline-pill ${timeInfo.urgent ? 'urgent' : ''}">
                                    ⏰ ${timeInfo.text}
                                </span>
                            </div>
                        </div>
                    </div>

                    <div class="donation-card-footer">
                        <button onclick="openRequestModal(${dId})" class="btn btn-primary btn-block">
                            🤝 Request This Food
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    } catch (e) {
        console.error('Failed to load available donations:', e);
        SharePlateApi.showToast('Could not load surplus food listings.', 'error');
    }
}

window.openRequestModal = async function (donationId) {
    const user = SharePlateApi.getCurrentUser();
    if (!user) {
        SharePlateApi.showToast('Please log in as an NGO to request food.', 'error');
        setTimeout(() => window.location.href = 'login.html', 1000);
        return;
    }
    if (user.role !== 'NGO' && user.role !== 'ADMIN') {
        SharePlateApi.showToast('Only registered NGOs can request food donations.', 'error');
        return;
    }

    try {
        const donation = await SharePlateApi.getDonationById(donationId);
        if (!donation) {
            SharePlateApi.showToast('Donation details could not be retrieved.', 'error');
            return;
        }

        const dId = donation.donationId || donation.id;
        document.getElementById('req-donation-id').value = dId;
        document.getElementById('req-modal-food-name').textContent = donation.foodName;
        document.getElementById('req-modal-quantity').textContent = donation.quantity;
        document.getElementById('req-modal-donor').textContent = `${donation.donorName || 'Donor'} (${donation.pickupLocation})`;

        const modal = document.getElementById('request-modal');
        if (modal) modal.classList.add('active');
    } catch (err) {
        SharePlateApi.showToast('Error preparing request modal', 'error');
    }
};

async function loadNgoDashboardData(user) {
    try {
        const userId = user.userId || user.id;
        const myRequests = await SharePlateApi.getRequests({ ngoId: userId });

        const totalReq = myRequests.length;
        const approvedCount = myRequests.filter(r => (r.requestStatus || r.status) === 'APPROVED').length;
        const pendingCount = myRequests.filter(r => (r.requestStatus || r.status) === 'PENDING').length;
        const deliveredCount = myRequests.filter(r => (r.requestStatus || r.status) === 'DELIVERED').length;

        document.getElementById('ngo-kpi-total').textContent = totalReq;
        document.getElementById('ngo-kpi-approved').textContent = approvedCount;
        document.getElementById('ngo-kpi-pending').textContent = pendingCount;
        document.getElementById('ngo-kpi-delivered').textContent = deliveredCount;

        document.getElementById('ngo-tab-counter-requests').textContent = totalReq;

        renderNgoRequestsTable(myRequests);
        await loadAvailableDonations();
    } catch (e) {
        console.error('Failed to load NGO dashboard data:', e);
        SharePlateApi.showToast('Could not load NGO records.', 'error');
    }
}

function renderNgoRequestsTable(requests) {
    const tbody = document.getElementById('ngo-requests-tbody');
    const emptyState = document.getElementById('ngo-requests-empty');
    if (!tbody) return;

    if (!requests || requests.length === 0) {
        tbody.innerHTML = '';
        if (emptyState) emptyState.style.display = 'block';
        return;
    }
    if (emptyState) emptyState.style.display = 'none';

    tbody.innerHTML = requests.map(r => {
        const rId = r.requestId || r.id;
        const st = (r.requestStatus || r.status || 'PENDING').toUpperCase();
        let statusBadge = `<span class="badge-status badge-${st.toLowerCase()}">${st}</span>`;
        let actionBtn = '';

        if (st === 'APPROVED') {
            actionBtn = `
                <button onclick="handleNgoStatusUpdate(${rId}, 'COLLECTED')" class="btn btn-sm btn-primary">
                    📦 Confirm Picked Up
                </button>
            `;
        } else if (st === 'COLLECTED') {
            actionBtn = `
                <button onclick="handleNgoStatusUpdate(${rId}, 'DELIVERED')" class="btn btn-sm btn-secondary" style="color:#059669; border-color:#10b981;">
                    🎉 Confirm Delivered
                </button>
            `;
        } else if (st === 'PENDING') {
            actionBtn = `<span style="font-size:0.85rem; color:var(--text-muted);">Awaiting Donor Approval</span>`;
        } else {
            actionBtn = `<span style="font-size:0.85rem; color:var(--primary-dark); font-weight:700;">✓ Completed (${st})</span>`;
        }

        return `
            <tr>
                <td>
                    <div class="primary-cell">${r.foodName}</div>
                    <span class="tag-category">${r.foodCategory || 'Surplus Food'}</span>
                    <div class="subtext">Quantity: ${r.foodQuantity}</div>
                </td>
                <td>
                    <strong>${r.donorName || 'Donor'}</strong>
                    <div class="subtext">📞 ${r.donorPhone || 'N/A'}</div>
                    <div class="subtext">📍 ${r.pickupLocation}</div>
                </td>
                <td>
                    <strong>👥 ${r.beneficiaryCount || 10} people</strong>
                    <div class="subtext">Requested: ${SharePlateApi.formatDateTime(r.requestDate || r.createdAt)}</div>
                </td>
                <td>
                    ${statusBadge}
                </td>
                <td>
                    ${actionBtn}
                </td>
            </tr>
        `;
    }).join('');
}

window.handleNgoStatusUpdate = async function (requestId, nextStatus) {
    const actionLabel = nextStatus === 'COLLECTED' 
        ? 'confirm that you have collected this food donation from the donor' 
        : 'confirm that this food has been safely delivered to beneficiaries';
    if (!confirm(`Are you ready to ${actionLabel}? This updates the status in MySQL.`)) {
        return;
    }

    try {
        await SharePlateApi.updateRequestStatus(requestId, nextStatus);
        SharePlateApi.showToast(`Status updated to ${nextStatus} in MySQL!`, 'success');
        const user = SharePlateApi.getCurrentUser();
        if (user) await loadNgoDashboardData(user);
    } catch (err) {
        SharePlateApi.showToast(err.message || 'Status update failed.', 'error');
    }
};
