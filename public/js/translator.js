/**
 * translator.js
 * Bouton flottant qui traduit toute la page via /api/translate
 * Inclure ce fichier dans base.html.twig
 */

(function () {
    'use strict';

    const SUPPORTED_LANGS = [
        { code: 'en', label: '🇬🇧 English' },
        { code: 'ar', label: '🇸🇦 العربية' },
        { code: 'de', label: '🇩🇪 Deutsch' },
        { code: 'es', label: '🇪🇸 Español' },
        { code: 'it', label: '🇮🇹 Italiano' },
    ];

    // Langue originale de la page
   const PAGE_LANG = 'fr';

    // Noeuds texte originaux sauvegardés pour restauration
    const originalTexts = new Map();
    let currentLang = PAGE_LANG;
    let isTranslating = false;

    // ─────────────────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────────────────

    function buildWidget() {
        // Conteneur principal
        const wrapper = document.createElement('div');
        wrapper.id = 'translator-widget';
        wrapper.style.cssText = `
            position: fixed;
            bottom: 24px;
            right: 24px;
            z-index: 9999;
            font-family: sans-serif;
        `;

        // Bouton principal
        const btn = document.createElement('button');
        btn.id = 'translator-btn';
        btn.title = 'Traduire la page';
        btn.innerHTML = '🌐';
        btn.style.cssText = `
            width: 52px;
            height: 52px;
            border-radius: 50%;
            background: #4A90D9;
            color: #fff;
            border: none;
            font-size: 22px;
            cursor: pointer;
            box-shadow: 0 4px 12px rgba(0,0,0,0.2);
            transition: background 0.2s;
            display: flex;
            align-items: center;
            justify-content: center;
        `;
        btn.addEventListener('mouseenter', () => btn.style.background = '#357ABD');
        btn.addEventListener('mouseleave', () => btn.style.background = '#4A90D9');

        // Dropdown langues
        const dropdown = document.createElement('div');
        dropdown.id = 'translator-dropdown';
        dropdown.style.cssText = `
            display: none;
            position: absolute;
            bottom: 60px;
            right: 0;
            background: #fff;
            border-radius: 10px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.15);
            overflow: hidden;
            min-width: 160px;
        `;

        // Option "Langue originale"
        const restoreBtn = document.createElement('button');
        restoreBtn.textContent = '🔄 Original';
        restoreBtn.style.cssText = langBtnStyle();
        restoreBtn.addEventListener('click', () => {
            restoreOriginal();
            closeDropdown();
        });
        dropdown.appendChild(restoreBtn);

        // Séparateur
        const sep = document.createElement('hr');
        sep.style.cssText = 'margin: 0; border: none; border-top: 1px solid #eee;';
        dropdown.appendChild(sep);

        // Options langues
        SUPPORTED_LANGS.forEach(({ code, label }) => {
            const langBtn = document.createElement('button');
            langBtn.textContent = label;
            langBtn.dataset.lang = code;
            langBtn.style.cssText = langBtnStyle();
            langBtn.addEventListener('click', () => {
                translatePage(code);
                closeDropdown();
            });
            dropdown.appendChild(langBtn);
        });

        // Spinner
        const spinner = document.createElement('div');
        spinner.id = 'translator-spinner';
        spinner.innerHTML = '⏳';
        spinner.style.cssText = `
            display: none;
            position: absolute;
            bottom: 60px;
            right: 0;
            background: #fff;
            border-radius: 10px;
            padding: 12px 16px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.15);
            font-size: 14px;
            color: #555;
        `;
        spinner.textContent = 'Traduction en cours...';

        btn.addEventListener('click', () => {
            if (isTranslating) return;
            dropdown.style.display = dropdown.style.display === 'none' ? 'block' : 'none';
        });

        // Fermer en cliquant ailleurs
        document.addEventListener('click', (e) => {
            if (!wrapper.contains(e.target)) closeDropdown();
        });

        wrapper.appendChild(dropdown);
        wrapper.appendChild(spinner);
        wrapper.appendChild(btn);
        document.body.appendChild(wrapper);
    }

    function langBtnStyle() {
        return `
            display: block;
            width: 100%;
            padding: 10px 16px;
            background: none;
            border: none;
            text-align: left;
            cursor: pointer;
            font-size: 14px;
            color: #333;
            transition: background 0.15s;
        `;
    }

    function closeDropdown() {
        const d = document.getElementById('translator-dropdown');
        if (d) d.style.display = 'none';
    }

    function setSpinner(visible) {
        const s = document.getElementById('translator-spinner');
        const b = document.getElementById('translator-btn');
        if (s) s.style.display = visible ? 'block' : 'none';
        if (b) b.style.opacity = visible ? '0.6' : '1';
        isTranslating = visible;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Collecte des noeuds texte
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retourne tous les noeuds texte visibles hors scripts/styles/widget.
     */
    function collectTextNodes() {
        const walker = document.createTreeWalker(
            document.body,
            NodeFilter.SHOW_TEXT,
            {
                acceptNode(node) {
                    const parent = node.parentElement;
                    if (!parent) return NodeFilter.FILTER_REJECT;

                    // Exclure le widget lui-même
                    if (parent.closest('#translator-widget')) return NodeFilter.FILTER_REJECT;

                    // Exclure scripts, styles, inputs
                    const tag = parent.tagName.toLowerCase();
                    if (['script', 'style', 'noscript', 'input', 'textarea', 'select'].includes(tag)) {
                        return NodeFilter.FILTER_REJECT;
                    }

                    // Exclure texte vide
                    if (node.textContent.trim() === '') return NodeFilter.FILTER_REJECT;

                    return NodeFilter.FILTER_ACCEPT;
                }
            }
        );

        const nodes = [];
        let node;
        while ((node = walker.nextNode())) {
            nodes.push(node);
        }
        return nodes;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Traduction
    // ─────────────────────────────────────────────────────────────────────────

    async function translatePage(targetLang) {
        if (targetLang === currentLang) return;
        if (isTranslating) return;

        setSpinner(true);

        const nodes = collectTextNodes();

        // Sauvegarde des originaux (une seule fois)
        nodes.forEach((node, i) => {
            if (!originalTexts.has(node)) {
                originalTexts.set(node, node.textContent);
            }
        });

        // Regrouper en batch de 20 noeuds max pour limiter les appels API
        const BATCH = 20;
        const batches = [];
        for (let i = 0; i < nodes.length; i += BATCH) {
            batches.push(nodes.slice(i, i + BATCH));
        }

        try {
            for (const batch of batches) {
                // Joindre les textes avec un séparateur unique
                const separator = ' ||| ';
                const combined  = batch.map(n => originalTexts.get(n) || n.textContent).join(separator);

                const result = await callTranslateApi(combined, targetLang, PAGE_LANG);

                // Séparer les traductions
                const parts = result.split(separator);
                batch.forEach((node, idx) => {
                    if (parts[idx] !== undefined) {
                        node.textContent = parts[idx];
                    }
                });
            }

            currentLang = targetLang;
        } catch (err) {
            console.error('Erreur de traduction :', err);
            alert('Erreur lors de la traduction. Veuillez réessayer.');
        } finally {
            setSpinner(false);
        }
    }

    function restoreOriginal() {
        originalTexts.forEach((original, node) => {
            node.textContent = original;
        });
        currentLang = PAGE_LANG;
    }

    async function callTranslateApi(text, target, source) {
        const response = await fetch('/api/translate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text, target, source }),
        });

        if (!response.ok) {
            throw new Error('HTTP ' + response.status);
        }

        const data = await response.json();
        if (typeof data === 'string') {
            return data;
        }

        return data.translated ?? text;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Init
    // ─────────────────────────────────────────────────────────────────────────

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', buildWidget);
    } else {
        buildWidget();
    }

})();