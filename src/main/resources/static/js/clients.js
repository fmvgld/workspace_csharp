(() => {
  const search = document.getElementById('q');
  const rows = [...document.querySelectorAll('.client-row')];
  const count = document.getElementById('client-count');
  const body = document.getElementById('client-table-body');
  if (!search || !count || !body) return;

  const normalize = value => (value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();

  function filterClients() {
    const words = normalize(search.value).split(/\s+/).filter(Boolean);
    let visible = 0;
    for (const row of rows) {
      const text = normalize(row.dataset.search);
      const matches = words.every(word => text.includes(word));
      row.hidden = !matches;
      if (matches) visible++;
    }
    count.textContent = `${visible} résultat(s)`;
    let empty = document.getElementById('client-filter-empty');
    if (!visible && rows.length) {
      if (!empty) {
        empty = document.createElement('tr');
        empty.id = 'client-filter-empty';
        const cell = document.createElement('td');
        cell.colSpan = 6;
        cell.className = 'empty';
        cell.textContent = 'Aucun client trouvé.';
        empty.appendChild(cell);
        body.appendChild(empty);
      }
      empty.hidden = false;
    } else if (empty) {
      empty.hidden = true;
    }
  }

  search.addEventListener('input', filterClients);
  for (const row of rows) {
    const open = event => {
      if (event.target.closest('a,button,form,input')) return;
      window.location.href = row.dataset.editUrl;
    };
    row.addEventListener('click', open);
    row.addEventListener('keydown', event => {
      if (event.key === 'Enter') {
        event.preventDefault();
        window.location.href = row.dataset.editUrl;
      }
    });
  }
  filterClients();
})();
