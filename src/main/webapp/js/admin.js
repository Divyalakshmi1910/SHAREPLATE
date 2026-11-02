/**
 * SharePlate - Admin Dashboard Controller
 */

document.addEventListener('DOMContentLoaded', async () => {
    const adminDashboard = document.getElementById('admin-dashboard-page');
    if (!adminDashboard) return;

    const user = SharePlateApi.requireAuth(['ADMIN']);
    if (!user) return;

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

    // Close detail modal
    const detailModal = document.getElementById('admin-detail-modal');
    if (detailModal) {
        document.querySelectorAll('.modal-close, .btn-modal-close').forEach(b => {
            b.addEventListener('click', () => detailModal.classList.remove('active'));
        });
        detailModal.addEventListener('click', (e) => {
            if (e.target === detailModal) detailModal.classList.remove('active');
        });
    }

    await loadAdminDashboard();
});

let adminMasterDonations = [];
let adminMasterUsers = [];

async function loadAdminDashboard() {
    try {
        const stats = await SharePlateApi.getAdminStats();
        adminMasterDonations = await SharePlateApi.getDonations({ scope: 'all' });
        adminMasterUsers = await SharePlateApi.getAdminUsers();

        // Update KPIs with real MySQL statistics
        document.getElementById('admin-kpi-donors').textContent = stats.totalDonors || 0;
        document.getElementById('admin-kpi-ngos').textContent = stats.totalNgos || 0;
        document.getElementById('admin-kpi-donations').textContent = stats.totalDonations || 0;
        document.getElementById('admin-kpi-delivered').textContent = stats.completedCount || 0;

        document.getElementById('admin-tab-counter-donations').textContent = adminMasterDonations.length;
        document.getElementById('admin-tab-counter-users').textContent = adminMasterUsers.length;

        renderAdminDonationsTable(adminMasterDonations);
        renderAdminUsersTable(adminMasterUsers);

        setupAdminFilters();
    } catch (e) {
        console.error('Failed to load admin stats:', e);
        SharePlateApi.showToast('Could not load administrative analytics from MySQL.', 'error');
    }
}

function renderAdminDonationsTable(donations) {
    const tbody = document.getElementById('admin-donations-tbody');
    const emptyState = document.getElementById('admin-donations-empty');
    if (!tbody) return;

    if (!donations || donations.length === 0) {
        tbody.innerHTML = '';
        if (emptyState) emptyState.style.display = 'block';
        return;
    }
    if (emptyState) emptyState.style.display = 'none';

    tbody.innerHTML = donations.map(d => {
        const dId = d.donationId || d.id;
        const deadlineVal = d.safeCollectionDeadline || d.collectionDeadline;

        return `
            <tr>
                <td><strong>#${dId}</strong></td>
                <td>
                    <div class="primary-cell">${d.foodName}</div>
                    <span class="tag-category">${d.category}</span>
                </td>
                <td>
                    <div style="font-weight:600;">${d.donorName || 'Donor #' + d.donorId}</div>
                    <div class="subtext">📞 ${d.donorPhone || 'N/A'}</div>
                </td>
                <td>${d.quantity}</td>
                <td>
                    <div>Safe By: ${SharePlateApi.formatDateTime(deadlineVal)}</div>
                    <div class="subtext">Storage: ${d.storageConditions}</div>
                </td>
                <td>
                    <span class="badge-status badge-${d.status.toLowerCase()}">${d.status}</span>
                </td>
                <td>
                    <div class="table-actions">
                        <button onclick="inspectDonation(${dId})" class="btn btn-sm btn-secondary" title="View Full Details">
                            🔍 View
                        </button>
                        <button onclick="adminDeleteDonation(${dId})" class="btn btn-sm btn-danger" title="Admin Delete">
                            🗑️
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function renderAdminUsersTable(users) {
    const tbody = document.getElementById('admin-users-tbody');
    const emptyState = document.getElementById('admin-users-empty');
    if (!tbody) return;

    if (!users || users.length === 0) {
        tbody.innerHTML = '';
        if (emptyState) emptyState.style.display = 'block';
        return;
    }
    if (emptyState) emptyState.style.display = 'none';

    tbody.innerHTML = users.map(u => {
        const uId = u.userId || u.id;
        let roleBadgeColor = '#059669';
        if (u.role === 'NGO') roleBadgeColor = '#2563eb';
        if (u.role === 'ADMIN') roleBadgeColor = '#7c3aed';

        return `
            <tr>
                <td><strong>#${uId}</strong></td>
                <td>
                    <div class="primary-cell">${u.name}</div>
                    <div class="subtext">${u.roleDisplayName || u.role}</div>
                </td>
                <td>
                    <span style="display:inline-block; padding:2px 8px; border-radius:9999px; font-size:0.75rem; font-weight:700; color:white; background:${roleBadgeColor}">
                        ${u.role}
                    </span>
                </td>
                <td>
                    <div>${u.email}</div>
                    <div class="subtext">📞 ${u.phone || 'N/A'}</div>
                </td>
                <td>
                    <span class="subtext">Registered: ${SharePlateApi.formatDateTime(u.createdAt)}</span>
                </td>
            </tr>
        `;
    }).join('');
}

function setupAdminFilters() {
    const searchDonations = document.getElementById('admin-search-donations');
    const filterStatus = document.getElementById('admin-filter-status');

    function applyDonationsFilter() {
        const q = (searchDonations?.value || '').toLowerCase().trim();
        const st = filterStatus?.value || 'ALL';

        const filtered = adminMasterDonations.filter(d => {
            const matchesQ = !q || (d.foodName && d.foodName.toLowerCase().includes(q)) || 
                                  (d.donorName && d.donorName.toLowerCase().includes(q));
            const matchesSt = st === 'ALL' || d.status.toUpperCase() === st.toUpperCase();
            return matchesQ && matchesSt;
        });

        renderAdminDonationsTable(filtered);
    }

    searchDonations?.addEventListener('input', applyDonationsFilter);
    filterStatus?.addEventListener('change', applyDonationsFilter);

    const searchUsers = document.getElementById('admin-search-users');
    const filterRole = document.getElementById('admin-filter-role');

    function applyUsersFilter() {
        const q = (searchUsers?.value || '').toLowerCase().trim();
        const role = filterRole?.value || 'ALL';

        const filtered = adminMasterUsers.filter(u => {
            const matchesQ = !q || (u.name && u.name.toLowerCase().includes(q)) || 
                                  (u.email && u.email.toLowerCase().includes(q));
            const matchesRole = role === 'ALL' || u.role.toUpperCase() === role.toUpperCase();
            return matchesQ && matchesRole;
        });

        renderAdminUsersTable(filtered);
    }

    searchUsers?.addEventListener('input', applyUsersFilter);
    filterRole?.addEventListener('change', applyUsersFilter);
}

window.inspectDonation = async function (id) {
    const donation = adminMasterDonations.find(d => (d.donationId || d.id) === parseInt(id));
    if (!donation) return;

    document.getElementById('modal-det-food').textContent = donation.foodName;
    document.getElementById('modal-det-status').innerHTML = `<span class="badge-status badge-${donation.status.toLowerCase()}">${donation.status}</span>`;
    document.getElementById('modal-det-category').textContent = donation.category;
    document.getElementById('modal-det-quantity').textContent = donation.quantity;
    document.getElementById('modal-det-donor').textContent = `${donation.donorName || 'Donor'} (#${donation.donorId})`;
    document.getElementById('modal-det-phone').textContent = donation.donorPhone || 'N/A';
    document.getElementById('modal-det-location').textContent = donation.pickupLocation;
    document.getElementById('modal-det-storage').textContent = donation.storageConditions;
    document.getElementById('modal-det-allergens').textContent = donation.allergenInformation || donation.allergenInfo || 'None reported';
    document.getElementById('modal-det-prep').textContent = SharePlateApi.formatDateTime(donation.preparationDatetime);
    document.getElementById('modal-det-deadline').textContent = SharePlateApi.formatDateTime(donation.safeCollectionDeadline || donation.collectionDeadline);
    document.getElementById('modal-det-notes').textContent = 'Registered in MySQL database.';

    document.getElementById('admin-detail-modal')?.classList.add('active');
};

window.adminDeleteDonation = async function (id) {
    if (!confirm(`ADMIN ACTION: Permanently delete donation #${id} from MySQL database?`)) {
        return;
    }
    try {
        await SharePlateApi.deleteDonation(id);
        SharePlateApi.showToast('Donation permanently deleted from MySQL.', 'success');
        await loadAdminDashboard();
    } catch (err) {
        SharePlateApi.showToast(err.message || 'Failed to remove donation.', 'error');
    }
};
