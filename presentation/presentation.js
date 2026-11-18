/* Keyboard / button navigation for the IOPMS slide deck. No dependencies. */
(function () {
  'use strict';

  const slides = [...document.querySelectorAll('.slide')];
  const counter = document.getElementById('counter');
  const progress = document.getElementById('progressBar');
  const prev = document.getElementById('prevBtn');
  const next = document.getElementById('nextBtn');
  const notesBtn = document.getElementById('notesBtn');
  let current = 0;

  function show(n) {
    current = Math.max(0, Math.min(slides.length - 1, n));
    slides.forEach((s, i) => s.classList.toggle('active', i === current));
    counter.textContent = String(current + 1).padStart(2, '0') + ' / ' + String(slides.length).padStart(2, '0');
    progress.style.width = ((current + 1) / slides.length * 100) + '%';
    prev.disabled = current === 0;
    next.disabled = current === slides.length - 1;
    slides[current].scrollTop = 0;
    history.replaceState(null, '', '#slide-' + (current + 1));
  }

  function toggleNotes() {
    document.body.classList.toggle('show-notes');
    notesBtn.classList.toggle('on');
  }

  prev.addEventListener('click', () => show(current - 1));
  next.addEventListener('click', () => show(current + 1));
  notesBtn.addEventListener('click', toggleNotes);

  document.addEventListener('keydown', (e) => {
    if (e.target.matches('input, textarea, select')) return;
    const forward = ['ArrowRight', 'ArrowDown', 'PageDown', ' '];
    const backward = ['ArrowLeft', 'ArrowUp', 'PageUp'];
    if (forward.includes(e.key)) { e.preventDefault(); show(current + 1); }
    else if (backward.includes(e.key)) { e.preventDefault(); show(current - 1); }
    else if (e.key === 'Home') { e.preventDefault(); show(0); }
    else if (e.key === 'End') { e.preventDefault(); show(slides.length - 1); }
    else if (e.key.toLowerCase() === 'n') { toggleNotes(); }
    else if (e.key.toLowerCase() === 'p') { e.preventDefault(); window.print(); }
  });

  // click on the thin progress bar jumps proportionally
  document.querySelector('.progress').addEventListener('click', (e) => {
    const ratio = e.offsetX / e.currentTarget.clientWidth;
    show(Math.floor(ratio * slides.length));
  });

  const fromHash = Number((location.hash.match(/slide-(\d+)/) || [])[1]);
  show(fromHash ? fromHash - 1 : 0);
})();
