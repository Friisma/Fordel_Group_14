/*
server-side was nice.

then I wanted instant search.

javascript joever.
*/

document.addEventListener("DOMContentLoaded", () => {
    const searchInput = document.getElementById("search");
    const cards = Array.from(document.querySelectorAll("[data-user-name]"));
    const noResults = document.getElementById("users-no-results");

    if (!searchInput || cards.length === 0) {
        return;
    }

    let debounceTimer = null;

    const applySearch = () => {
        const query = searchInput.value.trim().toLowerCase();
        let visibleCount = 0;

        const nameMatches = cards.filter((card) => {
            const name = (card.dataset.userName || "").toLowerCase();
            return query === "" || name.includes(query);
        });

        const useEmailFallback = query !== "" && nameMatches.length === 0;

        cards.forEach((card) => {
            const name = (card.dataset.userName || "").toLowerCase();
            const email = (card.dataset.userEmail || "").toLowerCase();
            const matches = useEmailFallback
                ? email.includes(query)
                : query === "" || name.includes(query);
            card.hidden = !matches;

            if (matches) {
                visibleCount += 1;
            }
        });

        if (noResults) {
            noResults.hidden = visibleCount !== 0;
        }
    };

    searchInput.addEventListener("input", () => {
        window.clearTimeout(debounceTimer);
        debounceTimer = window.setTimeout(applySearch, 250);
    });

    applySearch();
});