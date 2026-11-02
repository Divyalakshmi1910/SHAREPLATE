/**
 * SharePlate - Donor Dashboard & Donation Management Controller
 */

document.addEventListener('DOMContentLoaded', async () => {
    // -------------------------------------------------------------
    // DONOR DASHBOARD PAGE LOGIC
    // -------------------------------------------------------------
    const donorDashboard = document.getElementById('donor-dashboard-page');
    if (donorDashboard) {
        const user = SharePlateApi.requireAuth(['DONOR', 'ADMIN']);
        if (!user) return;

        // Populate donor welcome name
        const donorNameEl = document.getElementById('donor-profile-name');
        if (donorNameEl) donorNameEl.textContent = user.name;
        const donorOrgEl = document.getElementById('donor-profile-org');
        if (donorOrgEl) donorOrgEl.textContent = user.roleDisplayName || (user.role === 'ADMIN' ? 'Platform Administrator' : 'Food Donor');

        // Tab switching logic
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

        await loadDonorDashboardData(user);
    }

    // -------------------------------------------------------------
    // ADD / EDIT FOOD DONATION FORM LOGIC (add-donation.html)
    // -------------------------------------------------------------
    const donationForm = document.getElementById('donation-form');
    if (donationForm) {
        const user = SharePlateApi.requireAuth(['DONOR', 'ADMIN']);
        if (!user) return;

        const urlParams = new URLSearchParams(window.location.search);
        const editId = urlParams.get('id');

        // Set default timestamps for convenience
        const prepInput = document.getElementById('donation-prep-time');
        const deadlineInput = document.getElementById('donation-deadline');
        
        function formatDateTimeLocal(date) {
            const pad = (n) => String(n).padStart(2, '0');
            return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
        }

        if (prepInput && deadlineInput && !editId) {
            const now = new Date();
            prepInput.value = formatDateTimeLocal(now);
            const later = new Date(now.getTime() + (6 * 3600 * 1000)); // +6 hours
            deadlineInput.value = formatDateTimeLocal(later);
        }

        // If in EDIT mode, fetch existing donation
        if (editId) {
            document.getElementById('form-page-title').textContent = 'Edit Food Donation';
            document.getElementById('form-page-subtitle').textContent = 'Update information regarding your surplus food listing.';
            document.getElementById('btn-submit-donation').textContent = 'Update Food Donation';

            loadExistingDonationForEdit(editId);
        }

        donationForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const foodName = document.getElementById('donation-food-name').value.trim();
            const quantity = document.getElementById('donation-quantity').value.trim();
            const category = document.getElementById('donation-category').value;
            const prepTime = document.getElementById('donation-prep-time').value;
            const deadline = document.getElementById('donation-deadline').value;
            const pickupLocation = document.getElementById('donation-pickup').value.trim();
            const storageConditions = document.getElementById('donation-storage').value;
            const allergenInfo = document.getElementById('donation-allergens').value.trim();

            if (!foodName || !quantity || !category || !prepTime || !deadline || !pickupLocation || !storageConditions) {
                SharePlateApi.showToast('Please fill out all required donation fields.', 'error');
                return;
            }

            // Deadline must be after preparation
            if (new Date(deadline) <= new Date(prepTime)) {
                SharePlateApi.showToast('Safe collection deadline must be after the preparation date & time.', 'error');
                return;
            }

            const payload = {
                foodName,
                quantity,
                category,
                preparationDatetime: prepTime,
                safeCollectionDeadline: deadline,
                collectionDeadline: deadline,
                pickupLocation,
                storageConditions,
                allergenInformation: allergenInfo || 'None reported',
                allergenInfo: allergenInfo || 'None reported'
            };

            const submitBtn = document.getElementById('btn-submit-donation');

            try {
                submitBtn.disabled = true;
                submitBtn.innerText = editId ? 'Updating...' : 'Publishing Listing...';

                if (editId) {
                    payload.donationId = parseInt(editId);
                    payload.id = parseInt(editId);
                    await SharePlateApi.updateDonation(payload);
                    SharePlateApi.showToast('Donation listing updated successfully in MySQL!', 'success');
                } else {
                    await SharePlateApi.createDonation(payload);
                    SharePlateApi.showToast('Food donation published successfully! NGOs can now view and request it.', 'success');
                }

                setTimeout(() => {
                    window.location.href = 'donor-dashboard.html';
                }, 900);
            } catch (err) {
                SharePlateApi.showToast(err.message || 'Failed to save donation.', 'error');
                submitBtn.disabled = false;
                submitBtn.innerText = editId ? 'Update Food Donation' : 'Publish Food Donation';
            }
        });
    }
});

async function loadExistingDonationForEdit(id) {
    try {
        const donation = await SharePlateApi.getDonationById(id);
        if (!donation) {
            SharePlateApi.showToast('Donation not found.', 'error');
            setTimeout(() => window.location.href = 'donor-dashboard.html', 1000);
            return;
        }

        document.getElementById('donation-food-name').value = donation.foodName || '';
        document.getElementById('donation-quantity').value = donation.quantity || '';
        document.getElementById('donation-category').value = donation.category || 'Cooked Food';
        
        if (donation.preparationDatetime) {
            document.getElementById('donation-prep-time').value = donation.preparationDatetime.substring(0, 16);
        }
        const deadlineVal = donation.safeCollectionDeadline || donation.collectionDeadline;
        if (deadlineVal) {
            document.getElementById('donation-deadline').value = deadlineVal.substring(0, 16);
        }
        document.getElementById('donation-pickup').value = donation.pickupLocation || '';
        document.getElementById('donation-storage').value = donation.storageConditions || 'Room Temperature';
        document.getElementById('donation-allergens').value = donation.allergenInformation || donation.allergenInfo || '';
    } catch (e) {
        SharePlateApi.showToast('Error loading donation details', 'error');
    }
}

async function loadDonorDashboardData(user) {
    try {
        const userId = user.userId || user.id;
        const donations = await SharePlateApi.getDonations({ donorId: userId, scope: 'my' });
        const requests = await SharePlateApi.getRequests({ donorId: userId });

        // Calculate KPI metrics
        const totalDonated = donations.length;
        const availableCount = donations.filter(d => d.status === 'Available').length;
        const pendingReqCount = requests.filter(r => (r.requestStatus || r.status) === 'PENDING').length;
        const completedCount = donations.filter(d => d.status === 'Collected' || d.status === 'Delivered').length;

        document.getElementById('kpi-total-donations').textContent = totalDonated;
        document.getElementById('kpi-available-donations').textContent = availableCount;
        document.getElementById('kpi-pending-requests').textContent = pendingReqCount;
        document.getElementById('kpi-completed-donations').textContent = completedCount;

        document.getElementById('tab-counter-donations').textContent = totalDonated;
        document.getElementById('tab-counter-requests').textContent = requests.length;

        renderDonorDonationsTable(donations);
        renderIncomingRequestsTable(requests);
    } catch (e) {
        console.error('Failed to load donor dashboard data:', e);
        SharePlateApi.showToast('Unable to load dashboard records.', 'error');
    }
}

function renderDonorDonationsTable(donations) {
    const tbody = document.getElementById('donor-donations-tbody');
    const emptyState = document.getElementById('donor-donations-empty');
    if (!tbody) return;

    if (!donations || donations.length === 0) {
        tbody.innerHTML = '';
        if (emptyState) emptyState.style.display = 'block';
        return;
    }
    if (emptyState) emptyState.style.display = 'none';

    tbody.innerHTML = donations.map(d => {
        const dId = d.donationId || d.id;
        const statusBadgeClass = `badge-${d.status.toLowerCase()}`;
        const timeInfo = SharePlateApi.formatTimeRemaining(d.safeCollectionDeadline || d.collectionDeadline);

        return `
            <tr>
                <td>
                    <div class="primary-cell">${d.foodName}</div>
                    <span class="tag-category">${d.category}</span>
                </td>
                <td>
                    <strong>${d.quantity}</strong>
                    <div class="subtext">📦 ${d.storageConditions}</div>
                </td>
                <td>
                    <div>Prep: ${SharePlateApi.formatDateTime(d.preparationDatetime)}</div>
                    <div class="deadline-pill ${timeInfo.urgent ? 'urgent' : ''}" style="margin-top:4px;">
                        ⏰ ${timeInfo.text}
                    </div>
                </td>
                <td>
                    <span class="badge-status ${statusBadgeClass}">${d.status}</span>
                </td>
                <td>
                    <div class="table-actions">
                        <a href="add-donation.html?id=${dId}" class="btn btn-sm btn-secondary" title="Edit Donation">
                            ✏️ Edit
                        </a>
                        <button onclick="handleDeleteDonation(${dId})" class="btn btn-sm btn-danger" title="Delete Listing">
                            🗑️ Delete
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function renderIncomingRequestsTable(requests) {
    const tbody = document.getElementById('donor-requests-tbody');
    const emptyState = document.getElementById('donor-requests-empty');
    if (!tbody) return;

    if (!requests || requests.length === 0) {
        tbody.innerHTML = '';
        if (emptyState) emptyState.style.display = 'block';
        return;
    }
    if (emptyState) emptyState.style.display = 'none';

    tbody.innerHTML = requests.map(r => {
        const rId = r.requestId || r.id;
        const reqStatus = (r.requestStatus || r.status || 'PENDING').toUpperCase();
        let actionButtons = '';

        if (reqStatus === 'PENDING') {
            actionButtons = `
                <div class="table-actions">
                    <button onclick="handleRequestDecision(${rId}, 'APPROVED')" class="btn btn-sm btn-primary" title="Approve Request">
                        ✓ Approve
                    </button>
                    <button onclick="handleRequestDecision(${rId}, 'REJECTED')" class="btn btn-sm btn-danger" title="Reject Request">
                        ✕ Reject
                    </button>
                </div>
            `;
        } else if (reqStatus === 'APPROVED') {
            actionButtons = `
                <button onclick="handleRequestDecision(${rId}, 'COLLECTED')" class="btn btn-sm btn-secondary" style="color:#5b21b6; border-color:#c4b5fd;">
                    📦 Mark Handed Over
                </button>
            `;
        } else {
            actionButtons = `<span style="font-size:0.85rem; color:var(--text-muted); font-weight:600;">Completed (${reqStatus})</span>`;
        }

        return `
            <tr>
                <td>
                    <div class="primary-cell">${r.ngoName || 'Registered NGO'}</div>
                    <div class="subtext">📞 ${r.ngoPhone || 'N/A'}</div>
                    <div class="subtext">✉️ ${r.ngoEmail || 'N/A'}</div>
                </td>
                <td>
                    <strong>${r.foodName}</strong>
                    <div class="subtext">${r.foodQuantity || ''}</div>
                </td>
                <td>
                    <div style="font-weight:700; color:var(--primary-darker);">👥 ${r.beneficiaryCount || 10} people</div>
                    <div class="subtext" style="margin-top:4px;">"${r.requestNotes || r.requestMessage || 'No specific note'}"</div>
                </td>
                <td>
                    <div>${SharePlateApi.formatDateTime(r.requestDate || r.createdAt)}</div>
                </td>
                <td>
                    <span class="badge-status badge-${reqStatus.toLowerCase()}">${reqStatus}</span>
                </td>
                <td>
                    ${actionButtons}
                </td>
            </tr>
        `;
    }).join('');
}

window.handleDeleteDonation = async function (id) {
    if (!confirm('Are you sure you want to delete this food donation? This operation deletes the record from MySQL.')) {
        return;
    }
    try {
        await SharePlateApi.deleteDonation(id);
        SharePlateApi.showToast('Donation deleted from MySQL.', 'success');
        const user = SharePlateApi.getCurrentUser();
        if (user) await loadDonorDashboardData(user);
    } catch (err) {
        SharePlateApi.showToast(err.message || 'Failed to delete donation.', 'error');
    }
};

window.handleRequestDecision = async function (requestId, decision) {
    const actionText = decision === 'APPROVED' ? 'approve' : (decision === 'REJECTED' ? 'reject' : 'mark as handed over');
    if (!confirm(`Are you sure you want to ${actionText} this request? This will update the status in MySQL.`)) {
        return;
    }
    try {
        await SharePlateApi.updateRequestStatus(requestId, decision);
        SharePlateApi.showToast(`Request has been ${decision.toLowerCase()} in MySQL!`, 'success');
        const user = SharePlateApi.getCurrentUser();
        if (user) await loadDonorDashboardData(user);
    } catch (err) {
        SharePlateApi.showToast(err.message || 'Failed to update request status.', 'error');
    }
};
