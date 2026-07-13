(function initializePhoneFormatting() {
    function formatPhone(value) {
        var digits = (value || '').replace(/\D/g, '').slice(0, 11);
        if (digits.length <= 3) {
            return digits;
        }
        if (digits.startsWith('02')) {
            if (digits.length <= 6) {
                return digits.slice(0, 2) + '-' + digits.slice(2);
            }
            if (digits.length <= 10) {
                return digits.slice(0, 2) + '-' + digits.slice(2, digits.length - 4) + '-' + digits.slice(-4);
            }
        }
        if (digits.length <= 7) {
            return digits.slice(0, 3) + '-' + digits.slice(3);
        }
        return digits.slice(0, 3) + '-' + digits.slice(3, 7) + '-' + digits.slice(7);
    }

    function isPhoneInput(input) {
        if (!input || input.dataset.phoneFormatBound === 'true') {
            return false;
        }
        if (input.type === 'tel' || input.dataset.phoneFormat === 'true') {
            return true;
        }
        var name = input.getAttribute('name') || '';
        var id = input.getAttribute('id') || '';
        return /(^|\.)(primaryContactNo|contactNo|blackUserContact|appHphone|appTele|mobilePhone|telPhone)$/.test(name)
            || /^(primaryContactNo|contactNo|blackUserContact|appHphone|appTele|mobilePhone|telPhone)$/.test(id);
    }

    function formatInput(input) {
        var formatted = formatPhone(input.value);
        if (input.value !== formatted) {
            input.value = formatted;
        }
    }

    function bindPhoneInputs(root) {
        root.querySelectorAll('input').forEach(function (input) {
            if (!isPhoneInput(input)) {
                return;
            }
            input.dataset.phoneFormatBound = 'true';
            input.setAttribute('inputmode', 'numeric');
            input.setAttribute('autocomplete', input.name === 'mobilePhone' ? 'tel' : input.getAttribute('autocomplete') || 'off');
            input.addEventListener('input', function () {
                formatInput(input);
            });
            input.addEventListener('blur', function () {
                formatInput(input);
            });
            if (input.form) {
                input.form.addEventListener('submit', function () {
                    formatInput(input);
                });
            }
            formatInput(input);
        });
    }

    window.formatKoreanPhoneNumber = formatPhone;
    window.bindKoreanPhoneInputs = bindPhoneInputs;

    document.addEventListener('DOMContentLoaded', function () {
        bindPhoneInputs(document);
    });
}());
