# IDB Gitlab-Repository

## Struktur

### Vorlesungsfolien

Die Vorlesungsfolien befinden sich im Zugehörigen Ordner *Vorlesungsfolien*.

### Übungsunterlagen

#### [Blätter]

[![Download Übungspdfs here](../badges/main/pipeline.svg?key_text=Download+Blätter&key_width=120)][Blätter]

Die Blätter befinden sich unter *Uebung/Blaetter*. Sie können mithilfe des Makefiles generiert werden.
Anschließend befinden diese sich dann in *Uebung/Blaetter/_out*.  

#### [Folien]

[![Download Übungspdfs here](../badges/main/pipeline.svg?key_text=Download+Folien&key_width=120)][Folien]

Folien existieren teils in LaTeX unter *Uebung/Folien*.

#### Code

Der Code für die Semesterbegleitende Implementierungsaufgabe befindet sich in *Uebung/Code*.  
Er kann mit ```ant -f private.xml test``` getestet werden.  
Die Vorlage kann mit ```ant -f private.xml package``` erzeugt werden.

### Sonstiges

In *Uebung/Materialien* findet sich eine SQL-Datenbank für das erste Übungsblatt.

In *Material* finden sich nichtverwendete Materialien.

[Blätter]: ../-/jobs/artifacts/main/download?job=build_blaetter
[Folien]: ../-/jobs/artifacts/main/download?job=build_folien
