### Makefile to creat alle from IDB
###
### Authors: Demian Vöhringer
### Date 7.12.2020

################################################################################

# Folder for UebungsBlaetter
UebungBlaetter = Uebung/Blaetter/

# Folder for UebungsFolien
UebungFolien = Uebung/Folien/LaTeX/

# Folder for VorlesungsFolien
VorlesungsFolien = Vorlesungsfolien/neues_format/

################################################################################

.PHONY: all ci UBlaetter UFolien Uebung UBlaetter_ci UFolien_ci Vorlesung Vorlesung_ci

all: Uebung Vorlesung

ci: UFolien_ci UBlaetter_ci Vorlesung_ci

Uebung: UBlaetter UFolien 

UBlaetter:
	$(MAKE) -j -C $(UebungBlaetter)

UBlaetter_ci:
	$(MAKE) -C $(UebungBlaetter) ci

UFolien:
	$(MAKE) -j -C $(UebungFolien)

UFolien_ci:
	$(MAKE) -C $(UebungFolien) ci

Vorlesung:
	$(MAKE) -j -C $(VorlesungsFolien)

Vorlesung_ci:
	$(MAKE) -C $(VorlesungsFolien) ci

clean: cleanUF cleanUB cleanVl

cleanUF:
	$(MAKE) -C $(UebungFolien) clean

cleanUB:
	$(MAKE) -C $(UebungBlaetter) clean

cleanVl:
	$(MAKE) -C $(VorlesungsFolien) clean

