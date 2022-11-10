#!/bin/bash
paper=_Blatt.tex
solution=_ML.tex
tutor=_Notizen.tex
beamer=_Beamer.tex
rm *.tex
for i in $(ls Inhalt | grep tex);
do
        filename=${i%%.*}
        printf "\\input{Format/paper}\n\\input{Inhalt/$filename}\n" > $filename$paper;
        printf "\\input{Format/solution}\n\\input{Inhalt/$filename}\n" > $filename$solution;
	printf "\\input{Format/tutor}\n\\input{Inhalt/$filename}\n" > $filename$tutor;
       	printf "\\input{Format/beamer}\n\\input{Inhalt/$filename}\n" > $filename$beamer;
done
cp Includes/Uebung_lang.tex Includes/Uebung.tex
