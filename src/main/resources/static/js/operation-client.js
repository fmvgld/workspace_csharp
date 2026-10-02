(() => {
  const form = document.getElementById('operation-form');
  const selectedClient = document.getElementById('selected-client');
  const hiddenId = document.getElementById('client-id');
  const requiredError = document.getElementById('client-required-error');
  const receiptMailError = document.getElementById('receipt-mail-error');
  const sendReceipt = form.querySelector('input[name="envoiRecu"]');
  const source = document.getElementById('client-source');

  const pickerDialog = document.getElementById('client-picker-dialog');
  const search = document.getElementById('client-search');
  const results = document.getElementById('client-results');
  const resultCount = document.getElementById('client-result-count');
  const validateButton = document.getElementById('validate-client-choice');
  const addressName = document.getElementById('client-address-name');
  const addressMail = document.getElementById('client-address-mail');
  const addressStreet = document.getElementById('client-address-street');
  const addressCity = document.getElementById('client-address-city');

  const addDialog = document.getElementById('client-dialog');
  const addForm = document.getElementById('client-add-form');
  const addError = document.getElementById('client-add-error');
  if (!form || !selectedClient || !hiddenId || !source || !pickerDialog || !search || !results || !addDialog || !addForm) return;

  const normalize = value => (value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
  const clean = value => typeof value === 'string' ? value : '';
  const clients = [...source.querySelectorAll('[data-client-id]')].map(item => ({
    id: item.dataset.clientId,
    nom: clean(item.dataset.nom),
    prenom: clean(item.dataset.prenom),
    label: clean(item.dataset.label),
    mail: clean(item.dataset.mail),
    rue: clean(item.dataset.rue),
    complement: clean(item.dataset.complement),
    cp: clean(item.dataset.cp),
    ville: clean(item.dataset.ville),
    search: normalize(`${item.dataset.label} ${item.dataset.mail} ${item.dataset.ville}`)
  }));

  let committed = clients.find(client => String(client.id) === hiddenId.value) || null;
  let candidate = committed;

  function addressLine(client) {
    return [client.rue, client.complement].filter(Boolean).join(' ');
  }

  function clearPreview() {
    addressName.textContent = 'Sélectionnez un client dans la liste';
    addressMail.textContent = '';
    addressStreet.textContent = '';
    addressCity.textContent = '';
    validateButton.disabled = true;
  }

  function preview(client) {
    candidate = client;
    addressName.textContent = client.label;
    addressMail.textContent = client.mail ? `Courriel : ${client.mail}` : 'Courriel non renseigné';
    addressStreet.textContent = addressLine(client) || 'Adresse non renseignée';
    addressCity.textContent = [client.cp, client.ville].filter(Boolean).join(' ');
    validateButton.disabled = false;
    [...results.querySelectorAll('tr')].forEach(row => row.classList.toggle('selected', row.dataset.clientId === String(client.id)));
  }

  function commit(client) {
    committed = client;
    candidate = client;
    hiddenId.value = client.id;
    selectedClient.value = client.label;
    requiredError.hidden = true;
    validateReceiptMail();
  }

  function validateReceiptMail() {
    const valid = !sendReceipt?.checked || Boolean(committed?.mail?.trim());
    if (receiptMailError) receiptMailError.hidden = valid;
    return valid;
  }

  function rowFor(client) {
    const row = document.createElement('tr');
    row.dataset.clientId = client.id;
    row.tabIndex = 0;
    row.setAttribute('role', 'button');
    row.setAttribute('aria-label', `Sélectionner ${client.label}`);
    for (const value of [client.nom, client.prenom, client.mail]) {
      const cell = document.createElement('td');
      cell.textContent = value;
      row.appendChild(cell);
    }
    const select = () => preview(client);
    row.addEventListener('click', select);
    row.addEventListener('keydown', event => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        select();
      }
    });
    return row;
  }

  function showMatches() {
    const query = normalize(search.value);
    const matches = clients.filter(client => !query || client.search.includes(query));
    results.replaceChildren(...matches.slice(0, 100).map(rowFor));
    resultCount.textContent = String(matches.length);
    if (!matches.length) {
      const row = document.createElement('tr');
      const cell = document.createElement('td');
      cell.colSpan = 3;
      cell.className = 'empty';
      cell.textContent = 'Aucun client trouvé';
      row.appendChild(cell);
      results.appendChild(row);
    }
    if (candidate) {
      const selectedRow = results.querySelector(`[data-client-id="${CSS.escape(String(candidate.id))}"]`);
      if (selectedRow) selectedRow.classList.add('selected');
    }
  }

  function openPicker(initialSearch = '') {
    candidate = committed;
    search.value = initialSearch;
    showMatches();
    if (candidate) preview(candidate); else clearPreview();
    pickerDialog.showModal();
    search.focus();
  }

  function cancelPicker() {
    candidate = committed;
    pickerDialog.close();
    selectedClient.focus();
  }

  selectedClient.addEventListener('click', () => openPicker());
  selectedClient.addEventListener('keydown', event => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      openPicker();
    } else if (event.key.length === 1 && !event.ctrlKey && !event.altKey && !event.metaKey) {
      event.preventDefault();
      openPicker(event.key);
    }
  });
  search.addEventListener('input', showMatches);
  document.getElementById('close-client-picker').addEventListener('click', cancelPicker);
  document.getElementById('cancel-client-choice').addEventListener('click', cancelPicker);
  pickerDialog.addEventListener('cancel', () => { candidate = committed; });
  validateButton.addEventListener('click', () => {
    if (!candidate) return;
    commit(candidate);
    pickerDialog.close();
    selectedClient.focus();
  });

  document.getElementById('open-client-dialog').addEventListener('click', () => {
    addError.hidden = true;
    addDialog.showModal();
    addForm.elements.nom.focus();
  });
  document.getElementById('close-client-dialog').addEventListener('click', () => addDialog.close());
  if (sendReceipt) sendReceipt.addEventListener('change', () => {
    if (sendReceipt.checked && !validateReceiptMail()) {
      window.alert('L\'envoi du reçu est impossible : le client sélectionné n\'a pas d\'adresse courriel renseignée.');
      sendReceipt.checked = false;
      if (receiptMailError) receiptMailError.hidden = false;
      return;
    }
    validateReceiptMail();
  });

  addForm.addEventListener('submit', async event => {
    event.preventDefault();
    addError.hidden = true;
    const submit = addForm.querySelector('button[type=submit]');
    const data = new FormData(addForm);
    submit.disabled = true;
    try {
      const response = await fetch(addForm.action, {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
        body: new URLSearchParams(data)
      });
      if (!response.ok) throw new Error((await response.text()) || `Erreur ${response.status}`);
      const created = await response.json();
      const client = {
        id: String(created.id),
        nom: clean(data.get('nom')).trim(),
        prenom: clean(data.get('prenom')).trim(),
        label: created.label,
        mail: clean(data.get('mail')).trim(),
        rue: clean(data.get('nomRue')).trim(),
        complement: clean(data.get('complement')).trim(),
        cp: clean(data.get('cp')).trim(),
        ville: clean(data.get('ville')).trim()
      };
      client.search = normalize(`${client.label} ${client.mail} ${client.ville}`);
      clients.unshift(client);
      commit(client);
      addForm.reset();
      addDialog.close();
      selectedClient.focus();
    } catch (error) {
      addError.textContent = `Impossible de créer le client : ${error.message}`;
      addError.hidden = false;
    } finally {
      submit.disabled = false;
    }
  });

  form.addEventListener('submit', event => {
    if (!hiddenId.value) {
      event.preventDefault();
      requiredError.hidden = false;
      openPicker();
      return;
    }
    if (!validateReceiptMail()) {
      event.preventDefault();
      selectedClient.focus();
    }
  });
})();
