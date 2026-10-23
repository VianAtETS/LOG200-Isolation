// =============================================================================
// ets-report.typ — Gabarit de rapport académique pour l'ÉTS
// =============================================================================
//
// MISE EN PLACE DU LOGO
//   Le logo officiel est versionné dans ce répertoire (ets.svg). Pour le
//   télécharger à nouveau :
//     curl -fsSL https://www.etsmtl.ca/assets/img/ets.svg -o ets.svg
//   Pour utiliser un autre chemin, passer le paramètre `logo`.
//   Pour omettre le logo : logo: none
//
// UTILISATION MINIMALE
//   #import "ets-report.typ": report, pts, ans, q, todo
//   #show: report.with(
//     work-type:   "Rapport de laboratoire",
//     course-code: "GTI611",
//     students:    ("Dupont Jean (12345678)",),
//     semester:    "Été 2026",
//     group:       "01",
//   )
//   = Introduction
//   ...
//
// COMPILATION (depuis la racine du dépôt, pour que read("/src/...") fonctionne)
//   typst compile --root . remise/rapport.typ
// =============================================================================

// ---------------------------------------------------------------------------
// Palette ÉTS
// ---------------------------------------------------------------------------
#let _blue    = rgb("#003087")   // bleu marine officiel ÉTS
#let _red     = rgb("#DA291C")   // rouge officiel ÉTS
#let _gray    = luma(140)        // texte secondaire
#let _stripe  = luma(248)        // rangées alternées des tableaux

// Mois en toutes lettres (datetime.display() ne produit que l'anglais)
#let _months = (
  fr: ("janvier", "février", "mars", "avril", "mai", "juin", "juillet",
       "août", "septembre", "octobre", "novembre", "décembre"),
  en: ("January", "February", "March", "April", "May", "June", "July",
       "August", "September", "October", "November", "December"),
)

// ---------------------------------------------------------------------------
// Aides publiques (disponibles dans les documents utilisateurs)
// ---------------------------------------------------------------------------

/// Marque un pointage en rouge.
/// Usage : == Partie 1 #pts[(15 pts)]
#let pts(body) = text(fill: _red, weight: "bold", body)

/// Colore une réponse en bleu très foncé pour la distinguer du texte de question.
#let ans(body) = text(fill: rgb("#00002A"), body)

/// Formate du texte de question (emprunté à l'énoncé) en bleu italique.
#let q(body) = emph(text(fill: _blue, body))

/// Signale un élément manquant (capture d'écran, fichier de résultats, etc.)
/// à fournir avant la remise finale. Usage : #todo[Capture d'écran de X ici.]
#let todo(body) = block(
  fill:   rgb("#FFF4E0"),
  stroke: 1pt + rgb("#B8860B"),
  inset:  8pt,
  radius: 3pt,
  width:  100%,
  [#text(fill: rgb("#B8860B"), weight: "bold")[À faire - ] #body],
)

// ---------------------------------------------------------------------------
// Aides internes
// ---------------------------------------------------------------------------

// Produit une paire (label fort, valeur) pour le tableau d'infos.
#let _cell(lbl, val) = (strong(lbl), val)

// Tableau d'infos standard à deux colonnes.
#let _info-table(rows) = table(
  columns: (auto, 1fr),
  stroke: 0.4pt + luma(200),
  inset: (x: 9pt, y: 6pt),
  fill: (_, row) => if calc.odd(row) { _stripe } else { white },
  ..rows.flatten(),
)

// Date du jour en toutes lettres, ex. « 22 septembre 2026 » ou « September 22, 2026 ».
#let _today(lang) = {
  let d = datetime.today()
  if lang == "fr" {
    str(d.day()) + " " + _months.fr.at(d.month() - 1) + " " + str(d.year())
  } else {
    _months.en.at(d.month() - 1) + " " + str(d.day()) + ", " + str(d.year())
  }
}

// ---------------------------------------------------------------------------
// Template principal
// ---------------------------------------------------------------------------

/// Gabarit de rapport/laboratoire/devoir académique pour l'ÉTS.
///
/// Paramètres
/// ----------
/// logo        : Chemin vers le logo SVG (défaut : "ets.svg" ; none pour omettre)
/// school      : Nom de l'établissement
/// department  : Nom du département
/// work-type   : Type de travail — "Rapport de laboratoire", "Devoir", "Projet"...
/// course-code : Sigle du cours, ex. "GTI611"
/// course-name : Titre complet du cours (optionnel)
/// title       : Titre spécifique du travail (optionnel)
/// number      : Numéro du travail (int ou chaîne ; none pour omettre)
/// students    : Tableau de chaînes — "Prénom Nom (CODE)" ou juste "Prénom Nom"
/// semester    : Session, ex. "Été 2026", "Automne 2026"
/// group       : Identifiant de groupe, ex. "01"
/// subgroup    : Identifiant de sous-groupe (optionnel)
/// supervisors : Tableau de noms de superviseurs / chargés de lab (optionnel)
/// date        : Date de remise (chaîne ; auto = date du jour)
/// lang        : Langue du document — "fr" ou "en"
/// show-outline: Afficher la table des matières après la page de titre
/// font        : Police principale (défaut : "New Computer Modern")
/// body-size   : Corps du texte (défaut : 11pt)
/// heading-num : Schéma de numérotation des titres (défaut : "1.1.")
/// grading     : Tableau de (critère, pondération) pour le barème ; () pour omettre
///               Exemple : (("Introduction", "5 %"), ("Conclusion", "10 %"))
///               Le total affiché est toujours « 100 % » (la somme n'est pas vérifiée).
/// body        : Contenu du document (positionnel, en dernier)
#let report(
  logo:         "ets.svg",
  school:       "École de technologie supérieure",
  department:   "Département de génie logiciel et des TI",
  work-type:    "",
  course-code:  "",
  course-name:  "",
  title:        "",
  number:       none,
  students:     (),
  semester:     "",
  group:        "",
  subgroup:     none,
  supervisors:  (),
  date:         auto,
  lang:         "fr",
  show-outline: false,
  font:         "New Computer Modern",
  body-size:    11pt,
  heading-num:  "1.1.",
  grading:      (),
  body,
) = {

  // ---- Validation ----
  assert(work-type   != "", message: "work-type est requis")
  assert(course-code != "", message: "course-code est requis")
  assert(semester    != "", message: "semester est requis")
  assert(group       != "", message: "group est requis")
  assert(students.len() >= 1, message: "Au moins un étudiant est requis")
  assert(lang in ("fr", "en"), message: "lang doit être \"fr\" ou \"en\"")

  let num-str = if number == none {
    none
  } else if type(number) == int {
    str(number)
  } else {
    number
  }

  let date-str = if date == auto { _today(lang) } else { date }

  // Ligne affichée dans l'en-tête des pages de corps
  let hdr-line = course-code + " \u{2014} " + work-type + if num-str != none { " " + num-str } else { "" }

  // Étiquettes bilingues
  let lbl = if lang == "fr" {(
    students:   if students.len() > 1 { "Étudiants" } else { "Étudiant" },
    course:     "Cours",
    semester:   "Session",
    group:      "Groupe",
    subgroup:   "Sous-groupe",
    number:     "Numéro",
    supervisor: if supervisors.len() > 1 { "Superviseurs" } else { "Superviseur" },
    date:       "Date de remise",
    grading:    "Barème d'évaluation",
    toc:        "Table des matières",
    total:      "Total",
  )} else {(
    students:   if students.len() > 1 { "Students" } else { "Student" },
    course:     "Course",
    semester:   "Semester",
    group:      "Group",
    subgroup:   "Subgroup",
    number:     "Number",
    supervisor: if supervisors.len() > 1 { "Supervisors" } else { "Supervisor" },
    date:       "Submission date",
    grading:    "Grading",
    toc:        "Table of Contents",
    total:      "Total",
  )}

  // ---- Métadonnées ----
  set document(
    title:  hdr-line,
    author: students,
    date:   none,
  )

  // ---- Typographie ----
  set text(font: font, size: body-size, lang: lang, hyphenate: true)
  set par(justify: true, leading: 0.65em, spacing: 1.2em)

  // ---- Titres ----
  set heading(numbering: heading-num)

  show heading.where(level: 1): it => {
    v(1.4em, weak: true)
    text(size: body-size + 3pt, weight: "bold", fill: _blue, it)
    v(1em, weak: true)
  }
  show heading.where(level: 2): it => {
    v(1em, weak: true)
    text(size: body-size + 1pt, weight: "bold", fill: _blue, it)
    v(0.9em, weak: true)
  }
  show heading.where(level: 3): it => {
    v(0.7em, weak: true)
    text(size: body-size, weight: "semibold", fill: _blue, it)
    v(0.8em, weak: true)
  }
  show heading.where(level: 4): it => {
    v(0.8em, weak: true)
    text(
      size: body-size,
      weight: "semibold",
      fill: _blue,
      it,
    )
    v(0.7em, weak: true)
  }

  // ---- Blocs de code ----
  show raw.where(block: false): box.with(
    fill:     luma(232),
    inset:    (x: 3pt, y: 2pt),
    radius:   2pt,
    baseline: 0pt,
  )
  show raw.where(block: true): it => block(
    fill:   luma(232),
    inset:  (x: 1em, y: 0.8em),
    radius: 4pt,
    width:  100%,
    it,
  )

  // ---- Mise en page ----
  // La page de titre (page physique 1) n'a ni en-tête ni pied de page.
  // Les pages suivantes ont un en-tête avec le sigle et le numéro de page
  // (numérotées à partir de 1). Le test porte sur la page physique, car le
  // compteur vaut 1 sur la première page du corps.
  set page(
    paper:     "a4",
    margin:    (top: 2.5cm, bottom: 2.5cm, x: 2.5cm),
    numbering: none,
    header: context {
      if here().page() > 1 {
        set text(size: 9pt, fill: _gray)
        grid(
          columns: (1fr, auto),
          align:   (left + horizon, right + horizon),
          hdr-line,
          counter(page).display("1"),
        )
        v(-0.5em)
        line(length: 100%, stroke: 0.4pt + luma(210))
      }
    },
  )

  // ==========================================================================
  // Page de titre
  // ==========================================================================
  {
    set align(center)

    // Logo + identité
    if logo != none {
      image(logo, height: 3.19528cm)
    } else {
      v(0.6cm)
    }
    text(size: 14pt, weight: "bold", school)
    linebreak()
    text(size: 10.5pt, fill: _gray, department)

    // Diviseur rouge
    v(1.8em)
    line(length: 58%, stroke: 1.2pt + _red)
    v(1.8em)

    // Type de travail et numéro
    text(size: 22pt, weight: "bold", fill: _blue, work-type)
    if num-str != none {
      linebreak()
      v(0.3em)
      text(size: 14pt, fill: _blue, "No.\u{a0}" + num-str)
    }

    // Sigle et nom du cours
    v(0.9em)
    text(size: 12pt, style: "italic")[
      #course-code#if course-name != "" [ \u{2014} #course-name]
    ]

    // Titre spécifique (optionnel)
    if title != "" {
      v(0.7em)
      text(size: 12pt, weight: "bold", title)
    }

    // Diviseur gris
    v(1.8em)
    line(length: 58%, stroke: 0.5pt + luma(190))
    v(1.8em)

    // Tableau d'informations
    {
      set align(left)

      // Construire les lignes conditionnellement
      let rows = (
        _cell(lbl.students, students.sorted().join(linebreak())),
        _cell(lbl.course,   course-code),
        _cell(lbl.semester, semester),
        _cell(lbl.group,    group),
      )

      if subgroup != none {
        rows = rows + (_cell(lbl.subgroup, subgroup),)
      }
      if num-str != none {
        rows = rows + (_cell(lbl.number, num-str),)
      }
      if supervisors != () {
        rows = rows + (_cell(lbl.supervisor, supervisors.sorted().join(linebreak())),)
      }
      rows = rows + (_cell(lbl.date, date-str),)

      _info-table(rows)
    }

    // Barème (optionnel)
    if grading.len() > 0 {
      v(1.6em)
      {
        set align(left)
        text(size: 10.5pt, weight: "bold", lbl.grading)
        v(0.5em)
        table(
          columns: (1fr, auto),
          stroke:  0.4pt + luma(200),
          inset:   (x: 9pt, y: 6pt),
          fill:    (_, row) => if calc.odd(row) { _stripe } else { white },
          ..grading.map(row => (row.at(0), strong(row.at(1)))).flatten(),
          strong(lbl.total), strong("100\u{a0}%"),
        )
      }
    }
  }

  // ==========================================================================
  // Corps du document
  // ==========================================================================

  // Numéroter les pages de corps à partir de 1 (numéro affiché dans l'en-tête
  // uniquement). La remise à 0 se fait AVANT le saut de page : l'en-tête d'une
  // page est évalué avant son contenu, donc un update(1) placé après le saut
  // ne serait vu qu'à partir de la page suivante (d'où « 2, 2, 3... »).
  counter(page).update(0)
  pagebreak()

  if show-outline {
    outline(
      title:  text(size: body-size + 2pt, weight: "bold", fill: _blue, lbl.toc),
      indent: auto,
      depth:  3,
    )
    pagebreak()
  }

  body
}
