### Makefile to creat alle from IDB
###
### Authors: Demian Vöhringer
### Date 7.12.2020

################################################################################

# Folder for UebungsBlaetter
UebungBlaetter = Uebung/Blaetter/

# Folder for UebungsFolien
UebungFolien = Uebung/Folien/

################################################################################

.PHONY: all ci UBlaetter UFolien Uebung UBlaetter_ci UFolien_ci Vorlesung Vorlesung_ci

all: Uebung

ci: UFolien_ci UBlaetter_ci

Uebung: UBlaetter UFolien

UBlaetter:
	$(MAKE) -j -C $(UebungBlaetter)

UBlaetter_ci: UBlaetter
	$(MAKE) -C $(UebungBlaetter) ci
	cp -r $(UebungBlaetter)_out Blaetter

UFolien:
	$(MAKE) -j -C $(UebungFolien)

UFolien_ci: UFolien
	$(MAKE) -C $(UebungFolien) ci
	cp -r $(UebungFolien)_out Folien

clean: cleanUF cleanUB

cleanUF:
	$(MAKE) -C $(UebungFolien) clean
	rm -rf Folien

cleanUB:
	$(MAKE) -C $(UebungBlaetter) clean
	rm -rf Blaetter
