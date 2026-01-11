(function () {
    'use strict';

    const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    const csrfToken = csrfTokenMeta ? csrfTokenMeta.getAttribute('content') : null;
    const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.getAttribute('content') : 'X-CSRF-TOKEN';

    const notify = (message, type = 'info') => {
        if (window.showToast) {
            window.showToast(message, type);
        } else {
            alert(message);
        }
    };

    const sendToggleRequest = (jobId) => {
        const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        const body = new URLSearchParams({ jobId }).toString();

        return fetch('/jobs/save', {
            method: 'POST',
            headers,
            body,
            credentials: 'same-origin'
        }).then(async (response) => {
            const raw = await response.text();
            if (!response.ok) {
                throw new Error(raw || 'REQUEST_FAILED');
            }

            const contentType = response.headers.get('content-type') || '';
            if (!contentType.includes('application/json')) {
                if (raw && raw.includes('<!DOCTYPE')) {
                    throw new Error('AUTH_REQUIRED');
                }
                throw new Error('UNEXPECTED_RESPONSE');
            }

            try {
                return JSON.parse(raw);
            } catch (error) {
                throw new Error('INVALID_JSON');
            }
        });
    };

    const applySavedState = (btn, isSaved) => {
        const savedClass = btn.dataset.savedClass || 'saved';
        const unsavedClass = btn.dataset.unsavedClass || '';
        if (savedClass) {
            btn.classList.toggle(savedClass, isSaved);
        }
        if (unsavedClass) {
            btn.classList.toggle(unsavedClass, !isSaved);
        }

        const icon = btn.querySelector('i');
        const savedIcon = btn.dataset.savedIcon || 'fa-solid';
        const unsavedIcon = btn.dataset.unsavedIcon || 'fa-regular';
        if (icon) {
            icon.classList.remove(savedIcon, unsavedIcon);
            icon.classList.add(isSaved ? savedIcon : unsavedIcon);
            icon.classList.add('fa-heart');
        }
    };

    window.toggleSaveJob = function (btn) {
        if (!btn) {
            return;
        }

        const jobId = btn.dataset.jobId;
        const canSave = btn.dataset.canSave !== 'false';

        if (!jobId) {
            return;
        }

        if (!canSave) {
            notify('Vui lòng đăng nhập để lưu việc làm!', 'info');
            window.location.href = '/auth/login';
            return;
        }

        if (btn.dataset.loading === 'true') {
            return;
        }

        btn.dataset.loading = 'true';
        btn.classList.add('is-processing');

        sendToggleRequest(jobId)
            .then((data) => {
                if (!data.success) {
                    throw new Error(data.message || 'REQUEST_FAILED');
                }
                applySavedState(btn, data.saved);
                notify(data.message || (data.saved ? 'Đã lưu việc làm' : 'Đã bỏ lưu việc làm'), data.saved ? 'success' : 'info');
            })
            .catch((error) => {
                console.error('toggleSaveJob error:', error);
                if (error.message === 'AUTH_REQUIRED') {
                    notify('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.', 'danger');
                    window.location.href = '/auth/login';
                    return;
                }
                notify('Không thể lưu việc làm. Vui lòng thử lại.', 'danger');
            })
            .finally(() => {
                btn.dataset.loading = 'false';
                btn.classList.remove('is-processing');
            });
    };
})();
