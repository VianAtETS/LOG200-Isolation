# LOG200 — Jeu Isolation

**Équipe XXX** : TODO Prénom Nom (CODE12345678), TODO Prénom Nom (CODE12345678),
TODO Prénom Nom (CODE12345678) — groupe TODO

## Contenu de la remise

| Fichier               | Rôle                                              |
| --------------------- | ------------------------------------------------- |
| `Client-eqXXX.jar`    | Client exécutable (agent d'IA)                    |
| `Client.java`         | Point d'entrée, boucle de jeu avec le serveur     |
| `GameConnection.java` | Protocole de communication (fourni, inchangé)     |
| `rapport.pdf`         | Rapport                                           |

## Exécution

```bash
java -jar Client-eqXXX.jar [hôte] [port]   # défaut : localhost 8888
```

## Compilation

Toutes les classes sont dans le package par défaut (Java 25, aucune dépendance) :

```bash
javac -d bin *.java
jar --create --file Client-eqXXX.jar --main-class Client -C bin .
```

## Remarques sur l'implémentation

- TODO
