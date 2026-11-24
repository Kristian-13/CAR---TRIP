// Frontend – mluví s Java backendem přes API na adrese /api/...
// Každá akce pošle požadavek (fetch), backend vrátí nový stav garáže
// a ten se znovu vykreslí. Simulace výletu jen přehrává výsledek,
// který spočítal backend (kolik paliva auto na trase spotřebovalo).

const $ = (id) => document.getElementById(id);
const f1 = (cislo) => Number(cislo).toLocaleString("cs-CZ", { maximumFractionDigits: 1 });
const f0 = (cislo) => Math.round(cislo).toLocaleString("cs-CZ");

let stav = { kapacita: 5, auta: [], denik: [] };

// Oblíbené cíle (přibližné vzdálenosti po silnici z Prahy)
const CILE = [
    { nazev: "Karlštejn", km: 35 },
    { nazev: "Kutná Hora", km: 70 },
    { nazev: "Krkonoše", km: 160 },
    { nazev: "Český Krumlov", km: 175 },
    { nazev: "Brno", km: 205 },
    { nazev: "Vídeň", km: 330 },
    { nazev: "Ostrava", km: 370 },
];

const UKAZKOVA_AUTA = [
    { znacka: "Škoda", model: "Octavia", barva: "modrá", motor: "2.0 TDI", spotreba: 5.2, vykon: 110, objem: 50, velikostKol: 17, typKol: "letní", barvaKol: "stříbrná", natankovat: 35 },
    { znacka: "Fiat", model: "500", barva: "červená", motor: "1.2", spotreba: 5.8, vykon: 51, objem: 35, velikostKol: 15, typKol: "celoroční", barvaKol: "černá", natankovat: 12 },
    { znacka: "Volkswagen", model: "Golf", barva: "bílá", motor: "1.5 TSI", spotreba: 6.4, vykon: 110, objem: 50, velikostKol: 16, typKol: "zimní", barvaKol: "šedá", natankovat: 50 },
];

// Barva z textu („modrá“) na barvu pro obrazovku
const BARVY = {
    bila: "#f2f2f0", cerna: "#24262a", stribrna: "#b9bec6", seda: "#7d838c", cervena: "#d6363b",
    modra: "#2563eb", zelena: "#2f9e62", zluta: "#f2c230", oranzova: "#ef7d22", hneda: "#8a5a3b",
    fialova: "#7c4dce", ruzova: "#e46aa6", bezova: "#d8c3a0", zlata: "#c9a23c", tyrkysova: "#1fb5b0",
};

function barvaAuta(text) {
    const klic = text.toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "");
    for (const [nazev, hex] of Object.entries(BARVY)) {
        if (klic.includes(nazev.slice(0, -1))) return hex; // „modrá“, „modrou“, „tmavě modrá“…
    }
    return "#5b6472";
}

// ===================== Komunikace s backendem =====================

async function api(metoda, adresa, data) {
    const moznosti = { method: metoda };
    if (data) moznosti.body = new URLSearchParams(data); // znacka=Škoda&objem=50…
    const odpoved = await fetch("/api" + adresa, moznosti);
    const json = await odpoved.json();
    if (!odpoved.ok) throw new Error(json.chyba || "Něco se pokazilo.");
    return json;
}

// ===================== Vykreslení garáže =====================

function vykresli(novyStav) {
    stav = novyStav;
    const { auta, kapacita } = stav;

    $("statAuta").textContent = `${auta.length} / ${kapacita}`;
    $("statKm").textContent = `${f0(auta.reduce((s, a) => s + a.najeto, 0))} km`;
    $("btnPridat").disabled = auta.length >= kapacita;
    $("btnPridat").title = auta.length >= kapacita ? "Garáž je plná" : "";
    $("prazdna").hidden = auta.length > 0;

    $("seznam").replaceChildren(...auta.map(vytvorKartu));
    vykresliDenik();
}

function vytvorKartu(auto, index) {
    const karta = $("sablonaAuta").content.firstElementChild.cloneNode(true);
    const najdi = (s) => karta.querySelector(s);

    karta.style.setProperty("--barva-auta", barvaAuta(auto.barva));
    najdi(".auto-nazev").textContent = `${auto.znacka} ${auto.model}`;
    najdi(".auto-barva").textContent = auto.barva;
    najdi(".stitek-vylet").hidden = !auto.naVylete;

    najdi(".p-motor").textContent = auto.motor;
    najdi(".p-vykon").textContent = `${f1(auto.vykon)} kW`;
    najdi(".p-spotreba").textContent = `${f1(auto.spotreba)} l/100 km`;
    najdi(".p-kola").textContent = `${f1(auto.velikostKol)}″ ${auto.typKol}`;
    najdi(".p-najeto").textContent = `${f0(auto.najeto)} km`;
    najdi(".p-dojezd").textContent = auto.dojezd > 0 ? `${f0(auto.dojezd)} km` : "prázdná nádrž";

    const procento = (auto.palivo / auto.objem) * 100;
    const hladina = najdi(".nadrz-hladina");
    hladina.style.width = `${procento}%`;
    hladina.classList.toggle("malo", procento < 20);
    najdi(".nadrz-stav").textContent = `${f1(auto.palivo)} / ${f1(auto.objem)} l`;

    const volne = auto.objem - auto.palivo;
    const inputLitry = najdi(".akce-tankovani input");
    inputLitry.placeholder = volne > 0.05 ? `max ${f1(volne)} l` : "plná nádrž";
    najdi(".btn-plna").disabled = volne <= 0.05;

    najdi(".akce-tankovani").addEventListener("submit", (e) => {
        e.preventDefault();
        proved(api("POST", `/auta/${index}/tankovani`, { litry: inputLitry.value }),
            `${auto.znacka} ${auto.model}: natankováno.`);
    });
    najdi(".btn-plna").addEventListener("click", () => {
        proved(api("POST", `/auta/${index}/tankovani`, { litry: volne.toFixed(2) }),
            `${auto.znacka} ${auto.model}: nádrž je plná.`);
    });
    najdi(".btn-vylet").addEventListener("click", () => otevriPlanovac(index));
    najdi(".btn-odstranit").addEventListener("click", async () => {
        const nazev = `${auto.znacka} ${auto.model}`;
        if (await potvrd("Odstranit auto?", `${nazev} zmizí z garáže. Tohle nejde vrátit.`)) {
            proved(api("DELETE", `/auta/${index}`), `${nazev} odstraněno.`);
        }
    });

    return karta;
}

function vykresliDenik() {
    const polozky = stav.denik.map((j) => {
        const li = document.createElement("li");
        li.innerHTML = `<span class="denik-cil"></span><span class="denik-auto"></span>
            <span class="denik-cislo">${f0(j.km)} km</span><span class="denik-cislo">${f1(j.litry)} l</span>`;
        li.querySelector(".denik-cil").textContent = j.cil;
        li.querySelector(".denik-auto").textContent = j.auto;
        return li;
    });
    $("denik").replaceChildren(...polozky);
    $("denikPrazdny").hidden = polozky.length > 0;
}

async function proved(pozadavek, textUspechu) {
    try {
        vykresli(await pozadavek);
        ukazZpravu(textUspechu);
    } catch (chyba) {
        ukazZpravu(chyba.message, true);
    }
}

// Vlastní potvrzovací okno – vrací Promise s true (Odstranit) nebo false (Zrušit / Esc)
function potvrd(nadpis, text) {
    const dialog = $("dialogPotvrdit");
    $("potvrditNadpis").textContent = nadpis;
    $("potvrditText").textContent = text;
    dialog.returnValue = "";
    dialog.showModal();
    return new Promise((odpoved) =>
        dialog.addEventListener("close", () => odpoved(dialog.returnValue === "ano"), { once: true }));
}

let casovacZpravy;
function ukazZpravu(text, jeChyba = false) {
    const z = $("zprava");
    z.textContent = text;
    z.classList.toggle("chyba", jeChyba);
    z.hidden = false;
    clearTimeout(casovacZpravy);
    casovacZpravy = setTimeout(() => (z.hidden = true), 3500);
}

// ===================== Přidání auta =====================

$("btnPridat").addEventListener("click", () => {
    $("formPridat").reset();
    $("chybaFormulare").hidden = true;
    $("dialogPridat").showModal();
});

$("formPridat").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
        vykresli(await api("POST", "/auta", new FormData(e.target)));
        $("dialogPridat").close();
        ukazZpravu("Auto přidáno do garáže.");
    } catch (chyba) {
        $("chybaFormulare").textContent = chyba.message;
        $("chybaFormulare").hidden = false;
    }
});

$("btnUkazka").addEventListener("click", async () => {
    try {
        for (const { natankovat, ...auto } of UKAZKOVA_AUTA) {
            if (stav.auta.length >= stav.kapacita) break;
            vykresli(await api("POST", "/auta", auto));
            vykresli(await api("POST", `/auta/${stav.auta.length - 1}/tankovani`, { litry: natankovat }));
        }
        ukazZpravu("Ukázková auta jsou v garáži.");
    } catch (chyba) {
        ukazZpravu(chyba.message, true);
    }
});

// Všechna tlačítka „Zrušit“ / „Zpět“ zavřou svůj dialog
document.querySelectorAll("[data-zavrit]").forEach((btn) =>
    btn.addEventListener("click", () => btn.closest("dialog").close()));

// ===================== Plánovač výletu =====================

let planovaneAuto = -1;
const formVylet = $("formVylet");

function otevriPlanovac(index) {
    planovaneAuto = index;
    const auto = stav.auta[index];
    formVylet.reset();
    $("chybaVyletu").hidden = true;
    $("vyletAuto").textContent = `${auto.znacka} ${auto.model}`;

    $("cile").replaceChildren(...CILE.map((cil) => {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "cil";
        btn.innerHTML = `<span></span><small>${cil.km} km</small>`;
        btn.firstChild.textContent = cil.nazev;
        btn.addEventListener("click", () => {
            formVylet.cil.value = cil.nazev;
            formVylet.km.value = cil.km;
            aktualizujPredpoved();
        });
        return btn;
    }));

    aktualizujPredpoved();
    $("dialogVylet").showModal();
}

// Kolik paliva je potřeba dotankovat, aby auto dojelo (0 = nemusí, null = nejde ani s plnou nádrží).
// Přidá se rezerva 3 l, aby auto nedojelo do cíle úplně na suchu.
const REZERVA = 3;
function chybejiciPalivo(auto, km) {
    const potreba = (auto.spotreba * km) / 100;
    if (potreba <= auto.palivo) return 0;
    if (potreba > auto.objem) return null;
    return Math.min(Math.ceil((potreba - auto.palivo + REZERVA) * 10) / 10, auto.objem - auto.palivo);
}

function aktualizujPredpoved() {
    const auto = stav.auta[planovaneAuto];
    const km = Number(formVylet.km.value);
    const cil = formVylet.cil.value.trim();

    document.querySelectorAll(".cil").forEach((btn, i) =>
        btn.classList.toggle("vybrany", CILE[i].nazev === cil && CILE[i].km === km));

    const btn = $("btnVyrazit");
    if (!(km > 0)) {
        $("predpoved").hidden = true;
        btn.textContent = "Vyrazit";
        btn.disabled = false;
        return;
    }

    const potreba = (auto.spotreba * km) / 100;
    const chybi = chybejiciPalivo(auto, km);
    $("predpoved").hidden = false;
    $("pSpotreba").textContent = `${f1(potreba)} l`;
    $("pNadrz").textContent = `${f1(auto.palivo)} l`;
    $("pZbyde").textContent = chybi === 0 ? `${f1(auto.palivo - potreba)} l` : "—";

    const varovani = $("pVarovani");
    varovani.hidden = chybi === 0;
    btn.disabled = chybi === null;
    if (chybi === null) {
        varovani.textContent = `Ani s plnou nádrží to nedáš – maximální dojezd je ${f0((auto.objem / auto.spotreba) * 100)} km.`;
        btn.textContent = "Nedojede";
    } else if (chybi > 0) {
        varovani.textContent = `Auto nedojede (dojezd ${f0(auto.dojezd)} km). Před cestou natankuješ ${f1(chybi)} l (i s malou rezervou).`;
        btn.textContent = `Natankovat ${f1(chybi)} l a vyrazit`;
    } else {
        btn.textContent = "Vyrazit";
    }
}

formVylet.addEventListener("input", aktualizujPredpoved);

formVylet.addEventListener("submit", async (e) => {
    e.preventDefault();
    const index = planovaneAuto;
    const km = Number(formVylet.km.value);
    const cil = formVylet.cil.value.trim() || "Výlet";
    const predCestou = stav.auta[index];

    try {
        const chybi = chybejiciPalivo(predCestou, km);
        if (chybi > 0) {
            vykresli(await api("POST", `/auta/${index}/tankovani`, { litry: chybi }));
        }
        const palivoPred = stav.auta[index].palivo;
        const novyStav = await api("POST", `/auta/${index}/vylet`, { km, cil });
        $("dialogVylet").close();

        await simulujJizdu({
            auto: predCestou, cil, km,
            palivoPred, palivoPo: novyStav.auta[index].palivo,
            natankovano: chybi,
        });
        vykresli(novyStav);
    } catch (chyba) {
        $("chybaVyletu").textContent = chyba.message;
        $("chybaVyletu").hidden = false;
    }
});

// ===================== Simulace jízdy =====================

const sim = {
    dialog: $("dialogSimulace"),
    vrstvy: [...document.querySelectorAll("#scena [data-rychlost]")],
    kola: [...document.querySelectorAll("#autoSim .kolo")],
};

// Rychlostní profil: auto se rozjede, jede, a na konci zabrzdí.
// u = čas 0..1, vrací rychlost 0..1 a ujetou část trasy 0..1
const ROZJEZD = 0.14;
function profil(u) {
    const a = ROZJEZD;
    const rychlost = Math.max(0, Math.min(u / a, 1, (1 - u) / a));
    let draha;
    if (u < a) draha = (u * u) / (2 * a);
    else if (u <= 1 - a) draha = a / 2 + (u - a);
    else draha = 1 - a - ((1 - u) * (1 - u)) / (2 * a);
    return { rychlost, cast: Math.min(1, draha / (1 - a)) };
}

function udalost(cast, km, cil) {
    if (cast < 0.04) return "Vyjíždíš z garáže…";
    if (cast < 0.3) return km > 150 ? "Najíždíš na dálnici" : "Okresní silnice, slabý provoz";
    if (cast < 0.55) return km > 150 ? "Tempomat na 130, rádio hraje" : "Projíždíš vesnicí, pozor na přechod";
    if (cast < 0.62) return "Polovina cesty je za tebou";
    if (cast < 0.9) return "Pěkná krajina, jede se dobře";
    if (cast < 1) return `Blížíš se k cíli: ${cil}`;
    return "Jsi v cíli!";
}

function simulujJizdu({ auto, cil, km, palivoPred, palivoPo, natankovano }) {
    return new Promise((hotovo) => {
        const omezitPohyb = matchMedia("(prefers-reduced-motion: reduce)").matches;
        const delka = omezitPohyb ? 0 : Math.min(12, Math.max(5, 4 + km / 45)) * 1000; // ms
        const cestovni = km > 150 ? 125 : 85; // km/h
        const spotrebovano = palivoPred - palivoPo;

        sim.dialog.style.setProperty("--barva-auta", barvaAuta(auto.barva));
        $("simAuto").textContent = `${auto.znacka} ${auto.model}`;
        $("simCil").textContent = `Cesta: ${cil}`;
        $("trasaCil").textContent = cil;
        $("simSouhrn").hidden = true;
        $("btnPreskocit").hidden = false;
        sim.dialog.classList.remove("v-cili");
        sim.dialog.showModal();

        let start = null;
        let posun = 0;
        let natoceni = 0;
        let predchozi = 0;
        let bezi = true;

        function vykresliSnimek(u, dt) {
            const { rychlost, cast } = profil(u);
            const ujeto = km * cast;
            const palivo = palivoPred - spotrebovano * cast;
            const zakmit = rychlost > 0.98 ? Math.sin(u * 40) * 3 : 0;

            posun += rychlost * 900 * dt;
            natoceni += rychlost * 1400 * dt;
            sim.vrstvy.forEach((v) => (v.style.backgroundPositionX = `${-posun * v.dataset.rychlost}px`));
            sim.kola.forEach((k) => (k.style.transform = `rotate(${natoceni}deg)`));
            $("autoSim").style.translate = `0 ${rychlost > 0.1 ? Math.sin(u * 160) * 1.2 : 0}px`;

            $("hudUjeto").textContent = f1(ujeto);
            $("hudRychlost").textContent = f0(cestovni * rychlost + zakmit);
            $("hudPalivo").textContent = f1(palivo);
            $("hudZbyva").textContent = f1(km - ujeto);
            $("hudNadrz").style.width = `${(palivo / auto.objem) * 100}%`;
            $("hudNadrz").classList.toggle("malo", palivo / auto.objem < 0.2);
            $("trasaUjeto").style.width = `${cast * 100}%`;
            $("trasaZnacka").style.left = `${cast * 100}%`;
            $("simUdalost").textContent = udalost(cast, km, cil);
        }

        function snimek(cas) {
            if (!bezi) return;
            if (start === null) start = predchozi = cas;
            const dt = (cas - predchozi) / 1000;
            predchozi = cas;
            const u = delka === 0 ? 1 : Math.min(1, (cas - start) / delka);
            vykresliSnimek(u, dt);
            if (u < 1) requestAnimationFrame(snimek);
            else dojel();
        }

        function dojel() {
            bezi = false;
            vykresliSnimek(1, 0);
            $("btnPreskocit").hidden = true;
            sim.dialog.classList.add("v-cili");
            $("souhrnNadpis").textContent = `V cíli: ${cil}`;
            const dojezd = (palivoPo / auto.spotreba) * 100;
            $("souhrnText").textContent =
                (natankovano > 0 ? `Před cestou se dotankovalo ${f1(natankovano)} l. ` : "") +
                `Ujeto ${f0(km)} km, spotřeba ${f1(spotrebovano)} l paliva. ` +
                `V nádrži zbývá ${f1(palivoPo)} l, to vystačí ještě na ${f0(dojezd)} km.`;
            $("simSouhrn").hidden = false;
            $("simSouhrn").querySelector("button").focus();
        }

        $("btnPreskocit").onclick = () => bezi && dojel();
        sim.dialog.addEventListener("close", () => { bezi = false; hotovo(); }, { once: true });
        requestAnimationFrame(snimek);
    });
}

// ===================== Start =====================

api("GET", "/garaz")
    .then(vykresli)
    .catch(() => ukazZpravu("Nepodařilo se spojit se serverem.", true));
