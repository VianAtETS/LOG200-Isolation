#import "ets-report.typ": report, pts, ans, q, todo

#show: report.with(
  work-type:   "Laboratoire",
  course-code: "LOG200",
  course-name: "Structures de données et algorithmes",
  title:       "Jeu Isolation",
  students:    ("Prénom Nom (CODE12345678)",),
  semester:    "Automne 2026",
  group:       "01",
)

#todo[Compléter le nom, le code permanent et le groupe sur la page de titre.]

= Introduction

#q[Implémenter un agent intelligent pour une version modifiée du jeu Isolation
(plateau 12×12) à l'aide de l'algorithme Alpha-Bêta, d'heuristiques adaptées
au jeu et de stratégies permettant d'améliorer l'efficacité de la recherche.]

= Implémentation

== Client (`Client`)

#raw(read("/src/Client.java"), lang: "java", block: true)
