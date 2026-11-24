// Frontend – mluví s Java backendem přes API na adrese /api/...
// Každá akce pošle požadavek (fetch), backend vrátí nový stav garáže
// a ten se znovu vykreslí.

const seznam = document.getElementById("seznam");
const prazdna = document.getElementById("prazdna");
const kapacita = document.getElementById("kapacita");
const btnPridat = document.getElementById("btnPridat");
const sablona = document.getElementById("sablonaAuta");
const dialog = document.getElementById("dialogPridat");
const formPridat = document.getElementById("formPridat");
const chybaFormulare = document.getElementById("chybaFormulare");
const zprava = document.getElementById("zprava");

const f1 = (cislo) => cislo.toLocaleString("cs-CZ", { maximumFractionDigits: 1 });

// ---------- Komunikace s backendem ----------

async function api(metoda, adresa, data) {
    const moznosti = { method: metoda };
    if (data) {
        // posíláme jako formulář: znacka=Škoda&objem=50 ...
        moznosti.body = new URLSearchParams(data);
    }
    const odpoved = await fetch("/api" + adresa, moznosti);
    const json = await odpoved.json();
    if (!odpoved.ok) {
        throw new Error(json.chyba || "Něco se pokazilo.");
    }
    return json;
}

// ---------- Vykreslení ----------

function vykresli(garaz) {
    kapacita.textContent = `${garaz.auta.length} / ${garaz.kapacita} míst`;
    btnPridat.disabled = garaz.auta.length >= garaz.kapacita;
    btnPridat.title = btnPridat.disabled ? "Garáž je plná" : "";
    prazdna.hidden = garaz.auta.length > 0;

    seznam.replaceChildren(...garaz.auta.map((auto, index) => vytvorKartu(auto, index)));
}

function vytvorKartu(auto, index) {
    const karta = sablona.content.firstElementChild.cloneNode(true);
    const najdi = (selektor) => karta.querySelector(selektor);

    najdi(".auto-nazev").textContent = `${auto.znacka} ${auto.model}`;
    najdi(".auto-barva").textContent = auto.barva;
    najdi(".stitek-vylet").hidden = !auto.naVylete;
    if (auto.naVylete) karta.classList.add("na-vylete");

    najdi(".p-motor").textContent = auto.motor;
    najdi(".p-vykon").textContent = `${f1(auto.vykon)} kW`;
    najdi(".p-spotreba").textContent = `${f1(auto.spotreba)} l/100 km`;
    najdi(".p-kola").textContent = `${f1(auto.velikostKol)}″ ${auto.typKol}, ${auto.barvaKol}`;

    const procento = auto.objem > 0 ? (auto.palivo / auto.objem) * 100 : 0;
    const hladina = najdi(".nadrz-hladina");
    hladina.style.width = `${procento}%`;
    if (procento < 20) hladina.classList.add("malo");
    najdi(".nadrz-stav").textContent = `${f1(auto.palivo)} / ${f1(auto.objem)} l`;
    najdi(".dojezd").textContent = auto.dojezd > 0
        ? `Dojezd ${f1(auto.dojezd)} km`
        : "Prázdná nádrž – nejdřív natankuj";

    const volne = auto.objem - auto.palivo;
    const inputLitry = najdi(".akce-tankovani input");
    inputLitry.placeholder = volne > 0 ? `max ${f1(volne)} l` : "plná nádrž";

    najdi(".akce-tankovani").addEventListener("submit", (e) => {
        e.preventDefault();
        proved(api("POST", `/auta/${index}/tankovani`, { litry: inputLitry.value }),
            `${auto.znacka} natankováno.`);
    });

    const inputKm = najdi(".akce-vylet input");
    najdi(".akce-vylet").addEventListener("submit", (e) => {
        e.preventDefault();
        proved(api("POST", `/auta/${index}/vylet`, { km: inputKm.value }),
            `Výlet ${f1(Number(inputKm.value))} km proběhl.`);
    });

    najdi(".btn-odstranit").addEventListener("click", () => {
        if (confirm(`Opravdu odstranit ${auto.znacka} ${auto.model}?`)) {
            proved(api("DELETE", `/auta/${index}`), "Auto odstraněno.");
        }
    });

    return karta;
}

// Provede akci, překreslí garáž a ukáže zprávu (nebo chybu z backendu).
async function proved(pozadavek, textUspechu) {
    try {
        vykresli(await pozadavek);
        ukazZpravu(textUspechu);
    } catch (chyba) {
        ukazZpravu(chyba.message, true);
    }
}

let casovac;
function ukazZpravu(text, jeChyba = false) {
    zprava.textContent = text;
    zprava.classList.toggle("chyba", jeChyba);
    zprava.hidden = false;
    clearTimeout(casovac);
    casovac = setTimeout(() => (zprava.hidden = true), 3500);
}

// ---------- Přidání auta ----------

btnPridat.addEventListener("click", () => {
    formPridat.reset();
    chybaFormulare.hidden = true;
    dialog.showModal();
});

document.getElementById("btnZrusit").addEventListener("click", () => dialog.close());

formPridat.addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
        vykresli(await api("POST", "/auta", new FormData(formPridat)));
        dialog.close();
        ukazZpravu("Auto přidáno do garáže.");
    } catch (chyba) {
        chybaFormulare.textContent = chyba.message;
        chybaFormulare.hidden = false;
    }
});

// ---------- Start ----------

api("GET", "/garaz")
    .then(vykresli)
    .catch(() => ukazZpravu("Nepodařilo se spojit se serverem.", true));
